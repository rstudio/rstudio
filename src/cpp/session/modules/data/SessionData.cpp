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
 
#include <r/RExec.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RJson.hpp>
#include <r/RSourceManager.hpp>
#include <r/session/RSessionUtils.hpp>

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
           const json::JsonRpcRequest& request,
           const json::JsonRpcFunctionContinuation& continuation)
   {
      boost::shared_ptr<AsyncDataPreviewRProcess> pDataPreview(
                  new AsyncDataPreviewRProcess(request, continuation));
      pDataPreview->start();
      return pDataPreview;
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

   Error saveRDS(const json::JsonRpcRequest& request)
   {
      r::sexp::Protect rProtect;
      r::exec::RFunction rFunction("saveRDS");

      rFunction.addParam(request.params);
      rFunction.addParam("file", inputLocation_);

      SEXP resultSEXP;
      return rFunction.call(&resultSEXP, &rProtect);
   }

   void start()
   {
      inputLocation_ = module_context::tempFile("input", "rds").absolutePath();
      outputLocation_ = module_context::tempFile("output", "rds").absolutePath();

      Error err = saveRDS(request_);
      if (err)
      {
         continuationWithError("Failed to prepare the operation.");
         return;
      }

      std::string cmd = std::string(".rs.callWithRDS(") +
        "\".rs.rpc.preview_data_import\", \"" +
        inputLocation_ +
        "\", \"" +
        outputLocation_ +
        "\")";

      std::vector<core::FilePath> sources;
      sources.push_back(pathFromSource("Tools.R"));
      sources.push_back(pathFromModulesSource("ModuleTools.R"));
      sources.push_back(pathFromModulesSource("SessionDataViewer.R"));
      sources.push_back(pathFromModulesSource("SessionDataImportV2.R"));

      async_r::AsyncRProcess::start(cmd.c_str(), FilePath(), async_r::R_PROCESS_VANILLA, sources);
   }

   Error readRDS(SEXP* pResult)
   {
       r::sexp::Protect rProtect;
       r::exec::RFunction rFunction("readRDS");

       rFunction.addParam(outputLocation_);
       rFunction.call(pResult, &rProtect);

       return Success();
   }

   void continuationWithError(const char* message)
   {
      json::Object jsonError;

      jsonError["message"] = message;
      if (errors_.size() > 0)
      {
         jsonError["message"] = json::toJsonArray(errors_);
      }

      json::Object jsonErrorResponse;
      jsonErrorResponse["error"] = jsonError;

      json::JsonRpcResponse response;
      response.setResult(jsonErrorResponse);

      continuation_(Success(), &response);
   }

   void onCompleted(int exitStatus)
   {
      if (terminationRequested())
      {
         json::JsonRpcResponse response;
         continuation_(Success(), &response);
         return;
      }

      if (exitStatus != EXIT_SUCCESS)
      {
         continuationWithError("Operation finished with error code.");
         return;
      }

      SEXP resultSEXP;;
      Error error = readRDS(&resultSEXP);
      if (error)
      {
         continuationWithError("Failed to complete the operation.");
         return;
      }
      else
      {
         core::json::Value resultValue;
         error = r::json::jsonValueFromObject(resultSEXP, &resultValue);
         if (error)
         {
            continuationWithError("Failed to parse result from the operation execution.");
            return;
         }

         json::JsonRpcResponse response;
         response.setResult(resultValue);

         continuation_(Success(), &response);
      }
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

   std::string inputLocation_;
   std::string outputLocation_;
};

boost::shared_ptr<AsyncDataPreviewRProcess> s_pActiveDataPreview;

bool getPreviewDataImportAsync(
        const json::JsonRpcRequest& request,
        const json::JsonRpcFunctionContinuation& continuation)
{
   if (s_pActiveDataPreview &&
       s_pActiveDataPreview->isRunning())
   {
      return true;
   }
   else
   {
      s_pActiveDataPreview = AsyncDataPreviewRProcess::create(request, continuation);
      return false;
   }
}

Error abortPreviewDataImportAsync(const json::JsonRpcRequest& request,
                                 json::JsonRpcResponse* pResponse)
{
   if (s_pActiveDataPreview &&
       s_pActiveDataPreview->isRunning())
   {
      s_pActiveDataPreview->terminate();
   }

   return Success();
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
      (bind(registerAsyncRpcMethod, "preview_data_import_async", getPreviewDataImportAsync))
      (bind(registerRpcMethod, "preview_data_import_async_abort", abortPreviewDataImportAsync));

   return initBlock.execute();
}

} // namespace data
} // namespace modules
} // namespace session
} // namespace rstudio

