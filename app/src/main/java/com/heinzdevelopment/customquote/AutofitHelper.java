package com.heinzdevelopment.customquote;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class AutofitHelper
{
    private static final String TAG = "AutoFitTextHelper";

    // Minimum size of the text in pixels
    private static final int DEFAULT_MIN_TEXT_SIZE = 15; //sp
    // How precise we want to be when reaching the target textWidth size
    private static final float DEFAULT_PRECISION = 0.5f;

    private Context context;
    private int mWidgetWidth = 0;
    private int mWidgetHeight = 0;

    private AppWidgetManager mAppWidgetManager;
    private int mAppWidgetId;

    public AutofitHelper(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean showSpeaker)
    {
        this.context = context;
        this.mAppWidgetId = appWidgetId;
        this.mAppWidgetManager = appWidgetManager;
        adjustWidgetSize(showSpeaker);
    }

    public Rect getWidgetSize()
    {
        int widgetWidth = 0;
        int widgetHeight = 0;
        boolean port = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        AppWidgetProviderInfo providerInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(mAppWidgetId);
        widgetWidth = providerInfo.minWidth;
        widgetHeight = providerInfo.minHeight;

        Bundle mAppWidgetOptions = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            mAppWidgetOptions = mAppWidgetManager.getAppWidgetOptions(mAppWidgetId);//= appWidgetManager.getAppWidgetOptions(appWidgetId);
        if(mAppWidgetOptions != null)
        {
            if (port)
            {
                widgetWidth = mAppWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                widgetHeight = mAppWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            }
            else
            {
                widgetWidth = mAppWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
                widgetHeight = mAppWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            }
        }
        int margin = (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8,
                context.getResources().getDisplayMetrics());
      //  margin = 0;
        widgetWidth = (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                widgetWidth,
                context.getResources().getDisplayMetrics()) - margin;
        widgetHeight = (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                widgetHeight,
                context.getResources().getDisplayMetrics()) - margin;

        if(widgetWidth <= 0)
            widgetWidth = 200;
        if(widgetHeight <= 0)
            widgetHeight = 100;

        return new Rect(0, 0, widgetWidth, widgetHeight);
    }

    private void adjustWidgetSize(boolean showSpeaker)
    {
        Rect widgetRect = getWidgetSize();
        mWidgetHeight = widgetRect.height();
        mWidgetWidth = widgetRect.width();

        if(showSpeaker)
        {
            //int speaker = 25;
            int speaker = (int)TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    QuoteProvider.SPEAKER_HEIGHT,
                    context.getResources().getDisplayMetrics());
          //  speaker = (int) (speaker / 0.75);
            mWidgetHeight -= speaker;
        }

        if(mWidgetWidth <= 0)
            mWidgetWidth = 200;
        if(mWidgetHeight <= 0)
            mWidgetHeight = 100;
    }

    /**
     * Re-sizes the textSize of the TextView so that the text fits within the bounds of the View.
     */
    public Bitmap autofit(String text, int align, boolean wrap, int color, String fontFile)
    {
        int maxLines = 1;
        if(wrap)
        {
            maxLines = mWidgetHeight * 4 / 100;
            if(maxLines > 10)
                maxLines = 16;
            if(maxLines <= 0)
                maxLines = 1;
        }

        Resources r = Resources.getSystem();
        if (context != null)
        {
            r = context.getResources();
        }

        DisplayMetrics displayMetrics = r.getDisplayMetrics();

      /*  int padding = mWidgetHeight * 5 / 100;
        if(mWidgetHeight / 100 == 1)
            padding += 8;*/

        int padding = (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                displayMetrics);

        int targetWidth = mWidgetWidth - padding; //maybe check padding


        float size = mWidgetHeight;//mWidgetHeight - padding;
        float high = size;
        float low = DEFAULT_MIN_TEXT_SIZE;

        TextPaint paint = new TextPaint();
        Typeface font = Typeface.createFromFile(fontFile);
        paint.setAntiAlias(true);
        paint.setTypeface(font);
        paint.setColor(color);
        paint.setTextSize(size);

        if(maxLines == 1)
        {
            size = getAutofitTextSize(text, paint, targetWidth, maxLines, low, high, DEFAULT_PRECISION,
                    displayMetrics,0);
        }
        else
        {
            size = getBestFitForMultiLines(text, paint, targetWidth, maxLines, low, high, DEFAULT_PRECISION,
                    displayMetrics);
        }

        if (size < DEFAULT_MIN_TEXT_SIZE)
        {
            size = DEFAULT_MIN_TEXT_SIZE;
        }

        float newSize = size * 0.90f;
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, newSize, displayMetrics));
        //paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, size, displayMetrics));
        StaticLayout layout = new StaticLayout(text, paint, targetWidth, Layout.Alignment.ALIGN_NORMAL,
                1.0f, 0.0f, true);

        Paint.Align textAlign = Paint.Align.LEFT;
        if(align == QuoteProvider.ALIGN_RIGHT)
            textAlign = Paint.Align.RIGHT;
        else if(align == QuoteProvider.ALIGN_CENTER)
            textAlign = Paint.Align.CENTER;

        paint.setTextAlign(textAlign);
     //   int height = (int) (mWidgetHeight / 0.95);
        Bitmap bitmap = Bitmap.createBitmap(mWidgetWidth, mWidgetHeight, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        for(int i = 0; i < layout.getLineCount(); i++)
        {
            String s = text.substring(layout.getLineStart(i),layout.getLineEnd(i));
            int k = padding/2 + padding/4;
            if(align == QuoteProvider.ALIGN_RIGHT)
                k = targetWidth + padding/2 + padding/4;
            else if(align == QuoteProvider.ALIGN_CENTER)
                k = targetWidth / 2 + padding/2 + padding/4;
            int l = (int)(newSize + i * newSize);// - padding/2);
            canvas.drawText(s,k,l,paint);
        }

        return bitmap;
    }

    private float getBestFitForMultiLines(CharSequence text, TextPaint paint,
                                          float targetWidth, int maxLines, float low, float high, float precision,
                                          DisplayMetrics displayMetrics)
    {
        float maxSize = DEFAULT_MIN_TEXT_SIZE;
        StaticLayout layout = null;

        for(int i = 1; i <= maxLines; ++i)
        {
            float size = getAutofitTextSize(text, paint, targetWidth, i, low, high,
                    DEFAULT_PRECISION, displayMetrics,0);

            paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, size, displayMetrics));
            layout = new StaticLayout(text, paint, (int)targetWidth, Layout.Alignment.ALIGN_NORMAL,
                    1.0f, 0.0f, true);
            int lines = layout.getLineCount();

            int count = 2;
            while((lines * size) > mWidgetHeight && count++ <= 5)
            {
                layout = null;

                size = getAutofitTextSize(text, paint, targetWidth, i, low, (high / count),
                        DEFAULT_PRECISION, displayMetrics,0);

                paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, size, displayMetrics));
                layout = new StaticLayout(text, paint, (int) targetWidth, Layout.Alignment.ALIGN_NORMAL,
                        1.0f, 0.0f, true);
                lines = layout.getLineCount();
            }
            if((lines * size) < mWidgetHeight && size > maxSize)
                maxSize = size;

            layout = null;
        }

        return maxSize;
    }

    /**
     * Recursive binary search to find the best size for the text.
     */
    private float getAutofitTextSize(CharSequence text, TextPaint paint,
                                            float targetWidth, int maxLines, float low, float high,
                                            float precision, DisplayMetrics displayMetrics, int itter)
    {
        float mid = (low + high) / 2.0f;
        if(itter > 10)
        {
            low = high;
            return mid;
        }

        int lineCount = 1;
        StaticLayout layout = null;

        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, mid,
                displayMetrics));

        if (maxLines != 1)
        {
            layout = new StaticLayout(text, paint, (int)targetWidth, Layout.Alignment.ALIGN_NORMAL,
                    1.0f, 0.0f, true);
            lineCount = layout.getLineCount();
        }

        if (lineCount > maxLines)
        {
            // For the case that `text` has more newline characters than `maxLines`.
            if ((high - low) < precision)
            {
                return low;
            }
            return getAutofitTextSize(text, paint, targetWidth, maxLines, low, mid, precision, displayMetrics,++itter);
        }
        else if (lineCount < maxLines && low != high)
        {
            return getAutofitTextSize(text, paint, targetWidth, maxLines, mid, high, precision, displayMetrics,++itter);
        }
        else
        {
            float maxLineWidth = 0;
            if (maxLines == 1)
            {
                maxLineWidth = paint.measureText(text, 0, text.length());
            }
            else
            {
                for (int i = 0; i < lineCount; i++)
                {
                    if (layout.getLineWidth(i) > maxLineWidth)
                    {
                        maxLineWidth = layout.getLineWidth(i);
                    }
                }
            }

            if ((high - low) < precision)
            {
                return low;
            }
            else if (maxLineWidth > targetWidth)
            {
                return getAutofitTextSize(text, paint, targetWidth, maxLines, low, mid, precision, displayMetrics,++itter);
            }
            else if (maxLineWidth < targetWidth)
            {
                return getAutofitTextSize(text, paint, targetWidth, maxLines, mid, high, precision, displayMetrics,++itter);
            }
            else
            {
                return mid;
            }
        }
    }

    private int getLineCount(CharSequence text, TextPaint paint, float size, float width, DisplayMetrics displayMetrics)
    {
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, size, displayMetrics));
        StaticLayout layout = new StaticLayout(text, paint, (int)width,Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        return layout.getLineCount();
    }
}
