package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Assessment
import com.example.data.model.AssessmentType
import com.example.data.model.AttendanceRecord
import com.example.data.model.ComputedGrade
import com.example.data.model.Course
import com.example.data.model.Score
import com.example.data.model.Student
import com.example.data.model.TeacherProfile
import com.example.data.repository.ProfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DepartmentReport(
    val department: String,
    val totalStudents: Int,
    val averageGrade: Double,
    val passRate: Double,
    val attendanceRate: Double,
    val topStudent: Student?,
    val gradeDistribution: Map<String, Int> // "A", "B", "C", "D", "F" counts
)

class ProfViewModel(
    private val repository: ProfRepository,
    val currentUserId: String
) : ViewModel() {

    val teacherProfile: StateFlow<TeacherProfile?> = repository.observeTeacherProfile(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val courses: StateFlow<List<Course>> = repository.observeCourses(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val students: StateFlow<List<Student>> = repository.observeStudents(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val assessments: StateFlow<List<Assessment>> = repository.observeAssessments(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val scores: StateFlow<List<Score>> = repository.observeScores(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val attendanceRecords: StateFlow<List<AttendanceRecord>> = repository.observeAttendanceRecords(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // UI Selection States
    private val _selectedCourseId = MutableStateFlow<String?>(null)
    val selectedCourseId: StateFlow<String?> = _selectedCourseId.asStateFlow()

    private val _selectedDepartment = MutableStateFlow<String?>(null)
    val selectedDepartment: StateFlow<String?> = _selectedDepartment.asStateFlow()

    private val _selectedSection = MutableStateFlow<String?>(null)
    val selectedSection: StateFlow<String?> = _selectedSection.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun selectCourse(courseId: String?) {
        _selectedCourseId.value = courseId
    }

    fun selectDepartment(dept: String?) {
        _selectedDepartment.value = dept
    }

    fun selectSection(section: String?) {
        _selectedSection.value = section
    }

    // Filtered students by selected course, department, and section
    val filteredStudents: StateFlow<List<Pair<Student, ComputedGrade>>> = combine(
        students,
        _selectedCourseId,
        _selectedDepartment,
        _selectedSection
    ) { allStudents, courseId, dept, section ->
        allStudents
            .filter { student ->
                (courseId == null || student.courseId == courseId) &&
                (dept == null || student.department.equals(dept, ignoreCase = true)) &&
                (section == null || student.section.equals(section, ignoreCase = true))
            }
            .map { student ->
                val grade = ComputedGrade.calculate(
                    assignmentQuiz100 = student.assignmentQuizTotal,
                    midterm100 = student.midtermScore,
                    final100 = student.finalTermScore
                )
                student to grade
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // All Department Reports for Administrative Oversight
    val departmentReports: StateFlow<List<DepartmentReport>> = combine(
        students,
        teacherProfile,
        courses
    ) { allStudents, profile, allCourses ->
        val departments = (
            (profile?.departments ?: emptyList()) +
            allCourses.map { it.department } +
            allStudents.map { it.department }
        ).filter { it.isNotBlank() }.distinct()

        departments.map { dept ->
            val deptStudents = allStudents.filter { it.department.equals(dept, ignoreCase = true) }
            val grades = deptStudents.map {
                ComputedGrade.calculate(it.assignmentQuizTotal, it.midtermScore, it.finalTermScore)
            }
            val avg = if (grades.isNotEmpty()) grades.map { it.totalPercentage }.average() else 0.0
            val passCount = grades.count { it.isPassing }
            val passRate = if (grades.isNotEmpty()) (passCount.toDouble() / grades.size) * 100.0 else 0.0

            val totalAttPossible = deptStudents.sumOf { it.totalAttendancePossible }
            val totalAttAttended = deptStudents.sumOf { it.totalAttendanceCount }
            val attRate = if (totalAttPossible > 0) (totalAttAttended.toDouble() / totalAttPossible) * 100.0 else 100.0

            val dist = mutableMapOf("A" to 0, "B" to 0, "C" to 0, "D" to 0, "F" to 0)
            grades.forEach { g ->
                when {
                    g.letterGrade.startsWith("A") -> dist["A"] = (dist["A"] ?: 0) + 1
                    g.letterGrade.startsWith("B") -> dist["B"] = (dist["B"] ?: 0) + 1
                    g.letterGrade.startsWith("C") -> dist["C"] = (dist["C"] ?: 0) + 1
                    g.letterGrade.startsWith("D") -> dist["D"] = (dist["D"] ?: 0) + 1
                    else -> dist["F"] = (dist["F"] ?: 0) + 1
                }
            }

            val top = deptStudents.maxByOrNull {
                ComputedGrade.calculate(it.assignmentQuizTotal, it.midtermScore, it.finalTermScore).totalPercentage
            }

            DepartmentReport(
                department = dept,
                totalStudents = deptStudents.size,
                averageGrade = avg,
                passRate = passRate,
                attendanceRate = attRate,
                topStudent = top,
                gradeDistribution = dist
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Profile Management
    fun saveTeacherProfile(
        name: String,
        email: String,
        primaryDepartment: String,
        subjects: List<String>,
        departments: List<String>
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val profile = TeacherProfile(
                    userId = currentUserId,
                    name = name.trim(),
                    email = email.trim(),
                    primaryDepartment = primaryDepartment.trim(),
                    subjects = subjects.map { it.trim() }.filter { it.isNotBlank() },
                    departments = departments.map { it.trim() }.filter { it.isNotBlank() }
                )
                repository.saveTeacherProfile(currentUserId, profile)
                _statusMessage.value = "Teacher profile synchronized"
            } catch (e: Exception) {
                _statusMessage.value = "Error updating profile: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Course & Section Management
    fun addOrUpdateCourse(
        courseId: String = "",
        courseCode: String,
        courseTitle: String,
        department: String,
        section: String,
        creditHours: Double = 3.0,
        semester: String = "Fall 2026"
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val course = Course(
                    id = courseId,
                    userId = currentUserId,
                    courseCode = courseCode.trim().uppercase(),
                    courseTitle = courseTitle.trim(),
                    department = department.trim(),
                    section = section.trim(),
                    creditHours = creditHours,
                    semester = semester.trim()
                )
                val id = repository.saveCourse(currentUserId, course)
                if (_selectedCourseId.value == null) {
                    _selectedCourseId.value = id
                }
                _statusMessage.value = "Course saved and synced"
            } catch (e: Exception) {
                _statusMessage.value = "Error saving course: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.deleteCourse(currentUserId, courseId)
                if (_selectedCourseId.value == courseId) {
                    _selectedCourseId.value = null
                }
                _statusMessage.value = "Course removed"
            } catch (e: Exception) {
                _statusMessage.value = "Error removing course: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Student Management
    fun addOrUpdateStudent(
        studentId: String = "",
        courseId: String,
        department: String,
        section: String,
        rollNumber: String,
        name: String,
        email: String = "",
        assignmentQuizTotal: Double = 0.0,
        midtermScore: Double = 0.0,
        finalTermScore: Double = 0.0
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val student = Student(
                    id = studentId,
                    userId = currentUserId,
                    courseId = courseId,
                    department = department.trim(),
                    section = section.trim(),
                    rollNumber = rollNumber.trim().uppercase(),
                    name = name.trim(),
                    email = email.trim(),
                    assignmentQuizTotal = assignmentQuizTotal.coerceIn(0.0, 100.0),
                    midtermScore = midtermScore.coerceIn(0.0, 100.0),
                    finalTermScore = finalTermScore.coerceIn(0.0, 100.0)
                )
                repository.saveStudent(currentUserId, student)
                _statusMessage.value = "Student records updated"
            } catch (e: Exception) {
                _statusMessage.value = "Error saving student: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun batchAddStudents(
        department: String,
        section: String,
        courseId: String,
        studentsData: List<Triple<String, String, String>> // rollNumber, name, email
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val studentObjects = studentsData.map { (roll, name, email) ->
                    Student(
                        userId = currentUserId,
                        courseId = courseId,
                        department = department.trim(),
                        section = section.trim(),
                        rollNumber = roll.trim().uppercase(),
                        name = name.trim(),
                        email = email.trim(),
                        assignmentQuizTotal = 0.0,
                        midtermScore = 0.0,
                        finalTermScore = 0.0
                    )
                }
                repository.saveStudentsBatch(currentUserId, studentObjects)
                _statusMessage.value = "Saved ${studentObjects.size} students for $department!"
            } catch (e: Exception) {
                _statusMessage.value = "Error saving students for $department: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun updateStudentScores(
        student: Student,
        assignmentQuizTotal: Double,
        midtermScore: Double,
        finalTermScore: Double
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val updated = student.copy(
                    assignmentQuizTotal = assignmentQuizTotal.coerceIn(0.0, 100.0),
                    midtermScore = midtermScore.coerceIn(0.0, 100.0),
                    finalTermScore = finalTermScore.coerceIn(0.0, 100.0)
                )
                repository.saveStudent(currentUserId, updated)
                _statusMessage.value = "Marks recorded & synced"
            } catch (e: Exception) {
                _statusMessage.value = "Error recording marks: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.deleteStudent(currentUserId, studentId)
                _statusMessage.value = "Student removed"
            } catch (e: Exception) {
                _statusMessage.value = "Error removing student: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Assessment Management
    fun addOrUpdateAssessment(
        assessmentId: String = "",
        courseId: String,
        title: String,
        type: AssessmentType,
        maxMarks: Double = 100.0,
        weightPercentage: Double = 25.0,
        date: String
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val assessment = Assessment(
                    id = assessmentId,
                    userId = currentUserId,
                    courseId = courseId,
                    title = title.trim(),
                    type = type.name,
                    maxMarks = maxMarks,
                    weightPercentage = weightPercentage,
                    date = date.trim()
                )
                repository.saveAssessment(currentUserId, assessment)
                _statusMessage.value = "Assessment recorded"
            } catch (e: Exception) {
                _statusMessage.value = "Error saving assessment: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteAssessment(assessmentId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.deleteAssessment(currentUserId, assessmentId)
                _statusMessage.value = "Assessment deleted"
            } catch (e: Exception) {
                _statusMessage.value = "Error deleting assessment: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Score Recording for individual Assessment
    fun recordAssessmentScore(
        courseId: String,
        studentId: String,
        assessmentId: String,
        obtainedMarks: Double,
        submitted: Boolean = true,
        notes: String = ""
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val score = Score(
                    userId = currentUserId,
                    courseId = courseId,
                    studentId = studentId,
                    assessmentId = assessmentId,
                    obtainedMarks = obtainedMarks,
                    submitted = submitted,
                    submissionNotes = notes
                )
                repository.saveScore(currentUserId, score)
                recalculateStudentScoresFromAssessments(studentId, courseId)
                _statusMessage.value = "Score recorded & synced"
            } catch (e: Exception) {
                _statusMessage.value = "Error recording score: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun batchRecordAssessmentScores(
        courseId: String,
        assessmentId: String,
        studentScores: List<Pair<String, Double>> // studentId to marks
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val scoreObjects = studentScores.map { (studentId, marks) ->
                    Score(
                        userId = currentUserId,
                        courseId = courseId,
                        studentId = studentId,
                        assessmentId = assessmentId,
                        obtainedMarks = marks,
                        submitted = true,
                        submissionNotes = "Batch entry"
                    )
                }
                repository.saveScoresBatch(currentUserId, scoreObjects)
                // Recalculate students
                val assessment = assessments.value.find { it.id == assessmentId }
                if (assessment != null) {
                    val currentStudentsMap = students.value.associateBy { it.id }
                    val studentsToUpdate = mutableListOf<Student>()
                    studentScores.forEach { (studentId, marks) ->
                        val student = currentStudentsMap[studentId]
                        if (student != null) {
                            val updated = when (assessment.type) {
                                AssessmentType.ASSIGNMENT.name, AssessmentType.QUIZ.name -> {
                                    val percent = if (assessment.maxMarks > 0) (marks / assessment.maxMarks) * 100.0 else marks
                                    student.copy(assignmentQuizTotal = percent.coerceIn(0.0, 100.0))
                                }
                                AssessmentType.MIDTERM.name -> {
                                    val percent = if (assessment.maxMarks > 0) (marks / assessment.maxMarks) * 100.0 else marks
                                    student.copy(midtermScore = percent.coerceIn(0.0, 100.0))
                                }
                                AssessmentType.FINAL.name -> {
                                    val percent = if (assessment.maxMarks > 0) (marks / assessment.maxMarks) * 100.0 else marks
                                    student.copy(finalTermScore = percent.coerceIn(0.0, 100.0))
                                }
                                else -> student
                            }
                            studentsToUpdate.add(updated)
                        }
                    }
                    if (studentsToUpdate.isNotEmpty()) {
                        repository.saveStudentsBatch(currentUserId, studentsToUpdate)
                    }
                }
                _statusMessage.value = "Saved ${studentScores.size} scores & recalculated grades!"
            } catch (e: Exception) {
                _statusMessage.value = "Error batch saving scores: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun recalculateStudentScoresFromAssessments(studentId: String, courseId: String) {
        val student = students.value.find { it.id == studentId } ?: return
        val courseAssessments = assessments.value.filter { it.courseId == courseId }
        val studentScores = scores.value.filter { it.studentId == studentId && it.courseId == courseId }

        var asgQuizPossible = 0.0
        var asgQuizObtained = 0.0

        var midPossible = 0.0
        var midObtained = 0.0

        var finalPossible = 0.0
        var finalObtained = 0.0

        courseAssessments.forEach { assessment ->
            val score = studentScores.find { it.assessmentId == assessment.id }
            val marks = score?.obtainedMarks ?: 0.0

            when (assessment.type) {
                AssessmentType.ASSIGNMENT.name, AssessmentType.QUIZ.name -> {
                    asgQuizPossible += assessment.maxMarks
                    asgQuizObtained += marks
                }
                AssessmentType.MIDTERM.name -> {
                    midPossible += assessment.maxMarks
                    midObtained += marks
                }
                AssessmentType.FINAL.name -> {
                    finalPossible += assessment.maxMarks
                    finalObtained += marks
                }
            }
        }

        val updatedAsgQuiz = if (asgQuizPossible > 0) (asgQuizObtained / asgQuizPossible) * 100.0 else student.assignmentQuizTotal
        val updatedMid = if (midPossible > 0) (midObtained / midPossible) * 100.0 else student.midtermScore
        val updatedFinal = if (finalPossible > 0) (finalObtained / finalPossible) * 100.0 else student.finalTermScore

        val updatedStudent = student.copy(
            assignmentQuizTotal = updatedAsgQuiz,
            midtermScore = updatedMid,
            finalTermScore = updatedFinal
        )
        repository.saveStudent(currentUserId, updatedStudent)
    }

    // Attendance Management
    fun recordAttendanceSession(
        courseId: String,
        date: String,
        topic: String,
        statuses: Map<String, String>
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val record = AttendanceRecord(
                    userId = currentUserId,
                    courseId = courseId,
                    date = date,
                    topic = topic.trim(),
                    studentStatuses = statuses
                )
                repository.saveAttendanceRecord(currentUserId, record)

                // Update cumulative attendance on all students in this course
                statuses.forEach { (studentId, statusStr) ->
                    val student = students.value.find { it.id == studentId }
                    if (student != null) {
                        val isPresent = statusStr == "PRESENT" || statusStr == "LATE"
                        val newCount = student.totalAttendanceCount + if (isPresent) 1 else 0
                        val newPossible = student.totalAttendancePossible + 1
                        repository.saveStudent(
                            currentUserId,
                            student.copy(
                                totalAttendanceCount = newCount,
                                totalAttendancePossible = newPossible
                            )
                        )
                    }
                }
                _statusMessage.value = "Attendance session saved and synced"
            } catch (e: Exception) {
                _statusMessage.value = "Error saving attendance: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Seed Demo Data for quick start & verification
    fun seedDemoData(professorName: String = "Dr. Robert Vance") {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val profile = TeacherProfile(
                    userId = currentUserId,
                    name = professorName,
                    email = "dr.vance@university.edu",
                    primaryDepartment = "Computer Science",
                    subjects = listOf("Data Structures", "Algorithms", "Software Engineering"),
                    departments = listOf("Computer Science", "Software Engineering", "Electrical Engineering")
                )
                repository.saveTeacherProfile(currentUserId, profile)

                val course1 = Course(
                    userId = currentUserId,
                    courseCode = "CS-301",
                    courseTitle = "Data Structures & Algorithms",
                    department = "Computer Science",
                    section = "Section A",
                    creditHours = 3.0,
                    semester = "Fall 2026"
                )
                val c1Id = repository.saveCourse(currentUserId, course1)

                val course2 = Course(
                    userId = currentUserId,
                    courseCode = "CS-301",
                    courseTitle = "Data Structures & Algorithms",
                    department = "Software Engineering",
                    section = "Section B",
                    creditHours = 3.0,
                    semester = "Fall 2026"
                )
                val c2Id = repository.saveCourse(currentUserId, course2)

                val course3 = Course(
                    userId = currentUserId,
                    courseCode = "EE-210",
                    courseTitle = "Digital Logic & Computer Design",
                    department = "Electrical Engineering",
                    section = "Section A",
                    creditHours = 4.0,
                    semester = "Fall 2026"
                )
                val c3Id = repository.saveCourse(currentUserId, course3)

                // Students for CS-301 Section A
                val csStudents = listOf(
                    Student(userId = currentUserId, courseId = c1Id, department = "Computer Science", section = "Section A", rollNumber = "CS-2023-01", name = "Aiden Brooks", email = "aiden@cs.edu", assignmentQuizTotal = 92.0, midtermScore = 88.0, finalTermScore = 94.0, totalAttendanceCount = 20, totalAttendancePossible = 20),
                    Student(userId = currentUserId, courseId = c1Id, department = "Computer Science", section = "Section A", rollNumber = "CS-2023-02", name = "Sophia Chen", email = "sophia@cs.edu", assignmentQuizTotal = 85.0, midtermScore = 90.0, finalTermScore = 88.0, totalAttendanceCount = 19, totalAttendancePossible = 20),
                    Student(userId = currentUserId, courseId = c1Id, department = "Computer Science", section = "Section A", rollNumber = "CS-2023-03", name = "Marcus Lee", email = "marcus@cs.edu", assignmentQuizTotal = 74.0, midtermScore = 70.0, finalTermScore = 78.0, totalAttendanceCount = 17, totalAttendancePossible = 20),
                    Student(userId = currentUserId, courseId = c1Id, department = "Computer Science", section = "Section A", rollNumber = "CS-2023-04", name = "Emma Watson", email = "emma@cs.edu", assignmentQuizTotal = 58.0, midtermScore = 62.0, finalTermScore = 65.0, totalAttendanceCount = 15, totalAttendancePossible = 20),
                    Student(userId = currentUserId, courseId = c1Id, department = "Computer Science", section = "Section A", rollNumber = "CS-2023-05", name = "Daniel Kim", email = "daniel@cs.edu", assignmentQuizTotal = 45.0, midtermScore = 48.0, finalTermScore = 52.0, totalAttendanceCount = 12, totalAttendancePossible = 20)
                )
                csStudents.forEach { repository.saveStudent(currentUserId, it) }

                // Students for CS-301 Section B (Software Engineering)
                val seStudents = listOf(
                    Student(userId = currentUserId, courseId = c2Id, department = "Software Engineering", section = "Section B", rollNumber = "SE-2023-11", name = "Liam Taylor", email = "liam@se.edu", assignmentQuizTotal = 95.0, midtermScore = 92.0, finalTermScore = 96.0, totalAttendanceCount = 20, totalAttendancePossible = 20),
                    Student(userId = currentUserId, courseId = c2Id, department = "Software Engineering", section = "Section B", rollNumber = "SE-2023-12", name = "Olivia Martinez", email = "olivia@se.edu", assignmentQuizTotal = 82.0, midtermScore = 80.0, finalTermScore = 85.0, totalAttendanceCount = 18, totalAttendancePossible = 20),
                    Student(userId = currentUserId, courseId = c2Id, department = "Software Engineering", section = "Section B", rollNumber = "SE-2023-13", name = "Noah Patel", email = "noah@se.edu", assignmentQuizTotal = 68.0, midtermScore = 64.0, finalTermScore = 72.0, totalAttendanceCount = 16, totalAttendancePossible = 20)
                )
                seStudents.forEach { repository.saveStudent(currentUserId, it) }

                // Assessments
                repository.saveAssessment(currentUserId, Assessment(userId = currentUserId, courseId = c1Id, title = "Assignment 1: Linked Lists & Complexity", type = AssessmentType.ASSIGNMENT.name, maxMarks = 50.0, weightPercentage = 10.0, date = "2026-09-15"))
                repository.saveAssessment(currentUserId, Assessment(userId = currentUserId, courseId = c1Id, title = "Quiz 1: Stack & Queue Operations", type = AssessmentType.QUIZ.name, maxMarks = 25.0, weightPercentage = 5.0, date = "2026-09-28"))
                repository.saveAssessment(currentUserId, Assessment(userId = currentUserId, courseId = c1Id, title = "Midterm Examination", type = AssessmentType.MIDTERM.name, maxMarks = 100.0, weightPercentage = 25.0, date = "2026-10-10"))
                repository.saveAssessment(currentUserId, Assessment(userId = currentUserId, courseId = c1Id, title = "Final Term Examination", type = AssessmentType.FINAL.name, maxMarks = 100.0, weightPercentage = 50.0, date = "2026-11-20"))

                _selectedCourseId.value = c1Id
                _selectedDepartment.value = "Computer Science"
                _statusMessage.value = "Demonstration courses and records created successfully"
            } catch (e: Exception) {
                _statusMessage.value = "Error generating demo data: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
