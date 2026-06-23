package com.example.newstart.ui.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object ImageDownloader {
    suspend fun downloadImage(context: Context, imageUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Cần thiết để lấy Bitmap từ drawable
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as BitmapDrawable).bitmap
                    
                    // Đảm bảo ảnh tải về cũng được cắt vuông nếu nó chưa vuông
                    // (Dành cho các ảnh cũ chụp trước khi cập nhật logic)
                    val width = bitmap.width
                    val height = bitmap.height
                    val finalBitmap = if (width != height) {
                        val dimension = if (width < height) width else height
                        val x = (width - dimension) / 2
                        val y = (height - dimension) / 2
                        Bitmap.createBitmap(bitmap, x, y, dimension, dimension)
                    } else {
                        bitmap
                    }
                    
                    saveBitmapToGallery(context, finalBitmap)
                } else {
                    showToast(context, "Không thể tải ảnh")
                }
            } catch (e: Exception) {
                showToast(context, "Lỗi: ${e.message}")
            }
        }
    }

    private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
        val filename = "NewStart_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver?.also { resolver ->
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NewStart")
                    }
                    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = imageUri?.let { resolver.openOutputStream(it) }
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                val imageFile = java.io.File(imagesDir, filename)
                fos = java.io.FileOutputStream(imageFile)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                showToast(context, "Đã lưu ảnh vào thư viện")
            }
        } catch (e: Exception) {
            showToast(context, "Lỗi khi lưu: ${e.message}")
        }
    }

    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
