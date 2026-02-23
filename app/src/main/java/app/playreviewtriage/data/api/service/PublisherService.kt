package app.playreviewtriage.data.api.service

import app.playreviewtriage.data.api.dto.ReviewListResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PublisherService {
    @GET("androidpublisher/v3/applications/{packageName}/reviews")
    suspend fun listReviews(
        @Path("packageName") packageName: String,
        @Query("translationLanguage") translationLanguage: String = "ja",
        @Query("maxResults") maxResults: Int = 100,
        @Query("token") pageToken: String? = null,
    ): Response<ReviewListResponseDto>
}
