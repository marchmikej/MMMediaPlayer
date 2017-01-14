package com.tenantsync.mmmediaplayer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<MediaFile> mediaFiles;
    ListView listView;
    ArrayAdapter<String> arrayAdapter;
    SQLiteDatabase myDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = this;
        myDatabase = this.openOrCreateDatabase("MMMedia", MODE_PRIVATE, null);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS media (name VARCHAR , description VARCHAR, filename VARCHAR, filetype INT(3), downloaded INT(3), id INTEGER PRIMARY KEY)");
        listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("Value",Integer.toString(position));
                if(mediaFiles.size() > position) {
                    Log.i("Value",mediaFiles.get(position).name);
                    Intent intent = new Intent(context, AudioPlayer.class);
                    intent.putExtra("id", Integer.toString(mediaFiles.get(position).id));
                    intent.putExtra("name", mediaFiles.get(position).name);
                    intent.putExtra("description", mediaFiles.get(position).description);
                    intent.putExtra("filename", mediaFiles.get(position).filename);
                    intent.putExtra("fileType", Integer.toString(mediaFiles.get(position).fileType));
                    intent.putExtra("downloaded", Integer.toString(mediaFiles.get(position).downloaded));
                    startActivity(intent);
                }
            }
        });

        String url = String.format("http://6aa3b6b9.ngrok.io/androidtest");

        mediaFiles = new ArrayList<MediaFile>();

        new GetJSONNetwork().execute(url);
    }

    private class GetJSONNetwork extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String jsonString = "Error";
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder builder = new StringBuilder();

                String inputString;
                while ((inputString = bufferedReader.readLine()) != null) {
                    builder.append(inputString);
                }

                jsonString = builder.toString();

                urlConnection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                jsonString = "Error";
            }
            return jsonString;
        }

        @Override
        protected void onPostExecute(String temp) {
            Log.i("Value", temp);
            if(temp == "Error") {
                Context context = getApplicationContext();
                CharSequence text = "You are offline";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                offLineUpdate();
            } else {
                parseMediaJSON(temp);
            }
        }
    }

    public void parseMediaJSON(String incomingJSON)
    {
        try {
            JSONArray json = new JSONArray(incomingJSON);
            ArrayList<String> tempList = new ArrayList<String>();
            for(int i=0;i<json.length();i++) {
                JSONObject jsonMessageObject = json.getJSONObject(i);
                String query = "SELECT * FROM media WHERE id = '" + jsonMessageObject.getInt("id") + "'";
                Cursor c = myDatabase.rawQuery(query, null);
                Log.i("Database", "1");
                int idIndex = c.getColumnIndex("id");
                int nameIndex = c.getColumnIndex("name");
                int descriptionIndex = c.getColumnIndex("description");
                int filenameIndex = c.getColumnIndex("filename");
                int filetypeIndex = c.getColumnIndex("filetype");
                int downloadedIndex = c.getColumnIndex("downloaded");
                Log.i("Database", "2");
                c.moveToFirst();
                Log.i("Database c", Integer.toString(c.getCount()));
                if(c.getCount()>0) {
                    Log.i("Database id", Integer.toString(c.getInt(idIndex)));
                    Log.i("Database name", c.getString(nameIndex));
                    Log.i("Database description", c.getString(descriptionIndex));
                    mediaFiles.add(new MediaFile(jsonMessageObject.getString("name"), jsonMessageObject.getString("description"), jsonMessageObject.getString("filename"), jsonMessageObject.getInt("file_type_id"), jsonMessageObject.getInt("id"), c.getInt(downloadedIndex)));
                    //c.moveToNext();
                    myDatabase.execSQL("UPDATE media SET " +
                            "name = '" + jsonMessageObject.getString("name") +
                            "' ,description = '" + jsonMessageObject.getString("description") +
                            "' ,filename = '" + jsonMessageObject.getString("filename") +
                            "' ,filetype = " + jsonMessageObject.getInt("file_type_id") +
                            " WHERE id = " + jsonMessageObject.getInt("id"));
                } else {
                    Log.i("Database", "4");
                    mediaFiles.add(new MediaFile(jsonMessageObject.getString("name"), jsonMessageObject.getString("description"), jsonMessageObject.getString("filename"), jsonMessageObject.getInt("file_type_id"), jsonMessageObject.getInt("id"), 0));
                    myDatabase.execSQL("INSERT INTO media (name, description, filename, filetype, downloaded, id) Values ('" +
                            jsonMessageObject.getString("name") +
                            "','" + jsonMessageObject.getString("description") +
                            "','" + jsonMessageObject.getString("filename") +
                            "'," + jsonMessageObject.getInt("file_type_id") +
                            ",0" +
                            "," + jsonMessageObject.getInt("id") + ")");
                }
            }
            for(int i=0;i<mediaFiles.size();i++) {
                tempList.add(mediaFiles.get(i).name + "\n" + mediaFiles.get(i).description);
            }
            arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, tempList);
            listView.setAdapter(arrayAdapter);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
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

        //c.moveToFirst();
        Log.i("Database c", Integer.toString(c.getCount()));
        while(c.moveToNext()) {
            Log.i("Database id", Integer.toString(c.getInt(idIndex)));
            Log.i("Database name", c.getString(nameIndex));
            Log.i("Database description", c.getString(descriptionIndex));
            mediaFiles.add(new MediaFile(c.getString(nameIndex), c.getString(descriptionIndex), c.getString(filenameIndex), c.getInt(filetypeIndex), c.getInt(idIndex), c.getInt(downloadedIndex)));
        }

        ArrayList<String> tempList = new ArrayList<String>();
        for(int i=0;i<mediaFiles.size();i++) {
            tempList.add(mediaFiles.get(i).name + "\n" + mediaFiles.get(i).description);
        }
        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, tempList);
        listView.setAdapter(arrayAdapter);
    }
}
