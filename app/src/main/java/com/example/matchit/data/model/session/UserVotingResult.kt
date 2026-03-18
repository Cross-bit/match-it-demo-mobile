package com.example.matchit.data.model.session

data class SessionMetadata  (
    val sessionParameters: SessionParameters?,
    val sensorData: SensorData?
)

data class SensorData (
    val location: GpsCoordinates?
)

data class UserVotingResult(
    val itemId: String,
    val rating: Int // -1 0 1
)

data class SessionUpdateRequestDTO(
    val sessionUUID: String,
    val votingResult: List<UserVotingResult>,
    val sessionMetadata: SessionMetadata?
)