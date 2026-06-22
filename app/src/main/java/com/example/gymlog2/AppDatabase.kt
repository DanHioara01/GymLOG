package com.example.gymlog2

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "antrenamente")
data class AntrenamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val grupaMusculara: String,
    val data: Long = System.currentTimeMillis(),
    val notes: String = "",
    val totalWeight: Double = 0.0
)

@Entity(tableName = "exercitii", foreignKeys = [ForeignKey(
    entity = AntrenamentEntity::class,
    parentColumns = ["id"],
    childColumns = ["antrenamentId"],
    onDelete = ForeignKey.CASCADE
)])
data class ExercitiuEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val antrenamentId: Long,
    val numeExercitiu: String,
    val setIndex: Int = 0,
    val greutateKg: Double = 0.0,
    val repetari: Int = 0,
    val notes: String = ""
)

@Entity(tableName = "exercises")
data class ExerciseDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val group: String,
    val equipment: String = "",
    val isDefault: Boolean = true,
    val isFavorite: Boolean = false,
    val usageCount: Int = 0
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String
)

@Entity(tableName = "template_exercises", foreignKeys = [ForeignKey(
    entity = TemplateEntity::class,
    parentColumns = ["id"],
    childColumns = ["templateId"],
    onDelete = ForeignKey.CASCADE
)])
data class TemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseName: String,
    val group: String
)

@Entity(tableName = "personal_records")
data class PersonalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val volume: Double = 0.0,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "muscle_recovery")
data class MuscleRecoveryEntity(
    @PrimaryKey val grupaMusculara: String,
    val level: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercise_metadata")
data class ExerciseMetadataEntity(
    @PrimaryKey val exerciseName: String,
    val grupaMusculara: String,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false
)

@Entity(tableName = "biometric_entries")
data class BiometricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val weightKg: Double = 0.0,
    val bodyFatPercent: Double = 0.0,
    val waistCm: Double = 0.0,
    val hipsCm: Double = 0.0,
    val thighsCm: Double = 0.0,
    val chestCm: Double = 0.0,
    val armsCm: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "food_entries")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val barcode: String = "",
    val name: String,
    val brand: String = "",
    val mealType: String = "snack",
    val servingSize: Double = 100.0,
    val servingUnit: String = "g",
    val calories: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class MostFrequentExercise(
    val numeExercitiu: String,
    val cnt: Int
)

@Dao
interface AntrenamentDao {
    @Insert
    suspend fun insert(antrenament: AntrenamentEntity): Long

    @Update
    suspend fun update(antrenament: AntrenamentEntity)

    @Delete
    suspend fun delete(antrenament: AntrenamentEntity)

    @Query("SELECT * FROM antrenamente WHERE userId = :userId ORDER BY data DESC")
    suspend fun getAllForUser(userId: String): List<AntrenamentEntity>

    @Query("SELECT * FROM antrenamente WHERE id = :id")
    suspend fun getById(id: Long): AntrenamentEntity?

    @Query("DELETE FROM antrenamente WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT SUM(totalWeight) FROM antrenamente WHERE userId = :userId AND data BETWEEN :startTime AND :endTime")
    suspend fun getTotalVolume(userId: String, startTime: Long, endTime: Long): Double?

    @Query("SELECT * FROM antrenamente WHERE userId = :userId AND data BETWEEN :startTime AND :endTime ORDER BY data DESC")
    suspend fun getWorkoutsInPeriod(userId: String, startTime: Long, endTime: Long): List<AntrenamentEntity>
}

@Dao
interface ExercitiuDao {
    @Insert
    suspend fun insert(exercitiu: ExercitiuEntity)

    @Insert
    suspend fun insertAll(list: List<ExercitiuEntity>)

    @Update
    suspend fun update(exercitiu: ExercitiuEntity)

    @Delete
    suspend fun delete(exercitiu: ExercitiuEntity)

    @Query("SELECT * FROM exercitii WHERE antrenamentId = :antrenamentId ORDER BY setIndex")
    suspend fun getForAntrenament(antrenamentId: Long): List<ExercitiuEntity>

    @Query("DELETE FROM exercitii WHERE antrenamentId = :antrenamentId")
    suspend fun deleteForAntrenament(antrenamentId: Long)

