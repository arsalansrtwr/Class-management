package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TeacherProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val primaryDepartment: String = "",
    val subjects: List<String> = emptyList(),
    val departments: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Course(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseCode: String = "",
    val courseTitle: String = "",
    val department: String = "",
    val section: String = "",
    val creditHours: Double = 3.0,
    val semester: String = "Fall 2026",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Student(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val department: String = "",
    val section: String = "",
    val rollNumber: String = "",
    val name: String = "",
    val email: String = "",
    val assignmentQuizTotal: Double = 0.0, // Scaled 0-100
    val midtermScore: Double = 0.0,        // 0-100
    val finalTermScore: Double = 0.0,      // 0-100
    val totalAttendanceCount: Long = 0L,
    val totalAttendancePossible: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AssessmentType {
    ASSIGNMENT,
    QUIZ,
    MIDTERM,
    FINAL
}

@IgnoreExtraProperties
data class Assessment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val title: String = "",
    val type: String = AssessmentType.ASSIGNMENT.name,
    val maxMarks: Double = 100.0,
    val weightPercentage: Double = 25.0,
    val date: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Score(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val studentId: String = "",
    val assessmentId: String = "",
    val obtainedMarks: Double = 0.0,
    val submitted: Boolean = true,
    val submissionNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE,
    EXCUSED
}

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val date: String = "",
    val topic: String = "",
    val studentStatuses: Map<String, String> = emptyMap(), // studentId -> status name
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// Computed Grade Representation:
// Formula: 25% of assignments and quizzes + 25% of midterms + 50% of final exam marks
data class ComputedGrade(
    val assignmentQuizContribution: Double, // max 25.0
    val midtermContribution: Double,        // max 25.0
    val finalContribution: Double,          // max 50.0
    val totalPercentage: Double,            // 0 - 100%
    val letterGrade: String,                // A, B+, B, C, D, F
    val gpa: Double,                        // 4.0 scale
    val isPassing: Boolean                  // >= 50%
) {
    companion object {
        fun calculate(assignmentQuiz100: Double, midterm100: Double, final100: Double): ComputedGrade {
            val aAndQ = (assignmentQuiz100.coerceIn(0.0, 100.0) * 0.25)
            val mid = (midterm100.coerceIn(0.0, 100.0) * 0.25)
            val fin = (final100.coerceIn(0.0, 100.0) * 0.50)
            val total = aAndQ + mid + fin

            val (letter, gpa) = when {
                total >= 90.0 -> "A" to 4.0
                total >= 85.0 -> "A-" to 3.7
                total >= 80.0 -> "B+" to 3.3
                total >= 75.0 -> "B" to 3.0
                total >= 70.0 -> "B-" to 2.7
                total >= 65.0 -> "C+" to 2.3
                total >= 60.0 -> "C" to 2.0
                total >= 55.0 -> "C-" to 1.7
                total >= 50.0 -> "D" to 1.0
                else -> "F" to 0.0
            }

            return ComputedGrade(
                assignmentQuizContribution = aAndQ,
                midtermContribution = mid,
                finalContribution = fin,
                totalPercentage = total,
                letterGrade = letter,
                gpa = gpa,
                isPassing = total >= 50.0
            )
        }
    }
}
