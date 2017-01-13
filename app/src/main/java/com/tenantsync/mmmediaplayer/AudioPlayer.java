package com.tenantsync.mmmediaplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        timeLeft = (TextView)findViewById(R.id.timeLeft);
        timePlayed = (TextView)findViewById(R.id.timePlayed);

        playing = false;
        paused = false;

        //mediaPlayer = MediaPlayer.create(this,R.raw.dogpanting);

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource("http://9f36e30e.ngrok.io/audio/dogpanting.mp3");
            mediaPlayer.prepare();
            playing = true;
            mediaPlayer.start();
        } catch (Exception e) {
            Log.i("Exception", "Exception creating media player");
        }

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
                Log.i("Location Seekbar Value", Integer.toString(progress));
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
                Log.i("Volume Seekbar Value", Integer.toString(progress));
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
        });

        ////////////////////////////////
        // End Volume Control         //
        ////////////////////////////////
    }

    public void playAudio(View view) {
        playing = true;
        mediaPlayer.start();
    }

    public void pauseAudio(View view) {
        mediaPlayer.pause();
        playing = false;
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

    public class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection)url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while(data!=1) {
                    char current = (char) data;
                    result+= current;
                    data = reader.read();
                }
                Log.i("Value2", result);
                return result;
            } catch(MalformedURLException e) {
                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            }

            return "Done";
        }
    }
}

