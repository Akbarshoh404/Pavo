package uz.angrykitten.pavo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import uz.angrykitten.pavo.ui.localization.LocalAppLanguage
import uz.angrykitten.pavo.ui.localization.AppLanguage
import uz.angrykitten.pavo.ui.navigation.Screen
import uz.angrykitten.pavo.ui.theme.Brand
import uz.angrykitten.pavo.ui.viewmodel.AppViewModel

@Composable
fun SplashScreen(viewModel: AppViewModel, navController: NavController) {

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        // Always go to Home — account is optional throughout the app
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .scale(scale)
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brand),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Pets, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Pavo",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                letterSpacing = 4.sp,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when (LocalAppLanguage.current) {
                    AppLanguage.EN -> "Animal Marketplace"
                    AppLanguage.RU -> "Рынок животных"
                    else -> "Hayvonlar bozori"
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}
