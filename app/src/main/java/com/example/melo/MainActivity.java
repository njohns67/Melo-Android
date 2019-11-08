package com.example.melo;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.types.Track;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.spotify.MESSAGE";
    private static final String CLIENT_ID = "ba9b13ccba204ed9a25f1a9bb73ceb8e";
    private static final String REDIRECT_URI = "com.example.melo://callback";
    public static SpotifyAppRemote mSpotifyAppRemote;
    public static String lobbyCode;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EditText lobbyCode = (EditText) findViewById(R.id.lobbyCode);
        lobbyCode.setOnKeyListener(new View.OnKeyListener(){
            public boolean onKey(View v, int keyCode, KeyEvent event){
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            joinLobby(v);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    public void joinLobby(View view){
        Intent intent = new Intent(this, LobbyUser.class);
        EditText editText = (EditText) findViewById(R.id.lobbyCode);
        String code = editText.getText().toString();
        if(code.isEmpty()){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.enter_lobby_code_error).setTitle(R.string.error);
            builder.setPositiveButton("Go Back", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id){
                    return;
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(code)){
                    lobbyCode = code;
                    startActivity(intent);
                }
                else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Lobby \"" + code + "\" does not exist").setTitle(R.string.error);
                    builder.setPositiveButton("Go Back", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int id){
                            return;
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    public void createLobby(View view){
        Intent intent = new Intent(this, LobbyAdmin.class);
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        String code = String.format("%06d", number);
        lobbyCode = code;
        intent.putExtra(EXTRA_MESSAGE, "Lobby Code:\n" + code);
        intent.putExtra("lobbyCode", code);
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference();
        ref.child(code).child("null").setValue("null");
        startActivity(intent);
    }
}
