package mc.fhhgb.at.ev3;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lejos.hardware.Audio;
import lejos.hardware.DeviceException;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.remote.ev3.RemoteRequestEV3;
import lejos.remote.ev3.RemoteRequestSampleProvider;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;


/**
 * Class ConnectionActivity includes all necessary libraries of leJOS to
 * provide the control over a Lego Mindstorm EV3 robot via wireless LAN.
 * <p>
 * It contains a nested class Control for the asynchronous tasks
 * sending commands to the robot and receiving data from its sensors.
 */
public class ConnectionActivity extends AppCompatActivity implements View.OnClickListener {

    private String address;

    private RegulatedMotor leftMotor;
    private RegulatedMotor rightMotor;
    private RegulatedMotor shoot;
    private Audio audio;
    private EV3IRSensor irSensor;
    private SampleProvider irDistanceSampler;
    private RemoteRequestSampleProvider touchSampleProvider;
    private float[] irDistSamples;
    private float[] touchSample;

    // This variable is used to turn off the sensor input sampling
    // in case of connection interferences
    // true = sensors are offline, false = sensors are online
    private boolean isSafeMode = true;

    // Variable used to get feedback from the rear bumper
    // false = you can move backwards, true = sensor is pressed, you hit an obstacle
    private boolean stopMovingBackwards = false;

    // Variable to check if connection is established or not
    private boolean isConnected = false;

    // Left turn movement
    private boolean leftTurn;
    // Right turn movement
    private boolean rightTurn;
    // robot is moving forward
    private boolean moveForwards;
    // robot is moving backwards
    private boolean moveBackwards;

    // Service for the sensor data threads
    private final ExecutorService threads = Executors.newCachedThreadPool();
    private RemoteRequestEV3 ev3;

    TextView distance;
    Button connect;
    Button safeMode;
    ImageButton forward;
    ImageButton backward;
    ImageButton left;
    ImageButton right;
    ImageButton stop1;
    ImageButton stop2;
    ImageButton shoot1;
    ImageButton shoot2;


    /**
     * Entry point of ConnectionActivity
     * Defines the UI
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        // gets the previously created intent
        Intent myIntent = getIntent();
        // get the IP address as parameter
        address = myIntent.getStringExtra("IP");

        distance = (TextView) findViewById(R.id.distance);
        forward = (ImageButton) findViewById(R.id.forward);
        backward = (ImageButton) findViewById(R.id.backward);
        left = (ImageButton) findViewById(R.id.left);
        right = (ImageButton) findViewById(R.id.right);
        stop1 = (ImageButton) findViewById(R.id.stop1);
        stop2 = (ImageButton) findViewById(R.id.stop2);
        shoot1 = (ImageButton) findViewById(R.id.shoot1);
        shoot2 = (ImageButton) findViewById(R.id.shoot2);
        safeMode = (Button) findViewById(R.id.safe_mode);
        connect = (Button) findViewById(R.id.connect);

        stop1.setOnClickListener(this);
        stop2.setOnClickListener(this);
        forward.setOnClickListener(this);
        backward.setOnClickListener(this);
        left.setOnClickListener(this);
        right.setOnClickListener(this);
        shoot1.setOnClickListener(this);
        shoot2.setOnClickListener(this);
        safeMode.setOnClickListener(this);
        connect.setOnClickListener(this);

        if(stopMovingBackwards){
            new Control().execute("stop");
        }
        if (isConnected) {
            connect.setText("Disconnect");
        } else {
            connect.setText("Connect");
        }
    }

    /**
     * onPause is a possible state for an activity and should care
     * about the data and functions if the app is moved to the background.
     */
    @Override
    protected void onPause() {
        super.onPause();

        if (isConnected) {
          try {
              leftMotor.stop(true);
              rightMotor.stop(true);
              shoot.stop(true);
              isSafeMode = true;
              leftTurn = false;
              rightTurn = false;
              moveForwards = false;
              ev3.disConnect();
              isConnected = false;
              threads.shutdown();
          }catch(Exception e) {
              e.printStackTrace();
          }
        }
    }

