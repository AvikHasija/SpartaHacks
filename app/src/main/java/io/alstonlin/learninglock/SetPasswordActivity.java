package io.alstonlin.learninglock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import me.zhanghai.android.patternlock.PatternView;
import me.zhanghai.android.patternlock.SetPatternActivity;

public class SetPasswordActivity extends SetPatternActivity {

    private ArrayList<int[]> pattern = null;
    private ArrayList<Double> timeAtClick = new ArrayList<>();
    private int count = 0;
    private boolean onValid = true;
    private TextView promptTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);
        promptTextView = (TextView) findViewById(R.id.promptText);
        promptTextView.setText("Please draw a pattern 5 times");
        // Listener for the pattern activities
        final PatternView patternView = (PatternView) findViewById(R.id.setPasswordPattern);
        patternView.setOnPatternListener(new PatternView.OnPatternListener() {
            @Override
            public void onPatternStart() {
            }

            @Override
            public void onPatternCleared() {
            }

            @Override
            public void onPatternCellAdded(List<PatternView.Cell> pattern) {
                timeAtClick.add(((double) System.currentTimeMillis()));
            }

            @Override
            public void onPatternDetected(List<PatternView.Cell> p) {
                ArrayList<int[]> current = toList(p);

                if (pattern == null && current.size() >= 6){
                    pattern = current;
                    savePattern(current);
                    LockScreenML.getInstance().setInputLayerCount(calculateTimeElapsed((timeAtClick)).length);
                }

                if (onValid && current.size() >= 6) {
                    if (equal(current, pattern)){
                        LockScreenML.getInstance().addEntry(calculateTimeElapsed(timeAtClick), true, false);
                        count++;
                    }
                    else Snackbar.make(patternView, "Did not match first pattern", Snackbar.LENGTH_SHORT).show();
                } else if (current.size() >= 6){
                    if (equal(current, pattern)){
                        LockScreenML.getInstance().addEntry(calculateTimeElapsed(timeAtClick), false, false);
                        count++;
                    }
                    else Snackbar.make(patternView, "Did not match first pattern", Snackbar.LENGTH_SHORT).show();

                } else {
                    Snackbar.make(patternView, "Pattern is too short; please hit 6 or more circles", Snackbar.LENGTH_SHORT).show();
                }

                patternView.clearPattern();
                timeAtClick.clear();

                if (count == 5){
                    if (onValid) {
                        promptTextView.setText("Now, have other people draw the pattern 5 times");
                        onValid = false;
                        count = 0;
                    }else {
                        LockScreenML.getInstance().train();
                        Intent intent = new Intent(SetPasswordActivity.this, KeypadActivity.class);
                        startActivityForResult(intent, KeypadActivity.ACTIVITY_CODE);
                    }
                };
            }
        });
    }


    public void onBackPressed(){
    }

    private double[] calculateTimeElapsed(ArrayList<Double> timeAtClick){
        double[] elapsedTimes = new double[timeAtClick.size()-1];
        for (int i = 0; i < timeAtClick.size() - 1; i++) {
            elapsedTimes[i] = timeAtClick.get(i + 1) - timeAtClick.get(i);
        }
        return elapsedTimes;
    }

    /**
     * The callback for when KeypadActivity returns with a result
     * @param requestCode The code that was passed when the activity was requested
     * @param resultCode The code that signifies the result's status
     * @param data The data of the actual result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == KeypadActivity.ACTIVITY_CODE) {
            if (resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra(KeypadActivity.PASSCODE_VALUE);
                // Saves passcode to file
                try {
                    FileOutputStream fos = openFileOutput(LockScreenService.PASSCODE_FILENAME, Context.MODE_PRIVATE);
                    ObjectOutputStream os = new ObjectOutputStream(fos);
                    os.writeObject(result);
                    os.close();
                }
                catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }
                stopService(new Intent(this, LockScreenService.class));
                startService(new Intent(this, LockScreenService.class));
                Toast.makeText(this, "All set up!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * Converts the list of Cells representing patterns to a list of int[2] with the same row/col info
     * @param pattern The pattern to convert
     * @return The converted list of int[2]
     */
    private ArrayList<int[]> toList(List<PatternView.Cell> pattern){
        ArrayList<int[]> list = new ArrayList<>();
        for (PatternView.Cell cell : pattern){
            int[] a = new int[2];
            a[0] = cell.getRow();
            a[1] = cell.getColumn();
            list.add(a);
        }
        return list;
    }

    /**
     * Writes the given pattern into disk.
     * @param list The pattern in the form of a List of int[2]
     */
    private void savePattern(List<int[]> list){
        FileOutputStream fos = null;
        ObjectOutputStream os = null;
        try {
            fos = openFileOutput(LockScreenService.PATTERN_FILENAME, Context.MODE_PRIVATE);
            os = new ObjectOutputStream(fos);
            os.writeObject(list);
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                if (os != null) os.close();
            } catch (IOException e){
                e.printStackTrace();
            }
            try {
                if (fos != null) fos.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Determines if two lists of int[2] are equal, because appearently Java can't check that for us.
     * @param l1 The first list
     * @param l2 The second list
     * @return If the lists are equal
     */
    private boolean equal(List<int[]> l1, List<int[]> l2){
        if (l1.size() != l2.size()) return false;
        for (int i = 0; i < l1.size(); i++){
            int[] e1 = l1.get(i);
            int[] e2 = l2.get(i);
            if (e1[0] != e2[0] || e1[1] != e2[1]) return false;
        }
        return true;
    }
}
