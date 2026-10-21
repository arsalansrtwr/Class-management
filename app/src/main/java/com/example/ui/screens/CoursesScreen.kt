package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Course
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GradeInfoBg
import com.example.ui.theme.ProfNavy
import com.example.ui.theme.ProfNavyDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ProfViewModel

@Composable
fun CoursesScreen(
    viewModel: ProfViewModel,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val teacherProfile by viewModel.teacherProfile.collectAsState()

    var showAddCourseDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Course & Section Directory",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfNavyDark
                        )
                        Text(
                            text = "Each department maintains distinct sections for quick access and streamlined oversight.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (courses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Class,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No courses created yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap the + button to add a course section for a department",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(courses, key = { it.id }) { course ->
                    val enrolledCount = allStudents.count { it.courseId == course.id }
                    CourseCard(
                        course = course,
                        enrolledCount = enrolledCount,
                        onDelete = { viewModel.deleteCourse(course.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        FloatingActionButton(
            onClick = { showAddCourseDialog = true },
            containerColor = ProfNavy,
            contentColor = PureWhite,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_course_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Course")
        }
    }

    if (showAddCourseDialog) {
        val suggestedDepts = (teacherProfile?.departments ?: emptyList()).ifEmpty {
            listOf("Computer Science", "Software Engineering", "Electrical Engineering")
        }
        AddCourseDialog(
            suggestedDepartments = suggestedDepts,
            onDismiss = { showAddCourseDialog = false },
            onConfirm = { code, title, dept, sec, credits, semester ->
                viewModel.addOrUpdateCourse(
                    courseCode = code,
                    courseTitle = title,
                    department = dept,
                    section = sec,
                    creditHours = credits,
                    semester = semester
                )
                showAddCourseDialog = false
            }
        )
    }
}

@Composable
fun CourseCard(
    course: Course,
    enrolledCount: Int,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GradeInfoBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = ProfNavy,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = course.courseCode,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = course.section,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ProfNavyDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = course.courseTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Text(
                    text = "Dept: ${course.department} • ${course.creditHours.toInt()} Credits • ${course.semester}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$enrolledCount Students Enrolled",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ProfNavy
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Course",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddCourseDialog(
    suggestedDepartments: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (code: String, title: String, dept: String, section: String, credits: Double, semester: String) -> Unit
) {
    var courseCode by remember { mutableStateOf("") }
    var courseTitle by remember { mutableStateOf("") }
    var department by remember { mutableStateOf(suggestedDepartments.firstOrNull() ?: "Computer Science") }
    var section by remember { mutableStateOf("Section A") }
    var creditsText by remember { mutableStateOf("3") }
    var semester by remember { mutableStateOf("Fall 2026") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Course Section",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ProfNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code (e.g. CS-301)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = courseTitle,
                    onValueChange = { courseTitle = it },
                    label = { Text("Course Title") },
                    placeholder = { Text("e.g. Data Structures & Algorithms") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Target Department") },
                    placeholder = { Text("e.g. Computer Science") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section Name (e.g. Section A)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = creditsText,
                        onValueChange = { creditsText = it },
                        label = { Text("Credits") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = semester,
                        onValueChange = { semester = it },
                        label = { Text("Semester") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courseCode.isBlank() || courseTitle.isBlank() || department.isBlank() || section.isBlank()) {
                        errorMessage = "Please complete all course fields."
                        return@Button
                    }
                    val credits = creditsText.toDoubleOrNull() ?: 3.0
                    onConfirm(courseCode, courseTitle, department, section, credits, semester)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfNavy)
            ) {
                Text("Create Section")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
