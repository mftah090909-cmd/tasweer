package com.lumix.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumix.pro.gallery.MediaItem
import com.lumix.pro.gallery.MediaRepository
import com.lumix.pro.ui.theme.LumixCyan
import com.lumix.pro.ui.theme.LumixRed
import com.lumix.pro.ui.theme.LumixTextPrimary
import com.lumix.pro.ui.theme.LumixTextSecondary
import kotlinx.coroutines.launch

private val ALBUMS = listOf("All", "Photos", "Videos", "RAW", "Favorites", "Private")
private val ALBUMS_AR = listOf("الكل", "صور", "فيديو", "خام", "المفضلة", "خاص")

@Composable
fun GalleryScreen(
    repository: MediaRepository,
    isArabic: Boolean,
    onOpenDetail: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedAlbum by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    fun reload() {
        scope.launch {
            loading = true
            items = repository.loadAll(filterLumixOnly = false)
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val filtered = remember(items, selectedAlbum, searchQuery) {
        val base = when (selectedAlbum) {
            1 -> items.filter { !it.isVideo }
            2 -> items.filter { it.isVideo }
            3 -> items.filter { it.isRaw }
            4 -> items.filter { it.isFavorite }
            5 -> items.filter { it.isPrivateAlbum }
            else -> items.filter { !it.isPrivateAlbum }
        }
        if (searchQuery.isBlank()) base else base.filter {
            it.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0C10),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = LumixTextPrimary)
                    }
                    Text(
                        text = if (isArabic) "معرض لومكس" else "LUMIX GALLERY",
                        color = LumixTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = { reload() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = LumixCyan)
                    }
                }
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isArabic) "بحث بالاسم أو التاريخ…" else "Search by name or date…", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LumixCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = Color(0x33121722),
                        unfocusedContainerColor = Color(0x33121722),
                        focusedTextColor = LumixTextPrimary,
                        unfocusedTextColor = LumixTextPrimary
                    )
                )
                // Album chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val titles = if (isArabic) ALBUMS_AR else ALBUMS
                    titles.forEachIndexed { i, title ->
                        val active = i == selectedAlbum
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) LumixCyan else Color.White.copy(alpha = 0.08f))
                                .clickable { selectedAlbum = i }
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (active) Color.Black else LumixTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = LumixCyan)
            } else if (filtered.isEmpty()) {
                Text(
                    text = if (isArabic) "لا توجد عناصر بعد — التقط صورتك الأولى!" else "No media yet — take your first shot!",
                    color = LumixTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { item ->
                        GalleryCell(item = item, onClick = { onOpenDetail(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryCell(item: MediaItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.contentUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Video duration chip
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(item.durationLabel, color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
        // RAW badge
        if (item.isRaw) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LumixCyan)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("RAW", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
        // Favorite heart
        if (item.isFavorite) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = LumixRed,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }
    }
}
