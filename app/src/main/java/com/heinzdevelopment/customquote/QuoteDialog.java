package com.heinzdevelopment.customquote;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;

public class QuoteDialog 
{
	public static final int MODE_ADD_NEW 		= 1;
	public static final int MODE_EDIT 			= 2;
	public static final int MODE_ADD_TO_LIST 	= 3;
	
	Context context;
	AlertDialog dialog;
	private int mode;
	
	public QuoteDialog(Context context, int mode)
	{
		this.context = context;
		this.mode = mode;
	}
	
	public void create(String title, DialogInterface.OnClickListener accept)
	{
		LayoutInflater inflater = (LayoutInflater)context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        View view = inflater.inflate(R.layout.add_quote_dialog, null);
        ImageButton clear = (ImageButton)view.findViewById(R.id.clearTextButton);
        clear.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                EditText quote = (EditText)dialog.findViewById(R.id.newQuote);
                quote.setText("");
            }
        });
		
		dialog = new AlertDialog.Builder(context)
			.setView(view)
			.setPositiveButton("Done", accept)
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int id) 
				{
					dialog.cancel();
	            }
			})
			.setTitle(title)
			.create();
	}
	
	public void show(String quote, String speaker, String[] lists) 
	{
		dialog.show();
        EditText quoteView = (EditText)dialog.findViewById(R.id.newQuote);
        EditText speakerView = (EditText)dialog.findViewById(R.id.newSpeaker);

        if(quote != null && speaker != null)
		{
			quoteView.setText(quote);
			speakerView.setText(speaker);
		}

        if(mode == MODE_ADD_TO_LIST)
        {
            quoteView.setVisibility(View.GONE);
            speakerView.setVisibility(View.GONE);

            ImageButton clear = (ImageButton)dialog.findViewById(R.id.clearTextButton);
            clear.setVisibility(View.GONE);
        }
		
		final AutoCompleteTextView addToList = (AutoCompleteTextView)dialog.findViewById(R.id.addToListField);
		if(mode == MODE_EDIT)
		{
			addToList.setVisibility(View.GONE);
		}
		else
		{
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
	                 android.R.layout.simple_dropdown_item_1line, lists);
			addToList.setAdapter(adapter);
			addToList.setOnFocusChangeListener(new View.OnFocusChangeListener() 
			{
				@Override
				public void onFocusChange(View v, boolean hasFocus) 
				{
					if(hasFocus && addToList.getText().length() == 0)
						addToList.showDropDown();
				}
			});
			
			if(mode == MODE_ADD_TO_LIST)
			{
				addToList.setHint(R.string.add_to_list_hint);
			//	addToList.requestFocus();
			}
		}
	}

	public String getQuote()
	{
		EditText quote = (EditText)dialog.findViewById(R.id.newQuote);
		return quote.getText().toString();
	}
	
	public String getSpeaker()
	{
		EditText speaker = (EditText)dialog.findViewById(R.id.newSpeaker);
		return speaker.getText().toString();
	}
	
	public String getList()
	{
		AutoCompleteTextView addToList = (AutoCompleteTextView)dialog.findViewById(R.id.addToListField);
		return addToList.getText().toString();
	}
	
	public void close() 
	{
		dialog.dismiss();
	}
}
