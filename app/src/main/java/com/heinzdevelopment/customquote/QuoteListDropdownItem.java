package com.heinzdevelopment.customquote;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.CheckedTextView;

public class QuoteListDropdownItem extends CheckedTextView 
{
	private Paint paint = null;
	private static final int XX = 15;
	private OnDeleteListener onDeleteListener = null;
	
	public QuoteListDropdownItem(Context context) 
	{
		super(context);
		initDraw();
	}
	
	public QuoteListDropdownItem(Context context, AttributeSet attrs)
	{
		super(context,attrs);
		initDraw();
		
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
		        R.styleable.MyCustomWidget,
		        0, 0);
		try 
		{
			final String onDelete = a.getString(R.styleable.MyCustomWidget_onDelete);
			if(onDelete != null)
			{
				setOnDeleteListener(new OnDeleteListener() 
				{	
					private Method mHandler;

					@Override
					public void onListDelete(QuoteListDropdownItem v) 
					{
						if (mHandler == null) 
						{
		                    try 
		                    {
		                        mHandler = getContext().getClass().getMethod(onDelete, QuoteListDropdownItem.class);
		                    }
		                    catch (NoSuchMethodException e) 
		                    {
		                        throw new IllegalStateException();
		                    }
		                }

						try 
						{
		                    mHandler.invoke(getContext(), QuoteListDropdownItem.this);
		                }
						catch (IllegalAccessException e) 
						{
		                    throw new IllegalStateException();
		                }
						catch (InvocationTargetException e)
						{
		                    throw new IllegalStateException();
		                }
					}
				});
			}
		} 
		finally 
		{
			a.recycle();
		}
	}
	
	public QuoteListDropdownItem(Context context, AttributeSet attrs, int defStyle)
	{
		super(context,attrs,defStyle);
		initDraw();
	}
	
	private void initDraw()
	{
		setWillNotDraw(false);
		
		paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeWidth(5);
	}
	
	@Override
	public void onDraw(Canvas canvas)
	{
		if(getText().toString().compareToIgnoreCase("all") != 0)
		{
			int height = canvas.getHeight();
			
			canvas.drawLine(XX, XX, height-XX, height-XX, paint);
			canvas.drawLine(XX, height-XX, height-XX, XX, paint);
		}
		super.onDraw(canvas);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if(event.getX() < 40 && event.getAction() == MotionEvent.ACTION_DOWN)
		{
			if(onDeleteListener != null &&
			   getText().toString().compareToIgnoreCase("all") != 0) 
			{
				onDeleteListener.onListDelete(this);
		    }
			
			return true;
		}
		return false;
	}
	
	// Define our custom Listener interface
	public interface OnDeleteListener 
	{
	    void onListDelete(QuoteListDropdownItem v);
	}
	
	public void setOnDeleteListener(OnDeleteListener listener)
	{
		onDeleteListener = listener;
	}

}
