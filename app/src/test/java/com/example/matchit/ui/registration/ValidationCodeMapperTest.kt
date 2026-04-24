package com.example.matchit.ui.registration

import com.example.matchit.R
import com.example.matchit.data.remote.client.ApiErrors.ValidationCode
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidationCodeMapperTest {

    @Test fun required_maps_to_required_error() = assertMapping(ValidationCode.REQUIRED, R.string.error_required)
    @Test fun invalid_format_maps_to_invalid_format_error() = assertMapping(ValidationCode.INVALID_FORMAT, R.string.error_invalid_format)
    @Test fun too_short_maps_to_too_short_error() = assertMapping(ValidationCode.TOO_SHORT, R.string.error_too_short)
    @Test fun too_long_maps_to_too_long_error() = assertMapping(ValidationCode.TOO_LONG, R.string.error_too_long)
    @Test fun out_of_range_maps_to_out_of_range_error() = assertMapping(ValidationCode.OUT_OF_RANGE, R.string.error_out_of_range)
    @Test fun invalid_email_maps_to_invalid_email_error() = assertMapping(ValidationCode.INVALID_EMAIL, R.string.error_invalid_email)
    @Test fun invalid_uuid_maps_to_invalid_uuid_error() = assertMapping(ValidationCode.INVALID_UUID, R.string.error_invalid_uuid)
    @Test fun not_an_array_maps_to_not_an_array_error() = assertMapping(ValidationCode.NOT_AN_ARRAY, R.string.error_not_an_array)
    @Test fun array_too_small_maps_to_array_too_small_error() = assertMapping(ValidationCode.ARRAY_TOO_SMALL, R.string.error_array_too_small)
    @Test fun array_too_large_maps_to_array_too_large_error() = assertMapping(ValidationCode.ARRAY_TOO_LARGE, R.string.error_array_too_large)
    @Test fun invalid_enum_maps_to_invalid_enum_error() = assertMapping(ValidationCode.INVALID_ENUM_VALUE, R.string.error_invalid_enum)
    @Test fun weak_password_maps_to_weak_password_error() = assertMapping(ValidationCode.WEAK_PASSWORD, R.string.error_weak_password)
    @Test fun invalid_file_maps_to_invalid_file_error() = assertMapping(ValidationCode.INVALID_FILE, R.string.error_invalid_file)
    @Test fun file_required_maps_to_file_required_error() = assertMapping(ValidationCode.FILE_REQUIRED, R.string.error_file_required)
    @Test fun file_too_large_maps_to_file_too_large_error() = assertMapping(ValidationCode.FILE_TOO_LARGE, R.string.error_file_too_large)
    @Test fun unexpected_file_field_maps_to_unexpected_file_error() = assertMapping(ValidationCode.UNEXPECTED_FILE_FIELD, R.string.error_unexpected_file)
    @Test fun file_type_not_allowed_maps_to_file_type_not_allowed_error() = assertMapping(ValidationCode.FILE_TYPE_NOT_ALLOWED, R.string.error_file_type_not_allowed)
    @Test fun image_type_not_allowed_maps_to_image_type_not_allowed_error() = assertMapping(ValidationCode.IMAGE_TYPE_NOT_ALLOWED, R.string.error_image_type_not_allowed)

    private fun assertMapping(code: ValidationCode, expected: Int) {
        assertEquals(expected, code.toStringRes())
    }
}
