package org.nahoft.nahoft.activities

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.app.ActivityCompat.startActivityForResult
import kotlinx.android.synthetic.main.activity_import_image_text.*
import kotlinx.android.synthetic.main.activity_new_message.*
import org.nahoft.nahoft.R
import org.nahoft.util.RequestCodes

class ImportImageText : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_image_text)

        val REQUEST_CODE = 100


        fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE)
    }

        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE){
                imageView.setImageURI(data?.data) // handle chosen image
            }
        }

}
}


