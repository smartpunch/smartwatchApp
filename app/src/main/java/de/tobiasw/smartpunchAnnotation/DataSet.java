package de.tobiasw.smartpunchAnnotation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DataSet {
    private String label;
    private String hand;
    private String annotator;
    private int count;

    // stores the (total) time the annotation took (in nano seconds)
    private long periodNS = 0L;

    // stores the raw data
    private ArrayList<RawData> raws;

    public DataSet(String label,String hand,String annotator, ArrayList<RawData> raws){
        this.count = raws.size();
        this.label = label;
        this.hand = hand;
        this.annotator = annotator;
        this.raws = new ArrayList<RawData>();

        // store the rawdata and accumulate the period length
        for(int i = 0; i < raws.size();i++){
            this.periodNS += raws.get(i).getTimestamp();
            this.raws.add(raws.get(i));
        }
    }

    // creates a new DataSet of a given DataSet object
    public DataSet(DataSet ds){
        this.label = ds.label;
        this.count = ds.getRaws().size();
        this.hand = ds.hand;
        this.annotator = ds.annotator;
        this.raws = new ArrayList<RawData>();

        // store the rawdata and accumulate the period length
        for(int i = 0; i < ds.getRaws().size();i++){
            this.periodNS += ds.getRaw(i).getTimestamp();
            this.raws.add(ds.getRaw(i));
        }
    }

    public String toString(){
        // for testing and dbg
        return "[Values="+this.count+";period (in ns)="+this.periodNS+";label="+this.label+";side="+this.hand+";annotator:="+this.annotator+"]";
    }

    /**
     * Returns the annotator name
     * @return annotator
     */
    public String getAnnotator(){ return this.annotator;}

    /**
     * Returns the time period of the dataset
     * @return periodNS
     */
    public long getPeriod(){
        return this.periodNS;
    }

    /**
     * Returns a specific raw data element
     * @param index
     * @return RawData element or null if index not valid
     */
    public RawData getRaw(int index){
        RawData result;
        if(index < this.raws.size()){
            result = this.raws.get(index);
        }else{
            result = null;
        }
        return result;
    }

    /**
     * Returns the label of the dataset.
     * @return label String
     */
    public String getLabel(){
        return this.label;
    }

    /**
     * Returns the hand type of the dataset.
     * @return hand type String
     */
    public String getHand(){
        return this.hand;
    }

    /**
     * Returns the number of data elements
     * @return int count
     */
    public int getCount(){
        return this.count;
    }

    /**
     * Returns the DataSet representation of the current object in JSON format.
     * ! Note: Changes in this method causes changes in the backend/api !
     * @return
     */
    public JSONObject toJsonObject(){
        JSONObject jsob = new JSONObject();
        JSONArray rawdataarr = new JSONArray();
        try {
            jsob.put("label", this.getLabel());
            jsob.put("hand", this.getHand());
            jsob.put("annotator", this.getAnnotator());
            jsob.put("count",this.count);
            jsob.put("periodNS",this.periodNS);

            for(int i = 0; i < this.count; i++){
                rawdataarr.put(this.raws.get(i).toJsonObject());
            }

            jsob.put("raws",rawdataarr);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsob;
    }


    /**
     * Returns all raw data elements of the dataset
     * @return ArrayList<RawData> raws
     */
    public ArrayList<RawData> getRaws(){
        return this.raws;
    }
}
