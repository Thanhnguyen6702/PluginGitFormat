package com.thanhnguyen.git.format

import com.thanhnguyen.git.format.constant.GitConstant
import junit.framework.TestCase
import java.util.stream.IntStream

class HeaderTest : TestCase() {

    fun testCase1() {
        val message = "feat(compile): hello world"
        val matcher = GitConstant.HEADER_PATTERN.matcher(message)
        matcher.find()

        assertTrue(matcher.group(1) == "feat")
        assertTrue(matcher.group(3) == "compile")
        assertTrue(matcher.group(5) == "hello world")
    }

    fun testCase2() {
        val message = "feat(compile): "
        val matcher = GitConstant.HEADER_PATTERN.matcher(message)
        matcher.find()

        assertTrue(matcher.group(1) == "feat")
        assertTrue(matcher.group(3) == "compile")
        assertTrue(matcher.group(5) == null)
    }

    fun testCase3() {
        val message = "feat(): hello world"
        val matcher = GitConstant.HEADER_PATTERN.matcher(message)
        matcher.find()

        assertTrue(matcher.group(1) == "feat")
        assertTrue(matcher.group(3) == null)
        assertTrue(matcher.group(5) == "hello world")
    }

    fun testCase4() {
        val message = "feat: "
        val matcher = GitConstant.HEADER_PATTERN.matcher(message)
        matcher.find()

        assertTrue(matcher.group(1) == "feat")
        assertTrue(matcher.group(3) == null)
        assertTrue(matcher.group(5) == null)
    }

    fun testCase5() {
        val message = "feat(compile)：hello world"
        val matcher = GitConstant.HEADER_PATTERN.matcher(message)
        matcher.find()

        assertTrue(matcher.group(1) == "feat")
        assertTrue(matcher.group(3) == "compile")
        assertTrue(matcher.group(5) == "hello world")
    }

    fun testCase6() {
        val message = "FEAT(compile)：hello world"
        val matcher = GitConstant.HEADER_PATTERN.matcher(message)
        matcher.find()

        assertTrue(matcher.group(1) == "FEAT")
        assertTrue(matcher.group(3) == "compile")
        assertTrue(matcher.group(5) == "hello world")
    }

    fun testCase7() {
        val message = "1234(compile)：hello world"
        val matcher = GitConstant.HEADER_PATTERN.matcher(message)
        matcher.find()

        assertTrue(matcher.group(1) == "1234")
        assertTrue(matcher.group(3) == "compile")
        assertTrue(matcher.group(5) == "hello world")
    }

    fun testCase8() {
        val message = "功能(compile)：hello world"
        val matcher = GitConstant.HEADER_PATTERN.matcher(message)
        matcher.find()

        assertTrue(matcher.group(1) == "功能")
        assertTrue(matcher.group(3) == "compile")
        assertTrue(matcher.group(5) == "hello world")
    }


    fun test() {
        val message = "feat(compile): hello world"
        println(message)
        val matcher = GitConstant.HEADER_PATTERN.matcher(message)

        if (matcher.find()) {
            IntStream.rangeClosed(0, matcher.groupCount())
                .mapToObj { i: Int -> i.toString() + ". " + matcher.group(i) }
                .forEach { println(it) }
        }
    }
}
