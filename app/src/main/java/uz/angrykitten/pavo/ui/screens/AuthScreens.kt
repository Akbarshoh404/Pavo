package uz.angrykitten.pavo.ui.screens

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import uz.angrykitten.pavo.BuildConfig
import uz.angrykitten.pavo.ui.localization.tr
import uz.angrykitten.pavo.ui.navigation.Screen
import uz.angrykitten.pavo.ui.theme.*
import uz.angrykitten.pavo.ui.viewmodel.AppViewModel
import uz.angrykitten.pavo.ui.viewmodel.PhoneStep

// Auth colors now come from MaterialTheme — supports light & dark mode
private val AuthBg @Composable get() = MaterialTheme.colorScheme.background
private val AuthSurface @Composable get() = MaterialTheme.colorScheme.surface
private val AuthBorder @Composable get() = MaterialTheme.colorScheme.outline
private val AuthText @Composable get() = MaterialTheme.colorScheme.onBackground
private val AuthSubtext @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val AuthCardShape = RoundedCornerShape(28.dp)
private val AuthFieldShape = RoundedCornerShape(22.dp)
private val AuthChipShape = RoundedCornerShape(20.dp)

// ════════════════════════════════════════════════════════════════════════════
// LOGIN SCREEN
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun LoginScreen(viewModel: AppViewModel, navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(AuthTab.EMAIL) }

    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current as Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val resetSentText = tr("Tiklash xati yuborildi", "Reset email sent", "Письмо для сброса отправлено")
    val emailNotFoundText = tr("Email topilmadi", "Email not found", "Email не найден")

    LaunchedEffect(authState.error) {
        authState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearAuthError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pv ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(pv)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(56.dp))

            // Logo
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brand),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Pets, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(24.dp))

            Text(tr("Xush kelibsiz!", "Welcome!", "Добро пожаловать!"), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AuthText)
            Spacer(Modifier.height(4.dp))
            Text(tr("Email yoki telefon bilan kiring", "Sign in with email or phone", "Войдите по email или телефону"), color = AuthSubtext, fontSize = 15.sp)
            Spacer(Modifier.height(32.dp))

            Card(
                shape = AuthCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AuthTabRow(selected = activeTab, onSelect = { activeTab = it; viewModel.resetPhoneStep() })
                    Spacer(Modifier.height(24.dp))

                    AnimatedContent(targetState = activeTab, label = "tab") { tab ->
                when (tab) {
                    AuthTab.EMAIL -> Column {
                        DarkAuthField(value = email, onChange = { email = it }, label = tr("Email", "Email", "Email"),
                            icon = Icons.Default.Email, keyboardType = KeyboardType.Email)
                        Spacer(Modifier.height(16.dp))
                        DarkAuthField(value = password, onChange = { password = it }, label = tr("Parol", "Password", "Пароль"),
                            icon = Icons.Default.Lock, keyboardType = KeyboardType.Password,
                            isPassword = true, passwordVisible = passwordVisible,
                            onToggle = { passwordVisible = !passwordVisible })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                if (email.isNotBlank()) viewModel.sendPasswordReset(email) {
                                    scope.launch { snackbarHostState.showSnackbar(if (it) resetSentText else emailNotFoundText) }
                                }
                            }) { Text(tr("Parolni unutdingizmi?", "Forgot password?", "Забыли пароль?"), color = Brand, fontSize = 13.sp) }
                        }
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton(text = tr("Kirish", "Sign in", "Войти"), isLoading = authState.isLoading,
                            enabled = email.isNotBlank() && password.isNotBlank()) {
                            viewModel.loginWithEmail(email, password,
                                onSuccess = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } },
                                onError = {})
                        }
                    }
                    AuthTab.PHONE -> PhoneSection(
                        viewModel = viewModel, activity = activity,
                        onSuccess = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } }
                    )
                }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            DarkDividerOr()
            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                SocialCircleButton(
                    isLoading = authState.isLoading, label = "Google",
                    icon = { Text("G", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF4285F4)) },
                    onClick = {
                        scope.launch { performGoogleSignIn(context, activity, viewModel, navController, snackbarHostState) }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Guest option ──
            TextButton(
                onClick = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tr("Mehmon sifatida davom etish", "Continue as guest", "Продолжить как гость"), color = AuthSubtext, fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AuthSubtext, modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Akkaunt yo'qmi? ", "No account yet? ", "Нет аккаунта? "), color = AuthSubtext, fontSize = 14.sp)
                TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
                    Text(tr("Ro'yxatdan o'ting", "Register", "Регистрация"), color = Brand, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// REGISTER SCREEN
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun RegisterScreen(viewModel: AppViewModel, navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(false) }

    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current as Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState.error) {
        authState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearAuthError() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = MaterialTheme.colorScheme.background) { pv ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
                .padding(pv).padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(48.dp))
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AuthText)
            }
            Spacer(Modifier.height(16.dp))
            Text(tr("Hisob yaratish", "Create account", "Создать аккаунт"), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AuthText)
            Spacer(Modifier.height(4.dp))
            Text(tr("Barcha imkoniyatlardan foydalaning", "Unlock the full experience", "Откройте все возможности"), color = AuthSubtext, fontSize = 15.sp)
            Spacer(Modifier.height(32.dp))

            Card(
                shape = AuthCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    DarkAuthField(value = name, onChange = { name = it }, label = tr("Ism Familiya", "Full name", "Полное имя"), icon = Icons.Default.Person)
                    Spacer(Modifier.height(16.dp))
                    DarkAuthField(value = email, onChange = { email = it }, label = tr("Email", "Email", "Email"),
                        icon = Icons.Default.Email, keyboardType = KeyboardType.Email)
                    Spacer(Modifier.height(16.dp))
                    DarkAuthField(value = password, onChange = { password = it }, label = tr("Parol", "Password", "Пароль"),
                        icon = Icons.Default.Lock, keyboardType = KeyboardType.Password,
                        isPassword = true, passwordVisible = passwordVisible,
                        onToggle = { passwordVisible = !passwordVisible })
                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = agreed, onCheckedChange = { agreed = it },
                            colors = CheckboxDefaults.colors(checkedColor = Brand, uncheckedColor = AuthBorder))
                        Text(tr("Foydalanish ", "I agree to the ", "Я принимаю "), color = AuthText, fontSize = 13.sp)
                        Text(tr("shartlari", "terms", "условия"), color = Brand, fontSize = 13.sp, modifier = Modifier.clickable {})
                        Text(tr(" va ", " and ", " и "), color = AuthText, fontSize = 13.sp)
                        Text(tr("maxfiylik", "privacy policy", "политику конфиденциальности"), color = Brand, fontSize = 13.sp, modifier = Modifier.clickable {})
                    }
                    Spacer(Modifier.height(24.dp))

                    PrimaryButton(text = tr("Ro'yxatdan o'tish", "Register", "Регистрация"), isLoading = authState.isLoading,
                        enabled = email.isNotBlank() && password.length >= 6 && agreed) {
                        viewModel.registerWithEmail(name, email, password,
                            onSuccess = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } },
                            onError = {})
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            DarkDividerOr()
            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                SocialCircleButton(isLoading = authState.isLoading, label = "Google",
                    icon = { Text("G", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF4285F4)) },
                    onClick = {
                        scope.launch { performGoogleSignIn(context, activity, viewModel, navController, snackbarHostState) }
                    })
            }

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Akkauntingiz bormi? ", "Already have an account? ", "Уже есть аккаунт? "), color = AuthSubtext, fontSize = 14.sp)
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(tr("Kirish", "Sign in", "Войти"), color = Brand, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// GUEST PROMPT (soft, non-blocking)
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun GuestPromptScreen(title: String, message: String, navController: NavController) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurface)
        }
        Column(Modifier.fillMaxSize().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(AuthCardShape)
                    .background(Brand.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(36.dp), tint = Brand)
            }
            Spacer(Modifier.height(20.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))
            PrimaryButton(tr("Kirish", "Sign in", "Войти"), isLoading = false, enabled = true) { navController.navigate(Screen.Login.route) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { navController.navigate(Screen.Register.route) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = AuthFieldShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Text(tr("Ro'yxatdan o'tish", "Register", "Регистрация"), fontWeight = FontWeight.SemiBold, color = AuthText)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ════════════════════════════════════════════════════════════════════════════

enum class AuthTab { EMAIL, PHONE }

@Composable
private fun AuthTabRow(selected: AuthTab, onSelect: (AuthTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, AuthCardShape)
            .background(MaterialTheme.colorScheme.surface).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AuthTab.entries.forEach { tab ->
            val sel = tab == selected
            Box(
                Modifier.weight(1f)
                    .clip(AuthChipShape)
                    .background(if (sel) Brand else Color.Transparent)
                    .clickable { onSelect(tab) }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (tab == AuthTab.EMAIL) tr("Email", "Email", "Email") else tr("Telefon", "Phone", "Телефон"),
                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AuthField(
    value: String, onChange: (String) -> Unit, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false, passwordVisible: Boolean = false,
    onToggle: (() -> Unit)? = null
) {
    DarkAuthField(value, onChange, label, icon, keyboardType, isPassword, passwordVisible, onToggle)
}

@Composable
fun DarkAuthField(
    value: String, onChange: (String) -> Unit, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false, passwordVisible: Boolean = false,
    onToggle: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Brand, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) ({
            IconButton(onClick = { onToggle?.invoke() }) {
                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }) else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = AuthFieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = Brand
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}

@Composable
fun PrimaryButton(text: String, isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = AuthFieldShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Brand,
            disabledContainerColor = Brand.copy(alpha = 0.3f)
        ),
        enabled = enabled && !isLoading,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
    }
}

@Composable
private fun DarkDividerOr() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text("  yoki  ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SocialCircleButton(
    isLoading: Boolean, label: String,
    icon: @Composable () -> Unit, onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp)
                .clip(AuthFieldShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, AuthFieldShape)
                .clickable(enabled = !isLoading) { onClick() },
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(4.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

// ─── Phone OTP Section ───────────────────────────────────────────────────────
@Composable
private fun PhoneSection(viewModel: AppViewModel, activity: Activity, onSuccess: () -> Unit) {
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    var phone by remember { mutableStateOf("+998") }
    var otpDigits by remember { mutableStateOf(List(6) { "" }) }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    AnimatedContent(targetState = authState.phoneStep, label = "phoneStep") { step ->
        when (step) {
            PhoneStep.ENTER_NUMBER -> Column {
                DarkAuthField(value = phone, onChange = { phone = it }, label = tr("Telefon (+998XXXXXXXXX)", "Phone (+998XXXXXXXXX)", "Телефон (+998XXXXXXXXX)"),
                    icon = Icons.Default.Phone, keyboardType = KeyboardType.Phone)
                Spacer(Modifier.height(20.dp))
                PrimaryButton(tr("SMS kod yuborish", "Send SMS code", "Отправить SMS-код"), isLoading = authState.isLoading, enabled = phone.length >= 12) {
                    viewModel.sendPhoneOtp(phone, activity, onAutoVerified = onSuccess)
                }
            }
            PhoneStep.ENTER_OTP -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Sms, null, tint = Brand, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(tr("SMS kodni kiriting", "Enter the SMS code", "Введите SMS-код"), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AuthText)
                Spacer(Modifier.height(6.dp))
                Text(tr("$phone ga yuborildi", "Sent to $phone", "Отправлено на $phone"), color = AuthSubtext, fontSize = 14.sp)
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    otpDigits.forEachIndexed { i, digit ->
                        OutlinedTextField(
                            value = digit,
                            onValueChange = { v ->
                                val c = v.filter { it.isDigit() }.take(1)
                                otpDigits = otpDigits.toMutableList().also { it[i] = c }
                                if (c.isNotEmpty()) {
                                    if (i < 5) focusRequesters[i + 1].requestFocus()
                                    else focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier.width(46.dp).height(56.dp).focusRequester(focusRequesters[i]),
                            shape = AuthFieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Brand, unfocusedBorderColor = AuthBorder,
                                focusedContainerColor = AuthSurface, unfocusedContainerColor = AuthSurface,
                                focusedTextColor = AuthText, unfocusedTextColor = AuthText, cursorColor = Brand
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AuthText
                            ),
                            singleLine = true
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Kod kelmadimi? ", "Didn't get the code? ", "Код не пришел? "), color = AuthSubtext, fontSize = 13.sp)
                    TextButton(onClick = { viewModel.resetPhoneStep() }) {
                        Text(tr("Qayta yuborish", "Resend", "Отправить снова"), color = ColorError, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(tr("Tasdiqlash", "Verify", "Подтвердить"), isLoading = authState.isLoading,
                    enabled = otpDigits.all { it.isNotEmpty() }) {
                    viewModel.verifyPhoneOtp(otpDigits.joinToString(""), onSuccess = onSuccess, onError = {})
                }
                TextButton(onClick = { viewModel.resetPhoneStep(); otpDigits = List(6) { "" } }) {
                    Text(tr("Raqamni o'zgartirish", "Change number", "Изменить номер"), color = AuthSubtext, fontSize = 13.sp)
                }
            }
        }
    }
    LaunchedEffect(authState.phoneStep) {
        if (authState.phoneStep == PhoneStep.ENTER_OTP) {
            try { focusRequesters[0].requestFocus() } catch (_: Exception) {}
        }
    }
}

// ─── Google Sign-In ──────────────────────────────────────────────────────────
private suspend fun performGoogleSignIn(
    context: android.content.Context,
    activity: Activity,
    viewModel: AppViewModel,
    navController: NavController,
    snackbar: SnackbarHostState
) {
    val webClientId = resolveGoogleWebClientId(context)
    if (webClientId == null) {
        snackbar.showSnackbar(
            "Google kirish sozlanmagan. Firebase Console → Authentication → Google → Web Client ID ni " +
            "local.properties ga GOOGLE_WEB_CLIENT_ID=... qilib qo'shing."
        )
        return
    }

    val credentialManager = CredentialManager.create(context)

    // Step 1: Try One-Tap (GetGoogleIdOption). Works great for returning users.
    val oneTapRequest = GetCredentialRequest.Builder()
        .addCredentialOption(
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()
        ).build()

    val credentialResult = try {
        credentialManager.getCredential(request = oneTapRequest, context = activity)
    } catch (e: GetCredentialException) {
        val isNoCredential = e.javaClass.simpleName.contains("NoCredential", ignoreCase = true) ||
            e.message?.contains("No credentials available", ignoreCase = true) == true ||
            e.message?.contains("no credential", ignoreCase = true) == true

        if (!isNoCredential) {
            // User cancelled or a hard error — bail out
            if (e.message?.contains("Cancel", ignoreCase = true) == true ||
                e.message?.contains("interrupt", ignoreCase = true) == true) return
            snackbar.showSnackbar("Google kirish xatosi: ${e.message?.take(80)}")
            return
        }

        // Step 2: Fall back to the full Google Sign-In bottom sheet
        try {
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetSignInWithGoogleOption.Builder(webClientId).build()
                ).build()
            credentialManager.getCredential(request = fallbackRequest, context = activity)
        } catch (fe: GetCredentialException) {
            if (fe.message?.contains("Cancel", ignoreCase = true) == true ||
                fe.message?.contains("interrupt", ignoreCase = true) == true) return
            snackbar.showSnackbar("Google kirish xatosi: ${fe.message?.take(80)}")
            return
        } catch (fe: Exception) {
            snackbar.showSnackbar("Xatolik: ${fe.message?.take(80)}")
            return
        }
    } catch (e: Exception) {
        snackbar.showSnackbar("Xatolik: ${e.message?.take(80)}")
        return
    }

    // Extract the ID token from whichever flow succeeded
    val credential = credentialResult.credential
    val idToken = when {
        credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
            try {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } catch (e: Exception) {
                snackbar.showSnackbar("Google token noto'g'ri: ${e.message?.take(60)}")
                return
            }
        }
        else -> {
            snackbar.showSnackbar("Google kirish uchun yaroqli credential olinmadi.")
            return
        }
    }

    viewModel.signInWithGoogle(
        idToken = idToken,
        onSuccess = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } },
        onError = { msg -> /* error is already shown via authState.error → LaunchedEffect snackbar */ }
    )
}

private fun resolveGoogleWebClientId(context: android.content.Context): String? {
    // 1. Check build config (set via local.properties GOOGLE_WEB_CLIENT_ID)
    BuildConfig.GOOGLE_WEB_CLIENT_ID.takeUnless { it.isBlank() }?.let { return it }
    // 2. Check auto-generated string from google-services.json (only present when oauth_client is configured)
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId != 0) {
        val id = context.getString(resId)
        if (id.isNotBlank() && !id.startsWith("YOUR_")) return id
    }
    return null
}
