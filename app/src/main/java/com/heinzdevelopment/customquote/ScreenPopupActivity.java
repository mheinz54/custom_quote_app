package com.heinzdevelopment.customquote;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

public class ScreenPopupActivity extends Activity
{
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_screen_popup);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null)
        {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            SharedPreferences settings = getSharedPreferences("Quote" + appWidgetId, 0);
            boolean autoMode = settings.getBoolean(QuoteProvider.SHARED_AUTO_MODE, false);
            if(!autoMode)
                findViewById(R.id.ScreenNext).setVisibility(View.GONE);
        }
        else
            finish();
    }

    public void onScreenEdit(View view)
    {
        Intent launchIntent = new Intent(this, QuoteConfigure.class);
        launchIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        startActivity(launchIntent);
    }

    public void onScreenFavorite(View view)
    {
        SqlHelper sql;
        sql = new SqlHelper(this);
        sql.createDataBase();
        sql.openDataBase();

        QuoteSpeaker quoteSpeaker = getQuoteAndSpeaker();

        SharedPreferences settings = getSharedPreferences("Quote" + appWidgetId,0);
        String favoriteList = settings.getString(QuoteProvider.SHARED_FAVORITE_LIST, QuoteProvider.START_FAVORITE);

        int id = (int)sql.getQuoteId(quoteSpeaker.Quote,quoteSpeaker.Speaker);
        sql.addQuoteToList(id, favoriteList);
        sql.close();

        finish();
    }

    public void onScreenShare(View view)
    {
        QuoteSpeaker quoteSpeaker = getQuoteAndSpeaker();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                "\"" + quoteSpeaker.Quote + "\"\r\n" + quoteSpeaker.Speaker);

        startActivity(Intent.createChooser(shareIntent, "Share Quote with..."));
    }

    public void onScreenNext(View view)
    {
        QuoteFinder finder = new QuoteFinder(this, appWidgetId);
        finder.retrieveQuote();

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        QuoteProvider.updateQuote(this, widgetManager, appWidgetId);
        //doing it twice helps for some reason :(
        QuoteProvider.updateQuote(this, widgetManager, appWidgetId);

        finish();
    }

    private QuoteSpeaker getQuoteAndSpeaker()
    {
        QuoteSpeaker quoteSpeaker = new QuoteSpeaker();
        SharedPreferences settings = getSharedPreferences("Quote" + appWidgetId, 0);
        boolean autoMode = settings.getBoolean(QuoteProvider.SHARED_AUTO_MODE, false);

        if(autoMode)
        {
            quoteSpeaker.Quote = settings.getString(QuoteProvider.SHARED_AUTO_QUOTE, QuoteProvider.START_QUOTE);
            quoteSpeaker.Speaker = settings.getString(QuoteProvider.SHARED_AUTO_SPEAKER,QuoteProvider.START_SPEAKER);
        }
        else
        {
            quoteSpeaker.Quote = settings.getString(QuoteProvider.SHARED_MANUAL_QUOTE, QuoteProvider.START_QUOTE);
            quoteSpeaker.Speaker = settings.getString(QuoteProvider.SHARED_MANUAL_SPEAKER,QuoteProvider.START_SPEAKER);
        }
        return quoteSpeaker;
    }

    private class QuoteSpeaker
    {
        String Quote = "";
        String Speaker = "";
    }
}
