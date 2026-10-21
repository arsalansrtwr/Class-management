package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import com.example.data.model.Assessment
import com.example.data.model.AssessmentType
import com.example.data.model.Course
import com.example.data.model.Score
import com.example.data.model.Student
import com.example.ui.theme.BorderMedium
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GradeDanger
import com.example.ui.theme.GradeInfo
import com.example.ui.theme.GradeInfoBg
import com.example.ui.theme.GradeSuccess
import com.example.ui.theme.GradeSuccessBg
import com.example.ui.theme.GradeWarning
import com.example.ui.theme.GradeWarningBg
import com.example.ui.theme.ProfNavy
import com.example.ui.theme.ProfNavyDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ProfViewModel

@Composable
fun AssessmentsScreen(
    viewModel: ProfViewModel,
    modifier: Modifier = Modifier
) {
    val assessments by viewModel.assessments.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val scores by viewModel.scores.collectAsState()
    val selectedCourseId by viewModel.selectedCourseId.collectAsState()

    var filterType by remember { mutableStateOf<AssessmentType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var gradingAssessment by remember { mutableStateOf<Assessment?>(null) }

    val filterOptions = listOf(
        "All" to null,
        "Assignments" to AssessmentType.ASSIGNMENT,
        "Quizzes" to AssessmentType.QUIZ,
        "Midterms" to AssessmentType.MIDTERM,
        "Final Exams" to AssessmentType.FINAL
    )

    val displayedAssessments = assessments.filter {
        (selectedCourseId == null || it.courseId == selectedCourseId) &&
        (filterType == null || it.type == filterType?.name)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Category Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = filterOptions.indexOfFirst { it.second == filterType }.coerceAtLeast(0),
                containerColor = PureWhite,
                contentColor = ProfNavy,
                edgePadding = 16.dp
            ) {
                filterOptions.forEachIndexed { index, (label, type) ->
                    Tab(
                        selected = filterType == type,
                        onClick = { filterType = type },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (filterType == type) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (displayedAssessments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No assessments created yet",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tap the + button to record assignments, quizzes, midterms, or finals",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(displayedAssessments, key = { it.id }) { assessment ->
                        val course = courses.find { it.id == assessment.courseId }
                        val courseStudents = allStudents.filter { it.courseId == assessment.courseId }
                        val assessmentScores = scores.filter { it.assessmentId == assessment.id }

                        AssessmentCard(
                            assessment = assessment,
                            course = course,
                            gradedCount = assessmentScores.size,
                            totalStudents = courseStudents.size,
                            onGradeClicked = { gradingAssessment = assessment },
                            onDeleteClicked = { viewModel.deleteAssessment(assessment.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = ProfNavy,
            contentColor = PureWhite,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_assessment_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Assessment")
        }
    }

    // Add Assessment Dialog
    if (showAddDialog) {
        AddAssessmentDialog(
            courses = courses,
            defaultCourseId = selectedCourseId,
            onDismiss = { showAddDialog = false },
            onConfirm = { courseId, title, type, maxMarks, weight, date ->
                viewModel.addOrUpdateAssessment(
                    courseId = courseId,
                    title = title,
                    type = type,
                    maxMarks = maxMarks,
                    weightPercentage = weight,
                    date = date
                )
                showAddDialog = false
            }
        )
    }

    // Grade Assessment & Student Submission Dialog
    gradingAssessment?.let { assessment ->
        val relevantStudents = allStudents.filter { it.courseId == assessment.courseId }
        val assessmentScores = scores.filter { it.assessmentId == assessment.id }

        GradeAssessmentDialog(
            assessment = assessment,
            students = relevantStudents,
            existingScores = assessmentScores,
            onDismiss = { gradingAssessment = null },
            onSaveScore = { studentId, obtainedMarks, notes ->
                viewModel.recordAssessmentScore(
                    courseId = assessment.courseId,
                    studentId = studentId,
                    assessmentId = assessment.id,
                    obtainedMarks = obtainedMarks,
                    notes = notes
                )
            },
            onBatchSaveScores = { scoresList ->
                viewModel.batchRecordAssessmentScores(
                    courseId = assessment.courseId,
                    assessmentId = assessment.id,
                    studentScores = scoresList
                )
            }
        )
    }
}

@Composable
fun AssessmentCard(
    assessment: Assessment,
    course: Course?,
    gradedCount: Int,
    totalStudents: Int,
    onGradeClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val (typeColor, typeBg) = when (assessment.type) {
        AssessmentType.ASSIGNMENT.name -> ProfNavy to GradeInfoBg
        AssessmentType.QUIZ.name -> Color(0xFF0F766E) to Color(0xFFCCFBF1)
        AssessmentType.MIDTERM.name -> Color(0xFFB45309) to Color(0xFFFEF3C7)
        AssessmentType.FINAL.name -> Color(0xFFB91C1C) to Color(0xFFFEE2E2)
        else -> ProfNavy to GradeInfoBg
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = assessment.type,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${assessment.maxMarks.toInt()} Marks (${assessment.weightPercentage.toInt()}% Weight)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onDeleteClicked,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Assessment",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = assessment.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (course != null) {
                Text(
                    text = "${course.courseCode} • ${course.department} (${course.section})",
                    fontSize = 12.sp,
                    color = ProfNavyDark,
                    fontWeight = FontWeight.Medium
                )
            }

            if (assessment.date.isNotBlank()) {
                Text(
                    text = "Scheduled: ${assessment.date}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Graded: $gradedCount of $totalStudents submissions",
                    fontSize = 12.sp,
                    color = if (gradedCount == totalStudents && totalStudents > 0) GradeSuccess else TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = onGradeClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = ProfNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Grade,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Grade Papers", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AddAssessmentDialog(
    courses: List<Course>,
    defaultCourseId: String?,
    onDismiss: () -> Unit,
    onConfirm: (courseId: String, title: String, type: AssessmentType, maxMarks: Double, weight: Double, date: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AssessmentType.ASSIGNMENT) }
    var selectedCourse by remember { mutableStateOf(courses.find { it.id == defaultCourseId } ?: courses.firstOrNull()) }
    var maxMarksText by remember { mutableStateOf("100") }
    var weightText by remember { mutableStateOf("25") }
    var dateText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Assessment / Exam Paper",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ProfNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Assessment Title") },
                    placeholder = { Text("e.g. Midterm Examination") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Assessment Type Selector
                Text(
                    text = "Assessment Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssessmentType.values().forEach { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ProfNavy else Color(0xFFF1F5F9))
                                .clickable {
                                    selectedType = type
                                    // Set default weights based on prompt formula
                                    weightText = when (type) {
                                        AssessmentType.ASSIGNMENT -> "10"
                                        AssessmentType.QUIZ -> "15"
                                        AssessmentType.MIDTERM -> "25"
                                        AssessmentType.FINAL -> "50"
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (type) {
                                    AssessmentType.ASSIGNMENT -> "Asg"
                                    AssessmentType.QUIZ -> "Quiz"
                                    AssessmentType.MIDTERM -> "Mid"
                                    AssessmentType.FINAL -> "Final"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PureWhite else TextPrimary
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = maxMarksText,
                        onValueChange = { maxMarksText = it },
                        label = { Text("Total Marks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Scheduled Date / Due Date") },
                    placeholder = { Text("e.g. 2026-11-15") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = GradeDanger,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorText = "Please enter an assessment title"
                        return@Button
                    }
                    val maxMarks = maxMarksText.toDoubleOrNull() ?: 100.0
                    val weight = weightText.toDoubleOrNull() ?: 25.0
                    val courseId = selectedCourse?.id ?: "general_course"
                    onConfirm(courseId, title, selectedType, maxMarks, weight, dateText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfNavy)
            ) {
                Text("Create Assessment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GradeAssessmentDialog(
    assessment: Assessment,
    students: List<Student>,
    existingScores: List<Score>,
    onDismiss: () -> Unit,
    onSaveScore: (studentId: String, marks: Double, notes: String) -> Unit,
    onBatchSaveScores: (List<Pair<String, Double>>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var deptFilter by remember { mutableStateOf<String?>(null) }
    var quickFillMarksText by remember { mutableStateOf("") }

    val departments = remember(students) {
        students.map { it.department }.filter { it.isNotBlank() }.distinct()
    }

    val marksMap = remember(students, existingScores) {
        mutableStateMapOf<String, String>().apply {
            students.forEach { s ->
                val score = existingScores.find { it.studentId == s.id }
                if (score != null) {
                    put(s.id, score.obtainedMarks.toString())
                }
            }
        }
    }

    val displayedStudents = remember(students, searchQuery, deptFilter) {
        students.filter { s ->
            (deptFilter == null || s.department.equals(deptFilter, ignoreCase = true)) &&
            (searchQuery.isBlank() || s.name.contains(searchQuery, ignoreCase = true) || s.rollNumber.contains(searchQuery, ignoreCase = true))
        }
    }

    val gradedCount = marksMap.values.count { it.isNotBlank() && it.toDoubleOrNull() != null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grade: ${assessment.title}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfNavy
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GradeInfoBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$gradedCount/${students.size} Graded",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfNavyDark
                        )
                    }
                }
                Text(
                    text = "Max Marks: ${assessment.maxMarks.toInt()} • Category: ${assessment.type} • Supports 100+ Cohorts",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search Field for 100+ Students
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search roll no or student name...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProfNavy,
                        unfocusedBorderColor = BorderMedium,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    )
                )

                // Department filter chips
                if (departments.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (deptFilter == null) ProfNavy else Color(0xFFF1F5F9))
                                .clickable { deptFilter = null }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("All (${students.size})", fontSize = 11.sp, color = if (deptFilter == null) PureWhite else TextPrimary)
                        }
                        departments.forEach { dept ->
                            val isSel = deptFilter.equals(dept, ignoreCase = true)
                            val count = students.count { it.department.equals(dept, ignoreCase = true) }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) ProfNavy else Color(0xFFF1F5F9))
                                    .clickable { deptFilter = if (isSel) null else dept }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("$dept ($count)", fontSize = 11.sp, color = if (isSel) PureWhite else TextPrimary)
                            }
                        }
                    }
                }

                // Quick Fill Empty helper for large batches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickFillMarksText,
                        onValueChange = { quickFillMarksText = it },
                        placeholder = { Text("e.g. ${assessment.maxMarks.toInt() * 3 / 4}") },
                        label = { Text("Quick Fill") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            val marks = quickFillMarksText.toDoubleOrNull()
                            if (marks != null) {
                                displayedStudents.forEach { s ->
                                    if (marksMap[s.id].isNullOrBlank()) {
                                        marksMap[s.id] = marks.toString()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Fill Empty", fontSize = 11.sp)
                    }
                }

                // Student List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (displayedStudents.isEmpty()) {
                        item {
                            Text(
                                text = if (students.isEmpty()) "No enrolled students in this course yet." else "No students match this search.",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(displayedStudents, key = { it.id }) { student ->
                            val currentMarks = marksMap[student.id] ?: ""

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(
                                            text = student.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${student.rollNumber} • ${student.department}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    OutlinedTextField(
                                        value = currentMarks,
                                        onValueChange = { marksMap[student.id] = it },
                                        placeholder = { Text("0-${assessment.maxMarks.toInt()}") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.width(90.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val scoresToSave = mutableListOf<Pair<String, Double>>()
                        marksMap.forEach { (studentId, marksStr) ->
                            val marks = marksStr.toDoubleOrNull()
                            if (marks != null) {
                                scoresToSave.add(studentId to marks)
                            }
                        }
                        if (scoresToSave.isNotEmpty()) {
                            onBatchSaveScores(scoresToSave)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfNavy)
                ) {
                    Text("Save All Marks & Sync", fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}
