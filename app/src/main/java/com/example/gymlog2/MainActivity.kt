package com.example.gymlog2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferencesManager = PreferencesManager(this)
        LanguageManager.loadSavedLanguage(this)

        setContent {
            val themeMode = remember { mutableStateOf(preferencesManager.getThemeMode()) }
            var showWelcome by remember { mutableStateOf(true) }
            val context = androidx.compose.ui.platform.LocalContext.current
            val strings = LanguageManager.getStrings(context)
            val userName = remember {
                val profile = UserProfileManager(this).getOwnProfile()
                profile?.name?.takeIf { it.isNotBlank() && it != "Guest" && it != "Facebook User" } ?: ""
            }

            GymLOGTheme(themeMode = themeMode.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                ) {
                    if (showWelcome) {
                        WelcomeScreen(
                            userName = userName.ifEmpty { strings.athlete },
                            strings = strings,
                            onFinished = { showWelcome = false }
                        )
                    } else {
                        var mainAlphaTarget by remember { mutableFloatStateOf(0f) }
                        val mainAlpha by animateFloatAsState(
                            targetValue = mainAlphaTarget,
                            animationSpec = tween(500),
                            label = "mainAlpha"
                        )
                        LaunchedEffect(Unit) {
                            mainAlphaTarget = 1f
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(mainAlpha),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            MuscleGroupList(
                                onThemeChanged = { themeMode.value = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// Helper functions for weight conversion
// ============================================
private fun convertWeight(kg: Double, isLbs: Boolean): Double = if (isLbs) kg * 2.20462 else kg
internal fun weightLabel(kg: Double, isLbs: Boolean): String {
    val value = if (isLbs) kg * 2.20462 else kg
    val unit = if (isLbs) "lbs" else "kg"
    return if (value == value.toLong().toDouble()) "${value.toLong()} $unit" else "${String.format("%.1f", value)} $unit"
}

// ============================================
// Ecranul 1: Lista de Grupe Musculare
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleGroupList(onThemeChanged: (ThemeMode) -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val userProfileManager = remember { UserProfileManager(context) }

    LaunchedEffect(Unit) {
        val savedUrl = preferencesManager.getServerUrl()
        if (savedUrl.isNotBlank()) {
            NetworkClient.getApi(savedUrl)
        }
    }

    var isLoggedIn by remember { mutableStateOf(preferencesManager.isLoggedIn()) }
    var showOnboarding by remember {
        mutableStateOf(isLoggedIn && !preferencesManager.isOnboardingComplete() &&
            (userProfileManager.getOwnProfile()?.name ?: "").let { it.isEmpty() || it == "Guest" || it == "Facebook User" })
    }
    var showProfileSetup by remember { mutableStateOf(false) }
    var selectedGroup: String? by remember { mutableStateOf(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var showRecovery by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showUnitsDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf<DrawerPage?>(null) }
    var currentDashboardTab by remember { mutableIntStateOf(0) }
    var isLbs by remember { mutableStateOf(preferencesManager.isLbs()) }
    var currentLanguage by remember { mutableStateOf(LanguageManager.getLanguage()) }
    var currentThemeMode by remember { mutableStateOf(preferencesManager.getThemeMode()) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var badgeCheckTrigger by remember { mutableIntStateOf(0) }
    var newBadgeNotifications by remember { mutableStateOf<List<String>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }

    val strings = LanguageManager.getStrings(context)
    val isDark = when (currentThemeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val profile = userProfileManager.getOwnProfile()
    val profileName = profile?.name ?: strings.guest
    val profilePhoto = profile?.photoUri ?: ""
    val userId = profile?.userId ?: userProfileManager.getOwnUserId()

    LaunchedEffect(isLoggedIn, userId, profileName, profilePhoto) {
        if (isLoggedIn && userId != "local_user") {
            kotlinx.coroutines.Dispatchers.IO.let { dispatcher ->
                kotlinx.coroutines.withContext(dispatcher) {
                    try {
                        FirestoreHelper().saveFcmToken(userId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        if (profileName.isNotBlank()) {
                            FirestoreHelper().saveUserProfile(userId, profileName, profilePhoto)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        if (profileName.isNotBlank()) {
                            val db = AppDatabase.getDatabase(context)
                            SocialRepository(db).syncUserProfile(userId, profileName, profilePhoto)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    var weeklyTopExercise by remember { mutableStateOf<String?>(null) }
    var weeklyTotalKg by remember { mutableDoubleStateOf(0.0) }
    var todayExercises by remember { mutableStateOf<List<String>>(emptyList()) }
    var todayVolume by remember { mutableDoubleStateOf(0.0) }
    var lastPR by remember { mutableStateOf<PersonalRecordEntity?>(null) }
    var recoveryMap by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var currentStreak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var badgeCount by remember { mutableIntStateOf(0) }
    var recentBadges by remember { mutableStateOf<List<BadgeEntity>>(emptyList()) }

    var showBiometricInput by remember { mutableStateOf(false) }
    var showBiometricCharts by remember { mutableStateOf(false) }
    var lastBiometric by remember { mutableStateOf<BiometricEntity?>(null) }
    var allBiometrics by remember { mutableStateOf<List<BiometricEntity>>(emptyList()) }
    var weeksSinceMeasurement by remember { mutableIntStateOf(-1) }

    var showFoodJournal by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showAddFood by remember { mutableStateOf(false) }
    var foodEntries by remember { mutableStateOf<List<FoodEntity>>(emptyList()) }
    var pendingFoodProduct by remember { mutableStateOf<FoodProduct?>(null) }
    var showAiTrainer by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }

    val onDashboard = selectedGroup == null && !showCalendar && !showRecovery && !showTemplates && !showBiometricInput && !showBiometricCharts && !showFoodJournal && !showBarcodeScanner && !showAddFood && !showAiTrainer

    LaunchedEffect(badgeCheckTrigger) {
        if (badgeCheckTrigger > 0 && userId != "local_user") {
            kotlinx.coroutines.Dispatchers.IO.let { dispatcher ->
                kotlinx.coroutines.withContext(dispatcher) {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val sm = StreakManager(db)
                        sm.recordWorkout(userId)
                        val be = BadgeEngine(db)
                        val newBadges = be.checkAndAward(userId)
                        if (newBadges.isNotEmpty()) {
                            newBadgeNotifications = newBadges
                            reloadToken++
                        }
                    } catch (_: Exception) { }
                }
            }
        }
    }

    LaunchedEffect(newBadgeNotifications) {
        if (newBadgeNotifications.isNotEmpty()) {
            val badgeNames = newBadgeNotifications.joinToString(", ") { key ->
                BadgeEngine.ALL_BADGES.find { it.key == key }?.title ?: key
            }
            snackbarHostState.showSnackbar("🏆 $badgeNames")
            newBadgeNotifications = emptyList()
        }
    }

    LaunchedEffect(reloadToken, userId) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val bm = BiometricManager(db)
                lastBiometric = bm.getLatest(userId)
                allBiometrics = bm.getAll(userId)
                weeksSinceMeasurement = bm.getWeeksSinceLastMeasurement(userId)
                val fm = FoodManager(db)
                foodEntries = fm.getAll(userId)
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(reloadToken, onDashboard) {
        kotlinx.coroutines.Dispatchers.IO.let { dispatcher ->
            kotlinx.coroutines.withContext(dispatcher) {
                val db = AppDatabase.getDatabase(context)
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val dayStart = cal.timeInMillis
                val dayEnd = System.currentTimeMillis()
                cal.timeInMillis = dayStart
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val weekStart = cal.timeInMillis
                cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                val weekEnd = cal.timeInMillis

                weeklyTotalKg = db.antrenamentDao().getTotalVolume("simple", weekStart, weekEnd) ?: 0.0
                val mostFrequent = db.exercitiuDao().getMostFrequentExercise("simple", weekStart, weekEnd)
                weeklyTopExercise = mostFrequent?.numeExercitiu

                val todayWorkouts = db.antrenamentDao().getWorkoutsInPeriod("simple", dayStart, dayEnd)
                todayVolume = todayWorkouts.sumOf { it.totalWeight }
                val exerciseNames = mutableListOf<String>()
                for (w in todayWorkouts) {
                    val exercises = db.exercitiuDao().getForAntrenament(w.id)
                    exercises.forEach { if (it.numeExercitiu !in exerciseNames) exerciseNames.add(it.numeExercitiu) }
                }
                todayExercises = exerciseNames

                val prs = db.personalRecordDao().getAllForUser("simple")
                lastPR = prs.firstOrNull()

                val antrenamentRepo = AntrenamentRepository(db)
                recoveryMap = antrenamentRepo.getToateRecuperarile().toMap()

                val streakEntity = db.streakDao().getForUser(userId)
                currentStreak = streakEntity?.currentStreak ?: 0
                bestStreak = streakEntity?.bestStreak ?: 0

                val userBadges = db.userBadgeDao().getForUser(userId)
                badgeCount = userBadges.size
                val allBadges = db.badgeDao().getAll()
                val badgeMap = allBadges.associateBy { it.key }
                recentBadges = userBadges.mapNotNull { badgeMap[it.badgeKey] }
            }
        }
    }

    val exportCsvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                val db = AppDatabase.getDatabase(context)
                val workouts = kotlinx.coroutines.runBlocking { db.antrenamentDao().getAllForUser("simple") }
                val exercises = mutableMapOf<Long, List<ExercitiuEntity>>()
                for (w in workouts) {
                    exercises[w.id] = kotlinx.coroutines.runBlocking { db.exercitiuDao().getForAntrenament(w.id) }
                }
                os.bufferedWriter().use { w ->
                    w.write("Date,Group,Exercise,Set,WeightKg,Reps")
                    w.newLine()
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    for (aw in workouts) {
                        val exList = exercises[aw.id] ?: emptyList()
                        for (ex in exList) {
                            w.write("${sdf.format(java.util.Date(aw.data))},${aw.grupaMusculara},${ex.numeExercitiu},${ex.setIndex + 1},${ex.greutateKg},${ex.repetari}")
                            w.newLine()
                        }
                    }
                }
            }
        }
    }

    val importCsvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val importer = CsvImporter(context)
            val sessions = importer.importWorkouts(it)
            val repo = AntrenamentRepository(AppDatabase.getDatabase(context))
            kotlinx.coroutines.runBlocking {
                for (session in sessions) {
                    for (ex in session.exercitii) {
                        repo.salveazaAntrenamentSimple(session.grupaMusculara, ex.numeExercitiu, ex.seturi, "")
                    }
                }
            }
            reloadToken++
        }
    }

    val authManager = remember { AuthManager(context) }
    var googleSignInError by remember { mutableStateOf<String?>(null) }

    val googleSignInClient = remember {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("580154418325-8a0fc9d2icragaf7da62oqdvtk2hjiis.apps.googleusercontent.com")
            .requestEmail()
            .build()
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                kotlinx.coroutines.MainScope().launch {
                    val authResult = authManager.signInWithGoogle(idToken)
                    authResult.onSuccess { firebaseUser ->
                        val db = AppDatabase.getDatabase(context)
                        val firebaseUid = firebaseUser.uid
                        val prefs = context.getSharedPreferences("user_profiles", android.content.Context.MODE_PRIVATE)
                        val userId = kotlinx.coroutines.withContext(Dispatchers.IO) {
                            val savedId = prefs.getString("uid_map_$firebaseUid", null)
                            if (savedId != null) {
                                savedId
                            } else {
                                val existing = db.userProfileDao().getByLoginKey(firebaseUid)
                                if (existing != null) {
                                    prefs.edit().putString("uid_map_$firebaseUid", existing.userId).apply()
                                    existing.userId
                                } else {
                                    var newId: String
                                    while (true) {
                                        newId = (100000..999999).random().toString()
                                        if (db.userProfileDao().getByUserId(newId) == null) break
                                    }
                                    db.userProfileDao().upsert(UserProfileEntity(
                                        userId = newId, loginKey = firebaseUid,
                                        name = firebaseUser.displayName ?: "Google User",
                                        photoUri = firebaseUser.photoUrl?.toString() ?: ""
                                    ))
                                    prefs.edit().putString("uid_map_$firebaseUid", newId).apply()
                                    newId
                                }
                            }
                        }
                        preferencesManager.setLoggedIn(true)
                        preferencesManager.setLoginMethod("google")
                        userProfileManager.createOrUpdateProfile(
                            name = firebaseUser.displayName ?: "Google User",
                            photoUri = firebaseUser.photoUrl?.toString() ?: "",
                            userId = userId
                        )
                        isLoggedIn = true
                    }.onFailure {
                        googleSignInError = it.message
                    }
                }
            }
        } catch (e: Exception) {
            googleSignInError = e.message
        }
    }

    val facebookSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Facebook login callback - handled via CallbackManager
    }

    if (!isLoggedIn) {
        val isDark = when (currentThemeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        }
        LoginScreen(
            strings = strings,
            isDark = isDark,
            error = googleSignInError,
            onEmailLogin = { email, _ ->
                val db = AppDatabase.getDatabase(context)
                val prefs = context.getSharedPreferences("user_profiles", android.content.Context.MODE_PRIVATE)
                val loginKey = "email:$email"
                val userId = runBlocking(Dispatchers.IO) {
                    val savedId = prefs.getString("uid_map_$loginKey", null)
                    if (savedId != null) {
                        savedId
                    } else {
                        val existing = db.userProfileDao().getByLoginKey(loginKey)
                        if (existing != null) {
                            prefs.edit().putString("uid_map_$loginKey", existing.userId).apply()
                            existing.userId
                        } else {
                            var newId: String
                            while (true) {
                                newId = (100000..999999).random().toString()
                                if (db.userProfileDao().getByUserId(newId) == null) break
                            }
                            db.userProfileDao().upsert(UserProfileEntity(
                                userId = newId, loginKey = loginKey,
                                name = email.substringBefore("@"), photoUri = ""
                            ))
                            prefs.edit().putString("uid_map_$loginKey", newId).apply()
                            newId
                        }
                    }
                }
                preferencesManager.setLoggedIn(true)
                preferencesManager.setLoginMethod("email")
                userProfileManager.createOrUpdateProfile(
                    name = email.substringBefore("@"),
                    photoUri = "",
                    userId = userId
                )
                isLoggedIn = true
            },
            onGoogleLogin = {
                googleSignInError = null
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            onFacebookLogin = {
                val db = AppDatabase.getDatabase(context)
                val prefs = context.getSharedPreferences("user_profiles", android.content.Context.MODE_PRIVATE)
                val loginKey = "facebook"
                val userId = runBlocking(Dispatchers.IO) {
                    val savedId = prefs.getString("uid_map_$loginKey", null)
                    if (savedId != null) {
                        savedId
                    } else {
                        val existing = db.userProfileDao().getByLoginKey(loginKey)
                        if (existing != null) {
                            prefs.edit().putString("uid_map_$loginKey", existing.userId).apply()
                            existing.userId
                        } else {
                            var newId: String
                            while (true) {
                                newId = (100000..999999).random().toString()
                                if (db.userProfileDao().getByUserId(newId) == null) break
                            }
                            db.userProfileDao().upsert(UserProfileEntity(
                                userId = newId, loginKey = loginKey,
                                name = "Facebook User", photoUri = ""
                            ))
                            prefs.edit().putString("uid_map_$loginKey", newId).apply()
                            newId
                        }
                    }
                }
                preferencesManager.setLoggedIn(true)
                preferencesManager.setLoginMethod("facebook")
                userProfileManager.createOrUpdateProfile(
                    name = "Facebook User",
                    photoUri = "",
                    userId = userId
                )
                isLoggedIn = true
            },
            onGuestLogin = {
                val db = AppDatabase.getDatabase(context)
                val guestKey = preferencesManager.getGuestKey().ifEmpty {
                    val key = "guest_${(100000..999999).random()}"
                    preferencesManager.setGuestKey(key)
                    key
                }
                val userId = runBlocking(Dispatchers.IO) {
                    val existing = db.userProfileDao().getByLoginKey(guestKey)
                    existing?.userId ?: run {
                        var newId: String
                        while (true) {
                            newId = (100000..999999).random().toString()
                            if (db.userProfileDao().getByUserId(newId) == null) break
                        }
                        db.userProfileDao().upsert(UserProfileEntity(
                            userId = newId, loginKey = guestKey,
                            name = "Guest", photoUri = ""
                        ))
                        newId
                    }
                }
                preferencesManager.setLoggedIn(true)
                preferencesManager.setLoginMethod("guest")
                userProfileManager.createOrUpdateProfile(
                    name = "Guest",
                    photoUri = "",
                    userId = userId
                )
                isLoggedIn = true
            }
        )

        if (showOnboarding) {
            OnboardingScreen(
                strings = strings,
                onProfileComplete = { profile ->
                    preferencesManager.setFitnessGoal(profile.goal)
                    preferencesManager.setExperienceLevel(profile.experience)
                    preferencesManager.setEquipmentAvailable(profile.equipment)
                    preferencesManager.setSessionsPerWeek(profile.sessionsPerWeek)
                    preferencesManager.setPhysicalLimitations(profile.limitations)
                    preferencesManager.setSelectedMuscleGroups(profile.selectedGroups)
                    showOnboarding = false
                    showProfileSetup = true
                }
            )
        } else if (showProfileSetup) {
            ProfileSetupScreen(
                strings = strings,
                onSave = { name, photoUri ->
                    userProfileManager.createOrUpdateProfile(
                        name = name,
                        photoUri = photoUri,
                        userId = userProfileManager.getOwnUserId()
                    )
                    preferencesManager.setOnboardingComplete(true)
                    showProfileSetup = false
                }
            )
        }
        return
    }

    BackHandler {
        when {
            selectedGroup != null -> selectedGroup = null
            showCalendar -> { showCalendar = false; currentPage = null }
            showRecovery -> { showRecovery = false; currentPage = null }
            showTemplates -> { showTemplates = false; currentPage = null }
            currentPage != null -> currentPage = null
            currentDashboardTab != 0 -> currentDashboardTab = 0
            drawerState.isOpen -> { /* let drawer close itself */ }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
                DrawerMenu(
                    profileName = profileName,
                    profilePhotoUri = profilePhoto,
                    userId = userId,
                    currentPage = currentPage,
                    isLbs = isLbs,
                    isDark = isDark,
                    currentLanguage = currentLanguage,
                    badgeCount = badgeCount,
                    currentStreak = currentStreak,
                    onNavigate = { page ->
                        currentPage = page
                        when (page) {
                            null -> { showCalendar = false; showRecovery = false; showTemplates = false; showFoodJournal = false; showBarcodeScanner = false; showAddFood = false; showAiTrainer = false; showFriends = false; showLeaderboard = false; selectedGroup = null }
                            DrawerPage.RECOVERY -> { showCalendar = false; showRecovery = true; showTemplates = false; showFoodJournal = false; showBarcodeScanner = false; showAddFood = false; showAiTrainer = false; showFriends = false; showLeaderboard = false; selectedGroup = null }
                            DrawerPage.CALENDAR -> { showCalendar = true; showRecovery = false; showTemplates = false; showFoodJournal = false; showBarcodeScanner = false; showAddFood = false; showAiTrainer = false; showFriends = false; showLeaderboard = false; selectedGroup = null }
                            DrawerPage.FOOD_JOURNAL -> { showCalendar = false; showRecovery = false; showTemplates = false; showFoodJournal = true; showBarcodeScanner = false; showAddFood = false; showAiTrainer = false; showFriends = false; showLeaderboard = false; selectedGroup = null }
                            DrawerPage.AI_TRAINER -> { showCalendar = false; showRecovery = false; showTemplates = false; showFoodJournal = false; showBarcodeScanner = false; showAddFood = false; showAiTrainer = true; showFriends = false; showLeaderboard = false; selectedGroup = null }
                            DrawerPage.FRIENDS -> { showCalendar = false; showRecovery = false; showTemplates = false; showFoodJournal = false; showBarcodeScanner = false; showAddFood = false; showAiTrainer = false; showFriends = true; showLeaderboard = false; selectedGroup = null }
                        }
                    },
                    onExportCsv = {
                        exportCsvLauncher.launch("kinetic_export.csv")
                    },
                    onImportCsv = {
                        importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*"))
                    },
                    onLogout = {
                        preferencesManager.setLoggedIn(false)
                        preferencesManager.setLoginMethod("")
                        isLoggedIn = false
                    },
                    onLanguageSelected = { code ->
                        LanguageManager.saveLanguage(context, code)
                        currentLanguage = code
                        reloadToken++
                    },
                    onOpenLanguageDialog = { showLanguageDialog = true },
                    onOpenUnitsDialog = { showUnitsDialog = true },
                    strings = strings,
                    onClose = { scope.launch { drawerState.close() } },
                    onOpenServerSettings = { showServerDialog = true }
                )
            }
        ) {
        // Content inside drawer
        val surfaceBg = if (isDark) bgColor() else LightBackground
        val textPrimary = if (isDark) textColor() else LightTextPrimary
        val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
        val cardBg = if (isDark) cardColor() else LightCard
        val accent = if (isDark) accentColor() else LightPrimaryRed
        val iconBg = if (isDark) IconBackground else LightIconBackground

        Scaffold(
            containerColor = surfaceBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(containerColor = surfaceBg) {
                    NavigationBarItem(
                        selected = currentDashboardTab == 0,
                        onClick = {
                            if (currentDashboardTab == 0) {
                                when {
                                    selectedGroup != null -> selectedGroup = null
                                    showRecovery -> { showRecovery = false; currentPage = null }
                                    showTemplates -> { showTemplates = false; currentPage = null }
                                    showCalendar -> { showCalendar = false; currentPage = null }
                                    showFoodJournal -> { showFoodJournal = false; currentPage = null }
                                    showAiTrainer -> { showAiTrainer = false; currentPage = null }
                                    showBarcodeScanner -> { showBarcodeScanner = false; currentPage = null }
                                    showAddFood -> { showAddFood = false; currentPage = null }
                                    showBiometricInput -> { showBiometricInput = false; currentPage = null }
                                    showBiometricCharts -> { showBiometricCharts = false; currentPage = null }
                                    showFriends -> { showFriends = false; currentPage = null }
                                    showLeaderboard -> { showLeaderboard = false; currentPage = null }
                                    currentPage != null -> currentPage = null
                                }
                            } else {
                                currentDashboardTab = 0
                                selectedGroup = null
                                showCalendar = false
                                showRecovery = false
                                showTemplates = false
                                showFoodJournal = false
                                showAiTrainer = false
                                showBarcodeScanner = false
                                showAddFood = false
                                showBiometricInput = false
                                showBiometricCharts = false
                                showFriends = false
                                showLeaderboard = false
                                currentPage = null
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = strings.acasa,
                                tint = if (currentDashboardTab == 0) accent else textSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accent,
                            selectedTextColor = accent,
                            unselectedIconColor = textSecondary,
                            unselectedTextColor = textSecondary,
                            indicatorColor = cardBg
                        )
                    )
                    NavigationBarItem(
                        selected = currentDashboardTab == 1,
                        onClick = {
                            if (currentDashboardTab == 1) {
                                when {
                                    showTemplates -> { showTemplates = false; currentPage = null }
                                    currentPage != null -> currentPage = null
                                }
                            } else {
                                currentDashboardTab = 1
                                selectedGroup = null
                                showCalendar = false
                                showRecovery = false
                                showTemplates = false
                                showFoodJournal = false
                                showAiTrainer = false
                                showBarcodeScanner = false
                                showAddFood = false
                                showBiometricInput = false
                                showBiometricCharts = false
                                showFriends = false
                                showLeaderboard = false
                                currentPage = null
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = strings.workouts,
                                tint = if (currentDashboardTab == 1) accent else textSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accent,
                            selectedTextColor = accent,
                            unselectedIconColor = textSecondary,
                            unselectedTextColor = textSecondary,
                            indicatorColor = cardBg
                        )
                    )
                    NavigationBarItem(
                        selected = currentDashboardTab == 2,
                        onClick = {
                            if (currentDashboardTab == 2) {
                                when {
                                    currentPage != null -> currentPage = null
                                }
                            } else {
                                currentDashboardTab = 2
                                selectedGroup = null
                                showCalendar = false
                                showRecovery = false
                                showTemplates = false
                                showFoodJournal = false
                                showAiTrainer = false
                                showBarcodeScanner = false
                                showAddFood = false
                                showBiometricInput = false
                                showBiometricCharts = false
                                showFriends = false
                                showLeaderboard = false
                                currentPage = null
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = strings.stats,
                                tint = if (currentDashboardTab == 2) accent else textSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accent,
                            selectedTextColor = accent,
                            unselectedIconColor = textSecondary,
                            unselectedTextColor = textSecondary,
                            indicatorColor = cardBg
                        )
                    )
                    NavigationBarItem(
                        selected = currentDashboardTab == 3,
                        onClick = {
                            if (currentDashboardTab == 3) {
                                when {
                                    currentPage != null -> currentPage = null
                                }
                            } else {
                                currentDashboardTab = 3
                                selectedGroup = null
                                showCalendar = false
                                showRecovery = false
                                showTemplates = false
                                showFoodJournal = false
                                showAiTrainer = false
                                showBarcodeScanner = false
                                showAddFood = false
                                showBiometricInput = false
                                showBiometricCharts = false
                                showFriends = false
                                showLeaderboard = false
                                currentPage = null
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.LocalDrink,
                                contentDescription = strings.waterIntake,
                                tint = if (currentDashboardTab == 3) accent else textSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accent,
                            selectedTextColor = accent,
                            unselectedIconColor = textSecondary,
                            unselectedTextColor = textSecondary,
                            indicatorColor = cardBg
                        )
                    )
                    NavigationBarItem(
                        selected = currentDashboardTab == 4,
                        onClick = {
                            if (currentDashboardTab == 4) {
                                when {
                                    showBiometricCharts -> { showBiometricCharts = false; currentPage = null }
                                    showBiometricInput -> { showBiometricInput = false; currentPage = null }
                                    currentPage != null -> currentPage = null
                                }
                            } else {
                                currentDashboardTab = 4
                                selectedGroup = null
                                showCalendar = false
                                showRecovery = false
                                showTemplates = false
                                showFoodJournal = false
                                showAiTrainer = false
                                showBarcodeScanner = false
                                showAddFood = false
                                showBiometricInput = false
                                showBiometricCharts = false
                                showFriends = false
                                showLeaderboard = false
                                currentPage = null
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = strings.profile,
                                tint = if (currentDashboardTab == 4) accent else textSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accent,
                            selectedTextColor = accent,
                            unselectedIconColor = textSecondary,
                            unselectedTextColor = textSecondary,
                            indicatorColor = cardBg
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (showRecovery) {
                    MuscleRecoveryScreen(onBackClick = { showRecovery = false; currentPage = null })
                } else if (showTemplates) {
                    TemplateScreen(onBackClick = { showTemplates = false; currentPage = null })
                } else if (showCalendar) {
                    CalendarWorkoutScreen(onBackClick = { showCalendar = false; currentPage = null })
                } else if (showBiometricInput) {
                    BiometricInputScreen(
                        isDark = isDark,
                        strings = strings,
                        latestEntry = lastBiometric,
                        onSave = { weight, bf, waist, hips, thighs, chest, arms ->
                            kotlinx.coroutines.runBlocking {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val db = AppDatabase.getDatabase(context)
                                    val bm = BiometricManager(db)
                                    bm.saveEntry(userId, weight, bf, waist, hips, thighs, chest, arms)
                                    lastBiometric = bm.getLatest(userId)
                                    allBiometrics = bm.getAll(userId)
                                    weeksSinceMeasurement = bm.getWeeksSinceLastMeasurement(userId)
                                }
                            }
                            showBiometricInput = false
                            reloadToken++
                        },
                        onBack = { showBiometricInput = false }
                    )
                } else if (showBiometricCharts) {
                    BiometricChartScreen(
                        isDark = isDark,
                        strings = strings,
                        entries = allBiometrics,
                        onDelete = { entry ->
                            kotlinx.coroutines.runBlocking {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val db = AppDatabase.getDatabase(context)
                                    val bm = BiometricManager(db)
                                    bm.delete(entry)
                                    lastBiometric = bm.getLatest(userId)
                                    allBiometrics = bm.getAll(userId)
                                    weeksSinceMeasurement = bm.getWeeksSinceLastMeasurement(userId)
                                }
                            }
                        },
                        onBack = { showBiometricCharts = false }
                    )
                } else if (showFoodJournal) {
                    FoodJournalScreen(
                        isDark = isDark,
                        strings = strings,
                        entries = foodEntries,
                        onDelete = { entry ->
                            kotlinx.coroutines.runBlocking {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val db = AppDatabase.getDatabase(context)
                                    val fm = FoodManager(db)
                                    fm.delete(entry)
                                    foodEntries = fm.getAll(userId)
                                }
                            }
                        },
                        onScanBarcode = { showFoodJournal = false; showBarcodeScanner = true },
                        onAddManual = { showFoodJournal = false; showAddFood = true },
                        onBack = { showFoodJournal = false; currentPage = null }
                    )
                } else if (showBarcodeScanner) {
                    BarcodeScannerScreen(
                        isDark = isDark,
                        strings = strings,
                        onBarcodeScanned = { barcode ->
                            showBarcodeScanner = false
                            scope.launch {
                                val product = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    OpenFoodFactsApi.getProduct(barcode)
                                }
                                // Mergem mereu la AddFoodScreen — dacă produsul nu e găsit,
                                // câmpurile sunt goale dar barcode-ul e pre-completat
                                pendingFoodProduct = product
                                showAddFood = true
                            }
                        },
                        onBack = { showBarcodeScanner = false; showFoodJournal = true }
                    )
                } else if (showAddFood) {
                    AddFoodScreen(
                        isDark = isDark,
                        strings = strings,
                        prefilledProduct = pendingFoodProduct,
                        onSave = { name, brand, mealType, servingSize, calories, protein, carbs, fat, fiber ->
                            kotlinx.coroutines.runBlocking {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val db = AppDatabase.getDatabase(context)
                                    val fm = FoodManager(db)
                                    fm.addFood(
                                        userId = userId,
                                        barcode = pendingFoodProduct?.barcode ?: "",
                                        name = name,
                                        brand = brand,
                                        mealType = mealType,
                                        servingSize = servingSize,
                                        servingUnit = pendingFoodProduct?.servingUnit ?: "g",
                                        calories = calories,
                                        proteinG = protein,
                                        carbsG = carbs,
                                        fatG = fat,
                                        fiberG = fiber
                                    )
                                    foodEntries = fm.getAll(userId)
                                    pendingFoodProduct = null
                                }
                            }
                            showAddFood = false
                        },
                        onBack = { showAddFood = false; pendingFoodProduct = null; showFoodJournal = true }
                    )
                } else if (showAiTrainer) {
                    val db = remember { AppDatabase.getDatabase(context) }
                    val manager = remember { AiTrainerManager(db) }
                    AiTrainerScreen(
                        aiManager = manager,
                        isDark = isDark,
                        strings = strings,
                        userId = userId,
                        preferencesManager = preferencesManager,
                        onBack = { showAiTrainer = false; currentPage = null }
                    )
                } else if (showLeaderboard) {
                    LeaderboardScreen(
                        isDark = isDark,
                        isLbs = isLbs,
                        strings = strings,
                        onBackClick = { showLeaderboard = false; showFriends = true }
                    )
                } else if (showFriends) {
                    FriendsScreen(
                        isDark = isDark,
                        isLbs = isLbs,
                        strings = strings,
                        onBackClick = { showFriends = false; currentPage = null },
                        onOpenLeaderboard = { showFriends = false; showLeaderboard = true }
                    )
                } else if (selectedGroup != null) {
                    ExerciseListScreen(
                        grupaMusculara = selectedGroup!!,
                        isLbs = isLbs,
                        isDark = isDark,
                        onBackClick = { selectedGroup = null },
                        onWorkoutSaved = { reloadToken++; badgeCheckTrigger++ }
                    )
                } else if (currentPage == DrawerPage.CALENDAR) {
                    CalendarWorkoutScreen(onBackClick = { currentPage = null })
                } else {
                    Scaffold(
                        containerColor = surfaceBg,
                        topBar = {
                            TopAppBar(
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                title = {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "KINETIC",
                                            fontFamily = BebasNeue,
                                            fontSize = 26.sp,
                                            letterSpacing = 6.sp
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = strings.menu,
                                            tint = textPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = surfaceBg,
                                    titleContentColor = textPrimary
                                ),
                                actions = {
                                    IconButton(onClick = {
                                        val newMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                                        preferencesManager.setThemeMode(newMode)
                                        currentThemeMode = newMode
                                        onThemeChanged(newMode)
                                    }) {
                                        Icon(
                                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = if (isDark) strings.light else strings.dark,
                                            tint = accent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        if (currentDashboardTab == 0) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = paddingValues.calculateBottomPadding() + 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                item(span = { GridItemSpan(2) }) {
                                    val onboardingProfile = remember { preferencesManager.getOnboardingProfile() }
                                    val generatedWorkout = remember(onboardingProfile) {
                                        if (onboardingProfile.goal.isNotEmpty() && onboardingProfile.selectedGroups.isNotEmpty()) {
                                            FitnessAssistant.generateWorkout(onboardingProfile)
                                        } else emptyList()
                                    }
                                    val generatedTips = remember(onboardingProfile) {
                                        if (onboardingProfile.goal.isNotEmpty()) {
                                            FitnessAssistant.generateTips(onboardingProfile)
                                        } else emptyList()
                                    }

                                    if (generatedWorkout.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(18.dp),
                                            colors = CardDefaults.cardColors(containerColor = cardBg)
                                        ) {
                                            Column(modifier = Modifier.padding(18.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "TODAY'S WORKOUT",
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        color = accent,
                                                        letterSpacing = 2.sp
                                                    )
                                                    val goalLabel = when (onboardingProfile.goal) {
                                                        "strength" -> "STRENGTH"
                                                        "mass" -> "MASS"
                                                        "weight_loss" -> "WEIGHT LOSS"
                                                        "maintenance" -> "MAINTENANCE"
                                                        else -> ""
                                                    }
                                                    if (goalLabel.isNotEmpty()) {
                                                        Text(
                                                            goalLabel,
                                                            fontSize = 10.sp,
                                                            letterSpacing = 1.sp,
                                                            color = AccentPurple,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(AccentPurple.copy(alpha = 0.12f))
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.height(14.dp))

                                                val groupedByGroup = generatedWorkout.groupBy { it.group }
                                                groupedByGroup.forEach { (group, exercises) ->
                                                    Text(
                                                        group.uppercase(),
                                                        fontSize = 11.sp,
                                                        letterSpacing = 2.sp,
                                                        color = textSecondary,
                                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                                    )
                                                    exercises.forEach { ex ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 3.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                ex.name,
                                                                fontSize = 14.sp,
                                                                color = textPrimary,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Text(
                                                                "${ex.sets}x${ex.reps}",
                                                                fontSize = 13.sp,
                                                                color = accent,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        if (ex.note.isNotBlank()) {
                                                            Text(
                                                                ex.note,
                                                                fontSize = 11.sp,
                                                                color = textSecondary,
                                                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                if (generatedTips.isNotEmpty()) {
                                                    Spacer(Modifier.height(14.dp))
                                                    HorizontalDivider(color = dividerColor())
                                                    Spacer(Modifier.height(10.dp))
                                                    Text(
                                                        "TIPS",
                                                        fontSize = 11.sp,
                                                        letterSpacing = 2.sp,
                                                        color = AccentPurple,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                    )
                                                    generatedTips.forEach { tip ->
                                                        Row(
                                                            modifier = Modifier.padding(vertical = 3.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Text("•", color = accent, fontSize = 12.sp)
                                                            Text(
                                                                tip.text,
                                                                fontSize = 12.sp,
                                                                color = textSecondary,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item(span = { GridItemSpan(2) }) {
                                    PageTitle(strings.muscleGroups, modifier = Modifier.padding(top = 0.dp, bottom = 0.dp))
                                }

                                items(DataProvider.grupeMusculare.size) { idx ->
                                    val group = DataProvider.grupeMusculare[idx]
                                    val iconRes = when (group) {
                                        "Piept" -> R.drawable.ic_piept
                                        "Spate" -> R.drawable.ic_spate
                                        "Umeri" -> R.drawable.ic_umeri
                                        "Biceps" -> R.drawable.ic_biceps
                                        "Triceps" -> R.drawable.ic_triceps
                                        "Abdomen" -> R.drawable.ic_abdomen
                                        "Picioare" -> R.drawable.ic_picioare
                                        "Fese" -> R.drawable.ic_fese
                                        "Gambe" -> R.drawable.ic_gambe
                                        "Antebrate" -> R.drawable.ic_antebrat
                                        "Gat & Trapezi" -> R.drawable.ic_gat
                                        else -> R.drawable.ic_piept
                                    }

                                    val recLevel = recoveryMap[group] ?: 0.0

                                    Card(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { selectedGroup = group },
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(iconBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(iconRes),
                                                    contentDescription = group,
                                                    modifier = Modifier.size(42.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = LanguageManager.translateMuscleGroup(group, strings),
                                                fontSize = 14.sp,
                                                color = textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                            if (recLevel > 0.0) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(getRecoveryColor(recLevel))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (currentDashboardTab == 1) {
                            TemplateScreen(onBackClick = { currentDashboardTab = 0 })
                        } else if (currentDashboardTab == 2) {
                            StatsScreen(
                                isDark = isDark,
                                isLbs = isLbs,
                                strings = strings,
                                weeklyTopExercise = weeklyTopExercise,
                                weeklyTotalKg = weeklyTotalKg,
                                lastPR = lastPR,
                                paddingValues = innerPadding,
                                onExerciseHistoryClick = { exerciseName ->
                                    currentPage = DrawerPage.CALENDAR
                                }
                            )
                        } else if (currentDashboardTab == 3) {
                            WaterTrackingScreen(
                                preferencesManager = preferencesManager,
                                strings = strings,
                                accent = accent,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                cardBg = cardBg,
                                surfaceBg = surfaceBg,
                                paddingValues = innerPadding
                            )
                        } else if (currentDashboardTab == 4) {
                            ProfileScreen(
                                isDark = isDark,
                                preferencesManager = preferencesManager,
                                userProfileManager = userProfileManager,
                                strings = strings,
                                onLanguageClick = { showLanguageDialog = true },
                                onUnitsClick = { showUnitsDialog = true },
                                onLogout = {
                                    preferencesManager.setLoggedIn(false)
                                    preferencesManager.setOnboardingComplete(false)
                                    isLoggedIn = false
                                    reloadToken++
                                },
                                onBiometricClick = {
                                    showBiometricInput = true
                                },
                                onBiometricChartsClick = {
                                    showBiometricCharts = true
                                },
                                lastBiometric = lastBiometric,
                                weeksSinceMeasurement = weeksSinceMeasurement,
                                hasBiometricData = allBiometrics.isNotEmpty(),
                                paddingValues = innerPadding
                            )
                        }
                    }
                }
            }
        }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            isDark = isDark,
            currentLanguage = currentLanguage,
            strings = strings,
            onSelect = { code ->
                LanguageManager.saveLanguage(context, code)
                currentLanguage = code
                reloadToken++
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    if (showUnitsDialog) {
        UnitsSelectionDialog(
            isDark = isDark,
            isLbs = isLbs,
            strings = strings,
            onSelect = { lbs ->
                isLbs = lbs
                preferencesManager.setLbs(lbs)
                showUnitsDialog = false
            },
            onDismiss = { showUnitsDialog = false }
        )
    }
    if (showServerDialog) {
        ServerUrlDialog(
            isDark = isDark,
            currentUrl = preferencesManager.getServerUrl(),
            onSave = { url ->
                preferencesManager.setServerUrl(url)
                NetworkClient.getApi(url)
                showServerDialog = false
            },
            onDismiss = { showServerDialog = false }
        )
    }
}
}

// ============================================
// Ecranul 2: Lista de Exercitii
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(grupaMusculara: String, isLbs: Boolean = false, isDark: Boolean = true, onBackClick: () -> Unit, onWorkoutSaved: () -> Unit = {}) {
    val context = LocalContext.current
    val strings = LanguageManager.getStrings(context)
    val viewModel: MainViewModel = viewModel()
    var exercitii by remember { mutableStateOf<List<ExerciseListItem>>(emptyList()) }
    var selectedExercise: ExerciseDefinition? by remember { mutableStateOf(null) }
    var selectedProgressExercise: String? by remember { mutableStateOf(null) }
    var reloadToken by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEquipment by remember { mutableStateOf<String?>(null) }

    val surfaceBg = if (isDark) bgColor() else LightBackground
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val accent = if (isDark) accentColor() else LightPrimaryRed

    LaunchedEffect(grupaMusculara, reloadToken) {
        viewModel.getExercitiiPentruGrupa(grupaMusculara) { exercitii = it }
    }

    val equipmentTypes = listOf("Dumbbells", "Barbell", "Machine", "Cable", "Bodyweight", "EZ Bar", "Smith Machine", "Kettlebell", "Stability Ball", "Sled Machine", "Band")
    val filteredExercises = exercitii.filter { item ->
        (searchQuery.isBlank() || item.exercise.nume.contains(searchQuery, ignoreCase = true)) &&
        (selectedEquipment == null || item.equipment == selectedEquipment)
    }

    BackHandler {
        when {
            selectedProgressExercise != null -> selectedProgressExercise = null
            selectedExercise != null -> selectedExercise = null
            else -> onBackClick()
        }
    }

    if (selectedProgressExercise != null) {
        CalendarScreen(
            isLbs = isLbs,
            initialExercise = selectedProgressExercise,
            onBackClick = { selectedProgressExercise = null }
        )
    } else if (selectedExercise != null) {
        ExerciseInputScreen(
            exercise = selectedExercise!!,
            grupaMusculara = grupaMusculara,
            isLbs = isLbs,
            onBackClick = { selectedExercise = null },
            onOpenProgress = { name -> selectedProgressExercise = name; selectedExercise = null },
            onWorkoutSaved = onWorkoutSaved,
            strings = strings
        )
    } else {
    val listState = rememberLazyListState()
    val searchBarPx = with(LocalDensity.current) { 170.dp.roundToPx() }
    var scrollOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(listState) {
        snapshotFlow {
            val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (firstItem != null && firstItem.index == 0) {
                -firstItem.offset.toFloat() / firstItem.size.toFloat()
            } else if (firstItem != null) {
                1f
            } else {
                0f
            }
        }.collect { progress ->
            scrollOffset = progress.coerceIn(0f, 1f)
        }
    }

    val offsetAnim by animateFloatAsState(
        targetValue = scrollOffset,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "offset"
    )

    Scaffold(
        containerColor = bgColor(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(LanguageManager.translateMuscleGroup(grupaMusculara, strings)) },
                navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = accent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = surfaceBg,
                        titleContentColor = textPrimary
                    )
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 170.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredExercises) { item ->
                        val exercitiu = item.exercise
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedExercise = exercitiu },
                            colors = CardDefaults.cardColors(containerColor = cardColor()),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val gifUrl = ExerciseGifs.getGif(exercitiu.nume)
                                    if (gifUrl != null) {
                                        AsyncImage(
                                            model = gifUrl,
                                            contentDescription = exercitiu.nume,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(240.dp)
                                                .padding(4.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = accentColor().copy(alpha = 0.6f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = strings.demoExercise,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = secondaryTextColor(),
                                                letterSpacing = 2.sp
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = exercitiu.nume,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = textColor(),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.setFavorite(
                                                grupa = grupaMusculara,
                                                numeExercitiu = exercitiu.nume,
                                                isFavorite = !item.isFavorite
                                            ) {
                                                reloadToken++
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = strings.favorite,
                                            tint = if (item.isFavorite) RecoveryYellow else secondaryTextColor()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkRed.copy(alpha = 0.2f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            strings.add,
                                            color = accentColor(),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-170 * offsetAnim).dp)
                        .alpha(1f - offsetAnim)
                        .background(surfaceBg)
                ) {
                    RecoveryBarForGroup(grupaMusculara = grupaMusculara)

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.search, color = textSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = textSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = dividerColor(),
                            cursorColor = accent,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedContainerColor = cardBg.copy(alpha = 0.5f),
                            unfocusedContainerColor = cardBg.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedEquipment == null,
                                onClick = { selectedEquipment = null },
                                label = { Text(strings.allGroups) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                                    containerColor = cardBg,
                                    labelColor = textSecondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        items(equipmentTypes) { eq ->
                            FilterChip(
                                selected = selectedEquipment == eq,
                                onClick = { selectedEquipment = if (selectedEquipment == eq) null else eq },
                                label = { Text(eq) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                                    containerColor = cardBg,
                                    labelColor = textSecondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// Ecranul 3: Input Seturi
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseInputScreen(
    exercise: ExerciseDefinition,
    grupaMusculara: String,
    isLbs: Boolean = false,
    onBackClick: () -> Unit,
    onOpenProgress: (String) -> Unit = {},
    onWorkoutSaved: () -> Unit = {},
    strings: LanguageManager.Strings
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    var currentSets by remember { mutableStateOf(listOf<SetEntry>(SetEntry(0.0, 0))) }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var isPR by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var history by remember { mutableStateOf<List<ExercitiuEntity>>(emptyList()) }
    var stats by remember { mutableStateOf(ExerciseStats(0.0, 0, 0.0)) }
    var volumeSummary by remember { mutableStateOf(VolumeSummary(0.0, 0.0, 0.0)) }
    var restSeconds by remember { mutableStateOf(90) }
    var remainingSeconds by remember { mutableStateOf(0) }
    var customTimerText by remember { mutableStateOf("") }
    var editingSet by remember { mutableStateOf<ExercitiuEntity?>(null) }

    BackHandler {
        when {
            editingSet != null -> editingSet = null
            else -> onBackClick()
        }
    }

    fun refreshExerciseData() {
        viewModel.getIstoricExercitiu(exercise.nume) { history = it }
        viewModel.getStatisticiExercitiu(exercise.nume) { stats = it }
        viewModel.getVolumeSummary { volumeSummary = it }
    }

    LaunchedEffect(exercise.nume) {
        viewModel.incarcaUltimulAntrenament(exercise.nume) { ultimeleSeturi ->
            if (ultimeleSeturi.isNotEmpty()) {
                currentSets = ultimeleSeturi
            }
        }
        refreshExerciseData()
    }

    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1_000)
            remainingSeconds--
        }
    }

    editingSet?.let { set ->
        EditSetDialog(
            set = set,
            isLbs = isLbs,
            onDismiss = { editingSet = null },
            onSave = { updated ->
                viewModel.updateSet(updated) {
                    editingSet = null
                    refreshExerciseData()
                }
            }
        )
    }

    if (showSaveConfirmation) {
        var animScale by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) { animScale = 1f }
        val scale by animateFloatAsState(targetValue = animScale, animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f), label = "check")
        val iconColor = if (isPR) GoldPR else RecoveryGreen
        val icon = if (isPR) Icons.Default.EmojiEvents else Icons.Default.CheckCircle

        val validSets = currentSets.filter { it.greutateKg > 0 || it.repetari > 0 }
        val totalVolume = validSets.sumOf { it.greutateKg * it.repetari }
        val totalSets = validSets.size
        val maxWeight = validSets.maxOfOrNull { it.greutateKg } ?: 0.0
        val totalReps = validSets.sumOf { it.repetari }

        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            containerColor = surfaceColor(),
            titleContentColor = textColor(),
            textContentColor = secondaryTextColor(),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(56.dp).scale(scale)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isPR) "🎉 NEW PR!" else LanguageManager.getStrings(context).workoutCompleted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exercise.nume,
                        fontSize = 14.sp,
                        color = secondaryTextColor(),
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = LanguageManager.getStrings(context).volume, value = String.format("%.0f", totalVolume), unit = "kg", accent = accentColor())
                        StatItem(label = LanguageManager.getStrings(context).sets, value = "$totalSets", unit = "", accent = accentColor())
                        StatItem(label = LanguageManager.getStrings(context).reps, value = "$totalReps", unit = "", accent = accentColor())
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        StatItem(label = LanguageManager.getStrings(context).maxWeight, value = String.format("%.1f", maxWeight), unit = "kg", accent = accentColor())
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveConfirmation = false
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = accentColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        )
    }

    Scaffold(
        containerColor = bgColor(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(exercise.nume)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = accentColor())
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenProgress(exercise.nume) }) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = strings.progress,
                            tint = accentColor()
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.salveazaAntrenament(
                                grupaMusculara = grupaMusculara,
                                numeExercitiu = exercise.nume,
                                seturi = currentSets.filter { it.greutateKg > 0 || it.repetari > 0 },
                                note = noteText
                            ) { newPR ->
                                refreshExerciseData()
                                onWorkoutSaved()
                                isPR = newPR
                                showSaveConfirmation = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = strings.saveWorkout,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor(),
                    titleContentColor = textColor()
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item { RecoveryBarForGroup(grupaMusculara = grupaMusculara) }
            item {
                val gifUrl = ExerciseGifs.getGif(exercise.nume)
                if (gifUrl != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor()),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                exercise.nume,
                                color = textColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            AsyncImage(
                                model = gifUrl,
                                contentDescription = exercise.nume,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
            item {
                RestTimerCard(
                    restSeconds = restSeconds,
                    remainingSeconds = remainingSeconds,
                    customTimerText = customTimerText,
                    onRestSecondsChange = { restSeconds = it },
                    onCustomTimerTextChange = { customTimerText = it.filter { char -> char.isDigit() } },
                    onStart = {
                        remainingSeconds = customTimerText.toIntOrNull() ?: restSeconds
                    },
                    onStop = { remainingSeconds = 0 }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.setLabel, color = secondaryTextColor(), fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                    Text("KG", color = secondaryTextColor(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.reps, color = secondaryTextColor(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(48.dp))
                }
                HorizontalDivider(color = dividerColor(), modifier = Modifier.padding(vertical = 4.dp))
            }
            itemsIndexed(currentSets) { index, set ->
                SetInputRow(
                    index = index,
                    setEntry = set,
                    isLbs = isLbs,
                    onUpdate = { updatedSet ->
                        currentSets = currentSets.toMutableList().also { it[index] = updatedSet }
                    },
                    onDelete = {
                        currentSets = currentSets.toMutableList().also { it.removeAt(index) }
                    }
                )
            }
            item {
                OutlinedButton(
                    onClick = { currentSets = currentSets + SetEntry(0.0, 0) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(DarkRed, Red))
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = accentColor(),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.addSet, color = accentColor(), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            item {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(strings.exerciseNotes) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor(),
                        unfocusedBorderColor = dividerColor(),
                        focusedLabelColor = accentColor(),
                        unfocusedLabelColor = secondaryTextColor(),
                        cursorColor = accentColor(),
                        focusedTextColor = textColor(),
                        unfocusedTextColor = textColor()
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                ExerciseHistoryCard(
                    history = history,
                    isLbs = isLbs,
                    onEdit = { editingSet = it },
                    onDelete = { set ->
                        viewModel.deleteSet(set) {
                            refreshExerciseData()
                        }
                    }
                )
            }
            item { ExerciseStatsCard(stats = stats, volumeSummary = volumeSummary, isLbs = isLbs) }
        }
    }
}

// ============================================
// Componenta: Statistici + PR-uri
// ============================================
@Composable
fun ExerciseStatsCard(stats: ExerciseStats, volumeSummary: VolumeSummary, isLbs: Boolean = false) {
    val strings = LanguageManager.getStrings(LocalContext.current)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor()),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(strings.prAndVolume, color = textColor(), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Max", weightLabel(stats.maxGreutate, isLbs), Modifier.weight(1f))
                StatPill("Max reps", "${stats.maxRepetari}", Modifier.weight(1f))
                StatPill("Max set", weightLabel(stats.maxVolumSet, isLbs), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Today", weightLabel(volumeSummary.azi, isLbs), Modifier.weight(1f))
                StatPill("Week", weightLabel(volumeSummary.saptamana, isLbs), Modifier.weight(1f))
                StatPill("Month", weightLabel(volumeSummary.luna, isLbs), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceColor())
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = secondaryTextColor(), fontSize = 11.sp)
        Text(value, color = textColor(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ============================================
// Componenta: Timer Pauza
// ============================================
@Composable
fun RestTimerCard(
    restSeconds: Int,
    remainingSeconds: Int,
    customTimerText: String,
    onRestSecondsChange: (Int) -> Unit,
    onCustomTimerTextChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val strings = LanguageManager.getStrings(LocalContext.current)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor()),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.restTimer, color = textColor(), fontWeight = FontWeight.Bold)
                Text(formatSeconds(remainingSeconds), color = accentColor(), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120).forEach { seconds ->
                    FilterChip(
                        selected = restSeconds == seconds,
                        onClick = { onRestSecondsChange(seconds) },
                        label = { Text("${seconds}s") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkRed,
                            selectedLabelColor = textColor(),
                            labelColor = secondaryTextColor()
                        )
                    )
                }
                OutlinedTextField(
                    value = customTimerText,
                    onValueChange = onCustomTimerTextChange,
                    label = { Text("custom") },
                    modifier = Modifier.width(96.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor(),
                        unfocusedBorderColor = dividerColor(),
                        focusedLabelColor = accentColor(),
                        unfocusedLabelColor = secondaryTextColor(),
                        cursorColor = accentColor(),
                        focusedTextColor = textColor(),
                        unfocusedTextColor = textColor()
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start")
                }
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) {
                    Text("Stop", color = accentColor())
                }
            }
        }
    }
}

// ============================================
// Componenta: Istoric Exercitiu
// ============================================
@Composable
fun ExerciseHistoryCard(
    history: List<ExercitiuEntity>,
    isLbs: Boolean = false,
    onEdit: (ExercitiuEntity) -> Unit,
    onDelete: (ExercitiuEntity) -> Unit
) {
    val strings = LanguageManager.getStrings(LocalContext.current)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor()),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(strings.exerciseHistory, color = textColor(), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (history.isEmpty()) {
                Text(strings.noSavedSetsYet, color = secondaryTextColor(), fontSize = 13.sp)
            } else {
                history.take(8).forEach { set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${weightLabel(set.greutateKg, isLbs)} x ${set.repetari} reps",
                                color = textColor(),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Set ${set.setIndex + 1}  (${set.numeExercitiu})",
                                color = secondaryTextColor(),
                                fontSize = 12.sp
                            )
                            if (set.notes.isNotBlank()) {
                                Text(set.notes, color = secondaryTextColor(), fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = { onEdit(set) }) {
                            Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = accentColor())
                        }
                        IconButton(onClick = { onDelete(set) }) {
                            Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = DarkRed)
                        }
                    }
                    Divider(color = dividerColor().copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun EditSetDialog(
    set: ExercitiuEntity,
    isLbs: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (ExercitiuEntity) -> Unit
) {
    val strings = LanguageManager.getStrings(LocalContext.current)
    val displayWeight = convertWeight(set.greutateKg, isLbs)
    var kgText by remember(set.id) { mutableStateOf(String.format("%.1f", displayWeight)) }
    var repsText by remember(set.id) { mutableStateOf(set.repetari.toString()) }
    var noteText by remember(set.id) { mutableStateOf(set.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor(),
        titleContentColor = textColor(),
        textContentColor = secondaryTextColor(),
        title = { Text(strings.editSet) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = kgText,
                    onValueChange = { kgText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text(if (isLbs) "lbs" else "kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it.filter { char -> char.isDigit() } },
                    label = { Text("reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("notite") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val displayVal = kgText.toDoubleOrNull() ?: (if (isLbs) set.greutateKg * 2.20462 else set.greutateKg)
                val kgVal = if (isLbs) displayVal / 2.20462 else displayVal
                onSave(
                    set.copy(
                        greutateKg = kgVal,
                        repetari = repsText.toIntOrNull() ?: set.repetari,
                        notes = noteText
                    )
                )
            }) {
                Text(strings.saveNotes, color = accentColor())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = secondaryTextColor())
            }
        }
    )
}

fun formatSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return "%d:%02d".format(minutes, remaining)
}

fun formatDate(date: java.util.Date): String {
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(date)
}

@Composable
fun SetInputRow(
    index: Int,
    setEntry: SetEntry,
    isLbs: Boolean = false,
    onUpdate: (SetEntry) -> Unit,
    onDelete: () -> Unit
) {
    val strings = LanguageManager.getStrings(LocalContext.current)
    val displayWeight = convertWeight(setEntry.greutateKg, isLbs)
    var kgText by remember { mutableStateOf(if (displayWeight > 0) String.format("%.1f", displayWeight) else "") }
    var repsText by remember { mutableStateOf(if (setEntry.repetari > 0) setEntry.repetari.toString() else "") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(DarkRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = textColor(),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        OutlinedTextField(
            value = kgText,
            onValueChange = { newValue ->
                kgText = newValue.filter { it.isDigit() || it == '.' }
                val displayVal = kgText.toDoubleOrNull() ?: 0.0
                val kgVal = if (isLbs) displayVal / 2.20462 else displayVal
                onUpdate(SetEntry(kgVal, repsText.toIntOrNull() ?: 0))
            },
            label = { Text(if (isLbs) "lbs" else "kg", color = secondaryTextColor()) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor(),
                unfocusedBorderColor = dividerColor(),
                focusedLabelColor = accentColor(),
                unfocusedLabelColor = secondaryTextColor(),
                cursorColor = accentColor(),
                focusedTextColor = textColor(),
                unfocusedTextColor = textColor()
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = repsText,
            onValueChange = { newValue ->
                repsText = newValue.filter { it.isDigit() }
                val reps = repsText.toIntOrNull() ?: 0
                onUpdate(SetEntry(kgText.toDoubleOrNull() ?: 0.0, reps))
            },
            label = { Text("Reps", color = secondaryTextColor()) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor(),
                unfocusedBorderColor = dividerColor(),
                focusedLabelColor = accentColor(),
                unfocusedLabelColor = secondaryTextColor(),
                cursorColor = accentColor(),
                focusedTextColor = textColor(),
                unfocusedTextColor = textColor()
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = strings.delete,
                tint = DarkRed.copy(alpha = 0.7f)
            )
        }
    }
}

// ============================================
// Page Title with border
// ============================================
@Composable
fun PageTitle(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, accentColor(), RoundedCornerShape(24.dp))
                .padding(horizontal = 32.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.uppercase(),
                color = accentColor(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp
            )
        }
    }
}

// ============================================
// Template color config
// ============================================
private fun templateGradient(templateName: String): List<Color> {
    return when (templateName.lowercase()) {
        "push" -> listOf(DarkRed.copy(alpha = 0.85f), Color(0xFFFF1744).copy(alpha = 0.85f))
        "pull" -> listOf(Color(0xFF1565C0).copy(alpha = 0.85f), Color(0xFF42A5F5).copy(alpha = 0.85f))
        "legs" -> listOf(Color(0xFF2E7D32).copy(alpha = 0.85f), Color(0xFF66BB6A).copy(alpha = 0.85f))
        "upper" -> listOf(Color(0xFF6A1B9A).copy(alpha = 0.85f), Color(0xFFAB47BC).copy(alpha = 0.85f))
        "full body" -> listOf(Color(0xFFE65100).copy(alpha = 0.85f), Color(0xFFFF9800).copy(alpha = 0.85f))
        else -> listOf(DarkRed.copy(alpha = 0.85f), Red.copy(alpha = 0.85f))
    }
}

private fun templateIcon(templateName: String): Int {
    return when (templateName.lowercase()) {
        "push" -> R.drawable.template_push
        "pull" -> R.drawable.template_pull
        "legs" -> R.drawable.template_legs
        "upper" -> R.drawable.template_upper
        "full body" -> R.drawable.template_fullbody
        else -> R.drawable.template_push
    }
}

private fun templateMuscleGroups(template: WorkoutTemplate): List<String> {
    return template.exercitii.map { it.grupaMusculara }.distinct()
}

// ============================================
// Ecranul Template-uri de antrenament
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val strings = LanguageManager.getStrings(context)
    var selectedTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }

    BackHandler {
        if (selectedTemplate != null) selectedTemplate = null
        else onBackClick()
    }

    if (selectedTemplate != null) {
        TemplateDetailScreen(
            template = selectedTemplate!!,
            onBackClick = { selectedTemplate = null },
            onBackToMain = onBackClick
        )
    } else {
        Scaffold(
            containerColor = bgColor(),
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = accentColor())
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = bgColor(),
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(paddingValues)
            ) {
                item {
                    PageTitle(strings.templates)
                }
                items(DataProvider.templateuri) { template ->
                    val gradientColors = templateGradient(template.nume)
                    val estimatedDuration = template.exercitii.size * 3
                    val totalSets = template.exercitii.size * 4
                    val muscleGroups = templateMuscleGroups(template)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { selectedTemplate = template },
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = gradientColors,
                                            start = Offset(0f, 0f),
                                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.45f),
                                                Color.Transparent,
                                                Color.Transparent
                                            ),
                                            startX = 0f,
                                            endX = 600f
                                        )
                                    )
                            )

                            Image(
                                painter = painterResource(id = templateIcon(template.nume)),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(200.dp)
                                    .align(Alignment.CenterEnd)
                                    .alpha(0.55f),
                                contentScale = ContentScale.Crop
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(22.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        template.nume.uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 28.sp,
                                        letterSpacing = 4.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "${template.exercitii.size} ${strings.exercises}  ·  ~${estimatedDuration}min  ·  ${totalSets} sets",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(muscleGroups) { mg ->
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = Color.White.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    LanguageManager.translateMuscleGroup(mg, strings),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    template: WorkoutTemplate,
    onBackClick: () -> Unit,
    onBackToMain: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val strings = LanguageManager.getStrings(context)
    var selectedExercise by remember { mutableStateOf<ExerciseDefinition?>(null) }
    var selectedGrupa by remember { mutableStateOf("") }

    val exercises = remember {
        mutableStateListOf<TemplateExercise>().apply {
            addAll(template.exercitii)
        }
    }

    var workoutStarted by remember { mutableStateOf(false) }
    var currentExerciseIndex by remember { mutableIntStateOf(0) }

    BackHandler {
        when {
            selectedExercise != null -> {
                selectedExercise = null
                if (workoutStarted && currentExerciseIndex < exercises.size - 1) {
                    currentExerciseIndex++
                    selectedExercise = exercises[currentExerciseIndex].exercise
                    selectedGrupa = exercises[currentExerciseIndex].grupaMusculara
                }
            }
            workoutStarted -> { workoutStarted = false; currentExerciseIndex = 0 }
            else -> onBackClick()
        }
    }

    if (selectedExercise != null) {
        ExerciseInputScreen(
            exercise = selectedExercise!!,
            grupaMusculara = selectedGrupa,
            onBackClick = {
                selectedExercise = null
                if (workoutStarted && currentExerciseIndex < exercises.size - 1) {
                    currentExerciseIndex++
                    selectedExercise = exercises[currentExerciseIndex].exercise
                    selectedGrupa = exercises[currentExerciseIndex].grupaMusculara
                } else if (workoutStarted) {
                    workoutStarted = false
                    currentExerciseIndex = 0
                }
            },
            strings = strings
        )
    } else {
        val gradientColors = templateGradient(template.nume)

        Scaffold(
            containerColor = bgColor(),
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = accentColor())
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = bgColor(),
                        titleContentColor = textColor()
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(paddingValues)
            ) {
                item {
                    PageTitle(template.nume)
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = gradientColors,
                                            start = Offset(0f, 0f),
                                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.45f),
                                                Color.Transparent,
                                                Color.Transparent
                                            ),
                                            startX = 0f,
                                            endX = 600f
                                        )
                                    )
                            )
                            Image(
                                painter = painterResource(id = templateIcon(template.nume)),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(160.dp)
                                    .align(Alignment.CenterEnd)
                                    .alpha(0.55f),
                                contentScale = ContentScale.Crop
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(22.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${exercises.size} ${strings.exercises}  ·  ~${exercises.size * 3}min",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Button(
                                    onClick = {
                                        workoutStarted = true
                                        currentExerciseIndex = 0
                                        selectedExercise = exercises[0].exercise
                                        selectedGrupa = exercises[0].grupaMusculara
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = gradientColors[0],
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "START WORKOUT",
                                        color = gradientColors[0],
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                items(exercises.size) { index ->
                    val te = exercises[index]
                    val gifUrl = ExerciseGifs.getGif(te.exercise.nume)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedExercise = te.exercise
                                selectedGrupa = te.grupaMusculara
                            },
                        colors = CardDefaults.cardColors(containerColor = cardColor()),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = gifUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    te.exercise.nume,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = textColor(),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    LanguageManager.translateMuscleGroup(te.grupaMusculara, strings),
                                    color = secondaryTextColor(),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    te.exercise.equipment,
                                    color = accentColor(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Column(
                                modifier = Modifier.padding(end = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val item = exercises.removeAt(index)
                                            exercises.add(index - 1, item)
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = if (index > 0) accentColor() else Color.Transparent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (index < exercises.size - 1) {
                                            val item = exercises.removeAt(index)
                                            exercises.add(index + 1, item)
                                        }
                                    },
                                    enabled = index < exercises.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (index < exercises.size - 1) accentColor() else Color.Transparent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// Componente Muscle Recovery
// ============================================

fun getRecoveryColor(level: Double): Color {
    return when {
        level < 0.3 -> RecoveryGreen
        level < 0.6 -> RecoveryYellow
        level < 0.8 -> RecoveryOrange
        else -> RecoveryRed
    }
}

internal fun getRecoveryLabel(level: Double, strings: LanguageManager.Strings): String {
    return when {
        level < 0.05 -> strings.recovered
        level < 0.3 -> strings.almostRecovered
        level < 0.6 -> strings.moderate
        level < 0.8 -> strings.tired
        else -> strings.exhausted
    }
}

internal fun formatTimpRamas(ms: Long, strings: LanguageManager.Strings): String {
    if (ms <= 0) return strings.recovered
    val ore = (ms / 3_600_000).toInt()
    val minute = ((ms % 3_600_000) / 60_000).toInt()
    return if (ore > 0) "~${ore}h ${minute}m" else "~${minute}m"
}

@Composable
fun RecoveryBarCompact(
    level: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LanguageManager.getStrings(context)
    val animatedLevel by animateFloatAsState(
        targetValue = level.toFloat(),
        animationSpec = tween(durationMillis = 1000)
    )
    val barColor = getRecoveryColor(level)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                strings.muscleRecovery,
                color = secondaryTextColor(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${(level * 100).toInt()}%",
                color = barColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RecoveryTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedLevel.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun RecoveryBarForGroup(grupaMusculara: String) {
    val viewModel: MainViewModel = viewModel()
    var level by remember { mutableStateOf(0.0) }

    LaunchedEffect(grupaMusculara) {
        viewModel.getRecuperareMusculara(grupaMusculara) { level = it }
    }

    var refreshTick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            refreshTick = System.currentTimeMillis()
        }
    }
    LaunchedEffect(refreshTick) {
        viewModel.getRecuperareMusculara(grupaMusculara) { level = it }
    }

    RecoveryBarCompact(
        level = level,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleRecoveryScreen(onBackClick: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val strings = LanguageManager.getStrings(context)
    var recoveryData by remember { mutableStateOf<List<Pair<String, Double>>>(listOf()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.getToateRecuperarile { data ->
            recoveryData = data
            isLoading = false
        }
    }

    var refreshTick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            refreshTick = System.currentTimeMillis()
        }
    }
    LaunchedEffect(refreshTick) {
        viewModel.getToateRecuperarile { data ->
            recoveryData = data
        }
    }

    Scaffold(
        containerColor = bgColor(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(strings.recovery) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = accentColor()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor(),
                    titleContentColor = textColor()
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor())
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recoveryData) { (grupa, level) ->
                        val remainingMs = MainViewModel.calculeazaTimpRamas(level, grupa)
                        val barColor = getRecoveryColor(level)
                        val animatedLevel by animateFloatAsState(
                            targetValue = level.toFloat(),
                            animationSpec = tween(durationMillis = 1000)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardColor()),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        LanguageManager.translateMuscleGroup(grupa, strings),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = textColor(),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        getRecoveryLabel(level, strings),
                                        color = barColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(RecoveryTrack)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = animatedLevel.coerceIn(0f, 1f))
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(barColor)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${(level * 100).toInt()}% ${strings.fatigue}",
                                        color = secondaryTextColor(),
                                        fontSize = 12.sp
                                    )
                                    if (level > 0.05) {
                                        Text(
                                            formatTimpRamas(remainingMs, strings),
                                            color = secondaryTextColor(),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val recHours = when (grupa) {
                                    "Biceps" -> 36L
                                    "Gambe" -> 36L
                                    "Antebrate" -> 36L
                                    "Triceps" -> 48L
                                    "Abdomen" -> 48L
                                    "Umeri" -> 48L
                                    "Piept" -> 48L
                                    "Gat & Trapezi" -> 48L
                                    "Spate" -> 72L
                                    "Picioare" -> 72L
                                    "Fese" -> 72L
                                    else -> 48L
                                }
                                Text(
                                    "${strings.recommendedRecovery}: ~${recHours}h",
                                    color = accentColor().copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// Componenta: Line Chart (Canvas)
// ============================================
@Composable
fun LineChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = accentColor(),
    dotColor: Color = accentColor()
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOf { it.second }
    val minValue = data.minOf { it.second }.coerceAtMost(maxValue * 0.8)
    val range = (maxValue - minValue).coerceAtLeast(1.0)
    val topPadding = 32.dp
    val bottomPadding = 40.dp
    val startPadding = 48.dp
    val endPadding = 16.dp

    val gridLines = 4
    val resolvedDivider = dividerColor()
    val resolvedText = textColor()
    val resolvedCard = cardColor()

    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 24f
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val chartLeft = startPadding.toPx()
        val chartRight = w - endPadding.toPx()
        val chartTop = topPadding.toPx()
        val chartBottom = h - bottomPadding.toPx()
        val chartW = chartRight - chartLeft
        val chartH = chartBottom - chartTop

        for (i in 0..gridLines) {
            val y = chartTop + chartH * i / gridLines
            drawLine(
                color = resolvedDivider,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
            val value = maxValue - (range * i / gridLines)
            drawContext.canvas.nativeCanvas.apply {
                textPaint.color = 0xFFB0B0B0.toInt()
                textPaint.textSize = 24f
                textPaint.textAlign = android.graphics.Paint.Align.RIGHT
                drawText("%.0f".format(value), chartLeft - 8f, y + 6f, textPaint)
            }
        }

        if (data.size == 1) {
            val x = chartLeft + chartW / 2
            val y = chartBottom - ((data[0].second - minValue) / range * chartH).toFloat()
            drawCircle(color = dotColor, radius = 8f, center = Offset(x, y))
            drawContext.canvas.nativeCanvas.apply {
                textPaint.color = 0xFFFFFFFF.toInt()
                textPaint.textSize = 22f
                textPaint.textAlign = android.graphics.Paint.Align.CENTER
                drawText("%.1f".format(data[0].second), x, y - 16f, textPaint)
                textPaint.color = 0xFFB0B0B0.toInt()
                drawText(data[0].first, x, chartBottom + 30f, textPaint)
            }
            return@Canvas
        }

        val points = data.mapIndexed { index, (label, value) ->
            val x = chartLeft + chartW * index / (data.size - 1)
            val y = chartBottom - ((value - minValue) / range * chartH).toFloat()
            Offset(x, y) to label
        }

        val path = Path()
        path.moveTo(points.first().first.x, chartBottom)
        points.forEach { (pt, _) -> path.lineTo(pt.x, pt.y) }
        path.lineTo(points.last().first.x, chartBottom)
        path.close()

        drawPath(
            path,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f)),
                startY = chartTop,
                endY = chartBottom
            )
        )

        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i].first,
                end = points[i + 1].first,
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        points.forEachIndexed { index, (pt, label) ->
            drawCircle(color = resolvedCard, radius = 7f, center = pt)
            drawCircle(color = dotColor, radius = 5f, center = pt)

            drawContext.canvas.nativeCanvas.apply {
                textPaint.color = 0xFFFFFFFF.toInt()
                textPaint.textSize = 20f
                textPaint.textAlign = android.graphics.Paint.Align.CENTER
                drawText("%.1f".format(data[index].second), pt.x, pt.y - 14f, textPaint)
                textPaint.color = 0xFFB0B0B0.toInt()
                drawText(label, pt.x, chartBottom + 28f, textPaint)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, unit: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        if (unit.isNotBlank()) {
            Text(text = unit, fontSize = 11.sp, color = secondaryTextColor())
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = secondaryTextColor()
        )
    }
}

// ============================================
// Ecranul Progres (redesignat cu Line Chart)
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(isLbs: Boolean = false, initialExercise: String? = null, onBackClick: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val strings = LanguageManager.getStrings(context)
    var selectedExercise by remember { mutableStateOf(initialExercise ?: "") }
    var progresData by remember { mutableStateOf<List<ProgresLunar>>(listOf()) }
    var showExerciseSelector by remember { mutableStateOf(initialExercise == null) }
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf(ExerciseStats(0.0, 0, 0.0)) }

    BackHandler {
        when {
            showExerciseSelector -> showExerciseSelector = false
            else -> onBackClick()
        }
    }

    LaunchedEffect(initialExercise) {
        if (initialExercise != null) {
            viewModel.getProgresLunar(initialExercise) { progres -> progresData = progres }
            viewModel.getStatisticiExercitiu(initialExercise) { stats = it }
        }
    }

    Scaffold(
        containerColor = bgColor(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = accentColor())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor(),
                    titleContentColor = textColor()
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            PageTitle(strings.progress)
            if (showExerciseSelector) {
                if (selectedGroupFilter == null) {
                    Text(
                        strings.chooseMuscleGroup,
                        style = MaterialTheme.typography.titleLarge,
                        color = secondaryTextColor(),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(DataProvider.grupeMusculare) { group ->
                            val iconRes = when (group) {
                                "Piept" -> R.drawable.ic_piept
                                "Spate" -> R.drawable.ic_spate
                                "Umeri" -> R.drawable.ic_umeri
                                "Biceps" -> R.drawable.ic_biceps
                                "Triceps" -> R.drawable.ic_triceps
                                "Abdomen" -> R.drawable.ic_abdomen
                                "Picioare" -> R.drawable.ic_picioare
                                "Fese" -> R.drawable.ic_fese
                                "Gambe" -> R.drawable.ic_gambe
                                "Antebrate" -> R.drawable.ic_antebrat
                                "Gat & Trapezi" -> R.drawable.ic_gat
                                else -> R.drawable.ic_piept
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedGroupFilter = group },
                                colors = CardDefaults.cardColors(containerColor = cardColor()),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(IconBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(iconRes),
                                            contentDescription = group,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = LanguageManager.translateMuscleGroup(group, strings),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = textColor(),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = accentColor(),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    TextButton(onClick = { selectedGroupFilter = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = accentColor())
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("< ${strings.back} ${strings.muscleGroups.lowercase()}", color = accentColor())
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        LanguageManager.translateMuscleGroup(selectedGroupFilter!!, strings).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = textColor(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    val exercitiiDinGrupa = DataProvider.exercitiiPeGrupa[selectedGroupFilter] ?: listOf()
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(exercitiiDinGrupa) { exercitiu ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedExercise = exercitiu.nume
                                        showExerciseSelector = false
                                        selectedGroupFilter = null
                                        viewModel.getProgresLunar(exercitiu.nume) { progres ->
                                            progresData = progres
                                        }
                                        viewModel.getStatisticiExercitiu(exercitiu.nume) { stats = it }
                                    },
                                colors = CardDefaults.cardColors(containerColor = cardColor()),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(DarkRed.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = accentColor(),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercitiu.nume,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = textColor(),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = LanguageManager.translateMuscleGroup(exercitiu.group, strings),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = secondaryTextColor()
                                        )
                                    }
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = accentColor().copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                TextButton(onClick = {
                    showExerciseSelector = true
                    selectedGroupFilter = null
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = accentColor())
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.changeExercise, color = accentColor())
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor()),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            selectedExercise.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = textColor(),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(strings.monthlyProgress, color = secondaryTextColor(), fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Max", weightLabel(stats.maxGreutate, isLbs), Modifier.weight(1f))
                    StatPill("Max reps", "${stats.maxRepetari}", Modifier.weight(1f))
                    StatPill("Max set", weightLabel(stats.maxVolumSet, isLbs), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (progresData.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor()),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = secondaryTextColor(),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(strings.noDataYet, color = secondaryTextColor(), fontSize = 16.sp)
                            Text(
                                strings.completeWorkoutsToSee,
                                color = secondaryTextColor().copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val chartData = progresData.map { p ->
                        val parts = p.luna.split("-")
                        val monthName = when (parts.getOrElse(1) { "" }) {
                            "01" -> strings.jan; "02" -> strings.feb; "03" -> strings.mar
                            "04" -> strings.apr; "05" -> strings.may; "06" -> strings.jun
                            "07" -> strings.jul; "08" -> strings.aug; "09" -> strings.sep
                            "10" -> strings.oct; "11" -> strings.nov; "12" -> strings.dec
                            else -> p.luna
                        }
                        monthName to p.greutateMaxima
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor()),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(strings.weightProgression, color = textColor(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            LineChart(
                                data = chartData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(strings.monthlyDetails, color = textColor(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(strings.month, color = textColor(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Max ${if (isLbs) "lbs" else "kg"}", color = textColor(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            progresData.reversed().forEachIndexed { index, progres ->
                                val bg = if (index % 2 == 0) cardColor() else surfaceColor()
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = bg),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(progres.luna, color = textColor(), fontSize = 13.sp)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(DarkRed.copy(alpha = 0.2f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                "${weightLabel(progres.greutateMaxima, isLbs)}",
                                                 color = accentColor(),
                                                 fontWeight = FontWeight.Bold,
                                                 fontSize = 13.sp
                                             )
                }
            }
        }
    }
}
                }
            }
        }
    }
}
}

// ============================================
// Profile Screen
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isDark: Boolean,
    preferencesManager: PreferencesManager,
    userProfileManager: UserProfileManager,
    strings: LanguageManager.Strings,
    onLanguageClick: () -> Unit,
    onUnitsClick: () -> Unit,
    onLogout: () -> Unit,
    onBiometricClick: () -> Unit,
    onBiometricChartsClick: () -> Unit = {},
    lastBiometric: BiometricEntity? = null,
    weeksSinceMeasurement: Int = -1,
    hasBiometricData: Boolean = false,
    paddingValues: PaddingValues = PaddingValues()
) {
    val surfaceBg = if (isDark) bgColor() else LightBackground
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val accent = if (isDark) accentColor() else LightPrimaryRed
    val iconBg = if (isDark) IconBackground else LightIconBackground

    val profile = userProfileManager.getOwnProfile()
    val profileName = profile?.name ?: strings.guest
    val initials =
        profileName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()

    val context = androidx.compose.ui.platform.LocalContext.current

    var editingWeight by remember { mutableStateOf(false) }
    var editingHeight by remember { mutableStateOf(false) }
    var weightText by remember {
        mutableStateOf(
            preferencesManager.getUserWeight().let {
                if (it == it.toLong().toFloat()) it.toLong().toString() else String.format(
                    "%.1f",
                    it
                )
            })
    }
    var heightText by remember {
        mutableStateOf(
            preferencesManager.getUserHeight().let {
                if (it == it.toLong().toFloat()) it.toLong().toString() else String.format(
                    "%.1f",
                    it
                )
            })
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = paddingValues.calculateTopPadding() + 4.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageTitle(strings.profile)
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    profileName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        strings.personalInfo,
                        style = MaterialTheme.typography.titleMedium,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingWeight = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.weight,
                            color = textSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (editingWeight) {
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                modifier = Modifier.width(120.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accent,
                                    unfocusedBorderColor = textSecondary,
                                    cursorColor = accent,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                val w =
                                    weightText.toFloatOrNull() ?: preferencesManager.getUserWeight()
                                preferencesManager.setUserWeight(w)
                                editingWeight = false
                            }) {
                                Text(strings.confirm, color = accent)
                            }
                        } else {
                            Text(
                                "${
                                    preferencesManager.getUserWeight().let {
                                        if (it == it.toLong().toFloat()) it.toLong()
                                            .toString() else String.format("%.1f", it)
                                    }
                                } ${strings.kg}",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = textSecondary.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingHeight = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.height,
                            color = textSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (editingHeight) {
                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { heightText = it },
                                modifier = Modifier.width(120.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accent,
                                    unfocusedBorderColor = textSecondary,
                                    cursorColor = accent,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                val h =
                                    heightText.toFloatOrNull() ?: preferencesManager.getUserHeight()
                                preferencesManager.setUserHeight(h)
                                editingHeight = false
                            }) {
                                Text(strings.confirm, color = accent)
                            }
                        } else {
                            Text(
                                "${
                                    preferencesManager.getUserHeight().let {
                                        if (it == it.toLong().toFloat()) it.toLong()
                                            .toString() else String.format("%.1f", it)
                                    }
                                } cm",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = textSecondary.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.waterGoal,
                            color = textSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "${
                                preferencesManager.getUserWeight().toInt()
                            } × 33${strings.ml} = ${preferencesManager.getWaterGoalMl()}${strings.ml}",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBiometricClick() },
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.biometricTracking,
                            style = MaterialTheme.typography.titleMedium,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (lastBiometric != null) {
                        Text(
                            strings.lastMeasurement,
                            color = textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val units = if (preferencesManager.isLbs()) "lbs" else "kg"
                        if (lastBiometric.weightKg > 0) {
                            val displayWeight = if (preferencesManager.isLbs()) lastBiometric.weightKg * 2.20462 else lastBiometric.weightKg
                            Text(
                                "${strings.weight}: ${String.format("%.1f", displayWeight)} $units",
                                color = textPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (lastBiometric.bodyFatPercent > 0) {
                            Text(
                                "${strings.bodyFat}: ${String.format("%.1f", lastBiometric.bodyFatPercent)}${strings.percent}",
                                color = textPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val timeLabel = when {
                            weeksSinceMeasurement == 0 -> strings.thisWeek
                            weeksSinceMeasurement > 0 -> "$weeksSinceMeasurement ${strings.weeksAgo}"
                            else -> ""
                        }
                        if (timeLabel.isNotEmpty()) {
                            Text(
                                timeLabel,
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { onBiometricChartsClick() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                strings.viewCharts,
                                color = accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            strings.noMeasurements,
                            color = textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            strings.addMeasurement,
                            color = accent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                strings.biometricReminder,
                                color = textSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        var biometricReminderEnabled by remember { mutableStateOf(preferencesManager.isBiometricReminderEnabled()) }
                        Switch(
                            checked = biometricReminderEnabled,
                            onCheckedChange = { enabled ->
                                biometricReminderEnabled = enabled
                                preferencesManager.setBiometricReminderEnabled(enabled)
                                val receiver = BiometricReminderReceiver()
                                if (enabled) {
                                    receiver.scheduleWeekly(context)
                                } else {
                                    receiver.cancelAlarm(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accent,
                                uncheckedThumbColor = textSecondary,
                                uncheckedTrackColor = textSecondary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        strings.settingsAndMore,
                        style = MaterialTheme.typography.titleMedium,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onLanguageClick)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                strings.language,
                                color = textSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = textSecondary
                        )
                    }

                    HorizontalDivider(color = textSecondary.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onUnitsClick)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Straighten,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                strings.units,
                                color = textSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = textSecondary
                        )
                    }

                    HorizontalDivider(color = textSecondary.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onLogout)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                strings.logout,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}



