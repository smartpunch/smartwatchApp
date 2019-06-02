package de.tobiasw.smartpunchAnnotation;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class AnnotationMain extends WearableActivity {

    // for backend availability check
    OkHttpClient client;
    MediaType JSON;

    private TextView mTextView;
    private TextView connectionStatusFailed;
    private TextView connectionStatusSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annotation_main);

        client = new OkHttpClient();
        JSON = MediaType.parse("application/json; charset=utf-8");

        // initial connection check
        try {
            check_server_connection(this.getCurrentFocus());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // search available fitness tracker sensors
        check_sensor_availability();
    }

    /**
     * Checks wheater the current hardware has the needed accelerometer sensor
     */
    private void check_sensor_availability() {
        // not neccessary for the current used hardware because sensor is availabe
    }

    /**
     * Button for left-hand annotation mode
     * @param v View
     */
    public void btn_click_left_hand_choosen(View v){
        btn_click_hand_choosen("left");
    }

    /**
     * Button for right-hand annotation mode
     * @param v View
     */
    public void btn_click_right_hand_choosen(View v) {
        btn_click_hand_choosen("right");
    }

    /**
     * General method if a button was clicked.
     * @param side Side
     */
    public void btn_click_hand_choosen(String side) {
        // for testing and dbg
        Log.d("General", "Hand "+side+" choosen.");

        // next activity to show
        // Intent intent = new Intent(getApplicationContext(), ChooseAnnotationTypeActivity.class);
        Intent intent = new Intent(getApplicationContext(), ChooseAnnotatorNameActivity.class);

        // set chosenHand parameter for the new annotations
        boolean resu = AnnotationManager.setHand(side);

        if(resu){
            // show the "chooseAnnotationType" activity
            startActivity(intent);
        } else {
            // error occurred. stop the app.
            Log.d("Error", "Hand "+side+" choosen. This is not a valid hand-type");
            System.exit(-1);
        }
    }

    /**
     * For updating the UIs network status text-field.
     * @param val boolean
     */
    public void setServerStatus(boolean val){
        connectionStatusSuccess = (TextView) findViewById(R.id.txv_connection_status_success);
        connectionStatusFailed = (TextView) findViewById(R.id.txv_connection_status_failed);

        if(val){
            connectionStatusSuccess.setText("connected");
            connectionStatusFailed.setText("");
        }else {
            connectionStatusSuccess.setText("");
            connectionStatusFailed.setText("offline");
        }
    }

    /**
     * Calls the backend server to see if api is available.
     * @param v
     * @throws IOException
     */
    public void check_server_connection(View v) throws IOException {
        CheckServerConnection task = new CheckServerConnection();
        task.execute();
    }

    /**
     * Does the server http call...
     */
    public class CheckServerConnection extends AsyncTask<String, Void, String> {
        private Exception exception;

        protected String doInBackground(String... urls) {
            try {
                boolean isConnected = get(AnnotationManager.getServerCheckUrl());

                if(isConnected) {
                    Log.d("Backend", "Server connection ok");
                    setServerStatus(true);
                }else{
                    Log.d("Backend", "Server disconnected");
                    setServerStatus(false);
                }
                return "";
            } catch (Exception e) {
                setServerStatus(false);
                this.exception = e;
                String mes = "Error while connecting to server: "+e.toString();
                Log.d("Backend", mes);
                return "";
            }
        }

        protected void onPostExecute(String getResponse) {
            System.out.println(getResponse);
        }

        public boolean get(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();

            int code = response.code();
            boolean result = false;

            if(code != 200){
                result = false;
            } else{
                result = true;
            }

            return result;
        }
    }

}
