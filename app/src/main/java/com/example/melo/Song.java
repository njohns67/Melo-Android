package com.example.melo;

import android.widget.ProgressBar;

public class Song {
    public String title;
    public String artist;
    public String uri;
    public Song(String _title, String _artist, String _uri){
        title = _title;
        artist = _artist;
        uri = _uri;
    }

    public String getTitle(){
        return title;
    }

    public String getArtist(){
        return artist;
    }

    public String getUri(){
        return uri;
    }
}
