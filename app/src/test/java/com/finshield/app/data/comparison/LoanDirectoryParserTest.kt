package com.finshield.app.data.comparison

import com.finshield.app.model.LenderClassification
import com.finshield.app.model.LoanCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanDirectoryParserTest {

    @Test
    fun testLoadsAll570DirectoryRecordsFromAsset() {
        val stream = javaClass.classLoader?.getResourceAsStream("loan_directory.csv")
            ?: javaClass.getResourceAsStream("/loan_directory.csv")
        assertNotNull("loan_directory.csv resource must be available", stream)

        val records = LoanDirectoryParser.parse(stream!!)
        assertEquals(570, records.size)

        // Verify all 30 banks are represented
        val banks = records.map { it.bank }.distinct()
        assertEquals(30, banks.size)

        // Verify all 19 loan types are represented
        val loanTypes = records.map { it.loanType }.distinct()
        assertEquals(19, loanTypes.size)

        // Verify each loan type has exactly 30 bank entries
        for (type in loanTypes) {
            val count = records.count { it.loanType == type }
            assertEquals("Loan type $type should have 30 records", 30, count)
        }
    }

    @Test
    fun testAll19LoanTypesMapToValidLoanCategory() {
        val stream = javaClass.classLoader?.getResourceAsStream("loan_directory.csv")
            ?: javaClass.getResourceAsStream("/loan_directory.csv")
        val records = LoanDirectoryParser.parse(stream!!)

        val categories = records.map { LoanCategory.fromLabel(it.loanType) }.distinct()
        assertEquals(19, categories.size)

        // Verify specific key categories
        assertTrue(categories.contains(LoanCategory.HOME))
        assertTrue(categories.contains(LoanCategory.PERSONAL))
        assertTrue(categories.contains(LoanCategory.EDUCATION))
        assertTrue(categories.contains(LoanCategory.CAR))
        assertTrue(categories.contains(LoanCategory.GOLD))
        assertTrue(categories.contains(LoanCategory.BUSINESS))
        assertTrue(categories.contains(LoanCategory.OVERDRAFT))
        assertTrue(categories.contains(LoanCategory.PROPERTY))
    }

    @Test
    fun testHomeLoanCategoryHasExpected30Banks() {
        val stream = javaClass.classLoader?.getResourceAsStream("loan_directory.csv")
            ?: javaClass.getResourceAsStream("/loan_directory.csv")
        val records = LoanDirectoryParser.parse(stream!!)

        val homeLoans = records.filter { it.loanType.equals("Home Loan", ignoreCase = true) }
        assertEquals(30, homeLoans.size)

        // Check State Bank of India Home Loan
        val sbiHome = homeLoans.firstOrNull { it.bank == "State Bank of India" }
        assertNotNull(sbiHome)
        assertTrue(sbiHome!!.securityType.contains("property", ignoreCase = true))
        assertEquals("Verify on bank website", sbiHome.availabilityStatus)
        assertTrue(sbiHome.officialProductUrl.contains("sbi.bank.in"))
        assertTrue(sbiHome.ratesChargesUrl.contains("sbi.bank.in"))
        assertEquals("2026-10-24", sbiHome.lastChecked)
    }

    @Test
    fun testMissingNumericRatesRemainNullAndNotZero() {
        val stream = javaClass.classLoader?.getResourceAsStream("loan_directory.csv")
            ?: javaClass.getResourceAsStream("/loan_directory.csv")
        val records = LoanDirectoryParser.parse(stream!!)

        // In the directory, rate columns are blank until verified
        for (rec in records) {
            assertNull("Min rate must be null if unlisted", rec.minAnnualRate)
            assertNull("Max rate must be null if unlisted", rec.maxAnnualRate)

            val offer = rec.toLoanOffer()
            assertNull(offer.minAnnualRate)
            assertNull(offer.maxAnnualRate)
            assertEquals("Rate not listed — check lender", offer.publishedInterestRate)
            assertFalse(offer.isComparableStartingRate)
        }
    }

    @Test
    fun testDeterministicIdsDerivedFromBankAndLoanType() {
        val id1 = LoanDirectoryParser.generateOfferId("State Bank of India", "Home Loan")
        assertEquals("state-bank-of-india-home-loan", id1)

        val id2 = LoanDirectoryParser.generateOfferId("HDFC Bank", "Car / Auto Loan")
        assertEquals("hdfc-bank-car-auto-loan", id2)

        val id3 = LoanDirectoryParser.generateOfferId("Bank of Baroda", "Overdraft / Cash Credit")
        assertEquals("bank-of-baroda-overdraft-cash-credit", id3)

        // Ensure bank name alone is never used as ID
        assertFalse(id1 == "state-bank-of-india")
        assertFalse(id2 == "hdfc-bank")
    }

    @Test
    fun testBankClassificationSeparatesPublicAndPrivateBanks() {
        assertEquals(LenderClassification.PUBLIC_BANK, LenderClassification.fromBankName("State Bank of India"))
        assertEquals(LenderClassification.PUBLIC_BANK, LenderClassification.fromBankName("Bank of Baroda"))
        assertEquals(LenderClassification.PUBLIC_BANK, LenderClassification.fromBankName("Punjab National Bank"))
        assertEquals(LenderClassification.PUBLIC_BANK, LenderClassification.fromBankName("Canara Bank"))

        assertEquals(LenderClassification.PRIVATE_BANK, LenderClassification.fromBankName("HDFC Bank"))
        assertEquals(LenderClassification.PRIVATE_BANK, LenderClassification.fromBankName("ICICI Bank"))
        assertEquals(LenderClassification.PRIVATE_BANK, LenderClassification.fromBankName("Axis Bank"))
        assertEquals(LenderClassification.PRIVATE_BANK, LenderClassification.fromBankName("AU Small Finance Bank"))
    }
}
