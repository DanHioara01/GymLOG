package com.example.gymlog2

class BadgeEngine(private val db: AppDatabase) {
    companion object {
        val ALL_BADGES = listOf(
            BadgeEntity(key = "first_workout", title = "First Workout", description = "Completed your first workout", icon = "🏋️"),
            BadgeEntity(key = "7day_streak", title = "7-Day Streak", description = "Trained 7 days in a row", icon = "🔥"),
            BadgeEntity(key = "30day_streak", title = "30-Day Streak", description = "Trained 30 days in a row", icon = "🔥"),
            BadgeEntity(key = "pr_machine", title = "PR Machine", description = "Set 10 personal records", icon = "🏆"),
            BadgeEntity(key = "century_club", title = "Century Club", description = "Logged 100 workouts", icon = "💯"),
            BadgeEntity(key = "social_butterfly", title = "Social Butterfly", description = "Added 10 friends", icon = "🦋"),
            BadgeEntity(key = "helping_hand", title = "Helping Hand", description = "Commented on 10 posts", icon = "🤝"),
            BadgeEntity(key = "1000kg_club", title = "1000kg Club", description = "Lifted 1000kg total in one session", icon = "💪")
        )
    }

    suspend fun seedBadges() {
        for (badge in ALL_BADGES) {
            db.badgeDao().upsert(badge)
        }
    }

    suspend fun awardIfAbsent(userId: String, badgeKey: String): Boolean {
        val existing = db.userBadgeDao().getForUser(userId)
        if (existing.none { it.badgeKey == badgeKey }) {
            db.userBadgeDao().award(UserBadgeEntity(userId = userId, badgeKey = badgeKey))
            return true
        }
        return false
    }

    suspend fun checkAndAward(userId: String): List<String> {
        val newlyAwarded = mutableListOf<String>()
        val workouts = db.antrenamentDao().getAllForUser(userId)
        if (workouts.isNotEmpty() && awardIfAbsent(userId, "first_workout")) newlyAwarded.add("first_workout")
        if (workouts.size >= 100 && awardIfAbsent(userId, "century_club")) newlyAwarded.add("century_club")

        val streak = db.streakDao().getForUser(userId)
        if (streak != null && streak.currentStreak >= 7 && awardIfAbsent(userId, "7day_streak")) newlyAwarded.add("7day_streak")
        if (streak != null && streak.currentStreak >= 30 && awardIfAbsent(userId, "30day_streak")) newlyAwarded.add("30day_streak")

        val prCount = db.personalRecordDao().getAllForUser(userId).size
        if (prCount >= 10 && awardIfAbsent(userId, "pr_machine")) newlyAwarded.add("pr_machine")

        val acceptedFriends = db.friendshipDao().getFriendsFor(userId).count { it.status == "accepted" }
        if (acceptedFriends >= 10 && awardIfAbsent(userId, "social_butterfly")) newlyAwarded.add("social_butterfly")

        val myComments = db.commentDao().countForUser(userId)
        if (myComments >= 10 && awardIfAbsent(userId, "helping_hand")) newlyAwarded.add("helping_hand")

        val has1000kgSession = workouts.any { it.totalWeight >= 1000.0 }
        if (has1000kgSession && awardIfAbsent(userId, "1000kg_club")) newlyAwarded.add("1000kg_club")

        return newlyAwarded
    }
}
