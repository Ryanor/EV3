package mc.fhhgb.at.ev3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Class MainActivity is the start activity for EV3Control
 * In this activity the user chooses the IP address for the connection
 * to the robot. The IP is visible on the EV3 brick.
 * IP addresses can be selected from a dropdown menu or it is possible to
 * enter a new IP in an input field. New IP´s will be stored in the shared preferences.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String APP_PREFS = "Application_Preferences";
    private String address;

    private ArrayList<String> ipList = new ArrayList<>();

    Spinner spinner;
    EditText ipAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inputfield for the ip address
        ipAddress = (EditText) findViewById(R.id.ipadress);

        // Button and its listener
        Button select = (Button) findViewById(R.id.select);
        select.setOnClickListener(this);

        // Spinner and its listener
        spinner = (Spinner) findViewById(R.id.ipselection);
        spinner.setOnItemSelectedListener(this);

        // load all existing IP´s from shared preferences
        loadIpAddresses();

    }

    /**
     * onClick is the interface method for the OnClickListener
     * and sends the given IP address as input to the ConnectionActivity
     * to establish the wireless connection.
     * If not yet included stores the IP address to the shared preferences.
     *
     * @param view The actual view of the application
     */
    @Override
    public void onClick(View view) {

        // get address from input field
        address = ipAddress.getText().toString();

        // check if input address is a valid IP address
        if (Patterns.IP_ADDRESS.matcher(address).matches()) {

            // check if actual address is already existing in shared preferences
            if (!ipList.contains(address)) {
                ipList.add(address); // if not add to array list
                saveIpAdresses(); // and save it
            }

            // start ConnectionActivity and send IP address as parameter
            Intent intent = new Intent(this, ConnectionActivity.class);
            intent.putExtra("IP", address);
            startActivity(intent);
        } else {
            // Not a valid IP address? -> show error message to user
            Toast.makeText(MainActivity.this, "Enter valid IP address!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Let the user select an existing IP address for the connection to the robot.
     * The selected IP is written in the inputfield.
     *
     * @param adapterView Adapter for the spinner to select items from
     * @param view actual view of the application
     * @param i actual position of the item in the list
     * @param l unused parameter
     */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        // On selecting a spinner item set ip address to selected address
        address = adapterView.getItemAtPosition(i).toString();
        // show it in the input field
        ipAddress.setText(address);
    }


    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // Not implemented
    }

    /**
     * Method used to store new IP addresses into the shared preferences.
     *
     */
    public void saveIpAdresses() {
        SharedPreferences settings = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor;

        Set<String> set = new HashSet<>();
        set.addAll(ipList);

        editor = settings.edit();
        editor.putStringSet("IP", set);
        editor.apply();
    }

    /**
     * Method used to load already existing IP addresses from the shared preferences
     * and populate the items for the spinner.
     * If no IP address is stored the first entry becomes 0.0.0.0
     */
    public void loadIpAddresses() {
        SharedPreferences settings = getSharedPreferences(APP_PREFS, MODE_PRIVATE);

        // get address from the storage
        Set<String> set = settings.getStringSet("IP", null);
        if (set == null) {
            ipList.add("0.0.0.0");
        } else {
            ipList.addAll(set);
        }

        ArrayAdapter<String> ipelements = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ipList);
        ipelements.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(ipelements);
    }
}
