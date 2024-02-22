package com.github.thoebert.krosbridgecodegen


/**
 * New parser for ROS messages
 */
class FieldsParser {

    var lines: List<String> = listOf()

    private val groups: MutableList<MutableList<Field>> = mutableListOf(mutableListOf())

    fun parseField(position: Int, fatherField: Field? = null, groupPosition: Int = 0, nestedLevel: String = "") {
        if (position > lines.lastIndex) return
        if (lines[position].isBlank()) return parseField(
            position + 1,
            fatherField,
            groupPosition = groupPosition,
            nestedLevel
        )
        if (lines[position] == "---") return parseField(position + 1)

        // regex demonstration: https://regex101.com/r/Fc8wUB/1
        val matched =
            Regex("(?<type>[\\w_\\-/]+|[\\d+])(\\[<?=?(?<size>\\d+)]|<?=?(?<size2>\\d+))?\\s+(?<name>[\\w_\\-/]+)([\\s]{0,}?=[\\s]{0,}(?<value>\\S*[\\w_\\-/]+|[\\d+]))?").find(
                lines[position]
            )
                ?: return parseField(position + 1)
        // get values by group
        val type = (matched.groups["type"] ?: return parseField(
            position + 1,
            fatherField,
            groupPosition = groupPosition,
            nestedLevel
        )).value
        val name = (matched.groups["name"] ?: return parseField(
            position + 1,
            fatherField,
            groupPosition = groupPosition,
            nestedLevel
        )).value
        val size = (matched.groups["size"] ?: matched.groups["size2"])?.value?.toInt() ?: -1
        val value = matched.groups["value"]?.value
        val normalType = createTypeFromString(type)
        val field = Field(normalType, name, value = value, arrayLength = size)
        var newPosition = position + 1
        while (newPosition < lines.size && lines[newPosition].startsWith(nestedLevel + "\t")) {
            parseField(
                newPosition,
                fatherField = field,
                nestedLevel = nestedLevel + "\t",
                groupPosition = groupPosition
            )
            newPosition++
        }
        if (fatherField == null) {
            if (groupPosition == groups.size) groups.add(mutableListOf())
            groups[groupPosition].add(field)
        } else {
            fatherField.children.add(field)
            return
        }
        parseField(newPosition , null, groupPosition = groupPosition, nestedLevel)

    }

    fun parseToEnd(value: String): List<List<Field>> {
        groups.clear()
        lines = value.reader().readLines()
        parseField(position = 0)

        return groups;
    }
}

fun parseFields(text: String): List<List<Field>> {
    return FieldsParser().parseToEnd(text)
}

fun checkFields(fieldCount: Int, fields: List<List<Field>>) {
    if (fieldCount != fields.size) throw IllegalArgumentException("Expecting $fieldCount fields but parsed ${fields.size} fields")
}

fun parseAndCheckFields(fieldCount: Int, text: String): List<List<Field>> {
    val fields = parseFields(text)
    checkFields(fieldCount, fields)
    return fields
}

fun parseMessage(name: Type, text: String): Message {
    return Message(name, parseAndCheckFields(1, text)[0])
}

fun parseService(name: Type, text: String): Service {
    val fields = parseAndCheckFields(2, text)
    return Service(name, fields[0], fields[1])
}

fun parseAction(name: Type, text: String): Action {
    val fields = parseAndCheckFields(3, text)
    return Action(name, fields[0], fields[1], fields[2])
}