package gov.cdc.prime.router

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

// a singleton instance to let us build documentation off of a schema or off of an element
object DocumentationFactory {
    // will generate a documentation string based on markdown that can then be presented
    // to end users or be converted into HTML if we want to be fancy

    fun getElementDocumentation(element: Element): String {
        val csvField = element.csvFields?.get(0)
        val sb = StringBuilder()
        val displayName = csvField?.name ?: element.name

        // our top-level element data points
        sb.appendLine("") // start with a blank line at the top 
        appendLabelAndData(sb, "Name", displayName)
        appendLabelAndData(sb, "Type", element.type?.name)
        appendLabelAndData(sb, "Format", csvField?.format)
        if (element.hl7OutputFields != null)
            appendLabelAndData(sb, "HL7 Fields", element.hl7OutputFields.joinToString(", "))
        else if (element.hl7Field != null)
            appendLabelAndData(sb, "HL7 Field", element.hl7Field)
        if (element.hl7Field == "AOE") appendLabelAndData(sb, "LOINC Code", element.hl7AOEQuestion)
        appendLabelAndData(
            sb, "Cardinality",
            element.cardinality?.toFormatted() ?: Element.Cardinality.ZERO_OR_ONE.toFormatted()
        )

        // output the reference url
        if (element.referenceUrl?.isNotBlank() == true) {
            appendLabelAndUrl(sb, "Reference URL", element.referenceUrl)
        }

        // build the valuesets
        if (element.valueSetRef != null) {
            appendValueSetTable(sb, "Value Sets", element.valueSetRef.values)
        }

        if (element.altValues?.isNotEmpty() == true) {
            appendValueSetTable(sb, "Alt Value Sets", element.altValues)
        }

        if (element.table?.isNotEmpty() == true) {
            appendLabelAndData(sb, "Table", element.table)
            appendLabelAndData(sb, "Table Column", element.tableColumn)
        }

        if (element.documentation?.isNotEmpty() == true) {
            sb.appendLine(
                """**Documentation**:

${element.documentation}
"""
            )
        }

        // output a horizontal line
        sb.appendLine("---")

        return sb.toString()
    }

    // gets the documentation
    fun getSchemaDocumentation(schema: Schema): String {
        val sb = StringBuilder()

        sb.appendLine(
            """
### Schema:         ${schema.name}
#### Description:   ${schema.description}

---"""
        )

        schema.elements.forEach { element ->
            sb.append(getElementDocumentation(element))
        }

        return sb.toString()
    }

    /**
     * Write markdown documentation for a schema to a file.
     *
     * @param schema The schema to document.
     * @param outputDir The directory to write the file to. If this directory
     *        doesn't exist, it will be created.
     * @param outputFileName Name of the file to write in `outputDir`. If not
     *        set, the file will be named after the schema.
     * @param includeTimestamps Append the current date to the filename.
     */
    fun writeDocumentationForSchema(
        schema: Schema,
        outputDir: String = ".",
        outputFileName: String? = null,
        includeTimestamps: Boolean = false,
        generateMarkupFile: Boolean = true,
        generateHtmlFile: Boolean = false
    ) {
        // Why are you even calling this
        if (!generateMarkupFile && !generateHtmlFile) return;

        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val createDate = LocalDate.now().format(formatter)
        // change any slashes to dashes for the file name
        val schemaName = schema.name.replace("/", "-")

        val mdText = getSchemaDocumentation(schema)
        val path = Paths.get(outputDir)
        if (!Files.exists(path)) {
            Files.createDirectory(path)
        }

        // Generate the markup file
        if(generateMarkupFile) {
            val markupName = (outputFileName ?: schemaName) + if (includeTimestamps) {
                "-$createDate.md"
            } else {
                ".md"
            }
            File(outputDir, markupName).writeText(mdText)
        }

        // Generate the HTML file
        if(generateHtmlFile) {
            val htmlName = (outputFileName ?: schemaName) + if (includeTimestamps) {
                "-$createDate.html"
            } else {
                ".html"
            }
            File(outputDir, htmlName).writeText(convertMarkdownToHtml(mdText))
        }
    }

    /**
     * Convert [markdown] text to HTML.
     * @return HTML text
     */
    private fun convertMarkdownToHtml(markdown : String) : String {
        val parser = Parser.builder().build()
        val document = parser.parse(markdown)
        val renderer = HtmlRenderer.builder().build()
        return renderer.render(document)
    }

    private fun appendLabelAndData(appendable: Appendable, label: String, value: Any?) {
        if (value != null) {
            appendable.appendLine(
                "**$label**: $value\n"
            )
        }
    }

    private fun appendLabelAndUrl(appendable: Appendable, label: String, url: String, linkText: String? = null) {
        if (url.isNotBlank()) {
            appendable.appendLine(
                """
**$label**:
[${linkText ?: url}]($url) 
"""
            )
        }
    }

    private fun appendValueSetTable(appendable: Appendable, label: String, values: Collection<ValueSet.Value>?) {
        if (values?.isNotEmpty() == true) {

            appendable.appendLine("**$label**\n")
            appendable.appendLine("Code | Display")
            appendable.appendLine("---- | -------")

            values.forEach { vs ->
                appendable.appendLine("${vs.code}|${vs.display}")
            }
            appendable.appendLine("")
        }
    }
}