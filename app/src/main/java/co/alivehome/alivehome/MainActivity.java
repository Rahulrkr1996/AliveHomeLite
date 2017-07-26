package co.alivehome.alivehome;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;

import static co.alivehome.alivehome.LoginActivity.ADDRESS;
import static co.alivehome.alivehome.LoginActivity.PASSWORD;
import static co.alivehome.alivehome.LoginActivity.USERNAME;
import static co.alivehome.alivehome.LoginActivity.USER_INFO;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private int backPressedCount = 0;
    private ProgressDialog pd;
    private ImageView home_help, home_settings, home_audio;
    private ImageView home_bulb_image;
    private ImageView home_fan_image;
    private ImageButton home_fan_speed1, home_fan_speed2, home_fan_speed3, home_fan_speed4, home_fan_speed5;
    private static final String TAG = "MainActivity";
    private Handler handler = new Handler();
    private boolean userAuthenticated = false;
    private SharedPreferences sharedPreferences;

    private String username = null, password = null, address = null;
    private String[] data_parsed = null;
    private String transfer_session = "";
    private String shared_aes_encryption_key;
    private WebSocketConnection mConnection = new WebSocketConnection();
    private String BULB_STATE = null;
    private String FAN_STATE = null;

    private boolean webSocketConnected = false;

    // BLe variables
    public static boolean BLEConnected = false;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    // Code to manage Service lifecycle.
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };
    private Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle user_data = getIntent().getExtras();
        if (user_data == null) {
            UnAuthenticateUser("Error!!");
        } else {
            username = user_data.getString(USERNAME);
            password = user_data.getString(PASSWORD);
            pd = new ProgressDialog(this);
            pd.show();
            pd.setMessage("Receiving Device States!! Please Wait...");
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (pd.isShowing())
                        UnAuthenticateUser("Cannot Receieve Device States!! Login Again...");
                }
            }, 8000);
            LoginToServer();

            home_bulb_image = (ImageView) findViewById(R.id.home_bulb_image);

            home_fan_image = (ImageView) findViewById(R.id.home_fan_image);
            home_fan_speed1 = (ImageButton) findViewById(R.id.home_fan_speed1);
            home_fan_speed2 = (ImageButton) findViewById(R.id.home_fan_speed2);
            home_fan_speed3 = (ImageButton) findViewById(R.id.home_fan_speed3);
            home_fan_speed4 = (ImageButton) findViewById(R.id.home_fan_speed4);
            home_fan_speed5 = (ImageButton) findViewById(R.id.home_fan_speed5);

            home_settings = (ImageView) findViewById(R.id.home_settings);
            home_audio = (ImageView) findViewById(R.id.home_audio);
            home_help = (ImageView) findViewById(R.id.home_help);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);


            home_settings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Feature will be available very soon!!", Toast.LENGTH_SHORT).show();
                }
            });
            home_help.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Feature will be available very soon!!", Toast.LENGTH_SHORT).show();
                }
            });

            //For speech rec
            home_audio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "To be added!!", Toast.LENGTH_SHORT).show();
                    // promptSpeechInput();
                }
            });

            home_bulb_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectBluetooth(address);

                    if (webSocketConnected) {
                        if (BULB_STATE == "TL_ON")
                            changeBulb(false, true);
                        else
                            changeBulb(true, true);
                    } else {
                        if (!isWiFiON()) {
                            try {
                                enableWifi();
                            } catch (Exception e) {
                                Log.d(TAG, "WebSocket Closed!!!");
                            }
                        } else {
                            AttemptLogin();
                        }
                    }
                }
            });

            home_fan_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectBluetooth(address);

                    if (FAN_STATE == "FAN_OFF") {
                        if (webSocketConnected) {
                            changeFanSpeed(1, true);
                        } else {
                            if (!isWiFiON()) {
                                try {
                                    enableWifi();
                                } catch (Exception e) {
                                    Log.d(TAG, "WebSocket Closed!!!");
                                }
                            } else {
                                AttemptLogin();
                            }
                        }
                    } else {
                        if (webSocketConnected) {
                            changeFanSpeed(0, true);
                        } else {
                            if (!isWiFiON()) {
                                try {
                                    enableWifi();
                                } catch (Exception e) {
                                    Log.d(TAG, "WebSocket Closed!!!");
                                }
                            } else {
                                AttemptLogin();
                            }
                        }
                    }
                }
            });
            home_fan_speed1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectBluetooth(address);

                    if (webSocketConnected) {
                        changeFanSpeed(1, true);
                    } else {
                        if (!isWiFiON()) {
                            try {
                                enableWifi();
                            } catch (Exception e) {
                                Log.d(TAG, "WebSocket Closed!!!");
                            }
                        } else {
                            AttemptLogin();
                        }
                    }
                }
            });
            home_fan_speed2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectBluetooth(address);

                    if (webSocketConnected) {
                        changeFanSpeed(2, true);
                    } else {
                        if (!isWiFiON()) {
                            try {
                                enableWifi();
                            } catch (Exception e) {
                                Log.d(TAG, "WebSocket Closed!!!");
                            }
                        } else {
                            AttemptLogin();
                        }
                    }
                }
            });
            home_fan_speed3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectBluetooth(address);

                    if (webSocketConnected) {
                        changeFanSpeed(3, true);
                    } else {
                        if (!isWiFiON()) {
                            try {
                                enableWifi();
                            } catch (Exception e) {
                                Log.d(TAG, "WebSocket Closed!!!");
                            }
                        } else {
                            AttemptLogin();
                        }
                    }
                }
            });
            home_fan_speed4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectBluetooth(address);

                    if (webSocketConnected) {
                        changeFanSpeed(4, true);
                    } else {
                        if (!isWiFiON()) {
                            try {
                                enableWifi();
                            } catch (Exception e) {
                                Log.d(TAG, "WebSocket Closed!!!");
                            }
                        } else {
                            AttemptLogin();
                        }
                    }
                }
            });
            home_fan_speed5.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectBluetooth(address);

                    if (webSocketConnected) {
                        changeFanSpeed(5, true);
                    } else {
                        if (!isWiFiON()) {
                            try {
                                enableWifi();
                            } catch (Exception e) {
                                Log.d(TAG, "WebSocket Closed!!!");
                            }
                        } else {
                            AttemptLogin();
                        }
                    }
                }
            });
        }
    }

    public void changeBulb(boolean state, boolean controlling) {
        if (controlling == true) {
            String BULB_STATE_TEMP = "";
            if (state == false)
                BULB_STATE_TEMP = "TL_OFF";
            else
                BULB_STATE_TEMP = "TL_ON";

            mConnection.sendTextMessage(encryption("CTRL-" + username + "-" + BULB_STATE_TEMP + "-" + FAN_STATE + "-" + transfer_session, shared_aes_encryption_key));
        } else if (state == true) {
            home_bulb_image.setImageResource(R.drawable.bulb_on);
            BULB_STATE = "TL_ON";
        } else if (state == false) {
            home_bulb_image.setImageResource(R.drawable.bulb_off);
            BULB_STATE = "TL_OFF";
        }
    }

    public void changeFanSpeed(int speed, boolean controlling) {
        if (controlling == true) {
            String FAN_STATE_TEMP = "";
            if (speed == 0) {
                FAN_STATE_TEMP = "FAN_OFF";
            } else if (speed == 1) {
                FAN_STATE_TEMP = "FAN_ON_1";
            } else if (speed == 2) {
                FAN_STATE_TEMP = "FAN_ON_2";
            } else if (speed == 3) {
                FAN_STATE_TEMP = "FAN_ON_3";
            } else if (speed == 4) {
                FAN_STATE_TEMP = "FAN_ON_4";
            } else if (speed == 5) {
                FAN_STATE_TEMP = "FAN_ON_5";
            }
            mConnection.sendTextMessage(encryption("CTRL-" + username + "-" + BULB_STATE + "-" + FAN_STATE_TEMP + "-" + transfer_session, shared_aes_encryption_key));
        } else if (speed == 0) {
            home_fan_image.setImageResource(R.drawable.ic_home_fan);

            home_fan_speed1.setBackgroundResource(R.color.fan_low);
            home_fan_speed2.setBackgroundResource(R.color.fan_low);
            home_fan_speed3.setBackgroundResource(R.color.fan_low);
            home_fan_speed4.setBackgroundResource(R.color.fan_low);
            home_fan_speed5.setBackgroundResource(R.color.fan_low);

            FAN_STATE = "FAN_OFF";
        } else if (speed == 1) {
            home_fan_image.setImageResource(R.drawable.ic_home_fan1);

            home_fan_speed1.setBackgroundResource(R.color.fan_high1);
            home_fan_speed2.setBackgroundResource(R.color.fan_low);
            home_fan_speed3.setBackgroundResource(R.color.fan_low);
            home_fan_speed4.setBackgroundResource(R.color.fan_low);
            home_fan_speed5.setBackgroundResource(R.color.fan_low);

            FAN_STATE = "FAN_ON_1";
        } else if (speed == 2) {
            home_fan_image.setImageResource(R.drawable.ic_home_fan2);

            home_fan_speed1.setBackgroundResource(R.color.fan_high1);
            home_fan_speed2.setBackgroundResource(R.color.fan_high2);
            home_fan_speed3.setBackgroundResource(R.color.fan_low);
            home_fan_speed4.setBackgroundResource(R.color.fan_low);
            home_fan_speed5.setBackgroundResource(R.color.fan_low);

            FAN_STATE = "FAN_ON_2";
        } else if (speed == 3) {
            home_fan_image.setImageResource(R.drawable.ic_home_fan3);

            home_fan_speed1.setBackgroundResource(R.color.fan_high1);
            home_fan_speed2.setBackgroundResource(R.color.fan_high2);
            home_fan_speed3.setBackgroundResource(R.color.fan_high3);
            home_fan_speed4.setBackgroundResource(R.color.fan_low);
            home_fan_speed5.setBackgroundResource(R.color.fan_low);

            FAN_STATE = "FAN_ON_3";
        } else if (speed == 4) {
            home_fan_image.setImageResource(R.drawable.ic_home_fan4);

            home_fan_speed1.setBackgroundResource(R.color.fan_high1);
            home_fan_speed2.setBackgroundResource(R.color.fan_high2);
            home_fan_speed3.setBackgroundResource(R.color.fan_high3);
            home_fan_speed4.setBackgroundResource(R.color.fan_high4);
            home_fan_speed5.setBackgroundResource(R.color.fan_low);

            FAN_STATE = "FAN_ON_4";
        } else if (speed == 5) {
            home_fan_image.setImageResource(R.drawable.ic_home_fan5);

            home_fan_speed1.setBackgroundResource(R.color.fan_high1);
            home_fan_speed2.setBackgroundResource(R.color.fan_high2);
            home_fan_speed3.setBackgroundResource(R.color.fan_high3);
            home_fan_speed4.setBackgroundResource(R.color.fan_high4);
            home_fan_speed5.setBackgroundResource(R.color.fan_high5);

            FAN_STATE = "FAN_ON_5";
        }
    }

    public boolean isWiFiON() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!mWifi.isConnected()) {
            return false;
        }
        return true;
    }

    public boolean isBTON() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            return true;
        }
        return false;
    }

    public void enableBT() {
        if (!isBTON()) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothAdapter.enable();
        }
    } //Do nothing if BT is enabled

    public void enableWifi() {
        if (!isWiFiON()) {
            mConnection.disconnect();
            Toast.makeText(this, "Connecting WiFi!!", Toast.LENGTH_SHORT).show();
            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifi.setWifiEnabled(true); // true or false to activate/deactivate wifi
        }
    }   //Do nothing if WiFi is enabled

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean connectBluetooth(final String DeviceAddress) {

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                if (!mBluetoothLeService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }
                // Automatically connects to the device upon successful start-up initialization.
                mBluetoothLeService.connect(DeviceAddress);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mBluetoothLeService = null;
            }
        };

        mGattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

                } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

                } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    if (mBluetoothLeService != null) {
                        final BluetoothGattCharacteristic characteristic = mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(0);
                        final int charaProp = mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(0).getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        }
                    }
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    /**-- Todo --*/
                }
            }
        };
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth not supported. Alive sensing won't work!!", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "BLe not supported. Alive sensing won't work!!", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            enableBT();
        }
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(DeviceAddress);
            return result;
        }
        return false;
    }

    public void disconnectBluetooth() {
        Toast.makeText(this, "BLE Disconnected!!", Toast.LENGTH_SHORT).show();
        if (BLEConnected == true) {
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
            BLEConnected = false;
        }
    }

    public boolean device_address_present() {
        sharedPreferences = getSharedPreferences(USER_INFO, Context.MODE_PRIVATE);
        address = sharedPreferences.getString(ADDRESS, null);

        if (TextUtils.isEmpty(address))
            return false;
        else
            return true;
    }

    public void LoginToServer() {
        final String wsuri = "ws://10.124.195.9:80";

        try {
            mConnection.connect(wsuri, new WebSocketConnectionHandler() {

                @Override
                public void onOpen() {
                    webSocketConnected = true;

                    shared_aes_encryption_key = shared_key_generator();
                    try {
                        mConnection.sendTextMessage(encryptCrypto("LOGI-" + username + "-" + password + "-" + shared_aes_encryption_key));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        e.printStackTrace();
                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();
                    }
                    try {

                        mConnection.sendTextMessage(encryptCrypto("ENQ-" + username + "-" + shared_aes_encryption_key));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        e.printStackTrace();
                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();
                    }

                    webSocketConnected = true;
                }

                @Override
                public void onTextMessage(String payload) {
                    String decrypted_data = decryption(payload, shared_aes_encryption_key);
                    if (decrypted_data != null) {
                        data_parsed = decrypted_data.split("-");
                        int size = data_parsed.length;
                        
                        if (data_parsed[0].equals(String.valueOf("PROXIMITY"))) {
                            if (data_parsed[2].equals(String.valueOf("Connected"))) {
                                timer = new Timer();
                                BLEConnected = true;
                            } else if (data_parsed[2].equals(String.valueOf("Disconnected"))) {
                                disconnectBluetooth();
                                if (device_address_present()) {
                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            if (!isBTON()) {
                                                enableBT();
                                                handler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        connectBluetooth(address);
                                                    }
                                                }, 5000);
                                            } else {
                                                if (!BLEConnected)
                                                    connectBluetooth(address);
                                            }
                                        }
                                    }, 4000);
                                }
                            }
                        } else if (data_parsed[0].equals(String.valueOf("VERIFY"))) {
                            if (data_parsed[1].equals(String.valueOf("True"))) {
                                if ((size > 2) && data_parsed[2].equals(String.valueOf("STATUS"))) {
                                    if (data_parsed[3].equals(String.valueOf("TL_ON"))) {
                                        changeBulb(true, false);
                                    } else if (data_parsed[3].equals(String.valueOf("TL_OFF"))) {
                                        changeBulb(false, false);
                                    }


                                    if (data_parsed[4].equals("FAN_OFF")) {
                                        changeFanSpeed(0, false);
                                    } else if (data_parsed[4].equals("FAN_ON_1")) {
                                        changeFanSpeed(1, false);
                                    } else if (data_parsed[4].equals("FAN_ON_2")) {
                                        changeFanSpeed(2, false);
                                    } else if (data_parsed[4].equals("FAN_ON_3")) {
                                        changeFanSpeed(3, false);
                                    } else if (data_parsed[4].equals("FAN_ON_4")) {
                                        changeFanSpeed(4, false);
                                    } else if (data_parsed[4].equals("FAN_ON_5")) {
                                        changeFanSpeed(5, false);
                                    }
                                    if (!userAuthenticated)
                                        AuthenticateUser("User Credentials Saved!!");
                                } else if ((size > 2) && data_parsed[2].equals("BLEMAC")) {
                                    address = data_parsed[3].substring(0, 17);
                                    connectBluetooth(address);

                                    sharedPreferences = getSharedPreferences(USER_INFO, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString(ADDRESS, address);
                                    editor.apply();
                                    int i = 2;
                                    while (i > 0) {
                                        mConnection.sendTextMessage(encryption("sessionRequest-" + username, shared_aes_encryption_key));
                                        i--;
                                    }
                                }

                            } else if (new String("False").equals(data_parsed[1])) {
                                /**-- To-Do --*/
                                UnAuthenticateUser("Username or Password incorrect!!");
                            }
                        } else if (data_parsed[0].equals(String.valueOf("NOTIFY"))) { // User already SignedIn
                            //Do Nothing
                        } else if (data_parsed[0].equals("session")) {
                            transfer_session = data_parsed[1];
                            mConnection.sendTextMessage(encryption("STATUS-" + username + "-" + transfer_session, shared_aes_encryption_key));
                        } else if (data_parsed[0].equals("ERROR")) {
                            //Toast.makeText(MainActivity.this, data_parsed[1], Toast.LENGTH_SHORT).show();
                        } else {
                            UnAuthenticateUser(decrypted_data);
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason) {
                    /** To-Do */
                    mConnection.disconnect();
                    webSocketConnected = false;
                    UnAuthenticateUser("WiFi disconnected!!!");
                }
            });
        } catch (WebSocketException e) {
            UnAuthenticateUser("Cannot connect to server, please check network connectivity!!");
        }
    }

    private void UnAuthenticateUser(String toast_msg) {
        userAuthenticated = false;
        mConnection.sendTextMessage(encryption("LOGO-" + username + "-" + transfer_session, shared_aes_encryption_key));
        SharedPreferences sharedPreferences = getSharedPreferences(USER_INFO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putString(ADDRESS, address);
        editor.apply();
        if (pd.isShowing())
            pd.dismiss();

        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra(USERNAME, username);
        i.putExtra(PASSWORD, password);
        i.putExtra(ADDRESS, address);
        startActivity(i);
        Toast.makeText(this, toast_msg, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void AuthenticateUser(String toast_msg) {
        userAuthenticated = true;
        SharedPreferences sharedPreferences = getSharedPreferences(USER_INFO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USERNAME, username);
        editor.putString(PASSWORD, password);
        editor.putString(ADDRESS, address);
        editor.apply();
        Toast.makeText(this, toast_msg, Toast.LENGTH_SHORT).show();

        if (pd.isShowing())
            pd.dismiss();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (BLEConnected == false) {
                    if (isBTON()) {
                        if (device_address_present())
                            connectBluetooth(address);
                    } else {
                        if (device_address_present()) {
                            enableBT();
                            connectBluetooth(address);
                        }
                    }
                }
            }
        }, 0, 1000);
    }

    private void AttemptLogin() {
        mConnection = new WebSocketConnection();
        pd = new ProgressDialog(MainActivity.this);
        pd.show();
        LoginToServer();
        pd.setMessage("Receiving Device States!! Please Wait...");
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (pd.isShowing()) {
                    pd.dismiss();
                    Toast.makeText(MainActivity.this, "Cannot login to server, check connectivity", Toast.LENGTH_SHORT).show();
                }
            }
        }, 8000);

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public String encryptCrypto(String message) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException {
        InputStream is = getResources().openRawResource(R.raw.public_key);
        byte[] keyBytes = new byte[is.available()];
        is.read(keyBytes);
        is.close();
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey key = kf.generatePublic(spec);
        String s = message;
        Cipher c = Cipher.getInstance("RSA/ECB/OAEPPadding");
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] bytes = c.doFinal(s.getBytes());
        String encrypted = Base64.encodeToString(bytes, Base64.DEFAULT);
        return encrypted;
    }

    public static String encryption(String data, String passkey) {
        AESHelper.key = passkey;
        String encryptedData = "";
        try {
            encryptedData = AESHelper.encrypt_string(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedData;
    }

    public String decryption(String data, String passkey) {
        AESHelper.key = passkey;
        String decryptedData = null;
        try {
            decryptedData = AESHelper.decrypt_string(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedData;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (BLEConnected == false) {
            if (isBTON()) {
                if (device_address_present())
                    connectBluetooth(address);
            } else {
                if (device_address_present()) {
                    enableBT();
                    connectBluetooth(address);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (BLEConnected == false) {
            if (isBTON()) {
                if (device_address_present())
                    connectBluetooth(address);
            } else {
                if (device_address_present()) {
                    enableBT();
                    connectBluetooth(address);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetooth();
    }

    @NonNull
    public static String shared_key_generator() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = 12;
        char tempChar;
        for (int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (backPressedCount % 5 == 0) {
                Toast.makeText(this, "Use Home Button to exit!!", Toast.LENGTH_SHORT).show();

            }
            backPressedCount++;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Toast.makeText(this, "To be added soon!!", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_help) {
            Toast.makeText(this, "To be added soon!!", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_log_out) {
            SharedPreferences sharedPreferences = getSharedPreferences("user_info", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            UnAuthenticateUser("Logged Out!!");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
