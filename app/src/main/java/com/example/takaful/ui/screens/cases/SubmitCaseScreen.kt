package com.example.takaful.ui.screens.cases

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel

@Composable
fun SubmitCaseScreen(
    viewModel: TakafulViewModel,
    onCaseSubmitted: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) } // 1, 2, 3

    // State: Step 1 (Type)
    var selectedCategory by remember { mutableStateOf("") }
    var selectedUrgency by remember { mutableStateOf("عادي") }

    // State: Step 2 (Details)
    var caseTitle by remember { mutableStateOf("") }
    var caseDescription by remember { mutableStateOf("") }
    var caseLocation by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgressText by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Step UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = 80.dp)
        ) {
            // Header
            Surface(
                color = Brand600,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "طلب مساعدة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(16.dp))

                    // Step Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepItem(stepNumber = 1, title = "نوع الطلب", isActive = currentStep == 1, isCompleted = currentStep > 1)
                        HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = if (currentStep >= 2) Color.White else Color.White.copy(0.4f))
                        StepItem(stepNumber = 2, title = "التفاصيل", isActive = currentStep == 2, isCompleted = currentStep > 2)
                        HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = if (currentStep >= 3) Color.White else Color.White.copy(0.4f))
                        StepItem(stepNumber = 3, title = "المراجعة", isActive = currentStep == 3, isCompleted = false)
                    }
                }
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (currentStep) {
                    1 -> StepOneContent(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        selectedUrgency = selectedUrgency,
                        onUrgencySelected = { selectedUrgency = it },
                        onNext = {
                            if (selectedCategory.isBlank()) {
                                Toast.makeText(context, "الرجاء اختيار نوع المساعدة", Toast.LENGTH_SHORT).show()
                            } else {
                                currentStep = 2
                            }
                        }
                    )
                    2 -> StepTwoContent(
                        category = selectedCategory,
                        title = caseTitle,
                        onTitleChange = { caseTitle = it },
                        description = caseDescription,
                        onDescriptionChange = { caseDescription = it },
                        location = caseLocation,
                        onLocationChange = { caseLocation = it },
                        phone = contactPhone,
                        onPhoneChange = { contactPhone = it },
                        amount = targetAmountText,
                        onAmountChange = { targetAmountText = it },
                        selectedImageUri = selectedImageUri,
                        onSelectImageClick = { imagePickerLauncher.launch("image/*") },
                        onClearImage = { selectedImageUri = null },
                        onBack = { currentStep = 1 },
                        onNext = {
                            if (caseTitle.isBlank() || caseDescription.isBlank() || caseLocation.isBlank() || contactPhone.isBlank()) {
                                Toast.makeText(context, "الرجاء تعبئة جميع الحقول المطلوبة", Toast.LENGTH_SHORT).show()
                                return@StepTwoContent
                            }
                            if (contactPhone.length < 9) {
                                Toast.makeText(context, "الرجاء إدخال رقم هاتف صحيح", Toast.LENGTH_SHORT).show()
                                return@StepTwoContent
                            }
                            if (selectedCategory == "مالي" && targetAmountText.toDoubleOrNull() == null) {
                                Toast.makeText(context, "الرجاء إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                                return@StepTwoContent
                            }
                            currentStep = 3
                        }
                    )
                    3 -> StepThreeContent(
                        category = selectedCategory,
                        urgency = selectedUrgency,
                        title = caseTitle,
                        description = caseDescription,
                        location = caseLocation,
                        phone = contactPhone,
                        amount = targetAmountText,
                        hasImage = selectedImageUri != null,
                        isUploading = isUploading,
                        uploadProgressText = uploadProgressText,
                        onBack = { currentStep = 2 },
                        onSubmit = {
                            isUploading = true
                            uploadProgressText = "جاري رفع المستندات وحفظ الطلب..."
                            
                            val fullDescription = "$caseDescription\n\n📞 رقم الهاتف للتواصل: $contactPhone"
                            
                            submitFinalCase(
                                viewModel = viewModel,
                                title = caseTitle,
                                description = fullDescription,
                                location = caseLocation,
                                category = selectedCategory,
                                urgencyLevel = selectedUrgency,
                                targetAmount = targetAmountText.toDoubleOrNull() ?: 0.0,
                                imageUri = selectedImageUri,
                                context = context
                            ) {
                                isUploading = false
                                onCaseSubmitted()
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun submitFinalCase(
    viewModel: TakafulViewModel,
    title: String,
    description: String,
    location: String,
    category: String,
    urgencyLevel: String,
    targetAmount: Double,
    imageUri: Uri?,
    context: android.content.Context,
    onSuccess: () -> Unit
) {
    viewModel.addCase(
        title = title,
        location = location,
        category = category,
        urgencyLevel = urgencyLevel,
        targetAmount = targetAmount,
        description = description,
        imageUri = imageUri,
        context = context,
        onSuccess = {
            Toast.makeText(context, "تم إرسال طلبك بنجاح للمراجعة!", Toast.LENGTH_LONG).show()
            onSuccess()
        },
        onFailure = {
            Toast.makeText(context, "حدث خطأ أثناء الإرسال", Toast.LENGTH_LONG).show()
        }
    )
}

@Composable
private fun StepItem(stepNumber: Int, title: String, isActive: Boolean, isCompleted: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) Color.White 
                    else if (isActive) Brand100.copy(0.25f) 
                    else Color.White.copy(0.15f)
                )
                .border(
                    BorderStroke(1.5.dp, if (isActive || isCompleted) Color.White else Color.White.copy(0.4f)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Outlined.Check, contentDescription = "مكتمل", tint = Brand600, modifier = Modifier.size(18.dp))
            } else {
                Text(
                    text = "$stepNumber", 
                    color = if (isActive) Color.White else Color.White.copy(0.6f), 
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = title, 
            color = if (isActive || isCompleted) Color.White else Color.White.copy(alpha = 0.6f), 
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun StepOneContent(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedUrgency: String,
    onUrgencySelected: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("ما نوع المساعدة التي تحتاجها؟", color = Neutral900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        val categories = listOf(
            Triple("مالي", "دعم مالي مباشر", Icons.Outlined.MonetizationOn),
            Triple("عيني", "مواد ومستلزمات عينية", Icons.Outlined.Inventory2),
            Triple("طبي", "علاج ورعاية صحية", Icons.Outlined.LocalHospital)
        )

        categories.forEach { (cat, desc, icon) ->
            val isSelected = selectedCategory == cat
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onCategorySelected(cat) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Brand100 else Color.White),
                border = BorderStroke(1.dp, if (isSelected) Brand600 else Neutral200)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = if (isSelected) Brand600 else Neutral500, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat, color = Neutral900, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(desc, color = Neutral500, fontSize = 12.sp)
                    }
                    if (isSelected) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Brand600)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("درجة الإلحاح", color = Neutral900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("عادي", "متوسط", "عاجل").forEach { urgency ->
                val isSelected = selectedUrgency == urgency
                OutlinedButton(
                    onClick = { onUrgencySelected(urgency) },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) Gold100 else Color.Transparent,
                        contentColor = if (isSelected) Gold700 else Neutral500
                    ),
                    border = BorderStroke(1.dp, if (isSelected) Gold500 else Neutral200)
                ) {
                    Text(urgency)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand600),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("التالي", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepTwoContent(
    category: String,
    title: String, onTitleChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit,
    location: String, onLocationChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    amount: String, onAmountChange: (String) -> Unit,
    selectedImageUri: Uri?,
    onSelectImageClick: () -> Unit,
    onClearImage: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("عنوان الطلب (مثل: عملية جراحية عاجلة)") },
            modifier = Modifier.fillMaxWidth(),
            colors = darkTextFieldColors()
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("وصف الحالة بالتفصيل والاحتياجات") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 5,
            colors = darkTextFieldColors()
        )
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                label = { Text("الموقع / المدينة") },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                colors = darkTextFieldColors()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("رقم الهاتف للتواصل") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                colors = darkTextFieldColors()
            )
        }
        Spacer(Modifier.height(16.dp))

        if (category == "مالي") {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("المبلغ المطلوب (ج.س)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = darkTextFieldColors()
            )
            Spacer(Modifier.height(16.dp))
        }

        // Image Selection & Preview
        Text("إرفاق مستندات أو صور تدعم الحالة (اختياري)", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        if (selectedImageUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
            ) {
                coil.compose.AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "المرفق المختار",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                IconButton(
                    onClick = onClearImage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(0.5f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Outlined.Delete, "حذف الصورة", tint = Color.White)
                }
            }
        } else {
            OutlinedButton(
                onClick = onSelectImageClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Brand600),
                border = BorderStroke(1.dp, Neutral200),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = Brand600)
                Spacer(Modifier.width(8.dp))
                Text("اختيار ملف...")
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Neutral900),
                border = BorderStroke(1.dp, Neutral200)
            ) {
                Text("رجوع")
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("التالي", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepThreeContent(
    category: String,
    urgency: String,
    title: String,
    description: String,
    location: String,
    phone: String,
    amount: String,
    hasImage: Boolean,
    isUploading: Boolean,
    uploadProgressText: String,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("مراجعة البيانات", color = Neutral900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                ReviewRow("نوع الطلب:", category)
                ReviewRow("درجة الإلحاح:", urgency)
                ReviewRow("العنوان:", title)
                ReviewRow("الوصف والاحتياجات:", description)
                ReviewRow("الموقع / المدينة:", location)
                ReviewRow("رقم للتواصل:", phone)
                if (category == "مالي") {
                    ReviewRow("المبلغ المطلوب:", "$amount ج.س")
                }
                ReviewRow("المرفقات:", if (hasImage) "يوجد صورة/مستند" else "لا يوجد")
            }
        }

        Spacer(Modifier.height(32.dp))

        if (isUploading) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Brand600)
                Spacer(Modifier.height(8.dp))
                Text(uploadProgressText, color = Brand600)
            }
        } else {
            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Neutral900),
                    border = BorderStroke(1.dp, Neutral200)
                ) {
                    Text("رجوع")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إرسال الطلب", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Neutral500, fontSize = 12.sp)
        Text(value, color = Neutral900, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        HorizontalDivider(color = Neutral200, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Brand600,
    unfocusedBorderColor = Neutral200,
    focusedLabelColor = Brand600,
    unfocusedLabelColor = Neutral500,
    focusedTextColor = Neutral900,
    unfocusedTextColor = Neutral900,
    cursorColor = Brand600
)

