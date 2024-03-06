package wiki.catz.pedosoundeffect;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import wiki.catz.pedosoundeffect.databinding.ActivityMainBinding;
@SuppressLint("SetTextI18n")
public class MainActivity extends Activity implements SensorEventListener, StepListener {

    private TextView mTextView;
    private TextView acc_xTV;
    private TextView acc_yTV;
    private TextView acc_zTV;
    private TextView mostSignificantAxisTV;
    private Button statusBtn;
    private ActivityMainBinding binding;
    private AudioManager audioManager;
    private MediaPlayer[] mediaPlayer = new MediaPlayer[3];
    private boolean canUseBuiltinSpeakers = false;
    private Boolean noPlayMedia = false;
    private boolean everyStep = false;
    private boolean no_stepDetector = false;
    private boolean no_stepCounter = false;
    private boolean noUseStepCounter = true;
    private boolean noUseStepDetector = true;
    private double lastAX, lastAY, lastAZ;
    private double threshold = 0.048d; // fine tuned.
    private SimpleStepDetector simpleStepDetector;
    float lastStepCount = 0;
    int stepSet = 5;
    Thread mainLoop;
    private SensorManager sensorManager;
    private Sensor stepDetectorSensor;
    private Sensor stepCounterSensor;
    private int stepCount = 0;
    private String[] conditionStrings = {
            "Off",
            "Every Step",
            "5 Steps",
            "10 Steps",
            "100 Steps",
            "1000 Steps",
            "1 Mile (NYI)"
    };
    private int conditionStringsIndex = 0;
    double ax,ay,az;   // these are the acceleration in x,y and z axis
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mTextView = binding.text;
        acc_xTV = binding.accX;
        acc_yTV = binding.accY;
        acc_zTV = binding.accZ;
        mostSignificantAxisTV = binding.significantAxisTV;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        statusBtn = binding.GoatScreamBtn;
        statusBtn.setText(conditionStrings[conditionStringsIndex]);
        Sensor heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        acc_zTV.setText("noPlayMedia: " + noPlayMedia.toString());
        statusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (conditionStringsIndex != 0) {
                    noPlayMedia = false;
                } else {
                    noPlayMedia = true;
                }
                if (conditionStringsIndex == 0) {
//                    noPlayMedia = true;
                    acc_zTV.setText("noPlayMedia: " + noPlayMedia.toString());
                } else {
//                    noPlayMedia = false;
                }

                if (conditionStringsIndex == 1) {
                    noUseStepDetector = false;
                    noUseStepCounter = true;
                    stepSet = Integer.MAX_VALUE;
                } else {
                    noUseStepDetector = true;
                    noUseStepCounter = false;
                    stepSet = 5;
                }
                if (conditionStringsIndex == 2) {
                    stepSet = 5;
                    noUseStepCounter = false;
                }
                if (conditionStringsIndex == 3) {
                    stepSet = 10;
                    noUseStepCounter = false;
                }
                if (conditionStringsIndex == 4) {
                    stepSet = 100;
                    noUseStepCounter = false;
                }
                if (conditionStringsIndex == 5) {
                    stepSet = 1000;
                    noUseStepCounter = false;
                }
                if (conditionStringsIndex == conditionStrings.length - 1) {
                    conditionStringsIndex = 0;
                } else {
                    conditionStringsIndex++;
                }
                statusBtn.setText(conditionStrings[conditionStringsIndex]);
                acc_zTV.setText("noPlayMedia: " + noPlayMedia.toString());
                mostSignificantAxisTV.setText("conStrIndex: " + conditionStringsIndex);
                playMedia();
            }
        });
        mediaPlayer[0] = MediaPlayer.create(getApplicationContext(), R.raw.goat_scream);
        mediaPlayer[1] = MediaPlayer.create(getApplicationContext(), R.raw.goat_scream);
        mediaPlayer[2] = MediaPlayer.create(getApplicationContext(), R.raw.goat_scream);
        mTextView.setText("MEOW!!!");

        if (audioOutputAvailable(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)) {
            acc_xTV.setText("I can use built in speakers!");
            canUseBuiltinSpeakers = true;
        } else {
            acc_xTV.setText("Something's gone wrong. Good lick!");
        }