    @Query("""
        SELECT e.numeExercitiu, COUNT(*) as cnt
        FROM exercitii e
        INNER JOIN antrenamente a ON e.antrenamentId = a.id
        WHERE a.userId = :userId AND a.data BETWEEN :startTime AND :endTime
        GROUP BY e.numeExercitiu
        ORDER BY cnt DESC
        LIMIT 1
    """)
    suspend fun getMostFrequentExercise(userId: String, startTime: Long, endTime: Long): MostFrequentExercise?

    @Query("""
        SELECT e.* FROM exercitii e
        INNER JOIN antrenamente a ON e.antrenamentId = a.id
        WHERE a.userId = :userId AND e.numeExercitiu = :exerciseName
        ORDER BY a.data DESC
    """)
    suspend fun getHistoryForExercise(userId: String, exerciseName: String): List<ExercitiuEntity>

    @Query("""
        SELECT e.* FROM exercitii e
        INNER JOIN antrenamente a ON e.antrenamentId = a.id
        WHERE e.numeExercitiu = :exerciseName
        ORDER BY a.data DESC
    """)
    suspend fun getHistoryForExerciseSimple(exerciseName: String): List<ExercitiuEntity>

    @Query("""
        SELECT e.* FROM exercitii e
        INNER JOIN antrenamente a ON e.antrenamentId = a.id
        WHERE a.userId = :userId AND e.numeExercitiu = :exerciseName
        ORDER BY e.greutateKg DESC LIMIT 1
    """)
    suspend fun getBestSetForExercise(userId: String, exerciseName: String): ExercitiuEntity?
}

@Dao
interface ExerciseDefinitionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exercise: ExerciseDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exercises: List<ExerciseDefinitionEntity>)

    @Query("SELECT * FROM exercises ORDER BY isFavorite DESC, usageCount DESC, `group`, name")
    suspend fun getAll(): List<ExerciseDefinitionEntity>

    @Query("SELECT * FROM exercises WHERE `group` = :groupName ORDER BY isFavorite DESC, usageCount DESC, name")
    suspend fun getByGroup(groupName: String): List<ExerciseDefinitionEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("UPDATE exercises SET usageCount = usageCount + 1 WHERE name = :name")
    suspend fun incrementUsage(name: String)

    @Query("UPDATE exercises SET isFavorite = :isFavorite WHERE name = :name")
    suspend fun setFavorite(name: String, isFavorite: Boolean)

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ExerciseDefinitionEntity?
}

@Dao
interface TemplateDao {
    @Insert
    suspend fun insert(template: TemplateEntity): Long

    @Query("SELECT * FROM templates WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<TemplateEntity>

    @Delete
    suspend fun delete(template: TemplateEntity)
}

@Dao
interface TemplateExerciseDao {
    @Insert
    suspend fun insert(exercise: TemplateExerciseEntity)

    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId")
    suspend fun getForTemplate(templateId: Long): List<TemplateExerciseEntity>
}

@Dao
interface PersonalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pr: PersonalRecordEntity)

    @Query("SELECT * FROM personal_records WHERE userId = :userId AND exerciseName = :exerciseName ORDER BY weight DESC LIMIT 1")
    suspend fun getBest(userId: String, exerciseName: String): PersonalRecordEntity?

    @Query("SELECT * FROM personal_records WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllForUser(userId: String): List<PersonalRecordEntity>

    @Query("SELECT * FROM personal_records WHERE userId = :userId ORDER BY weight DESC")
    suspend fun getAllSortedByWeight(userId: String): List<PersonalRecordEntity>
}

@Dao
interface MuscleRecoveryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recovery: MuscleRecoveryEntity)

    @Query("SELECT * FROM muscle_recovery WHERE grupaMusculara = :grupa")
    suspend fun getByGroup(grupa: String): MuscleRecoveryEntity?

    @Query("SELECT * FROM muscle_recovery")
    suspend fun getAll(): List<MuscleRecoveryEntity>
}

@Dao
interface BiometricDao {
    @Insert
    suspend fun insert(entry: BiometricEntity): Long

    @Update
    suspend fun update(entry: BiometricEntity)

    @Delete
    suspend fun delete(entry: BiometricEntity)

