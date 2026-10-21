package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.ui.auth.signOutProfessor
import com.example.ui.components.SyncStatusChip
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GradeInfo
import com.example.ui.theme.GradeInfoBg
import com.example.ui.theme.GradeSuccess
import com.example.ui.theme.GradeSuccessBg
import com.example.ui.theme.ProfNavy
import com.example.ui.theme.ProfNavyDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ProfViewModel

enum class MainTab {
    GRADEBOOK,
    ATTENDANCE,
    ASSESSMENTS,
    REPORTS,
    COURSES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: ProfViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val teacherProfile by viewModel.teacherProfile.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(MainTab.GRADEBOOK) }
    var showMenu by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSeedConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    BackHandler(enabled = currentTab != MainTab.GRADEBOOK) {
        currentTab = MainTab.GRADEBOOK
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GradeInfoBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = ProfNavy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ProfGrade",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfNavyDark
                            )
                            Text(
                                text = teacherProfile?.let { "${it.name} • ${it.primaryDepartment}" } ?: "Academic Records",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    SyncStatusChip(isSyncing = isSyncing)

                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("app_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options Menu",
                            tint = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Seed Demonstration Data") },
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ProfNavy)
                            },
                            onClick = {
                                showMenu = false
                                showSeedConfirmDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Teaching Profile") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary)
                            },
                            onClick = {
                                showMenu = false
                                showEditProfileDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sign Out") },
                            leadingIcon = {
                                Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFDC2626))
                            },
                            onClick = {
                                showMenu = false
                                signOutProfessor(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onSignOut,
                                    scope = coroutineScope
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureWhite,
                    titleContentColor = ProfNavyDark
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = PureWhite,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.GRADEBOOK,
                    onClick = { currentTab = MainTab.GRADEBOOK },
                    icon = { Icon(Icons.Default.TableChart, contentDescription = "Gradebook") },
                    label = { Text("Gradebook", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ProfNavy,
                        selectedTextColor = ProfNavy,
                        indicatorColor = GradeInfoBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_gradebook")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.ATTENDANCE,
                    onClick = { currentTab = MainTab.ATTENDANCE },
                    icon = { Icon(Icons.Default.HowToReg, contentDescription = "Attendance") },
                    label = { Text("Attendance", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ProfNavy,
                        selectedTextColor = ProfNavy,
                        indicatorColor = GradeInfoBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_attendance")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.ASSESSMENTS,
                    onClick = { currentTab = MainTab.ASSESSMENTS },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Assessments") },
                    label = { Text("Assessments", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ProfNavy,
                        selectedTextColor = ProfNavy,
                        indicatorColor = GradeInfoBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_assessments")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.REPORTS,
                    onClick = { currentTab = MainTab.REPORTS },
                    icon = { Icon(Icons.Default.Grade, contentDescription = "Reports") },
                    label = { Text("Reports", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ProfNavy,
                        selectedTextColor = ProfNavy,
                        indicatorColor = GradeInfoBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_reports")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.COURSES,
                    onClick = { currentTab = MainTab.COURSES },
                    icon = { Icon(Icons.Default.Class, contentDescription = "Courses") },
                    label = { Text("Courses", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ProfNavy,
                        selectedTextColor = ProfNavy,
                        indicatorColor = GradeInfoBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_courses")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PureWhite
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                MainTab.GRADEBOOK -> GradebookScreen(viewModel = viewModel)
                MainTab.ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                MainTab.ASSESSMENTS -> AssessmentsScreen(viewModel = viewModel)
                MainTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                MainTab.COURSES -> CoursesScreen(viewModel = viewModel)
            }
        }
    }

    if (showSeedConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSeedConfirmDialog = false },
            title = {
                Text(
                    text = "Load Demonstration Records?",
                    fontWeight = FontWeight.Bold,
                    color = ProfNavy
                )
            },
            text = {
                Text(
                    text = "This will populate sample courses across Computer Science, Software Engineering, and Electrical Engineering, student records with 25-25-50 grade weights, and assessment papers. Perfect for instant testing and administrative review.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.seedDemoData(teacherProfile?.name ?: "Dr. Professor Vance")
                        showSeedConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfNavy)
                ) {
                    Text("Populate Demo Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSeedConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text("Edit Teaching Profile", fontWeight = FontWeight.Bold, color = ProfNavy)
            },
            text = {
                TeacherSetupScreen(
                    viewModel = viewModel,
                    existingProfile = teacherProfile,
                    onSetupComplete = { showEditProfileDialog = false }
                )
            },
            confirmButton = {}
        )
    }
}
