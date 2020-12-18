package com.heinzdevelopment.customquote;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.widget.CheckBox;

import java.util.LinkedList;
import java.util.List;

public class QuoteConfigure extends Activity
{
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private String mCurrentFont;
	private String mCurrentFontPath;
	private String mRefreshRate;
	private final String TAG = "QUOTE_CONFIGURE";
	
	public QuoteConfigure()
	{
		super();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		setContentView(R.layout.configure_layout);
	
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if(extras != null)
		{
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			Log.d(TAG, "widgetID: " + mAppWidgetId);

            FontPicker picker = new FontPicker(this);
            picker.Create(null);
            String initFont = picker.getFont(1);
            String initFontPath = picker.getFontFile(1);
			
			SharedPreferences settings = getSharedPreferences("Quote" + mAppWidgetId,0);
			boolean autoMode = settings.getBoolean(QuoteProvider.SHARED_AUTO_MODE, false);
			String quote = settings.getString(QuoteProvider.SHARED_MANUAL_QUOTE, QuoteProvider.START_QUOTE);
			String speaker = settings.getString(QuoteProvider.SHARED_MANUAL_SPEAKER,QuoteProvider.START_SPEAKER);
            if(autoMode)
            {
                quote = settings.getString(QuoteProvider.SHARED_AUTO_QUOTE, QuoteProvider.START_QUOTE);
                speaker = settings.getString(QuoteProvider.SHARED_AUTO_SPEAKER,QuoteProvider.START_SPEAKER);
            }
			mRefreshRate = settings.getString(QuoteProvider.SHARED_REFRESH_RATE, "1.0");
			int cycleOption = settings.getInt(QuoteProvider.SHARED_CYCLE_OPTION, QuoteProvider.CYCLE_RANDOM);
		//	String cycleList = settings.getString(QuoteProvider.SHARED_CYCLE_LIST, "All");
			mCurrentFont = settings.getString(QuoteProvider.SHARED_FONT,initFont);
			mCurrentFontPath = settings.getString(QuoteProvider.SHARED_FONT_FILE, initFontPath);
			int color = settings.getInt(QuoteProvider.SHARED_COLOR, 0xFF000000);
			boolean showSpeaker = settings.getBoolean(QuoteProvider.SHARED_SHOW_SPEAKER, true);
		//	boolean wrapQuote = settings.getBoolean(QuoteProvider.SHARED_WRAP_QUOTE, true);
			int alignQuote = settings.getInt(QuoteProvider.SHARED_ALIGN_QUOTE, QuoteProvider.ALIGN_LEFT);
            String prepend = settings.getString(QuoteProvider.SHARED_PREPEND, QuoteProvider.START_PREPEND);
            String favoriteList = settings.getString(QuoteProvider.SHARED_FAVORITE_LIST, QuoteProvider.START_FAVORITE);
			boolean noBackground = settings.getBoolean(QuoteProvider.SHARED_NO_BACKGROUND, true);
			int backgoundColor = settings.getInt(QuoteProvider.SHARED_BACKGROUND_COLOR, 0xFF000000);

            toggleButtons(autoMode);
			((EditText)findViewById(R.id.manualQuote)).setText(quote);
			((EditText)findViewById(R.id.manualSpeaker)).setText(speaker);
            ((EditText)findViewById(R.id.prependOption)).setText(prepend);
            ((EditText)findViewById(R.id.favoriteOption)).setText(favoriteList);
			findViewById(R.id.ColorListButton).setBackgroundColor(color);
			((Button)findViewById(R.id.TimeButton)).setText(formatRefreshTime(mRefreshRate));
			((CheckBox)findViewById(R.id.speakerCheck)).setChecked(showSpeaker);
			findViewById(R.id.BackgroundColorButton).setBackgroundColor(backgoundColor);
		//	((CheckBox)findViewById(R.id.oneLineCheck)).setChecked(wrapQuote);

			CheckBox backgroundCheckBox = (CheckBox)findViewById(R.id.backgroundColorCheck);
			backgroundCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					// inverse, if no background true, disable color button
					if (isChecked)
					{
						findViewById(R.id.BackgroundColorButton).setVisibility(View.GONE);
						findViewById(R.id.BackgroundColorText).setVisibility(View.GONE);
					}
					else
					{
						findViewById(R.id.BackgroundColorButton).setVisibility(View.VISIBLE);
						findViewById(R.id.BackgroundColorText).setVisibility(View.VISIBLE);
					}
				}
			});
			backgroundCheckBox.setChecked(noBackground);
			if(noBackground)
			{
				findViewById(R.id.BackgroundColorButton).setVisibility(View.GONE);
				findViewById(R.id.BackgroundColorText).setVisibility(View.GONE);
			}
			else
			{
				findViewById(R.id.BackgroundColorButton).setVisibility(View.VISIBLE);
				findViewById(R.id.BackgroundColorText).setVisibility(View.VISIBLE);
			}
			
			Typeface font;
			try
			{
				font = Typeface.createFromFile(mCurrentFontPath);
			}
			catch (RuntimeException e)
			{
                Log.d(TAG, e.getMessage());

                mCurrentFont = initFont;
                mCurrentFontPath = initFontPath;
                font = Typeface.createFromFile(mCurrentFontPath);
			}
            ((Button) findViewById(R.id.FontListButton)).setTypeface(font);
            ((Button) findViewById(R.id.FontListButton)).setText(mCurrentFont);
			
			Spinner alignmentSpinner = (Spinner)findViewById(R.id.alignmentOptions);
			ArrayAdapter<CharSequence> alignmentAdapter = ArrayAdapter.createFromResource(this, R.array.AlignmentOptions, android.R.layout.simple_spinner_item);
			alignmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			alignmentSpinner.setAdapter(alignmentAdapter);
			alignmentSpinner.setSelection(alignQuote);
			
			Spinner optionsSpinner = (Spinner)findViewById(R.id.cycleOptionsSpinner);
			ArrayAdapter<CharSequence> optionsAdapter = ArrayAdapter.createFromResource(this, R.array.CycleOptions, android.R.layout.simple_spinner_item);
			optionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			optionsSpinner.setAdapter(optionsAdapter);
			optionsSpinner.setSelection(cycleOption);

			refreshListAdapter();
		}
		else
			finish();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_manage:
				startActivityForResult(new Intent(this, QuotePickerActivity.class),98);
				break;
            case R.id.action_import_export:
                startActivityForResult(new Intent(this, ImportExportActivity.class),97);
                break;
            case R.id.action_favorites:
                String favoriteList = ((EditText)findViewById(R.id.favoriteOption)).getText().toString();
                Intent intent = new Intent(this, QuotePickerActivity.class);
                intent.putExtra("list",favoriteList);
                startActivityForResult(intent,96);
        }
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
       
	    return super.onCreateOptionsMenu(menu);
	}
	
	public void onManual(View v)
	{
		boolean checked = ((ToggleButton)v).isChecked();
		toggleButtons(!checked);
	}
	
	public void onAuto(View v)
	{
		boolean checked = ((ToggleButton)v).isChecked();
		toggleButtons(checked);
	}
	
	public void onQuotePicker(View v)
	{
		startActivityForResult(new Intent(this, QuotePickerActivity.class), 99);
	}
	
	public void onAddQuote(View v)
	{
		final SqlHelper sql = new SqlHelper(this);
		sql.createDataBase();
		sql.openDataBase();
		String[] lists = sql.getListsArray();
		
		final QuoteDialog newDialog = new QuoteDialog(this,QuoteDialog.MODE_ADD_NEW);
		newDialog.create("Add a New Quote",new DialogInterface.OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int id) 
			{
				String quote = newDialog.getQuote();
				String speaker = newDialog.getSpeaker();
				String list = newDialog.getList();
				
				if(!quote.isEmpty() && !speaker.isEmpty())
				{
					long quoteId = sql.getQuoteId(quote, speaker);
					if(quoteId == -1)
						quoteId = sql.addQuote(quote, speaker);
					if(!list.isEmpty())
					{
						sql.addQuoteToList((int)quoteId, list);
						refreshListAdapter();
					}

                    ((EditText)findViewById(R.id.manualQuote)).setText(quote);
                    ((EditText)findViewById(R.id.manualSpeaker)).setText(speaker);
				}
			}
		});
		
		String preQuote = ((EditText)findViewById(R.id.manualQuote)).getText().toString();
		String preSpeaker = ((EditText)findViewById(R.id.manualSpeaker)).getText().toString();
		
		newDialog.show(preQuote, preSpeaker, lists);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
        if (requestCode == 99) 
        {
            if (resultCode == RESULT_OK) 
            {
            	Bundle extras = data.getExtras();
            	String quote = extras.getString("Quote");
            	String speaker = extras.getString("Speaker");
            	
            	((EditText)findViewById(R.id.manualQuote)).setText(quote);
				((EditText)findViewById(R.id.manualSpeaker)).setText(speaker);
            }
            refreshListAdapter();
        }
        else
        	refreshListAdapter();
    }
	
	public void onTimePicker(View v)
	{
		String[] timeSplit = mRefreshRate.split("[.]");
		int hour = Integer.parseInt(timeSplit[0]);
		int minute = Integer.parseInt(timeSplit[1]);
		TimePickerDialog picker = new TimePickerDialog(this,new TimePickerDialog.OnTimeSetListener()
		{	
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) 
			{
				mRefreshRate = hourOfDay + "." + minute;
				((Button)findViewById(R.id.TimeButton)).setText(formatRefreshTime(mRefreshRate));
			}
		},hour,minute,true);
		picker.show();
	}
	
	public void onFontPicker(View v)
	{
		final FontPicker picker = new FontPicker(this);
		picker.Create(new DialogInterface.OnClickListener() 
		{
			@Override 
			public void onClick(DialogInterface dialog, int which) 
			{
				mCurrentFont = picker.getFont(which);
				mCurrentFontPath = picker.getFontFile(which);
				
				Typeface font = Typeface.createFromFile(mCurrentFontPath);
				((Button)findViewById(R.id.FontListButton)).setTypeface(font);
				((Button)findViewById(R.id.FontListButton)).setText(mCurrentFont);
			}
		});
		picker.show();
	}
	
	public void onColorPicker(View v)
	{
		ColorDrawable  buttonColor = (ColorDrawable ) findViewById(R.id.ColorListButton).getBackground();
		int prevColor = buttonColor.getColor();
		AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, prevColor, new OnAmbilWarnaListener() 
		{
			@Override
			public void onOk(AmbilWarnaDialog dialog, int color) 
			{
				findViewById(R.id.ColorListButton).setBackgroundColor(color);
			}
			@Override
			public void onCancel(AmbilWarnaDialog dialog)
			{
			}
		});
		dialog.show();
	}

	public void onBackgroundColorPicker(View v)
	{
		ColorDrawable  buttonColor = (ColorDrawable ) findViewById(R.id.BackgroundColorButton).getBackground();
		int prevColor = buttonColor.getColor();
		AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, prevColor, new OnAmbilWarnaListener()
		{
			@Override
			public void onOk(AmbilWarnaDialog dialog, int color)
			{
				findViewById(R.id.BackgroundColorButton).setBackgroundColor(color);
			}
			@Override
			public void onCancel(AmbilWarnaDialog dialog)
			{
			}
		});
		dialog.show();
	}
	
	public void onSaveConfig(View v)
	{
		SharedPreferences settings = getSharedPreferences("Quote" + mAppWidgetId,0);
		SharedPreferences.Editor editor = settings.edit();
		
		ColorDrawable  buttonColor = (ColorDrawable ) findViewById(R.id.ColorListButton).getBackground();
		int color = buttonColor.getColor();
		editor.putInt(QuoteProvider.SHARED_COLOR, color);

		editor.putString(QuoteProvider.SHARED_FONT, mCurrentFont);
		editor.putString(QuoteProvider.SHARED_FONT_FILE, mCurrentFontPath);
		editor.putBoolean(QuoteProvider.SHARED_SHOW_SPEAKER,((CheckBox)findViewById(R.id.speakerCheck)).isChecked());
	//	editor.putBoolean(QuoteProvider.SHARED_WRAP_QUOTE,((CheckBox)findViewById(R.id.oneLineCheck)).isChecked());
		editor.putInt(QuoteProvider.SHARED_ALIGN_QUOTE,((Spinner)findViewById(R.id.alignmentOptions)).getSelectedItemPosition());
        editor.putString(QuoteProvider.SHARED_PREPEND, ((EditText) findViewById(R.id.prependOption)).getText().toString());
        editor.putString(QuoteProvider.SHARED_FAVORITE_LIST,((EditText)findViewById(R.id.favoriteOption)).getText().toString());
		editor.putBoolean(QuoteProvider.SHARED_NO_BACKGROUND, ((CheckBox) findViewById(R.id.backgroundColorCheck)).isChecked());

		ColorDrawable  backgroundColor = (ColorDrawable ) findViewById(R.id.BackgroundColorButton).getBackground();
		color = backgroundColor.getColor();
		editor.putInt(QuoteProvider.SHARED_BACKGROUND_COLOR, color);


        if(((ToggleButton)findViewById(R.id.manualButton)).isChecked())
		{
			String quote = ((EditText)findViewById(R.id.manualQuote)).getText().toString();
			String speaker = ((EditText)findViewById(R.id.manualSpeaker)).getText().toString();

			editor.putString(QuoteProvider.SHARED_MANUAL_QUOTE, quote);
			editor.putString(QuoteProvider.SHARED_MANUAL_SPEAKER, speaker);
			editor.putBoolean(QuoteProvider.SHARED_AUTO_MODE, false);
		}
		else
		{
			QuoteFinder finder = new QuoteFinder(this, mAppWidgetId);
			finder.retrieveQuote();

			TextView listSpinner = (TextView)((Spinner)findViewById(R.id.cycleListSpinner)).getSelectedView();
			String list = listSpinner.getText().toString();
			editor.putString(QuoteProvider.SHARED_REFRESH_RATE, mRefreshRate);
			editor.putBoolean(QuoteProvider.SHARED_AUTO_MODE, true);
			editor.putInt(QuoteProvider.SHARED_CYCLE_OPTION, ((Spinner) findViewById(R.id.cycleOptionsSpinner)).getSelectedItemPosition());
			editor.putString(QuoteProvider.SHARED_CYCLE_LIST, list);
		}
		editor.apply();
		closeConfig();
	}
	
	public void onCancelConfig(View v)
	{
		closeConfig();
	}
	
	private void refreshListAdapter()
	{
		SqlHelper sql;
		sql = new SqlHelper(this);
		sql.createDataBase();
		sql.openDataBase();
		Cursor cursor = sql.getLists();

        SharedPreferences settings = getSharedPreferences("Quote" + mAppWidgetId, 0);
        String cycleList = settings.getString(QuoteProvider.SHARED_CYCLE_LIST, "All");
        int cycleListPos = 0;

        List<String> quoteList = new LinkedList<String>();
        quoteList.add("all");

        while(!cursor.isAfterLast())
        {
            String list = cursor.getString(cursor.getColumnIndexOrThrow("list_name")).toLowerCase();
            if(!quoteList.contains(list))
            {
                quoteList.add(list);
                if(cycleList.compareToIgnoreCase(list) == 0)
                    cycleListPos = quoteList.size() - 1;
            }
            cursor.moveToNext();
        }

		sql.close();

        final String[] lists = quoteList.toArray(new String[quoteList.size()]);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,android.R.id.text1,lists);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		Spinner listSpinner = (Spinner)findViewById(R.id.cycleListSpinner);
		listSpinner.setAdapter(adapter);
        listSpinner.setSelection(cycleListPos);
	}
	
	private void closeConfig()
	{
		if(mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
		{
			AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
			
		//	QuoteProvider.updateQuote(this, widgetManager, mAppWidgetId);
			
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, resultValue);

			QuoteProvider.updateQuote(this, widgetManager, mAppWidgetId);
		}
		finish();
	}
	
	private void toggleButtons(boolean isAuto)
	{
		ToggleButton btManual = (ToggleButton)findViewById(R.id.manualButton);
		ToggleButton btAuto = (ToggleButton)findViewById(R.id.autoButton);
		if(isAuto)
		{
			btAuto.setChecked(isAuto);
			btManual.setChecked(!isAuto);
		}
		else //not auto
		{
			btManual.setChecked(!isAuto);
			btAuto.setChecked(isAuto);
		}
		findViewById(R.id.manualQuoteText).setEnabled(!isAuto);
		findViewById(R.id.manualSpeakerText).setEnabled(!isAuto);
		findViewById(R.id.manualQuote).setEnabled(!isAuto);
		findViewById(R.id.manualSpeaker).setEnabled(!isAuto);
		findViewById(R.id.PickQuoteButton).setEnabled(!isAuto);
		findViewById(R.id.AddQuoteButton).setEnabled(!isAuto);
		findViewById(R.id.TimeButton).setEnabled(isAuto);
		findViewById(R.id.refreshText).setEnabled(isAuto);
		findViewById(R.id.cycleOptionsText).setEnabled(isAuto);
		findViewById(R.id.cycleOptionsSpinner).setEnabled(isAuto);
		findViewById(R.id.cycleListText).setEnabled(isAuto);
		findViewById(R.id.cycleListSpinner).setEnabled(isAuto);
	}
	
	private String formatRefreshTime(String time)
	{
		String refreshButtonText;
		if(mRefreshRate.compareTo("0.0") == 0)
			refreshButtonText = "1 Day";
		else
		{
			String[] timeSplit = time.split("[.]");
			refreshButtonText = timeSplit[0] + "h " + timeSplit[1] + "m";
		}
		return refreshButtonText;
	}
}
