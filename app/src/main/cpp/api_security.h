#pragma once
#include <string>
#include <unordered_map>
#include <mutex>
#include "cookie_manager.h"

namespace sarrows {

static const std::string BASE_URL = "https://sarrows.vercel.app";

// Manages CSRF tokens, builds authenticated request headers,
// and enforces rate-limit awareness for stream endpoints.
class ApiSecurity {
public:
    explicit ApiSecurity(CookieManager* cookieManager);
    ~ApiSecurity();

    // ----- CSRF -----
    void storeCsrfToken(const std::string& token);
    std::string getCsrfToken() const;
    void clearCsrfToken();

    // ----- Header building -----
    // Returns a map of headers to inject into every authenticated request.
    std::unordered_map<std::string, std::string> buildAuthHeaders() const;

    // Build the form-encoded body for the login POST.
    std::string buildLoginBody(const std::string& email,
                               const std::string& password,
                               const std::string& csrfToken) const;

    // Build the form-encoded body for signout.
    std::string buildSignoutBody(const std::string& csrfToken) const;

    // ----- URL helpers -----
    std::string movieStreamUrl(const std::string& id) const;
    std::string movieEmbedUrl(const std::string& id) const;
    std::string episodeStreamUrl(const std::string& id) const;
    std::string episodeEmbedUrl(const std::string& id) const;

    // ----- Rate-limit tracking -----
    // Returns true if a stream-init call is allowed (30 per 60 s).
    bool checkStreamRateLimit();
    void recordStreamRequest();

    // ----- Obfuscated base URL -----
    std::string getBaseUrl() const;

private:
    CookieManager* cookieManager_; // non-owning
    mutable std::mutex mutex_;
    std::string csrfToken_;

    // Rate limit state
    long long streamWindowStart_; // epoch ms
    int streamCount_;

    static std::string urlEncode(const std::string& value);
};

} // namespace sarrows
