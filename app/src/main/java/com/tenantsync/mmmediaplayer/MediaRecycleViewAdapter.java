package com.tenantsync.mmmediaplayer;

/**
 * Created by michaelmarch on 1/30/17.
 */

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.MODE_WORLD_READABLE;
import static android.os.ParcelFileDescriptor.MODE_WORLD_WRITEABLE;

// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
public class MediaRecycleViewAdapter extends RecyclerView.Adapter<MediaRecycleViewAdapter.ViewHolder> {

    // Store a member variable for the mediaAcce
    private List<MediaFile> mMediaFiles;
    // Store the context for easy access
    private Context mContext;


    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView textTitle;
        public TextView textDescription;
        public ImageView imageDownload;
        public ImageView imageView;
        public ProgressBar progressBar;
        private Context context;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(Context context, View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            textTitle = (TextView) itemView.findViewById(R.id.textTitle);
            textDescription = (TextView) itemView.findViewById(R.id.textDescription);
            imageDownload = (ImageView) itemView.findViewById(R.id.imageDownload);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
            this.context = context;
            itemView.setOnClickListener(this);
            //textTitle.setOnClickListener(this);
            imageDownload.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            //Overridden below to have access to mMediaFiles list.
        }
    }

    // Pass in the contact array into the constructor
    public MediaRecycleViewAdapter(Context context, List<MediaFile> mediaFiles) {
        mMediaFiles = mediaFiles;
        mContext = context;
    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return mContext;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public MediaRecycleViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View mediaView = inflater.inflate(R.layout.list_layout_media, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(context, mediaView) {
            @Override
            public void onClick(View v) {
                final int position = getAdapterPosition();
                Log.d("RecyclerView", Integer.toString(position));
                if(imageDownload.getId() == v.getId()) {
                    Log.d("RecyclerView", "Download/Delete Clicked");
                    Log.d("RecyclerView", "Download Value: " + Integer.toString(mMediaFiles.get(position).downloaded));
                    if(mMediaFiles.get(position).downloaded == 0) {
                        // When downloaded =-1 that means download in progress
                        Log.d("RecyclerView", "Downloading file");
                        mMediaFiles.get(position).downloaded = -1;
                        progressBar.setVisibility(View.VISIBLE);
                        //new MediaRecycleViewAdapter.DownloadFile().execute(Helper.url + "/audio/" + mMediaFiles.get(position).filename, mMediaFiles.get(position).filename, Integer.toString(mMediaFiles.get(position).id));
                        InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, Helper.url + "/audio/" + mMediaFiles.get(position).filename,
                                new Response.Listener<byte[]>() {
                                    @Override
                                    public void onResponse(byte[] response) {
                                        // TODO handle the response
                                        try {
                                            if (response!=null) {
                                                //Writes response to internal app storage
                                                FileOutputStream outputStream;
                                                outputStream = mContext.openFileOutput(mMediaFiles.get(position).filename, Context.MODE_PRIVATE);
                                                outputStream.write(response);
                                                outputStream.close();
                                                Log.d("RecyclerView", "Download Complete");
                                                Toast.makeText(mContext, "Download complete.", Toast.LENGTH_LONG).show();
                                                mMediaFiles.get(position).downloaded = 1;
                                                imageDownload.setImageResource(R.drawable.ic_delete_black_24dp);
                                                progressBar.setVisibility(View.INVISIBLE);
                                            }
                                        } catch (Exception e) {
                                            // TODO Auto-generated catch block
                                            Log.d("KEY_ERROR", "UNABLE TO DOWNLOAD FILE");
                                            e.printStackTrace();
                                            progressBar.setVisibility(View.INVISIBLE);
                                            mMediaFiles.get(position).downloaded = 0;
                                        }
                                    }
                                } ,new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // TODO handle the error
                                error.printStackTrace();
                                progressBar.setVisibility(View.INVISIBLE);
                                mMediaFiles.get(position).downloaded = 0;
                                Toast.makeText(mContext, "Network Error.", Toast.LENGTH_LONG).show();
                            }
                        }, null);
                        RequestQueue mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext(), new HurlStack());
                        mRequestQueue.add(request);
                    } else if(mMediaFiles.get(position).downloaded == 1) {
                        File fileLocation = new File(context.getFilesDir(), mMediaFiles.get(position).filename);
                        Log.d("RecyclerView", "Deleting file");
                        if(fileLocation.exists()) {
                            Log.i("Values", "File Exists");
                            fileLocation.delete();
                            mMediaFiles.get(position).downloaded = 0;
                        } else {
                            Log.i("Values", "File Does Not Exist");
                            mMediaFiles.get(position).downloaded = 0;
                        }
                        imageDownload.setImageResource(R.drawable.cloud72px);
                    }
                } else {
                    Intent intent = null;
                    if(mMediaFiles.get(position).fileType == 1) {
                        // This is an audio file
                        intent = new Intent(context, AudioPlayer.class);
                    } else if(mMediaFiles.get(position).fileType == 2) {
                        // This is a video file
                        intent = new Intent(context, VideoPlayer.class);
                    }
                    intent.putExtra("id", Integer.toString(mMediaFiles.get(position).id));
                    intent.putExtra("name", mMediaFiles.get(position).name);
                    intent.putExtra("description", mMediaFiles.get(position).description);
                    intent.putExtra("filename", mMediaFiles.get(position).filename);
                    intent.putExtra("fileType", Integer.toString(mMediaFiles.get(position).fileType));
                    intent.putExtra("downloaded", Integer.toString(mMediaFiles.get(position).downloaded));
                    intent.putExtra("imagefile", mMediaFiles.get(position).imagefile);
                    context.startActivity(intent);
                }
            }
        };
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(MediaRecycleViewAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        final MediaFile mediaFile = mMediaFiles.get(position);

        // Set item views based on your views and data model
        TextView textTitle = viewHolder.textTitle;
        textTitle.setText(mediaFile.name);
        TextView textDescription = viewHolder.textDescription;
        textDescription.setText(mediaFile.description);
        ImageView imageDownload = viewHolder.imageDownload;
        final ImageView imageView = viewHolder.imageView;
        File fileLocation = new File(mContext.getFilesDir(), mediaFile.filename);
        if(!fileLocation.exists()) {
            imageDownload.setImageResource(R.drawable.cloud72px);
        } else {
            imageDownload.setImageResource(R.drawable.ic_delete_black_24dp);
        }
        if(!mediaFile.imagefile.isEmpty()) {

            // Retrieves an image specified by the URL, displays it in the UI.
            ImageRequest request = new ImageRequest(Helper.url + "/image/" + mediaFile.imagefile,
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
            MySingleton.getInstance(mContext).addToRequestQueue(request);
        }
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mMediaFiles.size();
    }
}