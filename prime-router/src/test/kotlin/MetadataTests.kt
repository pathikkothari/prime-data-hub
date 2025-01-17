package gov.cdc.prime.router

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MetadataTests {
    @Test
    fun `test loading metadata catalog`() {
        val metadata = Metadata("./metadata")
        assertNotNull(metadata)
    }

    @Test
    fun `test loading two schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a"), name = "one", topic = "test"),
            Schema(Element("a"), Element("b"), name = "two", topic = "test")
        )
        assertNotNull(metadata.findSchema("one"))
    }

    @Test
    fun `test loading basedOn schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a", default = "foo"), name = "one", topic = "test"),
            Schema(Element("a"), Element("b"), name = "two", topic = "test", basedOn = "one")
        )
        val two = metadata.findSchema("two")
        assertEquals("foo", two?.findElement("a")?.default)
    }

    @Test
    fun `test loading extends schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a", default = "foo"), Element("b"), name = "one", topic = "test"),
            Schema(Element("a"), name = "two", topic = "test", extends = "one")
        )
        val two = metadata.findSchema("two")
        assertEquals("foo", two?.findElement("a")?.default)
        assertNotNull(two?.findElement("b"))
    }

    @Test
    fun `test loading multi-level schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a", default = "foo"), Element("b"), name = "one", topic = "test"),
            Schema(Element("a"), Element("c"), name = "two", topic = "test", basedOn = "one"),
            Schema(Element("a"), Element("d"), name = "three", topic = "test", extends = "two")
        )
        val three = metadata.findSchema("three")
        assertEquals("foo", three?.findElement("a")?.default)
        assertNull(three?.findElement("b"))
        assertNotNull(three?.findElement("c"))
        assertNotNull(three?.findElement("d"))
    }

    @Test
    fun `load valueSets`() {
        val metadata = Metadata().loadValueSets(
            ValueSet("one", ValueSet.SetSystem.HL7),
            ValueSet("two", ValueSet.SetSystem.LOCAL)
        )
        assertNotNull(metadata.findValueSet("one"))
    }

    @Test
    fun `load value set directory`() {
        val metadata = Metadata().loadValueSetCatalog("./metadata/valuesets")
        assertNotNull(metadata.findValueSet("hl70136"))
    }

    @Test
    fun `test find schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(name = "One", topic = "test", elements = listOf(Element("a"))),
            Schema(name = "Two", topic = "test", elements = listOf(Element("a"), Element("b")))
        )
        assertNotNull(metadata.findSchema("one"))
    }

    @Test
    fun `test schema contamination`() {
        // arrange
        val valueSetA = ValueSet(
            "a_values",
            ValueSet.SetSystem.LOCAL,
            values = listOf(ValueSet.Value("Y", "Yes"), ValueSet.Value("N", "No"))
        )
        val elementA = Element("a", Element.Type.CODE, valueSet = "a_values", valueSetRef = valueSetA)
        val baseSchema = Schema(name = "base_schema", topic = "test", elements = listOf(elementA))
        val childSchema = Schema(
            name = "child_schema",
            extends = "base_schema",
            topic = "test",
            elements = listOf(
                Element(
                    "a",
                    altValues = listOf(
                        ValueSet.Value("J", "Ja"),
                        ValueSet.Value("N", "Nein")
                    ),
                    csvFields = listOf(Element.CsvField("Ja Oder Nein", format = "\$code"))
                )
            )
        )
        val siblingSchema = Schema(
            name = "sibling_schema",
            extends = "base_schema",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = listOf(Element.CsvField("yes/no", format = null)))
            )
        )
        val twinSchema = Schema(
            name = "twin_schema",
            basedOn = "base_schema",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = listOf(Element.CsvField("yes/no", format = null)))
            )
        )

        // act
        val metadata = Metadata()
        metadata.loadValueSets(valueSetA)
        metadata.loadSchemas(
            baseSchema,
            childSchema,
            siblingSchema,
            twinSchema
        )

        // assert
        val elementName = "a"
        val parent = metadata.findSchema("base_schema")
        assertNotNull(parent)
        assertTrue(parent.findElement(elementName)?.csvFields.isNullOrEmpty())
        // the first child element
        val child = metadata.findSchema("child_schema")
        assertNotNull(child)
        val childElement = child.findElement(elementName)
        assertNotNull(childElement)
        assertEquals("\$code", childElement.csvFields?.first()?.format)
        // sibling uses extends
        val sibling = metadata.findSchema("sibling_schema")
        assertNotNull(sibling)
        val siblingElement = sibling.findElement(elementName)
        assertNotNull(siblingElement)
        assertTrue(siblingElement.csvFields?.count() == 1)
        assertNull(siblingElement.csvFields?.first()?.format)
        // twin uses basedOn instead of extends
        val twin = metadata.findSchema("twin_schema")
        assertNotNull(twin)
        val twinElement = twin.findElement(elementName)
        assertNotNull(twinElement)
        assertTrue(twinElement.csvFields?.count() == 1)
        assertNull(twinElement.csvFields?.first()?.format)
    }

    @Test
    fun `test valueset merging`() {
        // arrange
        val valueSet = ValueSet(
            "a", ValueSet.SetSystem.LOCAL,
            values = listOf(
                ValueSet.Value("Y", "Yes"),
                ValueSet.Value("N", "No"),
                ValueSet.Value("UNK", "Unknown"),
            )
        )

        val emptyAltValues = listOf<ValueSet.Value>()
        val replacementValues = listOf(
            ValueSet.Value("U", "Unknown", replaces = "UNK")
        )
        val additionalValues = listOf(
            ValueSet.Value("M", "Maybe")
        )
        // act
        val shouldBeSame = valueSet.mergeAltValues(emptyAltValues)
        val shouldBeDifferent = valueSet.mergeAltValues(replacementValues)
        val shouldBeExtended = valueSet.mergeAltValues(additionalValues)

        // assert
        assertSame(valueSet, shouldBeSame)
        assertNotSame(valueSet, shouldBeDifferent)

        assertNotNull(shouldBeSame.values.find { it.code.equals("UNK", ignoreCase = true) })
        assertNotNull(shouldBeDifferent.values.find { it.code.equals("U", ignoreCase = true) })
        assertNotNull(shouldBeDifferent.values.find { it.replaces.equals("UNK", ignoreCase = true) })
        assertNull(shouldBeDifferent.values.find { it.code.equals("UNK", ignoreCase = true) })

        assertNotNull(shouldBeExtended.values.find { it.code.equals("M", ignoreCase = true) })
        assertNotNull(shouldBeExtended.values.find { it.code.equals("UNK", ignoreCase = true) })
    }
}