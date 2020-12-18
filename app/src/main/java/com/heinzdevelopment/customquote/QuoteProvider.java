package com.heinzdevelopment.customquote;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

public class QuoteProvider extends AppWidgetProvider
{
	public static final String SHARED_MANUAL_QUOTE 		= "SHARED_MANUAL_QUOTE";
	public static final String SHARED_MANUAL_SPEAKER 	= "SHARED_MANUAL_SPEAKER";
	public static final String SHARED_AUTO_QUOTE 		= "SHARED_AUTO_QUOTE";
	public static final String SHARED_AUTO_SPEAKER 		= "SHARED_AUTO_SPEAKER";
	public static final String SHARED_AUTO_MODE 		= "SHARED_AUTO_MODE";
	public static final String SHARED_REFRESH_RATE 		= "SHARED_REFRESH_RATE";
	public static final String SHARED_CYCLE_OPTION 		= "SHARED_CYCLE_OPTION";
	public static final String SHARED_CYCLE_LIST 		= "SHARED_CYCLE_LIST_STRING";
	public static final String SHARED_QUOTE_INDEX 		= "SHARED_QUOTE_INDEX";
	public static final String SHARED_FONT 				= "SHARED_FONT";
	public static final String SHARED_FONT_FILE 		= "SHARED_FONT_FILE";
	public static final String SHARED_COLOR 			= "SHARED_COLOR";
	public static final String SHARED_SHOW_SPEAKER 		= "SHARED_SHOW_SPEAKER";
//	public static final String SHARED_WRAP_QUOTE 		= "SHARED_WRAP_QUOTE";
	public static final String SHARED_ALIGN_QUOTE 		= "SHARED_ALIGN_QUOTE";
    public static final String SHARED_PREPEND 		    = "SHARED_PREPEND";
    public static final String SHARED_FAVORITE_LIST 	= "SHARED_FAVORITE_LIST";
	public static final String SHARED_NO_BACKGROUND 	= "SHARED_NO_BACKGROUND";
	public static final String SHARED_BACKGROUND_COLOR 	= "SHARED_BACKGROUND_COLOR";
	
	public static final String START_QUOTE = "Never pay full price for late pizza";
	public static final String START_SPEAKER = "A Wise Man";
    public static final String START_PREPEND = "--";
    public static final String START_FAVORITE = "favorites";
	
	//cycle options
	public static final int CYCLE_RANDOM = 0;
	public static final int CYCLE_ALPHA_QUOTE = 1;
	public static final int CYCLE_ALPHA_SPEAKER = 2;
	
	//align options
	public static final int ALIGN_LEFT = 0;
	public static final int ALIGN_CENTER = 1;
	public static final int ALIGN_RIGHT = 2;

    public static final int SPEAKER_HEIGHT = 15;
	
	private static final String TAG = "QUOTE_PROVIDER";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
	{	
		Log.d(TAG, "onUpdate");
		for(int i = 0; i < appWidgetIds.length; i++)
		{
			int appID = appWidgetIds[i];
			updateQuote(context,appWidgetManager,appID);
		}

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions)
    {
        updateQuote(context,appWidgetManager,appWidgetId);
    }

    static void updateQuote(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
	{
		Log.d(TAG, "updateQuote; widgetID: " + appWidgetId);
		
		boolean idExists = false;
		ComponentName appName = new ComponentName(context, QuoteProvider.class);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(appName);
		for(int i = 0; i < appWidgetIds.length; i++)
		{
			if(appWidgetIds[i] == appWidgetId)
				idExists = true;
		}
		
		if(!idExists)
		{
			Log.d(TAG, "Id does not exist");
			return;
		}
		
		SharedPreferences settings = context.getSharedPreferences("Quote" + appWidgetId,0);
		boolean autoMode = settings.getBoolean(SHARED_AUTO_MODE, false);
        String prepend = settings.getString(SHARED_PREPEND, START_PREPEND);

		String quote, speaker;
		if(autoMode)
		{
			quote = settings.getString(SHARED_AUTO_QUOTE, START_QUOTE);
			speaker = prepend + settings.getString(SHARED_AUTO_SPEAKER, START_SPEAKER);
			
			String refreshRate = settings.getString(QuoteProvider.SHARED_REFRESH_RATE, "1.0");
			long period;
			if(refreshRate.compareTo("0.0") == 0)
				period = 24 * 3600000;
			else
			{
				String[] timeSplit = refreshRate.split("[.]");
				int hour = Integer.parseInt(timeSplit[0]);
				int minute = Integer.parseInt(timeSplit[1]);
				period = minute * 60000;
            //    period = minute * 10000; //for debugging
				period += hour * 3600000;
			}
			final AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			final Intent intent = new Intent(context, QuoteAlarmService.class);
			intent.putExtra("APP_WIDGET_ID", appWidgetId);
			PendingIntent alarmService = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + period, alarmService);
		}
		else
		{
			quote = settings.getString(SHARED_MANUAL_QUOTE, START_QUOTE);
			speaker = prepend + settings.getString(SHARED_MANUAL_SPEAKER, START_SPEAKER);
		}

        FontPicker picker = new FontPicker(null);
        picker.Create(null);
        String initFontPath = picker.getFontFile(1);

		int color = settings.getInt(SHARED_COLOR, 0xFF000000);
		String fontFile = settings.getString(SHARED_FONT_FILE, initFontPath);
		boolean showSpeaker = settings.getBoolean(SHARED_SHOW_SPEAKER, true);
	//	boolean wrap = settings.getBoolean(SHARED_WRAP_QUOTE, true);
		int align = settings.getInt(SHARED_ALIGN_QUOTE, ALIGN_LEFT);
		boolean noBackground = settings.getBoolean(SHARED_NO_BACKGROUND, true);
		int backgroundColor = 0x00000000;
		if(!noBackground)
			backgroundColor = settings.getInt(SHARED_BACKGROUND_COLOR, 0xFF000000);
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_provider);

