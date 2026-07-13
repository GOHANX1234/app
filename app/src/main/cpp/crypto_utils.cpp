#include "crypto_utils.h"
#include <android/log.h>
#include <algorithm>
#include <cstring>
#include <sstream>
#include <iomanip>

#define LOG_TAG "SarrowsCrypto"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace sarrows {

// -------------------------------------------------------------------
// Key-stream generation: stretch the key into a keystream of arbitrary length.
// Uses a simple Fibonacci-like mixing so adjacent bytes differ.
// -------------------------------------------------------------------
static std::vector<uint8_t> generateKeyStream(const std::string& key, size_t length) {
    std::vector<uint8_t> stream;
    stream.reserve(length);
    if (key.empty()) return stream;

    uint8_t prev = 0xA5;
    size_t ki = 0;
    while (stream.size() < length) {
        uint8_t k = static_cast<uint8_t>(key[ki % key.size()]);
        uint8_t v = k ^ prev ^ static_cast<uint8_t>(ki * 0x1B);
        stream.push_back(v);
        prev = v;
        ki++;
    }
    return stream;
}

std::vector<uint8_t> CryptoUtils::encrypt(const std::string& plaintext, const std::string& key) {
    std::vector<uint8_t> result(plaintext.size());
    auto ks = generateKeyStream(key, plaintext.size());
    for (size_t i = 0; i < plaintext.size(); i++) {
        result[i] = static_cast<uint8_t>(plaintext[i]) ^ ks[i];
    }
    return result;
}

std::string CryptoUtils::decrypt(const std::vector<uint8_t>& ciphertext, const std::string& key) {
    std::string result(ciphertext.size(), '\0');
    auto ks = generateKeyStream(key, ciphertext.size());
    for (size_t i = 0; i < ciphertext.size(); i++) {
        result[i] = static_cast<char>(ciphertext[i] ^ ks[i]);
    }
    return result;
}

std::string CryptoUtils::toHex(const std::vector<uint8_t>& data) {
    std::ostringstream oss;
    for (auto b : data) {
        oss << std::hex << std::setw(2) << std::setfill('0') << static_cast<int>(b);
    }
    return oss.str();
}

std::vector<uint8_t> CryptoUtils::fromHex(const std::string& hex) {
    std::vector<uint8_t> out;
    out.reserve(hex.size() / 2);
    for (size_t i = 0; i + 1 < hex.size(); i += 2) {
        uint8_t b = static_cast<uint8_t>(std::stoi(hex.substr(i, 2), nullptr, 16));
        out.push_back(b);
    }
    return out;
}

std::string CryptoUtils::deriveKey(const std::string& seed, const std::string& salt, int rounds) {
    // Simple iterative mixing: XOR seed and salt bytes repeatedly.
    std::string state = seed + ":" + salt;
    for (int r = 0; r < rounds; r++) {
        for (size_t i = 0; i < state.size(); i++) {
            state[i] ^= static_cast<char>((r + i + 0x5A) & 0xFF);
        }
        // Rotate bytes
        char first = state[0];
        for (size_t i = 0; i < state.size() - 1; i++) state[i] = state[i + 1];
        state[state.size() - 1] = first;
    }
    return state;
}

bool CryptoUtils::secureCompare(const std::string& a, const std::string& b) {
    if (a.size() != b.size()) return false;
    uint8_t diff = 0;
    for (size_t i = 0; i < a.size(); i++) {
        diff |= static_cast<uint8_t>(a[i]) ^ static_cast<uint8_t>(b[i]);
    }
    return diff == 0;
}

void CryptoUtils::secureClear(std::string& s) {
    std::fill(s.begin(), s.end(), '\0');
    s.clear();
}

void CryptoUtils::secureClear(std::vector<uint8_t>& v) {
    std::fill(v.begin(), v.end(), 0);
    v.clear();
}

} // namespace sarrows
