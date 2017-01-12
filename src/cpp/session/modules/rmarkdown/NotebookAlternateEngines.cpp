/*
 * NotebookAlternateEngines.cpp
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


#include "SessionRmdNotebook.hpp"
#include "SessionExecuteChunkOperation.hpp"
#include "NotebookCache.hpp"
#include "NotebookAlternateEngines.hpp"
#include "NotebookWorkingDir.hpp"

#include <boost/algorithm/string.hpp>

#include <core/StringUtils.hpp>
#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

class ChunkExecCompletedScope : boost::noncopyable
{
public:
   ChunkExecCompletedScope(const std::string& docId,
                           const std::string& chunkId)
      : docId_(docId), chunkId_(chunkId)
   {
   }
   
   ~ChunkExecCompletedScope()
   {
      events().onChunkExecCompleted(
               docId_,
               chunkId_,
               notebookCtxId());
   }
   
private:
   std::string docId_;
   std::string chunkId_;
};

Error prepareCacheConsoleOutputFile(const std::string& docId,
                                    const std::string& chunkId,
                                    const std::string& nbCtxId,
                                    FilePath* pChunkOutputFile)
{
   // forward declare error
   Error error;
   
   // prepare chunk directory
   FilePath cachePath = notebook::chunkOutputPath(
            docId,
            chunkId,
            notebook::ContextExact);
   
   error = cachePath.resetDirectory();
   if (error)
      return error;
   
   // prepare cache console output file
   *pChunkOutputFile =
         notebook::chunkOutputFile(docId, chunkId, nbCtxId, ChunkOutputText);
   
   return Success();
}

void chunkConsoleOutputHandler(module_context::ConsoleOutputType type,
                               const std::string& output,
                               const FilePath& targetPath)
{
   using namespace module_context;
   
   Error error = appendConsoleOutput(
            type == ConsoleOutputNormal ? kChunkConsoleOutput : kChunkConsoleError,
            output,
            targetPath);
   if (error)
      LOG_ERROR(error);
}

void reportChunkExecutionError(const std::string& docId,
                               const std::string& chunkId,
                               const std::string& nbCtxId,
                               const std::string& message,
                               const FilePath& targetPath)
{
   // emit chunk error
   chunkConsoleOutputHandler(
            module_context::ConsoleOutputError,
            message,
            targetPath);
   
   // forward failure to chunk
   enqueueChunkOutput(
            docId,
            chunkId,
            nbCtxId,
            0, // no ordinal needed
            ChunkOutputText,
            targetPath);
}

Error executeRcppEngineChunk(const std::string& docId,
                             const std::string& chunkId,
                             const std::string& nbCtxId,
                             const std::string& code,
                             const std::map<std::string, std::string>& options)
{
   // forward declare error
   Error error;
   
   // always ensure we emit a 'execution complete' event on exit
   ChunkExecCompletedScope execScope(docId, chunkId);
   
   // prepare cache output file (use tempfile on failure)
   FilePath targetPath = module_context::tempFile("rcpp-cache", "");
   error = prepareCacheConsoleOutputFile(docId, chunkId, nbCtxId, &targetPath);
   if (error)
      LOG_ERROR(error);
   
   // capture console output, error
   boost::signals::scoped_connection consoleHandler =
         module_context::events().onConsoleOutput.connect(
            boost::bind(chunkConsoleOutputHandler,
                        _1,
                        _2,
                        targetPath));

   // call Rcpp::sourceCpp on code
   std::string escaped = boost::regex_replace(
            code,
            boost::regex("(\\\\|\")"),
            "\\\\$1");
   
   std::string execCode =
         "Rcpp::sourceCpp(code = \"" + escaped + "\")";
   
   // write input code to cache
   error = appendConsoleOutput(
            kChunkConsoleInput,
            code,
            targetPath);
   if (error)
      LOG_ERROR(error);
   
   // execute code (output captured on success; on failure we
   // explicitly forward the error message returned)
   error = r::exec::executeString(execCode);
   if (error)
   {
      chunkConsoleOutputHandler(module_context::ConsoleOutputError,
                                r::endUserErrorMessage(error),
                                targetPath);
   }

   // forward success / failure to chunk
   enqueueChunkOutput(
            docId,
            chunkId,
            nbCtxId,
            0, // no ordinal needed
            ChunkOutputText,
            targetPath);
   
   return error;
}

void reportStanExecutionError(const std::string& docId,
                              const std::string& chunkId,
                              const std::string& nbCtxId,
                              const FilePath& targetPath)
{
   std::string message =
         "engine.opts$output.var must be a character string providing a "
         "name for the returned `stanmodel` object";
   reportChunkExecutionError(docId, chunkId, nbCtxId, message, targetPath);
}


Error executeStanEngineChunk(const std::string& docId,
                             const std::string& chunkId,
                             const std::string& nbCtxId,
                             const std::string& code,
                             const std::map<std::string, std::string>& options)
{
   // forward-declare error
   Error error;
   
   // ensure we always emit an execution complete event on exit
   ChunkExecCompletedScope execScope(docId, chunkId);
   
   // prepare console output file -- use tempfile on failure
   FilePath targetPath = module_context::tempFile("stan-cache-", "");
   error = prepareCacheConsoleOutputFile(docId, chunkId, nbCtxId, &targetPath);
   if (error)
      LOG_ERROR(error);
   
   // capture console output, error
   boost::signals::scoped_connection consoleHandler =
         module_context::events().onConsoleOutput.connect(
            boost::bind(chunkConsoleOutputHandler,
                        _1,
                        _2,
                        targetPath)); 
   
   // write code to file
   FilePath tempFile = module_context::tempFile("stan-", ".stan");
   error = writeStringToFile(tempFile, code + "\n");
   if (error)
   {
      reportChunkExecutionError(
               docId,
               chunkId,
               nbCtxId,
               r::endUserErrorMessage(error),
               targetPath);
      LOG_ERROR(error);
      return Success();
   }
   RemoveOnExitScope removeOnExitScope(tempFile, ERROR_LOCATION);
   
   // evaluate engine options (so we can pass them through to stan call)
   r::sexp::Protect protect;
   SEXP engineOptsSEXP = R_NilValue;
   if (options.count("engine.opts"))
   {
      error = r::exec::evaluateString(
               options.at("engine.opts"),
               &engineOptsSEXP,
               &protect);

      if (error)
      {
         reportStanExecutionError(docId, chunkId, nbCtxId, targetPath);
         return Success();
      }
   }
   else
   {
      // if no engine.opts available, just use a plain empty list
      engineOptsSEXP = r::sexp::createList(std::vector<std::string>(), &protect);
   }
   
   // construct call to 'stan_model'
   r::exec::RFunction fStanEngine("rstan:::stan_model");
   std::vector<std::string> engineOptsNames;
   error = r::sexp::getNames(engineOptsSEXP, &engineOptsNames);
   if (error)
   {
      reportStanExecutionError(docId, chunkId, nbCtxId, targetPath);
      return Success();
   }
   
   // build parameters
   std::string modelName;
   for (std::size_t i = 0, n = r::sexp::length(engineOptsSEXP); i < n; ++i)
   {
      // skip 'output.var' engine option (this is the variable we wish to assign to
      // after evaluating the stan model)
      if (engineOptsNames[i] == "output.var" || engineOptsNames[i] == "x")
      {
         modelName = r::sexp::asString(VECTOR_ELT(engineOptsSEXP, i));
         continue;
      }
      
      fStanEngine.addParam(
               engineOptsNames[i],
               VECTOR_ELT(engineOptsSEXP, i));
   }
   
   // if 'output.var' was provided as part of the chunk parameters
   // (not as part of 'engine.opts') then use that here
   if (options.count("output.var"))
      modelName = options.at("output.var");
   
   // if no model name was set, return error message
   if (modelName.empty())
   {
      reportStanExecutionError(docId, chunkId, nbCtxId, targetPath);
      return Success();
   }
   
   // if the 'file' option was not set, set it explicitly
   if (!core::algorithm::contains(engineOptsNames, "file"))
      fStanEngine.addParam("file", string_utils::utf8ToSystem(tempFile.absolutePath()));
   
   // evaluate stan_model call
   SEXP stanModelSEXP = R_NilValue;
   error = fStanEngine.call(&stanModelSEXP, &protect);
   if (error)
   {
      std::string msg = r::endUserErrorMessage(error);
      reportChunkExecutionError(docId, chunkId, nbCtxId, msg, targetPath);
      return Success();
   }
   
   // assign in global env on success
   if (stanModelSEXP != R_NilValue)
   {
      r::exec::RFunction assign("base:::assign");
      assign.addParam("x", modelName);
      assign.addParam("value", stanModelSEXP);
      error = assign.call();
      if (error)
      {
         std::string msg = r::endUserErrorMessage(error);
         reportChunkExecutionError(docId, chunkId, nbCtxId, msg, targetPath);
         LOG_ERROR(error);
         return Success();
      }
   }
   
   // forward success / failure to chunk
   enqueueChunkOutput(
            docId,
            chunkId,
            notebookCtxId(),
            0, // no ordinal needed
            ChunkOutputText,
            targetPath);
   
   return Success();
}

Error executeSqlEngineChunk(const std::string& docId,
                             const std::string& chunkId,
                             const std::string& nbCtxId,
                             const std::string& code,
                             const json::Object& options)
{
   Error error;
   
   // ensure we always emit an execution complete event on exit
   ChunkExecCompletedScope execScope(docId, chunkId);

   // prepare console output file -- use tempfile on failure
   FilePath consolePath = module_context::tempFile("data-console-", "");
   error = prepareCacheConsoleOutputFile(docId, chunkId, nbCtxId, &consolePath);
   if (error)
      LOG_ERROR(error);
   
   // capture console output, error
   boost::signals::scoped_connection consoleHandler =
         module_context::events().onConsoleOutput.connect(
            boost::bind(chunkConsoleOutputHandler,
                        _1,
                        _2,
                        consolePath)); 

   FilePath parentPath = notebook::chunkOutputPath(
       docId, chunkId, nbCtxId, ContextSaved);
   error = parentPath.ensureDirectory();
   if (error)
   {
      std::string message = "Failed to create SQL chunk directory";
      reportChunkExecutionError(docId, chunkId, nbCtxId, message, parentPath);
        
      return Success();
   }
    
   FilePath dataPath =
         notebook::chunkOutputFile(docId, chunkId, nbCtxId, ChunkOutputData);

   // check package dependencies
   if (!module_context::isPackageVersionInstalled("DBI", "0.4"))
   {
      std::string message = "Executing SQL chunks requires version 0.4 or "
                            "later of the DBI package";
      reportChunkExecutionError(docId, chunkId, nbCtxId, message, consolePath);
      return Success();
   }

   // run sql and save result
   error = r::exec::RFunction(
               ".rs.runSqlForDataCapture",
               code,
               string_utils::utf8ToSystem(dataPath.absolutePath()),
               options).call();
   if (error)
   {
      std::string message = "Failed to execute SQL chunk";
      reportChunkExecutionError(docId, chunkId, nbCtxId, message, consolePath);

      return Success();
   }
   
   // write input code to cache
   error = appendConsoleOutput(
            kChunkConsoleInput,
            code,
            consolePath);
   if (error)
      LOG_ERROR(error);

   if (dataPath.exists()) {
      // forward success / failure to chunk
      enqueueChunkOutput(
               docId,
               chunkId,
               notebookCtxId(),
               0, // no ordinal needed
               ChunkOutputData,
               dataPath);
   }

   // forward console output
   enqueueChunkOutput(
            docId,
            chunkId,
            notebookCtxId(),
            0, // no ordinal needed
            ChunkOutputText,
            consolePath);

   return Success();
}

Error interruptEngineChunk(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string docId, chunkId;
   Error error = json::readParams(request.params,
                                  &docId,
                                  &chunkId);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   interruptChunk(docId, chunkId);
   return Success();
}

} // anonymous namespace

Error executeAlternateEngineChunk(const std::string& docId,
                                  const std::string& chunkId,
                                  const std::string& nbCtxId,
                                  const core::FilePath& workingDir,
                                  const std::string& engine,
                                  const std::string& code,
                                  const json::Object& jsonChunkOptions)
{
   // read json chunk options
   std::map<std::string, std::string> options;
   for (json::Object::const_iterator it = jsonChunkOptions.begin();
        it != jsonChunkOptions.end();
        ++it)
   {
      if (it->second.type() == json::StringType)
         options[it->first] = it->second.get_str();
   }

   // set working directory
   DirCapture dir;
   dir.connectDir(docId, workingDir);
   
   // handle some engines with their own custom routines
   Error error = Success();
   if (engine == "Rcpp")
      error = executeRcppEngineChunk(docId, chunkId, nbCtxId, code, options);
   else if (engine == "stan")
      error = executeStanEngineChunk(docId, chunkId, nbCtxId, code, options);
   else if (engine == "sql")
      error = executeSqlEngineChunk(docId, chunkId, nbCtxId, code, jsonChunkOptions);
   else
      runChunk(docId, chunkId, nbCtxId, engine, code, options);

   // release working directory
   dir.disconnect();

   return error;
}

Error initAlternateEngines()
{
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "interrupt_chunk", interruptEngineChunk));
   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio


