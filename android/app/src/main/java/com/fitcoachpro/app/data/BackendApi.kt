package com.fitcoachpro.app.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the FitCoach Pro backend. Base URL and the bearer
 * token are both user-configured at runtime (Settings screen) rather than
 * hardcoded, since backend hosting (VPS vs. tunnel vs. LAN-only during dev)
 * is an explicit open decision in ../../CLAUDE.md - the app shouldn't bake
 * in an assumption the design doc itself left open.
 */
interface BackendApi {
    @POST("checkin")
    suspend fun checkIn(@Body request: CheckInRequest): Response<CheckInResponse>
}
