package com.github.thoebert.krosbridgecodegen
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser


object FieldsGrammar : Grammar<List<List<Field>>>() {
    val space by regexToken("[\\ \\t]*", ignore = true)
    val newline by regexToken("\\n+", ignore = true)
    val comment by regexToken("#.*\\n+", ignore = true)
    val equals by literalToken("=")
    val arrayStart by literalToken("[")
    val arrayEnd by literalToken("]")
    val groupSeparator by literalToken("---")
    val digits by regexToken("\\d+")
    val ident by regexToken("[\\w_\\-\\/]+")

    val lineSep by newline or comment
    val text by (ident or digits) map { it.text }
    val integer by digits map { it.text.toInt() }

    inline fun <reified T> lined(p : Parser<T>) : Parser<T> {
        return p * -optional(lineSep)
    }

    val typeParser = text * optional(arrayStart * optional(integer) * arrayEnd) map { (text, arraySuffix) ->
        if (arraySuffix != null){
            if (arraySuffix.t2 != null){
                text to arraySuffix.t2!!
            } else {
                text to 0
            }
        } else {
            text to -1
        }
    }

    val field by typeParser * text * optional(-equals * text) map { (type, name, value) ->
        Field(createTypeFromString(type.first), name, value, type.second)
    }

    val group by separated(field, lineSep, true) map { it.terms }

    override val rootParser: Parser<List<List<Field>>> by separated(group, groupSeparator) * -optional(lineSep) map { it.terms }
}

fun parseFields(text: String) : List<List<Field>> {
    val modText = text.replace("\r", "\n").plus("\n")
    //println("Running $modText")
    //com.github.thoebert.krosbridgecodegen.FieldsGrammar.tokenizer.tokenize(modText).forEach { println("${it.row} ${it.column} - ${it.type.name}: ${it.text}") }
    return FieldsGrammar.parseToEnd(modText)
}

fun checkFields(fieldCount : Int, fields : List<List<Field>>){
    if (fieldCount != fields.size) throw IllegalArgumentException("Expecting $fieldCount fields but parsed ${fields.size} fields")
}

fun parseAndCheckFields(fieldCount : Int, text: String) : List<List<Field>>{
    val fields = parseFields(text)
    checkFields(fieldCount, fields)
    return fields
}

fun parseMessage(name : String, text: String) : Message {
    return Message(createTypeFromString(name), parseAndCheckFields(1, text)[0])
}

fun parseService(name : String, text: String) : Service {
    val fields = parseAndCheckFields(2, text)
    return Service(createTypeFromString(name), fields[0], fields[1])
}

fun parseAction(name : String, text: String) : Action {
    val fields = parseAndCheckFields(3, text)
    return Action(createTypeFromString(name), fields[0], fields[1], fields[2])
}