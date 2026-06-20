package com.example.gymlog2.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.gymlog2.R

val BebasNeue = FontFamily(Font(R.font.bebas_neue, FontWeight.Normal))

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = BebasNeue, fontSize = 57.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    displayMedium = TextStyle(fontFamily = BebasNeue, fontSize = 45.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    displaySmall = TextStyle(fontFamily = BebasNeue, fontSize = 36.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    headlineLarge = TextStyle(fontFamily = BebasNeue, fontSize = 32.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    headlineMedium = TextStyle(fontFamily = BebasNeue, fontSize = 28.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    headlineSmall = TextStyle(fontFamily = BebasNeue, fontSize = 24.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontFamily = BebasNeue, fontSize = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    titleMedium = TextStyle(fontFamily = BebasNeue, fontSize = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    titleSmall = TextStyle(fontFamily = BebasNeue, fontSize = 14.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    bodyLarge = TextStyle(fontFamily = BebasNeue, fontSize = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    bodyMedium = TextStyle(fontFamily = BebasNeue, fontSize = 14.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    bodySmall = TextStyle(fontFamily = BebasNeue, fontSize = 12.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    labelLarge = TextStyle(fontFamily = BebasNeue, fontSize = 14.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontFamily = BebasNeue, fontSize = 12.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = BebasNeue, fontSize = 11.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp)
)
