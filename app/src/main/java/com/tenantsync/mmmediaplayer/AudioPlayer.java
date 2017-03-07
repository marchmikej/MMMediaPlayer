package com.tenantsync.mmmediaplayer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class AudioPlayer extends AppCompatActivity {

    MediaPlayer mediaPlayer;
    AudioManager audioManager;
    boolean playing;
    boolean paused;
    TextView timeLeft;
    TextView timePlayed;
    String id;
    String name;
    String description;
    String filename;
    String fileType;
    String downloaded;
    String imagefile;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        timeLeft = (TextView)findViewById(R.id.timeLeft);
        timePlayed = (TextView)findViewById(R.id.timePlayed);
        imageView = (ImageView)findViewById(R.id.imageViewIcon);

        playing = false;
        paused = false;

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        name = intent.getStringExtra("name");
        description = intent.getStringExtra("description");
        filename = intent.getStringExtra("filename");
        fileType = intent.getStringExtra("fileType");
        downloaded = intent.getStringExtra("downloaded");
        imagefile = intent.getStringExtra("imagefile");

        SQLiteDatabase myDatabase = this.openOrCreateDatabase("MMMedia", MODE_PRIVATE, null);
        String query = "SELECT * FROM media WHERE id = " + id;
        Cursor c = myDatabase.rawQuery(query, null);
        int startIndex = c.getColumnIndex("startlocation");
        c.moveToFirst();
        int startlocation = 0;
        if(c.getCount()>0) {
            startlocation = c.getInt(startIndex);
        }

        // Retrieves an image specified by the URL, displays it in the UI.
        ImageRequest request = new ImageRequest(Helper.url + "/image/" + imagefile,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }
                }, 0, 0, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        imageView.setImageResource(R.drawable.musicnote);
                    }
                });
        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(request);

        Context context = this;
        File fileLocation = new File(context.getFilesDir(), filename);

        if(!fileLocation.exists()) {
            Log.i("Playing", "File being played from web");
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(Helper.url + "/audio/" + filename);
                mediaPlayer.prepare();
                playing = true;
                mediaPlayer.seekTo(startlocation);
                mediaPlayer.start();
            } catch (Exception e) {
                Log.i("Exception", "Exception creating media player from web");
                CharSequence text = "Error Playing File Over Network";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        } else  {
            Log.i("Playing", "File being played from internal storage");
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(this, Uri.fromFile(fileLocation));
                mediaPlayer.prepare();
                playing = true;
                mediaPlayer.seekTo(startlocation);
                mediaPlayer.start();

            } catch (Exception e) {
                Log.i("Exception", "Exception creating media player from internal storage " + filename);
                e.printStackTrace();
                Log.i("Exception", "Exception creating media player from web");
                CharSequence text = "Error Loading Audio File from Internal Storage";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }

        TextView audioDescription = (TextView)findViewById(R.id.textViewAudioDescription);
        audioDescription.setText(name + ": " + description);
        //////////////////////////////////
        // Start Location Scrub Control //
        //////////////////////////////////
        final SeekBar locationScrubber = (SeekBar)findViewById(R.id.locationScrubber);
        locationScrubber.setMax(mediaPlayer.getDuration());  // Set length for seekbar

        updateTimers();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                locationScrubber.setProgress(mediaPlayer.getCurrentPosition());
                //timeLeftHandler.obtainMessage(1).sendToTarget();
                //Log.i("Seconds listened", Integer.toString(mediaPlayer.getCurrentPosition()/1000));
                //Log.i("Seconds remaining", Integer.toString((mediaPlayer.getDuration()/1000)-(mediaPlayer.getCurrentPosition()/1000)));
            }
        }, 0, 100);

        locationScrubber.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(playing) {
                    mediaPlayer.pause();
                    paused = true;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(playing) {
                    mediaPlayer.start();
                    paused = false;
                }
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(paused || !playing) {
                    mediaPlayer.seekTo(progress);
                    //updateTimers();
                }
                if(playing) {
                    //updateTimers();
                }
                updateTimers();
            }
        });

        ////////////////////////////////
        // End Location Scrub Control //
        ////////////////////////////////

        ////////////////////////////////
        // Start Volume Control       //
        ////////////////////////////////
        SeekBar volumeControl = (SeekBar)findViewById(R.id.volumeControl);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        volumeControl.setMax(maxVolume);  // Set max value for volume control
        volumeControl.setProgress(currentVolume);  // Puts the volume seekbar at current volume

        volumeControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
        });

        ////////////////////////////////
        // End Volume Control         //
        ////////////////////////////////
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

    @Override
    public void onStop() {
        super.onStop();
        SQLiteDatabase myDatabase = this.openOrCreateDatabase("MMMedia", MODE_PRIVATE, null);
        myDatabase.execSQL("UPDATE media SET " +
                "startlocation = " + mediaPlayer.getCurrentPosition() +
                " WHERE id = " + id);
        myDatabase.close();
    }

    @Override
    public void onBackPressed() {
        // Otherwise defer to system default behavior.
        super.onBackPressed();
        mediaPlayer.pause();
        finish();
    }

    public void playPauseAudio(View view) {
        FloatingActionButton playPauseButton = (FloatingActionButton) findViewById(R.id.fabPlayPause);
        if(playing == true) {
            playing = false;
            mediaPlayer.pause();
            playPauseButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        } else {
            playing = true;
            mediaPlayer.start();
            playPauseButton.setImageResource(R.drawable.ic_pause_white_24dp);
        }
    }

    public void updateTimers() {
        int millisecondsPlayed = mediaPlayer.getCurrentPosition();
        int millisecondsTotal = mediaPlayer.getDuration();
        int millisecondsLeft = millisecondsTotal-millisecondsPlayed;
        // Convert total duration into time
        int hoursLeft = (int) (millisecondsLeft / (1000 * 60 * 60));
        int minutesLeft = (int) (millisecondsLeft % (1000 * 60 * 60)) / (1000 * 60);
        int secondsLeft = (int) ((millisecondsLeft % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        String timeLeftSeconds = Integer.toString(secondsLeft);
        String timeLeftMinutes = Integer.toString(minutesLeft);
        String timeLeftHours = Integer.toString(hoursLeft);
        if(hoursLeft<10) {
            timeLeftHours = "0" + timeLeftHours;
        }
        if(minutesLeft<10) {
            timeLeftMinutes = "0" + timeLeftMinutes;
        }
        if(secondsLeft<10) {
            timeLeftSeconds = "0" + timeLeftSeconds;
        }
        timeLeft.setText(timeLeftHours + ":" + timeLeftMinutes + ":" + timeLeftSeconds);

        // Convert total duration into time
        int hoursPlayed = (int) (millisecondsPlayed / (1000 * 60 * 60));
        int minutesPlayed = (int) (millisecondsPlayed % (1000 * 60 * 60)) / (1000 * 60);
        int secondsPlayed = (int) ((millisecondsPlayed % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        String timePlayedSeconds = Integer.toString(secondsPlayed);
        String timePlayedMinutes = Integer.toString(minutesPlayed);
        String timePlayedHours = Integer.toString(hoursPlayed);
        if(hoursPlayed <10) {
            timePlayedHours = "0" + timePlayedHours;
        }
        if(minutesPlayed<10) {
            timePlayedMinutes = "0" + timePlayedMinutes;
        }
        if(secondsPlayed<10) {
            timePlayedSeconds = "0" + timePlayedSeconds;
        }
        timePlayed.setText(timePlayedHours + ":" + timePlayedMinutes + ":" + timePlayedSeconds);
    }
}

