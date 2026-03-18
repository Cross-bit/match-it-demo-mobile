package com.example.matchit.data.deviceSensors

import com.example.matchit.data.model.session.GpsCoordinates
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationRepository @Inject constructor(
    private val locationClient: FusedLocationProviderClient,
)  {
    /**
     * Returns current device location.
     */
    suspend fun getCurrentLocation(): Result<GpsCoordinates> = withContext(Dispatchers.IO) {
        try {
            val location = suspendCancellableCoroutine { continuation ->
                locationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(GpsCoordinates(location.longitude, location.latitude))
                        } else {
                            continuation.resumeWithException(Exception("No last known location available"))
                        }
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }
            Result.success(location)
        } catch (e: SecurityException) {
            Result.failure(Exception("Location permission denied"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}