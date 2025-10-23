/*
 * SessionChat.cpp
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

#include "SessionChat.hpp"

#include <core/Exec.hpp>

#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include "session-config.h"

#define CHAT_LOG_IMPL(__LOGGER__, __FMT__, ...)                             \
   do                                                                       \
   {                                                                        \
      std::string __message__ = fmt::format(__FMT__, ##__VA_ARGS__);        \
      std::string __formatted__ =                                           \
          fmt::format("[{}]: {}", __func__, __message__);                   \
      __LOGGER__("copilot", __formatted__);                                 \
      if (chatLogLevel() >= 1)                                           \
         std::cerr << __formatted__ << std::endl;                           \
   } while (0)

#define DLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_DEBUG_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define WLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_WARNING_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)
#define ELOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_ERROR_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)

// Use a default section of 'chat' for errors / warnings
#ifdef LOG_ERROR
# undef LOG_ERROR
# define LOG_ERROR(error) LOG_ERROR_NAMED("chat", error)
#endif

using namespace rstudio::core;
using namespace rstudio::core::system;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {

namespace {

// The log level for Chat-specific logs. Primarily for developer use.
int s_chatLogLevel = 0;

int chatLogLevel()
{
   return s_chatLogLevel;
}

SEXP rs_chatSetLogLevel(SEXP logLevelSEXP)
{
   int logLevel = r::sexp::asInteger(logLevelSEXP);
   s_chatLogLevel = logLevel;
   return logLevelSEXP;
}

Error chatVerifyInstalled(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   json::Object responseJson;
   pResponse->setResult(false); // TODO: Implement actual check for chat feature installation
   // sleep for 5 seconds
   boost::this_thread::sleep(boost::posix_time::seconds(5));
   return Success();
}

} // end anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // Read default log level
   std::string chatLogLevel = core::system::getenv("CHAT_LOG_LEVEL");
   if (!chatLogLevel.empty())
      s_chatLogLevel = safe_convert::stringTo<int>(chatLogLevel, 0);

   RS_REGISTER_CALL_METHOD(rs_chatSetLogLevel);

   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(registerRpcMethod, "chat_verify_installed", chatVerifyInstalled))
         (bind(sourceModuleRFile, "SessionChat.R"))
         ;
   return initBlock.execute();
}

} // end namespace chat
} // end namespace modules
} // end namespace session
} // end namespace rstudio
