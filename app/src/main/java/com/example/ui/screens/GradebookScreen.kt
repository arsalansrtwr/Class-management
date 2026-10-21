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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComputedGrade
import com.example.data.model.Course
import com.example.data.model.Student
import com.example.ui.components.GradeBadge
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.BorderMedium
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GradeDanger
import com.example.ui.theme.GradeDangerBg
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

enum class StudentSortOrder(val label: String) {
    ROLL_ASC("Roll No ↑"),
    ROLL_DESC("Roll No ↓"),
    NAME_ASC("Name A-Z"),
    GRADE_DESC("Grade Highest"),
    GRADE_ASC("Grade Lowest")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradebookScreen(
    viewModel: ProfViewModel,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val teacherProfile by viewModel.teacherProfile.collectAsState()
    val selectedCourseId by viewModel.selectedCourseId.collectAsState()
    val selectedDepartment by viewModel.selectedDepartment.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()
    val filteredStudentsWithGrades by viewModel.filteredStudents.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showBatchEnrollDialog by remember { mutableStateOf(false) }
    var batchInitialDeptIndex by remember { mutableIntStateOf(0) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }

    // Unique departments from teacher profile, courses, and students
    val departments = remember(courses, allStudents, teacherProfile) {
        val list = mutableListOf<String>()
        teacherProfile?.departments?.let { list.addAll(it) }
        courses.map { it.department }.let { list.addAll(it) }
        allStudents.map { it.department }.let { list.addAll(it) }
        val distinct = list.filter { it.isNotBlank() }.distinct()
        if (distinct.isEmpty()) {
            listOf("Computer Science", "Software Engineering", "Electronics Engineering")
        } else distinct
    }

    var sortOrder by remember { mutableStateOf(StudentSortOrder.ROLL_ASC) }
    var expandedDepartments by remember(departments) { mutableStateOf(departments.toSet()) }

    // Summary calculations
    val totalCount = filteredStudentsWithGrades.size
    val averageScore = if (totalCount > 0) {
        filteredStudentsWithGrades.map { it.second.totalPercentage }.average()
    } else 0.0
    val passCount = filteredStudentsWithGrades.count { it.second.isPassing }
    val passRate = if (totalCount > 0) (passCount.toDouble() / totalCount) * 100.0 else 0.0

    // Filter by search query if any
    val filteredList = if (searchQuery.isBlank()) {
        filteredStudentsWithGrades
    } else {
        filteredStudentsWithGrades.filter {
            it.first.name.contains(searchQuery, ignoreCase = true) ||
            it.first.rollNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    // Sort order handling for high student counts (100+)
    val displayedList = remember(filteredList, sortOrder) {
        when (sortOrder) {
            StudentSortOrder.ROLL_ASC -> filteredList.sortedWith(
                compareBy<Pair<Student, ComputedGrade>> { it.first.rollNumber.length }
                    .thenBy { it.first.rollNumber }
            )
            StudentSortOrder.ROLL_DESC -> filteredList.sortedWith(
                compareByDescending<Pair<Student, ComputedGrade>> { it.first.rollNumber.length }
                    .thenByDescending { it.first.rollNumber }
            )
            StudentSortOrder.NAME_ASC -> filteredList.sortedBy { it.first.name.lowercase() }
            StudentSortOrder.GRADE_DESC -> filteredList.sortedByDescending { it.second.totalPercentage }
            StudentSortOrder.GRADE_ASC -> filteredList.sortedBy { it.second.totalPercentage }
        }
    }

    // Grouping students department-wise so all CS students stay together, followed by SE, Electronics, etc.
    val groupedByDepartment = remember(displayedList, departments) {
        val map = displayedList.groupBy { it.first.department }
        val orderedMap = linkedMapOf<String, List<Pair<Student, ComputedGrade>>>()
        departments.forEach { dept ->
            map[dept]?.let { orderedMap[dept] = it }
        }
        map.forEach { (dept, list) ->
            if (!orderedMap.containsKey(dept)) {
                orderedMap[dept] = list
            }
        }
        orderedMap
    }

    Box(modifier = modifier.fillMaxSize().background(PureWhite)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Grading Formula Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GradeInfoBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GradeInfo.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GradeInfo,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Grade Formula: 25% Asg/Quizzes + 25% Midterm + 50% Final",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfNavyDark
                            )
                            Text(
                                text = "Computed automatically per student and department in real-time.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Stat Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Enrolled",
                        value = "$totalCount",
                        subtitle = "${departments.size} Departments",
                        icon = Icons.Default.Person,
                        iconColor = ProfNavy,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Class Avg",
                        value = "${String.format("%.1f", averageScore)}%",
                        subtitle = if (averageScore >= 50) "Passing" else "Needs Attention",
                        icon = Icons.Default.Grade,
                        iconColor = if (averageScore >= 70) GradeSuccess else GradeWarning,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Pass Rate",
                        value = "${String.format("%.0f", passRate)}%",
                        subtitle = "$passCount Passed",
                        icon = Icons.Default.School,
                        iconColor = if (passRate >= 75) GradeSuccess else GradeDanger,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Department-Wise Batch Enrollment Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GradeInfoBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    tint = ProfNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Department-by-Department Enrollment",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfNavyDark
                                )
                                Text(
                                    text = "Add all CS students, then all Software Eng, then Electronics in one bar flow",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                batchInitialDeptIndex = 0
                                showBatchEnrollDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("batch_enroll_btn")
                        ) {
                            Text("Enroll Batch", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Department & Section Filtering Chips
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Filter by Department & Section",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAllSelected = selectedDepartment == null && selectedCourseId == null
                        FilterChipItem(
                            label = "All Departments",
                            isSelected = isAllSelected,
                            onClick = {
                                viewModel.selectDepartment(null)
                                viewModel.selectCourse(null)
                                viewModel.selectSection(null)
                            }
                        )

                        departments.forEach { dept ->
                            val isDeptSelected = selectedDepartment.equals(dept, ignoreCase = true)
                            FilterChipItem(
                                label = dept,
                                isSelected = isDeptSelected,
                                onClick = {
                                    if (isDeptSelected) {
                                        viewModel.selectDepartment(null)
                                    } else {
                                        viewModel.selectDepartment(dept)
                                    }
                                }
                            )
                        }
                    }

                    if (courses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            courses.forEach { course ->
                                val isCourseSelected = selectedCourseId == course.id
                                FilterChipItem(
                                    label = "${course.courseCode} - ${course.section} (${course.department})",
                                    isSelected = isCourseSelected,
                                    onClick = {
                                        if (isCourseSelected) {
                                            viewModel.selectCourse(null)
                                        } else {
                                            viewModel.selectCourse(course.id)
                                            viewModel.selectDepartment(course.department)
                                            viewModel.selectSection(course.section)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search student by name or roll number...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProfNavy,
                        unfocusedBorderColor = BorderMedium,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    )
                )
            }

            // Controls Row: Count indicator, Sort Order toggle, and Collapse/Expand All
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Students: ${displayedList.size} of $totalCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allExpanded = departments.all { expandedDepartments.contains(it) }
                        TextButton(
                            onClick = {
                                expandedDepartments = if (allExpanded) emptySet() else departments.toSet()
                            }
                        ) {
                            Icon(
                                imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ProfNavy
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (allExpanded) "Collapse All" else "Expand All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfNavy
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                sortOrder = when (sortOrder) {
                                    StudentSortOrder.ROLL_ASC -> StudentSortOrder.ROLL_DESC
                                    StudentSortOrder.ROLL_DESC -> StudentSortOrder.NAME_ASC
                                    StudentSortOrder.NAME_ASC -> StudentSortOrder.GRADE_DESC
                                    StudentSortOrder.GRADE_DESC -> StudentSortOrder.GRADE_ASC
                                    StudentSortOrder.GRADE_ASC -> StudentSortOrder.ROLL_ASC
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = ProfNavyDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sortOrder.label,
                                fontSize = 11.sp,
                                color = ProfNavyDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Department-Wise Structured Student Roster
            if (displayedList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (allStudents.isEmpty()) "No students enrolled yet" else "No students match this filter",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (allStudents.isEmpty()) "Tap 'Enroll Batch' above to add all CS students, then Software Eng, then Electronics!" else "Try clearing department or search filter",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Grouped Department Display with Expand/Collapse capability
                groupedByDepartment.forEach { (deptName, studentList) ->
                    val deptAvg = if (studentList.isNotEmpty()) {
                        studentList.map { it.second.totalPercentage }.average()
                    } else 0.0
                    val isExpanded = expandedDepartments.contains(deptName)

                    item(key = "dept_header_$deptName") {
                        DepartmentBarHeader(
                            departmentName = deptName,
                            studentCount = studentList.size,
                            averageGrade = deptAvg,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedDepartments = if (isExpanded) {
                                    expandedDepartments - deptName
                                } else {
                                    expandedDepartments + deptName
                                }
                            },
                            onAddStudents = {
                                val index = departments.indexOf(deptName)
                                batchInitialDeptIndex = if (index >= 0) index else 0
                                showBatchEnrollDialog = true
                            }
                        )
                    }

                    if (isExpanded) {
                        items(studentList, key = { it.first.id }) { (student, grade) ->
                            StudentGradeCard(
                                student = student,
                                grade = grade,
                                onEditClicked = { editingStudent = student }
                            )
                        }
                    } else {
                        item(key = "dept_collapsed_$deptName") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedDepartments = expandedDepartments + deptName
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📁 ${studentList.size} students collapsed",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Tap to view roster ▼",
                                        fontSize = 11.sp,
                                        color = ProfNavy,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item(key = "dept_spacer_$deptName") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button to Add Individual Student
        FloatingActionButton(
            onClick = { showAddStudentDialog = true },
            containerColor = ProfNavy,
            contentColor = PureWhite,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_student_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Student")
        }
    }

    // Sequential Department Batch Enrollment Dialog
    if (showBatchEnrollDialog) {
        DepartmentBatchEnrollmentDialog(
            departments = departments,
            initialIndex = batchInitialDeptIndex,
            courses = courses,
            onDismiss = { showBatchEnrollDialog = false },
            onSaveDepartmentStudents = { dept, section, courseId, studentsData ->
                viewModel.batchAddStudents(
                    department = dept,
                    section = section,
                    courseId = courseId,
                    studentsData = studentsData
                )
            }
        )
    }

    // Individual Add Student Dialog
    if (showAddStudentDialog) {
        AddStudentDialog(
            courses = courses,
            departments = departments,
            defaultCourseId = selectedCourseId,
            defaultDept = selectedDepartment,
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { courseId, dept, sec, roll, name, email ->
                viewModel.addOrUpdateStudent(
                    courseId = courseId,
                    department = dept,
                    section = sec,
                    rollNumber = roll,
                    name = name,
                    email = email
                )
                showAddStudentDialog = false
            }
        )
    }

    // Edit Student Scores Dialog
    editingStudent?.let { student ->
        EditScoresDialog(
            student = student,
            onDismiss = { editingStudent = null },
            onConfirm = { asgQuiz, mid, fin ->
                viewModel.updateStudentScores(student, asgQuiz, mid, fin)
                editingStudent = null
            }
        )
    }
}

@Composable
fun DepartmentBarHeader(
    departmentName: String,
    studentCount: Int,
    averageGrade: Double,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddStudents: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(containerColor = ProfNavy),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.CorporateFare,
                    contentDescription = null,
                    tint = PureWhite.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = departmentName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PureWhite.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (studentCount >= 100) "$studentCount Students (100+)" else "$studentCount Students",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PureWhite)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Avg: ${String.format("%.1f", averageGrade)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfNavyDark
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onAddStudents,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add students to $departmentName",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ================= Department-by-Department Sequential Batch Enrollment Dialog =================

data class StudentRowEntry(
    var rollNumber: String = "",
    var name: String = "",
    var email: String = ""
)

@Composable
fun DepartmentBatchEnrollmentDialog(
    departments: List<String>,
    initialIndex: Int = 0,
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSaveDepartmentStudents: (dept: String, section: String, courseId: String, students: List<Triple<String, String, String>>) -> Unit
) {
    var currentDeptIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, (departments.size - 1).coerceAtLeast(0))) }
    val currentDept = departments.getOrElse(currentDeptIndex) { "Computer Science" }
    val nextDept = if (currentDeptIndex + 1 < departments.size) departments[currentDeptIndex + 1] else null

    // Find course associated with this department or first available
    val matchingCourse = courses.find { it.department.equals(currentDept, ignoreCase = true) } ?: courses.firstOrNull()
    var selectedSection by remember(currentDept) { mutableStateOf(matchingCourse?.section ?: "Section A") }

    // 0: Auto Generator (100+ students), 1: Paste Roster / CSV, 2: Row by Row
    var inputModeTabIndex by remember { mutableIntStateOf(0) }

    // Auto Generator State (Specially built for departments with 100+ students)
    val defaultPrefix = when {
        currentDept.contains("Computer", ignoreCase = true) -> "CS-2024-"
        currentDept.contains("Software", ignoreCase = true) -> "SE-2024-"
        currentDept.contains("Electr", ignoreCase = true) -> "EE-2024-"
        else -> "${currentDept.take(3).uppercase()}-2024-"
    }
    var genPrefix by remember(currentDept) { mutableStateOf(defaultPrefix) }
    var genCountText by remember(currentDept) { mutableStateOf("120") }
    var genStartNumText by remember(currentDept) { mutableStateOf("1") }
    var genNamePrefix by remember(currentDept) {
        mutableStateOf(
            when {
                currentDept.contains("Computer", ignoreCase = true) -> "CS Scholar"
                currentDept.contains("Software", ignoreCase = true) -> "SE Scholar"
                currentDept.contains("Electr", ignoreCase = true) -> "EE Scholar"
                else -> "Student"
            }
        )
    }

    // Quick Rows State
    val quickRows = remember(currentDept) {
        mutableStateListOf(
            StudentRowEntry(rollNumber = "${defaultPrefix}001", name = ""),
            StudentRowEntry(rollNumber = "${defaultPrefix}002", name = ""),
            StudentRowEntry(rollNumber = "${defaultPrefix}003", name = "")
        )
    }

    // Paste Roster State
    var pasteText by remember(currentDept) {
        mutableStateOf(
            when {
                currentDept.contains("Computer", ignoreCase = true) ->
                    "CS-2024-001, Alex Brooks, alex@cs.edu\nCS-2024-002, Brenda Vance, brenda@cs.edu\nCS-2024-003, Carlos Gomez, carlos@cs.edu"
                currentDept.contains("Software", ignoreCase = true) ->
                    "SE-2024-001, David Kim, david@se.edu\nSE-2024-002, Elena Rostova, elena@se.edu\nSE-2024-003, Farhan Ali, farhan@se.edu"
                currentDept.contains("Electr", ignoreCase = true) ->
                    "EE-2024-001, Grace Hopper, grace@ee.edu\nEE-2024-002, Henry Ford, henry@ee.edu\nEE-2024-003, Ian Bell, ian@ee.edu"
                else ->
                    "STU-001, Sample Student 1\nSTU-002, Sample Student 2"
            }
        )
    }

    var validationMsg by remember { mutableStateOf<String?>(null) }
    var savedDeptNotification by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Department Batch Enrollment",
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
                            text = "Step ${currentDeptIndex + 1} of ${departments.size}: $currentDept",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfNavyDark
                        )
                    }
                }
                Text(
                    text = "Add and save all students for $currentDept (supports 100+ students), then proceed sequentially.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Department Step Indicator Tabs
                ScrollableTabRow(
                    selectedTabIndex = currentDeptIndex,
                    containerColor = Color(0xFFF8FAFC),
                    contentColor = ProfNavy,
                    edgePadding = 8.dp
                ) {
                    departments.forEachIndexed { idx, dept ->
                        Tab(
                            selected = currentDeptIndex == idx,
                            onClick = {
                                currentDeptIndex = idx
                                validationMsg = null
                            },
                            text = {
                                Text(
                                    text = "${idx + 1}. $dept",
                                    fontSize = 12.sp,
                                    fontWeight = if (currentDeptIndex == idx) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                if (savedDeptNotification != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GradeSuccessBg)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GradeSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = savedDeptNotification ?: "", fontSize = 12.sp, color = GradeSuccess, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Section & Course Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedSection,
                        onValueChange = { selectedSection = it },
                        label = { Text("Target Section") },
                        placeholder = { Text("e.g. Section A") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfNavy,
                            unfocusedBorderColor = BorderMedium,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )
                    matchingCourse?.let { c ->
                        Text(
                            text = "Course: ${c.courseCode}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ProfNavyDark
                        )
                    }
                }

                // Input Mode Tabs: Auto Generator (100+), Paste Roster / CSV, Row by Row
                TabRow(
                    selectedTabIndex = inputModeTabIndex,
                    containerColor = PureWhite,
                    contentColor = ProfNavy
                ) {
                    Tab(
                        selected = inputModeTabIndex == 0,
                        onClick = { inputModeTabIndex = 0 },
                        text = { Text("Auto 100+ Gen", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = inputModeTabIndex == 1,
                        onClick = { inputModeTabIndex = 1 },
                        text = { Text("Paste / CSV", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = inputModeTabIndex == 2,
                        onClick = { inputModeTabIndex = 2 },
                        text = { Text("Row by Row", fontSize = 11.sp) }
                    )
                }

                if (inputModeTabIndex == 0) {
                    // Auto-Generate 100+ Students Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "High-Capacity Roster Generator (100+ Students)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfNavyDark
                            )
                            Text(
                                text = "Instantly generates a full departmental cohort with sequentially numbered roll numbers and names for $currentDept.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = genPrefix,
                                    onValueChange = { genPrefix = it },
                                    label = { Text("Roll Prefix") },
                                    placeholder = { Text("e.g. CS-2024-") },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = genCountText,
                                    onValueChange = { genCountText = it },
                                    label = { Text("Total Students") },
                                    placeholder = { Text("120") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            // Quick preset chips for student counts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Presets:", fontSize = 11.sp, color = TextMuted)
                                listOf("60", "100", "120", "150").forEach { preset ->
                                    val isSelected = genCountText == preset
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) ProfNavy else PureWhite)
                                            .border(1.dp, if (isSelected) ProfNavy else BorderMedium, RoundedCornerShape(6.dp))
                                            .clickable { genCountText = preset }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "$preset Students",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PureWhite else TextPrimary
                                        )
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = genStartNumText,
                                    onValueChange = { genStartNumText = it },
                                    label = { Text("Start Number") },
                                    placeholder = { Text("1") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = genNamePrefix,
                                    onValueChange = { genNamePrefix = it },
                                    label = { Text("Name Pattern") },
                                    placeholder = { Text("CS Scholar") },
                                    modifier = Modifier.weight(1.4f),
                                    singleLine = true
                                )
                            }

                            val countInt = genCountText.toIntOrNull() ?: 0
                            val startInt = genStartNumText.toIntOrNull() ?: 1
                            val endInt = startInt + countInt - 1
                            if (countInt > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GradeSuccessBg)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "✓ Ready: $countInt students from ${genPrefix}${String.format("%03d", startInt)} to ${genPrefix}${String.format("%03d", endInt)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GradeSuccess
                                    )
                                }
                            }
                        }
                    }
                } else if (inputModeTabIndex == 1) {
                    // Paste Roster Text Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paste student list (Roll Number, Name, Email):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )

                        // 1-Click Load 100 Sample Students
                        TextButton(
                            onClick = {
                                val prefix = defaultPrefix
                                val buffer = StringBuilder()
                                for (i in 1..100) {
                                    val roll = "$prefix${String.format("%03d", i)}"
                                    val name = "$currentDept Student ${String.format("%03d", i)}"
                                    val email = "student${String.format("%03d", i)}@university.edu"
                                    buffer.append("$roll, $name, $email\n")
                                }
                                pasteText = buffer.toString().trim()
                            }
                        ) {
                            Text("Load 100 Students", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfNavy)
                        }
                    }

                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        placeholder = { Text("e.g.\nCS-2024-001, John Doe, john@cs.edu\nCS-2024-002, Jane Smith") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 10,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfNavy,
                            unfocusedBorderColor = BorderMedium,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )

                    val detectedCount = pasteText.lines().count { it.isNotBlank() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (detectedCount > 0) GradeInfoBg else Color(0xFFF1F5F9))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (detectedCount >= 100) "✓ $detectedCount students detected (100+ cohort ready)" else "$detectedCount lines detected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (detectedCount > 0) ProfNavyDark else TextMuted
                        )
                    }
                } else {
                    // Quick Rows Entry
                    Text(
                        text = "Enter roll numbers and names for $currentDept:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    quickRows.forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = row.rollNumber,
                                onValueChange = { row.rollNumber = it },
                                placeholder = { Text("Roll No") },
                                modifier = Modifier.weight(1.1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = row.name,
                                onValueChange = { row.name = it },
                                placeholder = { Text("Student Name") },
                                modifier = Modifier.weight(1.8f),
                                singleLine = true
                            )
                            IconButton(
                                onClick = { if (quickRows.size > 1) quickRows.removeAt(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove row", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val nextNum = quickRows.size + 1
                                quickRows.add(StudentRowEntry(rollNumber = "${defaultPrefix}${String.format("%03d", nextNum)}", name = ""))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ 1 Row", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val currentCount = quickRows.size
                                for (i in 1..10) {
                                    val nextNum = currentCount + i
                                    quickRows.add(StudentRowEntry(rollNumber = "${defaultPrefix}${String.format("%03d", nextNum)}", name = ""))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ 10 Rows", fontSize = 11.sp)
                        }
                    }
                }

                if (validationMsg != null) {
                    Text(
                        text = validationMsg ?: "",
                        color = GradeDanger,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val studentsToSave = mutableListOf<Triple<String, String, String>>()

                    if (inputModeTabIndex == 0) {
                        val count = genCountText.toIntOrNull() ?: 0
                        val start = genStartNumText.toIntOrNull() ?: 1
                        if (count <= 0) {
                            validationMsg = "Please enter a valid student count (e.g. 100 or 120)"
                            return@Button
                        }
                        for (i in start until (start + count)) {
                            val roll = "${genPrefix}${String.format("%03d", i)}"
                            val name = "${genNamePrefix.ifBlank { "Student" }} ${String.format("%03d", i)}"
                            val email = "${roll.lowercase().replace("-", "")}@university.edu"
                            studentsToSave.add(Triple(roll, name, email))
                        }
                    } else if (inputModeTabIndex == 1) {
                        pasteText.lines().map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
                            val parts = line.split(",", "\t", ";").map { it.trim() }
                            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                                val roll = parts[0]
                                val name = if (parts.size > 1 && parts[1].isNotBlank()) parts[1] else "Student $roll"
                                val email = if (parts.size > 2) parts[2] else ""
                                studentsToSave.add(Triple(roll, name, email))
                            }
                        }
                    } else {
                        quickRows.filter { it.rollNumber.isNotBlank() }.forEach {
                            val roll = it.rollNumber.trim()
                            val name = if (it.name.isNotBlank()) it.name.trim() else "Student $roll"
                            studentsToSave.add(Triple(roll, name, it.email.trim()))
                        }
                    }

                    if (studentsToSave.isEmpty()) {
                        validationMsg = "Please enter or generate at least one student for $currentDept"
                        return@Button
                    }

                    validationMsg = null
                    val courseId = matchingCourse?.id ?: "general_course"
                    val sec = selectedSection.ifBlank { "Section A" }

                    // Save this department's batch of students (handled in safe chunks of 400 in repository)
                    onSaveDepartmentStudents(currentDept, sec, courseId, studentsToSave)

                    if (nextDept != null) {
                        savedDeptNotification = "Saved ${studentsToSave.size} students for $currentDept! Now enrolling $nextDept."
                        currentDeptIndex++
                    } else {
                        // All departments completed
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfNavy),
                modifier = Modifier.testTag("save_and_next_dept_btn")
            ) {
                if (nextDept != null) {
                    Text("Save $currentDept & Next ($nextDept) →", fontSize = 12.sp)
                } else {
                    Text("Save All & Finish Enrollment ✓", fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            Row {
                if (nextDept != null) {
                    TextButton(
                        onClick = {
                            currentDeptIndex++
                            validationMsg = null
                        }
                    ) {
                        Text("Skip $currentDept", fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", fontSize = 12.sp)
                }
            }
        }
    )
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ProfNavy else Color(0xFFF1F5F9))
            .border(
                1.dp,
                if (isSelected) ProfNavy else BorderSubtle,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PureWhite else TextPrimary
        )
    }
}

@Composable
fun StudentGradeCard(
    student: Student,
    grade: ComputedGrade,
    onEditClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_card_${student.rollNumber}"),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = student.rollNumber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ProfNavy
                        )
                        Text(
                            text = " • ${student.department} (${student.section})",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                GradeBadge(letterGrade = grade.letterGrade, percentage = grade.totalPercentage)

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onEditClicked,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Scores",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Marks Breakdown: 25% Asg/Quiz + 25% Midterm + 50% Final
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ScoreBreakdownColumn(
                    label = "Asg & Quiz (25%)",
                    rawScore = student.assignmentQuizTotal,
                    weightedScore = grade.assignmentQuizContribution,
                    maxWeight = 25.0
                )
                ScoreBreakdownColumn(
                    label = "Midterm (25%)",
                    rawScore = student.midtermScore,
                    weightedScore = grade.midtermContribution,
                    maxWeight = 25.0
                )
                ScoreBreakdownColumn(
                    label = "Final Exam (50%)",
                    rawScore = student.finalTermScore,
                    weightedScore = grade.finalContribution,
                    maxWeight = 50.0
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attendance footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val attRate = if (student.totalAttendancePossible > 0) {
                    (student.totalAttendanceCount.toDouble() / student.totalAttendancePossible) * 100.0
                } else 100.0

                Text(
                    text = "Attendance: ${student.totalAttendanceCount}/${student.totalAttendancePossible} (${String.format("%.0f", attRate)}%)",
                    fontSize = 11.sp,
                    color = if (attRate < 75.0) GradeDanger else TextSecondary,
                    fontWeight = if (attRate < 75.0) FontWeight.Bold else FontWeight.Normal
                )

                Text(
                    text = if (grade.isPassing) "Passing (GPA ${grade.gpa})" else "At-Risk (Fail)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (grade.isPassing) GradeSuccess else GradeDanger
                )
            }
        }
    }
}

@Composable
private fun ScoreBreakdownColumn(
    label: String,
    rawScore: Double,
    weightedScore: Double,
    maxWeight: Double
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${String.format("%.1f", weightedScore)} / $maxWeight",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ProfNavyDark
        )
        Text(
            text = "${String.format("%.0f", rawScore)}/100",
            fontSize = 10.sp,
            color = TextMuted
        )
    }
}

@Composable
fun EditScoresDialog(
    student: Student,
    onDismiss: () -> Unit,
    onConfirm: (asgQuiz: Double, midterm: Double, final: Double) -> Unit
) {
    var asgQuizText by remember { mutableStateOf(student.assignmentQuizTotal.toString()) }
    var midText by remember { mutableStateOf(student.midtermScore.toString()) }
    var finalText by remember { mutableStateOf(student.finalTermScore.toString()) }

    val asgQuizVal = asgQuizText.toDoubleOrNull() ?: 0.0
    val midVal = midText.toDoubleOrNull() ?: 0.0
    val finalVal = finalText.toDoubleOrNull() ?: 0.0

    val previewGrade = ComputedGrade.calculate(asgQuizVal, midVal, finalVal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Update Marks: ${student.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProfNavy
                )
                Text(
                    text = "${student.rollNumber} • ${student.department}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GradeInfoBg)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Grade: ${String.format("%.1f", previewGrade.totalPercentage)}%",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfNavyDark
                            )
                            Text(
                                text = "Formula: (Asg * 0.25) + (Mid * 0.25) + (Final * 0.50)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        GradeBadge(previewGrade.letterGrade, previewGrade.totalPercentage)
                    }
                }

                OutlinedTextField(
                    value = asgQuizText,
                    onValueChange = { asgQuizText = it },
                    label = { Text("Assignments & Quizzes Score (0 - 100)") },
                    supportingText = { Text("25% Weight: ${(asgQuizVal * 0.25).coerceIn(0.0, 25.0)} pts") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = midText,
                    onValueChange = { midText = it },
                    label = { Text("Midterm Marks (0 - 100)") },
                    supportingText = { Text("25% Weight: ${(midVal * 0.25).coerceIn(0.0, 25.0)} pts") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = finalText,
                    onValueChange = { finalText = it },
                    label = { Text("Final Term Exam Marks (0 - 100)") },
                    supportingText = { Text("50% Weight: ${(finalVal * 0.50).coerceIn(0.0, 50.0)} pts") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(asgQuizVal, midVal, finalVal) },
                colors = ButtonDefaults.buttonColors(containerColor = ProfNavy)
            ) {
                Text("Save & Sync")
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
fun AddStudentDialog(
    courses: List<Course>,
    departments: List<String>,
    defaultCourseId: String?,
    defaultDept: String?,
    onDismiss: () -> Unit,
    onConfirm: (courseId: String, dept: String, section: String, roll: String, name: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf(courses.find { it.id == defaultCourseId } ?: courses.firstOrNull()) }
    var department by remember { mutableStateOf(selectedCourse?.department ?: defaultDept ?: departments.firstOrNull() ?: "Computer Science") }
    var section by remember { mutableStateOf(selectedCourse?.section ?: "Section A") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enroll New Student",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ProfNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rollNumber,
                    onValueChange = { rollNumber = it },
                    label = { Text("Registration / Roll Number") },
                    placeholder = { Text("e.g. CS-2023-45") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Student Email (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section (e.g. Section A)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (courses.isNotEmpty()) {
                    Text(
                        text = "Assigned Course: ${selectedCourse?.courseCode ?: "General"} - ${selectedCourse?.courseTitle ?: ""}",
                        fontSize = 12.sp,
                        color = ProfNavy,
                        fontWeight = FontWeight.Medium
                    )
                }

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
                    if (name.isBlank() || rollNumber.isBlank() || department.isBlank() || section.isBlank()) {
                        errorText = "Please fill in student name, roll number, department and section."
                        return@Button
                    }
                    val courseId = selectedCourse?.id ?: "general_course"
                    onConfirm(courseId, department, section, rollNumber, name, email)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfNavy)
            ) {
                Text("Enroll Student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
