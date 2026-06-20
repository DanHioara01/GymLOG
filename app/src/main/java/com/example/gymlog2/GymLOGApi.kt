package com.example.gymlog2

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface GymLOGApi {
    // Users
    @POST("users")
    suspend fun upsertUser(@Body body: Map<String, String>): Map<String, Any>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): Map<String, Any>

    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): List<Map<String, Any>>

    // Friendships
    @POST("friends/request")
    suspend fun sendFriendRequest(@Body body: Map<String, String>): Map<String, Any>

    @GET("friends/incoming/{userId}")
    suspend fun getIncomingRequests(@Path("userId") userId: String): List<FriendshipEntity>

    @POST("friends/accept")
    suspend fun acceptFriendRequest(@Body body: Map<String, String>): Map<String, Any>

    @POST("friends/reject")
    suspend fun rejectFriendRequest(@Body body: Map<String, String>): Map<String, Any>

    @POST("friends/remove")
    suspend fun removeFriend(@Body body: Map<String, String>): Map<String, Any>

    @GET("friends/{userId}")
    suspend fun getFriends(@Path("userId") userId: String): List<FriendshipEntity>

    // Feed & Posts
    @POST("posts")
    suspend fun createPost(@Body body: Map<String, String>): Map<String, Any>

    @GET("feed")
    suspend fun getFeed(@Query("limit") limit: Int): List<FeedPostEntity>

    @GET("posts/author/{authorId}")
    suspend fun getPostsByAuthor(@Path("authorId") authorId: String): List<FeedPostEntity>

    // Comments & Likes
    @POST("comments")
    suspend fun comment(@Body body: Map<String, String>): Map<String, Any>

    @GET("comments/{postId}")
    suspend fun getComments(@Path("postId") postId: Long): List<CommentEntity>

    @POST("posts/{postId}/like")
    suspend fun likePost(@Path("postId") postId: Long, @Body body: Map<String, String>): Map<String, Any>

    @DELETE("posts/{postId}/like")
    suspend fun unlikePost(@Path("postId") postId: Long, @Query("userId") userId: String): Map<String, Any>

    @GET("posts/{postId}/likes/count")
    suspend fun getLikesCount(@Path("postId") postId: Long): Map<String, Int>

    // Leaderboard
    @POST("leaderboard")
    suspend fun upsertLeaderboardEntry(@Body body: Map<String, Any>): Map<String, Any>

    @GET("leaderboard")
    suspend fun getLeaderboard(@Query("metric") metric: String, @Query("limit") limit: Int): List<Map<String, Any>>

    // Streaks & Workout Logging (Gamification)
    @POST("workouts/log")
    suspend fun logWorkout(@Body body: Map<String, Any>): WorkoutLogResponse

    @GET("streaks/{userId}")
    suspend fun getStreak(@Path("userId") userId: String): StreakEntity

    // Badges
    @GET("badges")
    suspend fun getAllBadges(): List<BadgeEntity>

    @GET("badges/user/{userId}")
    suspend fun getBadgesForUser(@Path("userId") userId: String): List<UserBadgeEntity>

    @POST("badges/award")
    suspend fun awardBadge(@Body body: Map<String, String>): Map<String, Any>
}

data class WorkoutLogResponse(
    val success: Boolean,
    val stats: Map<String, Int>,
    val streak: StreakResponse,
    val newlyAwardedBadges: List<String>
)

data class StreakResponse(
    val currentStreak: Int,
    val bestStreak: Int,
    val lastDate: Long
)

object NetworkClient {
    private const val DEFAULT_URL = "http://192.168.100.5:4242/"
    private var currentUrl: String = DEFAULT_URL
    private var currentApi: GymLOGApi? = null

    fun getApi(serverUrl: String? = null): GymLOGApi {
        val url = (serverUrl?.takeIf { it.isNotBlank() } ?: currentUrl).trimEnd('/') + "/"
        if (url == currentUrl && currentApi != null) return currentApi!!
        currentUrl = url
        currentApi = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GymLOGApi::class.java)
        return currentApi!!
    }

    val api: GymLOGApi get() = getApi()
}
