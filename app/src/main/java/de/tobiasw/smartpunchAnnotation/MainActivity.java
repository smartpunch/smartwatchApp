package de.tobiasw.smartpunchAnnotation;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;


public class MainActivity extends WearableActivity {

     private TextView mTextView;
     private TextView connectionStatusFailed;
     private TextView connectionStatusSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        import_server_settings();

    }

    private void import_server_settings(){
            String settings_loaded = "";

            try {
                InputStream inputStream =  getBaseContext().openFileInput("server_settings.txt");

                if ( inputStream != null ) {
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receiveString = "";
                    StringBuilder stringBuilder = new StringBuilder();

                    while ( (receiveString = bufferedReader.readLine()) != null ) {
                        stringBuilder.append(receiveString);
                    }

                    inputStream.close();
                    settings_loaded = stringBuilder.toString();
                    Log.e("Server Settings", "Read file: " + settings_loaded);
                }
            }
            catch (FileNotFoundException e) {
                Log.e("Server Settings", "Server settings file not found: " + e.toString());

                // create init server settings file
                try {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getBaseContext().openFileOutput("server_settings.txt", Context.MODE_PRIVATE));
                    settings_loaded = "localhost:3000"+" "+"the_user"+ " "+"the_pw";
                    outputStreamWriter.write(settings_loaded);
                    outputStreamWriter.close();
                }
                catch (IOException f) {
                    Log.e("Exception", "File write failed: " + f.toString());
                }
            } catch (IOException f) {
                Log.e("Server Settings", "Can not read server settings: " + f.toString());
            }
        AnnotationManager.init_server_data(settings_loaded);
    }

    /**
     * Button for annotation mode selected
     * @param v View
     */
    public void btn_click_annotation_choosen(View v){
       btn_click_mode_choosen("annotation");
    }

    /**
     * Button for training mode selected
     * @param v View
     */
    public void btn_click_training_choosen(View v) {
        btn_click_mode_choosen("training");
    }

    /**
     * General method if a button was clicked.
     * @param mode Mode
     */
    public void btn_click_mode_choosen(String mode) {
        // for testing and dbg
        Log.d("General", "Mode: "+mode+" choosen.");

        if(mode == "annotation") {
            // next activity to show
            Intent intent = new Intent(getApplicationContext(), AnnotationMain.class);
            // show the "AnnotationMain" activity
            startActivity(intent);
        }else{
            AnnotationManager.init_trainingsmode_settings();
            // next activity to show
            Intent intent = new Intent(getApplicationContext(), AnnotationSession.class);
            // show the "AnnotationSession" activity
            startActivity(intent);
        }
    }

    /**
     * Open Settings menu
     * @param v View
     */
    public void btn_click_settings(View v){
        // next activity to show
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        // show the "AnnotationMain" activity
        startActivity(intent);
    }
}
