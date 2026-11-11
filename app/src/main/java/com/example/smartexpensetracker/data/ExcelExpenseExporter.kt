package com.example.smartexpensetracker.data

import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExpenseExporter {

    fun exportExpenses(
        expenses: List<Expense>,
        outputStream: OutputStream
    ) {

        if (expenses.isEmpty()) {
            throw Exception("No expenses available to export.")
        }

        ZipOutputStream(outputStream).use { zip ->

            // Content Types
            addFile(
                zip,
                "[Content_Types].xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
                """.trimIndent()
            )

            // Root Relationships
            addFile(
                zip,
                "_rels/.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1"
                        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                        Target="xl/workbook.xml"/>
                </Relationships>
                """.trimIndent()
            )

            // Workbook
            addFile(
                zip,
                "xl/workbook.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets>
                        <sheet name="Expenses" sheetId="1" r:id="rId1"/>
                    </sheets>
                </workbook>
                """.trimIndent()
            )

            // Workbook Relationships
            addFile(
                zip,
                "xl/_rels/workbook.xml.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1"
                        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                        Target="worksheets/sheet1.xml"/>
                </Relationships>
                """.trimIndent()
            )

            // Worksheet
            val dateFormat = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            val headers = listOf(
                "Title",
                "Amount",
                "Category",
                "Date",
                "Payment Method",
                "Note"
            )

            val xml = StringBuilder()

            xml.append(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
            )

            xml.append(
                """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">"""
            )

            xml.append("<sheetData>")

            // Header Row
            xml.append("""<row r="1">""")

            headers.forEachIndexed { index, header ->
                val column = getColumnName(index + 1)

                xml.append(
                    createTextCell(
                        "$column" + "1",
                        header
                    )
                )
            }

            xml.append("</row>")

            // Expense Rows
            expenses.forEachIndexed { index, expense ->

                val rowNumber = index + 2

                xml.append("""<row r="$rowNumber">""")

                val title = escapeXml(expense.title)
                val category = escapeXml(expense.category)
                val payment = escapeXml(expense.paymentMethod)
                val note = escapeXml(expense.note)

                val date = dateFormat.format(
                    Date(expense.date)
                )

                // Title
                xml.append(
                    createTextCell("A$rowNumber", title)
                )

                // Amount as numeric value
                xml.append(
                    """<c r="B$rowNumber"><v>${expense.amount}</v></c>"""
                )

                // Category
                xml.append(
                    createTextCell("C$rowNumber", category)
                )

                // Date
                xml.append(
                    createTextCell("D$rowNumber", date)
                )

                // Payment Method
                xml.append(
                    createTextCell("E$rowNumber", payment)
                )

                // Note
                xml.append(
                    createTextCell("F$rowNumber", note)
                )

                xml.append("</row>")
            }

            xml.append("</sheetData>")
            xml.append("</worksheet>")

            addFile(
                zip,
                "xl/worksheets/sheet1.xml",
                xml.toString()
            )

            zip.finish()
        }
    }

    private fun createTextCell(
        cellReference: String,
        value: String
    ): String {

        return """
            <c r="$cellReference" t="inlineStr">
                <is><t xml:space="preserve">$value</t></is>
            </c>
        """.trimIndent()
    }

    private fun escapeXml(value: String): String {

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getColumnName(columnNumber: Int): String {

        var number = columnNumber
        val result = StringBuilder()

        while (number > 0) {
            val remainder = (number - 1) % 26

            result.insert(
                0,
                ('A'.code + remainder).toChar()
            )

            number = (number - 1) / 26
        }

        return result.toString()
    }

    private fun addFile(
        zip: ZipOutputStream,
        fileName: String,
        content: String
    ) {

        zip.putNextEntry(ZipEntry(fileName))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}
