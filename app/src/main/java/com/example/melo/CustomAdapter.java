package com.example.melo;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.ArrayList;

import androidx.appcompat.app.AlertDialog;

public class CustomAdapter extends ArrayAdapter<Song> implements View.OnClickListener {
    ArrayList<Song> results;
    Context context;
    private int lastPosition = -1;

    private static class ViewHolder {
        TextView title;
        TextView txtType;
        TextView artist;
        ImageView addToQueue;
    }


    public CustomAdapter(ArrayList<Song> results, Context context) {
        super(context, R.layout.row_item, results);
        this.results = results;
        this.context=context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        // Get the data item for this position
        Song song = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_item, parent, false);
            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
            viewHolder.artist = (TextView) convertView.findViewById(R.id.artist);
            viewHolder.addToQueue = (ImageView) convertView.findViewById(R.id.addToQueue);

            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(context, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        result.startAnimation(animation);
        lastPosition = position;

        viewHolder.title.setText(song.getTitle());
        viewHolder.artist.setText(song.getArtist());
        viewHolder.addToQueue.setOnClickListener(this);
        viewHolder.addToQueue.setTag(position);
        // Return the completed view to render on screen
        return convertView;
    }

    //Add to queue
    @Override
    public void onClick(View v){
        int position=(Integer) v.getTag();
        Object object= getItem(position);
        Song song=(Song)object;
        Snackbar.make(v, "Added " + song.getTitle() + " by " + song.getArtist() + " to the queue", Snackbar.LENGTH_LONG)
                .setAction("No action", null).show();
        SpotifyAppRemote mSpotifyAppRemote = LobbyUser.mSpotifyAppRemote;
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference();
        ref.child(MainActivity.lobbyCode).child(song.getTitle()).setValue(song.getUri());
        //mSpotifyAppRemote.getPlayerApi().queue(song.getUri());
        //TODO: Add verficiation for success/failure


    }

}
