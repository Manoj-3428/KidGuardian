package com.example.phonelock.models

import com.google.firebase.Timestamp

data class Child(
    val childId: String = "",  // Unique ID generated at signup
    val userId: String = "",   // Firebase Auth UID
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val email: String = "",
    val parentLinked: Boolean = false,
    val linkedParents: List<String> = emptyList(), // List of parent UIDs
    val parentId: String? = null, // Primary parent's Firebase Auth UID
    val linkPasscode: String? = null, // 6-digit passcode for parent linking (expires after 5 min)
    val passcodeGeneratedAt: Long? = null, // Timestamp when passcode was generated
    val logoutPasscode: String? = null, // 6-digit passcode for child logout verification (expires after 5 min)
    val logoutPasscodeGeneratedAt: Long? = null, // Timestamp when logout passcode was generated
    val createdAt: Long = System.currentTimeMillis()
)

data class Parent(
    val parentId: String = "",  // Firebase Auth UID
    val name: String = "",
    val email: String = "",
    val linkedChildId: String = "", // The child's unique ID
    val relationship: String = "", // "Mother", "Father", "Guardian"
    val monitoringEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class Complaint(
    val complaintId: String = "",
    val childId: String = "",
    val detectedWord: String = "",
    val screenshotUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val accessed: Boolean = false, // true if child unlocked after this complaint
    val unlockTimestamp: Long? = null,
    val appName: String = "",
    val category: String = "", // "offensive", "violence", "sexual", etc.
    val secretCode: String = "" // Unique code for this specific complaint
)

enum class UserRole {
    CHILD,
    PARENT
}

data class UserPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val role: UserRole? = null
)

