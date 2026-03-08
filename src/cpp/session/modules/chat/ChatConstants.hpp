/*
 * ChatConstants.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_CHAT_CONSTANTS_HPP
#define SESSION_CHAT_CONSTANTS_HPP

#include <chrono>
#include <string>
#include <vector>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace constants {

// ============================================================================
// Installation paths
// ============================================================================
extern const char* const kPositAiDirName;
extern const char* const kClientDirPath;
extern const char* const kServerScriptPath;
extern const char* const kIndexFileName;
extern const char* const kCspConfigPath;

// Sentinel value: no backend port is assigned
constexpr int kChatBackendPortNone = -1;

// ============================================================================
// Protocol Version (SUPPORTED_PROTOCOL_VERSION)
// ============================================================================
extern const char* const kProtocolVersion;

// ============================================================================
// Queue configuration
// ============================================================================
// Max pending notifications
constexpr size_t kMaxQueueSize = 10000;

// Buffer size chosen to balance latency vs notification volume
// 1KB ≈ 10-20 lines of typical R output, provides responsive feedback
// without excessive notification overhead
constexpr size_t kMaxBufferSize = 1024;

// Delay chosen to balance responsiveness vs overhead
// 100ms perceived as "instant" by users (< perceptual threshold)
// Reduces notification rate by ~10x for high-frequency output
constexpr std::chrono::milliseconds kMaxDelay{100};

// ============================================================================
// Capability negotiation
// ============================================================================

// Returns the set of JSON-RPC methods that RStudio can handle
// (i.e., requests/notifications that the peer may send to RStudio).
const std::vector<std::string>& rstudioCapabilities();

// ============================================================================
// Restart limits
// ============================================================================
constexpr int kMaxRestartAttempts = 1;

} // namespace constants
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_CONSTANTS_HPP
