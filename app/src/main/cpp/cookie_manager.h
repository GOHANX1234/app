#pragma once
#include <string>
#include <unordered_map>
#include <mutex>
#include "crypto_utils.h"

namespace sarrows {

// Thread-safe, in-memory encrypted cookie store.
// The session token never sits in plaintext in heap memory — it is
// encrypted immediately after being received and decrypted only
// when it needs to be sent on the wire.
class CookieManager {
public:
    explicit CookieManager(const std::string& encryptionKey);
    ~CookieManager();

    // Store a cookie (value is encrypted in memory).
    void setCookie(const std::string& name, const std::string& value);

    // Retrieve a cookie (decrypts on the fly, caller must clear the result).
    std::string getCookie(const std::string& name) const;

    // Remove a specific cookie.
    void removeCookie(const std::string& name);

    // Remove all cookies (secure wipe).
    void clearAll();

    // Build a Cookie: header value for the given domain.
    std::string buildCookieHeader() const;

    // Parse and store Set-Cookie header values from a response.
    // Accepts a newline-separated block of Set-Cookie lines.
    void parseAndStoreCookies(const std::string& setCookieBlock);

    // Returns true if the session cookie exists.
    bool hasSession() const;

    // Update the encryption key (re-encrypts all stored cookies).
    void rotateKey(const std::string& newKey);

private:
    mutable std::mutex mutex_;
    std::string encryptionKey_;

    // name → encrypted-hex-encoded value
    std::unordered_map<std::string, std::string> store_;

    std::string encryptValue(const std::string& value) const;
    std::string decryptValue(const std::string& hexCipher) const;
};

} // namespace sarrows
