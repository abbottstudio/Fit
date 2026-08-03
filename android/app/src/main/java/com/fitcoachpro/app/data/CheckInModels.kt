package com.fitcoachpro.app.data

import com.google.gson.annotations.SerializedName

/**
 * Mirrors the request body accepted by POST /checkin in
 * fitcoach-pro/backend/src/routes/checkin.js exactly - field names and
 * nullability must match, since that route reads req.body by these exact
 * keys. If you change one side, change the other in the same commit (see
 * AGENT_PROMPT.md's note on the structured-output contract - same principle
 * applies here).
 */
data class CheckInRequest(
    @SerializedName("date") val date: String, // YYYY-MM-DD, required
    @SerializedName("weight_kg") val weightKg: Double? = null,
    @SerializedName("sleep_hours") val sleepHours: Double? = null,
    @SerializedName("energy") val energy: Int? = null, // 1-10
    @SerializedName("stress") val stress: Int? = null, // 1-10
    @SerializedName("motivation") val motivation: Int? = null, // 1-10
    @SerializedName("soreness") val soreness: String? = null,
    @SerializedName("joint_pain") val jointPain: String? = null,
    @SerializedName("hydration_l") val hydrationL: Double? = null,
    @SerializedName("protein_g") val proteinG: Double? = null,
    @SerializedName("steps") val steps: Int? = null,
    @SerializedName("ready_to_train") val readyToTrain: Boolean? = null,
    @SerializedName("message") val message: String? = null // optional freeform message
)

/**
 * Mirrors the response shape from checkin.js: res.json({ reply: humanText, structured }).
 * `structured` is whatever shape the coach persona's JSON logging block used
 * that turn (see AGENT_PROMPT.md's "Structured output contract") - the app
 * doesn't need to parse it for Phase 1, just display `reply`, so it's typed
 * loosely as a raw JSON element rather than a fixed data class.
 */
data class CheckInResponse(
    @SerializedName("reply") val reply: String?,
    @SerializedName("structured") val structured: com.google.gson.JsonElement?
)

/** Mirrors the error shape the backend returns on failure (see checkin.js / auth.js). */
data class ApiErrorBody(
    @SerializedName("error") val error: String?,
    @SerializedName("detail") val detail: String?
)
