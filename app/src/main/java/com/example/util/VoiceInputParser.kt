package com.example.util

import java.util.Locale

object VoiceInputParser {

    private val BENGALI_DIGITS = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private val BENGALI_NUMBER_WORDS = mapOf(
        "এক" to 1.0, "দুই" to 2.0, "তিন" to 3.0, "চার" to 4.0, "পাঁচ" to 5.0,
        "ছয়" to 6.0, "ছয়" to 6.0, "সাত" to 7.0, "আট" to 8.0, "নয়" to 9.0, "নয়" to 9.0, "দশ" to 10.0,
        "এগারো" to 11.0, "বারো" to 12.0, "তেরো" to 13.0, "চৌদ্দ" to 14.0, "পনেরো" to 15.0,
        "ষোল" to 16.0, "সতেরো" to 17.0, "আঠারো" to 18.0, "উনিশ" to 19.0, "বিশ" to 20.0, "বীশ" to 20.0,
        "একুশ" to 21.0, "বাইশ" to 22.0, "তেইশ" to 23.0, "চব্বিশ" to 24.0, "পঁচিশ" to 25.0,
        "ছাব্বিশ" to 26.0, "সাতাশ" to 27.0, "আটাশ" to 28.0, "উনত্রিশ" to 29.0, "ত্রিশ" to 30.0, "তিশ" to 30.0,
        "একত্রিশ" to 31.0, "বত্রিশ" to 32.0, "তেত্রিশ" to 33.0, "চৌত্রিশ" to 34.0, "পঁয়ত্রিশ" to 35.0,
        "ছয়ত্রিশ" to 36.0, "সাইত্রিশ" to 37.0, "আটত্রিশ" to 38.0, "উনচল্লিশ" to 39.0, "চল্লিশ" to 40.0,
        "একচল্লিশ" to 41.0, "বিয়াল্লিশ" to 42.0, "তিচল্লিশ" to 43.0, "চৌয়াল্লিশ" to 44.0, "পঁয়তাল্লিশ" to 45.0,
        "ছেচল্লিশ" to 46.0, "সাতচল্লিশ" to 47.0, "আটচল্লিশ" to 48.0, "উনপঞ্চাশ" to 49.0, "পঞ্চাশ" to 50.0,
        "একান্ন" to 51.0, "বায়ান্ন" to 52.0, "তিপ্পান্ন" to 53.0, "চৌয়ান্ন" to 54.0, "পঞ্চান্ন" to 55.0,
        "ছাপ্পান্ন" to 56.0, "সাতান্ন" to 57.0, "আটান্ন" to 58.0, "উনষাট" to 59.0, "ষাট" to 60.0,
        "একষট্টি" to 61.0, "বাষট্টি" to 62.0, "তেষট্টি" to 63.0, "চৌষট্টি" to 64.0, "পঁয়ষট্টি" to 65.0,
        "ছেষট্টি" to 66.0, "সাতষট্টি" to 67.0, "আটষট্টি" to 68.0, "উনসত্তর" to 69.0, "সত্তর" to 70.0,
        "একাত্তর" to 71.0, "বাহাত্তর" to 72.0, "তিয়াত্তর" to 73.0, "চৌয়াত্তর" to 74.0, "পঁচাত্তর" to 75.0,
        "ছিয়াত্তর" to 76.0, "সাতাত্তর" to 77.0, "আটাত্তর" to 78.0, "উনআশি" to 79.0, "আশি" to 80.0,
        "একাশি" to 81.0, "বিraশি" to 82.0, "তিরাশি" to 83.0, "চৌরাশি" to 84.0, "পঁচাশী" to 85.0, "পঁচাশি" to 85.0,
        "ছিয়াশি" to 86.0, "সাতাশি" to 87.0, "আটাশি" to 88.0, "উননব্বই" to 89.0, "নব্বই" to 90.0,
        "একানব্বই" to 91.0, "বিরানব্বই" to 92.0, "তিরানব্বই" to 93.0, "চৌরানব্বই" to 94.0, "পঁচানব্বই" to 95.0,
        "ছিয়ানব্বই" to 96.0, "সাতানব্বই" to 97.0, "আটানব্বই" to 98.0, "নিরানব্বই" to 99.0,
        "একশ" to 100.0, "একশত" to 100.0, "দেড়শ" to 150.0, "দেড়শত" to 150.0, "আড়াইশ" to 250.0, "আড়াইশত" to 250.0,
        "দুইশ" to 200.0, "দুইশত" to 200.0, "তিনশ" to 300.0, "তিনশত" to 300.0, "চারশ" to 400.0, "চারশত" to 400.0,
        "পাঁচশ" to 500.0, "পাঁচশত" to 500.0, "হাজার" to 1000.0
    )

