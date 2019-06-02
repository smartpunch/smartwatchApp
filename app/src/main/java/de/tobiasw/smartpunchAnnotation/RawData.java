package de.tobiasw.smartpunchAnnotation;

import org.json.JSONException;
import org.json.JSONObject;

public class RawData {
    private long timestamp; // time in nanoseconds since the last rawdata was measured
    private float x;        // coordinates of accelerometer sensor
    private float y;        //                  ||
    private float z;        //                  ||

    public RawData(long tstmp, float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = tstmp;
    }

    /**
     * Returns the timestamp of the rawdata
     * @return timestamp
     */
    public long getTimestamp(){
        return this.timestamp;
    }

    /**
     * Returns the raw coordinates
     * @param coordinate Chosen coordinate (x,y,z)
     * @return The value of the chosen coordinate
     */
    public float getCoordinate(String coordinate){
        float result = 0.0f;
        switch  (coordinate){
            case "x":
                result = this.x;
                break;
            case "y":
                result = this.y;
                break;
            case "z":
                result = this.z;
                break;
            default:
                // error
                System.exit(-1);
                /* never reached */
                break;
        }
        return result;
    }

    public String toString(){
        return "[timestamp="+this.timestamp+"; x="+this.x+"; y="+this.y+"; z="+this.z+"]";
    }

    /**
     * Returns a JSON Object for the JSON representation of the RawData object
     * @return
     */
    public JSONObject toJsonObject(){
        JSONObject jsob = new JSONObject();
        try {
            jsob.put("timestamp",this.timestamp);
            jsob.put("x",this.x);
            jsob.put("y",this.y);
            jsob.put("z",this.z);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsob;
    }
}
