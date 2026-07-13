package com.sarrows.app.data.native

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kotlin wrapper around the NDK native security layer.
 * All sensitive operations (cookie storage, CSRF handling, URL building,
 * header construction) are delegated to C++ via JNI.
 */
@Singleton
class NativeSecurity @Inject constructor(private val context: Context) {

    companion object {
        private const val KEYSTORE_ALIAS = "sarrows_enc_key"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

        init {
            System.loadLibrary("sarrows_native")
        }
    }

    // ── JNI declarations ────────────────────────────────────────────────────
    private external fun nativeInit(encryptionKey: String)
    private external fun nativeDestroy()
    external fun nativeSetCookie(name: String, value: String)
    external fun nativeGetCookie(name: String): String
    external fun nativeParseCookies(setCookieBlock: String)
    external fun nativeClearCookies()
    external fun nativeHasSession(): Boolean
    external fun nativeBuildCookieHeader(): String
    external fun nativeStoreCsrfToken(token: String)
    external fun nativeGetCsrfToken(): String
    external fun nativeBuildAuthHeaders(): String
    external fun nativeBuildLoginBody(email: String, password: String, csrfToken: String): String
    external fun nativeBuildSignoutBody(csrfToken: String): String
    external fun nativeMovieStreamUrl(id: String): String
    external fun nativeMovieEmbedUrl(id: String): String
    external fun nativeEpisodeStreamUrl(id: String): String
    external fun nativeEpisodeEmbedUrl(id: String): String
    external fun nativeCheckStreamRateLimit(): Boolean
    external fun nativeRecordStreamRequest()
    external fun nativeRotateKey(newKey: String)

    // ── Initialisation ───────────────────────────────────────────────────────

    fun init() {
        val key = getOrCreateEncryptionKey()
        nativeInit(key)
    }

    fun destroy() = nativeDestroy()

    // ── Auth headers as Map ──────────────────────────────────────────────────

    /** Parses the newline-separated "Key: Value" string from C++ into a map. */
    fun buildAuthHeadersMap(): Map<String, String> {
        val raw = nativeBuildAuthHeaders()
        return raw.trim().lines()
            .filter { it.contains(": ") }
            .associate { line ->
                val colonIdx = line.indexOf(": ")
                line.substring(0, colonIdx) to line.substring(colonIdx + 2)
            }
    }

    // ── Android Keystore – encryption key management ─────────────────────────

    private fun getOrCreateEncryptionKey(): String {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGen = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER
            )
            keyGen.init(
                KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGen.generateKey()
        }
        val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        // Use encoded bytes as a hex-string seed for the C++ XOR keystream.
        return secretKey.encoded?.let { bytes ->
            bytes.joinToString("") { "%02x".format(it) }
        } ?: run {
            // Keystore keys may not expose raw bytes; derive from device ID + app package.
            val seed = context.packageName + android.os.Build.SERIAL + KEYSTORE_ALIAS
            seed.hashCode().toString(16) + seed.length.toString(16)
        }
    }
}
