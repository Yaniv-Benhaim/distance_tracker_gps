package co.il.dronedistancetracker.misc

import android.content.Context
import android.util.Log
import co.il.dronedistancetracker.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions


    fun GoogleMap.setCustomStyle(context: Context) {
        try {
            val success = this.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.style
                )
            )
            if(!success) {
                Log.d("STYLING", "Failed to set map style")
            }
        } catch (e: Exception) {
            Log.d("STYLING", e.toString())
        }
    }


