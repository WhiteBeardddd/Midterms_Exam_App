package com.example.midtermsexam_beauty;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class JSON_Parser extends AppCompatActivity {

    ArrayList<String> personName = new ArrayList<>();
    ArrayList<String> personEmail = new ArrayList<>();

    RecyclerView nameandemailRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_json_parser);

        nameandemailRecycler = findViewById(R.id.nameandemailRecycler);
        nameandemailRecycler.setLayoutManager(new LinearLayoutManager(this));

        try {
            String jsonString = loadJSONFromAsset();
            JSONObject jsonObject = new JSONObject(jsonString);

            // Assuming JSON format: {"people": [{"name": "John", "email": "john@example.com"}, {...}]}
            JSONArray userArray = jsonObject.getJSONArray("users");

            for (int i = 0; i < userArray.length(); i++) {
                JSONObject userDetail = userArray.getJSONObject(i);
                personName.add(userDetail.getString("name"));
                personEmail.add(userDetail.getString("email"));
            }

            // Set up adapter (Create a custom RecyclerView adapter)
            CustomAdapter adapter = new CustomAdapter(this,personName, personEmail);
            nameandemailRecycler.setAdapter(adapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("user_list.json"); // Ensure 'data.json' is in 'assets' folder
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }
}
