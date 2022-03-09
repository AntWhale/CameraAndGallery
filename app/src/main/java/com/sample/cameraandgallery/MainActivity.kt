package com.sample.cameraandgallery

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import com.sample.cameraandgallery.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import kotlin.concurrent.thread

class MainActivity : BaseActivity() {
    private val PERM_STORAGE = 99
    private val PERM_CAMERA = 100
    private val REQ_STORAGE = 102

    //val REQ_CAMERA = 101
    val TAG = "MainActivity"
    var realUri: Uri? = null
    var realFile: File? = null

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                realUri?.let { uri ->
                    val bitmap = loadBitmap(uri)
                    binding.imagePreview.setImageBitmap(bitmap)

                    realUri = null
                }
                /*if(it.data?.extras?.get("data") != null){
                    val bitmap = it.data?.extras?.get("data") as Bitmap
                    binding.imagePreview.setImageBitmap(bitmap)
                }*/
            }
        }

    private val galleryContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.data?.let { uri ->
                    binding.imagePreview.setImageURI(uri)
                    Log.d(TAG, "gallery_uri: $uri")
                }
            }
        }

    //커스텀 카메라 화면에서 데이터 받기
    private val captureContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.let { intent ->
                    val fileName = intent.getStringExtra("image") ?: return@let
                    val file = "/storage/emulated/0/Pictures/CameraX-Image/".plus(fileName).plus(".jpg")
                    Log.d(TAG, "full file name: $file ")
                    val image = BitmapFactory.decodeFile(file)
                    //val imgByteArray = intent.getByteArrayExtra("image") ?: return@let
                    //Log.d(TAG, "byteStream: $imgByteArray")
                    //val image = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray.size)
                    binding.imagePreview.setImageBitmap(image)
                }
            }
        }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }


    override fun permissionGranted(requestCode: Int) {
        when (requestCode) {
            PERM_STORAGE -> setViews()
            PERM_CAMERA -> openCamera()
        }
    }

    override fun permissionDenied(requestCode: Int) {
        when (requestCode) {
            PERM_STORAGE -> {
                Toast.makeText(baseContext, "외부 저장소 권한을 승인해야 앱을 사용할 수 있습니다.", Toast.LENGTH_LONG)
                    .show()
                finish()
            }

            PERM_CAMERA -> {
                Toast.makeText(baseContext, "카메라 권한을 승인해야 앱을 사용할 수 있습니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setViews() {
        binding.buttonCamera.setOnClickListener {
            requirePermissions(arrayOf(Manifest.permission.CAMERA), PERM_CAMERA)
        }
        binding.buttonGallery.setOnClickListener {
            openGallery()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        createImageUri(newFileName(), "image/jpg")?.let { uri ->
            realUri = uri
            intent.putExtra(MediaStore.EXTRA_OUTPUT, realUri)
            getContent.launch(intent)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        galleryContent.launch(intent)
    }

    private fun newFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())

        return "$filename.jpg"
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

    fun createImageUri(filename: String, mimeType: String): Uri? {
        var values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Log.d(TAG, "onCreate, filesDir: ${baseContext.filesDir}")

        //test
        /*val file = "/storage/emulated/0/Pictures/CameraX-Image/".plus("2022-03-10-00-08-50-747.jpg")
        val image = BitmapFactory.decodeFile(file)
        binding.imagePreview.setImageBitmap(image)*/

        requirePermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERM_STORAGE)

        binding.buttonCamera.setOnClickListener {
            val intent = Intent(baseContext, CameraActivity::class.java)
            captureContent.launch(intent)
        }

        /*var thread = WorkerThread()
        thread.start()*/

        /*var thread = Thread(WorkerRunnable())
        thread.start()*/

        /*Thread {
            var i =0
            while(i < 10){
                i += 1
                Log.i("MainActivity", "onCreate: $i")
            }
        }.start()*/

        /*thread(start = true) {
            var i = 0
            while (i < 10) {
                i += 1
                Log.i("MainActivity", "onCreate: $i")
            }
        }*/
    }

    //thread 공부
    class WorkerThread : Thread() {
        val TAG = "WorkerThread"

        override fun run() {
            var i = 0
            while (i < 10) {
                i += 1
                Log.i(TAG, "run: $i")
            }
        }
    }

    class WorkerRunnable : Runnable {
        val TAG = "WorkerRunnable"

        override fun run() {
            var i = 0
            while (i < 10) {
                i += 1
                Log.i(TAG, "run: $i")
            }
        }
    }


}