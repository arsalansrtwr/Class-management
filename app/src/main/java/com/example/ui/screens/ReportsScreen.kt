package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComputedGrade
import com.example.ui.components.GradeBadge
import com.example.ui.components.MetricStatCard
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
import com.example.ui.viewmodel.DepartmentReport
import com.example.ui.viewmodel.ProfViewModel

@Composable
fun ReportsScreen(
    viewModel: ProfViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val departmentReports by viewModel.departmentReports.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val profile by viewModel.teacherProfile.collectAsState()

    // Calculate aggregated overall metrics
    val totalStudents = allStudents.size
    val allGrades = allStudents.map {
        ComputedGrade.calculate(it.assignmentQuizTotal, it.midtermScore, it.finalTermScore)
    }
    val overallAverage = if (allGrades.isNotEmpty()) allGrades.map { it.totalPercentage }.average() else 0.0
    val overallPassCount = allGrades.count { it.isPassing }
    val overallPassRate = if (allGrades.isNotEmpty()) (overallPassCount.toDouble() / allGrades.size) * 100.0 else 0.0

    // At-risk students (<60% grade or <75% attendance)
    val atRiskStudents = allStudents.mapNotNull { student ->
        val grade = ComputedGrade.calculate(student.assignmentQuizTotal, student.midtermScore, student.finalTermScore)
        val attRate = if (student.totalAttendancePossible > 0) {
            (student.totalAttendanceCount.toDouble() / student.totalAttendancePossible) * 100.0
        } else 100.0
        if (grade.totalPercentage < 60.0 || attRate < 75.0) {
            Triple(student, grade, attRate)
        } else null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Administrative Action Buttons
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Administrative Oversight Report",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfNavyDark
                            )
                            Text(
                                text = "Professor: ${profile?.name ?: "Academic Staff"} • ${profile?.primaryDepartment ?: "Department"}",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Share via Android Share Sheet
                        Button(
                            onClick = {
                                val reportText = generateReportText(
                                    profile = profile?.name ?: "Professor",
                                    dept = profile?.primaryDepartment ?: "",
                                    totalStudents = totalStudents,
                                    overallAverage = overallAverage,
                                    passRate = overallPassRate,
                                    deptReports = departmentReports,
                                    atRiskCount = atRiskStudents.size
                                )
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                    putExtra(Intent.EXTRA_SUBJECT, "Administrative Academic Report - ProfGrade")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Administrative Report")
                                context.startActivity(shareIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("share_report_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Report", fontSize = 13.sp)
                        }

                        // Copy Report to Clipboard
                        OutlinedButton(
                            onClick = {
                                val reportText = generateReportText(
                                    profile = profile?.name ?: "Professor",
                                    dept = profile?.primaryDepartment ?: "",
                                    totalStudents = totalStudents,
                                    overallAverage = overallAverage,
                                    passRate = overallPassRate,
                                    deptReports = departmentReports,
                                    atRiskCount = atRiskStudents.size
                                )
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Academic Report", reportText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Summary", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Summary Metric Stat Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Total Enrolled",
                    value = "$totalStudents",
                    subtitle = "${departmentReports.size} Departments",
                    icon = Icons.Default.School,
                    iconColor = ProfNavy,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Overall Average",
                    value = "${String.format("%.1f", overallAverage)}%",
                    subtitle = "25-25-50 Formula",
                    icon = Icons.Default.Assessment,
                    iconColor = if (overallAverage >= 70) GradeSuccess else GradeWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Pass Rate",
                    value = "${String.format("%.0f", overallPassRate)}%",
                    subtitle = "$overallPassCount Passing",
                    icon = Icons.Default.Star,
                    iconColor = if (overallPassRate >= 75) GradeSuccess else GradeDanger,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section Title: Department Breakdown
        item {
            Text(
                text = "Department Performance Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ProfNavyDark
            )
        }

        if (departmentReports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No departmental data available. Enroll students in courses to generate reports.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(departmentReports) { report ->
                DepartmentReportCard(report = report)
            }
        }

        // Section Title: At-Risk Students Warning
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = GradeDanger,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "At-Risk Students Requiring Intervention (${atRiskStudents.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GradeDanger
                )
            }
        }

        if (atRiskStudents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GradeSuccessBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Excellent: All students maintain >= 60% grades and >= 75% attendance.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GradeSuccess,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(atRiskStudents) { (student, grade, attRate) ->
                AtRiskStudentCard(student = student, grade = grade, attendanceRate = attRate)
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun DepartmentReportCard(report: DepartmentReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Department Name & Pass Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GradeInfoBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CorporateFare,
                            contentDescription = null,
                            tint = ProfNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = report.department,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${report.totalStudents} enrolled students",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (report.passRate >= 70) GradeSuccessBg else GradeDangerBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${String.format("%.0f", report.passRate)}% Pass",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (report.passRate >= 70) GradeSuccess else GradeDanger
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Key Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Average Score", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = "${String.format("%.1f", report.averageGrade)}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfNavyDark
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Attendance Rate", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = "${String.format("%.0f", report.attendanceRate)}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (report.attendanceRate >= 75) GradeSuccess else GradeWarning
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Top Student", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = report.topStudent?.name ?: "N/A",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ProfNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grade Distribution Bar
            Text(
                text = "Grade Distribution (A, B, C, D, F):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GradeCountChip(grade = "A", count = report.gradeDistribution["A"] ?: 0, color = GradeSuccess, bgColor = GradeSuccessBg)
                GradeCountChip(grade = "B", count = report.gradeDistribution["B"] ?: 0, color = GradeInfo, bgColor = GradeInfoBg)
                GradeCountChip(grade = "C", count = report.gradeDistribution["C"] ?: 0, color = GradeWarning, bgColor = GradeWarningBg)
                GradeCountChip(grade = "D", count = report.gradeDistribution["D"] ?: 0, color = GradeWarning, bgColor = GradeWarningBg)
                GradeCountChip(grade = "F", count = report.gradeDistribution["F"] ?: 0, color = GradeDanger, bgColor = GradeDangerBg)
            }
        }
    }
}

@Composable
private fun GradeCountChip(
    grade: String,
    count: Int,
    color: Color,
    bgColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$grade: $count",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun AtRiskStudentCard(
    student: com.example.data.model.Student,
    grade: ComputedGrade,
    attendanceRate: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GradeDanger.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GradeDangerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = GradeDanger,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${student.rollNumber} • ${student.department} (${student.section})",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Attendance: ${String.format("%.0f", attendanceRate)}% • Score: ${String.format("%.1f", grade.totalPercentage)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GradeDanger
                )
            }

            GradeBadge(letterGrade = grade.letterGrade, percentage = grade.totalPercentage)
        }
    }
}

