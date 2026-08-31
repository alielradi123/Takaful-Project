package com.example.takaful.ui.screens.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.takaful.MainActivity
import com.example.takaful.data.model.SystemSettings
import com.example.takaful.ui.theme.*
import com.example.takaful.utils.SharedPrefsHelper
import com.example.takaful.viewmodel.TakafulViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: TakafulViewModel
) {
    val context = LocalContext.current
    val sharedPrefsHelper = remember { SharedPrefsHelper(context) }
    var isDarkMode by remember { mutableStateOf(sharedPrefsHelper.isDarkMode) }
    var isBiometricEnabled by remember { mutableStateOf(sharedPrefsHelper.isBiometricEnabled) }
    var isNotificationsEnabled by remember { mutableStateOf(sharedPrefsHelper.isNotificationsEnabled) }
    
    val profile by viewModel.userProfile.collectAsState()
    val name = profile.name.ifBlank { "مستخدم" }
    val email = profile.email
    val phone = profile.phone
    val role = profile.role

    var profileImagePath by remember { mutableStateOf(sharedPrefsHelper.profileImagePath) }
    val photoURL by viewModel.photoURL.collectAsState()
    var isUploadingImage by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAboutUsDialog by remember { mutableStateOf(false) }

    val donations by viewModel.donations.collectAsState()
    val cases by viewModel.cases.collectAsState()
    
    val totalDonationsAmount = donations.sumOf { it.amount }
    val totalSupportedCases = donations.map { it.caseId }.distinct().size
    val totalRequests = cases.count { it.id == profile.uid } // Assuming simplified for beneficiary

    val sysSettings by viewModel.sysSettings.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            isUploadingImage = true
            Toast.makeText(context, "جاري رفع الصورة...", Toast.LENGTH_SHORT).show()
            viewModel.userRepository.updateProfilePicture(
                context = context,
                uri = it,
                localPath = null,
                onSuccess = { newUrl ->
                    isUploadingImage = false
                    Toast.makeText(context, "تم تحديث الصورة بنجاح", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    isUploadingImage = false
                    Toast.makeText(context, "فشل في رفع الصورة", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val stat1Title = if (role == "donor") "إجمالي التبرعات" else "الطلبات المقدمة"
    val stat1Value = if (role == "donor") "${totalDonationsAmount.toInt()} ج.س" else "$totalRequests طلب"
    val stat2Title = if (role == "donor") "حالات دعمتها" else "الطلبات المنجزة"
    val stat2Value = if (role == "donor") "$totalSupportedCases حالة" else "0"

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (showEditDialog) {
            EditProfileDialog(
                currentName = name,
                currentPhone = phone,
                onDismiss = { showEditDialog = false },
                onSave = { newName, newPhone ->
                    viewModel.userRepository.updateProfile(
                        name = newName,
                        phone = newPhone,
                        onSuccess = {
                            showEditDialog = false
                            Toast.makeText(context, "تم تحديث البيانات", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding()),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                item {
                    ProfileHeaderCard(
                        name = name,
                        email = email,
                        phone = phone,
                        stat1Title = stat1Title,
                        stat1Value = stat1Value,
                        stat2Title = stat2Title,
                        stat2Value = stat2Value,
                        profileImagePath = profileImagePath,
                        photoURL = photoURL,
                        isUploadingImage = isUploadingImage,
                        onEditClick = { showEditDialog = true },
                        onImageClick = { if (!isUploadingImage) imagePickerLauncher.launch("image/*") },
                        onDeleteImageClick = {
                            if (!isUploadingImage) {
                                isUploadingImage = true
                                Toast.makeText(context, "جاري حذف الصورة...", Toast.LENGTH_SHORT).show()
                                viewModel.userRepository.deleteProfilePicture(
                                    context = context,
                                    onSuccess = {
                                        isUploadingImage = false
                                        profileImagePath = null
                                        Toast.makeText(context, "تم حذف الصورة بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = {
                                        isUploadingImage = false
                                        profileImagePath = null
                                        Toast.makeText(context, "تم حذف الصورة محلياً", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        onDonationsClick = { navController.navigate("donations_history") },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                item {
                    SectionHeader("الإعدادات العامة")
                    SettingsSection {
                        SettingsToggleItem(Icons.Default.DarkMode, "الوضع الليلي", isDarkMode) { checked ->
                            isDarkMode = checked; sharedPrefsHelper.isDarkMode = checked
                            var ctx = context as android.content.Context
                            while (ctx is android.content.ContextWrapper) { if (ctx is MainActivity) break; ctx = ctx.baseContext }
                            (ctx as? MainActivity)?.toggleDarkMode(checked)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsToggleItem(Icons.Default.Fingerprint, "الدخول بالبصمة", isBiometricEnabled) { checked ->
                            isBiometricEnabled = checked; sharedPrefsHelper.isBiometricEnabled = checked
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsToggleItem(Icons.Default.Notifications, "تفعيل الإشعارات", isNotificationsEnabled) { checked ->
                            isNotificationsEnabled = checked; sharedPrefsHelper.isNotificationsEnabled = checked
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("خيارات أخرى")
                    SettingsSection {
                        SettingsActionItem(Icons.Default.History, "سجل التبرعات") { navController.navigate("donations_history") }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsActionItem(Icons.Default.Info, "عن التطبيق") { showAboutUsDialog = true }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsActionItem(
                            Icons.AutoMirrored.Filled.ExitToApp, "تسجيل الخروج",
                            titleColor = Color(0xFFD32F2F), iconColor = Color(0xFFD32F2F)
                        ) {
                            viewModel.logout(context)
                            navController.navigate("login") { popUpTo(0) { inclusive = true } }
                        }
                    }
                }
            }

            if (showPaymentDialog) {
                PaymentMethodsDialog(onDismiss = { showPaymentDialog = false }, context = context)
            }
            if (showAboutUsDialog) {
                AboutUsDialog(sysSettings = sysSettings ?: SystemSettings(), onDismiss = { showAboutUsDialog = false }, context = context)
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    name: String, email: String, phone: String,
    stat1Title: String, stat1Value: String,
    stat2Title: String, stat2Value: String,
    profileImagePath: String?, photoURL: String,
    isUploadingImage: Boolean,
    onEditClick: () -> Unit, onImageClick: () -> Unit,
    onDeleteImageClick: () -> Unit,
    onDonationsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var showImageMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Gradient Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.verticalGradient(listOf(Brand800, Brand600)))
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // Custom Top Bar Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "رجوع", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text("الملف الشخصي", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
            }

            // User Info
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .clickable {
                                if (!isUploadingImage) {
                                    if (!profileImagePath.isNullOrEmpty() || photoURL.isNotEmpty()) {
                                        showImageMenu = true
                                    } else {
                                        onImageClick()
                                    }
                                }
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        if (isUploadingImage) {
                            Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand600, modifier = Modifier.size(24.dp)) }
                        } else {
                            val file = profileImagePath?.let { File(it) }
                            if (file != null && file.exists()) {
                                val bitmap = remember(profileImagePath) { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
                                if (bitmap != null) Image(bitmap, "Profile", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Icon(Icons.Default.Person, null, tint = Brand600, modifier = Modifier.padding(16.dp))
                            } else if (photoURL.isNotEmpty()) {
                                coil.compose.AsyncImage(photoURL, "Profile", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.Person, null, tint = Brand600, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = showImageMenu,
                        onDismissRequest = { showImageMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تغيير الصورة") },
                            onClick = { showImageMenu = false; onImageClick() },
                            leadingIcon = { Icon(Icons.Outlined.PhotoCamera, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف الصورة", color = MaterialTheme.colorScheme.error) },
                            onClick = { showImageMenu = false; onDeleteImageClick() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(email, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                    if (phone.isNotBlank()) Text(phone, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                }
                IconButton(onClick = onEditClick, modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)) {
                    Icon(Icons.Default.Edit, "تعديل", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            // Stats Card (Overlapping)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Brand900.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox(stat1Title, stat1Value, if (stat1Title.contains("تبرع")) Icons.Default.MonetizationOn else Icons.Outlined.FolderOpen, Modifier.weight(1f), onClick = { if(stat1Title.contains("تبرع")) onDonationsClick() })
                    Spacer(Modifier.width(16.dp))
                    StatBox(stat2Title, stat2Value, if (stat2Title.contains("حالات")) Icons.Default.Favorite else Icons.Outlined.CheckCircle, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatBox(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(modifier
        .clip(RoundedCornerShape(16.dp))
        .background(Brand600.copy(alpha = 0.04f))
        .clickable { onClick() }
        .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(48.dp).background(Brand600.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Brand600, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
}

@Composable
private fun SettingsSection(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) { Column(content = content) }
}

@Composable
private fun SettingsToggleItem(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, title, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        Switch(checked, onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Brand600, checkedTrackColor = Brand600.copy(alpha = 0.5f)))
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector, title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = Brand600,
    onClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (iconColor == Brand600) MaterialTheme.colorScheme.primaryContainer else iconColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, title, tint = if (iconColor == Brand600) MaterialTheme.colorScheme.onPrimaryContainer else iconColor)
            }
            Spacer(Modifier.width(16.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = titleColor)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun EditProfileDialog(currentName: String, currentPhone: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("تعديل الملف الشخصي", fontWeight = FontWeight.Bold, color = Brand800) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("الاسم الكامل") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(phone, { phone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = { Button(onClick = { onSave(name, phone) }, colors = ButtonDefaults.buttonColors(containerColor = Brand800)) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = MaterialTheme.colorScheme.onSurface) } },
            containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun PaymentMethodsDialog(onDismiss: () -> Unit, context: Context) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("طرق الدفع المتوفرة", fontWeight = FontWeight.Bold, color = Brand800) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("طرق الدفع المفعلة لحسابك مصنفة حسب المنطقة:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                    
                    Spacer(Modifier.height(4.dp))
                    
                    // --- Sudan Section ---
                    Text(
                        text = "جمهورية السودان (ج.س)",
                        fontWeight = FontWeight.Bold,
                        color = Gold700,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    val sudanMethods = listOf(
                        "تحويل مباشر (BBAN)" to Icons.Outlined.AccountBalance,
                        "بنكك | PAY (QR)" to Icons.Outlined.QrCode2,
                        "mBok - بنك الخرطوم" to Icons.Outlined.PhoneAndroid,
                        "صح (Sah)" to Icons.Outlined.Smartphone,
                        "فوري (Fawry)" to Icons.Outlined.Payment,
                        "أوكاش (Ocash)" to Icons.Outlined.Wallet
                    )
                    
                    sudanMethods.forEach { (name, icon) ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "طريقة الدفع \$name جاهزة للاستخدام", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = MaterialTheme.colorScheme.onSurface) } },
            containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun AboutUsDialog(
    sysSettings: SystemSettings,
    onDismiss: () -> Unit,
    context: Context
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("عن جمعية تكافل", fontWeight = FontWeight.Bold, color = Brand800) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = sysSettings.aboutUsText.ifBlank { "جمعية تكافل الإنسانية تهدف لدعم المحتاجين وتسهيل التبرعات عبر التطبيق." },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )

                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Neutral200)

                    if (sysSettings.contactEmail.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, null, tint = Brand600, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(sysSettings.contactEmail, fontSize = 14.sp, color = Neutral700)
                        }
                    }

                    if (sysSettings.contactPhone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, null, tint = Brand600, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(sysSettings.contactPhone, fontSize = 14.sp, color = Neutral700)
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Neutral200)

                    // Social Links
                    Text("تابعنا على:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Neutral900)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (sysSettings.facebookUrl.isNotBlank()) {
                            Text(
                                "فيسبوك",
                                color = Color.Blue,
                                modifier = Modifier.clickable {
                                    try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(sysSettings.facebookUrl))) } catch (e: Exception) {}
                                }
                            )
                        }
                        if (sysSettings.twitterUrl.isNotBlank()) {
                            Text(
                                "تويتر",
                                color = Color(0xFF1DA1F2),
                                modifier = Modifier.clickable {
                                    try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(sysSettings.twitterUrl))) } catch (e: Exception) {}
                                }
                            )
                        }
                        if (sysSettings.instagramUrl.isNotBlank()) {
                            Text(
                                "انستقرام",
                                color = Color(0xFFC13584),
                                modifier = Modifier.clickable {
                                    try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(sysSettings.instagramUrl))) } catch (e: Exception) {}
                                }
                            )
                        }
                    }

                    if (sysSettings.privacyPolicyUrl.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "سياسة الخصوصية",
                            color = Brand600,
                            modifier = Modifier.clickable {
                                try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(sysSettings.privacyPolicyUrl))) } catch (e: Exception) {}
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("إغلاق", color = Brand800) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
