package com.d.locker.lock.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

object AudioPlayer {
    private const val TAG = "AudioPlayer"

    fun playAudio(context: Context) {
        try {
            // Get resource ID for "alert"
            val resId = context.resources.getIdentifier("alert", "raw", context.packageName)
            
            if (resId != 0) {
                val mp = MediaPlayer.create(context, resId)
                if (mp != null) {
                    mp.setOnCompletionListener { 
                        it.release()
                        Log.d(TAG, "🔊 Audio finished")
                    }
                    mp.start()
                    Log.d(TAG, "🔊 Audio 'alert' playing")
                } else {
                     Log.e(TAG, "❌ Failed to create MediaPlayer")
                }
            } else {
                Log.e(TAG, "❌ Audio file 'alert' not found in raw resources")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to play audio", e)
        }
    }
}
