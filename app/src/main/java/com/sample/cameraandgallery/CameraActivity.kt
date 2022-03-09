package com.sample.cameraandgallery

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.sample.cameraandgallery.databinding.ActivityCameraBinding
import com.sample.cameraandgallery.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityCameraBinding
    private var mImageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    //val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.buttonCamera.setOnClickListener { takePhoto() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

    }

    private fun takePhoto() {
        var imageCapture = mImageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
                // put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        /*//그냥 내부저장소에 저장해보자!
        val file = File(filesDir, name)
        Log.d(TAG, "takePhoto, filesDir: $filesDir")*/

        val outputOptions = ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        //val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        //이미지 저장
        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val photoFile = File(outputFileResults.savedUri?.path)
                    Log.d(TAG, "savedUri.path: ${outputFileResults.savedUri?.path}")
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: ${outputFileResults.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    //deliverPhoto(photoFile)
                    deliverPhoto(name)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)                }
            }
        )

    }

    //photoFile: File 로?
    private fun deliverPhoto(name: String){
        //val fileName = photoFile.name
        //Log.d(TAG, "deliverPhoto, fileName: $fileName")
        //val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        //Log.d(TAG, "filePath: ${photoFile.absolutePath}")
        //Log.d(TAG, "file is absolute?: ${photoFile.isAbsolute} ")
        //val stream = ByteArrayOutputStream()
        //bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        //val imgByteArray = stream.toByteArray()
        intent.putExtra("image", name)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun loadBitmap(photoUri: Uri): Bitmap? {
        var image: Bitmap? = null
        try {
            image = if (Build.VERSION.SDK_INT > 27) {
                val source: ImageDecoder.Source =
                    ImageDecoder.createSource(this.contentResolver, photoUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(this.contentResolver, photoUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return image
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(viewBinding.cameraContainer.surfaceProvider)      //viewFinder랑 cameraContainer 같은거다!
                }
            mImageCapture = ImageCapture.Builder().build()


            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                //Unbind use cases before rebinding
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, mImageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed $e")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}