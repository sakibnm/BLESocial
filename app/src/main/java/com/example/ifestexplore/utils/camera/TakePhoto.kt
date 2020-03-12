package com.example.ifestexplore.utils.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.ifestexplore.R
import com.example.ifestexplore.fragments.GalleryFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.concurrent.Executors

class TakePhoto() : AppCompatActivity(), LifecycleOwner, GalleryFragment.ToTakePhoto {

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private lateinit var preview_image:ImageView
    private lateinit var purpose: String
    private lateinit var orientation: String
    private lateinit var button_capture:FloatingActionButton
    private lateinit var button_switch_camera:FloatingActionButton
    private lateinit var button_gallery:FloatingActionButton
    lateinit var progressContainer: ConstraintLayout

    val fragmentManager = supportFragmentManager
    val fragmentTransaction = fragmentManager.beginTransaction()

    private var CAM_REQ: Int = 0
    var screenWIDTH:Int = 720
    var screenHEIGHT:Int = 1280

    lateinit var lensFace: String
    var PERMISSION_CODE_READ: Int = 3333
    var PERMISSION_CODE_WRITE: Int = 5555
    var IMAGE_PICK_CODE: Int = 0x003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_photo)

        progressContainer = findViewById(R.id.progressContainer)
        button_capture = findViewById(R.id.button_capture)
        button_gallery = findViewById(R.id.button_gallery)
        viewFinder = findViewById(R.id.view_finder)
        screenWIDTH = viewFinder.width
        screenHEIGHT = viewFinder.height
//      Getting request from Parent Activity.....
        val bundle:Bundle = intent.extras!!
        purpose = bundle.getString("purpose", "POST")

        if(purpose=="PROFILE") lensFace = "FRONT"
        else lensFace = "BACK"

//      GALLERY photos......
        button_gallery.setOnClickListener(View.OnClickListener {
            val fragment = GalleryFragment(this,true)
            fragmentTransaction.add(R.id.fragment_gallery, fragment)
            fragmentTransaction.commit()
        })

        //        Switch camera......
        button_switch_camera = findViewById(R.id.button_flip)
        button_switch_camera.setOnClickListener(View.OnClickListener {
            if (lensFace == "FRONT") lensFace = "BACK"
            else lensFace = "FRONT"
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }

            } else {
                ActivityCompat.requestPermissions(
                        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }
        })

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }

        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    private fun startCamera() {
        CameraX.unbindAll()

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
//            setTargetResolution(Size(1280, 960))
            setTargetResolution(Size(screenWIDTH,screenHEIGHT))
            if (lensFace=="FRONT")setLensFacing(CameraX.LensFacing.FRONT)
            else setLensFacing(CameraX.LensFacing.BACK)
        }.build()


        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
                .apply {
                    // We don't set a resolution for image capture; instead, we
                    // select a capture mode which will infer the appropriate
                    // resolution based on aspect ration and requested mode
                    setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                    //        TO Use Front Camera: Set Lence Facing....
                    if (lensFace=="FRONT")setLensFacing(CameraX.LensFacing.FRONT)
                    else setLensFacing(CameraX.LensFacing.BACK)

                }.build()
        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        button_capture.setOnClickListener {
            val file = File(externalMediaDirs.first(),
                    "${System.currentTimeMillis()}.jpg")

            progressContainer.visibility = View.VISIBLE

            imageCapture.takePicture(file, executor,
                    object : ImageCapture.OnImageSavedListener {
                        override fun onError(
                                imageCaptureError: ImageCapture.ImageCaptureError,
                                message: String,
                                exc: Throwable?
                        ) {
                            val msg = "Photo capture failed: $message"
                            Log.e("CameraXApp", msg, exc)
                            viewFinder.post {
                                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onImageSaved(file: File) {
                            val msg = "Photo capture succeeded: ${file.absolutePath}"
                            Log.d("CameraXApp", msg)
                            viewFinder.post {
//                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                                val intent = Intent(applicationContext, GetImage::class.java)
                                intent.putExtra("filedir", file.absolutePath)
                                intent.putExtra("lensface", lensFace)

                                CAM_REQ = 0x2222
                                startActivityForResult(intent, CAM_REQ);
                            }

//                        findViewById<FloatingActionButton>(R.id.button_ok).visibility(View.VISIBLE)
//                        findViewById<FloatingActionButton>(R.id.button_cancel).visibility(View.VISIBLE)
                        }
                    })
        }
        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        // TODO: Implement camera viewfinder transformations
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = screenWIDTH / 2f
        val centerY = screenHEIGHT / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }
    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        progressContainer.visibility = View.INVISIBLE
        // Check which request we're responding to
        if (requestCode == CAM_REQ) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                val bundle = data?.extras
                val newpath = bundle!!.getString("filepath")
                val returnIntent = Intent()
                returnIntent.putExtra("filepath", newpath)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }
        }
    }
//    private fun checkPermissionForImage() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
//                    && (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
//            ) {
//                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
//                val permissionCoarse = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//
//                requestPermissions(permission, PERMISSION_CODE_READ) // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
//                requestPermissions(permissionCoarse, PERMISSION_CODE_WRITE) // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_WRITE LIKE 1002
//            } else {
////                GALLERY>>>>>
//                val fragment = GalleryFragment()
//                fragmentTransaction.add(R.id.fragment_gallery, fragment)
//                fragmentTransaction.commit()
//            }
//        }
//    }

    override fun setImageToMain(imagePath: String) {
        val returnIntent = Intent()

        returnIntent.putExtra("filepath",imagePath)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

}



private fun Intent.putExtra(s: String, newBitmap: Any?) {

}

