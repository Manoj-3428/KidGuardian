package com.example.phonelock.utils

import android.content.Context
import android.content.pm.PackageManager
import java.util.Locale

fun getAppDisplayName(context: Context, packageName: String): String {
    val packageNameLower = packageName.lowercase()
    
    val packageMappings = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.instagram.android" to "Instagram",
        "com.facebook.katana" to "Facebook",
        "com.linkedin.android" to "LinkedIn",
        "com.twitter.android" to "Twitter",
        "com.google.android.youtube" to "YouTube",
        "com.snapchat.android" to "Snapchat",
        "com.tiktok.android" to "TikTok",
        "com.google.android.apps.messaging" to "Messages"
    )
    
    packageMappings[packageNameLower]?.let { return it }
    
    when {
        packageNameLower.contains("whatsapp") -> return "WhatsApp"
        packageNameLower.contains("instagram") -> return "Instagram"
        packageNameLower.contains("linkedin") -> return "LinkedIn"
        packageNameLower.contains("facebook") -> return "Facebook"
        packageNameLower.contains("snapchat") -> return "Snapchat"
        packageNameLower.contains("tiktok") -> return "TikTok"
        packageNameLower.contains("twitter") -> return "Twitter"
        packageNameLower.contains("youtube") -> return "YouTube"
    }
    
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        val lastPart = packageName.substringAfterLast('.', packageName)
        lastPart.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
    }
}

fun groupAppsIntoCategories(appList: List<Pair<String, Int>>, context: Context): Map<String, Int> {
    val categoryCounts = mutableMapOf<String, Int>()
    
    appList.forEach { (packageName, count) ->
        val appName = getAppDisplayName(context, packageName)
        val appNameLower = appName.lowercase()
        val packageNameLower = packageName.lowercase()
        
        val category = when {
            packageNameLower.contains("com.whatsapp") || appNameLower.contains("whatsapp") -> "WhatsApp"
            packageNameLower.contains("com.instagram") || appNameLower.contains("instagram") -> "Instagram"
            packageNameLower.contains("com.linkedin") || appNameLower.contains("linkedin") -> "LinkedIn"
            packageNameLower.contains("chrome") || appNameLower.contains("chrome") -> "Chrome"
            packageNameLower.contains("com.facebook") || appNameLower.contains("facebook") -> "Facebook"
            else -> "Others"
        }
        categoryCounts[category] = (categoryCounts[category] ?: 0) + count
    }
    
    return categoryCounts
}