fun generateReportText(
    profile: String,
    dept: String,
    totalStudents: Int,
    overallAverage: Double,
    passRate: Double,
    deptReports: List<DepartmentReport>,
    atRiskCount: Int
): String {
    val sb = StringBuilder()
    sb.appendLine("==========================================")
    sb.appendLine("ADMINISTRATIVE ACADEMIC OVERSIGHT REPORT")
    sb.appendLine("PROFGRADE ACADEMIC SYSTEM")
    sb.appendLine("==========================================")
    sb.appendLine("Faculty / Professor: $profile")
    sb.appendLine("Primary Department: $dept")
    sb.appendLine("Grading Standard: 25% Asg/Quizzes | 25% Midterm | 50% Final Exam")
    sb.appendLine("------------------------------------------")
    sb.appendLine("EXECUTIVE SUMMARY:")
    sb.appendLine("Total Enrolled Students: $totalStudents")
    sb.appendLine("Overall Academic Average: ${String.format("%.1f", overallAverage)}%")
    sb.appendLine("Overall Passing Rate: ${String.format("%.1f", passRate)}%")
    sb.appendLine("At-Risk Students Requiring Intervention: $atRiskCount")
    sb.appendLine("------------------------------------------")
    sb.appendLine("DEPARTMENT-WISE BREAKDOWN:")
    deptReports.forEach { d ->
        sb.appendLine("Department: ${d.department}")
        sb.appendLine("  - Total Students: ${d.totalStudents}")
        sb.appendLine("  - Average Grade: ${String.format("%.1f", d.averageGrade)}%")
        sb.appendLine("  - Pass Rate: ${String.format("%.1f", d.passRate)}%")
        sb.appendLine("  - Attendance Average: ${String.format("%.1f", d.attendanceRate)}%")
        sb.appendLine("  - Top Student: ${d.topStudent?.name ?: "N/A"}")
        sb.appendLine("  - Grade Distribution: A=${d.gradeDistribution["A"] ?: 0}, B=${d.gradeDistribution["B"] ?: 0}, C=${d.gradeDistribution["C"] ?: 0}, D=${d.gradeDistribution["D"] ?: 0}, F=${d.gradeDistribution["F"] ?: 0}")
        sb.appendLine()
    }
    sb.appendLine("==========================================")
    sb.appendLine("Generated via ProfGrade Academic Portal")
    return sb.toString()
}
