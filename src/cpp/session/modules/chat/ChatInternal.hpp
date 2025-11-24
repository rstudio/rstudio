/*
 * ChatInternal.hpp
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

#ifndef RSTUDIO_SESSION_MODULES_CHAT_INTERNAL_HPP
#define RSTUDIO_SESSION_MODULES_CHAT_INTERNAL_HPP

#include <string>
#include <set>

#include <boost/thread/mutex.hpp>

#include <core/system/System.hpp>

#include <fmt/format.h>
#include <core/Log.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {

// ============================================================================
// Installation paths
// ============================================================================
extern const char* const kPositAiDirName;
extern const char* const kClientDirPath;
extern const char* const kServerScriptPath;
extern const char* const kIndexFileName;

// ============================================================================
// Protocol Version
// ============================================================================
extern const char* const kProtocolVersion;

// ============================================================================
// Process management
// ============================================================================
extern PidType s_chatBackendPid;
extern int s_chatBackendPort;
extern std::string s_chatBackendUrl;
extern int s_chatBackendRestartCount;
extern const int kMaxRestartAttempts;

// ============================================================================
// Suspension blocking
// ============================================================================
extern bool s_chatBusy;

// ============================================================================
// Logging
// ============================================================================
extern int s_chatLogLevel;
extern std::string s_chatBackendMinLogLevel;

int chatLogLevel();

// Map log level names to numeric priorities for filtering
int getLogLevelPriority(const std::string& level);

// ============================================================================
// Execution Tracking (for cancellation support)
// ============================================================================
extern boost::mutex s_executionTrackingMutex;
extern std::string s_currentTrackingId;
extern std::set<std::string> s_cancelledTrackingIds;

// ============================================================================
// Logging Macros
// ============================================================================
#define CHAT_LOG_IMPL(__LOGGER__, __FMT__, ...)                             \
   do                                                                       \
   {                                                                        \
      std::string __message__ = fmt::format(__FMT__, ##__VA_ARGS__);        \
      std::string __formatted__ =                                           \
          fmt::format("[{}]: {}", __func__, __message__);                   \
      __LOGGER__("chat", __formatted__);                                    \
      if (chatLogLevel() >= 1)                                              \
         std::cerr << __formatted__ << std::endl;                           \
   } while (0)

#define DLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_DEBUG_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define WLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_WARNING_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)
#define ELOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_ERROR_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define ILOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_INFO_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define TLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_TRACE_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)

} // end namespace chat
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_CHAT_INTERNAL_HPP */
