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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceScreen(
    viewModel: ProfViewModel,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val selectedCourseId by viewModel.selectedCourseId.collectAsState()
    val selectedDepartment by viewModel.selectedDepartment.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var sessionDate by remember { mutableStateOf(todayDateStr) }
    var lectureTopic by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    // Filter students for attendance
    val relevantStudents = remember(allStudents, selectedCourseId, selectedDepartment) {
        allStudents.filter { s ->
            (selectedCourseId == null || s.courseId == selectedCourseId) &&
            (selectedDepartment == null || s.department.equals(selectedDepartment, ignoreCase = true))
        }
    }

    var attendanceSearchQuery by remember { mutableStateOf("") }
    var attendanceDeptFilter by remember { mutableStateOf<String?>(null) }

    val departmentsInRollCall = remember(relevantStudents) {
        relevantStudents.map { it.department }.filter { it.isNotBlank() }.distinct()
    }

    val displayedStudentsForAttendance = remember(relevantStudents, attendanceSearchQuery, attendanceDeptFilter) {
        relevantStudents.filter { s ->
            (attendanceDeptFilter == null || s.department.equals(attendanceDeptFilter, ignoreCase = true)) &&
            (attendanceSearchQuery.isBlank() || s.name.contains(attendanceSearchQuery, ignoreCase = true) || s.rollNumber.contains(attendanceSearchQuery, ignoreCase = true))
        }
    }

    // Student ID to status map
    val attendanceMap = remember(relevantStudents) {
        mutableStateMapOf<String, AttendanceStatus>().apply {
            relevantStudents.forEach { put(it.id, AttendanceStatus.PRESENT) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite)
    ) {
        // Tabs: Take Attendance vs History Log
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = PureWhite,
            contentColor = ProfNavy
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Take Attendance", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Session History (${attendanceRecords.size})", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (selectedTabIndex == 0) {
            // Take Attendance Form
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Session Header Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Class Session Details",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfNavy
                                )

                                // Bulk Actions: Mark All Present & Mark All Absent
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            displayedStudentsForAttendance.forEach {
                                                attendanceMap[it.id] = AttendanceStatus.ABSENT
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("All Absent", fontSize = 11.sp, color = GradeDanger)
                                    }

                                    Button(
                                        onClick = {
                                            displayedStudentsForAttendance.forEach {
                                                attendanceMap[it.id] = AttendanceStatus.PRESENT
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GradeSuccess),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp).testTag("mark_all_present_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("All Present", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = sessionDate,
                                    onValueChange = { sessionDate = it },
                                    label = { Text("Session Date") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ProfNavy,
                                        unfocusedBorderColor = BorderMedium,
                                        focusedContainerColor = PureWhite,
                                        unfocusedContainerColor = PureWhite
                                    )
                                )

                                OutlinedTextField(
                                    value = lectureTopic,
                                    onValueChange = { lectureTopic = it },
                                    label = { Text("Lecture Topic") },
                                    placeholder = { Text("e.g. Graph Traversals") },
                                    modifier = Modifier.weight(1.5f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ProfNavy,
                                        unfocusedBorderColor = BorderMedium,
                                        focusedContainerColor = PureWhite,
                                        unfocusedContainerColor = PureWhite
                                    )
                                )
                            }
                        }
                    }
                }

                // Fast Search & Department Filter for Large Cohorts (100+ Students)
                item {
                    OutlinedTextField(
                        value = attendanceSearchQuery,
                        onValueChange = { attendanceSearchQuery = it },
                        placeholder = { Text("Search student by name or roll number...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                        },
                        modifier = Modifier.fillMaxWidth(),
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

                if (departmentsInRollCall.size > 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (attendanceDeptFilter == null) ProfNavy else Color(0xFFF1F5F9))
                                    .clickable { attendanceDeptFilter = null }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "All Departments (${relevantStudents.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (attendanceDeptFilter == null) PureWhite else TextPrimary
                                )
                            }

                            departmentsInRollCall.forEach { dept ->
                                val isSel = attendanceDeptFilter.equals(dept, ignoreCase = true)
                                val count = relevantStudents.count { it.department.equals(dept, ignoreCase = true) }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) ProfNavy else Color(0xFFF1F5F9))
                                        .clickable { attendanceDeptFilter = if (isSel) null else dept }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "$dept ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) PureWhite else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Student Attendance Roll Call List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Student Roll Call (${displayedStudentsForAttendance.size} shown of ${relevantStudents.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        val presentCount = displayedStudentsForAttendance.count {
                            attendanceMap[it.id] == AttendanceStatus.PRESENT || attendanceMap[it.id] == AttendanceStatus.LATE
                        }
                        Text(
                            text = "$presentCount / ${displayedStudentsForAttendance.size} Present",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ProfNavy
                        )
                    }
                }

                if (displayedStudentsForAttendance.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (relevantStudents.isEmpty()) "No students enrolled yet for this section." else "No students match this search filter.",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(displayedStudentsForAttendance, key = { it.id }) { student ->
                        val currentStatus = attendanceMap[student.id] ?: AttendanceStatus.PRESENT
                        StudentAttendanceRow(
                            studentName = student.name,
                            rollNumber = student.rollNumber,
                            department = student.department,
                            status = currentStatus,
                            onStatusChange = { newStatus ->
                                attendanceMap[student.id] = newStatus
                            }
                        )
                    }
                }

                // Submit Attendance Session Button
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val activeCourseId = selectedCourseId ?: courses.firstOrNull()?.id ?: "general_course"
                            val statuses = attendanceMap.mapValues { it.value.name }
                            viewModel.recordAttendanceSession(
                                courseId = activeCourseId,
                                date = sessionDate,
                                topic = lectureTopic.ifBlank { "Regular Lecture Session" },
                                statuses = statuses
                            )
                            saveMessage = "Session for $sessionDate saved and synced to database."
                            lectureTopic = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_attendance_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Session & Sync Cloud Records",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (saveMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = saveMessage ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GradeSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        } else {
            // Past Session History Log
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (attendanceRecords.isEmpty()) {
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
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No recorded attendance sessions",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Submit an attendance roll-call session to see logs here.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(attendanceRecords, key = { it.id }) { record ->
                        AttendanceRecordCard(record = record)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceRow(
    studentName: String,
    rollNumber: String,
    department: String,
    status: AttendanceStatus,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = studentName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "$rollNumber • $department",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            // Quick Status Selector Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusChoicePill(
                    label = "P",
                    color = GradeSuccess,
                    bgColor = GradeSuccessBg,
                    isSelected = status == AttendanceStatus.PRESENT,
                    onClick = { onStatusChange(AttendanceStatus.PRESENT) }
                )
                StatusChoicePill(
                    label = "A",
                    color = GradeDanger,
                    bgColor = GradeDangerBg,
                    isSelected = status == AttendanceStatus.ABSENT,
                    onClick = { onStatusChange(AttendanceStatus.ABSENT) }
                )
                StatusChoicePill(
                    label = "L",
                    color = GradeWarning,
                    bgColor = GradeWarningBg,
                    isSelected = status == AttendanceStatus.LATE,
                    onClick = { onStatusChange(AttendanceStatus.LATE) }
                )
                StatusChoicePill(
                    label = "E",
                    color = GradeInfo,
                    bgColor = GradeInfoBg,
                    isSelected = status == AttendanceStatus.EXCUSED,
                    onClick = { onStatusChange(AttendanceStatus.EXCUSED) }
                )
            }
        }
    }
}

@Composable
private fun StatusChoicePill(
    label: String,
    color: Color,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color else bgColor)
            .border(
                1.dp,
                if (isSelected) color else color.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) PureWhite else color
        )
    }
}

@Composable
fun AttendanceRecordCard(record: AttendanceRecord) {
    val total = record.studentStatuses.size
    val present = record.studentStatuses.values.count { it == "PRESENT" || it == "LATE" }
    val rate = if (total > 0) (present.toDouble() / total) * 100.0 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = ProfNavy,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = record.date,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (rate >= 75) GradeSuccessBg else GradeDangerBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${String.format("%.0f", rate)}% Attendance",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rate >= 75) GradeSuccess else GradeDanger
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Topic: ${record.topic.ifBlank { "General Lecture" }}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ProfNavyDark
            )

            Text(
                text = "$present of $total students attended",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}
