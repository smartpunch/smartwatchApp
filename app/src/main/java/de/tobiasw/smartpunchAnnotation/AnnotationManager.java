package de.tobiasw.smartpunchAnnotation;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public abstract class AnnotationManager {

    // collect xxx annotations before sending data to server
    // tested with 2 - ok. More caused overflow errors. Depending on dataset size
    private static int collectionSize = 2;

    // available label- and hand-types
    private static String[] label_types = new String[]{"hook","frontal","upper-cut","no-action","Training"};
    private static String[] hand_types = new String[]{"left","right","L+R"};

    private static boolean isInTrainingsMode = false;

    // stores the current choosen hand and -label
    private static String choosenHand = null;
    private static String choosenLabel = null;

    // stores the current annotator
    private static String annotator = null;

    // stores current annotation status
    private static boolean annoSessionRuns = false;

    // NetworkConnector for/to backend api server
    private static NetworkConnector backend = new NetworkConnector();

    // stores the annotated datasets
    private static ArrayList<DataSet> currentDatasetStorer = new ArrayList<DataSet>();

    /**
     * Sends the current DataSet(s) to the Database server
     * @return Response as String
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static String sendToBackend() throws ExecutionException, InterruptedException {
        String serverconnstr = backend.getServerHostUrl()+" "+backend.getServerUsername()+" "+backend.getServerPassword();
        backend = new NetworkConnector();
        backend.init_server_connection_settings(serverconnstr);
        backend.preparingDataSetForTransfer(currentDatasetStorer);
        String resu = backend.execute().get();
        return resu;
    }

    /**
     * Resets the current annotation-session. Deletes all local stored datasets.
     */
    public static void resetDataSets(){
        annoSessionRuns = false;
        clearDatabase();
    }

    /**
     * Returns collection size
     * @return collectionSize
     */
    public static boolean readyToSend(){
        boolean result = false;

        if(currentDatasetStorer.size() >= collectionSize){
            result = true;
        }
        return result;
    }

    /**
     * Returns the available label types
     * @return String[] label_types
     */
    public static String[] get_label_types(){
        return Arrays.copyOfRange(label_types,0,label_types.length-1);
    }

    /**
     * Set parameters for a new annoation session
     * @param label
     * @param hand
     * @return result
     */
    public static boolean setAnnotationSessionData(String label,String hand,String annotatorName){
        boolean result = false;
        if(Arrays.asList(label_types).contains(label) && Arrays.asList(hand_types).contains(hand) && annotatorName.length() > 0){
            annotator = annotatorName;
            choosenHand = hand;
            choosenLabel = label;
            result = true;
        }
        return result;
    }

    /**
     * Set the parameter for hand side
     * @param hand
     * @return result
     */
    public static boolean setHand(String hand){
        boolean result = false;
        if(Arrays.asList(hand_types).contains(hand)){
            choosenHand = hand;
            result = true;
        }
        return result;
    }

    /**
     * Set the parameter for the label
     * @param label
     * @return result
     */
    public static boolean setLabel(String label){
        boolean result = false;
        if(Arrays.asList(label_types).contains(label)){
            choosenLabel = label;
            result = true;
        }
        return result;
    }

    /**
     * Sets the annotators name
     * @param annotatorName
     * @return boolean
     */
    public static boolean setAnnotator(String annotatorName){
        annotator = annotatorName;
        return true;
    }

    /**
     * Returns the annotator name
     * @return annotator
     */
    public static String getAnnotator(){
        return annotator;
    }

    /**
     * Sets the AnnotationManager mode to stop
     * @return result
     */
    public static boolean stopAnnotationSession(){
        boolean result = false;

        if(annoSessionRuns) {
            annoSessionRuns = false;
            result = true;
        }
        return result;
    }

    /**
     * Sets the AnnotationManager mode to running
     * @return result
     */
    public static boolean startAnnotationSession(){
        boolean result = false;

        if(!annoSessionRuns) {
            annoSessionRuns = true;
            result = true;
        }
        return result;
    }

    /**
     * Deletes a specific dataset and stops the annotation mode if chosen
     * @param index
     * @return result
     */
    public static boolean deleteDataset(int index,boolean toStop){
        boolean result = false;
        if(index < currentDatasetStorer.size()){
            currentDatasetStorer.remove(index);
            result = true;
        }

        if(annoSessionRuns && toStop){
            stopAnnotationSession();
        }

        return result;
    }

    /**
     * Deletes all available annotated datasets and stops a running session
     * @return result
     */
    public static boolean clearDatabase(){
        if(annoSessionRuns){
            stopAnnotationSession();
        }
        currentDatasetStorer.clear();
        return true;
    }

    /**
     * Deletes all available annotated datasets and stops a running session
     * @return result
     */
    public static boolean resetTheDataSet(){
        currentDatasetStorer.clear();
        return true;
    }

    /**
     * Returns a specific database element
     * @param index
     * @return Database element or null if index not exists
     */
    public static DataSet getDataset(int index){
        DataSet result;
        if(index < currentDatasetStorer.size()){
           result = currentDatasetStorer.get(index);
        }else {
            result = null;
        }
        return result;
    }

    /**
     * Returns the whole database
     * @return currentDatasetStorer
     */
    public static ArrayList<DataSet> getDatabase(){
        return currentDatasetStorer;
    }

    /**
     * Saves a new DataSet object
     * @param ds
     * @return result
     */
    public static boolean saveNewDataset(DataSet ds){
        currentDatasetStorer.add(ds);
        return true;
    }

    /**
     * Returns true/false wheater the AnnotationManager is in mode running or stop
     * @return annoSessionRuns
     */
    public static boolean isAnnotationRunning(){
        return annoSessionRuns;
    }

    /**
     * Returns the chosen hand parameter
     * @return choosenHand
     */
    public static String getChoosenHand(){
        return choosenHand;
    }

    /**
     * Returns the chosen label parameter
     * @return choosenLabel
     */
    public static String getChoosenLabel(){
        return choosenLabel;
    }

    /**
     * Returns the available annotation label types
     * @return label_types
     */
    public static String[] getLabelTypes(){
        return Arrays.copyOfRange(label_types,0,label_types.length-2);
    }

    /**
     * Returns the available hand sides
     * @return hand_types
     */
    public static String[] getHand_types(){
        return Arrays.copyOfRange(hand_types,0,hand_types.length-2);
    }

    /**
     * Returns weather the mode is "continous" or not
     * @return annotation mode
     */
    public static boolean is_in_continuous_mode(){
        return choosenLabel == "no-action" ? true : false;
    }

    /**
     * Returns the URL where to check the server (api backend) availability
     * @return
     */
    public static String getServerCheckUrl(){
        return backend.getServerCheckURL();
    }

    /**
     * Changes the server-settings of the Network Connector instance
     * @return success/failure
     */
    public static boolean init_server_data(String settings){
        return backend.init_server_connection_settings(settings);
    }

    /**
     * Returns the server host url of the
     * current NetworkConnector instance
     * @return String
     */
    public static String get_server_host_url(){
        return backend.getServerHostUrl();
    }

    /**
     * Returns the username of the
     * current NetworkConnector instance
     * @return String
     */
    public static String get_server_username(){
        return backend.getServerUsername();
    }

    /**
     * Returns the password of the
     * current NetworkConnector instance
     * @return String
     */
    public static String get_server_password(){
        return backend.getServerPassword();
    }

    /**
     * Prepares the parameters for trainings mode
     * @return void
     */
    public static void init_trainingsmode_settings(){
        choosenHand = hand_types[hand_types.length-1];
        choosenLabel = label_types[label_types.length-1];
        annotator = "Trainingsmode";
        isInTrainingsMode = true;
    }

    /**
     * stops a running trainingsmode
     * @return void
     */
    public static void stop_trainingsmode(){
        isInTrainingsMode = false;
    }

    /**
     * Returns weather the app is in trainingsmode or not
     * @return boolean
     */
    public static boolean is_in_trainingsmode(){
        return isInTrainingsMode;
    }

    public static String get_last_classification_result(){
        return backend.getClassificationResult();
    }



}
