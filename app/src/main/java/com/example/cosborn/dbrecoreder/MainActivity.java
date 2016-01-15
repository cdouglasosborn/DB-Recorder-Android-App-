package com.example.cosborn.dbrecoreder;

import java.util.Date;
import java.net.URISyntaxException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.content.Context;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout;
import android.app.AlertDialog;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.graphics.Color;
import android.content.DialogInterface;
import android.view.Gravity;

import android.support.v4.content.ContextCompat;
import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.util.Log;
import android.app.Activity;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;


public class MainActivity extends AppCompatActivity {

    static final String VERSION = "2.9";
    static final String SERVER_ADDRESS = "https://decibelrecorder.herokuapp.com/";
    static final String EMIT_NAME = "dbChange";

    Socket mSocket;

    /**
     * Id to identify a camera permission request.
     */
    private static final int REQUEST_MIC = 0;

    TextView mSplModeTV = null;
    TextView mSplDataTV = null;
    Button mSplMaxButton = null;
    Button mSplOnOffButton = null;
    Button mSplModeButton = null;
    Button mSplCalibButton = null;
    Button mSplLogButton = null;
    Button startButton = null;
    ImageButton mSplCalibUpButton = null;
    ImageButton mSplCalibDownButton = null;
    Boolean mMode = false; // false -> fast , true -> slow
    Boolean mCalib = false;
    Boolean mLog = false;
    Boolean mMax = false;

    static final int MY_MSG = 1;
    static final int MAXOVER_MSG = 2;
    static final int ERROR_MSG = -1;

    static int PREFERENCES_GROUP_ID = 0;
    static final int RESET_OPTION = 1;
    static final int ABOUT_OPTION = 2;
    static final int FEEDBACK_OPTION = 4;
    static final int EXIT_OPTION = 3;

