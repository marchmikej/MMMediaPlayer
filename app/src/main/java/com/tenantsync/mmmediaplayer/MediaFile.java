package com.tenantsync.mmmediaplayer;

/**
 * Created by michaelmarch on 1/12/17.
 */

public class MediaFile {

    public String name;
    public String description;
    public String filename;
    public int fileType;

    public MediaFile(String name, String description, String filename, int fileType) {
        this.name = name;
        this.description = description;
        this.filename = filename;
        this.fileType = fileType;
    }
}
