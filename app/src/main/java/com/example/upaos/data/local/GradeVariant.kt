package com.example.upaos.data.local

import com.example.upaos.data.model.GradesResponse
import com.google.gson.Gson

fun gradeVariantKey(user: String, term: String, career: String) = "grade_variant_${user}_${term}_$career"

/** Old period-only keys are usable only when their own identity matches the selection. */
fun restoreGradeVariant(json: String?, term: String, career: String): GradesResponse? {
    if (json == null) return null
    return try {
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        val storedTerm = root.get("periodo")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        val storedCareer = root.get("carrera")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        // Gson's default UG must not invent a career for an incomplete stored response.
        if (storedTerm != term || storedCareer != career || root.get("cursos")?.isJsonArray != true) null
        else Gson().fromJson(root, GradesResponse::class.java)
    } catch (_: Exception) { null }
}
