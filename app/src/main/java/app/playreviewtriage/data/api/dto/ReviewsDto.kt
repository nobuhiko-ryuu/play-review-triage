package app.playreviewtriage.data.api.dto

import com.google.gson.annotations.SerializedName

data class ReviewListResponseDto(
    val reviews: List<ReviewDto>?,
    val tokenPagination: TokenPaginationDto?,
)

data class TokenPaginationDto(val nextPageToken: String?)

data class ReviewDto(
    val reviewId: String?,
    val authorName: String?,
    val comments: List<CommentDto>?,
)

data class CommentDto(
    val userComment: UserCommentDto?,
    val developerComment: DeveloperCommentDto?,
)

data class UserCommentDto(
    val text: String?,
    val lastModified: TimestampDto?,
    val starRating: Int?,
    val reviewerLanguage: String?,
    val appVersionName: String?,
    val appVersionCode: Int?,
    val androidOsVersion: Int?,
    val deviceMetadata: DeviceMetadataDto?,
)

data class DeveloperCommentDto(val text: String?, val lastModified: TimestampDto?)

data class TimestampDto(val seconds: Long?, val nanos: Int?)

data class DeviceMetadataDto(
    val productName: String?,
    val manufacturer: String?,
)
