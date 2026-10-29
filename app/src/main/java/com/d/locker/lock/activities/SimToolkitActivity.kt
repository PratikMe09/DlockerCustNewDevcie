package com.d.locker.lock.activities

import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.trustonic.overlaynewdevice.R

/**
 * Dummy SIM Toolkit Screen
 * 
 * This activity shows a fake SIM Toolkit interface
 * All touch events are disabled (non-interactive)
 */
class SimToolkitActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sim_toolkit)
        
        // Make the screen always on while this activity is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Hide the action bar for a cleaner look
        supportActionBar?.hide()
    }

    /**
     * Intercept all touch events to make screen non-interactive
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // Return true to consume the event and prevent any interaction
        // This makes the entire screen untouchable
        return true
    }

    /**
     * Disable back button
     */
    override fun onBackPressed() {
        // Do nothing - prevent user from going back
    }
}