    /**
     * Method closes all connections if activity is closed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isConnected) {
            try {
                leftMotor.stop(true);
                rightMotor.stop(true);
                shoot.stop(true);
                ev3.disConnect();
                ev3 = null;
                threads.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method stops all motors and closes connection to the brick
     * if hardware back button is pressed.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (isConnected) {
            try {
                leftMotor.stop(true);
                rightMotor.stop(true);
                shoot.stop(true);
                isSafeMode = true;
                leftTurn = false;
                rightTurn = false;
                moveForwards = false;
                ev3.disConnect();
                threads.shutdown();
                isConnected = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method onClick() is a necessary implementation because of the OnClickListener.
     * Defines which functions should be started when buttons are pressed.
     * <p>
     * These commands are sent to the Control class and therefore control the robot.
     *
     * @param v The actual view of the application
     */
    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.forward) {
            new Control().execute("forward");

        } else if (v.getId() == R.id.backward) {
            new Control().execute("backward");

        } else if (v.getId() == R.id.stop1 || v.getId() == R.id.stop2) {
            new Control().execute("stop");

        } else if (v.getId() == R.id.left) {
            new Control().execute("left");

        } else if (v.getId() == R.id.right) {
            new Control().execute("right");

        } else if (v.getId() == R.id.shoot1 || v.getId() == R.id.shoot2) {
            new Control().execute("shoot");

        } else if (v.getId() == R.id.safe_mode) {
            // This part is for the safe mode without
            // using robots sensors because of unknown crashes
            String safe;

            if (!isSafeMode) {
                isSafeMode = true;
                safe = "SafeMode is on";
            } else {
                isSafeMode = false;
                safe = "SafeMode is off";
            }
            // Output for the user
            Toast.makeText(this, safe, Toast.LENGTH_SHORT).show();

        } else {
            if (v.getId() == R.id.connect) {
                // This part establishes the connection to the brick
                if (ev3 == null && !isConnected) {
                    // connect to ip address
                    new Control().execute("connect", address);

                } else {
                    new Control().execute("disconnect");

                }
            }
        }
        if(stopMovingBackwards){
            new Control().execute("stop");
        }
        if (isConnected) {
            connect.setText("Disconnect");
        } else {
            connect.setText("Connect");
        }
    }


    /**
     * Nested class Control is used to establish the connection to the brick as asynchronous task.
     * Also the controls to steer the robot, let it shoot and get data output from the sensors
     * is managed from this class.
     * <p>
     * Created by WG on 15.06.2016.
     */
    public class Control extends AsyncTask<String, Integer, Long> {

        /**
         * Classes which extends AsyncTask base class have to implement this function:
         * doInBackground calls another method which uses threading to provide
         * sensor connection and data output
         *
         * @param cmd Command that controls the robot functions such as connect, move,
         *            stop and shoot.
         * @return error commands for the user information
         */
        protected Long doInBackground(String... cmd) {

            if (cmd[0].equals("connect")) {
                try {
                    // establish connection to brick, cmd[1] = IP address
                    ev3 = new RemoteRequestEV3(cmd[1]);

                    leftMotor = ev3.createRegulatedMotor("B", 'L'); // large motor on port B
                    rightMotor = ev3.createRegulatedMotor("A", 'L'); // large motor on port A
                    shoot = ev3.createRegulatedMotor("C", 'M'); // middle motor on port C
                    audio = ev3.getAudio(); // get access to audio
                    audio.systemSound(2);// beep when connection is established
                    // function to get sensors connected and sensor data
                    monitorSensorData();
                    isConnected = true;

                    return 0l;
                } catch (IOException e) {
                    return 1l;
                }

            } else if (cmd[0].equals("disconnect") && ev3 != null) {
                audio.systemSound(2); // beep when disconnecting
                leftMotor.close(); // close motor left
                rightMotor.close(); // close motor right
                shoot.close(); // close motor on port C
                ev3.disConnect();
                ev3 = null; // set brick to null
                isConnected = false;

                return 0l;
            }

            if (ev3 == null) return 2l;

            ev3.getAudio().systemSound(1);

            if (cmd[0].equals("stop")) {
                // Control command to stop motors
                leftMotor.stop(true);
                rightMotor.stop(true);
                leftTurn = false;
                rightTurn = false;
                moveBackwards = false;
                moveForwards = false;

            } else if (cmd[0].equals("backward")) {
                // Control command to move robot back

                if (!stopMovingBackwards) {
                    leftMotor.backward();
                    rightMotor.backward();
                    moveBackwards = true;
                } else {
                    leftMotor.stop();
                    rightMotor.stop();
                    moveBackwards = false;
                }
                moveForwards = false;
                leftTurn = false;
                rightTurn = false;

            } else if (cmd[0].equals("left")) {
                // Control command to spin robot to the left
                if (rightTurn) {
                    leftMotor.stop();
                    rightTurn = false;
                } else if (moveForwards) {
                    rightMotor.forward();
                    leftMotor.stop();
                    leftTurn = true;
                    moveForwards = false;
                } else if (moveBackwards) {
                    leftMotor.backward();
                    rightMotor.stop();
                    leftTurn = true;
                    moveBackwards = false;
                } else if (stopMovingBackwards) {
                    rightMotor.stop();
                    leftTurn = false;
                } else {
                    rightMotor.forward();
                    leftTurn = true;
                    moveForwards = false;
                    moveBackwards = false;

                }

            } else if (cmd[0].equals("forward")) {
                // Control command to move robot forward
                leftMotor.forward();
                rightMotor.forward();
                rightTurn = false;
                leftTurn = false;
                moveForwards = true;
                moveBackwards = false;

            } else if (cmd[0].equals("right")) {
                // Control command to spin robot to the right
                if (leftTurn) {
                    rightMotor.stop();
                    leftTurn = false;
                } else if (moveForwards) {
                    leftMotor.forward();
                    rightMotor.stop();
                    rightTurn = true;
                    moveForwards = false;

                } else if (moveBackwards) {
                    rightMotor.backward();
                    leftMotor.stop();
                    rightTurn = true;
                    moveBackwards = false;
                } else if (stopMovingBackwards) {
                    leftMotor.stop();
                    rightTurn = false;
                } else {
                    leftMotor.forward();
                    rightTurn = true;
                    moveForwards = false;
                    moveBackwards = false;
                }

            } else if (cmd[0].equals("shoot")) {
                // Control command to rotate motor C to shoot
                shoot.rotate(1080); // 1080 degree rotation for one ball
                shoot.stop(); // then stop
            }
            return 0l;
        }

        /**
         * Method onPostExecute defines error messages for the error codes
         * sent by doInBackground()
         *
         * @param result the error code from function doInBackground to generate user messages
         */
        protected void onPostExecute(Long result) {

            if (result == 1l)
                Toast.makeText(ConnectionActivity.this, "Could not connect to EV3 Address:" + address.toString(), Toast.LENGTH_LONG).show();
            else if (result == 2l)
                Toast.makeText(ConnectionActivity.this, "Not connected", Toast.LENGTH_LONG).show();

        }

        /**
         * Method establish´s a connection to the used IRSensor and TouchSensor.
         * The IRSensor delivers samples to display the distance to an object on screen,
         * the TouchSensor is an analog sensor and delivers 1 or 0 for pushed and not pushed.
         * Is the sensor activated is it no longer possible to move backwards.
         */
        protected void monitorSensorData() {

            // Try to establish connection to the IRSensor and the TouchSensor
            // The used ports on the brick are hardcoded because the sensor search function
            // does not work properly in the actual version of lejos.
            // saveMode disables all sensor functions
            if (!isSafeMode) {
                try {
                    // get new IR sensor
                    irSensor = new EV3IRSensor(ev3.getPort("S1"));
                    // get new touch sensor and remote sampler
                    touchSampleProvider = (RemoteRequestSampleProvider) ev3.createSampleProvider("S4", "lejos.hardware.sensor.EV3TouchSensor", "Touch");
                } catch (DeviceException e) {
                    e.printStackTrace();
                }
            }

            // set mode and sample rate for IRSensor
            if (!isSafeMode) {
                // set mode and sampler for measuring distance
                irDistanceSampler = irSensor.getDistanceMode();
                irDistSamples = new float[irDistanceSampler.sampleSize()];
                // set sampler for touch sensor
                touchSample = new float[touchSampleProvider.sampleSize()];
            }

            // Executor service gets a new thread to run
            threads.submit(new Runnable() {
                @Override
                public void run() {

                    while (!threads.isShutdown()) {
                        if (!isSafeMode) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(50);

                                // get samples from both sensors
                                irDistanceSampler.fetchSample(irDistSamples, 0);
                                touchSampleProvider.fetchSample(touchSample, 0);
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        if (!isSafeMode) {

                                            // set text color depending on distance to obstacle
                                            if (irDistSamples[0] < 20.0f) {
                                                distance.setTextColor(Color.RED);
                                            } else {
                                                distance.setTextColor(Color.GREEN);
                                            }
                                            // show distance
                                            distance.setText("" + irDistSamples[0]); // IR distance

                                            // set StopMovingBackwards to true if touch sensor is pressed
                                            stopMovingBackwards = touchSample[0] == 1.0;
                                        }
                                    }
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // Output if save mode is active so the user gets informed that
                            // there is no connection to the sensors
                            distance.setText("---");
                        }
                    }
                    // close any existing sensor in this thread
                    if (irSensor != null) {
                        irSensor.close();
                    }
                    if (touchSampleProvider != null) {
                        touchSampleProvider.close();
                    }
                    // close connection to the brick
                    if (ev3 != null) {
                        ev3 = null;
                        threads.shutdown();
                    }
                }
            });
        }
    }
}
