#include "cookie_manager.h"
#include <android/log.h>
#include <sstream>
#include <algorithm>

#define LOG_TAG "SarrowsCookies"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace sarrows {

// Cookie names we care about
static const char* SESSION_COOKIE = "authjs.session-token";

CookieManager::CookieManager(const std::string& encryptionKey)
    : encryptionKey_(encryptionKey) {}

CookieManager::~CookieManager() {
    clearAll();
    CryptoUtils::secureClear(encryptionKey_);
}

std::string CookieManager::encryptValue(const std::string& value) const {
    auto cipher = CryptoUtils::encrypt(value, encryptionKey_);
    return CryptoUtils::toHex(cipher);
}

std::string CookieManager::decryptValue(const std::string& hexCipher) const {
    auto cipher = CryptoUtils::fromHex(hexCipher);
    return CryptoUtils::decrypt(cipher, encryptionKey_);
}

void CookieManager::setCookie(const std::string& name, const std::string& value) {
    std::lock_guard<std::mutex> lock(mutex_);
    store_[name] = encryptValue(value);
}

std::string CookieManager::getCookie(const std::string& name) const {
    std::lock_guard<std::mutex> lock(mutex_);
    auto it = store_.find(name);
    if (it == store_.end()) return {};
    return decryptValue(it->second);
}

void CookieManager::removeCookie(const std::string& name) {
    std::lock_guard<std::mutex> lock(mutex_);
    auto it = store_.find(name);
    if (it != store_.end()) {
        CryptoUtils::secureClear(it->second);
        store_.erase(it);
    }
}

void CookieManager::clearAll() {
    std::lock_guard<std::mutex> lock(mutex_);
    for (auto& [k, v] : store_) {
        CryptoUtils::secureClear(v);
    }
    store_.clear();
}

std::string CookieManager::buildCookieHeader() const {
    std::lock_guard<std::mutex> lock(mutex_);
    std::ostringstream oss;
    bool first = true;
    for (const auto& [name, hexCipher] : store_) {
        auto value = decryptValue(hexCipher);
        if (!value.empty()) {
            if (!first) oss << "; ";
            oss << name << "=" << value;
            first = false;
            CryptoUtils::secureClear(value);
        }
    }
    return oss.str();
}

void CookieManager::parseAndStoreCookies(const std::string& setCookieBlock) {
    // Process one Set-Cookie line at a time.
    std::istringstream stream(setCookieBlock);
    std::string line;
    while (std::getline(stream, line)) {
        if (line.empty()) continue;
        // Trim \r
        if (!line.empty() && line.back() == '\r') line.pop_back();

        // The first segment before ; is name=value
        auto semi = line.find(';');
        std::string nameValue = (semi != std::string::npos) ? line.substr(0, semi) : line;

        auto eq = nameValue.find('=');
        if (eq == std::string::npos) continue;

        std::string name = nameValue.substr(0, eq);
        std::string value = nameValue.substr(eq + 1);

        // Trim whitespace
        auto trim = [](std::string& s) {
            s.erase(s.begin(), std::find_if(s.begin(), s.end(), [](unsigned char c){ return !std::isspace(c); }));
            s.erase(std::find_if(s.rbegin(), s.rend(), [](unsigned char c){ return !std::isspace(c); }).base(), s.end());
        };
        trim(name);
        trim(value);

        if (!name.empty() && !value.empty()) {
            std::lock_guard<std::mutex> lock(mutex_);
            store_[name] = encryptValue(value);
            LOGD("Stored cookie: %s (len=%zu)", name.c_str(), value.size());
            CryptoUtils::secureClear(value);
        }
    }
}

bool CookieManager::hasSession() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return store_.count(SESSION_COOKIE) > 0;
}

void CookieManager::rotateKey(const std::string& newKey) {
    std::lock_guard<std::mutex> lock(mutex_);
    // Re-encrypt every stored value with the new key.
    for (auto& [name, hexCipher] : store_) {
        auto plain = decryptValue(hexCipher);
        CryptoUtils::secureClear(hexCipher);
        auto newCipher = CryptoUtils::encrypt(plain, newKey);
        hexCipher = CryptoUtils::toHex(newCipher);
        CryptoUtils::secureClear(plain);
        CryptoUtils::secureClear(newCipher);
    }
    CryptoUtils::secureClear(encryptionKey_);
    encryptionKey_ = newKey;
}

} // namespace sarrows
