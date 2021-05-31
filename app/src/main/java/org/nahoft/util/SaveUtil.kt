package org.nahoft.util

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.OutputStream

object SaveUtil
{
    fun saveImageToGallery(context: Context, image: Bitmap, title: String, description: String): Boolean
    {
        val filename = "${System.currentTimeMillis()}.png"
        // FileOutputStream
        var fos: OutputStream? = null

        // Check the version of Android, as versions Q and higher require a new way of saving images
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            context.contentResolver.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)

                }

                val maybeImageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                // Open the output stream using the uri was got with our content values
                fos = maybeImageUri?.let { imageUri ->
                    resolver.openOutputStream(imageUri)
                }
            }
        }
        else if  (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)// Android versions earlier than Q
        {
            //val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

//            val wrapper = ContextWrapper(context)
//            var imagesDir = wrapper.getDir("images", Context.MODE_PRIVATE)

            val imagesDir =
                File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + separator + "test_pictures")
//            val dir: File = Environment.getExternalStorageDirectory()
//            val imagesDir: File = File(dir.absolutePath + "/MyPhotos")
            if (!imagesDir.exists()) {          
                imagesDir.mkdirs()
            }

            val imageFile = File(imagesDir.getAbsolutePath(), filename)
            fos = FileOutputStream(imageFile)

        }

        fos?.use { fileOutputStream ->
            val saved = image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            if (saved)
            {
                return true
            }
        }
        return false
    }
}