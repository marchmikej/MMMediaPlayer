package com.tenantsync.mmmediaplayer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

public class SplashScreen extends AppCompatActivity {

    SQLiteDatabase myDatabase;
    Context context1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        context1= this;
        myDatabase = this.openOrCreateDatabase("MMMedia", MODE_PRIVATE, null);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS media (name VARCHAR , description VARCHAR, filename VARCHAR, imagefile VARCHAR, filetype INT(3), startlocation INT(10), downloaded INT(3), id INTEGER PRIMARY KEY)");
        new GetJSONNetwork().execute(Helper.url + "/androidtest");
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
                CharSequence text = "You are offline";
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context1, text, duration);
                toast.show();
                Intent intent = new Intent(context1, MainActivity.class);
                context1.startActivity(intent);
                finish();
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
                                        //c.moveToNext();
                    myDatabase.execSQL("UPDATE media SET " +
                            "name = '" + jsonMessageObject.getString("name") +
                            "' ,description = '" + jsonMessageObject.getString("description") +
                            "' ,filename = '" + jsonMessageObject.getString("filename") +
                            "' ,filetype = " + jsonMessageObject.getInt("file_type_id") +
                            " ,imagefile= '" + jsonMessageObject.getString("image_file_name") +
                            "' WHERE id = " + jsonMessageObject.getInt("id"));
                } else {
                    Log.i("Database", "4");
                    myDatabase.execSQL("INSERT INTO media (name, description, filename, imagefile, filetype, downloaded, startlocation, id) Values ('" +
                            jsonMessageObject.getString("name") +
                            "','" + jsonMessageObject.getString("description") +
                            "','" + jsonMessageObject.getString("filename") +
                            "','" + jsonMessageObject.getString("image_file_name") +
                            "'," + jsonMessageObject.getInt("file_type_id") +
                            ",0,0" +
                            "," + jsonMessageObject.getInt("id") + ")");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(context1, MainActivity.class);
        context1.startActivity(intent);
        finish();
    }
}