    private val CURRENCY_KEYWORDS = listOf(
        "টাকার", "টাকায়", "টাকায়", "টাকা", "takar", "taka", "tk", "rupees", "rupee"
    )

    private val NOISE_WORDS = listOf(
        "এর", "র", "দিয়ে", "দিয়ে", "করে", "for", "of", "and", "ও", "এবং", "কিনেছি", "কিনলাম"
    )

    fun parseVoiceInput(rawText: String): Pair<Double?, String?> {
        if (rawText.isBlank()) return Pair(null, null)

        // 1. Convert to lower case and replace Bengali digits with English digits
        var text = rawText.lowercase(Locale.getDefault()).trim()
        val convertedDigitsSb = StringBuilder()
        for (char in text) {
            convertedDigitsSb.append(BENGALI_DIGITS[char] ?: char)
        }
        text = convertedDigitsSb.toString()

        var extractedPrice: Double? = null

        // 2. Look for any explicit decimal or integer numeric sequences in the converted string
        val numericRegex = Regex("\\d+(\\.\\d+)?")
        val numericMatches = numericRegex.findAll(text).toList()

        // Track what text representing the price needs to be removed from the item name
        var priceSubstringToRemove: String? = null

        if (numericMatches.isNotEmpty()) {
            // Find the most appropriate number (sometimes it's next to "taka", "tk", or "টাকা")
            var bestMatch = numericMatches.first()
            for (match in numericMatches) {
                val index = match.range.first
                val end = match.range.last
                // check if words surrounding the match are currency indicators
                val substringAfter = text.substring(end + 1).trim()
                val isNearCurrency = CURRENCY_KEYWORDS.any { substringAfter.startsWith(it) }
                if (isNearCurrency) {
                    bestMatch = match
                    break
                }
            }
            extractedPrice = bestMatch.value.toDoubleOrNull()
            priceSubstringToRemove = bestMatch.value
        }

        // 3. If no digit sequence is found, search for written Bengali number words
        if (extractedPrice == null) {
            val sortedNumberWords = BENGALI_NUMBER_WORDS.keys.sortedByDescending { it.length }
            for (word in sortedNumberWords) {
                if (text.contains(word)) {
                    extractedPrice = BENGALI_NUMBER_WORDS[word]
                    priceSubstringToRemove = word
                    break
                }
            }
        }

        // 4. Extract Item Name by removing the price portion and any noise/currency words
        var cleanedText = text
        if (priceSubstringToRemove != null) {
            cleanedText = cleanedText.replaceFirst(priceSubstringToRemove, "")
        }

        // Remove currency words
        for (keyword in CURRENCY_KEYWORDS) {
            cleanedText = cleanedText.replace(Regex("\\b$keyword\\b"), " ")
            cleanedText = cleanedText.replace(keyword, " ") // for non-boundary matches in script languages
        }

        // Remove noise words
        for (noise in NOISE_WORDS) {
            cleanedText = cleanedText.replace(Regex("\\b$noise\\b"), " ")
            cleanedText = cleanedText.replace(noise, " ")
        }

        // Clean extra spaces
        cleanedText = cleanedText.replace(Regex("\\s+"), " ").trim()

        // Handle dangling possessive letters/words
        if (cleanedText.startsWith("র ") || cleanedText.startsWith("এর ")) {
            val spaceIndex = cleanedText.indexOf(' ')
            if (spaceIndex != -1) {
                cleanedText = cleanedText.substring(spaceIndex + 1).trim()
            }
        }
        if (cleanedText.endsWith(" র") || cleanedText.endsWith(" এর")) {
            val lastSpaceIndex = cleanedText.lastIndexOf(' ')
            if (lastSpaceIndex != -1) {
                cleanedText = cleanedText.substring(0, lastSpaceIndex).trim()
            }
        }

        val finalItemName = if (cleanedText.isNotEmpty()) cleanedText else null

        // Capitalize first letter if it's English
        val formattedItemName = finalItemName?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        return Pair(extractedPrice, formattedItemName)
    }
}