    @Query("DELETE FROM biometric_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM biometric_entries WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllForUser(userId: String): List<BiometricEntity>

    @Query("SELECT * FROM biometric_entries WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(userId: String): BiometricEntity?

    @Query("SELECT * FROM biometric_entries WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(userId: String, limit: Int): List<BiometricEntity>
}

@Dao
interface FoodDao {
    @Insert
    suspend fun insert(entry: FoodEntity): Long

    @Update
    suspend fun update(entry: FoodEntity)

    @Delete
    suspend fun delete(entry: FoodEntity)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM food_entries WHERE userId = :userId AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getForDay(userId: String, start: Long, end: Long): List<FoodEntity>

    @Query("SELECT * FROM food_entries WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(userId: String, limit: Int): List<FoodEntity>

    @Query("SELECT SUM(calories) FROM food_entries WHERE userId = :userId AND timestamp BETWEEN :start AND :end")
    suspend fun getTotalCalories(userId: String, start: Long, end: Long): Double?

    @Query("SELECT SUM(proteinG) FROM food_entries WHERE userId = :userId AND timestamp BETWEEN :start AND :end")
    suspend fun getTotalProtein(userId: String, start: Long, end: Long): Double?

    @Query("SELECT SUM(carbsG) FROM food_entries WHERE userId = :userId AND timestamp BETWEEN :start AND :end")
    suspend fun getTotalCarbs(userId: String, start: Long, end: Long): Double?

    @Query("SELECT SUM(fatG) FROM food_entries WHERE userId = :userId AND timestamp BETWEEN :start AND :end")
    suspend fun getTotalFat(userId: String, start: Long, end: Long): Double?
}

@Dao
interface ExerciseMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: ExerciseMetadataEntity)

    @Query("SELECT * FROM exercise_metadata WHERE exerciseName = :name LIMIT 1")
    suspend fun getByName(name: String): ExerciseMetadataEntity?

    @Query("SELECT * FROM exercise_metadata WHERE grupaMusculara = :grupa")
    suspend fun getByGroup(grupa: String): List<ExerciseMetadataEntity>

    @Query("SELECT * FROM exercise_metadata")
    suspend fun getAll(): List<ExerciseMetadataEntity>

    @Query("UPDATE exercise_metadata SET isFavorite = :isFavorite WHERE exerciseName = :name")
    suspend fun setFavorite(name: String, isFavorite: Boolean)
}

@Database(
    entities = [
        AntrenamentEntity::class,
        ExercitiuEntity::class,
        ExerciseDefinitionEntity::class,
        TemplateEntity::class,
        TemplateExerciseEntity::class,
        PersonalRecordEntity::class,
        MuscleRecoveryEntity::class,
        ExerciseMetadataEntity::class,
        FriendshipEntity::class,
        FeedPostEntity::class,
        CommentEntity::class,
        LikeEntity::class,
        LeaderboardEntryEntity::class,
        BadgeEntity::class,
        UserBadgeEntity::class,
        StreakEntity::class,
        SubscriptionEntity::class,
        FeatureFlagEntity::class,
        UserProfileEntity::class,
        BiometricEntity::class,
        FoodEntity::class,
        CardioSessionEntity::class,
        DailyActivityEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun antrenamentDao(): AntrenamentDao
    abstract fun exercitiuDao(): ExercitiuDao
    abstract fun exerciseDefinitionDao(): ExerciseDefinitionDao
    abstract fun templateDao(): TemplateDao
    abstract fun templateExerciseDao(): TemplateExerciseDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun muscleRecoveryDao(): MuscleRecoveryDao
    abstract fun exerciseMetadataDao(): ExerciseMetadataDao
    abstract fun friendshipDao(): FriendshipDao
    abstract fun feedDao(): FeedDao
    abstract fun commentDao(): CommentDao
    abstract fun likeDao(): LikeDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun badgeDao(): BadgeDao
    abstract fun userBadgeDao(): UserBadgeDao
    abstract fun streakDao(): StreakDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun featureFlagDao(): FeatureFlagDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun biometricDao(): BiometricDao
    abstract fun foodDao(): FoodDao
    abstract fun cardioSessionDao(): CardioSessionDao
    abstract fun dailyActivityDao(): DailyActivityDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS friendships (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, friendId TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'pending', createdAt INTEGER NOT NULL DEFAULT 0)")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS feed_posts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, authorId TEXT NOT NULL, content TEXT NOT NULL, activityType TEXT NOT NULL DEFAULT 'post', createdAt INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS comments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, postId INTEGER NOT NULL, authorId TEXT NOT NULL, content TEXT NOT NULL, createdAt INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS likes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, postId INTEGER NOT NULL, userId TEXT NOT NULL, createdAt INTEGER NOT NULL DEFAULT 0)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS leaderboard_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, metric TEXT NOT NULL, value REAL NOT NULL, periodStart INTEGER NOT NULL, periodEnd INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS badges (key TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL, icon TEXT NOT NULL DEFAULT '')")
                db.execSQL("CREATE TABLE IF NOT EXISTS user_badges (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, badgeKey TEXT NOT NULL, awardedAt INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS streaks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, currentStreak INTEGER NOT NULL DEFAULT 0, bestStreak INTEGER NOT NULL DEFAULT 0, lastDate INTEGER NOT NULL DEFAULT 0)")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS subscriptions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, provider TEXT NOT NULL DEFAULT 'stripe', subscriptionId TEXT NOT NULL DEFAULT '', planId TEXT NOT NULL DEFAULT '', status TEXT NOT NULL DEFAULT 'inactive', currentPeriodEnd INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS feature_flags (key TEXT NOT NULL PRIMARY KEY, enabled INTEGER NOT NULL DEFAULT 0)")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS personal_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, exerciseName TEXT NOT NULL, weight REAL NOT NULL, reps INTEGER NOT NULL, date INTEGER NOT NULL DEFAULT 0)")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercitii ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercises ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN usageCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE personal_records ADD COLUMN volume REAL NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS muscle_recovery (grupaMusculara TEXT NOT NULL PRIMARY KEY, level REAL NOT NULL DEFAULT 0, lastUpdated INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS exercise_metadata (exerciseName TEXT NOT NULL PRIMARY KEY, grupaMusculara TEXT NOT NULL, isFavorite INTEGER NOT NULL DEFAULT 0, isCustom INTEGER NOT NULL DEFAULT 0)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN equipment TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS user_profiles (userId TEXT NOT NULL PRIMARY KEY, loginKey TEXT NOT NULL, name TEXT NOT NULL, photoUri TEXT NOT NULL DEFAULT '', createdAt INTEGER NOT NULL DEFAULT 0)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS biometric_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL DEFAULT 0,
                        weightKg REAL NOT NULL DEFAULT 0,
                        bodyFatPercent REAL NOT NULL DEFAULT 0,
                        waistCm REAL NOT NULL DEFAULT 0,
                        hipsCm REAL NOT NULL DEFAULT 0,
                        thighsCm REAL NOT NULL DEFAULT 0,
                        chestCm REAL NOT NULL DEFAULT 0,
                        armsCm REAL NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS food_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        barcode TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL,
                        brand TEXT NOT NULL DEFAULT '',
                        mealType TEXT NOT NULL DEFAULT 'snack',
                        servingSize REAL NOT NULL DEFAULT 100,
                        servingUnit TEXT NOT NULL DEFAULT 'g',
                        calories REAL NOT NULL DEFAULT 0,
                        proteinG REAL NOT NULL DEFAULT 0,
                        carbsG REAL NOT NULL DEFAULT 0,
                        fatG REAL NOT NULL DEFAULT 0,
                        fiberG REAL NOT NULL DEFAULT 0,
                        timestamp INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cardio_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL DEFAULT 0,
                        durationMs INTEGER NOT NULL DEFAULT 0,
                        steps INTEGER NOT NULL DEFAULT 0,
                        distanceMeters REAL NOT NULL DEFAULT 0,
                        activeTimeMinutes INTEGER NOT NULL DEFAULT 0,
                        caloriesBurned REAL NOT NULL DEFAULT 0,
                        avgHeartRate INTEGER NOT NULL DEFAULT 0,
                        routeJson TEXT NOT NULL DEFAULT '',
                        sessionType TEXT NOT NULL DEFAULT 'walk'
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_activity (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        date INTEGER NOT NULL DEFAULT 0,
                        steps INTEGER NOT NULL DEFAULT 0,
                        stepsGoal INTEGER NOT NULL DEFAULT 7000,
                        activeTimeMinutes INTEGER NOT NULL DEFAULT 0,
                        activeTimeGoal INTEGER NOT NULL DEFAULT 90,
                        activityCalories REAL NOT NULL DEFAULT 0,
                        activityCaloriesGoal REAL NOT NULL DEFAULT 500,
                        totalCaloriesBurned REAL NOT NULL DEFAULT 0,
                        distanceMeters REAL NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kinetic.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
