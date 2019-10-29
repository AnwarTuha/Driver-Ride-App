package com.anx.application.jdriver;

import androidx.annotation.NonNull;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class VerifyPhoneActivity extends AppCompatActivity {

    private String verificationId = "";
    private int btnType = 0;
    private String phoneNumber;

    private Button verify;
    private EditText otp;
    private TextView info;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallback;

    private int APP_START = 0;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_phone);

        mAuth = FirebaseAuth.getInstance();

        verify = findViewById(R.id.verify);
        otp = findViewById(R.id.otp);
        info = findViewById(R.id.info);

        phoneNumber = getIntent().getStringExtra("phoneNumber");
        info.setText("We have sent a verification Code to " + phoneNumber + " via SMS, enter the code below...");
        Toast.makeText(VerifyPhoneActivity.this, "Code is Sent", Toast.LENGTH_SHORT).show();

        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (otp.getText().toString().equals("")) {
                    otp.setError("Check SMS for code");
                    otp.requestFocus();
                    return;
                } else {
                    if (btnType == 0) {
                        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                                phoneNumber,
                                60,
                                TimeUnit.SECONDS,
                                VerifyPhoneActivity.this,
                                mCallback
                        );
                        Toast.makeText(VerifyPhoneActivity.this, "Code is Sent", Toast.LENGTH_SHORT).show();
                    } else {
                        verify.setEnabled(false);
                        String verificationCode = otp.getText().toString();
                        try {
                            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, verificationCode);
                            signInWithPhoneAuthCredential(credential);
                            dialog = ProgressDialog.show(VerifyPhoneActivity.this, "",
                                    "Signing in. Please wait...", true);
                        } catch (Exception e) {
                            if (dialog.isShowing()){
                                dialog.cancel();
                            }
                            Toast.makeText(VerifyPhoneActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        mCallback = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                otp.setText(verificationId);
                signInWithPhoneAuthCredential(phoneAuthCredential);
                dialog = ProgressDialog.show(VerifyPhoneActivity.this, "",
                        "Signing in. Please wait...", true);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    if (dialog.isShowing()){
                        dialog.cancel();
                    }
                    Toast.makeText(VerifyPhoneActivity.this, "Invalid code Entered", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    if (dialog.isShowing()){
                        dialog.cancel();
                    }
                    Toast.makeText(VerifyPhoneActivity.this, "SMS quota for this user is exceeded", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                btnType = 1;
                verificationId = s;
                verify.setText("Verify Code");
            }
        };


    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(VerifyPhoneActivity.this, "Sign in with credential Successful", Toast.LENGTH_SHORT).show();
                            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();;
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId);
                            current_user_db.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()){
                                        Toast.makeText(VerifyPhoneActivity.this, "Nebar user", Toast.LENGTH_SHORT).show();
                                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                        if (user != null) {
                                            Intent intent = new Intent(VerifyPhoneActivity.this, DriverMapActivity.class);
                                            startActivity(intent);
                                            dialog.cancel();
                                            finish();
                                        }
                                    } else {
                                        Toast.makeText(VerifyPhoneActivity.this, "New User", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(VerifyPhoneActivity.this, RegisterActivity.class);
                                        intent.putExtra("phoneNumber", phoneNumber.replace("+251", ""));
                                        startActivity(intent);
                                        dialog.cancel();
                                        finish();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        } else {
                            Toast.makeText(VerifyPhoneActivity.this, "Sign in with credential failed, " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(VerifyPhoneActivity.this, "Entered Code is invalid", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (APP_START == 0) {
            final String phoneNumber = getIntent().getStringExtra("phoneNumber");
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,
                    60,
                    TimeUnit.SECONDS,
                    VerifyPhoneActivity.this,
                    mCallback
            );
            APP_START = 1;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}
