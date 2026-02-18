package com.d.locker.lock.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import java.io.IOException

object WallpaperUtils {
    private const val TAG = "WallpaperUtils"

    fun setWarningWallpaper(context: Context) {
        try {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // White Background
            canvas.drawColor(Color.WHITE)
            
            // Red Warning Text
            val paint = Paint().apply {
                color = Color.RED
                textSize = 70f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                style = Paint.Style.FILL
                isFakeBoldText = true
            }
            
            // Draw text in center
            val text = "⚠️ चेतावनी"
            val text2 = "कृपया अपनी बकाया EMI तुरंत जमा करें"
            val text3 = "अन्यथा आप पर कार्रवाई की जा सकती है।"
            val x = width / 2f
            val y = height / 2f
            
            canvas.drawText(text, x, y - 60, paint)
            
            paint.textSize = 40f
            paint.color = Color.BLACK
            canvas.drawText(text2, x, y + 60, paint)
            canvas.drawText(text3, x, y + 120, paint)
            
            // Set Wallpaper
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(bitmap)
            
            Log.d(TAG, "✅ Warning wallpaper set successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set wallpaper", e)
        }
    }

    fun resetWallpaper(context: Context) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.clear() // Resets to default system wallpaper
            Log.d(TAG, "✅ Wallpaper reset to default")
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to clear wallpaper", e)
        }
    }
}
