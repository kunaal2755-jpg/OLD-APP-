package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.ShopSettings
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.StaffPayment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StaffPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 32f

    fun generateSalaryPdf(
        context: Context,
        shopSettings: ShopSettings,
        staff: StaffMember,
        monthYearDisplay: String,
        daysInMonth: Int,
        attendanceList: List<StaffAttendance>,
        paymentsList: List<StaffPayment>,
        summary: com.example.data.model.StaffMonthlySummary? = null
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawSalaryPdfContent(
            context = context,
            canvas = canvas,
            settings = shopSettings,
            staff = staff,
            monthYear = monthYearDisplay,
            daysInMonth = daysInMonth,
            attendanceList = attendanceList,
            paymentsList = paymentsList,
            summary = summary
        )

        pdfDocument.finishPage(page)

        val pdfsDir = File(context.cacheDir, "staff_salary_slips")
        if (!pdfsDir.exists()) pdfsDir.mkdirs()

        val cleanName = staff.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val cleanMonth = monthYearDisplay.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val outputFile = File(pdfsDir, "SalarySlip_${cleanName}_${cleanMonth}.pdf")

        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return outputFile
    }

    private fun drawSalaryPdfContent(
        context: Context,
        canvas: Canvas,
        settings: ShopSettings,
        staff: StaffMember,
        monthYear: String,
        daysInMonth: Int,
        attendanceList: List<StaffAttendance>,
        paymentsList: List<StaffPayment>,
        summary: com.example.data.model.StaffMonthlySummary? = null
    ) {
        val themeOption = com.example.ui.theme.getAppThemeOption(settings.appTheme)
        val navyPrimary = Color.rgb(
            (themeOption.primary.red * 255).toInt(),
            (themeOption.primary.green * 255).toInt(),
            (themeOption.primary.blue * 255).toInt()
        )
        val accentGold = Color.rgb(
            (themeOption.accent.red * 255).toInt(),
            (themeOption.accent.green * 255).toInt(),
            (themeOption.accent.blue * 255).toInt()
        )
        val textDark = Color.rgb(30, 41, 59)
        val textMuted = Color.rgb(100, 116, 139)
        val lightBg = Color.rgb(248, 250, 252)
        val borderColor = Color.rgb(226, 232, 240)
        val successGreen = Color.rgb(22, 163, 74)
        val errorRed = Color.rgb(220, 38, 38)
        val warningOrange = Color.rgb(234, 88, 12)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Top Decorative Bar
        paint.color = navyPrimary
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 10f, paint)

        // 2. Shop Logo / Emblem
        val logoSize = 42f
        val logoLeft = MARGIN
        val logoTop = 18f

        var logoDrawn = false
        if (!settings.logoUri.isNullOrBlank()) {
            try {
                val logoFile = File(settings.logoUri)
                if (logoFile.exists()) {
                    val rawBmp = BitmapFactory.decodeFile(logoFile.absolutePath)
                    if (rawBmp != null) {
                        val scaled = Bitmap.createScaledBitmap(rawBmp, logoSize.toInt(), logoSize.toInt(), true)
                        canvas.drawBitmap(scaled, logoLeft, logoTop, paint)
                        logoDrawn = true
                    }
                }
            } catch (e: Exception) {
                logoDrawn = false
            }
        }

        val displayShopName = settings.shopName.ifBlank { "TILES AND MARBLES" }

        if (!logoDrawn) {
            paint.color = accentGold
            canvas.drawRoundRect(RectF(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize), 8f, 8f, paint)
            paint.color = navyPrimary
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val initial = displayShopName.firstOrNull()?.uppercaseChar()?.toString() ?: "T"
            val textW = paint.measureText(initial)
            canvas.drawText(initial, logoLeft + (logoSize - textW) / 2f, logoTop + 29f, paint)
        }

        // Shop Name & Subtitle
        paint.color = textDark
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(displayShopName, logoLeft + logoSize + 10f, logoTop + 16f, paint)

        if (settings.subtitle.isNotBlank()) {
            paint.color = accentGold
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(settings.subtitle, logoLeft + logoSize + 10f, logoTop + 28f, paint)
        }

        paint.color = textMuted
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val contactInfo = listOfNotNull(
            settings.address.takeIf { it.isNotBlank() },
            settings.ownerPhone.takeIf { it.isNotBlank() }?.let { "Ph: $it" }
        ).joinToString(" • ")
        canvas.drawText(contactInfo.ifBlank { "Tiles, Granite & Marble Dealer" }, logoLeft + logoSize + 10f, logoTop + 40f, paint)

        // Document Title Badge on Right
        val badgeW = 165f
        val badgeH = 42f
        val badgeLeft = PAGE_WIDTH - MARGIN - badgeW
        paint.color = navyPrimary
        canvas.drawRoundRect(RectF(badgeLeft, logoTop, badgeLeft + badgeW, logoTop + badgeH), 8f, 8f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleText = "SALARY & LEAVE STATEMENT"
        val tw = paint.measureText(titleText)
        canvas.drawText(titleText, badgeLeft + (badgeW - tw) / 2f, logoTop + 17f, paint)

        paint.color = accentGold
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val monthText = "माह: $monthYear"
        val mw = paint.measureText(monthText)
        canvas.drawText(monthText, badgeLeft + (badgeW - mw) / 2f, logoTop + 33f, paint)

        // Divider
        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, logoTop + logoSize + 12f, PAGE_WIDTH - MARGIN, logoTop + logoSize + 12f, paint)

        // 3. Employee Information Card
        var y = logoTop + logoSize + 20f

        paint.color = lightBg
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 46f), 6f, 6f, paint)
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 46f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = navyPrimary
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("कर्मचारी: ${staff.name}", MARGIN + 10f, y + 18f, paint)

        paint.color = textMuted
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("पद: ${staff.role.ifBlank { "Worker" }}  |  मोबाइल: ${staff.phone.ifBlank { "N/A" }}", MARGIN + 10f, y + 34f, paint)

        paint.color = textDark
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val salText = "मासिक पगार: ₹${String.format(Locale.US, "%,.2f", staff.monthlySalary)}"
        val sw = paint.measureText(salText)
        canvas.drawText(salText, PAGE_WIDTH - MARGIN - sw - 10f, y + 18f, paint)

        val genDateText = "जारी दिनांक: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}"
        val gdw = paint.measureText(genDateText)
        paint.color = textMuted
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(genDateText, PAGE_WIDTH - MARGIN - gdw - 10f, y + 34f, paint)

        y += 54f

        // 4. Attendance & Salary Calculation Summary
        val presentCount = attendanceList.count { it.status == "PRESENT" }
        val halfDayCount = attendanceList.count { it.status == "HALF_DAY" }
        val absentCount = attendanceList.count { it.status == "ABSENT" }
        val paidLeaveCount = attendanceList.count { it.status == "PAID_LEAVE" }

        val dailyRate = when (staff.salaryCalculationType) {
            "FIXED_30_DAYS" -> if (staff.monthlySalary > 0) staff.monthlySalary / 30.0 else 0.0
            "ACTUAL_MONTH_DAYS" -> if (daysInMonth > 0 && staff.monthlySalary > 0) staff.monthlySalary / daysInMonth.toDouble() else 0.0
            "WORKING_DAYS" -> {
                val wDays = if (staff.workingDaysPerMonth > 0) staff.workingDaysPerMonth else 26
                if (staff.monthlySalary > 0) staff.monthlySalary / wDays.toDouble() else 0.0
            }
            "DAILY_WAGE" -> staff.dailyWageRate
            else -> if (daysInMonth > 0) staff.monthlySalary / daysInMonth.toDouble() else 0.0
        }
        val effectiveDays = presentCount + paidLeaveCount + (halfDayCount * 0.5)
        val earnedGross = effectiveDays * dailyRate

        val totalAdvance = paymentsList.filter { it.type == "ADVANCE" }.sumOf { it.amount }
        val totalSalaryPaid = paymentsList.filter { it.type == "SALARY" }.sumOf { it.amount }
        val netPayable = earnedGross + staff.openingBalance - totalAdvance - totalSalaryPaid

        // Attendance Stats Row (4 boxes)
        val boxW = (PAGE_WIDTH - 2 * MARGIN - 18f) / 4f
        val statBoxH = 34f

        drawStatBox(canvas, MARGIN, y, boxW, statBoxH, "महीने के दिन", "$daysInMonth दिन", textDark, borderColor)
        drawStatBox(canvas, MARGIN + boxW + 6f, y, boxW, statBoxH, "कुल उपस्थिति (P)", "$presentCount दिन", successGreen, borderColor)
        drawStatBox(canvas, MARGIN + (boxW + 6f) * 2, y, boxW, statBoxH, "आधे दिन (½ Day)", "$halfDayCount दिन", warningOrange, borderColor)
        drawStatBox(canvas, MARGIN + (boxW + 6f) * 3, y, boxW, statBoxH, "छुट्टी / गैरहाजिर", "$absentCount दिन", errorRed, borderColor)

        y += statBoxH + 10f

        // 5. CRITICAL REQUIREMENT: Clear Date-wise Breakdown of Leaves & Half Days
        // So that staff member can see which exact dates they were absent or half-day without any confusion!
        val absentDates = attendanceList.filter { it.status == "ABSENT" }.sortedBy { it.dateString }
        val halfDayDates = attendanceList.filter { it.status == "HALF_DAY" }.sortedBy { it.dateString }

        paint.color = Color.rgb(254, 242, 242)
        val leaveBoxH = 68f
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + leaveBoxH), 6f, 6f, paint)

        paint.color = Color.rgb(254, 202, 202)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + leaveBoxH), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Header for Leave / Half-day Section
        paint.color = errorRed
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("📅 छुट्टी एवं हाफ-डे का स्पष्ट विवरण (Leave & Half-Day Dates):", MARGIN + 10f, y + 16f, paint)

        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("• छुट्टियां (Absent - $absentCount दिन):", MARGIN + 10f, y + 32f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val absentDateStrings = if (absentDates.isNotEmpty()) {
            absentDates.joinToString(", ") { a ->
                formatReadableDate(a.dateString) + (if (a.notes.isNotBlank()) " (${a.notes})" else "")
            }
        } else {
            "इस माह कोई छुट्टी नहीं रही (0 दिन अनुपस्थित)"
        }
        paint.color = if (absentDates.isNotEmpty()) errorRed else successGreen
        val cleanAbsentText = if (absentDateStrings.length > 70) absentDateStrings.take(67) + "..." else absentDateStrings
        canvas.drawText(cleanAbsentText, MARGIN + 175f, y + 32f, paint)

        // Half-day dates
        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("• हाफ-डे (Half Day - $halfDayCount दिन):", MARGIN + 10f, y + 50f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val halfDayStrings = if (halfDayDates.isNotEmpty()) {
            halfDayDates.joinToString(", ") { h ->
                formatReadableDate(h.dateString) + (if (h.notes.isNotBlank()) " (${h.notes})" else "")
            }
        } else {
            "कोई हाफ-डे नहीं (0 दिन)"
        }
        paint.color = if (halfDayDates.isNotEmpty()) warningOrange else textMuted
        val cleanHalfText = if (halfDayStrings.length > 70) halfDayStrings.take(67) + "..." else halfDayStrings
        canvas.drawText(cleanHalfText, MARGIN + 175f, y + 50f, paint)

        y += leaveBoxH + 10f

        // 6. Salary Calculation Breakdown Box
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 58f), 6f, 6f, paint)

        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val priorArrears = summary?.previousUnfinalizedBalance ?: 0.0
        val isMonthFinalized = summary?.isFinalized ?: false
        val totalPayableWithArrears = netPayable + priorArrears

        canvas.drawText("1. तय मासिक पगार (Monthly Base):", MARGIN + 10f, y + 14f, paint)
        canvas.drawText("₹${String.format(Locale.US, "%,.2f", staff.monthlySalary)}", MARGIN + 300f, y + 14f, paint)

        canvas.drawText("2. प्रति दिन दर (पगार ÷ $daysInMonth दिन):", MARGIN + 10f, y + 26f, paint)
        canvas.drawText("₹${String.format(Locale.US, "%,.2f", dailyRate)} / दिन", MARGIN + 300f, y + 26f, paint)

        canvas.drawText("3. कार्य अनुसार कुल वेतन ($effectiveDays प्रभावी दिन):", MARGIN + 10f, y + 38f, paint)
        canvas.drawText("₹${String.format(Locale.US, "%,.2f", earnedGross)}", MARGIN + 300f, y + 38f, paint)

        if (priorArrears > 0) {
            paint.color = Color.rgb(185, 28, 28)
            val arrearsNote = if (!summary?.unfinalizedMonthsText.isNullOrBlank()) " (${summary?.unfinalizedMonthsText})" else ""
            canvas.drawText("4. पिछला अनफाइनल बकाया (Prior Unfinalized Arrears$arrearsNote):", MARGIN + 10f, y + 50f, paint)
            canvas.drawText("+ ₹${String.format(Locale.US, "%,.2f", priorArrears)}", MARGIN + 300f, y + 50f, paint)
        } else if (staff.openingBalance != 0.0) {
            paint.color = successGreen
            canvas.drawText("4. पिछला बकाया / पूर्व हिसाब (+ Opening Balance):", MARGIN + 10f, y + 50f, paint)
            canvas.drawText("+ ₹${String.format(Locale.US, "%,.2f", staff.openingBalance)}", MARGIN + 300f, y + 50f, paint)
        }

        y += 66f

        // 7. Itemized Advances Taken Table (लेन-देन व एडवांस की सूची)
        paint.color = navyPrimary
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("एडवांस भुगतान विवरणी (Advance Payments Ledger):", MARGIN, y + 10f, paint)

        y += 16f

        // Table Header
        val colDateW = 85f
        val colTypeW = 80f
        val colModeW = 60f
        val colAmtW = 80f
        val colNotesW = PAGE_WIDTH - 2 * MARGIN - colDateW - colTypeW - colModeW - colAmtW

        paint.color = navyPrimary
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("दिनांक (Date)", MARGIN + 8f, y + 12f, paint)
        canvas.drawText("प्रकार (Type)", MARGIN + colDateW + 6f, y + 12f, paint)
        canvas.drawText("माध्यम (Mode)", MARGIN + colDateW + colTypeW + 6f, y + 12f, paint)
        canvas.drawText("विवरण / नोट (Purpose)", MARGIN + colDateW + colTypeW + colModeW + 6f, y + 12f, paint)
        canvas.drawText("राशि (Amount ₹)", PAGE_WIDTH - MARGIN - colAmtW + 5f, y + 12f, paint)

        y += 18f

        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val advancesOnly = paymentsList.filter { it.type == "ADVANCE" }

        if (advancesOnly.isEmpty()) {
            paint.color = Color.WHITE
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, paint)
            paint.color = borderColor
            paint.style = Paint.Style.STROKE
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, paint)
            paint.style = Paint.Style.FILL

            paint.color = textMuted
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("इस माह कर्मचारी द्वारा कोई एडवांस नहीं लिया गया (No advances taken).", MARGIN + 10f, y + 14f, paint)
            y += 20f
        } else {
            advancesOnly.take(5).forEachIndexed { idx, p ->
                val rowBg = if (idx % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
                paint.color = rowBg
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f, paint)

                paint.color = borderColor
                paint.style = Paint.Style.STROKE
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f, paint)
                paint.style = Paint.Style.FILL

                paint.color = textDark
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawText(sdfDate.format(Date(p.dateMillis)), MARGIN + 8f, y + 13f, paint)
                canvas.drawText("ADVANCE", MARGIN + colDateW + 6f, y + 13f, paint)
                canvas.drawText(p.paymentMode, MARGIN + colDateW + colTypeW + 6f, y + 13f, paint)

                val noteText = p.notes.ifBlank { "Personal advance" }.take(30)
                canvas.drawText(noteText, MARGIN + colDateW + colTypeW + colModeW + 6f, y + 13f, paint)

                paint.color = errorRed
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("₹${String.format(Locale.US, "%,.2f", p.amount)}", PAGE_WIDTH - MARGIN - colAmtW + 5f, y + 13f, paint)

                y += 18f
            }
        }

        // Advance Total Row
        paint.color = Color.rgb(254, 226, 226)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f, paint)
        paint.color = errorRed
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("कुल लिया गया एडवांस (Total Advance Deducted):", MARGIN + 10f, y + 13f, paint)
        canvas.drawText("- ₹${String.format(Locale.US, "%,.2f", totalAdvance)}", PAGE_WIDTH - MARGIN - colAmtW + 5f, y + 13f, paint)

        y += 24f

        // 8. Net Payable Final Balance Box
        val netBoxH = 42f
        paint.color = if (totalPayableWithArrears >= 0) Color.rgb(240, 253, 244) else Color.rgb(254, 242, 242)
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + netBoxH), 6f, 6f, paint)

        paint.color = if (totalPayableWithArrears >= 0) successGreen else errorRed
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + netBoxH), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val netLabel = if (totalPayableWithArrears >= 0) "कुल बाकी देय वेतन (NET PAYABLE SALARY):" else "कर्मचारी पर एडवांस बकाया (EXCESS ADVANCE):"
        canvas.drawText(netLabel, MARGIN + 12f, y + 26f, paint)

        paint.textSize = 15f
        val netVal = "₹${String.format(Locale.US, "%,.2f", kotlin.math.abs(totalPayableWithArrears))}"
        val nvw = paint.measureText(netVal)
        canvas.drawText(netVal, PAGE_WIDTH - MARGIN - nvw - 12f, y + 27f, paint)

        // Settlement Status Tag
        y += netBoxH + 6f
        if (isMonthFinalized) {
            paint.color = Color.rgb(22, 163, 74)
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("✔ इस माह का भुगतान फाइनल है (Status: Finalized)", MARGIN + 4f, y + 12f, paint)
        } else {
            paint.color = Color.rgb(234, 88, 12)
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("⏳ इस माह का भुगतान अनफाइनल है (Status: Unfinalized - बकाया आगे जुड़ेगा)", MARGIN + 4f, y + 12f, paint)
        }

        y += 24f

        // 9. Signatures Section
        val signLineY = PAGE_HEIGHT - 60f

        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, signLineY, MARGIN + 160f, signLineY, paint)
        canvas.drawLine(PAGE_WIDTH - MARGIN - 160f, signLineY, PAGE_WIDTH - MARGIN, signLineY, paint)

        paint.color = textMuted
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("कर्मचारी हस्ताक्षर (Employee Signature)", MARGIN + 8f, signLineY + 12f, paint)
        val authText = "मालिक / प्रोपराइटर हस्ताक्षर (Authorized Sign)"
        val atw = paint.measureText(authText)
        canvas.drawText(authText, PAGE_WIDTH - MARGIN - atw - 8f, signLineY + 12f, paint)

        // Bottom Brand Footer
        paint.color = navyPrimary
        canvas.drawRect(0f, PAGE_HEIGHT - 8f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)
    }

    private fun formatReadableDate(dateString: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfOut = SimpleDateFormat("dd MMM", Locale.getDefault())
            val date = sdfIn.parse(dateString)
            if (date != null) sdfOut.format(date) else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun drawStatBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        label: String,
        value: String,
        valColor: Int,
        borderColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 5f, 5f, paint)

        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 5f, 5f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(label, x + 5f, y + 13f, paint)

        paint.color = valColor
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 5f, y + 27f, paint)
    }

    fun generateCustomDateRangePdf(
        context: Context,
        shopSettings: ShopSettings,
        staff: StaffMember,
        fromDate: String,
        toDate: String,
        attendanceList: List<StaffAttendance>,
        paymentsList: List<StaffPayment>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawCustomRangePdfContent(
            context = context,
            canvas = canvas,
            settings = shopSettings,
            staff = staff,
            fromDate = fromDate,
            toDate = toDate,
            attendanceList = attendanceList,
            paymentsList = paymentsList
        )

        pdfDocument.finishPage(page)

        val pdfsDir = File(context.cacheDir, "staff_salary_slips")
        if (!pdfsDir.exists()) pdfsDir.mkdirs()

        val cleanName = staff.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val cleanFrom = fromDate.replace("[^0-9]".toRegex(), "_")
        val cleanTo = toDate.replace("[^0-9]".toRegex(), "_")
        val outputFile = File(pdfsDir, "StaffReport_${cleanName}_${cleanFrom}_to_${cleanTo}.pdf")

        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return outputFile
    }

    private fun drawCustomRangePdfContent(
        context: Context,
        canvas: Canvas,
        settings: ShopSettings,
        staff: StaffMember,
        fromDate: String,
        toDate: String,
        attendanceList: List<StaffAttendance>,
        paymentsList: List<StaffPayment>
    ) {
        val themeOption = com.example.ui.theme.getAppThemeOption(settings.appTheme)
        val navyPrimary = Color.rgb(
            (themeOption.primary.red * 255).toInt(),
            (themeOption.primary.green * 255).toInt(),
            (themeOption.primary.blue * 255).toInt()
        )
        val textDark = Color.rgb(30, 41, 59)
        val textMuted = Color.rgb(100, 116, 139)
        val borderColor = Color.rgb(226, 232, 240)
        val successGreen = Color.rgb(22, 163, 74)
        val errorRed = Color.rgb(220, 38, 38)
        val warningOrange = Color.rgb(234, 88, 12)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top Accent Bar
        paint.color = navyPrimary
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 6f, paint)

        // Shop Header
        paint.textAlign = Paint.Align.LEFT
        paint.color = navyPrimary
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(settings.shopName.ifBlank { "MOURYA TILES & MARBLES" }, MARGIN, 28f, paint)

        paint.color = textMuted
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        var headerY = 40f
        if (settings.address.isNotBlank()) {
            canvas.drawText(settings.address.take(60), MARGIN, headerY, paint)
            headerY += 12f
        }
        val contactStr = if (settings.ownerPhone.isNotBlank()) "Ph: ${settings.ownerPhone}" else ""
        if (contactStr.isNotBlank()) {
            canvas.drawText(contactStr, MARGIN, headerY, paint)
        }

        // Title & Date on Right
        paint.textAlign = Paint.Align.RIGHT
        paint.color = navyPrimary
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("STAFF STATEMENT REPORT", PAGE_WIDTH - MARGIN, 28f, paint)

        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("अवधि (Period): $fromDate से $toDate", PAGE_WIDTH - MARGIN, 42f, paint)

        val sdfNow = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        paint.color = textMuted
        paint.textSize = 7.5f
        canvas.drawText("Generated: ${sdfNow.format(Date())}", PAGE_WIDTH - MARGIN, 54f, paint)

        // Divider
        paint.color = borderColor
        canvas.drawLine(MARGIN, 62f, PAGE_WIDTH - MARGIN, 62f, paint)

        // Staff Info Card
        val infoBoxTop = 70f
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(MARGIN, infoBoxTop, PAGE_WIDTH - MARGIN, infoBoxTop + 48f), 6f, 6f, paint)
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(RectF(MARGIN, infoBoxTop, PAGE_WIDTH - MARGIN, infoBoxTop + 48f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.textAlign = Paint.Align.LEFT
        paint.color = textDark
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("कर्मचारी विवरण (Staff Details):", MARGIN + 12f, infoBoxTop + 16f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f
        canvas.drawText("नाम: ${staff.name}  |  पद: ${staff.role}  |  मोबाइल: ${staff.phone.ifBlank { "N/A" }}", MARGIN + 12f, infoBoxTop + 30f, paint)
        canvas.drawText("मासिक तय वेतन: ₹${String.format(Locale.US, "%,.2f", staff.monthlySalary)}/माह", MARGIN + 12f, infoBoxTop + 42f, paint)

        // Attendance stats in range
        val presentCount = attendanceList.count { it.status == "PRESENT" }
        val halfDayCount = attendanceList.count { it.status == "HALF_DAY" }
        val absentCount = attendanceList.count { it.status == "ABSENT" }
        val leaveCount = attendanceList.count { it.status == "PAID_LEAVE" }
        val effectiveDays = presentCount + (halfDayCount * 0.5) + leaveCount

        val statY = infoBoxTop + 56f
        val statW = (PAGE_WIDTH - 2 * MARGIN - 24f) / 4f
        drawStatBox(canvas, MARGIN, statY, statW, 32f, "उपस्थित (Present)", "$presentCount दिन", successGreen, borderColor)
        drawStatBox(canvas, MARGIN + statW + 8f, statY, statW, 32f, "हाफ-डे (Half Day)", "$halfDayCount दिन", warningOrange, borderColor)
        drawStatBox(canvas, MARGIN + (statW + 8f) * 2, statY, statW, 32f, "अनुपस्थित (Absent)", "$absentCount दिन", errorRed, borderColor)
        drawStatBox(canvas, MARGIN + (statW + 8f) * 3, statY, statW, 32f, "सवेतन छुट्टी (Leaves)", "$leaveCount दिन", navyPrimary, borderColor)

        // Calculation in range
        var curY = statY + 40f
        val dailyRate = when (staff.salaryCalculationType) {
            "FIXED_30_DAYS" -> if (staff.monthlySalary > 0) staff.monthlySalary / 30.0 else 0.0
            "ACTUAL_MONTH_DAYS" -> if (staff.monthlySalary > 0) staff.monthlySalary / 30.0 else 0.0
            "WORKING_DAYS" -> {
                val wDays = if (staff.workingDaysPerMonth > 0) staff.workingDaysPerMonth else 26
                if (staff.monthlySalary > 0) staff.monthlySalary / wDays.toDouble() else 0.0
            }
            "DAILY_WAGE" -> staff.dailyWageRate
            else -> if (staff.monthlySalary > 0) staff.monthlySalary / 30.0 else 0.0
        }
        val earnedSalary = dailyRate * effectiveDays

        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 44f), 6f, 6f, paint)
        paint.color = textDark
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("प्रभावी कार्य दिवस (Effective Days): $effectiveDays दिन (दैनिक दर: ₹${String.format(Locale.US, "%,.2f", dailyRate)})", MARGIN + 12f, curY + 16f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("इस अवधि का कुल अर्जित वेतन (Earned Base): ₹${String.format(Locale.US, "%,.2f", earnedSalary)}", MARGIN + 12f, curY + 32f, paint)

        curY += 52f

        // Itemized Payments in range table
        paint.color = navyPrimary
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("अवधि में भुगतान व एडवांस विवरण (Payments & Advances in Range):", MARGIN, curY + 8f, paint)

        curY += 14f

        val colDateW = 80f
        val colTypeW = 85f
        val colModeW = 60f
        val colAmtW = 85f

        paint.color = navyPrimary
        canvas.drawRect(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 16f, paint)
        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("दिनांक (Date)", MARGIN + 8f, curY + 11f, paint)
        canvas.drawText("प्रकार (Type)", MARGIN + colDateW + 6f, curY + 11f, paint)
        canvas.drawText("माध्यम (Mode)", MARGIN + colDateW + colTypeW + 6f, curY + 11f, paint)
        canvas.drawText("विवरण / नोट (Purpose)", MARGIN + colDateW + colTypeW + colModeW + 6f, curY + 11f, paint)
        canvas.drawText("राशि (Amount ₹)", PAGE_WIDTH - MARGIN - colAmtW + 5f, curY + 11f, paint)

        curY += 16f
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var totalAdvancesTaken = 0.0

        if (paymentsList.isEmpty()) {
            paint.color = Color.WHITE
            canvas.drawRect(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 18f, paint)
            paint.color = textMuted
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("इस अवधि में कोई भुगतान या एडवांस दर्ज नहीं है।", MARGIN + 10f, curY + 12f, paint)
            curY += 18f
        } else {
            paymentsList.take(10).forEachIndexed { idx, p ->
                if (p.type == "ADVANCE") totalAdvancesTaken += p.amount
                val rowBg = if (idx % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
                paint.color = rowBg
                canvas.drawRect(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 16f, paint)
                paint.color = borderColor
                paint.style = Paint.Style.STROKE
                canvas.drawRect(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 16f, paint)
                paint.style = Paint.Style.FILL

                paint.color = textDark
                paint.textSize = 8f
                paint.typeface = Typeface.DEFAULT
                canvas.drawText(sdfDate.format(Date(p.dateMillis)), MARGIN + 8f, curY + 11f, paint)
                canvas.drawText(p.type, MARGIN + colDateW + 6f, curY + 11f, paint)
                canvas.drawText(p.paymentMode, MARGIN + colDateW + colTypeW + 6f, curY + 11f, paint)
                canvas.drawText(p.notes.ifBlank { "-" }.take(25), MARGIN + colDateW + colTypeW + colModeW + 6f, curY + 11f, paint)

                paint.color = if (p.type == "ADVANCE") errorRed else successGreen
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("₹${String.format(Locale.US, "%,.2f", p.amount)}", PAGE_WIDTH - MARGIN - colAmtW + 5f, curY + 11f, paint)

                curY += 16f
            }
        }

        // Net summary in range
        curY += 12f
        val netRangePayable = earnedSalary - totalAdvancesTaken

        val netBoxH = 38f
        paint.color = if (netRangePayable >= 0) Color.rgb(240, 253, 244) else Color.rgb(254, 242, 242)
        canvas.drawRoundRect(RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + netBoxH), 6f, 6f, paint)
        paint.color = if (netRangePayable >= 0) successGreen else errorRed
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        canvas.drawRoundRect(RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + netBoxH), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.textAlign = Paint.Align.LEFT
        paint.color = textDark
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("इस अवधि का कुल बाकी देय (NET PAYABLE FOR PERIOD):", MARGIN + 12f, curY + 23f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = if (netRangePayable >= 0) successGreen else errorRed
        paint.textSize = 13f
        canvas.drawText("₹${String.format(Locale.US, "%,.2f", kotlin.math.abs(netRangePayable))}", PAGE_WIDTH - MARGIN - 12f, curY + 24f, paint)

        // Signatures at bottom
        val signLineY = PAGE_HEIGHT - 60f
        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, signLineY, MARGIN + 160f, signLineY, paint)
        canvas.drawLine(PAGE_WIDTH - MARGIN - 160f, signLineY, PAGE_WIDTH - MARGIN, signLineY, paint)

        paint.color = textMuted
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("कर्मचारी के हस्ताक्षर (Staff Signature)", MARGIN, signLineY + 15f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("मालिक / मुनीम के हस्ताक्षर (Proprietor)", PAGE_WIDTH - MARGIN, signLineY + 15f, paint)

        paint.color = navyPrimary
        canvas.drawRect(0f, PAGE_HEIGHT - 6f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)
    }
}
