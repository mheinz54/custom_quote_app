package com.heinzdevelopment.customquote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class QuoteAlarmService extends BroadcastReceiver 
{
	private final String TAG = "QUOTE_AlarmService";
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		Bundle bundle = intent.getExtras();
		int appID = bundle.getInt("APP_WIDGET_ID");
		
		Log.d(TAG, "onStartCommand; widgetID: " + appID);
		
		QuoteFinder finder = new QuoteFinder(context, appID);
		finder.retrieveQuote();
	}

}
