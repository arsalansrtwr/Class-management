package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TeacherProfile
import com.example.ui.theme.BorderMedium
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GradeInfo
import com.example.ui.theme.GradeInfoBg
import com.example.ui.theme.ProfNavy
import com.example.ui.theme.ProfNavyDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ProfViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherSetupScreen(
    viewModel: ProfViewModel,
    existingProfile: TeacherProfile? = null,
    onSetupComplete: () -> Unit
) {
    val authUser = Firebase.auth.currentUser
    var name by remember { mutableStateOf(existingProfile?.name ?: authUser?.displayName ?: "Dr. ") }
    var email by remember { mutableStateOf(existingProfile?.email ?: authUser?.email ?: "") }
    var primaryDepartment by remember { mutableStateOf(existingProfile?.primaryDepartment ?: "Computer Science") }

    val defaultSuggestedDepartments = listOf(
        "Computer Science",
        "Software Engineering",
        "Electrical Engineering",
        "Information Technology",
        "Data Science",
        "Mechanical Engineering",
        "Business Administration"
    )

    val selectedDepartments = remember {
        mutableStateListOf<String>().apply {
            if (existingProfile != null && existingProfile.departments.isNotEmpty()) {
                addAll(existingProfile.departments)
            } else {
                addAll(listOf("Computer Science", "Software Engineering"))
            }
        }
    }

    var customDeptInput by remember { mutableStateOf("") }

    val subjects = remember {
        mutableStateListOf<String>().apply {
            if (existingProfile != null && existingProfile.subjects.isNotEmpty()) {
                addAll(existingProfile.subjects)
            } else {
                addAll(listOf("Data Structures & Algorithms", "Database Systems"))
            }
        }
    }

    var newSubjectInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GradeInfoBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = ProfNavy,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Professor Onboarding",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProfNavyDark
                )
                Text(
                    text = "Configure your department and teaching portfolio",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Step 1: Professor Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Personal Information",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfNavy
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Professor Full Name & Title") },
                    placeholder = { Text("e.g. Dr. Alex Vance, Ph.D.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prof_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProfNavy,
                        unfocusedBorderColor = BorderMedium,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Institutional Email") },
                    placeholder = { Text("e.g. professor@university.edu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProfNavy,
                        unfocusedBorderColor = BorderMedium,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = primaryDepartment,
                    onValueChange = { primaryDepartment = it },
                    label = { Text("Primary Department") },
                    placeholder = { Text("e.g. Computer Science") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("primary_dept_input"),
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

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Subjects Teaching
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Subjects You Are Teaching",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfNavy
                )
                Text(
                    text = "Add courses and topics in your curriculum",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { subject ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GradeInfoBg)
                                .border(1.dp, ProfNavy.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = subject,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ProfNavyDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove subject",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { subjects.remove(subject) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSubjectInput,
                        onValueChange = { newSubjectInput = it },
                        placeholder = { Text("Add subject (e.g. Operating Systems)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_subject_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfNavy,
                            unfocusedBorderColor = BorderMedium,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newSubjectInput.isNotBlank() && !subjects.contains(newSubjectInput.trim())) {
                                subjects.add(newSubjectInput.trim())
                                newSubjectInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(ProfNavy, RoundedCornerShape(8.dp))
                            .testTag("add_subject_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Subject",
                            tint = PureWhite
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Departments Taking Classes
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3. Departments Taking Classes With You",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfNavy
                )
                Text(
                    text = "Select all academic departments whose students you teach",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    defaultSuggestedDepartments.forEach { dept ->
                        val isSelected = selectedDepartments.contains(dept)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ProfNavy else PureWhite)
                                .border(
                                    1.dp,
                                    if (isSelected) ProfNavy else BorderMedium,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedDepartments.remove(dept)
                                    } else {
                                        selectedDepartments.add(dept)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = dept,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) PureWhite else TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customDeptInput,
                        onValueChange = { customDeptInput = it },
                        placeholder = { Text("Add other department...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_dept_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfNavy,
                            unfocusedBorderColor = BorderMedium,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (customDeptInput.isNotBlank() && !selectedDepartments.contains(customDeptInput.trim())) {
                                selectedDepartments.add(customDeptInput.trim())
                                customDeptInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(ProfNavy, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Department",
                            tint = PureWhite
                        )
                    }
                }
            }
        }

        if (validationError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = validationError ?: "",
                color = Color(0xFFDC2626),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Save & Continue Button
        Button(
            onClick = {
                if (name.isBlank()) {
                    validationError = "Please enter professor name"
                    return@Button
                }
                if (primaryDepartment.isBlank()) {
                    validationError = "Please enter primary department"
                    return@Button
                }
                if (subjects.isEmpty()) {
                    validationError = "Please add at least one subject taught"
                    return@Button
                }
                if (selectedDepartments.isEmpty()) {
                    selectedDepartments.add(primaryDepartment)
                }

                isSaving = true
                validationError = null
                viewModel.saveTeacherProfile(
                    name = name,
                    email = email,
                    primaryDepartment = primaryDepartment,
                    subjects = subjects.toList(),
                    departments = selectedDepartments.toList()
                )
                onSetupComplete()
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_profile_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = ProfNavy,
                contentColor = PureWhite
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PureWhite,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Save Profile & Launch Gradebook",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
