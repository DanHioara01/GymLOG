package com.example.gymlog2

object ExerciseGifs {
    private const val BASE = "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@v1.1.0"
    private const val CW = "https://raw.githubusercontent.com/omercotkd/exercises-gifs/main/assets"

    private val gifs = mapOf(
        // Piept (Chest)
        "Bench Press" to "$BASE/pectorals/barbell-bench-press.gif",
        "Incline Bench Press" to "$BASE/pectorals/barbell-incline-bench-press.gif",
        "Decline Bench Press" to "$BASE/pectorals/barbell-decline-bench-press.gif",
        "Dumbbell Fly" to "$BASE/pectorals/dumbbell-fly.gif",
        "Cable Crossover" to "$BASE/pectorals/cable-cross-over-variation.gif",
        "Push-Up" to "$BASE/pectorals/push-up.gif",
        "Dumbbell Bench Press" to "$CW/0289.gif",
        "Dumbbell Incline Bench Press" to "$CW/0314.gif",
        "Dumbbell Decline Bench Press" to "$CW/0301.gif",
        "Dumbbell Pullover" to "$CW/0375.gif",
        "Dumbbell Incline Fly" to "$CW/0319.gif",
        "Cable Incline Fly" to "$CW/0171.gif",
        "Cable Decline Fly" to "$CW/0158.gif",
        "Cable Bench Press" to "$CW/0151.gif",
        "Chest Dip" to "$CW/0251.gif",
        "Incline Push-Up" to "$CW/0493.gif",
        "Decline Push-Up" to "$CW/0279.gif",
        "Barbell Guillotine Bench Press" to "$CW/0045.gif",
        "Clap Push Up" to "$CW/1273.gif",
        "Dumbbell Decline Fly" to "$CW/0302.gif",
        "Cable Middle Fly" to "$CW/0188.gif",
        "Dumbbell Incline Breeding" to "$CW/0316.gif",
        "Wide Push Up" to "$CW/1311.gif",
        "Plyo Push Up" to "$CW/1306.gif",
        "Dumbbell Straight Arm Pullover" to "$CW/0433.gif",

        // Spate (Back)
        "Deadlift" to "$BASE/glutes/barbell-deadlift.gif",
        "Barbell Row" to "$BASE/upper-back/barbell-bent-over-row.gif",
        "Pull-Up" to "$BASE/lats/pull-up.gif",
        "Lat Pulldown" to "$BASE/lats/cable-lat-pulldown-full-range-of-motion.gif",
        "Seated Cable Row" to "$BASE/upper-back/cable-seated-row.gif",
        "T-Bar Row" to "$BASE/upper-back/lever-t-bar-row.gif",
        "Chin-Up" to "$CW/1326.gif",
        "Wide Grip Pull-Up" to "$CW/1429.gif",
        "Cable Straight Arm Pulldown" to "$CW/0238.gif",
        "Dumbbell One Arm Row" to "$CW/0292.gif",
        "Inverted Row" to "$CW/0499.gif",
        "Cable Underhand Pulldown" to "$CW/0245.gif",
        "Cable Lateral Pulldown" to "$CW/0177.gif",
        "Cable High Row" to "$CW/0167.gif",
        "Barbell Pendlay Row" to "$CW/3017.gif",
        "Lever Bent Over Row" to "$CW/0574.gif",
        "Lever Seated Row" to "$CW/1350.gif",
        "Lever T-Bar Row" to "$CW/0606.gif",
        "Dumbbell Row" to "$CW/0293.gif",
        "Cable Seated Wide-Grip Row" to "$CW/0218.gif",

        // Umeri (Shoulders)
        "Overhead Press" to "$BASE/delts/barbell-seated-overhead-press.gif",
        "Lateral Raise" to "$BASE/delts/dumbbell-lateral-raise.gif",
        "Front Raise" to "$BASE/delts/dumbbell-front-raise.gif",
        "Face Pull" to "$BASE/delts/cable-rear-delt-row-with-rope.gif",
        "Arnold Press" to "$BASE/delts/dumbbell-arnold-press.gif",
        "Rear Delt Fly" to "$BASE/delts/cable-cross-over-revers-fly.gif",
        "Dumbbell Shoulder Press" to "$CW/0405.gif",
        "Seated Dumbbell Press" to "$CW/0388.gif",
        "Cable Lateral Raise" to "$CW/0178.gif",
        "Barbell Upright Row" to "$CW/0120.gif",
        "Cable Front Raise" to "$CW/0162.gif",
        "Dumbbell Push Press" to "$CW/1700.gif",
        "Cable Shoulder Press" to "$CW/0219.gif",
        "Dumbbell Cuban Press" to "$CW/0299.gif",
        "Barbell Seated Overhead Press" to "$CW/0091.gif",
        "Dumbbell Lateral Raise" to "$CW/0334.gif",
        "Cable Seated Rear Lateral Raise" to "$CW/0215.gif",
        "Lever Shoulder Press" to "$CW/0603.gif",

        // Biceps
        "Barbell Curl" to "$BASE/biceps/barbell-curl.gif",
        "Hammer Curl" to "$BASE/biceps/dumbbell-hammer-curl.gif",
        "Preacher Curl" to "$BASE/biceps/barbell-preacher-curl.gif",
        "Concentration Curl" to "$BASE/biceps/cable-concentration-curl.gif",
        "Cable Bicep Curl" to "$CW/0868.gif",
        "Dumbbell Incline Curl" to "$CW/0318.gif",
        "EZ Bar Curl" to "$CW/0447.gif",
        "Barbell Reverse Curl" to "$CW/0080.gif",
        "Cable Hammer Curl" to "$CW/0165.gif",
        "EZ Bar Preacher Curl" to "$CW/1627.gif",
        "Dumbbell Preacher Curl" to "$CW/0372.gif",
        "Dumbbell Alternate Biceps Curl" to "$CW/0285.gif",
        "EZ Bar Reverse Curl" to "$CW/0451.gif",
        "Dumbbell Bicep Curl" to "$CW/0294.gif",

        // Triceps
        "Tricep Pushdown" to "$BASE/triceps/cable-pushdown.gif",
        "Skull Crusher" to "$BASE/triceps/barbell-lying-triceps-extension-skull-crusher.gif",
        "Dips" to "$BASE/pectorals/chest-dip.gif",
        "Close-Grip Bench Press" to "$CW/0030.gif",
        "Dumbbell Kickback" to "$CW/0333.gif",
        "Cable Overhead Tricep Extension" to "$CW/0194.gif",
        "Overhead Tricep Extension" to "$CW/0430.gif",
        "Diamond Push-Up" to "$CW/0283.gif",
        "Close-Grip Push-Up" to "$CW/0259.gif",
        "Bench Dip" to "$CW/0129.gif",
        "EZ Bar Standing French Press" to "$CW/1749.gif",
        "EZ Bar JM Bench Press" to "$CW/0450.gif",
        "Cable Tricep Pushdown V-Bar" to "$CW/0241.gif",

        // Abdomen (Core)
        "Plank" to "$BASE/abs/weighted-front-plank.gif",
        "Crunch" to "$BASE/abs/crunch-floor.gif",
        "Russian Twist" to "$BASE/abs/russian-twist.gif",
        "Hanging Leg Raise" to "$BASE/abs/hanging-leg-raise.gif",
        "Ab Rollout" to "$BASE/abs/barbell-rollerout.gif",
        "Cable Woodchop" to "$BASE/abs/cable-twist.gif",
        "Dead Bug" to "$CW/0276.gif",
        "Reverse Crunch" to "$CW/0872.gif",
        "Side Plank" to "$CW/0705.gif",
        "Lying Leg Raise" to "$CW/0620.gif",
        "Cable Crunch" to "$CW/0175.gif",
        "Cross Body Crunch" to "$CW/0262.gif",
        "Sit-Up" to "$CW/0735.gif",
        "Seated Leg Raise" to "$CW/0689.gif",
        "Decline Sit-Up" to "$CW/0282.gif",
        "Hanging Knee Raise" to "$CW/0472.gif",
        "Cable Seated Twist" to "$CW/2399.gif",
        "Jackknife Sit-Up" to "$CW/0507.gif",
        "Toe Touch" to "$CW/3212.gif",
        "Front Plank With Twist" to "$CW/0464.gif",

        // Picioare (Legs)
        "Squat" to "$BASE/glutes/barbell-full-squat.gif",
        "Leg Press" to "$BASE/quads/lever-alternate-leg-press.gif",
        "Romanian Deadlift" to "$BASE/glutes/barbell-romanian-deadlift.gif",
        "Bulgarian Split Squat" to "$BASE/quads/barbell-single-leg-split-squat.gif",
        "Hip Thrust" to "$CW/1409.gif",
        "Barbell Front Squat" to "$CW/0042.gif",
        "Barbell Lunge" to "$CW/0054.gif",
        "Barbell Good Morning" to "$CW/0044.gif",
        "Dumbbell Lunge" to "$CW/0336.gif",
        "Dumbbell Goblet Squat" to "$CW/1760.gif",
        "Dumbbell Step-Up" to "$CW/0431.gif",
        "Dumbbell Romanian Deadlift" to "$CW/1459.gif",
        "Jump Squat" to "$CW/0514.gif",
        "Walking Lunge" to "$CW/1460.gif",
        "Kettlebell Swing" to "$CW/0549.gif",
        "Kettlebell Goblet Squat" to "$CW/0534.gif",
        "Single Leg Deadlift" to "$CW/1757.gif",
        "Barbell Sumo Deadlift" to "$CW/0117.gif",
        "Barbell Reverse Lunge" to "$CW/0078.gif",
        "Barbell Step-Up" to "$CW/0114.gif",
        "Sled Hack Squat" to "$CW/0743.gif",
        "Dumbbell Lunge With Bicep Curl" to "$CW/1658.gif",
        "Kettlebell Turkish Get Up" to "$CW/0551.gif",
        "Kettlebell Front Squat" to "$CW/0533.gif",
        "Dumbbell Plyo Squat" to "$CW/0371.gif",
        "Split Squat" to "$CW/2368.gif",

        // Fese (Glutes)
        "Glute Bridge" to "$BASE/glutes/barbell-glute-bridge.gif",
        "Cable Kickback" to "$BASE/glutes/cable-standing-hip-extension.gif",
        "Sumo Deadlift" to "$BASE/glutes/barbell-sumo-deadlift.gif",
        "Bodyweight Glute Bridge" to "$CW/3523.gif",
        "Barbell Hip Thrust" to "$CW/1409.gif",
        "Kettlebell Deadlift" to "$CW/0157.gif",
        "Glute Bridge March" to "$CW/3561.gif",

        // Gambe (Calves)
        "Leg Curl" to "$BASE/hamstrings/lever-lying-leg-curl.gif",
        "Leg Extension" to "$BASE/quads/lever-leg-extension.gif",
        "Calf Raise" to "$BASE/calves/barbell-standing-calf-raise.gif",
        "Hack Squat" to "$BASE/glutes/barbell-hack-squat.gif",
        "Standing Calf Raise" to "$CW/1372.gif",
        "Seated Calf Raise" to "$CW/0088.gif",
        "Donkey Calf Raise" to "$CW/0284.gif",
        "Bodyweight Standing Calf Raise" to "$CW/1373.gif",

        // Antebrate (Forearms)
        "Barbell Wrist Curl" to "$CW/0126.gif",
        "Barbell Reverse Wrist Curl" to "$CW/0082.gif",
        "Farmer's Walk" to "$CW/2133.gif",
        "Finger Curls" to "$CW/0455.gif",
        "Dumbbell Wrist Curl" to "$CW/0369.gif",
        "Cable Wrist Curl" to "$CW/0247.gif",
        "Dumbbell Reverse Wrist Curl" to "$CW/0385.gif",

        // Gat (Neck)
        "Neck Side Stretch" to "$CW/1403.gif",
        "Side Push Neck Stretch" to "$CW/0716.gif",

        // Trapezi (Traps)
        "Barbell Shrugs" to "$CW/0095.gif",
        "Dumbbell Shrugs" to "$CW/0406.gif",
        "Cable Shrugs" to "$CW/0220.gif",
        "Barbell Shrug" to "$CW/0095.gif",

        // Cardio
        "Burpee" to "$CW/1160.gif",
        "Mountain Climber" to "$CW/0630.gif",
        "Jumping Jack" to "$CW/3224.gif",
        "Bear Crawl" to "$CW/3360.gif",

        // Kettlebell
        "Kettlebell Clean" to "$CW/0535.gif",
        "Kettlebell Snatch" to "$CW/0529.gif",
        "Kettlebell Row" to "$CW/0541.gif",
        "Kettlebell Windmill" to "$CW/0554.gif",
        "Kettlebell Lunge Pass Through" to "$CW/0536.gif",
        "Kettlebell Arnold Press" to "$CW/0523.gif",

        // EZ Bar
        "EZ Bar Reverse Grip Curl" to "$CW/0451.gif",
        "EZ Bar Seated Triceps Extension" to "$CW/0453.gif",
        "EZ Bar Lying Bent Arms Pullover" to "$CW/3010.gif",

        // Smith Machine
        "Smith Machine Bench Press" to "$CW/0748.gif",
        "Smith Machine Squat" to "$CW/0770.gif",
        "Smith Machine Shoulder Press" to "$CW/0766.gif",
        "Smith Machine Incline Bench Press" to "$CW/0757.gif",
        "Smith Machine Shrugs" to "$CW/0767.gif",

        // Lever/Machine
        "Lever Chest Press" to "$CW/0577.gif",
        "Lever Shoulder Press" to "$CW/0603.gif",
        "Lever Lateral Raise" to "$CW/0584.gif",
        "Lever Seated Leg Curl" to "$CW/0599.gif",
        "Lever Leg Extension" to "$CW/0585.gif",
        "Lever Seated Calf Raise" to "$CW/0594.gif",
        "Lever Seated Fly" to "$CW/0596.gif",

        // Dumbbell misc
        "Dumbbell Hammer Curl" to "$CW/0313.gif",
        "Dumbbell Shrugs" to "$CW/0406.gif",
        "Dumbbell Side Bend" to "$CW/0407.gif",
        "Dumbbell Front Raise" to "$CW/0310.gif",
        "Dumbbell Rear Fly" to "$CW/0378.gif",
        "Dumbbell Single Leg Squat" to "$CW/0411.gif",
        "Dumbbell Squat" to "$CW/0413.gif",
        "Dumbbell Stiff Leg Deadlift" to "$CW/0432.gif",
        "Dumbbell Standing Calf Raise" to "$CW/0417.gif",

        // Stability Ball
        "Stability Ball Crunch" to "$CW/0271.gif",

        // Weighted
        "Weighted Crunch" to "$CW/0832.gif",
        "Weighted Pull-Up" to "$CW/0841.gif",
        "Weighted Dip" to "$CW/0830.gif",

        // Band
        "Band Bicep Curl" to "$CW/0968.gif",
        "Band Shoulder Press" to "$CW/0997.gif",

        // Misc
        "Lever Deadlift" to "$CW/0578.gif",
        "Lever Hip Thrust" to "$CW/2286.gif",
        "Seated Row Machine" to "$CW/0861.gif",

        // Cardio (missing from defaultExercises)
        "Treadmill" to "$BASE/cardio/walking-on-incline-treadmill.gif",
        "Stationary Bike" to "$BASE/cardio/stationary-bike-run-v-3.gif",
        "Rowing Machine" to "$BASE/cardio/run.gif",
        "Jump Rope" to "$BASE/cardio/jump-rope.gif",
        "Stair Climber" to "$BASE/cardio/walking-on-stepmill.gif",
        "Elliptical" to "$BASE/cardio/walk-elliptical-cross-trainer.gif"
    )

    fun getGif(exerciseName: String): String? = gifs[exerciseName]
}
