package com.example

import com.example.data.model.Notebook
import com.example.voice.VoiceCommandType
import com.example.voice.VoiceReminderParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class VoiceReminderParserTest {

    private val sampleNotebooks = listOf(
        Notebook(id = "nb-robotica", name = "Robótica", color = "#4F46E5", label = "Robótica"),
        Notebook(id = "nb-datos", name = "Datos", color = "#06B6D4", label = "Datos"),
        Notebook(id = "nb-personal", name = "Personal", color = "#F59E0B", label = "Personal")
    )

    @Test
    fun testOfficialSpecificationExample() {
        val input = "Apunta: mañana a las 3 entregar el informe de robótica a Eduardo, y recordámelo una hora antes"
        val result = VoiceReminderParser.parse(input, sampleNotebooks)

        assertEquals("Entregar informe de robótica a Eduardo", result.title)
        assertEquals(60, result.leadTimeMinutes)
        assertEquals("nb-robotica", result.suggestedNotebookId)
        assertNotNull(result.timestampMillis)

        val cal = Calendar.getInstance().apply { timeInMillis = result.timestampMillis!! }
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testRelativeTimeParsing() {
        val input = "En 20 minutos llamar a mamá"
        val result = VoiceReminderParser.parse(input, sampleNotebooks)

        assertTrue(result.title.contains("llamar a mamá", ignoreCase = true))
        assertNotNull(result.timestampMillis)
    }

    @Test
    fun testMissingTimeDetection() {
        val input = "Apunta entregar informe"
        val result = VoiceReminderParser.parse(input, sampleNotebooks)

        assertTrue(result.isMissingTime)
        assertTrue(result.title.contains("entregar informe", ignoreCase = true))
    }

    @Test
    fun testInAppCommands() {
        val snoozeResult = VoiceReminderParser.parse("aplazalo 20 minutos", sampleNotebooks)
        assertEquals(VoiceCommandType.SNOOZE_REMINDER, snoozeResult.commandType)
        assertEquals("20", snoozeResult.commandParam)

        val doneResult = VoiceReminderParser.parse("marcá como hecho", sampleNotebooks)
        assertEquals(VoiceCommandType.MARK_DONE, doneResult.commandType)

        val noteResult = VoiceReminderParser.parse("agregá a la nota: comprar cables jumper", sampleNotebooks)
        assertEquals(VoiceCommandType.ADD_NOTE, noteResult.commandType)
        assertEquals("comprar cables jumper", noteResult.commandParam)

        val searchResult = VoiceReminderParser.parse("¿Qué anoté sobre informe de Eduardo?", sampleNotebooks)
        assertEquals(VoiceCommandType.SEARCH_NOTES, searchResult.commandType)
        assertTrue(searchResult.commandParam.contains("informe de Eduardo"))
    }

    @Test
    fun testEyApuntaPrefix() {
        val input = "Ey Apunta: mañana a las 3 reunión de equipo"
        val result = VoiceReminderParser.parse(input, sampleNotebooks)
        assertTrue(result.title.contains("reunión de equipo", ignoreCase = true))
        assertNotNull(result.timestampMillis)
    }
}
