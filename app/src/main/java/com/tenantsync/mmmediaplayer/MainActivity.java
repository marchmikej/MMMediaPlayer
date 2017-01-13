package com.tenantsync.mmmediaplayer;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*
        Intent intent = new Intent(this, AudioPlayer.class);
        String message = "Test Message";
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
*/
        listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("Value",Integer.toString(position));
                if(mediaFiles.size() > position) {
                    Log.i("Value",mediaFiles.get(position).name);
                }
            }
        });

        String url = String.format("http://94eab048.ngrok.io/androidtest");

        mediaFiles = new ArrayList<MediaFile>();

        new GetJSONNetwork().execute(url);
    }

    private class GetJSONNetwork extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String jsonString = "";
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
            }
            return jsonString;
        }

        @Override
        protected void onPostExecute(String temp) {
            Log.i("Value", temp);
            parseMediaJSON(temp);
        }
    }

    public void parseMediaJSON(String incomingJSON)
    {
        try {
            JSONArray json = new JSONArray(incomingJSON);
            ArrayList<String> tempList = new ArrayList<String>();
            for(int i=0;i<json.length();i++) {
                JSONObject jsonMessageObject = json.getJSONObject(i);
                mediaFiles.add(new MediaFile(jsonMessageObject.getString("name"), jsonMessageObject.getString("description"), jsonMessageObject.getString("filename"), jsonMessageObject.getInt("file_type_id")));
            }
            for(int i=0;i<mediaFiles.size();i++) {
                tempList.add(mediaFiles.get(i).name + "\n" + mediaFiles.get(i).description);
            }
            arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, tempList);
            listView.setAdapter(arrayAdapter);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
