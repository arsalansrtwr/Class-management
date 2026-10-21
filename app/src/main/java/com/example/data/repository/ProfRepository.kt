package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Assessment
import com.example.data.model.AttendanceRecord
import com.example.data.model.Course
import com.example.data.model.Score
import com.example.data.model.Student
import com.example.data.model.TeacherProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProfRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // ================= Teacher Profile =================

    fun observeTeacherProfile(userId: String): Flow<TeacherProfile?> {
        val path = "users/$userId"
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(TeacherProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun saveTeacherProfile(userId: String, profile: TeacherProfile) {
        val path = "users/$userId"
        val docRef = db.collection("users").document(userId)
        try {
            val exists = docRef.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "name" to profile.name,
                "primaryDepartment" to profile.primaryDepartment,
                "subjects" to profile.subjects,
                "departments" to profile.departments,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (profile.email.isNotBlank()) {
                payload["email"] = profile.email
            }
            if (!exists) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    // ================= Courses =================

    fun observeCourses(userId: String): Flow<List<Course>> {
        val path = "users/$userId/courses"
        return db.collection("users").document(userId).collection("courses")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.courseCode }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun saveCourse(userId: String, course: Course): String {
        val docId = if (course.id.isNotBlank()) course.id else UUID.randomUUID().toString()
        val path = "users/$userId/courses/$docId"
        val docRef = db.collection("users").document(userId).collection("courses").document(docId)
        try {
            val exists = docRef.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "courseCode" to course.courseCode.trim(),
                "courseTitle" to course.courseTitle.trim(),
                "department" to course.department.trim(),
                "section" to course.section.trim(),
                "creditHours" to course.creditHours,
                "semester" to course.semester.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            return docId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun deleteCourse(userId: String, courseId: String) {
        val path = "users/$userId/courses/$courseId"
        try {
            db.collection("users").document(userId).collection("courses").document(courseId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    // ================= Students =================

    fun observeStudents(userId: String, courseId: String? = null): Flow<List<Student>> {
        val path = "users/$userId/students"
        val collection = db.collection("users").document(userId).collection("students")
        val query = if (!courseId.isNullOrBlank()) {
            collection.whereEqualTo("courseId", courseId)
        } else {
            collection
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(Student::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.rollNumber }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun saveStudent(userId: String, student: Student): String {
        val docId = if (student.id.isNotBlank()) student.id else UUID.randomUUID().toString()
        val path = "users/$userId/students/$docId"
        val docRef = db.collection("users").document(userId).collection("students").document(docId)
        try {
            val exists = docRef.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "courseId" to student.courseId,
                "department" to student.department.trim(),
                "section" to student.section.trim(),
                "rollNumber" to student.rollNumber.trim(),
                "name" to student.name.trim(),
                "assignmentQuizTotal" to student.assignmentQuizTotal,
                "midtermScore" to student.midtermScore,
                "finalTermScore" to student.finalTermScore,
                "totalAttendanceCount" to student.totalAttendanceCount,
                "totalAttendancePossible" to student.totalAttendancePossible,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (student.email.isNotBlank()) {
                payload["email"] = student.email.trim()
            }
            if (!exists) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            return docId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun saveStudentsBatch(userId: String, studentList: List<Student>) {
        if (studentList.isEmpty()) return
        val path = "users/$userId/students"
        try {
            // Firestore write batches have a limit of 500 operations.
            // Chunking by 400 guarantees safety even when enrolling 100, 200, 500+ students per department.
            studentList.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { student ->
                    val docId = if (student.id.isNotBlank()) student.id else UUID.randomUUID().toString()
                    val docRef = db.collection("users").document(userId).collection("students").document(docId)
                    val payload = mutableMapOf<String, Any>(
                        "userId" to userId,
                        "courseId" to student.courseId,
                        "department" to student.department.trim(),
                        "section" to student.section.trim(),
                        "rollNumber" to student.rollNumber.trim(),
                        "name" to student.name.trim(),
                        "assignmentQuizTotal" to student.assignmentQuizTotal,
                        "midtermScore" to student.midtermScore,
                        "finalTermScore" to student.finalTermScore,
                        "totalAttendanceCount" to student.totalAttendanceCount,
                        "totalAttendancePossible" to student.totalAttendancePossible,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (student.email.isNotBlank()) {
                        payload["email"] = student.email.trim()
                    }
                    batch.set(docRef, payload, com.google.firebase.firestore.SetOptions.merge())
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun deleteStudent(userId: String, studentId: String) {
        val path = "users/$userId/students/$studentId"
        try {
            db.collection("users").document(userId).collection("students").document(studentId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    // ================= Assessments =================

    fun observeAssessments(userId: String, courseId: String? = null): Flow<List<Assessment>> {
        val path = "users/$userId/assessments"
        val collection = db.collection("users").document(userId).collection("assessments")
        val query = if (!courseId.isNullOrBlank()) {
            collection.whereEqualTo("courseId", courseId)
        } else {
            collection
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(Assessment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun saveAssessment(userId: String, assessment: Assessment): String {
        val docId = if (assessment.id.isNotBlank()) assessment.id else UUID.randomUUID().toString()
        val path = "users/$userId/assessments/$docId"
        val docRef = db.collection("users").document(userId).collection("assessments").document(docId)
        try {
            val exists = docRef.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "courseId" to assessment.courseId,
                "title" to assessment.title.trim(),
                "type" to assessment.type,
                "maxMarks" to assessment.maxMarks,
                "weightPercentage" to assessment.weightPercentage,
                "date" to assessment.date,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            return docId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun deleteAssessment(userId: String, assessmentId: String) {
        val path = "users/$userId/assessments/$assessmentId"
        try {
            db.collection("users").document(userId).collection("assessments").document(assessmentId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    // ================= Scores =================

    fun observeScores(userId: String, courseId: String? = null): Flow<List<Score>> {
        val path = "users/$userId/scores"
        val collection = db.collection("users").document(userId).collection("scores")
        val query = if (!courseId.isNullOrBlank()) {
            collection.whereEqualTo("courseId", courseId)
        } else {
            collection
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(Score::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun saveScore(userId: String, score: Score) {
        val docId = if (score.id.isNotBlank()) score.id else "${score.studentId}_${score.assessmentId}"
        val path = "users/$userId/scores/$docId"
        val docRef = db.collection("users").document(userId).collection("scores").document(docId)
        try {
            val exists = docRef.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "courseId" to score.courseId,
                "studentId" to score.studentId,
                "assessmentId" to score.assessmentId,
                "obtainedMarks" to score.obtainedMarks,
                "submitted" to score.submitted,
                "submissionNotes" to score.submissionNotes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun saveScoresBatch(userId: String, scoreList: List<Score>) {
        if (scoreList.isEmpty()) return
        val path = "users/$userId/scores"
        try {
            scoreList.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { score ->
                    val docId = if (score.id.isNotBlank()) score.id else "${score.studentId}_${score.assessmentId}"
                    val docRef = db.collection("users").document(userId).collection("scores").document(docId)
                    val payload = mutableMapOf<String, Any>(
                        "userId" to userId,
                        "courseId" to score.courseId,
                        "studentId" to score.studentId,
                        "assessmentId" to score.assessmentId,
                        "obtainedMarks" to score.obtainedMarks,
                        "submitted" to score.submitted,
                        "submissionNotes" to score.submissionNotes,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload, com.google.firebase.firestore.SetOptions.merge())
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    // ================= Attendance =================

    fun observeAttendanceRecords(userId: String, courseId: String? = null): Flow<List<AttendanceRecord>> {
        val path = "users/$userId/attendance_records"
        val collection = db.collection("users").document(userId).collection("attendance_records")
        val query = if (!courseId.isNullOrBlank()) {
            collection.whereEqualTo("courseId", courseId)
        } else {
            collection
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(AttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.date }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun saveAttendanceRecord(userId: String, record: AttendanceRecord): String {
        val docId = if (record.id.isNotBlank()) record.id else UUID.randomUUID().toString()
        val path = "users/$userId/attendance_records/$docId"
        val docRef = db.collection("users").document(userId).collection("attendance_records").document(docId)
        try {
            val exists = docRef.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "courseId" to record.courseId,
                "date" to record.date,
                "topic" to record.topic,
                "studentStatuses" to record.studentStatuses,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            return docId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }
}
