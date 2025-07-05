package com.thanhnguyen.git.format.util

/**
 * Utility class để parse và format thời gian
 * Hỗ trợ các định dạng:
 * - 30 (mặc định phút)
 * - 30m (phút)
 * - 1h (giờ)
 * - 1.5h hoặc 1,5h (giờ với phần thập phân)
 * - 90m sẽ được hiển thị là 1h30m
 */
object TimeParser {
    
    /**
     * Parse input string thành số phút
     * @param input Chuỗi đầu vào (vd: "30", "1h", "1.5h", "90m")
     * @return Số phút, hoặc null nếu không parse được
     */
    fun parseToMinutes(input: String): Int? {
        if (input.isBlank()) return null

        val cleanInput = input.trim().lowercase()

        return try {
            when {
                // Phút với hậu tố 'm'
                cleanInput.endsWith("m") -> {
                    val numberPart = cleanInput.dropLast(1)
                    if (numberPart.matches(Regex("^\\d+$"))) {
                        numberPart.toInt()
                    } else {
                        // Fallback: extract số đầu tiên từ toàn bộ input
                        val numberMatch = Regex("(\\d+)").find(cleanInput)
                        numberMatch?.groupValues?.get(1)?.toIntOrNull()
                    }
                }

                // Giờ với hậu tố 'h'
                cleanInput.endsWith("h") -> {
                    val numberPart = cleanInput.dropLast(1)
                    when {
                        // Số nguyên
                        numberPart.matches(Regex("^\\d+$")) -> {
                            val hours = numberPart.toInt()
                            hours * 60
                        }
                        // Số thập phân với dấu chấm
                        numberPart.matches(Regex("^\\d+\\.\\d+$")) -> {
                            val hours = numberPart.toDouble()
                            (hours * 60).toInt()
                        }
                        // Số thập phân với dấu phẩy
                        numberPart.matches(Regex("^\\d+,\\d+$")) -> {
                            val hours = numberPart.replace(",", ".").toDouble()
                            (hours * 60).toInt()
                        }
                        else -> {
                            // Fallback: extract số đầu tiên từ toàn bộ input
                            val numberMatch = Regex("(\\d+)").find(cleanInput)
                            numberMatch?.groupValues?.get(1)?.toIntOrNull()
                        }
                    }
                }

                // Chỉ có số (mặc định là phút)
                cleanInput.matches(Regex("^\\d+$")) -> {
                    cleanInput.toInt()
                }

                // Fallback: Extract số nguyên đầu tiên từ string bất kỳ
                else -> {
                    val numberMatch = Regex("(\\d+)").find(cleanInput)
                    numberMatch?.groupValues?.get(1)?.toIntOrNull()
                }
            }
        } catch (e: Exception) {
            // Fallback cuối cùng: Extract số nguyên đầu tiên
            try {
                val numberMatch = Regex("(\\d+)").find(cleanInput)
                numberMatch?.groupValues?.get(1)?.toIntOrNull()
            } catch (e2: Exception) {
                null
            }
        }
    }
    
    /**
     * Format số phút thành chuỗi hiển thị đẹp
     * @param minutes Số phút
     * @return Chuỗi format (vd: "30m", "1h30m", "2h")
     */
    fun formatMinutes(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}m"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                "${hours}h${remainingMinutes}m"
            }
        }
    }
    
    /**
     * Validate và format input string
     * @param input Chuỗi đầu vào
     * @return Chuỗi đã format hoặc null nếu không hợp lệ
     */
    fun validateAndFormat(input: String): String? {
        val minutes = parseToMinutes(input)
        return if (minutes != null && minutes > 0) {
            formatMinutes(minutes)
        } else null
    }
    
    /**
     * Kiểm tra xem input có hợp lệ không
     * @param input Chuỗi đầu vào  
     * @return true nếu hợp lệ
     */
    fun isValid(input: String): Boolean {
        return parseToMinutes(input) != null
    }
    
    /**
     * Lấy các ví dụ format hợp lệ
     * @return Danh sách các ví dụ
     */
    fun getExamples(): List<String> {
        return listOf(
            "30 (30 phút)",
            "90m (1h30m)", 
            "1h (1 giờ)",
            "1.5h (1h30m)",
            "2,5h (2h30m)"
        )
    }
}