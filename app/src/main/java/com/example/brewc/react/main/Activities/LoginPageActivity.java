package com.example.brewc.react.main.Activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brewc.react.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;

import android.os.Vibrator;


public class LoginPageActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private final String USERS = "users";
    private final int MIN_PASSWORD_LEN = 4, MAX_PASSWORD_LEN = 10;

    // Vibrate pattern
    private final long[] VIB_PATTERN = {0, 100, 50, 100};

    private DatabaseReference _rootReference;
    private AppCompatButton _loginButton;
    private EditText _inputEmail, _inputPassword;
    private TextView _linkRegister;
    private FirebaseAuth _auth;
    private FirebaseAuth.AuthStateListener _authListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove title bar and make full screen
        // source: http://stackoverflow.com/questions/30746109/how-to-remove-title-bar-from-activity-extending-actionbaractivity-or-appcompatac
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getSupportActionBar().hide();
        this.setContentView(R.layout.login_page_activity);

        _rootReference = FirebaseDatabase.getInstance().getReference();
        _auth = FirebaseAuth.getInstance();
        _loginButton = (AppCompatButton) findViewById(R.id.btn_login);
        _inputEmail = (EditText) findViewById(R.id.input_email);
        _inputPassword = (EditText) findViewById(R.id.input_password);
        _linkRegister = (TextView) findViewById(R.id.link_register);
        // https://firebase.google.com/docs/auth/android/password-auth
        _authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };


    }

    @Override
    protected void onStart() {
        super.onStart();

        updateLoginButton();
        _inputEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        _inputPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        _loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        _linkRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registrationPage = new Intent(LoginPageActivity.this, RegisterPageActivity.class);
                startActivity(registrationPage);
            }
        });
        _auth.addAuthStateListener(_authListener);
        _inputEmail.setFocusable(true);
        _inputPassword.setFocusable(true);
        if (this._auth.getCurrentUser() != null) {
            onLoginSuccess();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (_authListener != null) {
            _auth.removeAuthStateListener(_authListener);
        }
    }

    /**
     * attempts to login the user
     */
    private void login() {
        // use basic email patterns
        if (!safetyCheck()) {
            onLoginFail();
            return;
        }
        new LoginTask(this).execute();
    }

    /**
     * if the login fails
     */
    private void onLoginFail() {
        Toast.makeText(getBaseContext(), "Invalid Credentials", Toast.LENGTH_LONG).show();
        // vibrate
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VIB_PATTERN, -1);
        Log.v("", "Login Failure :(");
    }

    /**
     * if the login was successful
     */
    private void onLoginSuccess() {
        Log.v("", "Login Success!");
        Intent homePage = new Intent(LoginPageActivity.this, DrawerActivity.class);
        // go to home page
        startActivity(homePage);
    }

    /**
     * checks the validity of the email
     * @return true if the email address is valid
     */
    private boolean safetyCheck() {
        String email = getEmail();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.e(TAG, "non-valid-email");
            _inputEmail.setError("Enter a valid email address");
            return false;
        } else {
            Log.v(TAG, "valid-email");
            return true;
        }
    }

    /**
     * Grey out the login button if credentials are not filled
     * Else, color the login button
     */
    private void updateLoginButton() {
        boolean nonEmptyInputEmail = _inputEmail.getText().toString().length() > 0;
        boolean nonEmptyInputPassword = _inputPassword.getText().toString().length() > 0;
        this._loginButton.setEnabled(nonEmptyInputEmail && nonEmptyInputPassword);
    }

    /**
     * @return password from EditText
     */
    private String getPassword() {
        return _inputPassword.getText().toString();
    }

    /**
     * @return email from EditText
     */
    private String getEmail() {
        return _inputEmail.getText().toString();
    }

    /**
     * Login off of the main UI thread
     */
    private class LoginTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progressDialog;
        private final int SLEEP_TIME = 500;

        LoginTask(LoginPageActivity loginPageActivity) {
            progressDialog = new ProgressDialog(loginPageActivity);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Authenticating...");
        }

        @Override
        protected Void doInBackground(Void... params) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(SLEEP_TIME);
                        progressDialog.show();
                    } catch (Exception exception) {
                        Log.e(TAG, "Could not initiate progress dialog");
                    }
                }
            });
            _auth.signInWithEmailAndPassword(getEmail(), getPassword())
                    .addOnCompleteListener(LoginPageActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }

                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "signInWithEmail:failed", task.getException());
                                onLoginFail();
                            } else {
                                onLoginSuccess();
                            }
                        }
                    });
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // nada
        }
    }
}