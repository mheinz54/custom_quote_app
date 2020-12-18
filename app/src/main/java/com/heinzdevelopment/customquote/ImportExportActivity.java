package com.heinzdevelopment.customquote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

public class ImportExportActivity extends Activity
{
    private SqlHelper sql;
    private ProgressBar mProgress;
    private TextView mErrorText;
    private TextView mSuccessText;
    private int mProgressStatus = 0;
    private Handler mHandler = new Handler();
    private final String TAG = "IMPORT_EXPORT_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_export);
        setResult(RESULT_CANCELED);

        sql = new SqlHelper(this);
        sql.createDataBase();
        sql.openDataBase();

        setStartText();
        mErrorText = (TextView)findViewById((R.id.errorText));
        mSuccessText = (TextView)findViewById((R.id.successText));
        mProgress = (ProgressBar)findViewById(R.id.progressBar);

        if(!isExternalStorageReadable() || !isExternalStorageWritable())
        {
            findViewById(R.id.importButton).setEnabled(false);
            findViewById(R.id.exportButton).setEnabled(false);

            mErrorText.setText("External storage not available");
            mErrorText.setVisibility(View.VISIBLE);
        }
        else
        {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            path.mkdirs();
        }
    }

    @Override
    protected void onDestroy()
    {
        if(sql != null)
            sql.close();
        super.onDestroy();
    }

    private void resetViews()
    {
        mErrorText.setVisibility(View.INVISIBLE);
        mErrorText.setText("");

        mSuccessText.setVisibility(View.INVISIBLE);
        mSuccessText.setText("");

        mProgress.setVisibility(View.INVISIBLE);
        mProgressStatus = 0;
        mProgress.setProgress(mProgressStatus);
    }

    public void onClickImport(View v)
    {
        resetViews();

        if(!isExternalStorageReadable())
        {
            mErrorText.setText("Cannot find the external storage");
            mErrorText.setVisibility(View.VISIBLE);

            return;
        }

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        if(dir.isDirectory())
        {
            FilenameFilter textFilter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return name.toLowerCase().endsWith(".txt");
                }
            };

            final String[] fileList = dir.list(textFilter);

            if(fileList.length > 0)
            {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        android.R.layout.select_dialog_multichoice, android.R.id.text1, fileList);

                AlertDialog builder = new AlertDialog.Builder(this)
                        .setTitle("Choose a file to import\n(text files in download folder)")
                        .setAdapter(adapter, new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int item)
                            {
                                String fileName = fileList[item];
                                loadFile(fileName);
                            }
                        })
                        .create();
                builder.show();
            }
            else
            {
                mErrorText.setText("No text files found in download folder.\n"
                                + "Please put the file you want to import into the download folder");
                mErrorText.setVisibility(View.VISIBLE);
            }
        }
    }

    public void onClickExport(View v)
    {
        resetViews();
        saveFile();
    }

    public void onHowToImport(View v)
    {
        String explain = "To import a list of quotes, create a text (txt) file with each quote on a new line. ";
        explain += "Each line MUST be in this format\nQuote::Speaker\nthe quote followed by two colons then the speaker. ";
        explain += "That is the absolute minimum each line must have.\n\n";
        explain += "You can also add any number of lists you would like the quote to belong to. ";
        explain += "After Speaker add two colons then the list name\nQuote::Speaker::List  or\nQuote::Speaker::List1::List2\n\n";
        explain += "When you have added all your quotes to the text file, put the file in the Download folder on your device\n";
        explain += "for example: /storage/sdcard/Download\n";
        explain += "The text file can be named anything as long as it ends with .txt\n\n";
        explain += "Warning: Importing a large number of quotes at once may take several minutes";

        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle("How to Import")
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

    public void onHowToExport(View v)
    {
        String explain = "To export all your quotes, just tap the Export button and all your quotes " +
                "will be saved to a text file called CustomQuotes.txt.\n\n" +
                "This file is saved to the Download folder on your device\n" +
                "for example: /storage/sdcard/Download\n\n" +
                "You will also have the option to send the file to another app installed on your device\n" +
                "such as E-Mail, Google Drive, Dropbox, etc";

        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle("How to Export")
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

    private void saveFile()
    {
        mProgress.setVisibility(View.VISIBLE);
        new Thread(new Runnable()
        {
            public void run()
            {
                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "CustomQuotes.txt");

                Cursor cursor = sql.getQuote(QuoteProvider.CYCLE_ALPHA_SPEAKER, "All");

                int total = cursor.getCount();
                int count = 0;
                mProgressStatus = 0;

                try
                {
                    Log.d(TAG, "saveFile: " + file.getAbsolutePath());
                    FileWriter writer = new FileWriter(file);
                    while (!cursor.isAfterLast())
                    {
                        String quote = cursor.getString(cursor.getColumnIndexOrThrow("quote"));
                        String speaker = cursor.getString(cursor.getColumnIndexOrThrow("speaker"));
                        int quoteId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));

                        writer.write(quote + "::" + speaker);

                        Cursor listCursor = sql.getListsFromQuote(quoteId);
                        while (!listCursor.isAfterLast())
                        {
                            String listName = listCursor.getString(listCursor.getColumnIndexOrThrow("list_name"));
                            writer.write("::" + listName);
                            listCursor.moveToNext();
                        }
                        listCursor.close();

                        writer.write("\r\n");
                        cursor.moveToNext();

                        int progress = (++count * 100) / total;
                        if (progress != mProgressStatus)
                        {
                            mProgressStatus = progress;
                            mHandler.post(new Runnable()
                            {
                                public void run()
                                {
                                    mProgress.setProgress(mProgressStatus);
                                }
                            });
                        }
                    }
                    cursor.close();
                    writer.flush();
                    writer.close();

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    shareIntent.setType("text/plain");
                    startActivity(Intent.createChooser(shareIntent, "Send File to..."));

                    mHandler.post(new Runnable()
                    {
                        public void run()
                        {
                            mProgress.setVisibility(View.INVISIBLE);
                            mSuccessText.setText("Export Successful!");
                            mSuccessText.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (IOException e)
                {
                    e.printStackTrace();

                    mHandler.post(new Runnable()
                    {
                        public void run()
                        {
                            mErrorText.setText("Having problems connecting to storage, please ty again.\n "
                                    + "If problem continues, try restarting device.");
                            mErrorText.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }).start();
    }

    private void loadFile(final String fileName)
    {
        mProgress.setVisibility(View.VISIBLE);
        new Thread(new Runnable()
        {
            public void run()
            {
                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), fileName);
                Log.d(TAG, "loadFile: " + file.getAbsolutePath());

                int total = countFileLines(file);
                int count = 0;
                mProgressStatus = 0;

                try
                {
                    String line = null;
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

                    while((line = bufferedReader.readLine()) != null)
                    {
                        String[] parts = line.split("::");
                        if(parts.length >= 2)
                        {
                            String quote = parts[0];
                            String speaker = parts[1];
                            long quoteId = sql.getExactQuoteId(quote, speaker);
                            if(quoteId == -1)
                                quoteId = sql.addQuote(quote, speaker);
                            for(int i = 2; i < parts.length; i++)
                            {
                                String list = parts[i];
                                sql.addQuoteToList((int)quoteId, list);
                            }
                        }

                        int progress = (++count * 100) / total;
                        if(progress != mProgressStatus)
                        {
                            mProgressStatus = progress;
                            mHandler.post(new Runnable()
                            {
                                public void run()
                                {
                                    mProgress.setProgress(mProgressStatus);
                                }
                            });
                        }
                    }
                    bufferedReader.close();

                    mHandler.post(new Runnable()
                    {
                        public void run()
                        {
                            mProgress.setVisibility(View.INVISIBLE);
                            mSuccessText.setText("Import Successful!");
                            mSuccessText.setVisibility(View.VISIBLE);
                        }
                    });
                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();

                    mHandler.post(new Runnable()
                    {
                        public void run()
                        {
                            mErrorText.setText("Having problems connecting to storage, please ty again.\n "
                            + "If problem continues, try restarting device.");
                            mErrorText.setVisibility(View.VISIBLE);
                        }
                    });
                }
                catch (IOException e)
                {
                    e.printStackTrace();

                    mHandler.post(new Runnable()
                    {
                        public void run()
                        {
                            mErrorText.setText("Having problems connecting to storage, please ty again.\n "
                                    + "If problem continues, try restarting device.");
                            mErrorText.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }).start();
    }

    private int countFileLines(File file)
    {
        BufferedReader reader = null;
        int lines = 0;
        try
        {
            reader = new BufferedReader(new FileReader(file));
            while (reader.readLine() != null) lines++;
            reader.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return lines;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable()
    {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_import_export, menu);
        return true;
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void setStartText()
    {
        TextView startText = (TextView)findViewById(R.id.startText);
        String text = "Importing and Exporting your quotes allows you to transfer them to another " +
                "device, add a lot of new quotes at once, or make a backup of your existing quotes. " +
                "Please read \"How to Import\" and \"How to Export\" for an explanation how Importing" +
                " and Exporting works";
        startText.setText(text);
    }
}
