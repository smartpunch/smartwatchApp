package de.tobiasw.smartpunchAnnotation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static android.hardware.SensorManager.SENSOR_DELAY_FASTEST;

public class AnnotationSession extends WearableActivity implements SensorEventListener {

    // UI messages
    private final String text_message_server_not_available = "Could not transmit data to server! Please check network settings and try again.";
    private final String text_message_auth_failed = "Authentication failed! Please check username/password.";
    private final String text_message_server_error = "Internal server error, contact admin for help.";

    // UI elements
    private Button toggleBtn;
    private ProgressBar waiter;
    private Button saveCancelBtn;
    private TextView showHandTV;
    private TextView showLabelTV;

    // Vibration variables
    private Vibrator vibrator;
    private final long[] vibrateFailPattern = {500, 150, 500, 200};
    private final long[] vibrateSuccessPattern = {200, 100, 200, 150};
    private final long[] continueAnnoPattern = {100,100,100,100};
    private final int doNotRepeatVibratePattern = -1; //-1 to vibrate only once

    // sensor
    private SensorManager sensorManager;
    private Sensor sensor;
    private Instant lastTimestamp;

    // runtime variables
    private boolean isAlreadyListening = false;

    // timing variables
    private Instant startTime;
    private long currentTimeDifference;
    private long sendInterval_ms = 2500;

    // rawdataset storer. stores the current annotated rawdata
    private ArrayList<RawData> rawsensordata;

    // sets the timestamp of the first rawdata element to zero (0)
    private boolean isFirst = true;

    // store the current mode: Trainingsmode ?
    private boolean isInTrainingsMode = false;