//        sensorManager2 =  (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        simpleStepDetector = new SimpleStepDetector();
        simpleStepDetector.registerListener(this);
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepDetectorSensor == null) {
            no_stepDetector = true;
            acc_xTV.setText("Can't use stepDetector");
        } else {
            acc_xTV.setText("Can use stepDetector");
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepCounterSensor == null) {
            no_stepCounter = true;
            acc_yTV.setText("Can't use stepCounter");
        } else {
            acc_yTV.setText("Can use stepCounter");
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }



        //sensorManager.requestTriggerSensor(triggerEventListener, sensor);
        mainLoop = createNewMainLoop();

        //mainLoop.start();
    }

    private void checkAccelerometer() {
        Log.println(Log.VERBOSE, "MAIN_LOOP", "Working");
        acc_xTV.setText(ax + "");
        acc_yTV.setText(ay + "");
        acc_zTV.setText(az + "");

    }

    private void playMedia() {
        if (noPlayMedia)
            return;
        //mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.goat_scream);
        if (mediaPlayer[0] != null && mediaPlayer[1] != null && mediaPlayer[2] != null) {
            if (!mediaPlayer[0].isPlaying()) {
                mediaPlayer[0].start();
            } else if (!mediaPlayer[1].isPlaying()) {
                mediaPlayer[1].start();
            } else {
                mediaPlayer[2].start();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            playMedia();
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            simpleStepDetector.updateAccel(
//                    event.timestamp, event.values[0], event.values[1], event.values[2]);
            final float alpha = 0.1f;
            double[] gravity = new double[3]; // = double[3];
            double[] linear_acceleration = new double[3]; // = double[3];
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];
            lastAX = ax;
            lastAY = ay;
            lastAZ = az;
            ax = linear_acceleration[0];
            ay = linear_acceleration[1];
            az = linear_acceleration[2];
            /*
            ax=event.values[0];
            ay=event.values[1];
            az=event.values[2];
             */
            acc_xTV.setText(ax + "");
            acc_yTV.setText(ay + "");
            acc_zTV.setText(az + "");
            byte mostSignificantAxis = 0;
            if (ax > ay && ax > az) {
                //mostSignificantAxisTV.setText("Significant Axis: X");
                mostSignificantAxis = 0;
            } else if (ay > ax && ay > az) {
//                mostSignificantAxisTV.setText("Significant Axis: Y");
                mostSignificantAxis = 1;
            } else if (az > ax && az > ay) {
//                mostSignificantAxisTV.setText("Significant Axis: Z");
                mostSignificantAxis = 2;
            } else {
                mostSignificantAxis = 3;
            }
            /*if (Math.abs(ax) > Math.abs(ay) && Math.abs(ax) > Math.abs(az)) {
                mostSignificantAxisTV.setText("Significant Axis: X");
                mostSignificantAxis = 0;
            } else if (Math.abs(ay) > Math.abs(az) && Math.abs(ay) > Math.abs(az)) {
                mostSignificantAxisTV.setText("Significant Axis: Y");
                mostSignificantAxis = 1;
            } else if (Math.abs(az) > Math.abs(ay) && Math.abs(az) > Math.abs(ax)) {
                mostSignificantAxisTV.setText("Significant Axis: Z");
                mostSignificantAxis = 2;
            } else {
                mostSignificantAxis = 3;
            }*/
            //2mostSignificantAxisTV.setText("conStrIndex: " + conditionStringsIndex);
            if (mostSignificantAxis == 0) {// x axis
                if (Math.abs(Math.abs(ax) - Math.abs(lastAX)) >= threshold) {
                    step(0);
                }
            } else if (mostSignificantAxis == 1) {// y axis
                if (Math.abs(Math.abs(ay) - Math.abs(lastAY)) >= threshold) {
                    step(0);
                }
            } else if (mostSignificantAxis == 2) {// z axis
                if (Math.abs(Math.abs(az) - Math.abs(lastAZ)) >= threshold) {
                    step(0);
                }
            }

//            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.goat_scream);
//            mediaPlayer.start();
        }
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            acc_yTV.setText("StepCount: " + event.values[0]);
            if (event.values[0] - lastStepCount >= stepSet) {
                lastStepCount = event.values[0];
                playMedia();
            }
        }
//        if (no_stepDetector == false && noUseStepDetector == false) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            playMedia();
            acc_xTV.setText("StepDetect Val: " + event.values[0]);
            if (event.values[0] == 1.0) {
                playMedia();
            }
        }
//        }
    }

    private void stepTaken() {
        stepCount++;
        playMedia();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mainLoop.interrupt();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*Log.i("MAIN", "is mainLoop Thread alive: " + mainLoop.isAlive());
        if (!mainLoop.isAlive()) {
            mainLoop = createNewMainLoop();
            mainLoop.start();
        }*/
    }

    public Thread createNewMainLoop() {
        return new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    while (!interrupted()) {
                        checkAccelerometer();
                    }
                } catch (Exception e) {
                    Log.e("MAIN_LOOP", e.getMessage() + "\r\n" + e.getCause());
                }
            }
        };
    }
    public Boolean audioOutputAvailable(int type) {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            return false;
        }
        AudioDeviceInfo[] audioDeviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo deviceInfo : audioDeviceInfo) {
            if (deviceInfo.getType() == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void step(long timeNs) {
        stepCount++;
        mostSignificantAxisTV.setText(stepCount + " steps");
        if (!no_stepDetector) {
            playMedia();
        } else{
            if (stepCount - lastStepCount >= stepSet) {
                playMedia();
                lastStepCount = stepCount;
            }
        }
    }
}