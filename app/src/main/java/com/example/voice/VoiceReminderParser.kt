package com.example.voice

import com.example.data.model.Notebook
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ParsedVoiceResult(
    val title: String = "",
    val timestampMillis: Long? = null,
    val leadTimeMinutes: Int = 0,
    val suggestedNotebookId: String? = null,
    val suggestedNotebookName: String? = null,
    val isMissingTime: Boolean = false,
    val isMissingTitle: Boolean = false,
    // In-app command types
    val commandType: VoiceCommandType = VoiceCommandType.CREATE_REMINDER,
    val commandParam: String = ""
)

enum class VoiceCommandType {
    CREATE_REMINDER,
    SNOOZE_REMINDER, // "aplazalo 20 minutos"
    MARK_DONE,       // "marcá como hecho"
    ADD_NOTE,        // "agregá a la nota: ..."
    SEARCH_NOTES,    // "¿Qué anoté sobre ...?"
    UNKNOWN
}

object VoiceReminderParser {

    /**
     * Parses spoken Spanish text into a structured reminder or action command.
     */
    fun parse(rawInput: String, notebooks: List<Notebook>): ParsedVoiceResult {
        var input = rawInput.trim()
        if (input.isEmpty()) {
            return ParsedVoiceResult(isMissingTitle = true)
        }

        val lower = input.lowercase(Locale.ROOT)

        // 1. Check for in-app voice commands
        // Command: "aplazalo 20 minutos" / "aplazar 15 minutos"
        if (lower.contains("aplazalo") || lower.contains("aplázalo") || lower.contains("aplaza") || lower.contains("postergar")) {
            val minsMatcher = Pattern.compile("(\\d+)\\s*(minutos|min|m)").matcher(lower)
            val minutes = if (minsMatcher.find()) minsMatcher.group(1)?.toIntOrNull() ?: 20 else 20
            return ParsedVoiceResult(
                commandType = VoiceCommandType.SNOOZE_REMINDER,
                commandParam = minutes.toString()
            )
        }

        // Command: "marcá como hecho" / "marcar como hecho" / "completar"
        if (lower.contains("marcá como hecho") || lower.contains("marca como hecho") ||
            lower.contains("marcar como hecho") || lower.contains("listo") || lower.contains("completado")) {
            return ParsedVoiceResult(commandType = VoiceCommandType.MARK_DONE)
        }

        // Command: "agregá a la nota: ..." / "agregar nota: ..."
        val noteMatch = Pattern.compile("(?:agregá|agrega|agregar|anotá|anota)\\s+a\\s+la\\s+nota:?\\s*(.*)", Pattern.CASE_INSENSITIVE).matcher(input)
        if (noteMatch.find()) {
            val noteContent = noteMatch.group(1)?.trim() ?: ""
            return ParsedVoiceResult(
                commandType = VoiceCommandType.ADD_NOTE,
                commandParam = noteContent
            )
        }

        // Command: "¿Qué anoté sobre ...?" / "que anote de ..."
        val searchMatch = Pattern.compile("(?:¿?qué|que)\\s+anot[ée]\\s+(?:sobre|de)\\s+(.*)\\??", Pattern.CASE_INSENSITIVE).matcher(input)
        if (searchMatch.find()) {
            val query = searchMatch.group(1)?.trim()?.removeSuffix("?") ?: ""
            return ParsedVoiceResult(
                commandType = VoiceCommandType.SEARCH_NOTES,
                commandParam = query
            )
        }

        // 2. Reminder Creation Flow: "Apunta: mañana a las 3 entregar el informe de robótica a Eduardo, y recordámelo una hora antes"
        // Strip initial trigger prefix ("Ey Apunta:", "Apunta:", "Apunta", "Recordame", "Anotar", etc.)
        input = input.replace(Regex("^(?:ey\\s+apunta:?|oye\\s+apunta:?|apunta:?|apuntá:?|anota:?|anotá:?|recordame:?|recordámelo:?)\\s*", RegexOption.IGNORE_CASE), "")

        // Extract lead time / aviso previo (e.g. "y recordámelo una hora antes", "avísame 30 minutos antes", "10 minutos antes")
        var leadTimeMinutes = 0
        val leadTimeRegex = Regex("(?:,\\s*)?(?:\\by\\b\\s*)?(?:record[aá]melo|avisame|avísame|alerta)?\\s*(\\d+|una|media|quince|veinte|treinta)?\\s*(hora|horas|minuto|minutos)\\s*antes", RegexOption.IGNORE_CASE)
        val leadMatch = leadTimeRegex.find(input)
        if (leadMatch != null) {
            val numStr = leadMatch.groupValues[1].lowercase(Locale.ROOT)
            val unitStr = leadMatch.groupValues[2].lowercase(Locale.ROOT)
            leadTimeMinutes = when {
                numStr == "una" || numStr == "1" && unitStr.startsWith("hora") -> 60
                numStr == "media" && unitStr.startsWith("hora") -> 30
                numStr.startsWith("quin") -> 15
                numStr.startsWith("vein") -> 20
                numStr.startsWith("trein") -> 30
                unitStr.startsWith("hora") -> (numStr.toIntOrNull() ?: 1) * 60
                unitStr.startsWith("min") -> numStr.toIntOrNull() ?: 15
                else -> 30
            }
            input = input.replace(leadTimeRegex, " ").trim()
        }

        // Extract date and time
        val calendar = Calendar.getInstance()
        var dateSpecified = false
        var timeSpecified = false

        // Check relative times: "en 20 minutos", "en media hora", "en 1 hora"
        val relativeMinMatch = Regex("\\ben\\s+(\\d+|media|un[a]?)\\s*(minuto|minutos|hora|horas)\\b", RegexOption.IGNORE_CASE).find(input)
        if (relativeMinMatch != null) {
            val amountStr = relativeMinMatch.groupValues[1].lowercase(Locale.ROOT)
            val unit = relativeMinMatch.groupValues[2].lowercase(Locale.ROOT)
            val deltaMinutes = when {
                amountStr == "media" -> 30
                amountStr == "un" || amountStr == "una" -> if (unit.startsWith("hora")) 60 else 1
                unit.startsWith("hora") -> (amountStr.toIntOrNull() ?: 1) * 60
                else -> amountStr.toIntOrNull() ?: 10
            }
            calendar.add(Calendar.MINUTE, deltaMinutes)
            dateSpecified = true
            timeSpecified = true
            input = input.replace(relativeMinMatch.value, " ").trim()
        }

        // Check Days: "mañana", "hoy", "pasado mañana", "el viernes", "el lunes", etc.
        val dayLower = input.lowercase(Locale.ROOT)
        if (dayLower.contains("pasado mañana")) {
            calendar.add(Calendar.DAY_OF_YEAR, 2)
            dateSpecified = true
            input = input.replace(Regex("\\bpasado mañana\\b", RegexOption.IGNORE_CASE), " ")
        } else if (dayLower.contains("mañana")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            dateSpecified = true
            input = input.replace(Regex("\\bmañana\\b", RegexOption.IGNORE_CASE), " ")
        } else if (dayLower.contains("hoy")) {
            dateSpecified = true
            input = input.replace(Regex("\\bhoy\\b", RegexOption.IGNORE_CASE), " ")
        } else {
            // Days of week
            val daysOfWeek = mapOf(
                "domingo" to Calendar.SUNDAY,
                "lunes" to Calendar.MONDAY,
                "martes" to Calendar.TUESDAY,
                "miércoles" to Calendar.WEDNESDAY,
                "miercoles" to Calendar.WEDNESDAY,
                "jueves" to Calendar.THURSDAY,
                "viernes" to Calendar.FRIDAY,
                "sábado" to Calendar.SATURDAY,
                "sabado" to Calendar.SATURDAY
            )
            for ((dayName, dayConstant) in daysOfWeek) {
                val dayRegex = Regex("\\b(?:el\\s+)?$dayName\\b", RegexOption.IGNORE_CASE)
                if (dayRegex.containsMatchIn(input)) {
                    val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                    var daysUntil = dayConstant - currentDay
                    if (daysUntil <= 0) daysUntil += 7
                    calendar.add(Calendar.DAY_OF_YEAR, daysUntil)
                    dateSpecified = true
                    input = input.replace(dayRegex, " ")
                    break
                }
            }
        }

        // Check Hour: "a las 3", "a las 3 de la tarde", "a las 8 de la mañana", "a las 15:00", "a las 4:30 pm"
        val hourRegex = Regex("\\ba\\s+las\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(?:de\\s+la\\s+(mañana|tarde|noche)|(am|pm))?", RegexOption.IGNORE_CASE)
        val hourMatch = hourRegex.find(input)
        if (hourMatch != null) {
            val hourVal = hourMatch.groupValues[1].toIntOrNull() ?: 12
            val minVal = hourMatch.groupValues[2].toIntOrNull() ?: 0
            val period = (hourMatch.groupValues[3] + hourMatch.groupValues[4]).lowercase(Locale.ROOT)

            var adjustedHour = hourVal
            if ((period.contains("tarde") || period.contains("noche") || period.contains("pm")) && adjustedHour < 12) {
                adjustedHour += 12
            } else if ((period.contains("mañana") || period.contains("am")) && adjustedHour == 12) {
                adjustedHour = 0
            } else if (period.isEmpty() && adjustedHour in 1..6) {
                // If user says "a las 3" without period, in day-to-day context for tasks, 1-6 usually means afternoon (13:00 - 18:00)
                adjustedHour += 12
            }

            calendar.set(Calendar.HOUR_OF_DAY, adjustedHour)
            calendar.set(Calendar.MINUTE, minVal)
            calendar.set(Calendar.SECOND, 0)
            timeSpecified = true
            input = input.replace(hourMatch.value, " ")
        }

        // Clean up remaining text for the title
        var cleanTitle = input
            .replace(Regex("[,;\\.]+$"), "")
            .replace(Regex(",?\\s*\\by\\b\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[,;\\.]+$"), "")
            .replace(Regex("^[,;\\.\\s]+"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        // Normalize common action verb phrases: e.g. "entregar el informe" -> "entregar informe"
        cleanTitle = cleanTitle.replace(Regex("\\b(entregar|hacer|revisar|comprar|enviar|pagar|estudiar|leer|terminar)\\s+(?:el|la|los|las)\\b", RegexOption.IGNORE_CASE)) {
            it.groupValues[1]
        }

        if (cleanTitle.isNotEmpty()) {
            cleanTitle = cleanTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        // 3. Match against existing Notebooks (Agrupación inteligente)
        var matchedNotebook: Notebook? = null
        val titleLower = cleanTitle.lowercase(Locale.ROOT)
        for (nb in notebooks) {
            val nbNameLower = nb.name.lowercase(Locale.ROOT)
            val nbLabelLower = nb.label.lowercase(Locale.ROOT)
            if (titleLower.contains(nbNameLower) || (nbLabelLower.isNotEmpty() && titleLower.contains(nbLabelLower))) {
                matchedNotebook = nb
                break
            }
        }

        val hasMissingTime = !timeSpecified
        val hasMissingTitle = cleanTitle.isBlank()

        val finalTimestamp = if (timeSpecified || dateSpecified) {
            if (!timeSpecified) {
                // default to 15:00 if only date specified
                calendar.set(Calendar.HOUR_OF_DAY, 15)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            calendar.timeInMillis
        } else {
            null
        }

        return ParsedVoiceResult(
            title = cleanTitle,
            timestampMillis = finalTimestamp,
            leadTimeMinutes = leadTimeMinutes,
            suggestedNotebookId = matchedNotebook?.id,
            suggestedNotebookName = matchedNotebook?.name,
            isMissingTime = hasMissingTime,
            isMissingTitle = hasMissingTitle,
            commandType = VoiceCommandType.CREATE_REMINDER
        )
    }
}
