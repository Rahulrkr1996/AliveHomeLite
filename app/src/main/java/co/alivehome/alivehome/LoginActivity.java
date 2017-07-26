package co.alivehome.alivehome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
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
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.tavendo.autobahn.WebSocketConnection;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    // UI references.
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Handler handler;
    private SharedPreferences sharedPreferences;
    public String username, password, address;

    private static final String TAG = "LoginActivity";

    private boolean loading = false;
    public static String LOGIN_STATUS = "user_exists";
    public static final String USERNAME = "username", PASSWORD = "password", ADDRESS = "address", USER_INFO = "user_info";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    // Store values at the time of the login attempt.
                    String username = mUsernameView.getText().toString();
                    String password = mPasswordView.getText().toString();

                    attemptLogin();

                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isBTON = isBTON(), isWiFiON = isWiFiON();
                if (isBTON && isWiFiON) {
                    attemptLogin();
                } else {
                    if (isBTON)
                        enableWifi();
                    else if (isWiFiON)
                        enableBT();
                    else {
                        enableBT();
                        enableWifi();
                    }

                }
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(address)) {
            mUsernameView.setText(username);
            mPasswordView.setText(password);

        }

        if (UserLoggedIn()) {
            showProgress(true);
            if (!isBTON() && !isWiFiON()) {
                enableBT();
                enableWifi();
                Toast.makeText(this, "Enabling Bluetooth and WiFi!!", Toast.LENGTH_SHORT).show();
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (loading)
                            showProgress(false);
                        GoToMainActivity(true);
                    }
                }, 3000);
            } else if (!isWiFiON()) {
                enableWifi();
                Toast.makeText(this, "Enabling WiFi!!", Toast.LENGTH_SHORT).show();
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (loading)
                            showProgress(false);
                        GoToMainActivity(true);
                    }
                }, 3000);
            } else if (!isBTON()) {
                enableBT();
                Toast.makeText(this, "Enabling BT!!", Toast.LENGTH_SHORT).show();
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (loading)
                            showProgress(false);
                        GoToMainActivity(true);
                    }
                }, 3000);
            } else {
                showProgress(false);
                GoToMainActivity(true);
            }
        } else {
            username = getIntent().getStringExtra(USERNAME);
            password = getIntent().getStringExtra(PASSWORD);
            address = getIntent().getStringExtra(ADDRESS);
            if (TextUtils.isEmpty(address)) {
                enableBT();
            }

            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                mUsernameView.setText(username);
                mPasswordView.setText(password);
            }
            address = getIntent().getStringExtra(ADDRESS);
        }

    }

    private void attemptLogin() {
        boolean cancel = false;
        View focusView = null;
        username = mUsernameView.getText().toString();
        password = mPasswordView.getText().toString();

        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            focusView.requestFocus();
        } else if (TextUtils.isEmpty(username) || !isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            focusView.requestFocus();
        } else {
            GoToMainActivity(false);
        }
    }

    private void GoToMainActivity(boolean previously_logged_in) {
        if (previously_logged_in == true) { //Already loggedIn
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra(LOGIN_STATUS, true);
            i.putExtra(USERNAME, username);
            i.putExtra(PASSWORD, password);
            startActivity(i);
            finish();
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(LOGIN_STATUS, false);
            username = mUsernameView.getText().toString();
            password = mPasswordView.getText().toString();
            intent.putExtra(USERNAME, username);
            intent.putExtra(PASSWORD, password);
            startActivity(intent);
            finish();
        }
    }

    public boolean UserLoggedIn() {
        sharedPreferences = getSharedPreferences(USER_INFO, Context.MODE_PRIVATE);
        username = sharedPreferences.getString(USERNAME, null);
        password = sharedPreferences.getString(PASSWORD, null);
        address = sharedPreferences.getString(ADDRESS, null);
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(address)) {
            return true;
        } else {
            return false;
        }
    } // Automatically initializes username, password,address

    public void enableBT() {
        if (!isBTON()) {
            Toast.makeText(LoginActivity.this, "Enabling Bluetooth, Please wait!!", Toast.LENGTH_SHORT).show();
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothAdapter.enable();
        }
    } //Do nothing if BT is enabled

    public void enableWifi() {
        if (!isWiFiON()) {
            Toast.makeText(LoginActivity.this, "Enabling Wifi, Please wait!!", Toast.LENGTH_SHORT).show();
            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifi.setWifiEnabled(true); // true or false to activate/deactivate wifi
        }
    }   //Do nothing if WiFi is enabled

    private boolean isWiFiON() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!mWifi.isConnected()) {
            return false;
        }
        return true;
    }

    private boolean isBTON() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            return true;
        }
        return false;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private boolean isUsernameValid(String username) {
        //TODO: Replace this with your own logic
        return username.length() > 3;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 3;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

}

