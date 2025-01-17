package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.output.TermUi.echo
import gov.cdc.prime.router.*
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
import java.io.File

class FileUtilities {
    companion object {
        fun createFakeFile(
            metadata: Metadata,
            sender: Sender,
            count: Int,
            targetStates: String? = null,
            targetCounties: String? = null,
            directory: String = ".",
            format: Report.Format = Report.Format.CSV,
        ): File {
            val report = createFakeReport(
                metadata,
                sender,
                count,
                targetStates,
                targetCounties,
            )
            return writeReportToFile(report, format, metadata, directory, null)
        }

        fun createFakeReport(
            metadata: Metadata,
            sender: Sender,
            count: Int,
            targetStates: String? = null,
            targetCounties: String? = null,
        ): Report {
            return FakeReport(metadata).build(
                metadata.findSchema(sender.schemaName)
                    ?: error("Unable to find schema ${sender.schemaName}"),
                count,
                FileSource("fake"),
                targetStates,
                targetCounties,
            )
        }

        fun writeReportsToFile(
            reports: List<Pair<Report, Report.Format>>,
            metadata: Metadata,
            outputDir: String?,
            outputFileName: String?,
        ) {
            if (outputDir == null && outputFileName == null) return

            if (reports.isNotEmpty()) {
                echo("Creating these files:")
            }
            reports
                .flatMap { (report, format) ->
                    // Some report formats only support one result per file
                    if (format.isSingleItemFormat) {
                        val splitReports = report.split()
                        splitReports.map { Pair(it, format) }
                    } else {
                        listOf(Pair(report, format))
                    }
                }.forEach { (report, format) ->
                    var outputFile = writeReportToFile(report, format, metadata, outputDir, outputFileName)
                    echo(outputFile.absolutePath)
                }
        }

        fun writeReportToFile(
            report: Report,
            format: Report.Format,
            metadata: Metadata,
            outputDir: String?,
            outputFileName: String?,
        ): File {
            val outputFile = if (outputFileName != null) {
                File(outputFileName)
            } else {
                // is this config HL7?
                val hl7Config = report.destination?.translation as? Hl7Configuration?
                // if it is, get the test processing mode
                val processingMode = hl7Config?.processingModeCode ?: "P"
                val fileName = Report.formFilename(
                    report.id,
                    report.schema.baseName,
                    format,
                    report.createdDateTime,
                    nameFormat = Report.NameFormat.STANDARD,
                    report.destination?.translation?.receivingOrganization,
                    processingMode
                )
                File(outputDir ?: ".", fileName)
            }
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }

            val csvSerializer = CsvSerializer(metadata)
            val hl7Serializer = Hl7Serializer(metadata)
            val redoxSerializer = RedoxSerializer(metadata)
            outputFile.outputStream().use {
                when (format) {
                    Report.Format.INTERNAL -> csvSerializer.writeInternal(report, it)
                    Report.Format.CSV -> csvSerializer.write(report, it)
                    Report.Format.HL7 -> hl7Serializer.write(report, it)
                    Report.Format.HL7_BATCH -> hl7Serializer.writeBatch(report, it)
                    Report.Format.REDOX -> redoxSerializer.write(report, it)
                }
            }
            return outputFile
        }
    }
}