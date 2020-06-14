/*
 * SessionAsyncDownloadFile.cpp
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

#include <session/SessionAsyncDownloadFile.hpp>

#include <shared_core/Error.hpp>
#include <core/json/JsonRpc.hpp>

#include <core/system/Process.hpp>

#include <session/SessionAsyncRProcess.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace {

class AsyncDownloadFile : public async_r::AsyncRProcess
{
public:
   AsyncDownloadFile(const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
      : onCompleted_(onCompleted)
   {
   }

   virtual void onStdout(const std::string& output)
   {
      output_ += output;
   }

   virtual void onStderr(const std::string& output)
   {
      error_ += output;
   }

   virtual void onCompleted(int exitStatus)
   {
      core::system::ProcessResult result;
      result.exitStatus = exitStatus;
      result.stdOut = output_;
      result.stdErr = error_;
      onCompleted_(result);
   }

   std::string output_;
   std::string error_;
   boost::function<void(const core::system::ProcessResult&)> onCompleted_;
};



bool readJson(const std::string& output, json::Value* pValue, json::JsonRpcResponse* pResponse)
{
   Error error = pValue->parse(output);
   if (error)
   {
      Error parseError(boost::system::errc::state_not_recoverable,
                       errorMessage(error),
                       ERROR_LOCATION);
      setErrorResponse(parseError, pResponse);
      return false;
   }
   else
   {
      return true;
   }
}


void endJsonRpcRequest(const json::JsonRpcFunctionContinuation& cont,
                       const JsonRpcResponseHandler& handler,
                       const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;
   if (result.exitStatus == EXIT_SUCCESS)
   {
      json::Value jsonValue;
      if (readJson(result.stdOut, &jsonValue, &response))
         handler(jsonValue, &response);
   }
   else
   {
      setProcessErrorResponse(result, ERROR_LOCATION, &response);
   }
   cont(Success(), &response);
}


} // anonymous namespace

void asyncDownloadFile(const std::string& url, const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   // build the command
   std::string cmd("{ ");
   cmd += "tmp <- tempfile(); ";
   cmd += "download.file('" + url +"', destfile = tmp, quiet = TRUE); ";
   cmd += "cat(readLines(tmp, warn = FALSE)); ";
   cmd += "} ";

   // kickoff the process
   boost::shared_ptr<AsyncDownloadFile> pDownload(new AsyncDownloadFile(onCompleted));
   pDownload->start(cmd.c_str(), FilePath(), async_r::R_PROCESS_NO_RDATA);
}


void asyncJsonRpcRequest(const std::string& url,
                         const JsonRpcResponseHandler& handler,
                         const core::json::JsonRpcFunctionContinuation& cont)
{
   asyncDownloadFile(url, boost::bind(endJsonRpcRequest, cont, handler, _1));
}

} // namespace session
} // namespace rstudio

