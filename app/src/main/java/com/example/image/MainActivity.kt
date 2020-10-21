package com.example.image

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.renderscript.ScriptGroup
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.widget.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.*
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 111
    private var firebaseStorage: FirebaseStorage? = null
    private var storageReference: StorageReference? = null
    private var databaseReference: DatabaseReference? = null

    lateinit var dataUri: Uri
    var imageUrl: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        imageView1 = findViewById<ImageView>(R.id.imageView1) as ImageView
        val chooseImagebtn = findViewById<Button>(R.id.btn_loadimage) as Button
        val uploadImagebtn = findViewById<Button>(R.id.btn_updateimage) as Button
        val showAlbumbtn = findViewById<Button>(R.id.btn_showimage) as Button
        var editText = findViewById<EditText>(R.id.name_album)
        firebaseStorage = FirebaseStorage.getInstance()
        storageReference = FirebaseStorage.getInstance().getReference("updates")
        databaseReference = FirebaseDatabase.getInstance().getReference("updates")

        chooseImagebtn.setOnClickListener { loadImage() }
        uploadImagebtn.setOnClickListener { uploadFile() }
    }

    private fun loadImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            dataUri = data?.data!!
            Picasso.with(this).load(dataUri).into(image_view)
        }
    }

    fun uploadFile() {
        if (dataUri != null) {
            var pd = ProgressDialog(this)
            pd.setTitle("Uploading")
            pd.show()
            val fileRef = storageReference?.child("update/" + UUID.randomUUID().toString())

            fileRef?.putFile(dataUri)
                ?.addOnSuccessListener { p0 ->
                    pd.dismiss()
                    Toast.makeText(this, "Image Uploaded", Toast.LENGTH_SHORT).show()
                    writeNewAlbum(name_album.text.toString().trim(), fileRef.downloadUrl.toString())

                }
                ?.addOnFailureListener { p0 ->
                    pd.dismiss()
                    Toast.makeText(this, p0.message, Toast.LENGTH_SHORT).show()
                }
                ?.addOnProgressListener { p0 ->
                    var progress: Double = (100.0 * p0.bytesTransferred) / p0.totalByteCount
                    pd.setMessage("Upload ${progress.toInt()}%")
                }
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeNewAlbum(name: String, dataUri: String) {
        val upload = Upload(name, dataUri)
        alertWriteNote("mnote", name)
        databaseReference?.child(name)?.setValue(upload)

    }

    private fun alertWriteNote(key: String, name: String) {

        val alertDialog2 = AlertDialog.Builder(this)
        alertDialog2.setTitle("Update Note")

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(50, 10, 10, 10)

        val editText = EditText(this)
        editText.hint = "Write something about this image"

        linearLayout.addView(editText)
        alertDialog2.setView(linearLayout)
        alertDialog2.setPositiveButton("Update") { dialog, which ->
            val value = editText.text.toString().trim { it <= ' ' }
            val result = java.util.HashMap<String, Any>()
            result[key] = value
            databaseReference!!.child(name).updateChildren(result)
                .addOnSuccessListener {
                    Toast.makeText(this, "Updated Note", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Update Failed ", Toast.LENGTH_SHORT).show()
                }
        }

        alertDialog2.setNegativeButton("Cancel") { dialog, which ->
            Toast.makeText(this, "You clicked on Cancel", Toast.LENGTH_SHORT)
                .show()
            dialog.cancel()
        }
        alertDialog2.create().show()
    }

}


