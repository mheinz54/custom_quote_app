package com.heinzdevelopment.customquote;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
 
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
 

public class FontPicker
{
	private List< String >	m_fontPaths;
	private List< String >	m_fontNames;
	AlertDialog dialog;
	final Context fontContext;
 
	// Font adaptor responsible for redrawing the item TextView with the appropriate font.
	// We use BaseAdapter since we need both arrays, and the effort is quite small.
	public class FontAdapter extends BaseAdapter
	{
		@Override
		public int getCount()
		{
			return m_fontNames.size();
		}
 
		@Override
		public Object getItem(int position)
		{
			return m_fontNames.get( position );
		}
 
		@Override
		public long getItemId(int position)
		{
			// We use the position as ID
			return position;
		}
 
		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			View view = convertView;
 
			if ( view == null )
			{
				// Since we're using the system list for the layout, use the system inflater
				final LayoutInflater inflater = (LayoutInflater)fontContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
				view = inflater.inflate( android.R.layout.select_dialog_singlechoice,  parent, false);
			}
 
			if ( view != null )
			{
				// Find the text view from our interface
				CheckedTextView tv = (CheckedTextView) view.findViewById( android.R.id.text1 );
 
				// Replace the string with the current font name using our typeface
				Typeface tface = Typeface.createFromFile( m_fontPaths.get( position ) );
				tv.setTypeface( tface );
				tv.setText( m_fontNames.get( position ) );
			}
 
			return view;
		}
	}
	
	public FontPicker(Context context)
	{
		this.fontContext = context;
	}
	
	public void Create(DialogInterface.OnClickListener listener)
	{
		HashMap< String, String > fonts = FontManager.enumerateFonts();
		m_fontPaths = new ArrayList< String >();
		m_fontNames = new ArrayList< String >();
 
		for ( String path : fonts.keySet() )
		{
			m_fontPaths.add( path );
			m_fontNames.add( fonts.get(path) );
		}

        if(listener != null)
        {
            final FontAdapter adapter = new FontAdapter();
            dialog = new AlertDialog.Builder(fontContext)
                    .setAdapter(adapter, listener)
                    .create();
        }
	}
	
	public void show() 
	{
		dialog.show();
	}
	
	public String getFont(int position)
	{
		return m_fontNames.get( position );
	}
	
	public String getFontFile(int position)
	{
		return m_fontPaths.get( position );
	}

	public AlertDialog getDialog() 
	{
		return dialog;
	}
}