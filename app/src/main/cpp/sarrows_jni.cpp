#include <jni.h>
#include <string>
#include <memory>
#include <android/log.h>
#include "cookie_manager.h"
#include "api_security.h"
#include "crypto_utils.h"

#define LOG_TAG "SarrowsJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// --------------------------------------------------------------------------
// Singleton security context — lives for the app lifetime.
// --------------------------------------------------------------------------
static std::unique_ptr<sarrows::CookieManager> g_cookieManager;
static std::unique_ptr<sarrows::ApiSecurity>   g_apiSecurity;

static std::string jstring2std(JNIEnv* env, jstring str) {
    if (!str) return {};
    const char* chars = env->GetStringUTFChars(str, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(str, chars);
    return result;
}

static jstring std2jstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

extern "C" {

// ── Initialisation ──────────────────────────────────────────────────────────

JNIEXPORT void JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeInit(
        JNIEnv* env, jobject /* thiz */, jstring encryptionKey) {
    std::string key = jstring2std(env, encryptionKey);
    g_cookieManager = std::make_unique<sarrows::CookieManager>(key);
    g_apiSecurity   = std::make_unique<sarrows::ApiSecurity>(g_cookieManager.get());
    sarrows::CryptoUtils::secureClear(key);
    LOGI("NativeSecurity initialised");
}

JNIEXPORT void JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeDestroy(
        JNIEnv* /* env */, jobject /* thiz */) {
    g_apiSecurity.reset();
    g_cookieManager.reset();
    LOGI("NativeSecurity destroyed");
}

// ── Cookie management ───────────────────────────────────────────────────────

JNIEXPORT void JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeSetCookie(
        JNIEnv* env, jobject /* thiz */, jstring name, jstring value) {
    if (!g_cookieManager) return;
    g_cookieManager->setCookie(jstring2std(env, name), jstring2std(env, value));
}

JNIEXPORT jstring JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeGetCookie(
        JNIEnv* env, jobject /* thiz */, jstring name) {
    if (!g_cookieManager) return std2jstring(env, {});
    return std2jstring(env, g_cookieManager->getCookie(jstring2std(env, name)));
}

JNIEXPORT void JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeParseCookies(
        JNIEnv* env, jobject /* thiz */, jstring setCookieBlock) {
    if (!g_cookieManager) return;
    g_cookieManager->parseAndStoreCookies(jstring2std(env, setCookieBlock));
}

JNIEXPORT void JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeClearCookies(
        JNIEnv* /* env */, jobject /* thiz */) {
    if (g_cookieManager) g_cookieManager->clearAll();
    if (g_apiSecurity)   g_apiSecurity->clearCsrfToken();
}

JNIEXPORT jboolean JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeHasSession(
        JNIEnv* /* env */, jobject /* thiz */) {
    if (!g_cookieManager) return JNI_FALSE;
    return g_cookieManager->hasSession() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeBuildCookieHeader(
        JNIEnv* env, jobject /* thiz */) {
    if (!g_cookieManager) return std2jstring(env, {});
    return std2jstring(env, g_cookieManager->buildCookieHeader());
}

// ── CSRF ────────────────────────────────────────────────────────────────────

JNIEXPORT void JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeStoreCsrfToken(
        JNIEnv* env, jobject /* thiz */, jstring token) {
    if (g_apiSecurity) g_apiSecurity->storeCsrfToken(jstring2std(env, token));
}

JNIEXPORT jstring JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeGetCsrfToken(
        JNIEnv* env, jobject /* thiz */) {
    if (!g_apiSecurity) return std2jstring(env, {});
    return std2jstring(env, g_apiSecurity->getCsrfToken());
}

// ── Header building ─────────────────────────────────────────────────────────

// Returns newline-separated "Key: Value" pairs.
JNIEXPORT jstring JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeBuildAuthHeaders(
        JNIEnv* env, jobject /* thiz */) {
    if (!g_apiSecurity) return std2jstring(env, {});
    auto headers = g_apiSecurity->buildAuthHeaders();
    std::string result;
    for (const auto& [k, v] : headers) {
        result += k + ": " + v + "\n";
    }
    return std2jstring(env, result);
}

// ── Login / Signout body builders ───────────────────────────────────────────

JNIEXPORT jstring JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeBuildLoginBody(
        JNIEnv* env, jobject /* thiz */,
        jstring email, jstring password, jstring csrfToken) {
    if (!g_apiSecurity) return std2jstring(env, {});
    return std2jstring(env, g_apiSecurity->buildLoginBody(
            jstring2std(env, email),
            jstring2std(env, password),
            jstring2std(env, csrfToken)));
}

JNIEXPORT jstring JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeBuildSignoutBody(
        JNIEnv* env, jobject /* thiz */, jstring csrfToken) {
    if (!g_apiSecurity) return std2jstring(env, {});
    return std2jstring(env, g_apiSecurity->buildSignoutBody(jstring2std(env, csrfToken)));
}

// ── URL helpers ─────────────────────────────────────────────────────────────

JNIEXPORT jstring JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeMovieStreamUrl(
        JNIEnv* env, jobject /* thiz */, jstring id) {
    if (!g_apiSecurity) return std2jstring(env, {});
    return std2jstring(env, g_apiSecurity->movieStreamUrl(jstring2std(env, id)));
}

JNIEXPORT jstring JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeMovieEmbedUrl(
        JNIEnv* env, jobject /* thiz */, jstring id) {
    if (!g_apiSecurity) return std2jstring(env, {});
    return std2jstring(env, g_apiSecurity->movieEmbedUrl(jstring2std(env, id)));
}

JNIEXPORT jstring JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeEpisodeStreamUrl(
        JNIEnv* env, jobject /* thiz */, jstring id) {
    if (!g_apiSecurity) return std2jstring(env, {});
    return std2jstring(env, g_apiSecurity->episodeStreamUrl(jstring2std(env, id)));
}

JNIEXPORT jstring JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeEpisodeEmbedUrl(
        JNIEnv* env, jobject /* thiz */, jstring id) {
    if (!g_apiSecurity) return std2jstring(env, {});
    return std2jstring(env, g_apiSecurity->episodeEmbedUrl(jstring2std(env, id)));
}

// ── Rate limiting ────────────────────────────────────────────────────────────

JNIEXPORT jboolean JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeCheckStreamRateLimit(
        JNIEnv* /* env */, jobject /* thiz */) {
    if (!g_apiSecurity) return JNI_FALSE;
    return g_apiSecurity->checkStreamRateLimit() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeRecordStreamRequest(
        JNIEnv* /* env */, jobject /* thiz */) {
    if (g_apiSecurity) g_apiSecurity->recordStreamRequest();
}

// ── Key rotation ─────────────────────────────────────────────────────────────

JNIEXPORT void JNICALL
Java_com_sarrows_app_data_native_NativeSecurity_nativeRotateKey(
        JNIEnv* env, jobject /* thiz */, jstring newKey) {
    if (g_cookieManager) {
        std::string key = jstring2std(env, newKey);
        g_cookieManager->rotateKey(key);
        sarrows::CryptoUtils::secureClear(key);
    }
}

} // extern "C"
