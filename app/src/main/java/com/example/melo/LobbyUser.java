package com.example.melo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
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

public class LobbyUser extends AppCompatActivity {

    public static SpotifyAppRemote mSpotifyAppRemote;
    private static long songLength = 0;
    public static final String CLIENT_ID = "ba9b13ccba204ed9a25f1a9bb73ceb8e";
    public static final String CLIENT_SECRET= "327fb0ad3001470db0c94260b74cf120";
    private static final String REDIRECT_URI = "com.example.melo://callback";
    private static ProgressBar progressBar;
    private static int progress = 0;
    private static String currentSong = "";
    private static String currentArtist = "";
    private static boolean isPaused = false;
    private static Timer timer;
    private static String token = "";
    private DatabaseReference mPostReference;
    public static String lobbyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_user);
        progressBar = findViewById(R.id.progressBar);
        Intent intent = getIntent();
        lobbyCode = intent.getStringExtra("lobbyCode");
        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.lobbyCodeText);
        textView.setText("Lobby Code:\n" + MainActivity.lobbyCode);
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference(MainActivity.lobbyCode)
                .child("currentSong");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snap: dataSnapshot.getChildren()) {
                    Log.d("Value", snap.getValue().toString());
                    switch (snap.getKey()) {
                        case "title":
                            currentSong = snap.getValue().toString();
                            ((TextView)findViewById(R.id.currentSong)).setText(currentSong);
                            break;
                        case "artist":
                            currentArtist = snap.getValue().toString();
                            ((TextView)findViewById(R.id.currentArtist)).setText(currentArtist);
                            break;
                        case "duration":
                            songLength = Long.parseLong(snap.getValue().toString());
                            progressBar.setMax((int) songLength / 1000);
                            break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Error", "Cancelled");
            }
        });
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d("KeyIs", dataSnapshot.getKey());
                Log.d("ValueIs", dataSnapshot.getValue().toString());
                switch (dataSnapshot.getKey()){
                    case "title":
                        currentSong = dataSnapshot.getValue().toString();
                        ((TextView)findViewById(R.id.currentSong)).setText(currentSong);
                        break;
                    case "artist":
                        currentArtist = dataSnapshot.getValue().toString();
                        ((TextView)findViewById(R.id.currentArtist)).setText(currentArtist);
                        break;
                    case "duration":
                        songLength = Long.parseLong(dataSnapshot.getValue().toString());
                        progressBar.setMax((int) songLength / 1000);
                        break;
                    case "isPaused":
                        isPaused = Boolean.parseBoolean(dataSnapshot.getValue().toString());
                        break;
                    case "progress":
                        progress = Integer.parseInt(dataSnapshot.getValue().toString());
                        break;
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
        db.getReference(MainActivity.lobbyCode).child("currentSong").addChildEventListener(childEventListener);

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

    @Override
    protected void onStart(){
        super.onStart();
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

    private void connected(){
        EditText searchBox = findViewById(R.id.searchSong);
        searchBox.setOnKeyListener(new View.OnKeyListener(){
            public boolean onKey(View v, int keyCode, KeyEvent event){
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
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


    }

    public void searchSong(View v){
        EditText searchField = findViewById(R.id.searchSong);
        String searchText = searchField.getText().toString();
        if(searchText.isEmpty()){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("You must enter the name of a song or artist to search for").setTitle(R.string.error);
            builder.setPositiveButton("Go Back", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id){
                    return;
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }
        Intent intent = new Intent(this, SearchSpotify.class);
        intent.putExtra("query", searchText);
        intent.putExtra("token", token);
        intent.putExtra("admin", false);
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
//        {
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
