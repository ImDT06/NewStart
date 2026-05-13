package com.example.newstart.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.newstart.R
import com.example.newstart.domain.model.User
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.LanguagePreviews
import com.example.newstart.ui.util.LanguagePickerDialog
import com.example.newstart.ui.util.SmallLanguageSwitcher

private val HomePrimary = Color(0xFF1D5FE2)
private val HomeBg = Color(0xFFF8FAFC)
private val CardBlue = Color(0xFFE0E7FF)
private val CardOrange = Color(0xFFFFF7ED)
private val CardGreen = Color(0xFFF0FDF4)
private val CardPurple = Color(0xFFFAF5FF)

data class Category(val titleRes: Int, val icon: ImageVector, val color: Color)
data class Course(val id: String, val title: String, val author: String, val progress: Int, val color: Color)

@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsState()
    
    HomeContent(
        user = userState,
        onNavigateToDetail = onNavigateToDetail,
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    user: User?,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguagePicker by remember { mutableStateOf(false) }

    val categories = listOf(
        Category(R.string.home_category_courses, Icons.AutoMirrored.Filled.MenuBook, CardBlue),
        Category(R.string.home_category_exams, Icons.AutoMirrored.Filled.Assignment, CardOrange),
        Category(R.string.home_category_library, Icons.Default.CollectionsBookmark, CardGreen),
        Category(R.string.home_category_community, Icons.Default.Groups, CardPurple)
    )

    val popularCourses = listOf(
        Course("1", "Advanced Kotlin", "Jane Doe", 0, Color(0xFF6366F1)),
        Course("2", "Jetpack Compose Pro", "John Smith", 0, Color(0xFFEC4899)),
        Course("3", "Android Architecture", "Alex Johnson", 0, Color(0xFF10B981))
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header Section
                item {
                    HomeHeader(
                        userName = user?.name ?: "Guest",
                        onLanguageClick = { showLanguagePicker = true }
                    )
                }

                // Search Bar
                item {
                    HomeSearchBar()
                }

                // Continue Learning / Progress Card
                item {
                    ProgressCard()
                }

                // Categories Grid
                item {
                    CategorySection(categories)
                }

                // Popular Courses
                item {
                    SectionHeader(titleRes = R.string.home_popular_courses)
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(popularCourses) { course ->
                            CourseCard(course = course, onClick = { onNavigateToDetail(course.id) })
                        }
                    }
                }
            }

            if (showLanguagePicker) {
                LanguagePickerDialog(onDismiss = { showLanguagePicker = false })
            }
        }
    }
}

@Composable
fun HomeHeader(
    userName: String,
    onLanguageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.home_hello, userName),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(id = R.string.home_welcome),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallLanguageSwitcher(onClick = onLanguageClick)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { /* Profile */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = HomePrimary)
            }
        }
    }
}

@Composable
fun HomeSearchBar() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.home_search_placeholder),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ProgressCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(24.dp),
        color = HomePrimary
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_continue_learning),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "UI/UX Design Fundamental",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { 0.65f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.home_progress, 65),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategorySection(categories: List<Category>) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                CategoryItem(category)
            }
        }
    }
}

@Composable
fun CategoryItem(category: Category) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(category.color)
                .clickable { /* Navigate */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(category.icon, contentDescription = null, tint = HomePrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = category.titleRes),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
    }
}

@Composable
fun SectionHeader(titleRes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = titleRes),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(id = R.string.home_see_all),
            fontSize = 14.sp,
            color = HomePrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { /* See All */ }
        )
    }
}

@Composable
fun CourseCard(course: Course, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(course.color)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = course.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = course.author,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@LanguagePreviews
@Composable
fun HomeScreenPreview() {
    NewStartTheme {
        HomeContent(
            user = User("1", "Hilt User", "hilt@example.com"),
            onNavigateToDetail = {}
        )
    }
}
