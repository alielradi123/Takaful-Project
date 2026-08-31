package com.example.takaful.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.ui.components.InteractiveDialog
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel

private data class RoleOption(
    val key: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val bgColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: TakafulViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current

    // ── Step State ─────────────────────────────────────────────────────────────
    var currentStep by remember { mutableIntStateOf(1) }   // 1 = بيانات أساسية, 2 = إثبات هوية

    // ── Step 1 Fields ──────────────────────────────────────────────────────────
    var name            by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole    by remember { mutableStateOf("donor") }
    var profileUri   by remember { mutableStateOf<Uri?>(null) }

    // ── Step 2 Fields ──────────────────────────────────────────────────────────
    var identityType      by remember { mutableStateOf("رقم وطني") }
    var identityNumber    by remember { mutableStateOf("") }
    var country           by remember { mutableStateOf("") }
    var expiryDate        by remember { mutableStateOf("") }
    var identityFrontUri  by remember { mutableStateOf<Uri?>(null) }
    var identityBackUri   by remember { mutableStateOf<Uri?>(null) }
    var registrationReason by remember { mutableStateOf("") }

    // ── UI State ───────────────────────────────────────────────────────────────
    var isLoading    by remember { mutableStateOf(false) }
    var showError    by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf("") }
    var showSuccess  by remember { mutableStateOf(false) }

    // Image pickers
    val pickImage         = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> profileUri        = uri }
    val pickIdentityFront = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> identityFrontUri  = uri }
    val pickIdentityBack  = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> identityBackUri   = uri }

    val needsVerification = selectedRole == "beneficiary" || selectedRole == "volunteer"
    val totalSteps        = if (needsVerification) 2 else 1

    InteractiveDialog(showError, "تنبيه", errorMsg, true) { showError = false }

    // ── Success Dialog ─────────────────────────────────────────────────────────
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false; onRegisterSuccess() },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Surface(color = Brand100, shape = CircleShape, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Brand600, modifier = Modifier.size(44.dp))
                    }
                }
            },
            title = {
                Text("تم إرسال طلبك!", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Brand600, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (needsVerification) {
                        Text(
                            "طلبك قيد المراجعة من قِبل فريق تكافل. سيتم إشعارك بقرار الموافقة في أقرب وقت.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = Neutral700
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Gold50, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.HourglassTop, null, tint = Gold700, modifier = Modifier.size(20.dp))
                            Text("عادةً ما تستغرق المراجعة من 24 إلى 48 ساعة.", fontSize = 12.sp, color = Gold700)
                        }
                    } else {
                        Text(
                            "تم إنشاء حسابك بنجاح! يمكنك الآن تسجيل الدخول والبدء بالتبرع.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = Neutral700
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSuccess = false; onRegisterSuccess() },
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (needsVerification) "حسناً، سأنتظر" else "تسجيل الدخول", fontWeight = FontWeight.Bold) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val roles = listOf(
        RoleOption("donor",       "متبرع",  "ادعم الحالات وتابع أثر تبرعاتك",       Icons.Outlined.VolunteerActivism, Brand600, Brand100),
        RoleOption("beneficiary", "مستفيد", "قدّم طلب مساعدة وتابع حالته بسهولة", Icons.Outlined.PersonPin,         Teal600,  Teal100),
        RoleOption("volunteer",   "متطوع",  "ساهم ميدانياً في إيصال المساعدات",     Icons.Outlined.Groups,            Gold700,  Gold100),
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

            // ── Gradient Header ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Brush.verticalGradient(listOf(Brand800, Brand600)))
            ) {
                Box(Modifier.size(200.dp).offset((-50).dp, (-40).dp).background(Brush.radialGradient(listOf(Color.White.copy(0.07f), Color.Transparent)), CircleShape))
                Box(Modifier.size(140.dp).align(Alignment.TopEnd).offset(30.dp, 10.dp).background(Brush.radialGradient(listOf(Gold500.copy(0.12f), Color.Transparent)), CircleShape))
            }

            Column(
                modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))

                Text("إنشاء حساب جديد", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("انضم وابدأ بصنع الأثر",  fontSize = 14.sp, color = Color.White.copy(0.75f))

                Spacer(Modifier.height(20.dp))

                // ── Progress Stepper ─────────────────────────────────────────────
                if (needsVerification) {
                    StepProgressBar(currentStep = currentStep, totalSteps = totalSteps)
                    Spacer(Modifier.height(12.dp))
                }

                // ── Card ─────────────────────────────────────────────────────────
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape     = RoundedCornerShape(28.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState)
                                slideInHorizontally(tween(300)) { -it } + fadeIn() togetherWith
                                slideOutHorizontally(tween(300)) { it } + fadeOut()
                            else
                                slideInHorizontally(tween(300)) { it } + fadeIn() togetherWith
                                slideOutHorizontally(tween(300)) { -it } + fadeOut()
                        },
                        label = "step_anim"
                    ) { step ->
                        when (step) {
                            1 -> Step1Content(
                                name           = name,          onNameChange     = { name = it },
                                phone          = phone,         onPhoneChange    = { phone = it },
                                email          = email,         onEmailChange    = { email = it },
                                password       = password,      onPasswordChange = { password = it },
                                passwordVisible = passwordVisible, onTogglePass   = { passwordVisible = !passwordVisible },
                                selectedRole   = selectedRole,  onRoleChange     = { selectedRole = it },
                                profileUri     = profileUri,    onPickProfile    = { pickImage.launch("image/*") },
                                roles          = roles,
                                needsVerification = needsVerification,
                                onNext = {
                                    when {
                                        name.isBlank()        -> { errorMsg = "أدخل الاسم الكامل";               showError = true }
                                        phone.isBlank()       -> { errorMsg = "أدخل رقم الهاتف";                showError = true }
                                        !email.contains("@") -> { errorMsg = "بريد إلكتروني غير صالح";          showError = true }
                                        passwordStrength(password) < 2 -> { errorMsg = "كلمة المرور ضعيفة جداً\nيجب أن تحتوي على:\n• 8 أحرف على الأقل\n• حرف كبير وحرف صغير\n• رقم واحد على الأقل"; showError = true }
                                        else -> if (needsVerification) currentStep = 2 else {
                                            isLoading = true
                                            viewModel.register(
                                                name = name, phone = phone, email = email, password = password,
                                                role = selectedRole, profileImageUri = profileUri,
                                                identityType = null, identityNumber = null,
                                                identityFrontUri = null, identityBackUri = null,
                                                registrationReason = null, context = context,
                                                onSuccess = { isLoading = false; showSuccess = true },
                                                onFailure = { msg -> isLoading = false; errorMsg = msg; showError = true }
                                            )
                                        }
                                    }
                                },
                                isLoading      = isLoading,
                                onBackToLogin  = onBackToLogin
                            )
                            2 -> Step2Content(
                                selectedRole       = selectedRole,
                                identityType       = identityType,    onIdentityTypeChange    = { identityType = it },
                                identityNumber     = identityNumber,  onIdentityNumberChange  = { identityNumber = it },
                                country            = country,          onCountryChange          = { country = it },
                                expiryDate         = expiryDate,       onExpiryChange           = { expiryDate = it },
                                identityFrontUri   = identityFrontUri, onPickFront             = { pickIdentityFront.launch("image/*") },
                                identityBackUri    = identityBackUri,  onPickBack              = { pickIdentityBack.launch("image/*") },
                                registrationReason = registrationReason, onReasonChange        = { registrationReason = it },
                                onBack = { currentStep = 1 },
                                onSubmit = {
                                    when {
                                        identityNumber.isBlank()   -> { errorMsg = "أدخل رقم الهوية";              showError = true }
                                        country.isBlank()          -> { errorMsg = "أدخل البلد/المحافظة";          showError = true }
                                        identityFrontUri == null   -> { errorMsg = "يرجى رفع صورة الوجه الأمامي للهوية"; showError = true }
                                        identityType == "بطاقة قومية" && identityBackUri == null
                                                                   -> { errorMsg = "يرجى رفع صورة الوجه الخلفي للبطاقة القومية"; showError = true }
                                        registrationReason.isBlank() -> { errorMsg = "أدخل سبب التسجيل"; showError = true }
                                        else -> {
                                            isLoading = true
                                            viewModel.register(
                                                name = name, phone = phone, email = email, password = password,
                                                role = selectedRole, profileImageUri = profileUri,
                                                identityType = identityType,
                                                identityNumber = identityNumber,
                                                identityFrontUri = identityFrontUri,
                                                identityBackUri = if (identityType == "بطاقة قومية") identityBackUri else null,
                                                registrationReason = registrationReason,
                                                context = context,
                                                onSuccess = { isLoading = false; showSuccess = true },
                                                onFailure = { msg -> isLoading = false; errorMsg = msg; showError = true }
                                            )
                                        }
                                    }
                                },
                                isLoading = isLoading
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  مؤشر التقدم (Progress Bar)
// ══════════════════════════════════════════════════════════════════
@Composable
private fun StepProgressBar(currentStep: Int, totalSteps: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { index ->
                val stepNum   = index + 1
                val isDone    = currentStep > stepNum
                val isActive  = currentStep == stepNum
                val stepColor = when {
                    isDone   -> Brand600
                    isActive -> Gold500
                    else     -> Color.White.copy(0.3f)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(stepColor, CircleShape)
                        .border(2.dp, Color.White.copy(0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    else Text("$stepNum", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                if (index < totalSteps - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (currentStep > stepNum) Brand600 else Color.White.copy(0.3f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("البيانات الأساسية", fontSize = 11.sp, color = Color.White.copy(0.85f), fontWeight = if (currentStep == 1) FontWeight.Bold else FontWeight.Normal)
            Text("إثبات الهوية", fontSize = 11.sp, color = Color.White.copy(0.85f), fontWeight = if (currentStep == 2) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  الخطوة 1: البيانات الأساسية
// ══════════════════════════════════════════════════════════════════
@Composable
private fun Step1Content(
    name: String,              onNameChange: (String) -> Unit,
    phone: String,             onPhoneChange: (String) -> Unit,
    email: String,             onEmailChange: (String) -> Unit,
    password: String,          onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean = false, onTogglePass: () -> Unit = {},
    selectedRole: String,      onRoleChange: (String) -> Unit,
    profileUri: Uri?,          onPickProfile: () -> Unit,
    roles: List<RoleOption>,
    needsVerification: Boolean,
    onNext: () -> Unit,
    isLoading: Boolean,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier            = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Profile Image ──────────────────────────────────────────────────
        ProfileImagePicker(uri = profileUri, onClick = onPickProfile)
        Spacer(Modifier.height(20.dp))

        // ── Role Selector ──────────────────────────────────────────────────
        SectionHeader(text = "نوع الحساب")
        Spacer(Modifier.height(10.dp))

        roles.forEach { role ->
            val isSelected = selectedRole == role.key
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onRoleChange(role.key) },
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = if (isSelected) role.bgColor else MaterialTheme.colorScheme.surface),
                border   = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) role.accentColor else MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 0.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(42.dp).background(if (isSelected) role.accentColor.copy(0.15f) else MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(role.icon, null, tint = if (isSelected) role.accentColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(role.label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isSelected) role.accentColor else MaterialTheme.colorScheme.onSurface)
                        Text(role.description, fontSize = 11.sp, color = if (isSelected) role.accentColor.copy(0.75f) else MaterialTheme.colorScheme.onSurface.copy(0.55f))
                    }
                    if (isSelected) Icon(Icons.Outlined.CheckCircle, null, tint = role.accentColor, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Fields ─────────────────────────────────────────────────────────
        SectionHeader("البيانات الشخصية")
        Spacer(Modifier.height(12.dp))

        TakafulTextField(name,     onNameChange,     "الاسم الكامل",             Icons.Outlined.Person)
        Spacer(Modifier.height(12.dp))
        TakafulTextField(phone,    onPhoneChange,    "رقم الهاتف",               Icons.Outlined.Phone,   KeyboardType.Phone)
        Spacer(Modifier.height(12.dp))
        TakafulTextField(email,    onEmailChange,    "البريد الإلكتروني",         Icons.Outlined.Email,   KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        TakafulTextField(
            value           = password,
            onValueChange   = onPasswordChange,
            label           = "كلمة المرور",
            icon            = Icons.Outlined.Lock,
            keyboardType    = KeyboardType.Password,
            isPassword      = true,
            passwordVisible = passwordVisible,
            onTogglePass    = onTogglePass
        )
        if (password.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PasswordStrengthIndicator(password = password)
        }

        Spacer(Modifier.height(28.dp))

        // ── Next/Submit Button ─────────────────────────────────────────────
        Button(
            onClick  = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Brand600),
            enabled  = !isLoading
        ) {
            if (isLoading) {
                RegisteringAnimation()
            } else {
                if (needsVerification) {
                    Text("التالي", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Outlined.PersonAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("إنشاء الحساب", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text("لديك حساب بالفعل؟ ", color = MaterialTheme.colorScheme.onSurface.copy(0.55f), fontSize = 14.sp)
            Text("سجّل دخولك", color = Brand600, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onBackToLogin() })
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  الخطوة 2: إثبات الهوية
// ══════════════════════════════════════════════════════════════════
@Composable
private fun Step2Content(
    selectedRole: String,
    identityType: String,      onIdentityTypeChange: (String) -> Unit,
    identityNumber: String,    onIdentityNumberChange: (String) -> Unit,
    country: String,           onCountryChange: (String) -> Unit,
    expiryDate: String,        onExpiryChange: (String) -> Unit,
    identityFrontUri: Uri?,    onPickFront: () -> Unit,
    identityBackUri: Uri?,     onPickBack: () -> Unit,
    registrationReason: String, onReasonChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    val needsBack = identityType == "بطاقة قومية"

    Column(
        modifier            = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع", tint = Brand600)
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text("إثبات الهوية", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Brand700)
                Text("هذه المعلومات مطلوبة للتحقق من هويتك", fontSize = 12.sp, color = Neutral500)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Info box
        Row(
            modifier = Modifier.fillMaxWidth().background(Brand50, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Info, null, tint = Brand600, modifier = Modifier.size(18.dp))
            Text(
                "معلوماتك محمية ومشفرة. لن تُشارك إلا مع فريق تكافل لأغراض التحقق فقط.",
                fontSize = 12.sp, color = Brand700
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── نوع الهوية ──────────────────────────────────────────────────────
        SectionHeader("نوع الهوية")
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("رقم وطني", "جواز سفر", "بطاقة قومية").forEach { type ->
                val isSel = identityType == type
                OutlinedButton(
                    onClick  = { onIdentityTypeChange(type) },
                    colors   = ButtonDefaults.outlinedButtonColors(containerColor = if (isSel) Brand50 else Color.Transparent),
                    border   = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) Brand600 else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Text(type, color = if (isSel) Brand600 else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── رقم الهوية ──────────────────────────────────────────────────────
        SectionHeader("بيانات الهوية")
        Spacer(Modifier.height(12.dp))

        TakafulTextField(identityNumber, onIdentityNumberChange, "رقم الهوية", Icons.Outlined.Badge, KeyboardType.Number)
        Spacer(Modifier.height(12.dp))
        TakafulTextField(country, onCountryChange, "البلد / المحافظة", Icons.Outlined.LocationOn)
        Spacer(Modifier.height(12.dp))
        TakafulTextField(expiryDate, onExpiryChange, "تاريخ انتهاء الوثيقة (مثال: 2030-01)", Icons.Outlined.DateRange)

        Spacer(Modifier.height(20.dp))

        // ── صور الهوية ──────────────────────────────────────────────────────
        SectionHeader("صور الهوية")
        Spacer(Modifier.height(12.dp))

        // أمامي — دائماً مطلوب
        IdentityImagePicker(
            uri    = identityFrontUri,
            label  = "الوجه الأمامي (مطلوب)",
            onClick = onPickFront,
            tint   = Brand600,
            bg     = Brand50,
            border = Brand200
        )

        // خلفي — فقط للبطاقة القومية
        AnimatedVisibility(
            visible = needsBack,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                IdentityImagePicker(
                    uri    = identityBackUri,
                    label  = "الوجه الخلفي (للبطاقة القومية)",
                    onClick = onPickBack,
                    tint   = Teal600,
                    bg     = Teal100,
                    border = Teal400
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── سبب التسجيل ─────────────────────────────────────────────────────
        SectionHeader(if (selectedRole == "volunteer") "المهارات وسبب التطوع" else "سبب طلب المساعدة")
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value         = registrationReason,
            onValueChange = onReasonChange,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(14.dp),
            minLines      = 3,
            maxLines      = 5,
            label         = { Text(if (selectedRole == "volunteer") "اذكر مهاراتك وخبراتك..." else "اشرح وضعك وسبب الطلب...") },
            leadingIcon   = { Icon(Icons.Outlined.Description, null, tint = Brand600) },
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Brand600,
                focusedLabelColor    = Brand600,
                focusedLeadingIconColor = Brand600
            )
        )

        Spacer(Modifier.height(28.dp))

        // ── Submit ──────────────────────────────────────────────────────────
        Button(
            onClick  = onSubmit,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Brand600),
            enabled  = !isLoading
        ) {
            if (isLoading) {
                RegisteringAnimation()
            } else {
                Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("إرسال الطلب للمراجعة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Neutral500, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("العودة للخطوة السابقة", color = Neutral500, fontSize = 13.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  مكوّن رفع صورة الهوية
// ══════════════════════════════════════════════════════════════════
@Composable
private fun IdentityImagePicker(
    uri: Uri?,
    label: String,
    onClick: () -> Unit,
    tint: Color,
    bg: Color,
    border: Color
) {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (uri != null) Color.Transparent else bg)
            .border(1.5.dp, if (uri != null) tint else border, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            coil.compose.AsyncImage(
                model = uri, contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Edit, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Text("تغيير الصورة", color = Color.White, fontSize = 12.sp)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.AddPhotoAlternate, null, tint = tint, modifier = Modifier.size(32.dp))
                Text(label, fontSize = 13.sp, color = tint, fontWeight = FontWeight.Medium)
                Text("اضغط لاختيار صورة", fontSize = 11.sp, color = tint.copy(0.65f))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  مكوّن عنوان القسم
// ══════════════════════════════════════════════════════════════════
@Composable
private fun SectionHeader(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.width(3.dp).height(18.dp)
                .background(Brush.verticalGradient(listOf(Brand600, Teal600)), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ══════════════════════════════════════════════════════════════════
//  مكوّن صورة الملف الشخصي
// ══════════════════════════════════════════════════════════════════
@Composable
private fun ProfileImagePicker(uri: Uri?, onClick: () -> Unit) {
    Box(modifier = Modifier.size(90.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        if (uri != null) {
            coil.compose.AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brand50, CircleShape).border(2.dp, Brand600, CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CameraAlt, null, tint = Brand600, modifier = Modifier.size(28.dp))
                    Text("صورة", fontSize = 11.sp, color = Brand600, fontWeight = FontWeight.Medium)
                }
            }
        }
        Box(
            modifier = Modifier.size(26.dp).align(Alignment.BottomEnd).background(Brand600, CircleShape).border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════
//  أيقونة التحميل المتحركة أثناء التسجيل
// ══════════════════════════════════════════════════════════════════
@Composable
fun RegisteringAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "register_anim")

    // دوران الدرع
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shield_rotation"
    )
    // نبضة للأيقونة
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shield_scale"
    )
    // تألق النص
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "text_alpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { rotationZ = rotation; scaleX = scale; scaleY = scale }
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "جارٍ إنشاء الحساب...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = textAlpha)
        )
    }
}

// ══════════════════════════════════════════════════════════════════
//  مقياس قوة كلمة المرور
// ══════════════════════════════════════════════════════════════════

/** يُرجع: 0=ضعيفة جداً, 1=ضعيفة, 2=متوسطة, 3=قوية, 4=قوية جداً */
fun passwordStrength(password: String): Int {
    if (password.length < 6) return 0
    var score = 0
    if (password.length >= 8)                          score++
    if (password.any { it.isUpperCase() })             score++
    if (password.any { it.isDigit() })                 score++
    if (password.any { "!@#\$%^&*()-_=+[]{}|;:',.<>?/`~".contains(it) }) score++
    return score
}

@Composable
fun PasswordStrengthIndicator(password: String) {
    val strength = passwordStrength(password)

    val (label, barColor, bgColor, textColor) = when (strength) {
        0    -> Quadruple("ضعيفة جداً",  SemanticError,        SemanticError.copy(0.12f),   SemanticError)
        1    -> Quadruple("ضعيفة",       Color(0xFFFF7043),    Color(0xFFFF7043).copy(0.12f), Color(0xFFFF7043))
        2    -> Quadruple("متوسطة",      Color(0xFFFFC107),    Color(0xFFFFC107).copy(0.12f), Color(0xFF7B5800))
        3    -> Quadruple("قوية",         Color(0xFF4CAF50),    Color(0xFF4CAF50).copy(0.12f), Color(0xFF1B5E20))
        else -> Quadruple("قوية جداً ✓", Brand600,             Brand50,                       Brand700)
    }

    val animatedFraction by animateFloatAsState(
        targetValue   = (strength + 1) / 5f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "strength_bar"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── شريط التقدم ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(listOf(barColor.copy(0.7f), barColor))
                    )
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── تسمية المستوى ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = when (strength) {
                        0, 1 -> Icons.Outlined.LockOpen
                        2    -> Icons.Outlined.Lock
                        else -> Icons.Outlined.Shield
                    },
                    contentDescription = null,
                    tint   = textColor,
                    modifier = Modifier.size(14.dp)
                )
                Text("قوة كلمة المرور:", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
            }
            Text(label, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(8.dp))

        // ── قائمة الشروط ──────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PasswordRequirement("8 أحرف على الأقل",            password.length >= 8)
            PasswordRequirement("حرف كبير (A-Z)",               password.any { it.isUpperCase() })
            PasswordRequirement("رقم (0-9)",                    password.any { it.isDigit() })
            PasswordRequirement("رمز خاص (!@#\$...)",           password.any { "!@#\$%^&*()-_=+[]{}|;:',.<>?/`~".contains(it) })
        }
    }
}

@Composable
private fun PasswordRequirement(text: String, met: Boolean) {
    val color  = if (met) Color(0xFF388E3C) else Neutral400
    val icon   = if (met) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 11.sp, color = color)
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ══════════════════════════════════════════════════════════════════
//  TakafulTextField
// ══════════════════════════════════════════════════════════════════
@Composable
private fun TakafulTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePass: () -> Unit = {}
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(14.dp),
        label         = { Text(label) },
        leadingIcon   = { Icon(icon, null, tint = Brand600) },
        trailingIcon  = if (isPassword) ({
            IconButton(onClick = onTogglePass) {
                Icon(
                    if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                    tint = Neutral400
                )
            }
        }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine    = true,
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Brand600,
            focusedLabelColor    = Brand600,
            focusedLeadingIconColor = Brand600
        )
    )
}
