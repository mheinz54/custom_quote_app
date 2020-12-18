package com.heinzdevelopment.customquote;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

public class SqlHelper extends SQLiteOpenHelper 
{
	private static String DB_NAME = "quote_data.db";
	private static int DB_VERSION = 1;
	
	private static String QUOTE_TABLE_NAME = "quotes";
	private static String USER_LIST_TABLE_NAME = "user_lists";
	
	private final Context m_context;
	private final String m_databasePath;
	private SQLiteDatabase m_dataBase; 
	
	public SqlHelper(Context context) 
	{
		super(context, DB_NAME, null, DB_VERSION);
		m_context = context;
		m_databasePath = context.getApplicationInfo().dataDir + "/databases/";
	}
	
	public void createDataBase()
	{
    	if(!checkDataBase())
    	{
    		//By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
        	this.getReadableDatabase();
    		try 
    		{
				copyDataBase();
			} 
    		catch (IOException e) 
    		{
				e.printStackTrace();
			}
    	}
    }
	
	public void openDataBase() throws SQLException
	{
    	//Open the database
        String myPath = m_databasePath + DB_NAME;
        m_dataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
    }
	
	public long getQuoteId(String quote, String speaker)
	{
		long id = -1;
		String[] args = { quote,speaker };
		Cursor cursor = m_dataBase.query(
				QUOTE_TABLE_NAME,
				null,
				"quote LIKE ? AND speaker LIKE ?",
				args,
				null,
				null,
				"speaker, quote");
		cursor.moveToFirst();
		
		if(cursor.getCount() > 0)
			id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
        cursor.close();
		return id;
	}

    public long getExactQuoteId(String quote, String speaker)
    {
        long id = -1;
        String[] args = { quote,speaker };
        Cursor cursor = m_dataBase.query(
                QUOTE_TABLE_NAME,
                null,
                "quote = ? AND speaker = ?",
                args,
                null,
                null,
                "speaker, quote");
        cursor.moveToFirst();

        if(cursor.getCount() > 0)
            id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
        cursor.close();
        return id;
    }
	
	public long addQuote(String quote, String speaker)
	{
		ContentValues values = new ContentValues();
		values.put("quote", quote);
		values.put("speaker", speaker);
		return m_dataBase.insert(QUOTE_TABLE_NAME, null, values);
	}
	
	public void deleteQuote(int quoteId)
	{
		String[] args = { String.valueOf(quoteId) };
		m_dataBase.delete(QUOTE_TABLE_NAME, "_id LIKE ?", args);
	}
	
	public void editQuote(int quoteId,String quote, String speaker)
	{
		ContentValues values = new ContentValues();
		values.put("quote", quote);
		values.put("speaker", speaker);
		String[] args = { String.valueOf(quoteId) };
		m_dataBase.update(QUOTE_TABLE_NAME, values, "_id LIKE ?", args);
	}
	
	public Cursor getQuote(int cycleOption,String cycleList)
	{
		Cursor cursor;
		if(cycleList.compareToIgnoreCase("All") == 0)
		{
			if(cycleOption == QuoteProvider.CYCLE_RANDOM)
				cursor = m_dataBase.rawQuery("SELECT * FROM " + QUOTE_TABLE_NAME + " ORDER BY RANDOM() LIMIT 1;", null);
			else if(cycleOption == QuoteProvider.CYCLE_ALPHA_QUOTE)
				cursor = m_dataBase.rawQuery("SELECT * FROM " + QUOTE_TABLE_NAME + " ORDER BY quote;", null);
			else
				cursor = m_dataBase.rawQuery("SELECT * FROM " + QUOTE_TABLE_NAME + " ORDER BY speaker,quote;", null);
		}
		else
		{
			SQLiteQueryBuilder query = new SQLiteQueryBuilder();
			query.setTables(QUOTE_TABLE_NAME + " INNER JOIN " + USER_LIST_TABLE_NAME + 
				 	 " ON (" + QUOTE_TABLE_NAME + "._id = " + USER_LIST_TABLE_NAME + ".q_index)");
			query.appendWhere(USER_LIST_TABLE_NAME + ".list_name LIKE \"" + cycleList + "\"");
			String qString;
			if(cycleOption == QuoteProvider.CYCLE_RANDOM)
				qString = query.buildQuery(null,null,null, null, "RANDOM()", "1");
			else if(cycleOption == QuoteProvider.CYCLE_ALPHA_QUOTE)
				qString = query.buildQuery(null,null,null, null, "quote", null);
			else
				qString = query.buildQuery(null,null,null, null, "speaker,quote", null);
			cursor = m_dataBase.rawQuery(qString,null);
		}
		
		cursor.moveToFirst();
		return cursor;
	}
	
	public Cursor getSpeakers()
	{
		String qString = "SELECT * FROM " + QUOTE_TABLE_NAME + " GROUP BY speaker;";
		Cursor cursor = m_dataBase.rawQuery(qString,null);
		cursor.moveToFirst();
		return cursor;
	}
	
	public Cursor getQuotesFromSpeaker(String speaker)
	{
		String[] args = { speaker };
		Cursor cursor = m_dataBase.query(
				QUOTE_TABLE_NAME,
				null,
				"speaker = ?",
				args,
				null,
				null,
				"quote");
		cursor.moveToFirst();
		return cursor;
	}
	
