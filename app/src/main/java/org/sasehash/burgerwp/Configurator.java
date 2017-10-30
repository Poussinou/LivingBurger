/*
 * Licensed under GPL 3.0
 */

package org.sasehash.burgerwp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static org.sasehash.burgerwp.Type.BOOL;
import static org.sasehash.burgerwp.Type.FLOAT;
import static org.sasehash.burgerwp.Type.IMAGE;
import static org.sasehash.burgerwp.Type.INT;
import static org.sasehash.burgerwp.Type.LONG;

/**
 * Created by sami on 13/10/17.
 */

/**
 * Activity used for creating a config
 */

public class Configurator extends AppCompatActivity {
    private TableLayout v;
    private static SharedPreferences.Editor newSettings;
    private static ArrayList<String> intentKeys = new ArrayList<>();
    public final static String[] preconfigurated = new String[] {
            "standard",
            "christmas",
    };
    public static String[] prefvalues = new String[]{
            "count",
            "isExternalResource",
            "image",
            "x",
            "y",
            "actualTime",
            "totalTime",
            "selfDestroy",
            "bouncing",
            "speed",
            "rotation",
            "scalingFactor",
            "runsAway"
    };
    //prefvalues[i] has the type prefvaluesType[i]
    public static Type[] prefvaluesType = new Type[]{
            INT, BOOL, IMAGE, INT, INT, LONG, LONG, BOOL, BOOL, INT, FLOAT, FLOAT, BOOL
    };
    private final int importIntentID = 703;

    //the image requested
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (importIntentID == requestCode) {
                importChanges(data);
            } else {
                String helper = intentKeys.get(requestCode);
                intentKeys.remove(requestCode);
                newSettings.putString(helper + "_image", data.getDataString());
                newSettings.putString(helper + "_isExternalResource", "true");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        newSettings = PreferenceManager.getDefaultSharedPreferences(this).edit();
        //this gonna be complicated, but you have a tree here :
        // linearlayout
        // |--scrolling settingpanel
        // |     |--table with settings
        // |--Buttons (in a Row) (apply reset default, export, import, add a row, remove a row etc...)


        HorizontalScrollView scroller = new HorizontalScrollView(this);
        v = new TableLayout(this);
        scroller.addView(v);
        createTable(this.v);


        LinearLayout superLayout = new LinearLayout(this);
        superLayout.setOrientation(LinearLayout.VERTICAL);
        superLayout.addView(scroller);
        View.inflate(this, R.layout.buttons, superLayout);
        setContentView(superLayout);
    }

    //functions called when buttons are pressed
    public void cancelChanges(View v) {
        startActivity(new Intent(this, MainActivity.class));
    }

    public void applyChanges(View v) {
        newSettings.apply();
        //close after applying
        cancelChanges(v);
    }

