#include "api_security.h"
#include "crypto_utils.h"
#include <android/log.h>
#include <sstream>
#include <chrono>
#include <iomanip>

#define LOG_TAG "SarrowsSecurity"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace sarrows {

ApiSecurity::ApiSecurity(CookieManager* cookieManager)
    : cookieManager_(cookieManager),
      streamWindowStart_(0),
      streamCount_(0) {}

ApiSecurity::~ApiSecurity() {
    clearCsrfToken();
}

void ApiSecurity::storeCsrfToken(const std::string& token) {
    std::lock_guard<std::mutex> lock(mutex_);
    csrfToken_ = token;
}

std::string ApiSecurity::getCsrfToken() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return csrfToken_;
}

void ApiSecurity::clearCsrfToken() {
    std::lock_guard<std::mutex> lock(mutex_);
    CryptoUtils::secureClear(csrfToken_);
}

std::unordered_map<std::string, std::string> ApiSecurity::buildAuthHeaders() const {
    std::unordered_map<std::string, std::string> headers;
    headers["Accept"]       = "application/json";
    headers["Content-Type"] = "application/json";
    headers["Origin"]       = BASE_URL;
    headers["Referer"]      = BASE_URL + "/";
    headers["User-Agent"]   = "SarrowsAndroid/1.0";

    auto cookie = cookieManager_->buildCookieHeader();
    if (!cookie.empty()) {
        headers["Cookie"] = cookie;
    }
    return headers;
}

// URL-encode a value (application/x-www-form-urlencoded)
std::string ApiSecurity::urlEncode(const std::string& value) {
    std::ostringstream oss;
    for (unsigned char c : value) {
        if (std::isalnum(c) || c == '-' || c == '_' || c == '.' || c == '~') {
            oss << c;
        } else {
            oss << '%' << std::uppercase << std::hex << std::setw(2) << std::setfill('0')
                << static_cast<int>(c);
        }
    }
    return oss.str();
}

std::string ApiSecurity::buildLoginBody(const std::string& email,
                                        const std::string& password,
                                        const std::string& csrfToken) const {
    return "email=" + urlEncode(email) +
           "&password=" + urlEncode(password) +
           "&csrfToken=" + urlEncode(csrfToken) +
           "&redirect=false" +
           "&callbackUrl=" + urlEncode(BASE_URL + "/");
}

std::string ApiSecurity::buildSignoutBody(const std::string& csrfToken) const {
    return "csrfToken=" + urlEncode(csrfToken) +
           "&redirect=false" +
           "&callbackUrl=" + urlEncode(BASE_URL + "/");
}

std::string ApiSecurity::movieStreamUrl(const std::string& id) const {
    return BASE_URL + "/api/stream/movie/" + id;
}

std::string ApiSecurity::movieEmbedUrl(const std::string& id) const {
    return BASE_URL + "/api/stream/movie/" + id + "/embed";
}

std::string ApiSecurity::episodeStreamUrl(const std::string& id) const {
    return BASE_URL + "/api/stream/episode/" + id;
}

std::string ApiSecurity::episodeEmbedUrl(const std::string& id) const {
    return BASE_URL + "/api/stream/episode/" + id + "/embed";
}

bool ApiSecurity::checkStreamRateLimit() {
    std::lock_guard<std::mutex> lock(mutex_);
    auto now = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    if (now - streamWindowStart_ > 60000) {
        streamWindowStart_ = now;
        streamCount_ = 0;
    }
    return streamCount_ < 30;
}

void ApiSecurity::recordStreamRequest() {
    std::lock_guard<std::mutex> lock(mutex_);
    streamCount_++;
}

std::string ApiSecurity::getBaseUrl() const {
    return BASE_URL;
}

} // namespace sarrows
