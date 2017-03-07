package com.tenantsync.mmmediaplayer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class VideoPlayer extends AppCompatActivity {

    String id;
    String name;
    String description;
    String filename;
    String fileType;
    String downloaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        name = intent.getStringExtra("name");
        description = intent.getStringExtra("description");
        filename = intent.getStringExtra("filename");
        fileType = intent.getStringExtra("fileType");
        downloaded = intent.getStringExtra("downloaded");

        TextView textDescription = (TextView)findViewById(R.id.textDescription);
        textDescription.setText(name + ": " + description);

        VideoView videoView = (VideoView)findViewById(R.id.videoView);
        videoView.setVideoPath("android.resource://" +  getPackageName() + "/" + R.raw.bunny);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.start();
    }
}
