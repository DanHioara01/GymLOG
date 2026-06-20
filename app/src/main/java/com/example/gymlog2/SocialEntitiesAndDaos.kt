package com.example.gymlog2

import androidx.room.*

@Entity(tableName = "friendships")
data class FriendshipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val friendId: String,
    val status: String = "accepted",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "feed_posts")
data class FeedPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorId: String,
    val content: String,
    val activityType: String = "post",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val authorId: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "likes")
data class LikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val metric: String,
    val value: Double,
    val periodStart: Long,
    val periodEnd: Long
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val key: String,
    val title: String,
    val description: String,
    val icon: String = ""
)

@Entity(tableName = "user_badges")
data class UserBadgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val badgeKey: String,
    val awardedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastDate: Long = 0
)

@Dao
interface FriendshipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friendship: FriendshipEntity)

    @Query("SELECT * FROM friendships WHERE (userId = :userId OR friendId = :userId) AND status = 'accepted'")
    suspend fun getFriendsFor(userId: String): List<FriendshipEntity>

    @Query("DELETE FROM friendships WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM friendships WHERE friendId = :userId AND status = 'pending'")
    suspend fun getIncomingRequests(userId: String): List<FriendshipEntity>

    @Query("SELECT * FROM friendships WHERE userId = :userId AND friendId = :friendId LIMIT 1")
    suspend fun getBetween(userId: String, friendId: String): FriendshipEntity?

    @Query("SELECT * FROM friendships WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FriendshipEntity?

    @Query("UPDATE friendships SET status = 'accepted' WHERE id = :id")
    suspend fun accept(id: Long)
}

@Dao
interface FeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: FeedPostEntity)

    @Query("SELECT * FROM feed_posts ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<FeedPostEntity>

    @Query("SELECT * FROM feed_posts WHERE authorId = :authorId ORDER BY createdAt DESC")
    suspend fun getByAuthor(authorId: String): List<FeedPostEntity>
}

@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    suspend fun getForPost(postId: Long): List<CommentEntity>

    @Query("SELECT COUNT(*) FROM comments WHERE authorId = :userId")
    suspend fun countForUser(userId: String): Int
}

@Dao
interface LikeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("SELECT COUNT(*) FROM likes WHERE postId = :postId")
    suspend fun countForPost(postId: Long): Int

    @Query("DELETE FROM likes WHERE postId = :postId AND userId = :userId")
    suspend fun removeLike(postId: Long, userId: String)

    @Query("SELECT COUNT(*) > 0 FROM likes WHERE postId = :postId AND userId = :userId")
    suspend fun isLikedByUser(postId: Long, userId: String): Boolean
}

@Dao
interface LeaderboardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LeaderboardEntryEntity)

    @Query("SELECT * FROM leaderboard_entries WHERE metric = :metric ORDER BY value DESC LIMIT :limit")
    suspend fun top(metric: String, limit: Int = 50): List<LeaderboardEntryEntity>
}

@Dao
interface BadgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(badge: BadgeEntity)

    @Query("SELECT * FROM badges")
    suspend fun getAll(): List<BadgeEntity>
}

@Dao
interface UserBadgeDao {
    @Insert
    suspend fun award(badge: UserBadgeEntity)

    @Query("SELECT * FROM user_badges WHERE userId = :userId")
    suspend fun getForUser(userId: String): List<UserBadgeEntity>
}

@Dao
interface StreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streak: StreakEntity)

    @Query("SELECT * FROM streaks WHERE userId = :userId LIMIT 1")
    suspend fun getForUser(userId: String): StreakEntity?
}
