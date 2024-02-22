package com.github.thoebert.krosbridgecodegen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ParserKtTest {

    @Test
    fun parserSimpleTypesTest() {
        assertEquals(
            listOf(listOf(Field(Type("float32"), "temperature"))), parseFields(
                """
            float32 temperature
        """
            )
        )
        assertEquals(
            listOf(listOf(Field(Type("float"), "temperature", value = "3"))), parseFields(
                """
            float temperature = 3
        """
            )
        )
        assertEquals(
            listOf(listOf(Field(Type("float"), "temperature", value = "3"))), parseFields(
                """# startcomment #= ./
            float temperature = 3 # startcomment #= ./
        """
            )
        )
        assertEquals(
            listOf(
                listOf(
                    Field(Type("float"), "temperature"),
                    Field(Type("float32"), "temperature2", value = "3")
                )
            ), parseFields(
                """
            float temperature # = comment
            float32 temperature2 = 3 # comment2
        """
            )
        )
    }

    @Test
    fun parserComplexTypesTest() {
        assertEquals(
            listOf(
                listOf(
                    Field(
                        Type("Vector3"),
                        "linear",
                        children = mutableListOf(
                            Field(Type("float64"), "x"),
                            Field(Type("float64"), "y"),
                            Field(Type("float64"), "z"),
                        )
                    ),
                    Field(
                        Type("Vector3"),
                        "angular",
                        children = mutableListOf(
                            Field(Type("float64"), "x"),
                            Field(Type("float64"), "y"),
                            Field(Type("float64"), "z"),
                        )
                    )
                )
            ),
            parseFields(
                """
Vector3  linear
	float64 x
	float64 y
	float64 z
Vector3  angular
	float64 x
	float64 y
	float64 z"""
            )
        )


    }
}