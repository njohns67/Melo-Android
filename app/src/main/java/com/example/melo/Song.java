package com.example.melo;

import android.widget.ProgressBar;

public class Song {
    public String title;
    public String artist;
    public String uri;
    public String thumbnail;
    public Song(String _title, String _artist, String _uri, String _thumbnail){
        title = _title;
        artist = _artist;
        uri = _uri;
        thumbnail = _thumbnail;
    }

    public String getTitle(){
        return title;
    }

    public String getArtist(){
        return artist;
    }

    public String getUri(){ return uri; }

    public String getThumbnail(){ return thumbnail; }
}
