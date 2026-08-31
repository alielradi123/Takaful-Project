package com.example.takaful.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.data.model.CaseItem
import com.example.takaful.data.model.DonationRecord
import com.example.takaful.ui.theme.*
import java.io.File

@Composable
fun CaseCard(item: CaseItem, compact: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = if (compact) {
            modifier.width(280.dp).shadow(6.dp, RoundedCornerShape(24.dp))
        } else {
            modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp))
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            Text(item.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { item.progress },
                Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = Brand600,
                trackColor = Brand600.copy(alpha = 0.2f)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.amountFormatted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("${(item.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Brand600)
            }
        }
    }
}

@Composable
fun StatCard(value: String, label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier.height(110.dp).shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(28.dp), tint = color)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun DonationItemCard(donation: DonationRecord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp).shadow(4.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VolunteerActivism, null, tint = Brand600)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(donation.caseTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(donation.amountOrItem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Text(donation.status, color = Gold500, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun ListTile(icon: ImageVector, title: String, subtitle: String) {
    Card(
        Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Brand600.copy(alpha = 0.1f), shape = CircleShape) {
                Icon(icon, null, Modifier.padding(10.dp), tint = Brand600)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, color = Brand600) }
        }
    }
}

@Composable
fun CategoryChips(selected: String, onSelected: (String) -> Unit) {
    val cats = listOf("الكل", "مالي", "عيني", "طبي")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cats.forEach { cat ->
            val isSel = selected == cat
            Surface(
                modifier = Modifier.clickable { onSelected(cat) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSel) Brand600 else MaterialTheme.colorScheme.surface,
                shadowElevation = if (isSel) 4.dp else 1.dp
            ) {
                Text(
                    cat,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SoftHeaderCard(title: String, subtitle: String) {
    Card(
        Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Brand600)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(0.8f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftTopBar(title: String) {
    TopAppBar(
        title = { Text(title, color = Color.White) },
        colors = TopAppBarDefaults.topAppBarColors(Brand600)
    )
}

@Composable
fun HomeHeader(userName: String, photoURL: String, imagePath: String?) {
    val firstName = userName.split(" ").firstOrNull() ?: userName
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(Brand600, RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("أهلاً بك، $firstName 👋", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("نسعى لبناء جسور الخير بشفافية تامة", color = Color.White.copy(0.8f))
            }
            Surface(Modifier.size(50.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
                if (photoURL.isNotEmpty()) {
                    coil.compose.AsyncImage(
                        model = photoURL,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val file = imagePath?.let { File(it) }
                    if (file != null && file.exists()) {
                        val bitmap = remember(imagePath) { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
                        if (bitmap != null) {
                            Image(bitmap = bitmap, contentDescription = "Profile Picture", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp))
                        }
                    } else {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}
