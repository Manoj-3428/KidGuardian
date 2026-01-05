package com.example.phonelock.repository

import android.content.Context
import android.util.Log
import com.example.phonelock.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.*

class UserRepository(val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val childrenCollection = firestore.collection("children")
    private val parentsCollection = firestore.collection("parents")
    private val complaintsCollection = firestore.collection("complaints")
    
    companion object {
        private const val TAG = "UserRepository"
        private const val PREFS_NAME = "KidGuardianPrefs"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_HAS_PROFILE = "has_profile"
        private const val KEY_CHILD_ID = "child_id"
        private const val KEY_PARENT_ID = "parent_id"
        const val PASSCODE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes in milliseconds
    }
    
    // Generate unique child ID
    fun generateChildId(): String {
        return UUID.randomUUID().toString().substring(0, 8).uppercase()
    }
    
    // Generate unique secret code for each complaint
    fun generateSecretCode(): String {
        return (100000..999999).random().toString()
    }
    
    // Save user role locally
    fun saveUserRole(role: UserRole) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_ROLE, role.name)
            .apply()
    }
    
    // Get saved user role
    fun getUserRole(): UserRole? {
        val roleName = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ROLE, null)
        return roleName?.let { UserRole.valueOf(it) }
    }
    
    // Save profile completion status
    fun setProfileCompleted(completed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_PROFILE, completed)
            .apply()
    }
    
    // Check if profile is completed
    fun hasCompletedProfile(): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_PROFILE, false)
    }
    
    // Save child ID locally
    fun saveChildId(childId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CHILD_ID, childId)
            .apply()
    }
    
    // Get saved child ID
    fun getChildId(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CHILD_ID, null)
    }
    
    // Save parent ID locally
    fun saveParentId(parentId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PARENT_ID, parentId)
            .apply()
    }
    
    // Get saved parent ID
    fun getParentId(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PARENT_ID, null)
    }
    
    // Clear all local data
    fun clearLocalData() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
    
    // CHILD OPERATIONS
    
    suspend fun createChildAccount(name: String, email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Failed to create user")
            val childId = generateChildId()
            
            val child = Child(
                childId = childId,
                userId = userId,
                name = name,
                email = email
            )
            
            childrenCollection.document(userId).set(child).await()
            saveChildId(childId)
            setProfileCompleted(true) // Mark profile as complete since name was provided
            Result.success(childId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating child account", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateChildProfile(name: String, age: Int, gender: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            
            childrenCollection.document(userId).update(
                mapOf(
                    "name" to name,
                    "age" to age,
                    "gender" to gender
                )
            ).await()
            
            setProfileCompleted(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating child profile", e)
            Result.failure(e)
        }
    }
    
    suspend fun getChildData(): Result<Child> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            
            val doc = childrenCollection.document(userId).get().await()
            val child = doc.toObject(Child::class.java) ?: throw Exception("Child data not found")
            
            Result.success(child)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting child data", e)
            Result.failure(e)
        }
    }
    
    suspend fun getChildByChildId(childId: String): Result<Child> {
        return try {
            val querySnapshot = childrenCollection
                .whereEqualTo("childId", childId)
                .get()
                .await()
            
            if (querySnapshot.documents.isEmpty()) {
                throw Exception("Child not found with ID: $childId")
            }
            
            val child = querySnapshot.documents[0].toObject(Child::class.java)
                ?: throw Exception("Failed to parse child data")
            
            Result.success(child)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting child by ID", e)
            Result.failure(e)
        }
    }
    
    // PARENT OPERATIONS
    
    suspend fun linkParentWithChild(childId: String): Result<Unit> {
        return try {
            // First verify child exists
            val childResult = getChildByChildId(childId)
            if (childResult.isFailure) {
                throw Exception("Invalid Child ID")
            }
            
            // Create a temporary parent account (no email/password needed)
            val parentId = UUID.randomUUID().toString()
            
            val parent = Parent(
                parentId = parentId,
                name = "Parent", // Default name - needs to be updated
                email = "", // No email needed
                linkedChildId = childId,
                relationship = "Parent" // Default relationship - needs to be updated
            )
            
            parentsCollection.document(parentId).set(parent).await()
            
            // Update child's linkedParents list
            val child = childResult.getOrThrow()
            val updatedParents = child.linkedParents.toMutableList()
            updatedParents.add(parentId)
            
            childrenCollection.document(child.userId).update(
                mapOf(
                    "linkedParents" to updatedParents,
                    "parentLinked" to true
                )
            ).await()
            
            // Save parent ID locally for authentication
            saveParentId(parentId)
            
            // Set profile as NOT completed so parent has to complete it
            setProfileCompleted(false)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error linking parent with child", e)
            Result.failure(e)
        }
    }
    
    
    suspend fun getParentData(): Result<Parent> {
        return try {
            // Try Firebase auth first, then fall back to saved parent ID
            val userId = auth.currentUser?.uid ?: getParentId() ?: throw Exception("Not authenticated")
            
            val doc = parentsCollection.document(userId).get().await()
            val parent = doc.toObject(Parent::class.java) ?: throw Exception("Parent data not found")
            
            Result.success(parent)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting parent data", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateParentProfile(name: String, relationship: String): Result<Unit> {
        return try {
            // Try Firebase auth first, then fall back to saved parent ID
            val userId = auth.currentUser?.uid ?: getParentId() ?: throw Exception("Not authenticated")
            
            parentsCollection.document(userId).update(
                mapOf(
                    "name" to name,
                    "relationship" to relationship
                )
            ).await()
            
            setProfileCompleted(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating parent profile", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateMonitoringStatus(enabled: Boolean): Result<Unit> {
        return try {
            // Try Firebase auth first, then fall back to saved parent ID
            val userId = auth.currentUser?.uid ?: getParentId() ?: throw Exception("Not authenticated")
            
            parentsCollection.document(userId).update("monitoringEnabled", enabled).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating monitoring status", e)
            Result.failure(e)
        }
    }
    
    // COMPLAINT OPERATIONS
    
    suspend fun createComplaint(
        childId: String,
        detectedWord: String,
        screenshotUrl: String,
        appName: String,
        category: String
    ): Result<String> {
        return try {
            val complaintId = UUID.randomUUID().toString()
            val secretCode = generateSecretCode()

            val complaint = Complaint(
                complaintId = complaintId,
                childId = childId,
                detectedWord = detectedWord,
                screenshotUrl = screenshotUrl,
                appName = appName,
                category = category,
                secretCode = secretCode
            )

            complaintsCollection.document(complaintId).set(complaint).await()
            Result.success(complaintId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating complaint", e)
            Result.failure(e)
        }
    }
    
    suspend fun createComplaintWithScreenshot(
        childId: String,
        detectedWord: String,
        appName: String,
        category: String
    ): Result<String> {
        return try {
            val complaintId = UUID.randomUUID().toString()
            val secretCode = generateSecretCode()
            
            // Capture screenshot
            val screenshotUrl = captureAndUploadScreenshot(complaintId)
            
            val complaint = Complaint(
                complaintId = complaintId,
                childId = childId,
                detectedWord = detectedWord,
                screenshotUrl = screenshotUrl,
                appName = appName,
                category = category,
                secretCode = secretCode
            )

            complaintsCollection.document(complaintId).set(complaint).await()
            Result.success(complaintId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating complaint with screenshot", e)
            Result.failure(e)
        }
    }
    
    private suspend fun captureAndUploadScreenshot(complaintId: String): String {
        return try {
            // For now, return a placeholder URL
            // In a real implementation, you would:
            // 1. Capture screenshot using MediaProjection API
            // 2. Upload to Firebase Storage
            // 3. Return the download URL
            "https://firebasestorage.googleapis.com/screenshots/$complaintId.jpg"
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screenshot", e)
            ""
        }
    }
    
    suspend fun getComplaintById(complaintId: String): Result<Complaint> {
        return try {
            val doc = complaintsCollection.document(complaintId).get().await()
            val complaint = doc.toObject(Complaint::class.java) ?: throw Exception("Complaint not found")
            Result.success(complaint)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting complaint", e)
            Result.failure(e)
        }
    }
    
    suspend fun markComplaintAsAccessed(complaintId: String): Result<Unit> {
        return try {
            complaintsCollection.document(complaintId).update(
                mapOf(
                    "accessed" to true,
                    "unlockTimestamp" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking complaint as accessed", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateComplaintScreenshot(complaintId: String, screenshotUrl: String): Result<Unit> {
        return try {
            complaintsCollection.document(complaintId).update(
                mapOf("screenshotUrl" to screenshotUrl)
            ).await()
            Log.d(TAG, "Screenshot URL updated for complaint: $complaintId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating screenshot URL", e)
            Result.failure(e)
        }
    }
    
    suspend fun getComplaintsForChild(childId: String): Result<List<Complaint>> {
        return try {
            val querySnapshot = complaintsCollection
                .whereEqualTo("childId", childId)
                .get()
                .await()
            
            // Sort in memory instead of using Firestore orderBy to avoid index requirement
            val complaints = querySnapshot.documents.mapNotNull { 
                it.toObject(Complaint::class.java) 
            }.sortedByDescending { it.timestamp }
            
            Result.success(complaints)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting complaints", e)
            Result.failure(e)
        }
    }
    
    // AUTHENTICATION
    
    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in", e)
            Result.failure(e)
        }
    }
    
    fun getCurrentUserId(): String? {
        // Return Firebase user ID or parent ID (for parents who login via Child ID)
        return auth.currentUser?.uid ?: getParentId()
    }
    
    fun isUserLoggedIn(): Boolean {
        // Check if Firebase user is logged in OR if parent ID is stored (for parents who login via Child ID)
        return auth.currentUser != null || getParentId() != null
    }
    
    fun logout() {
        auth.signOut()
        clearLocalData()
    }
    
    
    // PARENT-CHILD LINKING
    
    // Save parent to Firestore
    suspend fun saveParentToFirestore(parent: com.example.phonelock.models.Parent): Result<Unit> {
        return try {
            parentsCollection.document(parent.parentId).set(parent).await()
            Log.d(TAG, "✅ Parent saved to Firestore successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving parent to Firestore", e)
            Result.failure(e)
        }
    }
    
    // Generate 6-digit passcode for linking
    fun generateLinkPasscode(): String {
        return (100000..999999).random().toString()
    }
    
    // Check if passcode is expired (older than 5 minutes)
    fun isPasscodeExpired(generatedAt: Long?): Boolean {
        if (generatedAt == null) return true
        val currentTime = System.currentTimeMillis()
        return (currentTime - generatedAt) > PASSCODE_EXPIRY_MS
    }
    
    // Get user (child) by their unique ID
    suspend fun getUserById(childId: String): Result<Child> {
        return try {
            val querySnapshot = childrenCollection
                .whereEqualTo("childId", childId)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                Result.failure(Exception("User not found"))
            } else {
                val child = querySnapshot.documents[0].toObject(Child::class.java)
                if (child != null) {
                    Result.success(child)
                } else {
                    Result.failure(Exception("Failed to parse user data"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID", e)
            Result.failure(e)
        }
    }
    
    // Link parent to child (allows multiple parents)
    suspend fun linkParentToChild(parentId: String, childId: String): Result<Unit> {
        return try {
            val querySnapshot = childrenCollection
                .whereEqualTo("childId", childId)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Child not found"))
            }
            
            val childDoc = querySnapshot.documents[0]
            val child = childDoc.toObject(com.example.phonelock.models.Child::class.java)
            
            if (child == null) {
                return Result.failure(Exception("Invalid child data"))
            }
            
            // Get existing linked parents list
            val linkedParents = child.linkedParents.toMutableList()
            
            // Add new parent if not already linked
            if (!linkedParents.contains(parentId)) {
                linkedParents.add(parentId)
                
                // Update child document with new parent list
                childrenCollection.document(childDoc.id)
                    .update(
                        mapOf(
                            "linkedParents" to linkedParents,
                            "parentLinked" to true
                        )
                    )
                    .await()
                
                Log.d(TAG, "✅ Parent $parentId linked to child $childId. Total parents: ${linkedParents.size}")
            } else {
                Log.d(TAG, "Parent $parentId already linked to child $childId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error linking parent to child", e)
            Result.failure(e)
        }
    }
    
    // Clear link passcode after successful linking
    suspend fun clearLinkPasscode(childId: String): Result<Unit> {
        return try {
            val querySnapshot = childrenCollection
                .whereEqualTo("childId", childId)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Child not found"))
            }
            
            val childDocId = querySnapshot.documents[0].id
            childrenCollection.document(childDocId)
                .update(
                    mapOf(
                        "linkPasscode" to null,
                        "passcodeGeneratedAt" to null
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing link passcode", e)
            Result.failure(e)
        }
    }
    
    // Generate and save link passcode for child (with timestamp for 5-min expiry)
    suspend fun generateAndSaveLinkPasscode(childId: String): Result<String> {
        return try {
            val passcode = generateLinkPasscode()
            val currentTime = System.currentTimeMillis()
            
            val querySnapshot = childrenCollection
                .whereEqualTo("childId", childId)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Child not found"))
            }
            
            val childDocId = querySnapshot.documents[0].id
            childrenCollection.document(childDocId)
                .update(
                    mapOf(
                        "linkPasscode" to passcode,
                        "passcodeGeneratedAt" to currentTime
                    )
                )
                .await()
            
            Result.success(passcode)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating link passcode", e)
            Result.failure(e)
        }
    }
    
    // Request new passcode for child (generates fresh code when parent requests)
    suspend fun requestLinkPasscode(childId: String): Result<Unit> {
        return try {
            val passcode = generateLinkPasscode()
            val currentTime = System.currentTimeMillis()
            
            val querySnapshot = childrenCollection
                .whereEqualTo("childId", childId)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Child not found"))
            }
            
            // Allow multiple parents to link - no restriction check needed
            val child = querySnapshot.documents[0].toObject(Child::class.java)
            if (child == null) {
                return Result.failure(Exception("Invalid child data"))
            }
            
            val childDocId = querySnapshot.documents[0].id
            childrenCollection.document(childDocId)
                .update(
                    mapOf(
                        "linkPasscode" to passcode,
                        "passcodeGeneratedAt" to currentTime
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Passcode generated and sent to child dashboard (expires in 5 min)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting link passcode", e)
            Result.failure(e)
        }
    }
    
    // Generate logout passcode for child logout verification
    suspend fun generateAndSaveLogoutPasscode(childId: String): Result<String> {
        return try {
            val passcode = generateLinkPasscode()
            val currentTime = System.currentTimeMillis()
            
            val querySnapshot = childrenCollection
                .whereEqualTo("childId", childId)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Child not found"))
            }
            
            val childDoc = querySnapshot.documents[0]
            childrenCollection.document(childDoc.id)
                .update(
                    mapOf(
                        "logoutPasscode" to passcode,
                        "logoutPasscodeGeneratedAt" to currentTime
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Logout passcode generated and saved for child: $childId")
            Result.success(passcode)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating logout passcode", e)
            Result.failure(e)
        }
    }
    
    // Clear logout passcode after successful logout
    suspend fun clearLogoutPasscode(childId: String): Result<Unit> {
        return try {
            val querySnapshot = childrenCollection
                .whereEqualTo("childId", childId)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Child not found"))
            }
            
            val childDoc = querySnapshot.documents[0]
            childrenCollection.document(childDoc.id)
                .update(
                    mapOf(
                        "logoutPasscode" to null,
                        "logoutPasscodeGeneratedAt" to null
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Logout passcode cleared for child: $childId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing logout passcode", e)
            Result.failure(e)
        }
    }
    
    // Check if logout passcode is expired
    fun isLogoutPasscodeExpired(generatedAt: Long?): Boolean {
        if (generatedAt == null) return true
        val currentTime = System.currentTimeMillis()
        return (currentTime - generatedAt) > PASSCODE_EXPIRY_MS
    }
}

