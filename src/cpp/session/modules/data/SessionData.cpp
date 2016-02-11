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
#include <r/session/RSessionUtils.hpp>

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
           const json::JsonRpcRequest& request,
           const json::JsonRpcFunctionContinuation& continuation)
   {
      boost::shared_ptr<AsyncDataPreviewRProcess> pDataPreview(
                  new AsyncDataPreviewRProcess(request, continuation));
      pDataPreview->start();
      return pDataPreview;
   }

   void terminateProcess()
   {
      async_r::AsyncRProcess::terminate();
   }

private:
   AsyncDataPreviewRProcess(
           const json::JsonRpcRequest& request,
           const json::JsonRpcFunctionContinuation& continuation) :
       continuation_(continuation),
       request_(request)
   {
   }

   FilePath pathFromModulesSource(std::string sourceFile)
   {
      FilePath modulesPath = session::options().modulesRSourcePath();
      FilePath srcPath = modulesPath.complete(sourceFile);

      return srcPath;
   }

   FilePath pathFromSource(std::string sourceFile)
   {
      FilePath sourcesPath = session::options().coreRSourcePath();
      FilePath srcPath = sourcesPath.complete(sourceFile);

      return srcPath;
   }

   void start()
   {
      std::ostringstream jsonStream;
      json::Value requestValue = request_.params;
      json::write(requestValue, jsonStream);
      std::string jsonData = jsonStream.str();

      std::string cmd = ".rs.preview_data_import_async('" + jsonData + "')";

      std::vector<core::FilePath> sources;
      sources.push_back(pathFromSource("Tools.R"));
      sources.push_back(pathFromModulesSource("ModuleTools.R"));
      sources.push_back(pathFromModulesSource("SessionDataViewer.R"));
      sources.push_back(pathFromModulesSource("SessionDataImportV2.R"));

      async_r::AsyncRProcess::start(cmd.c_str(), FilePath(), async_r::R_PROCESS_VANILLA, sources);
   }

   void onCompleted(int exitStatus)
   {
      if (errors_.size() > 0)
      {
         json::Object jsonError;
         jsonError["message"] = json::toJsonArray(errors_);

         json::Object jsonErrorResponse;
         jsonErrorResponse["error"] = jsonError;

         json::JsonRpcResponse response;
         response.setResult(jsonErrorResponse);

         continuation_(Success(), &response);

         return;
      }

      json::Value jsonResponse;
      json::parse(output_, &jsonResponse);

      json::JsonRpcResponse response;
      response.setResult(jsonResponse);

      continuation_(Success(), &response);
   }

   void onStdout(const std::string& output)
   {
      output_ += output;
   }

   void onStderr(const std::string& output)
   {
      errors_.push_back(output);
   }

   const json::JsonRpcFunctionContinuation continuation_;
   json::JsonRpcRequest request_;
   std::vector<std::string> errors_;
   std::string output_;
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
      s_pActiveDataPreview = AsyncDataPreviewRProcess::create(request, continuation);
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

