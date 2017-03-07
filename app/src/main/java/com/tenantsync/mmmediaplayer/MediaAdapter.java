package com.tenantsync.mmmediaplayer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.MODE_WORLD_READABLE;
import static android.os.ParcelFileDescriptor.MODE_WORLD_WRITEABLE;

/**
 * Created by michaelmarch on 1/17/17.
 */

public class MediaAdapter extends ArrayAdapter<MediaFile> {
    private final Context context;
    private final MediaFile[] values;
    private String url;
    ImageView downloadView;

    public MediaAdapter(Context context, MediaFile[] values) {
        super(context, R.layout.list_layout_media, values);
        Log.i("Adapter", "In Constructor");
        this.context = context;
        this.values = values;
        url = String.format("http://a7d419d2.ngrok.io");
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Log.i("Adapter", "In getView");
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView;
        // This is how to change a row's lyout.
        rowView = inflater.inflate(R.layout.list_layout_media, parent, false);

        downloadView = (ImageView)rowView.findViewById(R.id.imageDownload);

        if(values[position].downloaded == 0) {
            downloadView.setImageResource(R.drawable.cloud72px);
        } else if(values[position].downloaded == 1) {
            downloadView.setImageResource(R.drawable.garbage72px);
        }
        downloadView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.i("Value","you clicked me");
                new DownloadFile().execute(url + "/audio/" + values[position].filename, values[position].filename, Integer.toString(values[position].id));
                values[position].downloaded = 0;
                notifyDataSetChanged();
            }
        });

        Log.i("Adapter", "About to set values " + rowView.toString());
        TextView title = (TextView)rowView.findViewById(R.id.textTitle);
        title.setText(values[position].name);
        TextView description = (TextView)rowView.findViewById(R.id.textDescription);
        description.setText(values[position].description);
        return rowView;
    }

    public class DownloadFile extends AsyncTask<String , Void, String> {

        public DownloadFile(){}

        protected String doInBackground(String... urls){

            String fileName = urls[1];
            try {
                FileOutputStream output = context.openFileOutput(fileName, MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
                URL url = new URL(urls[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                // download the file
                InputStream input = new BufferedInputStream(url.openStream());

                byte data[] = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1)
                {
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
                String query = "SELECT * FROM media WHERE id = '" + urls[2] + "'";

                SQLiteDatabase myDatabase = context.openOrCreateDatabase("MMMedia", MODE_PRIVATE, null);

                Cursor c = myDatabase.rawQuery(query, null);
                myDatabase.execSQL("UPDATE media SET " +
                        "downloaded = 1" +
                        " WHERE id = " + urls[2]);
            } catch(Exception e)
            {
                e.printStackTrace();
            }
            return urls[1];
        }

        @Override
        protected void onPostExecute(String path)
        {
            Log.i("Downloaded", path);
        }
    }
}
