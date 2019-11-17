package com.example.melo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Artist;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import android.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LobbyAdmin extends AppCompatActivity {

    public static SpotifyAppRemote mSpotifyAppRemote;
    private static long songLength = 0;
    public static final String CLIENT_ID = "ba9b13ccba204ed9a25f1a9bb73ceb8e";
    public static final String CLIENT_SECRET= "327fb0ad3001470db0c94260b74cf120";
    private static final String REDIRECT_URI = "com.example.melo://callback";
    private static ProgressBar progressBar;
    private static int progress = 0;
    private static String currentSong = "";
    private static Timer timer;
    private static String token = "";
    private DatabaseReference mPostReference;
    public static String lobbyCode;
    public static boolean isPaused = false;
    public static boolean first = true;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_admin);
        Intent intent = getIntent();
        lobbyCode = intent.getStringExtra("lobbyCode");
        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.lobbyCodeText);
        textView.setText("Lobby Code:\n" + MainActivity.lobbyCode);
    }

    @Override
    protected void onStart(){
        super.onStart();
        Log.d("first", Boolean.toString(first));
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");

                        // Now you can start interacting with App Remote
                        connected();

                    }
                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MyActivity", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
        getAuth();
    }

    private void connected() {
        EditText searchBox = findViewById(R.id.searchSong);
        searchBox.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            searchSong(v);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final Track track = playerState.track;
                    FirebaseDatabase db = FirebaseDatabase.getInstance();
                    if (track != null) {
                        Log.d("MainActivity", track.name + " by " + track.artist.name);
                        TextView songName = findViewById(R.id.currentSong);
                        songName.setText(track.name);
                        TextView artist = findViewById(R.id.currentArtist);
                        artist.setText(track.artist.name);
                        if (!currentSong.equals(track.name)) {
                            currentSong = track.name;
                            Log.d("Main", currentSong);
                            Log.d("Main", track.name);
                            setProgress(playerState.playbackPosition, track.duration);
                            db.getReference(MainActivity.lobbyCode).child("currentSong").child("title").setValue(track.name)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("Success", "Added song");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("Fail", e.getMessage());
                                        }
                                    });
                            db.getReference(MainActivity.lobbyCode).child("currentSong").child("artist").setValue(track.artist.name);
                            db.getReference(MainActivity.lobbyCode).child("currentSong").child("duration").setValue(Long.toString(track.duration));
                            db.getReference(MainActivity.lobbyCode).child("currentSong").child("isPaused").setValue("false");

                        }
                        else {  //Song hasn't changed
                            if(playerState.isPaused){   //Song is paused
                                isPaused = true;
                                db.getReference(MainActivity.lobbyCode).child("currentSong").child("isPaused").setValue("true");
                                return;
                            }
                            else{
                                if(isPaused){   //Song is not paused but it was
                                    isPaused = false;
                                    db.getReference(MainActivity.lobbyCode).child("currentSong").child("isPaused").setValue("false");
                                    return;
                                }
                                //Song was restarted
                                setProgress(playerState.playbackPosition, track.duration);
                                db.getReference(MainActivity.lobbyCode).child("currentSong").child("position").setValue("0");
                                isPaused = false;
                            }
                        }
                    }
                    else {
                        setProgress(playerState.playbackPosition, track.duration);
                        db.getReference(MainActivity.lobbyCode).child("currentSong").child("position").setValue("0");
                    }
                    if (playerState.isPaused) {
                        Log.d("MainActivity", "Paused");
                        ImageButton button = findViewById(R.id.pausePlayButton);
                        button.setImageResource(android.R.drawable.ic_media_play);
                        isPaused = true;
                        db.getReference(MainActivity.lobbyCode).child("currentSong").child("isPaused").setValue("true");
                    } else {
                        isPaused = false;
                        db.getReference(MainActivity.lobbyCode).child("currentSong").child("isPaused").setValue("false");
                        ImageButton button = findViewById(R.id.pausePlayButton);
                        button.setImageResource(android.R.drawable.ic_media_pause);
                    }
                });


        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                if (dataSnapshot.getKey().equals("currentSong") || dataSnapshot.getKey().equals("userAdded")) {
                    return;
                }
                Log.d("Value", (String) dataSnapshot.getValue());
                Log.d("Key", dataSnapshot.getKey());

                if (!((String) dataSnapshot.getValue()).equals("null")) {
                    Log.d("Key", dataSnapshot.getKey());
                    mSpotifyAppRemote.getPlayerApi().queue((String) dataSnapshot.getValue());
                    FirebaseDatabase db = FirebaseDatabase.getInstance();
                    db.getReference(MainActivity.lobbyCode).child(dataSnapshot.getKey()).removeValue();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                if(dataSnapshot.getKey().equals("userAdded")){
                    if(dataSnapshot.getValue().toString().equals("true")){
                        mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
                            setProgress(playerState.playbackPosition, playerState.track.duration);
                            FirebaseDatabase db = FirebaseDatabase.getInstance();
                            db.getReference(MainActivity.lobbyCode).child("userAdded").setValue("false");
                        });
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference(MainActivity.lobbyCode).addChildEventListener(childEventListener);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!isPaused){
                    ((ProgressBar)findViewById(R.id.progressBar)).setProgress(progress++);
                }
            }
        }, 0,1000);
    }

    public void setProgress(Long _progress, Long _songLength){
        progress = (int) (_progress/1000);
        songLength = _songLength;
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax((int) songLength / 1000);
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference(MainActivity.lobbyCode).child("currentSong").child("position").setValue(Integer.toString(progress));
    }

    public void pausePlayClick(View v){
        PlayerApi api = mSpotifyAppRemote.getPlayerApi();
        api.getPlayerState()
                .setResultCallback(playerState -> {
                    if(playerState.isPaused){
                        api.resume();
                        ImageButton button = findViewById(R.id.pausePlayButton);
                        button.setImageResource(android.R.drawable.ic_media_pause);
                    }
                    else{
                        api.pause();
                        ImageButton button = findViewById(R.id.pausePlayButton);
                        button.setImageResource(android.R.drawable.ic_media_play);
                    }
                });
    }

    public void nextSong(View v){
        PlayerApi api = mSpotifyAppRemote.getPlayerApi();
        api.getPlayerState()
                .setResultCallback(playerState -> {
                    api.skipNext();
                });
    }

    public void previousSong(View v){
        PlayerApi api = mSpotifyAppRemote.getPlayerApi();
        api.getPlayerState()
                .setResultCallback(playerState -> {
                    api.skipPrevious();
                });
    }
    public void searchSong(View v){
        EditText searchField = findViewById(R.id.searchSong);
        String searchText = searchField.getText().toString();
        if(searchText.isEmpty()){
            return;
        }
        Intent intent = new Intent(this, SearchSpotify.class);
        intent.putExtra("query", searchText);
        intent.putExtra("token", token);
        intent.putExtra("admin", true);
        startActivity(intent);
    }

    public void getAuth(){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://accounts.spotify.com/api/token?grant_type=client_credentials";
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>(){
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d("Response", response.toString());
                        try {
                            token = response.getString("access_token");
                        }
                        catch(Exception e){
                            Log.e("Error", "Error is: " + e.getMessage());
                            token = "";
                        }
                        Log.d("Token", token);
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("ERROR","error => "+error.toString());
                    }
                }
        )
//        { // -
//            @Override
//            public Map<String, String> getParams(){
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("grant_type", "client_credentials");
//                return params;
//            }
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                byte[] encode;
                try {
                    encode = (LobbyUser.CLIENT_ID + ":" + LobbyUser.CLIENT_SECRET).getBytes("UTF-8");
                }
                catch(Exception e){
                    Log.e("Encode", e.getMessage());
                    return params;
                }
                params.put("Authorization", "Basic YmE5YjEzY2NiYTIwNGVkOWEyNWYxYTliYjczY2ViOGU6MzI3ZmIwYWQzMDAxNDcwZGIwYzk0MjYwYjc0Y2YxMjA=");// + Base64.encodeToString(encode, Base64.NO_WRAP));
                params.put("Content-Type", "application/x-www-form-urlencoded");
                Log.d("token", Base64.encodeToString(encode, Base64.NO_WRAP));
                return params;
            }
        };

        queue.add(postRequest);
    }

    @Override
    protected void onStop(){
        super.onStop();
        timer.cancel();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
