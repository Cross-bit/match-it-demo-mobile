package com.example.matchit.data.remote.client.ApiErrors

/**
 * Informs about the invalid fields in the request
 */
data class ValidationErrorDetail(
    val field: String,
    val code: ValidationCode,
    val msg: String? = null,
    val value: String? = null,
    val meta: Map<String, Any>? = null
)

enum class ValidationCode(val code: String) {
    REQUIRED("REQUIRED"),
    INVALID_FORMAT("INVALID_FORMAT"),
    TOO_SHORT("TOO_SHORT"),
    TOO_LONG("TOO_LONG"),
    OUT_OF_RANGE("OUT_OF_RANGE"),

    INVALID_EMAIL("INVALID_EMAIL"),
    INVALID_UUID("INVALID_UUID"),
    NOT_AN_ARRAY("NOT_AN_ARRAY"),
    ARRAY_TOO_SMALL("ARRAY_TOO_SMALL"),
    ARRAY_TOO_LARGE("ARRAY_TOO_LARGE"),
    INVALID_ENUM_VALUE("INVALID_ENUM_VALUE"),

    WEAK_PASSWORD("WEAK_PASSWORD"),

    INVALID_FILE("INVALID_FILE"),
    FILE_REQUIRED("FILE_REQUIRED"),
    FILE_TOO_LARGE("FILE_TOO_LARGE"),
    UNEXPECTED_FILE_FIELD("UNEXPECTED_FILE_FIELD"),
    FILE_TYPE_NOT_ALLOWED("FILE_TYPE_NOT_ALLOWED"),
    IMAGE_TYPE_NOT_ALLOWED("IMAGE_TYPE_NOT_ALLOWED")
}

typealias ValidationErrors = List<ValidationErrorDetail>