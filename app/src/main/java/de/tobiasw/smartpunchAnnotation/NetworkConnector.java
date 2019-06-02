package de.tobiasw.smartpunchAnnotation;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class NetworkConnector extends AsyncTask<String, Void, String> {

    OkHttpClient client = new OkHttpClient();
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private Exception exception;

    private String serverURL = "";
    private String serverAnnotationURL = "";
    private String serverCheckURL = "";
    private String serverHOSTurl = "";
    private String API_PW = "";
    private String API_USER = "";

    // Stores the classified result of the server sided classification request
    private String classified_result = "";

    // stores the annotated datasets
    private static ArrayList<DataSet> annoData;


    /**
     * Imports new datasets
     * @param ds ArrayList<DataSet>
     */
    public void preparingDataSetForTransfer(ArrayList<DataSet> ds){
        annoData = new ArrayList<DataSet>(ds.size());
        annoData.clear();
        for(DataSet datSet: ds){
            annoData.add(new DataSet(datSet));
        }
    }

    /**
     * Backends send routine for transmit the data to the server
     * @param urls
     * @return
     */
    protected String doInBackground(String... urls) {
        try {

            // create JSON representation of the data
            JSONObject jsonObject = new JSONObject();
            JSONArray datasetarr = new JSONArray();

            jsonObject.put("username",this.API_USER);
            jsonObject.put("password",this.API_PW);

            // generated dataset size for info
            jsonObject.put("gen_ds_size", annoData.size());

            // put the dataset array to the JSON request object
            for(int i = 0; i < annoData.size();i++){
                datasetarr.put(annoData.get(i).toJsonObject());
            }

            jsonObject.put("data_datasets",datasetarr);
            String getResponse = "";
            if(AnnotationManager.is_in_trainingsmode()) {
                getResponse = post(serverAnnotationURL, jsonObject.toString());
            }else {
                getResponse = post(serverURL, jsonObject.toString());
            }
            // for testing and dbg show the response
            Log.d("Backend", "Server API call responsed with: "+getResponse);
            return getResponse;
        } catch (Exception e) {
            this.exception = e;
            return null;
        }
    }

    private String post(String url, String json){
        try {
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            client = new OkHttpClient();
            String resu = "";
            if (response.code() == 200) {
                resu = "ok";
            } else if (response.code() == 511) {
                resu = "auth-failed";
            } else if (response.code() == 500) {
                resu = "server-error";
            } else {
                resu = null;
            }

            // if in trainings mode store the classification result
            if (AnnotationManager.is_in_trainingsmode()) {
                String jsonData = response.body().string();
                JSONObject Jobject = new JSONObject(jsonData);
                this.classified_result = Jobject.get("class").toString();
            }
            return resu;
        }catch (Exception e) {
            this.exception = e;
            return "server-error";
        }
    }

    /**
     * Returns the address of the api route for checking server availability
     * @return
     */
    public String getServerCheckURL(){
        return this.serverCheckURL;
    }

    public boolean init_server_connection_settings(String settings){
        String[] setts = settings.split(" ");
        this.serverURL = "http://"+setts[0]+"/api/newannodata";
        this.serverAnnotationURL = "http://"+setts[0]+"/api/classify";
        this.serverHOSTurl = setts[0];
        this.serverCheckURL = "http://"+setts[0]+"/api/servercheck";
        this.API_USER = setts[1];
        this.API_PW = setts[2];
        return true;
    }

    public String getServerUsername(){
        return this.API_USER;
    }

    public String getServerHostUrl(){
        return this.serverHOSTurl;
    }

    public String getServerPassword(){
        return this.API_PW;
    }

    public String getClassificationResult(){
        String returner = classified_result;

        if(returner != ""){
            classified_result = "";
        }
        return returner;
    }


}


