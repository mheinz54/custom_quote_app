package com.heinzdevelopment.customquote;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class QuotePickerActivity extends Activity implements SearchView.OnQueryTextListener,SearchView.OnCloseListener
{
	private static final String TITLE = "speaker";
    private static final String INFO = "quote";
    
    private SqlHelper sql;
    private boolean m_isTreeDisplay = false;
  //  private int m_selectedQuoteId;
    private Stack<Integer> m_selectedQuoteIds = new Stack<Integer>();
    private String m_selectedList;
    private ActionMode mActionMode = null;
    private Spinner mListSpinner;
    private String m_favoriteList = "";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_quote_picker);
		setResult(RESULT_CANCELED);

        m_selectedList = "all";

        sql = new SqlHelper(this);
        sql.createDataBase();
        sql.openDataBase();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null)
        {
            m_favoriteList = extras.getString("list").toLowerCase();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    if(mListSpinner != null)
                    {
                        m_selectedList = m_favoriteList;
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) mListSpinner.getAdapter();
                        int id = adapter.getPosition(m_selectedList);
                        if (id != -1)
                            mListSpinner.setSelection(id);
                    }
                }
            }, 1000);
        }

        forceBuild();
		
		ActionBar bar = getActionBar();
        if(bar != null)
            bar.setDisplayShowTitleEnabled(false);
	}

	@Override
	protected void onDestroy()
	{
		if(sql != null)
			sql.close();
		super.onDestroy();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		    // Respond to the action bar's Up/Home button
		    case android.R.id.home:
		    	finish();
		        return true;
		    case R.id.action_new_quote:
		    	showNewQuoteDialog();
		    	break;
	    }
		return super.onOptionsItemSelected(item);
	}
	
	private void showNewQuoteDialog() 
	{
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
						mListSpinner.setAdapter(getSpinnerAdapter());
					}
				}
				
				forceBuild();
			}
		});
		newDialog.show(null,null,lists);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
        final Menu finalMenu = menu;
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.quote_picker, finalMenu);
	    
	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    MenuItem searchItem = finalMenu.findItem(R.id.action_search);
	    searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener()
	    {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) 
			{
                MenuItem spinnerItem = finalMenu.findItem(R.id.action_lists);
                spinnerItem.setVisible(false);
				return true;
			}
			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) 
			{
                MenuItem spinnerItem = finalMenu.findItem(R.id.action_lists);
                spinnerItem.setVisible(true);
				forceBuild();
				return true;
			}
	    });
	    
	    SearchView searchView = (SearchView) searchItem.getActionView();
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
	    searchView.setIconifiedByDefault(false);
	    searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        
        
        MenuItem spinnerItem = finalMenu.findItem(R.id.action_lists);
        mListSpinner = (Spinner) spinnerItem.getActionView();
        mListSpinner.setAdapter(getSpinnerAdapter());
        mListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() 
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,int position, long id) 
			{
				m_selectedList = ((TextView)view).getText().toString();
				forceBuild();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) 
			{	
			}
		});
        
	    return super.onCreateOptionsMenu(menu);
	}
	
	public void onListDropdownDelete(QuoteListDropdownItem v)
	{
		final String listName = v.getText().toString();
		
		AlertDialog builder = new AlertDialog.Builder(this)
		    .setPositiveButton("Delete", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    if(listName.compareToIgnoreCase(m_favoriteList) == 0)
                        m_favoriteList = "";
                    sql.removeQuoteList(listName);
                    mListSpinner.setAdapter(getSpinnerAdapter());
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            })
            .setTitle("Are you sure you want to delete " + listName + " list?")
            .create();
		builder.show();
	}
	
	private void submitQuote()
	{
        if(m_selectedQuoteIds.isEmpty())
		{
			Intent result = new Intent();
			result.putExtra("Quote","");
			result.putExtra("Speaker", "");
			setResult(RESULT_OK, result);
		}
		else
		{
			int id = m_selectedQuoteIds.pop();
			Intent result = new Intent();
			result.putExtra("Quote", sql.getQuoteById(id));
			result.putExtra("Speaker", sql.getSpeakerById(id));
			setResult(RESULT_OK, result);
		}
		finish();
	}
	
	private ArrayAdapter<String> getSpinnerAdapter()
	{
		Cursor cursor = sql.getLists();
        List<String> quoteList = new LinkedList<String>();
        quoteList.add("all");
        if(!m_favoriteList.isEmpty())
            quoteList.add(m_favoriteList);
		
        while(!cursor.isAfterLast())
		{
			String list = cursor.getString(cursor.getColumnIndexOrThrow("list_name")).toLowerCase();
            if(!quoteList.contains(list))
                quoteList.add(list);
			cursor.moveToNext();
		}

        final String[] lists = quoteList.toArray(new String[quoteList.size()]);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,android.R.id.text1,lists);
		adapter.setDropDownViewResource(R.layout.quote_list_dropdown_item);
		return adapter;
	}
	
	private void forceBuild()
	{
        m_selectedQuoteIds.removeAllElements();
		if(m_selectedList.compareToIgnoreCase("all") == 0)
		{
			m_isTreeDisplay = false;
			buildTree();
		}
		else
		{
			m_isTreeDisplay = true;
			buildList(null);
		}
	}
	
	private void buildList(String search)
	{
		Cursor cursor;
		if(search != null)
			cursor = sql.searchQuotes(search,m_selectedList);
		else
			cursor = sql.getQuotesFromList(m_selectedList);
		
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(
				this,
				R.layout.quote_list_item,
				cursor,
				new String[] { TITLE, INFO },
				new int[] { android.R.id.text1, android.R.id.text2 },
				0);
		
		ExpandableListView elv = (ExpandableListView)findViewById(R.id.quoteExpandableView);
		elv.setVisibility(View.GONE);
		
		final ListView list = (ListView)findViewById(R.id.quoteListView);
		list.setAdapter(adapter);
		list.setVisibility(View.VISIBLE);
        list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
  //      list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		list.setOnItemClickListener(new OnItemClickListener() 
		{
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) 
			{		
				Cursor cursor = (Cursor)parent.getItemAtPosition(position);
				int quoteId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                if(m_selectedQuoteIds.contains(quoteId))
                {
                    list.setItemChecked(position, false);
                    int index = m_selectedQuoteIds.indexOf(quoteId);
                    m_selectedQuoteIds.removeElementAt(index);
                }
                else
                {
                    list.setItemChecked(position, true);
                    m_selectedQuoteIds.push(quoteId);
                    list.setItemChecked(position, true);
                }
                pickerActionMode();
			}
		});
		
        m_isTreeDisplay = false;
	}
	
	private void buildTree()
	{
		if(!m_isTreeDisplay)
		{
			Cursor cursor = sql.getSpeakers();
			
			MySimpleCursorTreeAdapter adapter = new MySimpleCursorTreeAdapter (
					this, 
					cursor, 
					android.R.layout.simple_expandable_list_item_1, 
					new String[] { TITLE }, 
					new int[] { android.R.id.text1 },
					R.layout.expandable_list_item2,
			        new String[] { INFO },
			        new int[] { android.R.id.text2});
			
			final ExpandableListView elv = (ExpandableListView)findViewById(R.id.quoteExpandableView);
	        elv.setAdapter(adapter);
	        elv.setVisibility(View.VISIBLE);
            elv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        //    elv.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            elv.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener()
            {
                @Override
                public void onGroupCollapse(int groupPosition)
                {
                    MySimpleCursorTreeAdapter myAdapter = (MySimpleCursorTreeAdapter) elv.getExpandableListAdapter();
                    Cursor groupCursor = myAdapter.getGroup(groupPosition);
                    Cursor childCursor = myAdapter.getChildrenCursor(groupCursor);
                    childCursor.moveToFirst();
                    while(!childCursor.isAfterLast())
                    {
                        int quoteId = childCursor.getInt(childCursor.getColumnIndexOrThrow("_id"));
                        if (m_selectedQuoteIds.contains(quoteId))
                        {
                            int i = m_selectedQuoteIds.indexOf(quoteId);
                            m_selectedQuoteIds.removeElementAt(i);
                        }
                        childCursor.moveToNext();
                    }
                    pickerActionMode();
                }
            });
	        elv.setOnChildClickListener(new ExpandableListView.OnChildClickListener()
            {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id)
                {
                    MySimpleCursorTreeAdapter myAdapter = (MySimpleCursorTreeAdapter) parent.getExpandableListAdapter();
                    Cursor groupCursor = myAdapter.getGroup(groupPosition);
                    Cursor childCursor = myAdapter.getChildrenCursor(groupCursor);
                    childCursor.moveToPosition(childPosition);

                    int quoteId = childCursor.getInt(childCursor.getColumnIndexOrThrow("_id"));
                    int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
                    if (m_selectedQuoteIds.contains(quoteId))
                    {
                        parent.setItemChecked(index, false);
                        int i = m_selectedQuoteIds.indexOf(quoteId);
                        m_selectedQuoteIds.removeElementAt(i);
                    }
                    else
                    {
                        parent.setItemChecked(index, true);
                        m_selectedQuoteIds.push(quoteId);
                    }

                    pickerActionMode();

                    return true;
                }
            });
	        		
			
			ListView list = (ListView)findViewById(R.id.quoteListView);
			list.setVisibility(View.GONE);
			
	        m_isTreeDisplay = true;
		}
	}
	
	private void pickerActionMode()
	{
        if(m_selectedQuoteIds.size() == 0 && mActionMode != null)
        {
            mActionMode.finish();
        }
        else if(m_selectedQuoteIds.size() == 1)
        {
            if(mActionMode == null)
            {
                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = startActionMode(mActionModeCallback);
                int doneButtonId = Resources.getSystem().getIdentifier("action_mode_close_button", "id", "android");
                View doneButton = findViewById(doneButtonId);
                doneButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        submitQuote();
                    }
                });
            }
            else
            {
                Menu menu = mActionMode.getMenu();
                menu.findItem(R.id.action_share_quote).setVisible(true);
                menu.findItem(R.id.action_edit_quote).setVisible(true);
                menu.findItem(R.id.action_remove_quote).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                menu.findItem(R.id.action_remove_quote_in_list).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }
        else if(m_selectedQuoteIds.size() > 1)
        {
            Menu menu = mActionMode.getMenu();
            menu.findItem(R.id.action_share_quote).setVisible(false);
            menu.findItem(R.id.action_edit_quote).setVisible(false);
            menu.findItem(R.id.action_remove_quote).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.action_remove_quote_in_list).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
	}
	
	// SimpleCursorTreeAdapter override
	class MySimpleCursorTreeAdapter extends SimpleCursorTreeAdapter
	{
        public MySimpleCursorTreeAdapter(Context context, Cursor cursor,
                        int groupLayout, String[] groupFrom, int[] groupTo,
                        int childLayout, String[] childFrom, int[] childTo) 
        {
                super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childFrom, childTo);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) 
        {
        	String speaker = groupCursor.getString(groupCursor.getColumnIndexOrThrow("speaker"));
        	return sql.getQuotesFromSpeaker(speaker);
        }
	}
	
	// ActionMode.Callback
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() 
	{
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) 
		{
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) 
		{	
			ExpandableListView elv = (ExpandableListView)findViewById(R.id.quoteExpandableView);
			elv.clearChoices();
			elv.requestLayout();

            ListView list = (ListView)findViewById(R.id.quoteListView);
            list.clearChoices();
            list.requestLayout();

            m_selectedQuoteIds.removeAllElements();
			mActionMode = null;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) 
		{
			MenuInflater inflater = mode.getMenuInflater();
	        inflater.inflate(R.menu.picker_action, menu);
	        
	        if(m_selectedList.compareToIgnoreCase("all") != 0)
	        {
	        	menu.findItem(R.id.action_remove_quote).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	        	menu.findItem(R.id.action_remove_quote_in_list).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	        	menu.findItem(R.id.action_remove_quote_in_list).setVisible(true);
	        }
	        else
	        {
	        	menu.findItem(R.id.action_remove_quote_in_list).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	        	menu.findItem(R.id.action_remove_quote_in_list).setVisible(false);
	        	menu.findItem(R.id.action_remove_quote).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	        }	
	        
	        return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) 
		{
			if(m_selectedQuoteIds.isEmpty())
			{
				mActionMode.finish();
				return true;
			}

			switch (item.getItemId()) 
			{
                case R.id.action_share_quote:
                    shareQuote();
                    return true;
            	case R.id.action_remove_quote:
            		removeQuote();
            		return true;
            	case R.id.action_edit_quote:
            		editQuote();
            		return true;
            	case R.id.action_addto_list:
            		addQuoteToList();
            		return true;
            	case R.id.action_remove_quote_in_list:
            		removeFromList();
                    return true;
			}
			return false;
		}
	};

    // should have one 1 id in m_selectedQuoteIds
    private void shareQuote()
    {
        int quoteId = m_selectedQuoteIds.peek();
        String quote = sql.getQuoteById(quoteId);
        String speaker = sql.getSpeakerById(quoteId);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                "\"" + quote + "\"\r\n" + speaker);

        startActivity(Intent.createChooser(shareIntent, "Share Quote with..."));
    }

    // remove all quotes from m_selectedQuoteIds
	private void removeQuote() 
	{
		 AlertDialog builder = new AlertDialog.Builder(this)
		.setPositiveButton("Delete", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int id) 
			{
                while(!m_selectedQuoteIds.isEmpty())
                {
                    int quoteId = m_selectedQuoteIds.pop();
                    sql.deleteQuote(quoteId);
                    sql.removeQuoteFromAllLists(quoteId);
                }
                mActionMode.finish();
				forceBuild();
            }
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int id) 
			{
				dialog.cancel();
            }
		})
		.setTitle("Are you sure you want to delete (" + m_selectedQuoteIds.size() + ") selected?")
		.create();
		 builder.show();
	}

    // should have one 1 id in m_selectedQuoteIds
	private void editQuote()
	{
        final int quoteId = m_selectedQuoteIds.peek();

        final QuoteDialog editDialog = new QuoteDialog(this,QuoteDialog.MODE_EDIT);
		editDialog.create("Edit Quote",new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int id)
			{
				String quote = editDialog.getQuote();
				String speaker = editDialog.getSpeaker();
				sql.editQuote(quoteId, quote, speaker);

				mActionMode.finish();
				forceBuild();
			}
		});

		String preQuote = sql.getQuoteById(quoteId);
		String preSpeaker = sql.getSpeakerById(quoteId);
		editDialog.show(preQuote,preSpeaker,null);
	}

    // all quotes from m_selectedQuoteIds
    private void addQuoteToList()
	{
        String[] lists = sql.getListsArray();
		
		final QuoteDialog editDialog = new QuoteDialog(this,QuoteDialog.MODE_ADD_TO_LIST);
		editDialog.create("Add Quote To List",new DialogInterface.OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int id) 
			{
				String list = editDialog.getList();

				if(!list.isEmpty())
				{
                    while(!m_selectedQuoteIds.isEmpty())
                    {
                        int quoteId = m_selectedQuoteIds.pop();
                        sql.addQuoteToList(quoteId, list);
                    }
					mListSpinner.setAdapter(getSpinnerAdapter());
				}
			}
		});

		editDialog.show(null,null,lists);
	}

    // remove all quotes from m_selectedQuoteIds
    private void removeFromList()
	{
		AlertDialog builder = new AlertDialog.Builder(this)
		.setPositiveButton("Remove", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int id) 
			{
                while(!m_selectedQuoteIds.isEmpty())
                {
                    int quoteId = m_selectedQuoteIds.pop();
                    sql.removeQuoteFromList(quoteId, m_selectedList);
                }
				mActionMode.finish();
				forceBuild();
            }
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int id) 
			{
				dialog.cancel();
            }
		})
		.setTitle("Remove (" + m_selectedQuoteIds.size() + ") quote(s) from list?")
		.create();
		 builder.show();
	}

	@Override
	public boolean onClose()
	{
		forceBuild();
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) 
	{
		buildList("%" + query + "%");
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) 
	{
		if(newText.length() > 3)
			buildList("%" + newText + "%");
		else if(newText.length() == 0)
			forceBuild();
		return false;
	}
}
