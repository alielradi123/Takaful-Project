package com.example.takaful.ui.screens.auth

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.takaful.ui.components.ForgotPasswordDialog
import com.example.takaful.ui.components.InteractiveDialog
import com.example.takaful.ui.theme.*
import com.example.takaful.utils.SharedPrefsHelper
import com.example.takaful.viewmodel.TakafulViewModel
import kotlinx.coroutines.CoroutineScope

@Composable
fun LoginScreen(
    viewModel: TakafulViewModel,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val prefs   = remember { SharedPrefsHelper(context) }

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading       by remember { mutableStateOf(false) }

    var showErrorDialog   by remember { mutableStateOf(false) }
    var errorMessage      by remember { mutableStateOf("") }
    var showForgotDialog  by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage    by remember { mutableStateOf("") }

    InteractiveDialog(showErrorDialog, "تنبيه", errorMessage, true) { showErrorDialog = false }
    InteractiveDialog(showSuccessDialog, "نجاح", successMessage, false) { showSuccessDialog = false }
    ForgotPasswordDialog(showForgotDialog, viewModel) { showForgotDialog = false }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Header wave ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.verticalGradient(listOf(Brand800, Brand600)))
        ) {
            // نقاط زخرفية
            DecorDots()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Logo ──────────────────────────────────────────────────────
            LoginLogoSection()

            Spacer(Modifier.height(32.dp))

            // ── Card ──────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(20.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "مرحباً بك مجدداً",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Neutral900
                    )
                    Text(
                        "سجّل دخولك للمتابعة",
                        fontSize = 14.sp,
                        color = Neutral500,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(Modifier.height(28.dp))

                    // ── Email ─────────────────────────────────────────────
                    TakafulTextField(
                        value    = email,
                        onChange = { email = it },
                        label    = "البريد الإلكتروني",
                        icon     = Icons.Outlined.Email,
                        keyboard = KeyboardType.Email
                    )

                    Spacer(Modifier.height(14.dp))

                    // ── Password ──────────────────────────────────────────
                    TakafulTextField(
                        value          = password,
                        onChange       = { password = it },
                        label          = "كلمة المرور",
                        icon           = Icons.Outlined.Lock,
                        keyboard       = KeyboardType.Password,
                        isPassword     = true,
                        passwordVisible = passwordVisible,
                        onTogglePass   = { passwordVisible = !passwordVisible }
                    )

                    TextButton(
                        onClick  = { showForgotDialog = true },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("نسيت كلمة المرور؟", color = Brand600, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Login Button ──────────────────────────────────────
                    Button(
                        onClick = {
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                isLoading = true
                                viewModel.login(email, password, context,
                                    onSuccess = { 
                                        isLoading = false
                                        prefs.saveCredentials(email, password)
                                        prefs.isBiometricEnabled = true
                                        onLoginSuccess() 
                                    },
                                    onFailure = { msg -> isLoading = false; errorMessage = msg; showErrorDialog = true }
                                )
                            } else {
                                errorMessage = "يرجى ملء جميع الحقول."
                                showErrorDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Brand600),
                        enabled  = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Text("دخول", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Divider ───────────────────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = Neutral200)
                        Text("  أو  ", color = Neutral400, fontSize = 13.sp)
                        HorizontalDivider(Modifier.weight(1f), color = Neutral200)
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Social Buttons ────────────────────────────────────
                    SocialAndBiometricSection(
                        context          = context,
                        viewModel        = viewModel,
                        scope            = scope,
                        onLoading        = { isLoading = it },
                        onSuccess        = onLoginSuccess,
                        onError          = { err -> errorMessage = err; showErrorDialog = true }
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Register ──────────────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ليس لديك حساب؟ ", color = Neutral500, fontSize = 14.sp)
                        Text(
                            "سجّل الآن",
                            color = Brand600,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onRegisterClick() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Logo for Login ────────────────────────────────────────────────────────
@Composable
private fun LoginLogoSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(Color.White, CircleShape)
                .border(2.dp, Gold500, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.takaful.R.drawable.takaful_logo),
                contentDescription = "تكافل",
                modifier = Modifier.size(62.dp).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("تـكـافـل", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text("نبض الحياة و أشارقة الامل", fontSize = 13.sp, color = Color.White.copy(0.75f))
    }
}

// ── Decorative dots on header ──────────────────────────────────────────────
@Composable
private fun DecorDots() {
    Box(modifier = Modifier.fillMaxSize()) {
        listOf(
            Triple((-20).dp, 30.dp, 100.dp),
            Triple(260.dp, 10.dp, 60.dp),
            Triple(300.dp, 150.dp, 140.dp),
            Triple(20.dp, 180.dp, 80.dp),
        ).forEach { (x, y, size) ->
            Box(
                modifier = Modifier
                    .offset(x, y)
                    .size(size)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
            )
        }
    }
}

// ── Reusable Text Field ────────────────────────────────────────────────────
@Composable
private fun TakafulTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboard: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePass: () -> Unit = {}
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(14.dp),
        singleLine    = true,
        leadingIcon   = { Icon(icon, null, tint = Brand600) },
        trailingIcon  = if (isPassword) ({
            IconButton(onClick = onTogglePass) {
                Icon(
                    if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    null, tint = Neutral400
                )
            }
        }) else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Brand600,
            unfocusedBorderColor = Neutral200,
            focusedLabelColor    = Brand600,
            unfocusedLabelColor  = Neutral400,
            focusedTextColor     = Neutral900,
            unfocusedTextColor   = Neutral900,
            cursorColor          = Brand600
        )
    )
}

// ── Social & Biometric ────────────────────────────────────────────────────
@Composable
private fun SocialAndBiometricSection(
    context: Context,
    viewModel: TakafulViewModel,
    scope: CoroutineScope,
    onLoading: (Boolean) -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val prefs = remember { SharedPrefsHelper(context) }
    val auth  = viewModel.authRepository

    // تحقق من دعم الجهاز للبيومتريك (أو رمز القفل)
    val biometricManager = BiometricManager.from(context)
    val canAuthStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    val canAuthWeak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    val canAuthDevice = biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)

    val hasAnyBiometric = canAuthStrong == BiometricManager.BIOMETRIC_SUCCESS ||
                          canAuthWeak == BiometricManager.BIOMETRIC_SUCCESS ||
                          canAuthDevice == BiometricManager.BIOMETRIC_SUCCESS

    OutlinedButton(
        onClick = { viewModel.loginWithGoogle(context, scope, onLoading, onSuccess, onError) },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Neutral200)
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.takaful.R.drawable.ic_google_logo),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text("المتابعة بـ Google", color = Neutral700, fontWeight = FontWeight.Medium)
    }

    Spacer(Modifier.height(10.dp))
    OutlinedButton(
        onClick = {
            if (hasAnyBiometric) {
                val activity = context.findFragmentActivity()
                if (activity != null) {
                    showBiometricPrompt(activity) { ok ->
                        if (ok) {
                            if (auth.isLoggedIn) { onSuccess() }
                            else {
                                val creds = prefs.getSavedCredentials()
                                if (creds != null) {
                                    onLoading(true)
                                    viewModel.login(creds.first, creds.second, context,
                                        onSuccess = { onLoading(false); onSuccess() },
                                        onFailure = { msg -> onLoading(false); onError(msg) }
                                    )
                                } else {
                                    onError("يرجى تسجيل الدخول بكلمة المرور أولاً لتفعيل هذا الخيار")
                                }
                            }
                        }
                    }
                } else {
                    onError("تعذر فتح نافذة البصمة")
                }
            } else {
                onError("لم يتم إعداد بصمة أو وجه أو رمز قفل على هذا الجهاز. يرجى إعدادها من إعدادات النظام أولاً.")
            }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, Brand600)
    ) {
        Icon(Icons.Outlined.Fingerprint, null, tint = Brand600)
        Spacer(Modifier.width(10.dp))
        Text("دخول بالبصمة أو الوجه", color = Brand600, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * استخراج FragmentActivity من Context بشكل آمن
 * يتعامل مع ContextWrapper المتداخلة في Compose
 */
fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun showBiometricPrompt(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    val biometricManager = BiometricManager.from(activity)

    val strongOk  = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)  == BiometricManager.BIOMETRIC_SUCCESS
    val weakOk    = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)    == BiometricManager.BIOMETRIC_SUCCESS
    val deviceOk  = biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    Log.d("BiometricDebug", "STRONG=$strongOk, WEAK=$weakOk, DEVICE=$deviceOk")

    if (!strongOk && !weakOk && !deviceOk) {
        Log.w("BiometricDebug", "لا توجد طريقة مصادقة متاحة على هذا الجهاز")
        onResult(false)
        return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) {
            Log.d("BiometricDebug", "✅ نجحت المصادقة")
            onResult(true)
        }
        override fun onAuthenticationError(code: Int, msg: CharSequence) {
            Log.d("BiometricDebug", "❌ خطأ في المصادقة: code=$code, msg=$msg")
            onResult(false)
        }
        override fun onAuthenticationFailed() {
            Log.d("BiometricDebug", "⚠️ فشلت المحاولة – بانتظار محاولة أخرى")
            // لا نفعل شيء – BiometricPrompt تُدير المحاولات تلقائياً
        }
    })

    // بناء PromptInfo بأفضل مزيج متاح
    val info = buildBiometricPromptInfo(strongOk, weakOk, deviceOk)
    if (info == null) { onResult(false); return }

    try {
        prompt.authenticate(info)
    } catch (e: Exception) {
        Log.e("BiometricDebug", "خطأ عند عرض نافذة البصمة", e)
        onResult(false)
    }
}

