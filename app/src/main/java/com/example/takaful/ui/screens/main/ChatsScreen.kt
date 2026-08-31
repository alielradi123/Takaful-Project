package com.example.takaful.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takaful.data.repository.ChatThread
import com.example.takaful.ui.components.SoftHeaderCard
import com.example.takaful.ui.theme.*
import com.example.takaful.viewmodel.TakafulViewModel

@Composable
fun ChatsScreen(viewModel: TakafulViewModel) {
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    val chatId = selectedChatId // local val to allow smart cast

    if (chatId == null) {
        ChatListScreen(viewModel) { id ->
            selectedChatId = id
        }
    } else {
        ChatDetailScreen(viewModel, chatId) {
            selectedChatId = null
        }
    }
}

@Composable
fun ChatListScreen(viewModel: TakafulViewModel, onChatClick: (String) -> Unit) {
    val chatThreads by viewModel.chatThreads.collectAsState()
    val isLoadingChats by viewModel.isLoadingChats.collectAsState()

    // Initialize chat listener when entering this screen
    LaunchedEffect(Unit) {
        viewModel.initializeChats()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Column(Modifier.padding(18.dp)) {
                    SoftHeaderCard("المحادثات", "تواصل مع فريق تكافل ومتابعة حالاتك")
                }
            }

            if (isLoadingChats) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Brand600)
                    }
                }
            } else if (chatThreads.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد محادثات حالياً", style = MaterialTheme.typography.bodyLarge, color = Neutral500)
                    }
                }
            } else {
                items(chatThreads) { chat ->
                    ChatItemCard(chat) { onChatClick(chat.id) }
                }
            }
        }
    }
}

@Composable
private fun ChatItemCard(chat: ChatThread, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Brand600.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.SupportAgent, null, tint = Brand600)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(chat.participantName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(chat.timeFormatted, style = MaterialTheme.typography.bodySmall, color = Neutral500)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    chat.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
            if (chat.unreadCount > 0) {
                Spacer(Modifier.width(8.dp))
                Surface(color = SemanticError, shape = CircleShape, modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${chat.unreadCount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(viewModel: TakafulViewModel, chatId: String, onBack: () -> Unit) {
    val messages by viewModel.chatMessages.collectAsState()
    val chatThreads by viewModel.chatThreads.collectAsState()
    val currentChat = chatThreads.find { it.id == chatId }
    val currentUser = viewModel.currentUser
    var messageText by remember { mutableStateOf("") }
    
    LaunchedEffect(chatId) {
        viewModel.listenToMessages(chatId)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentChat?.participantName ?: "المحادثة", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("اكتب رسالتك...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendChatMessage(chatId, messageText)
                                    messageText = ""
                                }
                            },
                            modifier = Modifier.background(Brand600, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "إرسال", tint = Color.White)
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { msg ->
                    val isMine = msg.senderId == currentUser?.uid
                    ChatBubble(
                        msg = msg, 
                        isMine = isMine,
                        onDelete = { viewModel.deleteChatMessage(chatId, msg.id) },
                        onEdit = { newText -> viewModel.editChatMessage(chatId, msg.id, newText) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    msg: com.example.takaful.data.repository.ChatMessage,
    isMine: Boolean,
    onDelete: () -> Unit = {},
    onEdit: (String) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val bubbleColor = if (isMine) Brand600 else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isMine) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMine) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            // Support Avatar
            if (msg.senderPhotoUrl.isNotEmpty()) {
                AsyncImage(
                    model = msg.senderPhotoUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = msg.senderName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.Start else Alignment.End,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Surface(
                color = bubbleColor,
                shape = shape,
                shadowElevation = 1.dp,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (isMine) showMenu = true
                        }
                    )
                }
            ) {
                Box {
                    Text(
                        text = msg.text,
                        color = textColor,
                        modifier = Modifier.padding(12.dp)
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تعديل") },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = msg.timeFormatted,
                fontSize = 11.sp,
                color = Neutral400
            )
        }

        if (isMine) {
            Spacer(modifier = Modifier.width(8.dp))
            // My Avatar
            if (msg.senderPhotoUrl.isNotEmpty()) {
                AsyncImage(
                    model = msg.senderPhotoUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = msg.senderName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var editText by remember { mutableStateOf(msg.text) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("تعديل الرسالة") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editText.isNotBlank()) {
                        onEdit(editText)
                    }
                    showEditDialog = false
                }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
