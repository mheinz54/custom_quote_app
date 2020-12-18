package com.heinzdevelopment.customquote;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
/*
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.io.BufferedInputStream;
*/
public class QuoteFinder 
{
	private Context appContext;
	private int widgetId;
	private final String TAG = "QUOTE_FINDER";
	private final Object quoteFinderMutex = new Object();
//	private String SEARCH_STRING =  "document.write";
	
	public QuoteFinder(Context appContext, int widgetId)
	{
		this.appContext = appContext;
		this.widgetId = widgetId;
	}
	
	public void retrieveQuote()
	{
		new retrieveQuoteAsync().execute();
	}
	
	private class retrieveQuoteAsync extends AsyncTask<Void, Void, Void>
	{
		private int quoteIndex = 0;
		
		@Override
		protected Void doInBackground(final Void...voids)
		{
			Log.d(TAG, "retrieveQuoteAsync; widgetID: " + widgetId);
			
			synchronized(quoteFinderMutex)
			{
				if(widgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
				{
					String[] quoteSet = getFromSQL();
					if(quoteSet != null)
					{
						SharedPreferences settings = appContext.getSharedPreferences("Quote" + widgetId,0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putString(QuoteProvider.SHARED_AUTO_QUOTE, quoteSet[0]);
						editor.putString(QuoteProvider.SHARED_AUTO_SPEAKER, quoteSet[1]);
						editor.putInt(QuoteProvider.SHARED_QUOTE_INDEX, quoteIndex);
						editor.apply();
					}
					
					AppWidgetManager widgetManager = AppWidgetManager.getInstance(appContext);
					QuoteProvider.updateQuote(appContext, widgetManager, widgetId);
				}	
			}
			
			return null;
		}
		
		private String[] getFromSQL()
		{
			String[] quoteSet = {QuoteProvider.START_QUOTE,QuoteProvider.START_SPEAKER};
			
			try
			{
				SqlHelper sql = new SqlHelper(appContext);
				sql.createDataBase();
				sql.openDataBase();
				
				SharedPreferences settings = appContext.getSharedPreferences("Quote" + widgetId,0);
				int cycleOption = settings.getInt(QuoteProvider.SHARED_CYCLE_OPTION, QuoteProvider.CYCLE_RANDOM);
				String cycleList = settings.getString(QuoteProvider.SHARED_CYCLE_LIST, "All");
				Log.d(TAG, "***********" + "CYCLE OPTION: " + cycleOption);
				
				Cursor cursor = sql.getQuote(cycleOption,cycleList);
				
				quoteIndex = settings.getInt(QuoteProvider.SHARED_QUOTE_INDEX, -1) + 1;
				if(quoteIndex >= cursor.getCount())
					quoteIndex = 0;
				
				Log.d(TAG, "***********" + "QUOTE INDEX: " + quoteIndex);
				
				if(cycleOption != QuoteProvider.CYCLE_RANDOM)
					cursor.moveToPosition(quoteIndex);
				
				quoteSet[0] = cursor.getString(cursor.getColumnIndexOrThrow("quote"));
				quoteSet[1] = cursor.getString(cursor.getColumnIndexOrThrow("speaker"));
				
				Log.d(TAG, "QUOTE: " + quoteSet[0]);
				Log.d(TAG, "SPEAKER: " + quoteSet[1]);		
				
				sql.close();
			}
			catch(Exception e)
			{
				Log.d(TAG, "getFromSQL::error - " + e.getMessage());
			} 
			
			return quoteSet;
		}
/*		
		private String[] getFromZip()
		{
			String[] quoteSet = {QuoteProvider.START_QUOTE,QuoteProvider.START_SPEAKER};
			
			try
			{
				InputStream is = appContext.getResources().getAssets().open("quotes.txt");
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				
				String line;
				List<String> quotes = new ArrayList<String>();
				while((line = reader.readLine()) != null)
					quotes.add(line);
				
				SharedPreferences settings = appContext.getSharedPreferences("Quote" + widgetId,Context.MODE_MULTI_PROCESS);
				int cycleOption = settings.getInt(QuoteProvider.SHARED_CYCLE_OPTION, QuoteProvider.CYCLE_RANDOM);
				Log.d(TAG, "***********" + "CYCLE OPTION: " + cycleOption);
				
				if(cycleOption == QuoteProvider.CYCLE_RANDOM)
					quoteIndex = (int)(Math.random()*quotes.size());
				else
					quoteIndex = settings.getInt(QuoteProvider.SHARED_QUOTE_INDEX, -1) + 1;
				
				if(quoteIndex >= quotes.size())
					quoteIndex = 0;
				Log.d(TAG, "***********" + "QUOTE INDEX: " + quoteIndex);
				
				if(cycleOption == QuoteProvider.CYCLE_ALPHA_QUOTE)
				{
					Collections.sort(quotes.subList(0, quotes.size()), new Comparator<String>() 
					{
			            @Override
			            public int compare(String st1, String st2) 
			            {
			            	if(st1.charAt(0) == '\'')
			            		st1 = st1.substring(1);
			            	if(st2.charAt(0) == '\'')
			            		st2 = st2.substring(1);

			                return st1.compareToIgnoreCase(st2);
			            }
			        });
				}
				
				line = quotes.get(quoteIndex);
				int index = line.lastIndexOf('@');
				quoteSet[0] = line.substring(0, index);
				quoteSet[1] = line.substring(index+1);
				
				Log.d(TAG, "QUOTE: " + quoteSet[0]);
				Log.d(TAG, "SPEAKER: " + quoteSet[1]);

				reader.close();
			}
			catch(Exception e)
			{
				Log.d(TAG, "getFromZip::open file error - " + e.getMessage());
			}
			return quoteSet;
		}
	
*/	
	}
/*	
	private String[] getFromNetwork()
	{
		String[] quoteSet = null;
		if(hasNetworkConnection())
		{
			try 
			{
				URL url = new URL("http://www.quotedb.com/quote/quote.php?action=random_quote&=&=&");
				HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				quoteSet = readStream(in);
				urlConnection.disconnect();
			} 
			catch (IOException e) 
			{
				Log.d(TAG, "getFromNetwork::exception - " + e.getMessage());
			}
		}
		else
		{
			Log.d(TAG, "getFromNetwork::no internet access!");
		}
		return quoteSet;
	}

	private String[] readStream(InputStream in) throws IOException 
	{
		String[] quoteSet = {QuoteProvider.START_QUOTE,QuoteProvider.START_SPEAKER};
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		while((line = reader.readLine()) != null)
		{
			int index = -1;
			if((index = line.indexOf(SEARCH_STRING)) > -1)
			{
				int endIndex = line.indexOf(')');
				if(endIndex == -1)
				{
					line += reader.readLine();
					endIndex = line.indexOf(')');
				}
				if(endIndex < index + SEARCH_STRING.length())
					continue;
				
				String quote = line.substring(index + SEARCH_STRING.length(), endIndex);
				boolean isQuote = !quote.contains("More quotes from");
				quote = trimUnwanted(quote);
				Log.d(TAG, "QUOTE: " + quote);
				if(isQuote)
					quoteSet[0] = quote;
				else
					quoteSet[1] = quote;
				
				String speaker = "";
				line = line.substring(endIndex + 1);
				index = line.indexOf(SEARCH_STRING);
				if(index > 0)
				{
					endIndex = line.indexOf(')');
					if(endIndex == -1)
					{
						line += reader.readLine();
						endIndex = line.indexOf(')');
					}
					if(endIndex > index + SEARCH_STRING.length())
					{
						speaker = line.substring(index + SEARCH_STRING.length(), endIndex);
						speaker = trimUnwanted(speaker);
						Log.d(TAG, "SPEAKER: " + speaker);
						if(isQuote)
							quoteSet[1] = speaker;
						else
							quoteSet[0] = speaker;
					}
				}
				if(speaker.length() < 1)
				{
					if(isQuote)
						quoteSet[1] = "Unknown";
					else
						quoteSet[0] = "Unknown";
				}
			}
			reader.close();
		}
		
		return quoteSet;
	}

	private String trimUnwanted(String line) 
	{
		line = line.replaceAll("[\'();]", "");
		
		int index = -1;
		while((index = line.indexOf('<')) > -1)
		{
			int endIndex = line.indexOf('>') + 1;
			line = line.substring(0, index) + line.substring(endIndex,line.length());
		}
		if(line.contains("More quotes from"))
		{
			String moreText = "More quotes from";
			int moreIndex = line.indexOf(moreText);
			line = line.substring(moreIndex + moreText.length() + 1);
		}
		return line;
	}
	
	private boolean hasNetworkConnection()
	{
		ConnectivityManager connectivityManager = (ConnectivityManager)appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnected();
	}
*/
}