private fun buildBiometricPromptInfo(
    strongOk: Boolean,
    weakOk: Boolean,
    deviceOk: Boolean
): BiometricPrompt.PromptInfo? {
    return try {
        // نحدد أقوى مستوى متاح للبيومتريك.
        // ملاحظة هامة: BIOMETRIC_WEAK يسمح أيضاً بالبصمة القوية.
        // إذا استخدمنا STRONG فقط، فإن النظام سيرفض استخدام الوجه (لأنه يُعتبر Weak في أغلب الأجهزة).
        var authenticators = 0
        if (weakOk || strongOk) {
            // نستخدم WEAK دائماً إذا كان متاحاً للسماح بالوجه + البصمة
            authenticators = authenticators or BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        if (deviceOk) {
            authenticators = authenticators or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        if (authenticators == 0) return null

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("تأكيد الهوية")
            .setSubtitle("استخدم البصمة أو الوجه أو رمز القفل")
            .setAllowedAuthenticators(authenticators)

        // إذا لم يكن رمز الجهاز مسموحاً، يتطلب النظام إضافة زر الإلغاء
        if ((authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 0) {
            builder.setNegativeButtonText("إلغاء")
        }

        builder.build()
    } catch (e: Exception) {
        Log.e("BiometricDebug", "خطأ في بناء PromptInfo", e)
        null
    }
}

