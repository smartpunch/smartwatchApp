package de.tobiasw.smartpunchAnnotation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChooseAnnotationTypeActivity extends WearableActivity {
    private TextView mTextView;
    private ListView annoLabels;

    private String selectedHand = null;
    private String selectedAnnoType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_choose_annotation_type);

        mTextView = (TextView) findViewById(R.id.txv_choosen_hand_info);

        // shows the user the selected hand
        this.selectedHand = AnnotationManager.getChoosenHand();
        mTextView.setText("(hand side: "+this.selectedHand+")");

        // creates a list of available annotation types
        create_annotation_type_list();
    }

    /**
     * Creates a list of available annotation types. Fills the UI list element and
     * handles onclick events of the list items.
     */
    private void create_annotation_type_list() {
        annoLabels = (ListView) findViewById(R.id.listview_label_types);

        // get available labels
        String[] labeltypes = AnnotationManager.get_label_types();


        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < labeltypes.length; ++i) {
            list.add(labeltypes[i]);
        }

        // import labels to UI list element
        final StableArrayAdapter adapter = new StableArrayAdapter(this,
                android.R.layout.simple_list_item_1, list);
        annoLabels.setAdapter(adapter);

        // sets the onclick listener for the UI list elements
        annoLabels.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                // for dbg only
                Log.d("General", "Annotation type: "+item.toString());

                // shows a dialog to the user with the choosen label type
                on_annotation_type_choosen_dialog(item.toString());
                }

        });

    }

    /**
     * Dialog to check wheater the user wants to start a annotation with the chosen label type.
     * @param annotype String
     */
    private void on_annotation_type_choosen_dialog(String annotype) {
        this.selectedAnnoType = annotype;
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Start annotation with "+this.selectedHand+" hand "+this.selectedAnnoType+"s?");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        Log.d("General", "Start annotation with the selected types.");
                        start_annotation_session();
                    }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        Log.d("General", "Annotation start canceled.");
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    /**
     * Opens the annotation activity for starting a annotation session
     */
    private void start_annotation_session() {
        boolean result = AnnotationManager.setAnnotationSessionData(this.selectedAnnoType,this.selectedHand,AnnotationManager.getAnnotator());
        if(result){
            // for dbg only
            Log.d("General", "Ready for starting new annotation session");
            Log.d("General", "Annotation-Session-Data: "+AnnotationManager.getChoosenLabel()+" , "+AnnotationManager.getChoosenHand()+" with Annotator: "+AnnotationManager.getAnnotator());

            // open annotation manager activity
            Intent intent = new Intent(getApplicationContext(), AnnotationSession.class);
            startActivity(intent);
        }
    }

    /**
     * Default StableArrayAdapter boiler plate. Do not change.
     */
    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);

            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

}


