package com.heinzdevelopment.customquote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


public class HowToWidget extends Activity 
{
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_how_to_widget);
		
		((TextView)findViewById(R.id.textExplaination)).setText(getExplanation());
	}

    public void onHowToAdd(View v)
    {
        String explain = "On most Android devices, a widget can be added to the screen by " +
                "a long-press on a blank available space on your home screen (not on an " +
                "icon or the app launcher). Just hold your finger down on the screen for " +
                "a few seconds.\n\n" +
                "Touch the Widgets option from the menu that pops up.\n\n" +
                "Once you find the widget you want to add, hold your finger on it and drag " +
                "it to where you would like it placed.\n\n" +
                "You can move or remove the widget at any time by doing a long-press on it " +
                "again and dragging it to the remove option.";

        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle("How to add a widget to your screen")
                .setMessage(explain)
                .setNegativeButton("Close", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                    }
                })
                .create();
        builder.show();
    }

    public void onNotShowing(View v)
    {
        String explain = "In the case that you cannot find Custom Quote in the list of your " +
                "widgets, go to \"Apps\" in your phone Settings menu. Click on \"Custom Quote\" from " +
                "the list of apps you have downloaded. If there is a button that says \"Move to " +
                "Internal storage\", press it. You should now be able to see Custom Quote in your " +
                "widgets list.";

        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle("Custom Quote not appearing in widget list")
                .setMessage(explain)
                .setNegativeButton("Close", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                    }
                })
                .create();
        builder.show();
    }

    public void onToPlayStore(View v)
    {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try
        {
            startActivity(goToMarket);
        }
        catch (ActivityNotFoundException e)
        {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    public void onSendEmail(View v)
    {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto","heinzdevelopment@gmail.com", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Custom Quote Feedback");
   //     emailIntent.putExtra(Intent.EXTRA_TEXT, "Body");

        startActivity(Intent.createChooser(intent, "Send Email"));
    }

    private String getExplanation()
	{
		return    "Custom Quote is not an app but a widget. A widget can be placed" +
                  " on your home screen or on your lock screen. Android widgets" +
                  " can vastly improve your experience, either by speeding things up" +
                  " via shortcuts or by just looking pretty. This widget's purpose" +
                  " is to allow you to add a little of your own flair to your device!";
	}
}
