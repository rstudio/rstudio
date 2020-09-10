/*
 * SessionAsyncDownloadFile.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef SESSION_ASYNC_DOWNLOAD_FILE_HPP
#define SESSION_ASYNC_DOWNLOAD_FILE_HPP

#include <string>

#include <core/http/Util.hpp>
#include <core/json/JsonRpc.hpp>

namespace rstudio {
namespace core {
   class Error;
   namespace system {
      struct ProcessResult;
   }
}
namespace session {

// use R download.file in an async subprocess (contents of file are in result.stdOut on success)

void asyncDownloadFile(const std::string& url,
                       const boost::function<void(const core::system::ProcessResult&)>& onCompleted);

void asyncDownloadFile(const std::string& url,
                       const core::http::Fields& headers,
                       const boost::function<void(const core::system::ProcessResult&)>& onCompleted);

void asyncDownloadFile(const std::string& url,
                       const std::string& userAgent,
                       const core::http::Fields& headers,
                       const boost::function<void(const core::system::ProcessResult&)>& onCompleted);

// wrapper for asyncDownloadFile that parses it's payload as JSON and satisfies a JsonRpcFunctionContinuation
// (including checking for and reporting errors on the continuation)

typedef boost::function<void(const core::json::Value&, core::json::JsonRpcResponse*)> JsonRpcResponseHandler;

void asyncJsonRpcRequest(const std::string& url,
                         const JsonRpcResponseHandler& handler,
                         const core::json::JsonRpcFunctionContinuation& cont);

void asyncJsonRpcRequest(const std::string& url,
                         const std::string& userAgent,
                         const core::http::Fields& headers,
                         const JsonRpcResponseHandler& handler,
                         const core::json::JsonRpcFunctionContinuation& cont);

bool is404Error(const std::string& stdErr);
bool isHostError(const std::string& stdErr);


} // namespace session
} // namespace rstudio

#endif // SESSION_ASYNC_DOWNLOAD_FILE_HPP

