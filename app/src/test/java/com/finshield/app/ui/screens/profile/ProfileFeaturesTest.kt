package com.finshield.app.ui.screens.profile

import com.finshield.app.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFeaturesTest {

    @Test
    fun testContactSupportPhoneConfiguration() {
        assertEquals("+91 9689718945", ContactSupportConfig.PHONE_NUMBER_DISPLAY)
        assertEquals("tel:+919689718945", ContactSupportConfig.PHONE_NUMBER_TEL_URI)
    }

    @Test
    fun testContactSupportDisclaimersAndPrivacyReminder() {
        // Must contain privacy warning regarding OTPs, PINs, passwords
        val reminder = ContactSupportConfig.PRIVACY_REMINDER
        assertTrue(reminder.contains("OTP", ignoreCase = true))
        assertTrue(reminder.contains("UPI PIN", ignoreCase = true))
        assertTrue(reminder.contains("passwords", ignoreCase = true))

        // Must clarify guidance vs formal legal/bureau authority
        val disclaimer = ContactSupportConfig.GUIDANCE_DISCLAIMER
        assertTrue(disclaimer.contains("FinShield support", ignoreCase = true))
        assertTrue(disclaimer.contains("formal legal advice", ignoreCase = true))
        assertTrue(disclaimer.contains("official institution", ignoreCase = true))
    }

    @Test
    fun testAssistantRouteProtected() {
        assertEquals("assistant", Routes.ASSISTANT)
        assertTrue(Routes.protectedRoutes.contains(Routes.ASSISTANT))
    }
}
