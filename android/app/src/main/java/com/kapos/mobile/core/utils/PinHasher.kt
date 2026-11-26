package com.kapos.mobile.core.utils

import java.security.MessageDigest

/**
 * Utility untuk hashing PIN menggunakan SHA-256 + salt tetap per-app.
 *
 * Kenapa SHA-256 dan bukan bcrypt?
 * Android tidak menyertakan bcrypt di standard library, dan untuk PIN 4-digit
 * yang user-defined, SHA-256 + salt sudah jauh lebih aman dari plaintext.
 * Untuk keamanan level enterprise, pertimbangkan Argon2 via library terpisah.
 */
object PinHasher {

    // Salt unik per-app. Bisa diganti jadi per-device (misal: pakai Android ID)
    // tapi pastikan konsisten agar verifikasi tetap valid.
    private const val APP_SALT = "KA_POS_2026_SALT_V1"

    /**
     * Hash PIN dengan SHA-256 + salt.
     * Kembalikan hex string 64 karakter.
     */
    fun hash(rawPin: String): String {
        val salted = "$APP_SALT:$rawPin"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifikasi PIN raw terhadap hash yang tersimpan.
     * Aman terhadap timing attack (constant-time comparison via MessageDigest.isEqual).
     */
    fun verify(rawPin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        val inputHash = hash(rawPin)
        return MessageDigest.isEqual(
            inputHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Cek apakah string yang tersimpan adalah hash (64 hex chars) atau plaintext lama.
     * Digunakan untuk migrasi PIN lama → hashed secara otomatis.
     */
    fun isHashed(value: String): Boolean = value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }
}
