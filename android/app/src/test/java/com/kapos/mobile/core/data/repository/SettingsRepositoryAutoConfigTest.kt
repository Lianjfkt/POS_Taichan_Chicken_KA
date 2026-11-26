package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryAutoConfigTest {

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        // Fresh install without stored preferences
        repository = SettingsRepository(context = null)
    }

    @Test
    fun testDefaultSupabaseCredentialsArePopulatedOnFreshInstall() {
        val settings = repository.settingsFlow.value
        assertEquals(AppConfig.DEFAULT_SUPABASE_URL, settings.supabaseUrl)
        assertEquals(AppConfig.DEFAULT_SUPABASE_ANON_KEY, settings.supabaseAnonKey)
        assertTrue(settings.supabaseUrl.isNotBlank())
        assertTrue(settings.supabaseAnonKey.isNotBlank())
    }

    @Test
    fun testCustomSupabaseCredentialsCanOverrideDefault() {
        val customUrl = "https://custom-project.supabase.co"
        val customKey = "custom-anon-key-12345"

        repository.saveSettings(
            repository.settingsFlow.value.copy(
                supabaseUrl = customUrl,
                supabaseAnonKey = customKey
            )
        )

        val updated = repository.settingsFlow.value
        assertEquals(customUrl, updated.supabaseUrl)
        assertEquals(customKey, updated.supabaseAnonKey)
    }
}
