package de.tobiasw.smartpunchAnnotation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class SettingsActivity extends WearableActivity {

    private String temp_username = "";
    private String temp_hostUrl = "";
    private String temp_pw = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        this.temp_pw = AnnotationManager.get_server_password();
        this.temp_username = AnnotationManager.get_server_username();
        this.temp_hostUrl = AnnotationManager.get_server_host_url();

        EditText server_url = (EditText) findViewById(R.id.iv_server_url);
        server_url.setText(this.temp_hostUrl, TextView.BufferType.EDITABLE);

        EditText server_username = (EditText) findViewById(R.id.iv_server_username);
        server_username.setText(temp_username, TextView.BufferType.EDITABLE);

        EditText server_password = (EditText) findViewById(R.id.iv_server_pw);
        server_password.setText(temp_pw, TextView.BufferType.EDITABLE);

        server_url.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d("AnnotatorInfo", "Server URL: " + server_url.getText());
                temp_hostUrl = server_url.getText().toString();
                return false;
            }
        });

        server_username.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d("AnnotatorInfo", "Server Username: " + server_username.getText());
                temp_username = server_username.getText().toString();
                return false;
            }
        });

        server_password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d("AnnotatorInfo", "Server PW: " + server_password.getText());
                temp_pw = server_password.getText().toString();
                return false;
            }
        });

        // Enables Always-on
        setAmbientEnabled();
    }

    /**
     * Save settings
     */
    public void btn_click_save_settings(View v) {
        String settings_string = this.temp_hostUrl + " " + this.temp_username + " " + this.temp_pw;
        boolean res = save_settings(settings_string);
        if(res) {
            // fetch main activity
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            // show the main activity to go on
            startActivity(intent);
        }
    }

    private boolean save_settings(String s) {
        String settings_to_save = s;
        boolean result = false;
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getBaseContext().openFileOutput("server_settings.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(settings_to_save);
            outputStreamWriter.close();
            AnnotationManager.init_server_data(settings_to_save);
            result = true;
        } catch (IOException f) {
            Log.e("Exception", "File write failed: " + f.toString());
        }
        return result;
    }
}
