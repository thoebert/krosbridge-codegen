package com.github.thoebert.krosbridgecodegen

import com.github.thoebert.krosbridgecodegen.parseFields
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class ParserKtTest {

    @Test
    fun parserTest() {
        assertEquals(listOf(listOf(Field("float32", "temperature"))), parseFields("""
            float32 temperature
        """)
        )
        assertEquals(listOf(listOf(Field("float", "temperature", "3"))), parseFields("""
            float temperature = 3
        """)
        )
        assertEquals(listOf(listOf(Field("float", "temperature", "3"))), parseFields("""# startcomment #= ./
            float temperature = 3 # startcomment #= ./
        """)
        )
        assertEquals(listOf(listOf(
            Field("float", "temperature"),
            Field("float32", "temperature2", "3")
        )), parseFields("""
            float temperature # = comment
            float32 temperature2 = 3 # comment2
        """)
        )
    }
}