        AutofitHelper autoFit = new AutofitHelper(context,appWidgetManager,appWidgetId,showSpeaker);
		views.setImageViewBitmap(R.id.quoteText, autoFit.autofit(quote, align, true, color, fontFile));
		views.setImageViewBitmap(R.id.speaker,
                makeTextBitmap(context, speaker, color, fontFile, SPEAKER_HEIGHT, autoFit, ALIGN_RIGHT));
		views.setViewVisibility(R.id.speaker, showSpeaker ? View.VISIBLE : View.GONE);

        //	views.setInt(R.id.quoteBackground, "setBackgroundColor", backgroundColor);
        views.setInt(R.id.bgcolor, "setColorFilter", backgroundColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            views.setInt(R.id.bgcolor, "setImageAlpha", Color.alpha(backgroundColor));
        else
            views.setInt(R.id.bgcolor, "setAlpha", Color.alpha(backgroundColor));


        //	Intent launchIntent = new Intent(context, QuoteConfigure.class);
        Intent launchIntent = new Intent(context, ScreenPopupActivity.class);
		launchIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		PendingIntent pendingIntent = PendingIntent.getActivity(context,appWidgetId,launchIntent,PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.quoteText, pendingIntent);
		
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	private static Bitmap makeTextBitmap(Context context, String text, int color, String fontFile,
                                         int size, AutofitHelper autoFit, int align)
    {
        int fontSizePX = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.getResources().getDisplayMetrics());
        int layWidth = autoFit.getWidgetSize().width();

        //int layWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, context.getResources().getDisplayMetrics());

        Paint.Align textAlign = Paint.Align.LEFT;
        if (align == QuoteProvider.ALIGN_RIGHT)
            textAlign = Paint.Align.RIGHT;
        else if (align == QuoteProvider.ALIGN_CENTER)
            textAlign = Paint.Align.CENTER;

        int pad = (fontSizePX / 2);
        Paint paint = new Paint();
        Typeface font = Typeface.createFromFile(fontFile);
        paint.setAntiAlias(true);
        paint.setTypeface(font);
        paint.setColor(color);
        paint.setTextSize(fontSizePX * 0.90f);
        paint.setTextAlign(textAlign);

        //  int textWidth = (int) (paint.measureText(quote) + pad * 2);
        int height = (int) (fontSizePX / 0.75);
        Bitmap bitmap = Bitmap.createBitmap(layWidth, fontSizePX, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);

        int k = pad / 2;
        if (align == QuoteProvider.ALIGN_RIGHT)
            k = layWidth - pad / 2;
        else if (align == QuoteProvider.ALIGN_CENTER)
            k = layWidth / 2;
        canvas.drawText(text.trim(), k, fontSizePX * 0.90f - pad/2, paint);

        return bitmap;
    }

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) 
	{
		super.onDeleted(context, appWidgetIds);
	}

	@Override
	public void onDisabled(Context context) 
	{
		super.onDisabled(context);
	}

	@Override
	public void onEnabled(Context context) 
	{
		super.onEnabled(context);
	}

	@Override
	public void onReceive(Context context, Intent intent) 
	{
		super.onReceive(context, intent);
	}
}