	public Cursor searchQuotes(String search, String list)
	{
		Cursor cursor;
		if(list.compareToIgnoreCase("all") != 0)
		{
			SQLiteQueryBuilder query = new SQLiteQueryBuilder();
			query.setTables(QUOTE_TABLE_NAME + " INNER JOIN " + USER_LIST_TABLE_NAME + 
				 	 " ON (" + QUOTE_TABLE_NAME + "._id = " + USER_LIST_TABLE_NAME + ".q_index)");
			query.appendWhere("(" + QUOTE_TABLE_NAME + ".quote LIKE \"" + search + "\" OR " + QUOTE_TABLE_NAME + ".speaker LIKE \"" + search + "\")");
			query.appendWhere(" AND " + USER_LIST_TABLE_NAME + ".list_name LIKE \"" + list + "\"");
			String qString = query.buildQuery(null,null, "quote", null, "speaker, quote", null);
			cursor = m_dataBase.rawQuery(qString, null);
		}
		else
		{
			String[] args = { search,search };
			cursor = m_dataBase.query(
					QUOTE_TABLE_NAME,
					null,
					"quote LIKE ? OR speaker LIKE ?",
					args,
					null,
					null,
					"speaker, quote");
		}
		
		cursor.moveToFirst();
		return cursor;
	}
	
	public String getQuoteById(int id)
	{
		String sql = "SELECT * FROM " + QUOTE_TABLE_NAME + 
				 	 " WHERE _id = " + id + ";";
		Cursor cursor = m_dataBase.rawQuery(sql, null);
		cursor.moveToFirst();
        String quote = cursor.getString(cursor.getColumnIndexOrThrow("quote"));
        cursor.close();
		return quote;
	}
	
	public String getSpeakerById(int id)
	{
		String sql = "SELECT * FROM " + QUOTE_TABLE_NAME + 
				 	 " WHERE _id = " + id + ";";
		Cursor cursor = m_dataBase.rawQuery(sql, null);
		cursor.moveToFirst();
        String speaker = cursor.getString(cursor.getColumnIndexOrThrow("speaker"));
        cursor.close();
		return speaker;
	}
	
	public Cursor getQuotesFromList(String listName)
	{
		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(QUOTE_TABLE_NAME + " INNER JOIN " + USER_LIST_TABLE_NAME + 
			 	 " ON (" + QUOTE_TABLE_NAME + "._id = " + USER_LIST_TABLE_NAME + ".q_index)");
		String qString = query.buildQuery(null, USER_LIST_TABLE_NAME + ".list_name LIKE \"" + listName + "\"", 
										  "quote", null, "speaker,quote", null);
		
		Cursor cursor = m_dataBase.rawQuery(qString, null);
		cursor.moveToFirst();
		return cursor;
	}

    public Cursor getListsFromQuote(int quoteId)
    {
        String sql = "SELECT list_name FROM " + USER_LIST_TABLE_NAME +
                " WHERE q_index = " + quoteId + ";";
        Cursor cursor = m_dataBase.rawQuery(sql, null);
        cursor.moveToFirst();
        return cursor;
    }
	
	public long addQuoteToList(int quoteId, String list)
	{
		ContentValues values = new ContentValues();
		values.put("list_name", list);
		values.put("q_index", quoteId);
		return m_dataBase.insert(USER_LIST_TABLE_NAME, null, values);
	}
	
	public void removeQuoteFromList(int quoteId, String list)
	{
		String[] args = { list, String.valueOf(quoteId) };
		m_dataBase.delete(USER_LIST_TABLE_NAME, "list_name Like ? AND q_index = ?", args);
	}
	
	public void removeQuoteFromAllLists(int quoteId)
	{
		String[] args = { String.valueOf(quoteId) };
		m_dataBase.delete(USER_LIST_TABLE_NAME, "q_index = ?", args);
	}
	
	public void removeQuoteList(String list)
	{
		String[] args = { list };
		m_dataBase.delete(USER_LIST_TABLE_NAME, "list_name LIKE ?", args);
	}
	
	public Cursor getLists()
	{
		Cursor cursor = m_dataBase.rawQuery("SELECT * FROM " + USER_LIST_TABLE_NAME + " GROUP BY list_name;", null);
		cursor.moveToFirst();
		return cursor;
	}
	
	public String[] getListsArray()
	{
		Cursor cursor = getLists();
		List<String> quoteList = new LinkedList<String>();

		while(!cursor.isAfterLast())
		{
			String list = cursor.getString(cursor.getColumnIndexOrThrow("list_name")).toLowerCase();
			if (!quoteList.contains(list))
			{
				quoteList.add(list);
			}
			cursor.moveToNext();
		}

		return quoteList.toArray(new String[quoteList.size()]);
	}
	
	private boolean checkDataBase()
	{
    	SQLiteDatabase checkDB = null;
 
    	try
    	{
    		String myPath = m_databasePath + DB_NAME;
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    	}
    	catch(SQLiteException e)
    	{
            e.printStackTrace();
    	}
 
    	if(checkDB != null)
    	{
    		checkDB.close();
    	}
 
    	return checkDB != null;
    }
	
	private void copyDataBase() throws IOException
	{ 
    	//Open your local db as the input stream
    	InputStream myInput = m_context.getAssets().open(DB_NAME);
 
    	// Path to the just created empty db
    	String outFileName = m_databasePath + DB_NAME;
    	OutputStream myOutput = new FileOutputStream(outFileName);
 
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = myInput.read(buffer))>0)
    	{
    		myOutput.write(buffer, 0, length);
    	}
 
    	//Close the streams
    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
    }

	@Override
	public synchronized void close() 
	{
		if(m_dataBase != null)
			m_dataBase.close();
		 
		super.close();
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) 
	{
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
	}
}
