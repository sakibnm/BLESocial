package com.example.ifestexplore.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ifestexplore.R;
import com.example.ifestexplore.Register;
import com.example.ifestexplore.models.Ad;
import com.example.ifestexplore.utils.camera.TakePhoto;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mukesh.countrypicker.Country;
import com.mukesh.countrypicker.CountryPicker;
import com.mukesh.countrypicker.listeners.OnCountryPickerListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CreatePosts.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CreatePosts#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreatePosts extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int CAM_REQ = 0x1111;
    private static final int REQ_CODE = 0x005;
    private static final String TAG = "demo";
    private FirebaseAuth mAuth;
    private FirebaseFirestore saveDB;
    private FirebaseFirestore getDB;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private View view;
    private CardView cardViewTakePhoto;
    private CardView cardViewShowPhoto;
    private ImageView iv_showAdPhoto;
    private Bitmap bitmap;
    private Button button_createAd;
    private Button button_createAdClear;
    private EditText et_Title;
    private EditText et_Comment;
    private Boolean takenPhoto = false;
    private Boolean commentGiven = false;
    String currentPhotoPath;


    private Ad createdAd;
    private boolean titleGiven;

    public CreatePosts() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CreatePosts.
     */
    // TODO: Rename and change types and number of parameters
    public static CreatePosts newInstance(String param1, String param2) {
        CreatePosts fragment = new CreatePosts();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_create_posts, container, false);
        button_createAd = view.findViewById(R.id.button_createAd_share);
        button_createAdClear = view.findViewById(R.id.button_createAdClearAll);
        cardViewTakePhoto = view.findViewById(R.id.card_addPhoto);
        cardViewShowPhoto= view.findViewById(R.id.card_showPhoto);
        iv_showAdPhoto = view.findViewById(R.id.iv_showPhoto);
        cardViewTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        cardViewShowPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        button_createAd.setOnClickListener(this);
        button_createAdClear.setOnClickListener(this);

        return view;
    }

//    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.button_createAd_share){
            et_Title = this.view.findViewById(R.id.et_Title);
            et_Comment = this.view.findViewById(R.id.et_Comment);
            String title = et_Title.getText().toString().trim();
            String comment = et_Comment.getText().toString().trim();
            if(comment.equals("")){
                commentGiven =false;
                et_Comment.setError("Can't be empty!");
            }
            else commentGiven =true;
            if(title.equals("")){
                et_Title.setError("Can't be empty!");
                titleGiven =false;
            }
            else titleGiven =true;
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();


            if(takenPhoto && titleGiven && commentGiven){
                displayProgressBar();
                String activeFlag = "active";
                this.createdAd = new Ad(user.getEmail(), user.getDisplayName(),"", user.getPhotoUrl().toString(),"",et_Title.getText().toString(),et_Comment.getText().toString(), null, null, activeFlag);
                uploadImage(bitmap);
            } else {
                Toast.makeText(getContext(), "Please check everything!", Toast.LENGTH_SHORT).show();
            }

        }else if (view.getId() == R.id.button_createAdClearAll){
            clearAll();
        }
    }

//    Taking photo____________________________________________________________________________________________________________________________________________

    private void dispatchTakePictureIntent() {
        Intent intent = new Intent(getContext(), TakePhoto.class);
        intent.putExtra("purpose", "POST");
        startActivityForResult(intent, CAM_REQ);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
        int targetW = iv_showAdPhoto.getMaxWidth();
        int targetH = iv_showAdPhoto.getMaxHeight();

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
        if (this.bitmap!=null) {
            cardViewTakePhoto.setVisibility(View.INVISIBLE);
            cardViewShowPhoto.setVisibility(View.VISIBLE);
            iv_showAdPhoto.setImageBitmap(this.bitmap);
            takenPhoto = true;
        }
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
    //    ____________________________________________________________________________________________________________________________________________


    private void uploadImage(Bitmap bitmap) {
        Log.d(TAG, "uploading Ad Image");
        Bitmap userPhotoBitmap = bitmap;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        userPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        final byte[] bytes = stream.toByteArray();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        byte[] array = new byte[12]; // length is bounded by 7
        new Random().nextBytes(array);
        String key = new String(array, Charset.forName("UTF-8"));
        key.replace("/","_");
        final StorageReference userPhotoReference = storage.getReference().child("v2adsImages/"+key+".png");
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
                Log.d(TAG, "onSuccess: ImageUpload" + imageURL);

                StorageReference downloadStorage = taskSnapshot.getMetadata().getReference();

                downloadStorage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        createdAd.setItemPhotoURL(uri.toString());
                        Log.d(TAG, "Created Ad: "+createdAd.toString());
                        getDB = FirebaseFirestore.getInstance();
                        saveDB = FirebaseFirestore.getInstance();
                        getDB.collection("v2adsRepo").document("adscounter").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                final long current_count = (long) documentSnapshot.get("count");
                                createdAd.setAdSerialNo(String.valueOf(current_count));
                                saveDB.collection("adminCheck").document(createdAd.getAdSerialNo()).set(createdAd.toHashMap())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        saveDB.collection("v2adsRepo").document("adscounter").update("count", current_count+1);

                                        view.findViewById(R.id.progress_createAd).setVisibility(View.GONE);
//                                        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                        Log.d(TAG, "onSuccess: Saving Ad Second!");

                                        getBackToReceived();

                                    }
                                });
                            }
                        });

//                        saveDB.collection("adsRepo").document(createdAd.)
                    }
                });}
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: FAILED URI");
                        Toast.makeText(getContext(), "Could not post due to network errors! Please try again!", Toast.LENGTH_SHORT).show();
                        view.findViewById(R.id.progress_createAd).setVisibility(View.GONE);
                        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        
                    }
                });

    }

    private void displayProgressBar() {
        view.findViewById(R.id.progress_createAd).setVisibility(View.VISIBLE);
//        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    public void clearAll(){

        if (mListener!=null)mListener.onClearAllPressedFromCreatePosts();
    }
    public void getBackToReceived(){
        Log.d(TAG, "getBackToReceived: done!");
        if (mListener!=null)mListener.onCreatePressedFromCreatePosts();
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
        void onClearAllPressedFromCreatePosts();
        void onCreatePressedFromCreatePosts();
    }

}
