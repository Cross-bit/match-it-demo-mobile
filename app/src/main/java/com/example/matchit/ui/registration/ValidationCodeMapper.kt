package com.example.matchit.ui.registration

import com.example.matchit.R
import com.example.matchit.data.remote.client.ApiErrors.ValidationCode


fun ValidationCode.toStringRes(): Int =
    when (this) {
        ValidationCode.REQUIRED -> R.string.error_required
        ValidationCode.INVALID_FORMAT -> R.string.error_invalid_format
        ValidationCode.TOO_SHORT -> R.string.error_too_short
        ValidationCode.TOO_LONG -> R.string.error_too_long
        ValidationCode.OUT_OF_RANGE -> R.string.error_out_of_range

        ValidationCode.INVALID_EMAIL -> R.string.error_invalid_email
        ValidationCode.INVALID_UUID -> R.string.error_invalid_uuid
        ValidationCode.NOT_AN_ARRAY -> R.string.error_not_an_array
        ValidationCode.ARRAY_TOO_SMALL -> R.string.error_array_too_small
        ValidationCode.ARRAY_TOO_LARGE -> R.string.error_array_too_large
        ValidationCode.INVALID_ENUM_VALUE -> R.string.error_invalid_enum

        ValidationCode.WEAK_PASSWORD -> R.string.error_weak_password

        ValidationCode.INVALID_FILE -> R.string.error_invalid_file
        ValidationCode.FILE_REQUIRED -> R.string.error_file_required
        ValidationCode.FILE_TOO_LARGE -> R.string.error_file_too_large
        ValidationCode.UNEXPECTED_FILE_FIELD -> R.string.error_unexpected_file
        ValidationCode.FILE_TYPE_NOT_ALLOWED -> R.string.error_file_type_not_allowed
        ValidationCode.IMAGE_TYPE_NOT_ALLOWED -> R.string.error_image_type_not_allowed
    }

