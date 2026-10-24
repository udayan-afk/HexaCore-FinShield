package com.finshield.app.data.comparison

import com.finshield.app.model.LenderClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LenderCsvParserTest {

    @Test
    fun testLoadsAll32ValidLenderRecordsFromAsset() {
        val stream = javaClass.classLoader?.getResourceAsStream("lenders.csv")
            ?: javaClass.getResourceAsStream("/lenders.csv")
        assertNotNull("lenders.csv resource must be available", stream)

        val records = LenderCsvParser.parse(stream!!)
        assertEquals(31, records.size)

        // Verify dataset metadata
        assertTrue(records.all { it.datasetVersion == "2026.10-research" })
        assertTrue(records.all { it.datasetStatus.contains("Indicative") })
    }

    @Test
    fun testHandlesQuotedCommasAndSpecialCharacters() {
        val stream = javaClass.classLoader?.getResourceAsStream("lenders.csv")
            ?: javaClass.getResourceAsStream("/lenders.csv")
        val records = LenderCsvParser.parse(stream!!)

        // Bank of Baroda row contains quoted commas in fee: "Up to 2% (max ₹10,000 in some schemes) + taxes"
        val bob = records.firstOrNull { it.lenderName == "Bank of Baroda" }
        assertNotNull(bob)
        assertEquals("Up to 2% (max ₹10,000 in some schemes) + taxes", bob!!.publishedProcessingFee)
        assertEquals("Public Sector Bank", bob.rawLenderType)
        assertEquals(LenderClassification.PUBLIC_BANK, bob.lenderType)

        // Axis Bank row contains quotes in notes
        val axis = records.firstOrNull { it.lenderName == "Axis Bank" }
        assertNotNull(axis)
        assertEquals(
            "Profile-dependent; verify official age, income, employment and credit rules.",
            axis!!.eligibilityNotes
        )
    }

    @Test
    fun testDistinguishesMonthlyRatesFromAnnualRates() {
        val stream = javaClass.classLoader?.getResourceAsStream("lenders.csv")
            ?: javaClass.getResourceAsStream("/lenders.csv")
        val records = LenderCsvParser.parse(stream!!)

        // CASHe: 2.79% per month
        val cashe = records.first { it.lenderName == "CASHe" }
        assertTrue(cashe.isMonthlyRate)
        assertFalse(cashe.isComparableStartingRate)
        assertNull(cashe.minAnnualRate)
        assertEquals(2.79, cashe.monthlyRatePercent!!, 0.01)

        // Home Credit India: 1.6% per month
        val homeCredit = records.first { it.lenderName == "Home Credit India" }
        assertTrue(homeCredit.isMonthlyRate)
        assertFalse(homeCredit.isComparableStartingRate)
        assertNull(homeCredit.minAnnualRate)
        assertEquals(1.6, homeCredit.monthlyRatePercent!!, 0.01)

        // MoneyTap: 1.25% per month
        val moneyTap = records.first { it.lenderName == "MoneyTap" }
        assertTrue(moneyTap.isMonthlyRate)
        assertFalse(moneyTap.isComparableStartingRate)

        // PaySense: 1.4% per month
        val paySense = records.first { it.lenderName == "PaySense" }
        assertTrue(paySense.isMonthlyRate)
        assertFalse(paySense.isComparableStartingRate)

        // Total monthly rate lenders must be exactly 4
        val monthlyLenders = records.filter { it.isMonthlyRate }
        assertEquals(4, monthlyLenders.size)

        // mPokket: Up to 36% p.a. (Starting rate unspecified/ambiguous)
        val mpokket = records.first { it.lenderName == "mPokket" }
        assertFalse(mpokket.isMonthlyRate)
        assertFalse(mpokket.isComparableStartingRate)
        assertNull(mpokket.minAnnualRate)
        assertEquals(36.0, mpokket.maxAnnualRate!!, 0.01)
    }

    @Test
    fun testParsesIndianCurrencyAmounts() {
        assertEquals(4_000_000.0, LenderCsvParser.parseIndianCurrencyAmount("₹40 lakh")!!, 0.1)
        assertEquals(10_000_000.0, LenderCsvParser.parseIndianCurrencyAmount("₹1 crore")!!, 0.1)
        assertEquals(480_000.0, LenderCsvParser.parseIndianCurrencyAmount("₹4.80 lakh")!!, 0.1)
        assertEquals(5_500_000.0, LenderCsvParser.parseIndianCurrencyAmount("₹55 lakh")!!, 0.1)
        assertEquals(200_000.0, LenderCsvParser.parseIndianCurrencyAmount("₹2 lakh")!!, 0.1)
    }

    @Test
    fun testLenderClassificationIntegrity() {
        val stream = javaClass.classLoader?.getResourceAsStream("lenders.csv")
            ?: javaClass.getResourceAsStream("/lenders.csv")
        val records = LenderCsvParser.parse(stream!!)

        val publicBanks = records.filter { it.lenderType == LenderClassification.PUBLIC_BANK }
        val privateBanks = records.filter { it.lenderType == LenderClassification.PRIVATE_BANK }
        val nbfcs = records.filter { it.lenderType == LenderClassification.NBFC }
        val digitalLenders = records.filter { it.lenderType == LenderClassification.DIGITAL_LENDER }

        assertEquals(9, publicBanks.size)
        assertEquals(9, privateBanks.size)
        assertEquals(5, nbfcs.size)
        assertEquals(8, digitalLenders.size)
        assertEquals(31, publicBanks.size + privateBanks.size + nbfcs.size + digitalLenders.size)

        // NBFCs must never be classified as banks
        assertTrue(nbfcs.all { it.lenderType == LenderClassification.NBFC })
        assertTrue(nbfcs.none { it.lenderType.shortLabel.contains("Bank", ignoreCase = true) })

        // Digital lenders must never be classified as banks
        assertTrue(digitalLenders.all { it.lenderType == LenderClassification.DIGITAL_LENDER })
        assertTrue(digitalLenders.none { it.lenderType.shortLabel.contains("Bank", ignoreCase = true) })
    }

    @Test
    fun testSortingComparableAnnualRates() {
        val stream = javaClass.classLoader?.getResourceAsStream("lenders.csv")
            ?: javaClass.getResourceAsStream("/lenders.csv")
        val records = LenderCsvParser.parse(stream!!)

        val comparableRecords = records.filter { it.isComparableStartingRate && it.minAnnualRate != null }
        // 31 minus 4 monthly lenders minus 1 cap-only lender = 26 comparable lenders
        assertEquals(26, comparableRecords.size)

        val sorted = comparableRecords.sortedBy { it.minAnnualRate }

        // Lowest starting rate must be Bandhan Bank (8.86%)
        assertEquals("Bandhan Bank", sorted.first().lenderName)
        assertEquals(8.86, sorted.first().minAnnualRate!!, 0.01)

        // Second lowest must be Indian Bank (9.00%)
        assertEquals("Indian Bank", sorted[1].lenderName)
        assertEquals(9.00, sorted[1].minAnnualRate!!, 0.01)

        // Highest starting rate in comparable set is Kissht (18.00%)
        assertEquals(18.00, sorted.last().minAnnualRate!!, 0.01)
    }

    @Test
    fun testMissingOrInvalidFieldsGracefulFallback() {
        val emptyRow = emptyList<String>()
        assertNull(LenderCsvParser.parseRowToRecord(emptyRow))

        val shortRow = listOf("Incomplete Lender", "Private Bank")
        assertNull(LenderCsvParser.parseRowToRecord(shortRow))

        val validMinimal = listOf(
            "Test Bank",
            "Private Bank",
            "10.5% onwards",
            "₹10 lakh",
            "60",
            "2% + GST",
        )
        val record = LenderCsvParser.parseRowToRecord(validMinimal)
        assertNotNull(record)
        assertEquals("Test Bank", record!!.lenderName)
        assertEquals(LenderClassification.PRIVATE_BANK, record.lenderType)
        assertEquals(10.5, record.minAnnualRate!!, 0.01)
        assertEquals(1_000_000.0, record.parsedMaxLoanAmount!!, 0.1)
    }
}
