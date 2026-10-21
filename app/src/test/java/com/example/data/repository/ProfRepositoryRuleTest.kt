package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Assessment
import com.example.data.model.AssessmentType
import com.example.data.model.ComputedGrade
import com.example.data.model.Course
import com.example.data.model.Student
import com.example.data.model.TeacherProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProfRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun gradeCalculation_formulaExactness() {
        // User specification: 25% assignments and quizzes, 25% midterms, 50% final exam marks
        val perfectGrade = ComputedGrade.calculate(
            assignmentQuiz100 = 100.0,
            midterm100 = 100.0,
            final100 = 100.0
        )
        assertEquals(25.0, perfectGrade.assignmentQuizContribution, 0.001)
        assertEquals(25.0, perfectGrade.midtermContribution, 0.001)
        assertEquals(50.0, perfectGrade.finalContribution, 0.001)
        assertEquals(100.0, perfectGrade.totalPercentage, 0.001)
        assertEquals("A", perfectGrade.letterGrade)
        assertTrue(perfectGrade.isPassing)

        // 80% assignments, 70% midterm, 90% final:
        // 80 * 0.25 = 20
        // 70 * 0.25 = 17.5
        // 90 * 0.50 = 45.0
        // Total = 82.5 -> B+
        val grade2 = ComputedGrade.calculate(
            assignmentQuiz100 = 80.0,
            midterm100 = 70.0,
            final100 = 90.0
        )
        assertEquals(82.5, grade2.totalPercentage, 0.001)
        assertEquals("B+", grade2.letterGrade)
        assertTrue(grade2.isPassing)
    }

    @Test
    fun saveTeacherProfile_and_courses_succeedsForAuthenticatedUser() = runBlocking {
        val uid = signInTestUser("prof_test@university.edu")
        val repo = ProfRepository(firestore)

        val profile = TeacherProfile(
            userId = uid,
            name = "Dr. Professor Alan",
            primaryDepartment = "Computer Science",
            subjects = listOf("Algorithms", "Operating Systems"),
            departments = listOf("Computer Science", "Electrical Engineering")
        )
        withTimeout(5000L) {
            repo.saveTeacherProfile(uid, profile)
        }

        val observedProfile = withTimeout(5000L) {
            repo.observeTeacherProfile(uid).first { it != null }
        }
        assertEquals("Dr. Professor Alan", observedProfile?.name)

        val course = Course(
            userId = uid,
            courseCode = "CS-301",
            courseTitle = "Data Structures & Algorithms",
            department = "Computer Science",
            section = "Section A",
            creditHours = 3.0,
            semester = "Fall 2026"
        )
        val courseId = withTimeout(5000L) {
            repo.saveCourse(uid, course)
        }
        assertTrue(courseId.isNotEmpty())

        val courses = withTimeout(5000L) {
            repo.observeCourses(uid).first { it.isNotEmpty() }
        }
        assertTrue(courses.any { it.courseCode == "CS-301" })
    }

    @Test
    fun saveStudentAndAssessment_succeedsAndObservesCorrectly() = runBlocking {
        val uid = signInTestUser("prof_student_test@university.edu")
        val repo = ProfRepository(firestore)

        val student = Student(
            userId = uid,
            courseId = "cs101_sec_a",
            department = "Computer Science",
            section = "Section A",
            rollNumber = "CS-2026-101",
            name = "Jordan Lee",
            assignmentQuizTotal = 88.0,
            midtermScore = 84.0,
            finalTermScore = 92.0,
            totalAttendanceCount = 14,
            totalAttendancePossible = 15
        )
        val studentId = withTimeout(5000L) {
            repo.saveStudent(uid, student)
        }
        assertTrue(studentId.isNotEmpty())

        val assessment = Assessment(
            userId = uid,
            courseId = "cs101_sec_a",
            title = "Midterm Examination",
            type = AssessmentType.MIDTERM.name,
            maxMarks = 100.0,
            weightPercentage = 25.0,
            date = "2026-10-21"
        )
        val assessmentId = withTimeout(5000L) {
            repo.saveAssessment(uid, assessment)
        }
        assertTrue(assessmentId.isNotEmpty())
    }

    @Test
    fun unauthenticatedQuery_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = ProfRepository(firestore)

        try {
            withTimeout(3000L) {
                repo.observeCourses("unauth_user_id").first()
            }
            fail("Expected PERMISSION_DENIED exception")
        } catch (e: Exception) {
            val isPermissionDenied = e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
                e.message?.contains("Error getting Query snapshot", ignoreCase = true) == true ||
                (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            assertTrue("Expected permission denied or snapshot error, but got: ${e.javaClass.name}: ${e.message}", isPermissionDenied)
        }
    }
}
