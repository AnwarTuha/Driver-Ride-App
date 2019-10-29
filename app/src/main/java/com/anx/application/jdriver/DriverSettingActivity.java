package com.anx.application.jdriver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hbb20.CountryCodePicker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSettingActivity extends AppCompatActivity {

    private Button mConfirm, mBack;
    private EditText mNameField, mPhoneField, mCarField;
    private ImageView mProfileImage;
    private EditText mEmailField;
    private CountryCodePicker ccp;

    private FirebaseAuth mAuth;
    private DatabaseReference mDriverDatabase;

    private ProgressBar mEmailProgress, mNameProgress, mCarProgress, mPhoneProgress;

    private String userId = "";
    private String mName = "";
    private String mPhone = "";
    private String mCarType = "";
    private String mCarColor = "";
    private String mPlate = "";
    private String mEmail = "";
    private String mService;
    private String mProfileImageUrl;

    private Uri resultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_setting);

        // Connecting Variable to layout
        mProfileImage = (ImageView) findViewById(R.id.profileImage);
        mConfirm =  findViewById(R.id.confirm);
        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.phone);
        mCarField = findViewById(R.id.car);
        mEmailField = findViewById(R.id.email);

        mEmailProgress = findViewById(R.id.emailProgress);
        mNameProgress = findViewById(R.id.nameProgress);
        mPhoneProgress = findViewById(R.id.phoneProgress);
        mCarProgress = findViewById(R.id.carProgress);
        ccp = findViewById(R.id.ccp);

        Toolbar toolbar = findViewById(R.id.bgHeader);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId);
        getUserInfo();

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });
    }

    private void getUserInfo(){
        mDriverDatabase.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null){
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                        mNameProgress.setVisibility(View.GONE);
                    }
                    if (map.get("phone") != null){
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone.replace("+251", ""));
                        mPhoneProgress.setVisibility(View.GONE);
                    }
                    if (map.get("cartype") != null){
                        mCarType = map.get("cartype").toString();
                        if (map.get("color") != null){
                            mCarColor = map.get("color").toString();
                        }
                        if (map.get("plate") != null){
                            mPlate = map.get("plate").toString();
                        }
                        mCarField.setText(mCarColor+" "+mCarType+", Plate: "+mPlate);
                        mCarProgress.setVisibility(View.GONE);
                    }
                    if (map.get("email") != null){
                        mEmail = map.get("email").toString();
                        mEmailField.setText(mEmail);
                        mEmailProgress.setVisibility(View.GONE);
                    }
                    if (map.get("profileImageUrl") != null){
                        StorageReference storageReference =  FirebaseStorage.getInstance().getReference().child("profileImage").child(userId);
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                //do your stuff- uri.toString() will give you download URL\\
                                Glide.with(getApplicationContext()).load(uri.toString()).error(R.drawable.ic_default_profile).into(mProfileImage);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void saveUserInformation(){
        mName = mNameField.getText().toString();
        mPhone = "+" + ccp.getFullNumber() + mPhoneField.getText().toString();
        mEmail = mEmailField.getText().toString();

        Map userInfo = new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhone);
        userInfo.put("email", mEmail);
        mDriverDatabase.updateChildren(userInfo);

        if (resultUri != null){
            final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profileImage").child(userId);
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream boas = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, boas);
            byte[] data = boas.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), "Upload Failed!", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> downloadUri = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                    Map newImage = new HashMap();
                    newImage.put("profileImageUrl", downloadUri.toString());
                    mDriverDatabase.updateChildren(newImage);
                    Toast.makeText(getApplicationContext(), "Image Uploaded Successfully", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            });
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        onBackPressed();
        return true;
    }



}
