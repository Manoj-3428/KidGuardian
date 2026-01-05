package com.example.phonelock

/**
 * Comprehensive abusive words database with categorization
 */
object AbusiveWordsDatabase {
    
    enum class Category(val displayName: String, val color: Int) {
        OFFENSIVE("Offensive Language", 0xFFEF4444.toInt()),     // Red
        VIOLENCE("Violence & Threats", 0xFFDC2626.toInt()),      // Dark Red
        SEXUAL("Sexual Content", 0xFFEC4899.toInt()),            // Pink
        SUBSTANCE("Substance Abuse", 0xFFF59E0B.toInt()),        // Orange
        BULLYING("Bullying & Harassment", 0xFF8B5CF6.toInt())   // Purple
    }
    
    // Categorized abusive words database
    private val wordsByCategory = mapOf(
        Category.OFFENSIVE to listOf(
            "bad", "stupid", "idiot", "dumb", "fool", "loser", 
            "moron", "jerk", "ass", "damn", "hell", "crap"
        ),
        
        Category.VIOLENCE to listOf(
            "kill", "die", "murder", "death", "dead", "shoot", 
            "stab", "attack", "fight", "beat", "hurt", "punch"
        ),
        
        Category.SEXUAL to listOf(
            "sex", "porn", "nude", "naked", "fuck", "dick",
            "pussy", "boobs", "xxx", "adult", "sexy"
        ),
        
        Category.SUBSTANCE to listOf(
            "drug", "weed", "marijuana", "cocaine", "heroin",
            "smoke", "alcohol", "drunk", "beer", "vodka"
        ),
        
        Category.BULLYING to listOf(
            "hate", "ugly", "fat", "worthless", "useless",
            "nobody", "pathetic", "failure", "reject", "trash"
        )
    )
    
    // Flat list of all abusive words
    val allWords: List<String> by lazy {
        wordsByCategory.values.flatten()
    }
    
    // Get category for a specific word
    fun getCategoryForWord(word: String): Category {
        wordsByCategory.forEach { (category, words) ->
            if (words.contains(word.lowercase())) {
                return category
            }
        }
        return Category.OFFENSIVE // Default
    }
    
    // Get all words in a specific category
    fun getWordsInCategory(category: Category): List<String> {
        return wordsByCategory[category] ?: emptyList()
    }
    
    // Get statistics for display
    fun getTotalWords(): Int = allWords.size
    
    fun getCategoryCount(): Int = Category.values().size
}

