package com.example.ifestexplore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.ifestexplore.models.Ad;
import com.example.ifestexplore.models.Events;
import com.example.ifestexplore.models.User;
import com.example.ifestexplore.models.UsersData;
import com.example.ifestexplore.utils.camera.TakePhoto;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.altbeacon.beacon.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Register extends AppCompatActivity {

    private static final String TAG = "demo";
    private static final int REQ_CODE = 0x005;
    private FirebaseAuth mAuth;

    private EditText etName, etEmail, etPassword, etRepPassword;
    private TextView tvInstr;
    private Button createAccount;
    private TextView tvLoginClicked;
    private ImageView ivUserPhoto;
    private static int CAM_REQ = 0x1111;
    private Bitmap bitmap;
    private ProgressBar progressBar;
    private URI userUri;
    private String currentPhotoPath;

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser!=null){
//            Log.d(TAG, "onStart: "+ currentUser.getEmail()+" name: "+currentUser.getDisplayName());
            Intent intent = new Intent(Register.this, Home.class);
            startActivity(intent);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
//        ActionBar bar = getSupportActionBar();
//        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#042529")));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        
        if(!isNetworkAvailable()) Log.d(TAG, "onCreate: Internet not Available!!!");

        ivUserPhoto = findViewById(R.id.iv_userphoto);
        tvInstr = findViewById(R.id.tvInstr);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etRepPassword = findViewById(R.id.et_rep_password);
        createAccount = findViewById(R.id.button_register2);
        tvLoginClicked = findViewById(R.id.tv_sign_in_click);

        mAuth = FirebaseAuth.getInstance();

//        Taking Photo.....
//        __________________________________________________________________________________________________
        ivUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dispatchTakePictureIntent();

            }
        });
//        ___________________________________________________________________________________________________

        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String userEmail = String.valueOf(etEmail.getText()).trim();
                String userPassword = String.valueOf(etPassword.getText()).trim();
                final String userName = String.valueOf(etName.getText()).trim();
                String userRepPassword = String.valueOf(etRepPassword.getText()).trim();
                boolean inputValid = true;
                if(! Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()){
                    etEmail.setError("Put a valid Email address!");
                    inputValid = false;
                }
                if(userName.equals("")|| userEmail.equals("") || bitmap==null||userPassword== ""||userRepPassword==""){
                    inputValid = false;
                    Toast.makeText(getApplicationContext(),"Please take a photo, and fill up all the fields", Toast.LENGTH_SHORT).show();
                }else if (!userPassword.equals(userRepPassword)){
                    inputValid = false;
                    etPassword.setError("Passwords do not match!");
                    etRepPassword.setError("Passwords do not match!");
                }

                else if(inputValid){
                    displayProgressBar();
                    User user = new User(userName, userEmail, "", userPassword);

                    signUp(user);

                }
            }
        });

        tvLoginClicked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
            }
        });



    }
//    Taking photo____________________________________________________________________________________________________________________________________________

    private void dispatchTakePictureIntent() {
        Intent intent = new Intent(Register.this, TakePhoto.class);
        intent.putExtra("purpose", "PROFILE");
        startActivityForResult(intent, CAM_REQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Camera photo......
        if (requestCode == CAM_REQ){
            if (data !=null) {
                Bundle bundle = data.getExtras();
                String imagepath = bundle.getString("filepath");
                currentPhotoPath = imagepath;
                Log.d(TAG, "Image path: "+currentPhotoPath);
                setPicFromPath();
            }
        }
    }

    private void setPicFromPath() {
        // Get the dimensions of the View
        int targetW = ivUserPhoto.getMaxWidth();
        int targetH = ivUserPhoto.getMaxHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
//        bmOptions.inPurgeable = true;

        this.bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        this.bitmap = cropCenter(this.bitmap);
//        this.bitmap = Bitmap.createScaledBitmap(this.bitmap, 1280, 960,false);
//        Log.d("demo", "setPic: "+ this.bitmap.getWidth()+" "+this.bitmap.getHeight());
//        Log.d("demo", "setPic: "+ this.bitmap.getByteCount()/1000);
//        ivUserPhoto.setImageBitmap(this.bitmap);
//        File imageFile = new File(currentPhotoPath);
//        if (imageFile.exists())Picasso.get().load(imageFile).into(ivUserPhoto);
        if (this.bitmap!=null) ivUserPhoto.setImageBitmap(this.bitmap);
        else Log.d(TAG, "setPic: "+ "can't find");

    }

    private Bitmap cropCenter(Bitmap srcBmp) {
        Bitmap dstBmp = null;
        if (srcBmp.getWidth() >= srcBmp.getHeight()){
            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );
        }else{
            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight()/2 - srcBmp.getWidth()/2,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );
        }
        return dstBmp;
    }

