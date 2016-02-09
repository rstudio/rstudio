/*
 * SessionData.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "SessionData.hpp"

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>

#include "DataViewer.hpp"

using namespace rstudio::core ;

namespace rstudio {
namespace session {
namespace modules {
namespace data {

class AsyncDataPreviewRProcess : public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<AsyncDataPreviewRProcess> create(
           const json::JsonRpcFunctionContinuation& continuation)
   {
      boost::shared_ptr<AsyncDataPreviewRProcess> pDataPreview(
                  new AsyncDataPreviewRProcess(continuation));
      pDataPreview->start();
      return pDataPreview;
   }

   void terminateProcess()
   {
      async_r::AsyncRProcess::terminate();
   }

private:
   AsyncDataPreviewRProcess(const json::JsonRpcFunctionContinuation& continuation) :
       continuation_(continuation)
   {
   }

   void start()
   {
       core::system::Options environment;

       std::string cmd = ""
               "asyncFile <- \"/tmp/async.RData\" \n"
               "asyncData <- list(4, 8, 15, 16, 23, 42) \n"
               "save(asyncData, file = asyncFile)";

       async_r::AsyncRProcess::start(cmd.c_str(), FilePath(), async_r::R_PROCESS_VANILLA);
   }

   void onCompleted(int exitStatus)
   {
      json::Object jsonResponse;
      jsonResponse["success"] = "true";

      json::JsonRpcResponse response;
      response.setResult(jsonResponse);
      continuation_(Success(), &response);
   }

   const json::JsonRpcFunctionContinuation continuation_;
};

boost::shared_ptr<AsyncDataPreviewRProcess> s_pActiveDataPreview;

void getPreviewDataImportAsync(
        const json::JsonRpcRequest& request,
        const json::JsonRpcFunctionContinuation& continuation)
{
    if (s_pActiveDataPreview &&
        s_pActiveDataPreview->isRunning())
    {
    }
    else
    {
       s_pActiveDataPreview = AsyncDataPreviewRProcess::create(continuation);
    }
}

Error initialize()
{
   using boost::bind;
   using namespace session::module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (data::viewer::initialize)
      (bind(sourceModuleRFile, "SessionDataImport.R"))
      (bind(sourceModuleRFile, "SessionDataImportV2.R"))
      (bind(registerAsyncRpcMethod, "preview_data_import_async", getPreviewDataImportAsync));

   return initBlock.execute();
}

} // namespace data
} // namespace modules
} // namesapce session
} // namespace rstudio

