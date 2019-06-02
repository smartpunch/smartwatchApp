package de.tobiasw.smartpunchAnnotation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;

public class ChooseAnnotatorNameActivity extends WearableActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_annotator_name);

        EditText annotatorName = (EditText) findViewById(R.id.editText_annotatorName);

        annotatorName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d("AnnotatorInfo", "Annotator: "+annotatorName.getText());
                String name = annotatorName.getText().toString();
                showDialog(name);
                return false;
            }
        });

        // Enables Always-on
        setAmbientEnabled();
    }

    private void showDialog(String name){
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Start annotation with name: "+name+" ?");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Yes",
                (DialogInterface.OnClickListener) (dialog, id) -> {
                    dialog.cancel();
                    AnnotationManager.setAnnotator(name);

                    // go to annotation-type chooser
                    Intent intent = new Intent(getApplicationContext(), ChooseAnnotationTypeActivity.class);
                    startActivity(intent);
                });

        builder1.setNegativeButton(
                "No, change name",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }





}