    // Text-To-Speech
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annotation_session);

        this.isInTrainingsMode = AnnotationManager.is_in_trainingsmode();

        // for always on screen in annotation mode
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // show the user the chosen hand side and label type
        this.showHandTV = (TextView) findViewById(R.id.annosess_hand_textview);
        this.showLabelTV = (TextView) findViewById(R.id.annosess_labeltype_textview);
        this.showHandTV.setText(AnnotationManager.getChoosenHand().toUpperCase());
        this.showLabelTV.setText(AnnotationManager.getChoosenLabel().toUpperCase());

        this.saveCancelBtn = (Button) findViewById(R.id.btn_save_anno_session);

        // instantiate the systems accelerometer sensor
        this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // stores the annotated sensor rawdata
        this.rawsensordata = new ArrayList<RawData>();

        // vibrations
        this.vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Enables Always-on
        setAmbientEnabled();

        // Enable TTS
        tts=new TextToSpeech(getBaseContext(), new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result=tts.setLanguage(Locale.GERMAN);
                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("error", "This Language is not supported");
                    }
                    else{
                        // TTS works. Set speech parameters
                        tts.setSpeechRate((float) 1.11);
                        tts.setPitch((float)-0.40);
                    }
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });


    }

    // TTS
    private void ConvertTextToSpeech(String outputText) {
        // TODO Auto-generated method stub
        if(outputText==null||"".equals(outputText))
        {
            String text = "Fehler bei der Sprachausgabe";
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }else
            tts.speak(outputText, TextToSpeech.QUEUE_FLUSH, null);
    }

    /**
     * Toggles the annotation mode UI
     * @param v View
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void btn_toggleAnnotation(View v) {
        toggleBtn = (Button) findViewById(R.id.btn_start_stop_anno);

        // waiter spinner
        waiter = (ProgressBar) findViewById(R.id.progress_waiting_anim);

        if (AnnotationManager.isAnnotationRunning()) {
            // stop the session
            if (AnnotationManager.stopAnnotationSession()) {
                onLoggingEnd();
                waiter.setVisibility(View.INVISIBLE);
                toggleBtn.setText("START");
            }
        } else {
            // start a session
            if (AnnotationManager.startAnnotationSession()) {
                onLoggingStart();
                waiter.setVisibility(View.VISIBLE);
                toggleBtn.setText("STOP");
            }
        }
    }

    /**
     * Cancels/Resets the complete annotation session and clears the AnnotationManager
     */
    public void cancelAnnotationSession(){
        if(AnnotationManager.isAnnotationRunning()){
            sensorManager.unregisterListener(this);
            AnnotationManager.stopAnnotationSession();
            AnnotationManager.clearDatabase();
            isAlreadyListening = false;
            isFirst = true;
            this.saveCancelBtn.setText("Save");
            toggleBtn.setText("START");
        }
    }

    /**
     * Cancels the current running annotation
     */
    public void cancelCurrentSession(){
        if(AnnotationManager.isAnnotationRunning()){
            sensorManager.unregisterListener(this);
            AnnotationManager.stopAnnotationSession();
            this.rawsensordata.clear();
            isAlreadyListening = false;
            isFirst = true;
            this.saveCancelBtn.setText("Save");
            toggleBtn.setText("START");
        }
    }

    /**
     * Deletes all locally stored annotation data
     */
    private void resetAnnoData(){
        AnnotationManager.clearDatabase();
    }

    /**
     * Displays a alert dialog if user pressed back or navigates back
     * @param v View
     */
    public void onBackPressed(View v) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Go back? Data will be deleted.");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Yes",
                (DialogInterface.OnClickListener) (dialog, id) -> {
                    this.cancelAnnotationSession();
                    AnnotationManager.resetDataSets();

                    dialog.cancel();

                    if(this.isInTrainingsMode){
                        AnnotationManager.stop_trainingsmode();
                        // go back to main activity
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    }else {
                        // go back to annotation-type chooser
                        startActivity(new Intent(getApplicationContext(), ChooseAnnotationTypeActivity.class));
                    }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    /**
     * Displays a alert dialog if user pressed "save" button.
     * This button has two functions:
     *
     * If a annotation is currently running: It works as a "cancel current session" button
     *
     * If no annotation runs currently it works as a "transmit data to server". If data is
     * available the data will be transmitted to the server.
     * If transmission failed a message is shown
     *
     * @param v View
     */
    public void onSavePressed(View v) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);

        // For mode: "transmit data to server"
        // this option only makes sense if data is available and no session is running
        if(AnnotationManager.getDatabase().size() > 0 && !AnnotationManager.isAnnotationRunning()) {

            // for better looking user dialog
            String s = AnnotationManager.getDatabase().size() > 1 ? "datasets" : "dataset";
            builder1.setMessage("Save current dataset? "+AnnotationManager.getDatabase().size()+" "+s+" available.");

            builder1.setCancelable(true);

            builder1.setPositiveButton(
                    "Yes",
                    (DialogInterface.OnClickListener) (dialog, id) -> {
                        this.manualInitiatedDataTransfer(dialog);
                        dialog.cancel();
                        // go back to annotation-type chooser
                        Intent intent = new Intent(getApplicationContext(), ChooseAnnotationTypeActivity.class);
                        startActivity(intent);
                    });

            builder1.setNegativeButton(
                    "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
        }else if(AnnotationManager.getDatabase().size() < 1 && !AnnotationManager.isAnnotationRunning()) {
            // There is no data to transmit - show a message
            builder1.setMessage("No local data to save. ");
            builder1.setCancelable(true);

            builder1.setNegativeButton(
                    "Go back",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
        }else if(AnnotationManager.isAnnotationRunning()){
            // For mode: "cancel current annotation"
            this.cancelCurrentSession();
            builder1.setMessage("Current session canceled!");
            builder1.setCancelable(true);

            builder1.setNegativeButton(
                    "Ok, go back",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
        }else{
            // this state should never be reached.
            builder1.setMessage("Unexpected state. Please restart the app!");
            builder1.setCancelable(true);

            builder1.setNegativeButton(
                    "Ok, go back",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
        }
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    /**
     * Initiates data transmission to the backend. Gives the user a UI feedback.
     * @param ds DialogInterfaces
     */
    private void manualInitiatedDataTransfer(DialogInterface ds){
        try {
            String resu = AnnotationManager.sendToBackend();

            if(resu == "ok"){
                AnnotationManager.resetDataSets();
            } else if(resu == "auth-failed"){
                this.showErrorMessage(this.text_message_auth_failed);
            } else if(resu == "server-error"){
                this.showErrorMessage(this.text_message_server_error);
            } else{
                this.showErrorMessage(this.text_message_server_not_available);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String sendContinousToServer(){
        String resu = null;
        try {
            resu = AnnotationManager.sendToBackend();
            if(resu != "ok") {
                resu = null;
            }
            AnnotationManager.resetTheDataSet();
            this.rawsensordata.clear();
            this.isFirst = true;
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return resu;
    }

    /**
     * Starts a annotation session. Updates the UI.
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean onLoggingStart() {
        boolean result = false;
        if (!this.isAlreadyListening) {
            this.saveCancelBtn.setText("Cancel");
            this.rawsensordata.clear();
            this.isAlreadyListening = true;
            this.sensorManager.registerListener(this, this.sensor, SENSOR_DELAY_FASTEST);
            result = true;
        }
        return result;
    }

    /**
     * Stops a annotation session. Updates the UI and transmits data if neccessary.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onLoggingEnd() {
        if (isAlreadyListening) {
            isAlreadyListening = false;
            sensorManager.unregisterListener(this);
            isFirst = true;
            this.saveCancelBtn.setText("Save");

            // create a new dataset for the annotated rawdata
            DataSet newValues = new DataSet(AnnotationManager.getChoosenLabel(),AnnotationManager.getChoosenHand(),AnnotationManager.getAnnotator(),this.rawsensordata);
            AnnotationManager.saveNewDataset(newValues);

            // log new values
            dbg_print_new_values();

            String result = null;

            // checks if there is enough data to transmit to the backend
            if(AnnotationManager.readyToSend() || this.isInTrainingsMode) {
                try {

                    result = AnnotationManager.sendToBackend();

                    if(result == "ok"){
                        // transmission finished without error
                        this.vibrator.vibrate(this.vibrateSuccessPattern, this.doNotRepeatVibratePattern);

                        // test: TTS
                        if(this.isInTrainingsMode) {
                            String s = AnnotationManager.get_last_classification_result();
                            if(s != "") {
                                ConvertTextToSpeech(s);
                            }
                        }

                        this.resetAnnoData();
                    } else if(result == "auth-failed"){
                        this.showErrorMessage(this.text_message_auth_failed);
                    } else if(result == "server-error"){
                        this.showErrorMessage(this.text_message_server_error);
                    } else{
                        this.showErrorMessage(this.text_message_server_not_available);
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.d("AnnotationSession", "Send to database with result: " + result);
                Log.d("AnnotationSession", "Database in AnnoManager: " + AnnotationManager.getDatabase().size());
            }else{
                // feedback for continuing annotation
                this.vibrator.vibrate(this.continueAnnoPattern, this.doNotRepeatVibratePattern);
            }
        }
    }

    // for test and dbg purposes
    private boolean dbg_print_new_values() {

        Log.d("AnnotationSession", "Session stopped!");
        // get the newest dataset
        int dbcount = AnnotationManager.getDatabase().size();
        if(dbcount > 0) {
            Log.d("AnnotationSession", "Available dataset count: " + dbcount);
            int size = AnnotationManager.getDataset(dbcount - 1).getRaws().size();
            Log.d("AnnotationSession", "New Dataset raw size: " + size);

            DataSet dsbuff = AnnotationManager.getDataset(dbcount - 1);

            for (int i = 0, index = 1; i < dsbuff.getRaws().size(); i++, index++) {
                Log.d("AnnotationSession", "#" + index + ": " + dsbuff.getRaw(i).toString());
            }
            Log.d("AnnotationSession", "Dataset (time) period (ns): " + AnnotationManager.getDataset(AnnotationManager.getDatabase().size() - 1).getPeriod());
        } else {
            Log.d("AnnotationSession", "No local data since last server upload!");
        }
        return true;
    }

    /**
     * Shows user dialog with the given string message
     * @param mes
     */
    private void showErrorMessage(String mes){
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(mes);
        builder1.setCancelable(true);
        this.vibrator.vibrate(this.vibrateFailPattern, this.doNotRepeatVibratePattern);
        builder1.setNegativeButton(
                "Ok, go back",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    /**
     * Sensor data has changed event. This method is always called if there is new sensor
     * data available.
     * @param event SensorEvent
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSensorChanged(SensorEvent event) {
        // only accelerometer data is needed
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            // is it the first datastamp ?
            if (isFirst) {
                // all following datastamps are ... not the first :-)
                this.isFirst = false;

                // store raw values for x,y,z
                float[] values = event.values;

                // create new rawsensordata object with ... timestamp 0 for the first
                this.rawsensordata.add(new RawData(0L, values[0], values[1], values[2]));

                // create the absolute time diff since annotation session started
                this.lastTimestamp = Instant.now();

                // initialize the timing variable to store starting time
                this.startTime = Instant.now();

                Log.d("AnnotationSession", "First timestamp: "+this.lastTimestamp.getNano());
            } else {
                // for all following datastamps use the time difference since the last datastamp
                long diff = Duration.between(this.lastTimestamp, Instant.now()).toNanos();
                this.lastTimestamp = Instant.now();

                // only values with timestamps greater 0 are valid. Sometimes 0 occured. so keep
                // this if statement here.
                if (diff > 0L) {
                    float[] values = event.values;
                    this.rawsensordata.add(new RawData(diff, values[0], values[1], values[2]));
                }

                // check if maximum period length is reached, than send current dataset
                if(AnnotationManager.is_in_continuous_mode()){
                    this.currentTimeDifference = Duration.between(this.startTime, Instant.now()).toMillis();
                    if(this.currentTimeDifference >= this.sendInterval_ms){
                        // create a new dataset for the annotated rawdata
                        DataSet newValues = new DataSet(AnnotationManager.getChoosenLabel(),AnnotationManager.getChoosenHand(),AnnotationManager.getAnnotator(),this.rawsensordata);
                        AnnotationManager.saveNewDataset(newValues);

                        // send current dataset to server
                        String resu = sendContinousToServer();

                        // reset time interval
                        this.startTime = Instant.now();

                        // dgb output
                        Log.d("AnnotationSession", "continous send transmitted after: "+this.currentTimeDifference+" ms.");
                    }
                }
            }
        }
    }


    /**
     * Needs to be imported because of the interface. Not needed in this app. Boiler plate.
     * @param sensor
     * @param accuracy
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

}
