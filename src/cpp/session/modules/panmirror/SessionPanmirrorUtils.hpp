/*
 * SessionPanmirrorUtils.hpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
#ifndef SESSION_MODULES_PANMIRROR_UTILS_HPP
#define SESSION_MODULES_PANMIRROR_UTILS_HPP

#include <string>
#include <core/json/JsonRpc.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {

extern const char * const kStatusOK;
extern const char * const kStatusNotFound;
extern const char * const kStatusNoHost;
extern const char * const kStatusError;

void resolveJsonRpcContinuation(const core::json::JsonRpcFunctionContinuation& cont,
                       const std::string& status,
                       const core::json::Value& messageJson = core::json::Value(),
                       const std::string& error = "");

core::Error handleJsonRpcProcessResult(const core::system::ProcessResult& result,
                                core::json::Value* pValue,
                                const core::ErrorLocation& location);


typedef boost::function<void(const core::json::JsonRpcFunctionContinuation&, core::json::Value)> JsonHandler;

inline void jsonPassthrough(const core::json::JsonRpcFunctionContinuation& cont,
                            const core::json::Value& value) {
   resolveJsonRpcContinuation(cont, kStatusOK, value);
}

void jsonRpcDownloadHandler(const core::json::JsonRpcFunctionContinuation& cont,
                            const core::system::ProcessResult& result,
                            const core::ErrorLocation& location,
                            const JsonHandler& jsonHandler);

} // namespace panmirror
} // namespace modules
} // namespace session
} // namespace rstudio

#endif /* SESSION_MODULES_PANMIRROR_UTILS_HPP */