//        ____________________________________________________________________________________________________________________________________________


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected() ||
                (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                        && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
            return false;
        }
        return true;
    }

    private void uploadImage(Bitmap bitmap, final User user){
        Bitmap userPhotoBitmap = bitmap;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//
        userPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 25, stream);
        final byte[] bytes = stream.toByteArray();
//        Log.d(TAG, "uploadImage: "+bytes.length);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        final StorageReference userPhotoReference = storage.getReference().child("userPhotos/"+user.getEmail()+"_photo.jpg");
        UploadTask uploadTask = userPhotoReference.putBytes(bytes);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                exception.printStackTrace();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                String imageURL = taskSnapshot.getMetadata().getPath();
                Log.d(TAG, "onSuccess: ImageUpload"+imageURL);
                user.setPhotoURL(imageURL);

                StorageReference downloadStorage = taskSnapshot.getMetadata().getReference();

                downloadStorage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
////                        .setDisplayName(userR.getName())
                                .setPhotoUri(uri)
                                .build();
                        mAuth.getCurrentUser().updateProfile(profileChangeRequest).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                findViewById(R.id.progressCard).setVisibility(View.GONE);
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                                Intent intent = new Intent(Register.this, Home.class);

                                startActivityForResult(intent, REQ_CODE);
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: FAILED URI");
                        findViewById(R.id.progressCard).setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        Toast.makeText(getApplicationContext(), "Please try again!", Toast.LENGTH_SHORT).show();
                    }
                });

//                if(downloadUri.isSuccessful()){
//                    Log.d(TAG, "onSuccess: URL: "+downloadUri.getResult());
//
//                }else{
//                    Log.d(TAG, "onFailure: NOT WORKING URI" );
//                }




//                signUp(user);

            }
        });


    }
    private void displayProgressBar() {
        findViewById(R.id.progressCard).setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void signUp(final User userR){
        Log.d(TAG, "signUpStarting: "+userR.toString());
//        final Uri userProfURI = Uri.parse(userR.getPhotoURL());
        mAuth.createUserWithEmailAndPassword(userR.getEmail(), userR.getPassword())
                .addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            final FirebaseUser user = mAuth.getCurrentUser();
                            UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(userR.getName())
//                                    .setPhotoUri(userProfURI)
                                    .build();

                            createUsersData(userR);
                            user.updateProfile(profileChangeRequest).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "User profile updated.");
                                    uploadImage(bitmap, userR);
                                }
                            });

                        }else{
                            Toast.makeText(getApplicationContext(), task.getException().getMessage().toString(), Toast.LENGTH_SHORT);
                        }
                    }
                });


    }

    private void createUsersData(final User userC){
        final String email = userC.getEmail();
        String name = userC.getName();
        List<Ad> ads = new ArrayList<Ad>();

        String instanceID = Identifier.parse(UUID.randomUUID().toString()).toString();
        final UsersData usersData = new UsersData(email, name, instanceID);

        final FirebaseFirestore saveDB = FirebaseFirestore.getInstance();

        saveDB.collection("users").document(email).set(usersData.toHashMap())
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "onSuccess Saving User to Firestore for: "+ email);
                    Map<String, Object> idToEmail = new HashMap<>();
                    idToEmail.put("email", usersData.getEmail());
                    String partInstanceID1 = usersData.getInstanceID().substring(0, 8);
                    String partInstanceID2 = usersData.getInstanceID().substring(9, 13);
                    String partInstanceID = "0x"+partInstanceID1+partInstanceID2;
                    Log.d(TAG, "PART INSTANCE ID: "+ partInstanceID1+" "+partInstanceID2+" "+partInstanceID);
                    saveDB.collection("mapIDtoemail").document(partInstanceID).set(idToEmail)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //                    LOGGING DATA....EVENT.....
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            Date date = new Date();
                            String datetime = formatter.format(date);
                            Events event = new Events(datetime, "Registered and Logged In");
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            db.collection("users").document(user.getEmail()).collection("events").add(event);
                        }
                    });
                }
            })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure Saving User to Firestore for: "+ email);
            }
        });
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
