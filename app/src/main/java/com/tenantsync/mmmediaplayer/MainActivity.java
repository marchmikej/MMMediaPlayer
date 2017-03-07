package com.tenantsync.mmmediaplayer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<MediaFile> mediaFiles;
    SQLiteDatabase myDatabase;
    Context context1;
    RecyclerView rvMedia;
    MediaRecycleViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = this;
        context1= this;
        myDatabase = this.openOrCreateDatabase("MMMedia", MODE_PRIVATE, null);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS media (name VARCHAR , description VARCHAR, filename VARCHAR, filetype INT(3), downloaded INT(3), id INTEGER PRIMARY KEY)");

        mediaFiles = new ArrayList<MediaFile>();

        offLineUpdate();

        // Lookup the recyclerview in activity layout
        rvMedia = (RecyclerView) findViewById(R.id.rvMedia);

        // Initialize contacts
        // Create adapter passing in the sample user data
        adapter = new MediaRecycleViewAdapter(this, mediaFiles);

        // Attach the adapter to the recyclerview to populate items
        rvMedia.setAdapter(adapter);
        // Set layout manager to position the items
        rvMedia.setLayoutManager(new LinearLayoutManager(this));
        // That's all!
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        Intent intent;
        switch (item.getItemId()) {
            case R.id.settings:
                Log.i("Menu Selection", "Settings");
                intent = new Intent(this, SettingPage.class);
                startActivity(intent);
                return true;
            case R.id.login:
                Log.i("Menu Selection", "Login");
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    public void offLineUpdate() {
        Log.i("Offline", "Device cannot connect to the internet");
        String query = "SELECT * FROM media";
        Cursor c = myDatabase.rawQuery(query, null);

        int idIndex = c.getColumnIndex("id");
        int nameIndex = c.getColumnIndex("name");
        int descriptionIndex = c.getColumnIndex("description");
        int filenameIndex = c.getColumnIndex("filename");
        int filetypeIndex = c.getColumnIndex("filetype");
        int downloadedIndex = c.getColumnIndex("downloaded");
        int imagefile = c.getColumnIndex("imagefile");

        //c.moveToFirst();
        Log.i("Database c", Integer.toString(c.getCount()));
        while(c.moveToNext()) {
            Log.i("Database id", Integer.toString(c.getInt(idIndex)));
            Log.i("Database name", c.getString(nameIndex));
            Log.i("Database description", c.getString(descriptionIndex));
            Log.i("Database imagefile", c.getString(imagefile));
            mediaFiles.add(new MediaFile(c.getString(nameIndex), c.getString(descriptionIndex), c.getString(filenameIndex), c.getInt(filetypeIndex), c.getInt(idIndex), c.getInt(downloadedIndex), c.getString(imagefile)));
        }
    }

    public class DownloadFile extends AsyncTask<String , Void, String> {

        public DownloadFile(){}

        protected String doInBackground(String... urls){

            String fileName = urls[1];
            try {
                FileOutputStream output = openFileOutput(fileName, MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
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

        public void test() {
            Log.i("Value", "HI Mike");
        }
    }
}