    public void importChanges(Intent intent) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(intent.getData());
            if (inputStream == null) {
                throw new IllegalStateException("InputStream is Null!");
            }
            Scanner lineScanner = new Scanner(inputStream);
            //lineScanner.useDelimiter("\n");
            newSettings.clear();
            Set<String> keys = new HashSet<>();
            if (!lineScanner.hasNextLine()) {
                throw new IllegalStateException("CANNOT READ FILE!");
            }
            while (lineScanner.hasNextLine()) {
                String currLine = lineScanner.nextLine();
                Scanner scanner = new Scanner(currLine);
                scanner.useDelimiter(";");
                String key = scanner.next();
                keys.add(key);
                System.out.append("key :" + key);
                for (String curr : prefvalues) {
                    String read = scanner.next();
                    System.out.append("just got " + read);
                    newSettings.putString(key + "_" + curr, read);
                }
                //ignore the rest
                scanner.close();
            }
            //put the keys in the thingie
            newSettings.putStringSet("objects", keys);
            newSettings.apply();
            lineScanner.close();
            inputStream.close();
            //reload activity
            startActivity(new Intent(this, this.getClass()));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not read File!", Toast.LENGTH_SHORT).show();
        }
    }

    public void importChanges(View v) {
        //send out an Intent!
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, importIntentID);
    }

    public void exportChanges(View v) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> objectNames = settings.getStringSet("objects", null);
        StringBuilder output = new StringBuilder();
        char c = ';';

        if (objectNames == null) {
            throw new IllegalStateException("Could not read config!");
        }
        for (String s : objectNames) {
            doubleAppend(output, s);
            for (String curr : prefvalues) {
                doubleAppend(output, settings.getString(s + "_" + curr, "0"));
            }
            output.append('\n');
        }
        //TODO:ask the user for a destination
        String timeStamp = new java.util.Date().toString();
        String fileName = "customConfigLivingBurger" + timeStamp + ".csv";
        fileName = fileName.replace(':', '.');
        File exportDestination = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), fileName);
        System.out.append(output);
        try {
            FileWriter writer = new FileWriter(exportDestination);
            writer.write(output.toString());
            writer.close();
            Toast.makeText(this, "Wrote File to " + exportDestination.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error while writing File to " + exportDestination.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    private void doubleAppend(StringBuilder s, String s2) {
        s.append(s2);
        s.append(';');
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * Good time to define how the config should be saved
     * there is a "objects" key that contains a set with all the key with entries
     * One Entry contains :
     * /!\ example for accessing count : settings.getBoolean("nameofentry_count","");
     *
     * count (eg 5 to draw this object 5 times)
     * isExternalResource
     * image
     * x
     * y
     * actualTime
     * totalTime
     * selfDestroy
     * bouncing
     * speed
     * rotation
     * scalingFactor
     */
    public void resetConfig() {
        resetConfig(this, newSettings);
        //restart activity
        startActivity(new Intent(this, Configurator.class));
    }


    private void loadChristmasConfig() {
        loadChristmasConfig(this,newSettings);
        //restart activity
        startActivity(new Intent(this, Configurator.class));
    }

    private void loadChristmasConfig(Context c, SharedPreferences.Editor edit) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        Set<String> deleteMe = settings.getStringSet("objects", null);

        //delete old preference
        if (deleteMe != null) {
            for (String s : deleteMe) {
                for (String curr : prefvalues) {
                    edit.remove(s + "_" + curr);
                }
            }
        }

        //set new Preferences
        String[] burgerOptions = new String[]{
                "20", "false", Integer.toString(R.drawable.noel), "0", "0", "0", "-1", "false", "true", "5", "0", "1.0", "true"
        };
        String[] pizzaOptions = new String[]{
                "20", "false", Integer.toString(R.drawable.pizza), "0", "0", "0", "-1", "false", "false", "5", "180", "1.0", "true"
        };
        Set<String> addMe = new HashSet<String>();
        addMe.add("burger");
        addMe.add("pizza");
        edit.putStringSet("objects", addMe);
        for (int i = 0; i < prefvalues.length; i++) {
            edit.putString("burger_" + prefvalues[i], burgerOptions[i]);
            edit.putString("pizza_" + prefvalues[i], pizzaOptions[i]);
        }
        edit.apply();
    }
    /**
     * the authentic wallpaper
     *
     */
    public static void resetConfig(Context c, SharedPreferences.Editor edit) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        Set<String> deleteMe = settings.getStringSet("objects", null);

        //delete old preference
        if (deleteMe != null) {
            for (String s : deleteMe) {
                for (String curr : prefvalues) {
                    edit.remove(s + "_" + curr);
                }
            }
        }

        //set new Preferences
        String[] burgerOptions = new String[]{
                "20", "false", Integer.toString(R.drawable.burger), "0", "0", "0", "-1", "false", "true", "5", "0", "1.0", "true"
        };
        String[] pizzaOptions = new String[]{
                "20", "false", Integer.toString(R.drawable.pizza), "0", "0", "0", "-1", "false", "false", "5", "180", "1.0", "true"
        };
        Set<String> addMe = new HashSet<String>();
        addMe.add("burger");
        addMe.add("pizza");
        edit.putStringSet("objects", addMe);
        for (int i = 0; i < prefvalues.length; i++) {
            edit.putString("burger_" + prefvalues[i], burgerOptions[i]);
            edit.putString("pizza_" + prefvalues[i], pizzaOptions[i]);
        }
        edit.apply();
    }

    /**help :
     * Contructor for a ToDraw. Note that you need to set xvec,yvec and rvec if you want your object to move!
     *
     * @param texture
     * @param x
     * @param y
     * @param currentMovementTime
     * @param maxMovementTime
     * @param selfDestroy
     * @param bouncing
     * @param speed
     * @param rotation
     * @param scaler
     */

    /**
     * adds the standard Header to current TableLayout, which may look like this:
     * bitmap,xpos,ypos,....and so on
     *
     * @return
     */
    private void addHeader(TableLayout v) {
        TableRow header = new TableRow(this);
        //TODO : repalce options with better names
        String[] options = prefvalues;
        for (String s : options) {
            TextView tv = new TextView(this);
            tv.setText(s);
            tv.setGravity(View.TEXT_ALIGNMENT_CENTER);
            header.addView(tv);
        }
        v.addView(header);
    }

    private void addHeader() {
        addHeader(this.v);
    }

    private CompoundButton.OnCheckedChangeListener generateToggleButtonListener(final String s, final int i) {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                newSettings.putString(s + "_" + prefvalues[i], Boolean.toString(isChecked));
            }
        };
    }

    private TextWatcher generateEditTextListener(final String str, final int i) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                newSettings.putString(str + "_" + prefvalues[i], s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                String checkedValue = null;
                //put it in the editor
                try {
                    switch (prefvaluesType[i]) {
                        case FLOAT:
                            checkedValue = Float.toString(Float.parseFloat(s.toString()));
                            break;
                        case IMAGE:
                            //can be ignored here
                            break;
                        case INT:
                            checkedValue = Integer.toString(Integer.parseInt(s.toString()));
                            break;
                        case LONG:
                            checkedValue = Long.toString(Long.parseLong(s.toString()));
                            break;
                        case BOOL:
                            //bools doesn't use this method
                        default:
                            throw new IllegalStateException("Maybe you forgot to implement something");
                    }
                    if (checkedValue == null) {
                        throw new IllegalStateException("this listener is broken!");
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(Configurator.this, "Incorrect value, must be " + prefvaluesType[i], Toast.LENGTH_SHORT).show();
                    s = Editable.Factory.getInstance()
                            .newEditable(PreferenceManager.getDefaultSharedPreferences(Configurator.this).getString(str + "_" + prefvalues[i], "0"));
                }
            }
        };
    }


    private void createTable(TableLayout v) {
        ArrayAdapter<String> preconfigs = new ArrayAdapter<String>(this, R.layout.selector, preconfigurated);
        Spinner preconfigSelector = new Spinner(this);
        preconfigSelector.setAdapter(preconfigs);
        preconfigSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean first = true;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (first) {
                    first= false;
                } else {
                    //sorry didn't find anything beautifuler
                    if (parent.getItemAtPosition(position).equals(preconfigurated[0])) {
                        resetConfig();
                    } else {
                        if (parent.getItemAtPosition(position).equals(preconfigurated[1])) {
                            loadChristmasConfig();
                        } else {
                            throw new IllegalStateException("Not implemented!");
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        v.addView(preconfigSelector);

        addHeader(v);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> rows = settings.getStringSet("objects", null);
        //a e s t h e t i c s
        v.setPadding(5, 5, 5, 5);
        v.setStretchAllColumns(true);
        //
        if (rows == null) {
            resetConfig();
            rows = settings.getStringSet("objects", null);
            if (rows == null) {
                throw new IllegalStateException("Settings not existing and generating new settings didn't work");
            }
        }
        int intentCounterHelper = -1;
        for (String s : rows) {
            final String helper = s;
            TableRow current = new TableRow(this);
            for (int i = 0; i < prefvalues.length; i++) {
                if (prefvaluesType[i] == BOOL) {
                    Switch tb = new Switch(this);
                    tb.setChecked(Boolean.parseBoolean(settings.getString(s + "_" + prefvalues[i], "false")));
                    tb.setOnCheckedChangeListener(generateToggleButtonListener(s, i));
                    current.addView(tb);
                    continue;
                }
                if (prefvaluesType[i] == IMAGE) {
                    ImageButton ib = new ImageButton(this);
                    try {
                        int id = Integer.parseInt(settings.getString(s + "_" + prefvalues[i], ""));
                        ib.setImageBitmap(BitmapFactory.decodeResource(getResources(), id));
                    } catch (Exception e) {
                        //maybe it was an uri
                        e.printStackTrace();
                        try {
                            ib.setImageBitmap(getBitmapFromUri(Uri.parse(settings.getString(s + "_" + prefvalues[i], ""))));
                        } catch (Exception e2) {
                            //ok use the burger
                            e2.printStackTrace();
                            ib.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.burger));
                        }
                    }
                    ib.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            intentKeys.add(helper);
                            startActivityForResult(intent, intentKeys.size() - 1);
                        }
                    });
                    current.addView(ib);
                    continue;
                }
                EditText et = new EditText(this);
                et.setText(settings.getString(s + "_" + prefvalues[i], ""));
                et.addTextChangedListener(generateEditTextListener(s, i));
                switch (prefvaluesType[i]) {
                    case INT:
                    case LONG:
                        final int numbersOnly = 0x1002;
                        et.setInputType(numbersOnly);
                        break;
                    case FLOAT:
                        final int floatNumbersOnly = 0x2002;
                        et.setInputType(floatNumbersOnly);
                        break;
                    case BOOL:
                        break;
                    case IMAGE:
                        break;
                }
                current.addView(et);
            }
            v.addView(current);
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }
}