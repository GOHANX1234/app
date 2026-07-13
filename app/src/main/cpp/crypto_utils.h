#pragma once
#include <string>
#include <vector>
#include <cstdint>

namespace sarrows {

// Simple XOR-based obfuscation for in-memory storage.
// The key is derived from a device-specific seed passed in from Kotlin / Android Keystore.
class CryptoUtils {
public:
    // Encrypt plaintext using XOR-key stream derived from key.
    static std::vector<uint8_t> encrypt(const std::string& plaintext, const std::string& key);

    // Decrypt previously encrypted bytes.
    static std::string decrypt(const std::vector<uint8_t>& ciphertext, const std::string& key);

    // Encode bytes as hex string.
    static std::string toHex(const std::vector<uint8_t>& data);

    // Decode hex string to bytes.
    static std::vector<uint8_t> fromHex(const std::string& hex);

    // Derive a stretched key via a simple PBKDF (rounds of SHA-like mixing).
    static std::string deriveKey(const std::string& seed, const std::string& salt, int rounds = 1000);

    // Constant-time compare (prevent timing attacks).
    static bool secureCompare(const std::string& a, const std::string& b);

    // Zero out sensitive memory.
    static void secureClear(std::string& s);
    static void secureClear(std::vector<uint8_t>& v);
};

} // namespace sarrows
