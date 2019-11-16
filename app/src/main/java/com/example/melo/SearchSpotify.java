package com.example.melo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchSpotify extends AppCompatActivity {

    public String token;
    ListView listView;
    private static CustomAdapter adapter;
    private boolean admin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_spotify);
        Intent intent = getIntent();
        String query = intent.getStringExtra("query");
        token = intent.getStringExtra("token");
        admin = intent.getBooleanExtra("admin", false);
        TextView textView = findViewById(R.id.text);
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.spotify.com/v1/search?q=" + URLEncoder.encode(query) + "&type=track";
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>(){
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d("Response", response.toString());
                        try {
                            //Log.d("Json", new JSONObject(response.toString()).toString(2));
                            JSONArray items = response.getJSONObject("tracks").getJSONArray("items");
                            ArrayList<Song> results = new ArrayList<>();
                            for(int i = 0; i<items.length(); i++){
                                JSONObject list = items.getJSONObject(i);
                                String artist = list.getJSONArray("artists").getJSONObject(0).getString("name");
                                String title = list.getString("name");
                                String uri = list.getString("uri");
                                String thumbnail = list.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");
                                Song song = new Song(title, artist, uri, thumbnail);
                                results.add(song);
                                Log.d("Song", title);
                                Log.d("Artist", artist);
                                Log.d("uri", uri);
                            }
                            displayResults(results);
                        }
                        catch(Exception e){
                            Log.e("Error", e.getMessage());
                        }
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
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Bearer " + token);
                params.put("Content-Type", "application/json");
                params.put("Accept", "application/json");
                return params;
            }
        };

        queue.add(getRequest);
    }

    @Override
    public void onStart(){
        super.onStart();
        EditText search = (EditText) findViewById(R.id.searchSong);
        search.setOnKeyListener(new View.OnKeyListener(){
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

    public void displayResults(ArrayList<Song> results){
        listView=(ListView)findViewById(R.id.list);
        if(results.isEmpty()){
            TextView noResultsText = findViewById(R.id.noResults);
            noResultsText.setVisibility(View.VISIBLE);
        }
        adapter = new CustomAdapter(results, getApplicationContext());
        listView.setAdapter(adapter);
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
        startActivity(intent);
    }

    public void back(View v){
        if(admin) {
            Intent intent = new Intent(this, LobbyAdmin.class);
            startActivity(intent);
        }
        else{
            Intent intent = new Intent(this, LobbyUser.class);
            startActivity(intent);
        }
    }


}