    SplEngine mEngine = null;
    Context mContext = MainActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });




        RelativeLayout layout = (RelativeLayout) findViewById(R.id.content);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //this.setTitle("SPL Meter FREE v" + VERSION);

        mSplModeTV = (TextView) findViewById(R.id.splModeTV);
        mSplModeTV.setBackgroundColor(Color.GRAY);
        mSplModeTV.setText("");

        mSplDataTV = (TextView) findViewById(R.id.splTV);
        mSplDataTV.setBackgroundColor(Color.GRAY);
        mSplDataTV.setText("");
        mSplDataTV.setGravity(Gravity.RIGHT);

        mSplOnOffButton = (Button) findViewById(R.id.splOnOffB);
        mSplOnOffButton.setOnClickListener(start_button_handle);

        mSplMaxButton = (Button) findViewById(R.id.splMaxB);
        mSplMaxButton.setOnClickListener(max_button_handle);

        mSplModeButton = (Button) findViewById(R.id.splModeB);
        mSplModeButton.setOnClickListener(mode_button_handle);

        mSplCalibButton = (Button) findViewById(R.id.splCalibB);
        mSplCalibButton.setOnClickListener(calib_button_handle);

        mSplLogButton = (Button) findViewById(R.id.splLogB);
        mSplLogButton.setOnClickListener(log_button_handle);

        mSplCalibUpButton = (ImageButton) findViewById(R.id.splCalibUpB);
        mSplCalibUpButton.setOnClickListener(calibup_button_handle);

        mSplCalibDownButton = (ImageButton) findViewById(R.id.splCalibDownB);
        mSplCalibDownButton.setOnClickListener(calibdown_button_handle);

        startButton = (Button) findViewById(R.id.StartButton);
        startButton.setOnClickListener(startRecorder);

        // start meter in ON state
        mSplOnOffButton.setText("OFF");
        mSplOnOffButton.setTextColor(Color.BLACK);
        mSplModeTV.setText("FAST");
        mSplModeTV.setBackgroundResource(R.drawable.lcd4);
        mSplDataTV.setText("");
        mSplDataTV.setBackgroundResource(R.drawable.lcd2);
        mSplCalibButton.setVisibility(View.VISIBLE);
        mSplLogButton.setVisibility(View.VISIBLE);
        mSplMaxButton.setVisibility(View.VISIBLE);
        mSplModeButton.setVisibility(View.VISIBLE);


    }


    private OnClickListener startRecorder = new OnClickListener() {
        public void onClick(View v) {
            Log.d("here 1", "here 1");
            // Here, thisActivity is the current activity
            if (
                    ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(mContext, Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED){

                Log.d("here 2", "here 2");
                // No explanation needed, we can request the permission.
                Activity activity = (Activity) mContext;
                ActivityCompat.requestPermissions(activity,
                        new String[]{
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.INTERNET
                        },
                        REQUEST_MIC);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            } else {
                Log.d("here 3", "here 3");
                start_meter();
            }
        }

    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_MIC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    start_meter();

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();  // Always call the superclass method first
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stop_meter();
        this.finish();
        //super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();  // Always call the superclass method first
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stop_meter();
        this.finish();
        //super.onDestroy();
    }

    /**
     * Handles start/stop Logging
     *
     * @param set
     */
    public void handle_log(boolean set) {
        if (set) {
            mEngine.startLogging();
        } else {
            mEngine.stopLogging();
            Date today = new Date();
            Toast.makeText(
                    mContext,
                    "Log saved to /sdcard/splmeter_" + today.getDate() + "_"
                            + today.getMonth() + "_" + (today.getYear() + 1900)
                            + ".xls", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays the maximum SPL Value
     */
    public void display_max() {
        mMax = true;
        handle_mode_display();
        mEngine.showMaxValue();
    }

    /**
     * Display appropriate mode in the mode Text View
     */
    private void handle_mode_display() {
        String modeString = "";

        if (mMode) {
            modeString = "SLOW";
        } else {
            modeString = "FAST";
        }

        if (mCalib) {
            modeString += "    CALIB";
        } else {
            modeString += "              "; // 1 char = 2 spaces ; 1 space = 1
        }

        if (mLog) {
            modeString += "    LOG";
        } else {
            modeString += "          ";
        }

        if (mMax) {
            modeString = "MAX";
        }
        mSplModeTV.setText(modeString);
    }

    /**
     * Sets the SPL Meter Mode
     */
    public void setMeterMode(String mode) {
        mEngine.setMode(mode);
    }

    /**
     * Starts the SPL Meter
     */
    public void start_meter() {
        mCalib = false;
        mMax = false;
        mLog = false;
        mMode = false;
        mEngine = new SplEngine(mhandle, mContext);
        mEngine.start_engine();

        //Start Socket IO too
        try {
            mSocket = IO.socket("http://chat.socket.io");
            mSocket.connect();
        } catch (URISyntaxException e) {

        }
    }

    /**
     * Stops the SPL Meter
     */
    public void stop_meter() {
        if(mEngine != null) {
            mEngine.stop_engine();
        }
    }

    /**
     * Calibration Down Button Handler
     */
    private OnClickListener calibdown_button_handle = new OnClickListener() {

        public void onClick(View v) {
            AnimationSet set = new AnimationSet(true);

            Animation animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.01f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.09f);
            animation.setDuration(100);
            set.addAnimation(animation);

            mSplCalibDownButton.startAnimation(animation);

            mEngine.calibDown();

        }
    };

    /**
     * Calibration Up Button Handler
     */
    private OnClickListener calibup_button_handle = new OnClickListener() {

        public void onClick(View v) {
            AnimationSet set = new AnimationSet(true);

            Animation animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.01f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, -0.09f);
            animation.setDuration(100);
            set.addAnimation(animation);

            mSplCalibUpButton.startAnimation(animation);
            mEngine.calibUp();

        }
    };

    /**
     * Log Button Handler
     */
    private OnClickListener log_button_handle = new OnClickListener() {

        public void onClick(View v) {
            if (mLog) {
                mSplLogButton.setTextColor(Color.parseColor("#6D7B8D"));
                mLog = false;
                handle_log(false);

            } else {
                mSplLogButton.setTextColor(Color.BLACK);
                mLog = true;
                handle_log(true);
            }

            handle_mode_display();

        }
    };

    /**
     * Calibration Button Handler
     */
    private OnClickListener calib_button_handle = new OnClickListener() {

        public void onClick(View v) {
            if (mCalib) {
                mSplCalibButton.setTextColor(Color.parseColor("#6D7B8D"));
                mSplCalibUpButton.setVisibility(View.INVISIBLE);
                mSplCalibDownButton.setVisibility(View.INVISIBLE);
                mSplMaxButton.setVisibility(View.VISIBLE);
                mSplLogButton.setVisibility(View.VISIBLE);
                mCalib = false;
                mEngine.storeCalibvalue();
                Toast.makeText(mContext, "Calibration Saved.",
                        Toast.LENGTH_SHORT).show();
            } else {
                mSplCalibButton.setTextColor(Color.BLACK);
                mSplCalibUpButton.setVisibility(View.VISIBLE);
                mSplCalibDownButton.setVisibility(View.VISIBLE);
                mSplMaxButton.setVisibility(View.INVISIBLE);
                mSplLogButton.setVisibility(View.INVISIBLE);
                mCalib = true;

            }
            handle_mode_display();

        }
    };

    /**
     * Mode Button Handler
     */
    private OnClickListener mode_button_handle = new OnClickListener() {

        public void onClick(View v) {
            if (mMode) {
                mSplModeButton.setText("SLOW");
                mMode = false;
                setMeterMode("FAST");
            } else {
                mSplModeButton.setText("FAST");
                mMode = true;
                setMeterMode("SLOW");
            }
            handle_mode_display();
        }
    };

    /**
     * ON/OFF Button Handler
     */
    private OnClickListener start_button_handle = new OnClickListener() {

        public void onClick(View v) {
            if (mSplOnOffButton.getText().equals("ON")) {
                mSplOnOffButton.setText("OFF");
                mSplOnOffButton.setTextColor(Color.BLACK);
                mSplModeTV.setText("FAST");
                mSplModeTV.setBackgroundResource(R.drawable.lcd4);
                mSplDataTV.setText("");
                mSplDataTV.setTextColor(Color.WHITE);
                //mSplDataTV.setBackgroundResource(R.drawable.lcd2);
                mSplCalibButton.setVisibility(View.VISIBLE);
                mSplLogButton.setVisibility(View.VISIBLE);
                mSplMaxButton.setVisibility(View.VISIBLE);
                mSplModeButton.setVisibility(View.VISIBLE);
                mSplModeButton.setText("SLOW");

                start_meter();

            } else {
                stop_meter();
                mSplOnOffButton.setText("ON");
                mSplOnOffButton.setTextColor(Color.parseColor("#6D7B8D"));
                mSplModeTV.setText("");
                mSplModeTV.setBackgroundColor(Color.GRAY);
                mSplDataTV.setText("");
                mSplDataTV.setTextColor(Color.GRAY);
                mSplDataTV.setBackgroundColor(Color.GRAY);
                mSplCalibButton.setVisibility(View.INVISIBLE);
                mSplLogButton.setVisibility(View.INVISIBLE);
                mSplMaxButton.setVisibility(View.INVISIBLE);
                mSplModeButton.setVisibility(View.INVISIBLE);
                mSplCalibUpButton.setVisibility(View.INVISIBLE);
                mSplCalibDownButton.setVisibility(View.INVISIBLE);

            }
        }
    };

    /**
     * MAX Button Handler
     */
    private OnClickListener max_button_handle = new OnClickListener() {

        public void onClick(View v) {
            mSplMaxButton.setTextColor(Color.BLACK);
            display_max();
        }
    };

    public void sendToServer(String value) {
        mSocket.emit(EMIT_NAME, value);
    };

    /**
     * Handler for displaying messages
     */
    public Handler mhandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MY_MSG :
                    mSplDataTV.setText(" " + msg.obj);
                    sendToServer("" + msg.obj);
                    break;
                case MAXOVER_MSG :
                    mMax = false;
                    handle_mode_display();
                    mSplMaxButton.setTextColor(Color.parseColor("#6D7B8D"));
                    break;
                case ERROR_MSG:
                    Toast.makeText(
                            mContext,
                            "Error " + msg.obj, Toast.LENGTH_LONG).show();
                    stop_meter();
                    break;
                default :
                    super.handleMessage(msg);
                    break;
            }
        }

    };

    /**
     * Create the option Menu's
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        menu.add(PREFERENCES_GROUP_ID, RESET_OPTION, 0, "RESET").setIcon(
                android.R.drawable.ic_menu_revert);
        menu.add(PREFERENCES_GROUP_ID, ABOUT_OPTION, 0, "HELP").setIcon(
                android.R.drawable.ic_menu_help);
        menu.add(PREFERENCES_GROUP_ID, FEEDBACK_OPTION, 0, "FEEDBACK").setIcon(
                android.R.drawable.ic_dialog_email);
        menu.add(PREFERENCES_GROUP_ID, EXIT_OPTION, 0, "EXIT").setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);

        return true;
    }

    /**
     * Reset the SPL Meter
     */
    public void reset_meter() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Reset SPL Meter");
        alertDialog.setMessage("Do you want to reset the SPL Meter?");
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mEngine.stop_engine();
                mEngine.reset();
                mEngine = new SplEngine(mhandle, mContext);
                mEngine.start_engine();
                Toast.makeText(mContext, "Calibration reset",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        });

        alertDialog.setButton2("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        alertDialog.show();
    }

    /**
     * Display the ABOUT details
     */
    public void show_about() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("SPL Meter " + VERSION);
        String message = "Hashir N A \n"
                + "hashir@mobware4u.com \n"
                + "www.mobware4u.com\n\n"
                + "HOW TO CALIBRATE\n\n"
                + "The SPL Meter works with the phone's built in microphone, the headset or the bluetooth headset. You can adjust the calibration if you have a professional calibrated spl meter to compare it to."
                + "\n\nTHINGS REQUIRED"
                + "\n1. a professional calibrated spl meter ( reference meter )"
                + "\n2. a way to generate pink or white noise. You may use any available software for this purpose."
                + "\n\nTHE PROCESS"
                + "\n1. Press the CALIB button on the SPL Meter app."
                + "\n2. Set your reference meter to have the same settings as the spl meter. ( For eg. SLOW db SPL on both the meters )"
                + "\n3. Play pink or white noise through your system to get a reading on the reference meter."
                + "\n4. Now adjust the spl meter app using the up and down arrows to match the reading on the reference meter."
                + "\n5. Press the CALIB button again to save the settings."
                + "\n\nUse Menu-Reset to reset the calibrations if required."
                + "\n\nHOW TO SAVE"
                + "\n\nPress the LOG button to enable logging(saving) of the readings."
                + "\nPress LOG button again to stop logging."
                + "\nThe log would be saved as a xls file in /sdcard"
                + "\n\nMAX button-to display the maximum value observed so far."
                + "\n\nSLOW button-toggle between fast and slow modes.";

        alertDialog.setMessage(message);
        alertDialog.setIcon(R.drawable.icon);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        alertDialog.show();
    }


    /**
     * Display Feedback dialog
     */
    public void show_feedback()
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getResources().getString(R.string.app_name));
        String message = "Send us your feedback/enhancement requests/criticisms.." ;


        alertDialog.setMessage(message);
        alertDialog.setIcon(R.drawable.icon);
        alertDialog.setButton("Feedback", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                sendEmail();
            }
        });
        alertDialog.setButton2("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.show();
    }


    /**
     * Send email intent
     */
    private void sendEmail()
    {
        String subject = "Feedback " + getResources().getString(R.string.app_name);
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent .setType("plain/text");
        emailIntent .putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"hashir@mobware4u.com"});
        emailIntent .putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        emailIntent .putExtra(android.content.Intent.EXTRA_TEXT, "");
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }


    /**
     * Call back function when an menu option is selected.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case RESET_OPTION :
                reset_meter();
                break;
            case ABOUT_OPTION :
                show_about();
                break;
            case FEEDBACK_OPTION :
                show_feedback();
                break;
            case EXIT_OPTION :
                stop_meter();
                super.onDestroy();
                this.finish();
                getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;

        }
        return true;
    }
}