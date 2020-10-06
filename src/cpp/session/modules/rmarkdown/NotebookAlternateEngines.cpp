/*
 * NotebookAlternateEngines.cpp
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


#include "SessionRmdNotebook.hpp"
#include "SessionExecuteChunkOperation.hpp"
#include "NotebookCache.hpp"
#include "NotebookAlternateEngines.hpp"
#include "NotebookWorkingDir.hpp"
#include "NotebookHtmlWidgets.hpp"

#include <boost/bind.hpp>
#include <boost/algorithm/string.hpp>

#include <core/StringUtils.hpp>
#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>

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
                           const std::string& chunkId,
                           const std::string& chunkCode,
                           const std::string& chunkLabel)
      : docId_(docId),
        chunkId_(chunkId),
        chunkCode_(chunkCode),
        chunkLabel_(chunkLabel)
   {
   }
   
   ~ChunkExecCompletedScope()
   {
      events().onChunkExecCompleted(
               docId_,
               chunkId_,
               chunkCode_,
               chunkLabel_,
               notebookCtxId());
   }
   
private:
   std::string docId_;
   std::string chunkId_;
   std::string chunkCode_;
   std::string chunkLabel_;
};

class ChunkExecDisconnectScope : boost::noncopyable
{
public:
   ChunkExecDisconnectScope(ChunkExecContext& chunkExecContext)
      : chunkExecContext_(chunkExecContext)
   {
   }

   ~ChunkExecDisconnectScope()
   {
      chunkExecContext_.disconnect();
   }

private:
   ChunkExecContext& chunkExecContext_;
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
   
   error = pChunkOutputFile->removeIfExists();
   if (error)
      return error;
   
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
                             const std::string& chunkLabel,
                             const std::string& nbCtxId,
                             const std::string& code,
                             const std::map<std::string, std::string>& options)
{
   // forward declare error
   Error error;
   
   // always ensure we emit a 'execution complete' event on exit
   ChunkExecCompletedScope execScope(docId, chunkId, code, chunkLabel);
   
   // prepare cache output file (use tempfile on failure)
   FilePath targetPath = module_context::tempFile("rcpp-cache-", "txt");
   error = prepareCacheConsoleOutputFile(docId, chunkId, nbCtxId, &targetPath);
   if (error)
      LOG_ERROR(error);
   
   // capture console output, error
   RSTUDIO_BOOST_SCOPED_CONNECTION consoleHandler =
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
                             const std::string& chunkLabel,
                             const std::string& nbCtxId,
                             const std::string& code,
                             const std::map<std::string, std::string>& options)
{
   // forward-declare error
   Error error;
   
   // ensure we always emit an execution complete event on exit
   ChunkExecCompletedScope execScope(docId, chunkId, code, chunkLabel);
   
   // prepare console output file -- use tempfile on failure
   FilePath targetPath = module_context::tempFile("stan-cache-", "txt");
   error = prepareCacheConsoleOutputFile(docId, chunkId, nbCtxId, &targetPath);
   if (error)
      LOG_ERROR(error);
   
   // capture console output, error
   RSTUDIO_BOOST_SCOPED_CONNECTION consoleHandler =
         module_context::events().onConsoleOutput.connect(
            boost::bind(chunkConsoleOutputHandler,
                        _1,
                        _2,
                        targetPath));
   
   // write code to file
   FilePath tempFile = module_context::tempFile("stan-", "stan");
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

   // write input code to cache
   error = appendConsoleOutput(
            kChunkConsoleInput,
            code,
            targetPath);
   if (error)
      LOG_ERROR(error);

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
      fStanEngine.addParam("file", string_utils::utf8ToSystem(tempFile.getAbsolutePath()));
   
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
                            const std::string& chunkLabel,
                            const std::string& nbCtxId,
                            const std::string& code,
                            const json::Object& options)
{
   Error error;
   
   // ensure we always emit an execution complete event on exit
   ChunkExecCompletedScope execScope(docId, chunkId, code, chunkLabel);

   // prepare console output file -- use tempfile on failure
   FilePath consolePath = module_context::tempFile("data-console-", "txt");
   error = prepareCacheConsoleOutputFile(docId, chunkId, nbCtxId, &consolePath);
   if (error)
      LOG_ERROR(error);
   
   // capture console output, error
   RSTUDIO_BOOST_SCOPED_CONNECTION consoleHandler =
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
               string_utils::utf8ToSystem(dataPath.getAbsolutePath()),
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

Error runUserDefinedEngine(const std::string& docId,
                           const std::string& chunkId,
                           const std::string& chunkLabel,
                           const std::string& nbCtxId,
                           const std::string& engine,
                           const std::string& code,
                           json::Object options,
                           ChunkExecContext& htmlCaptureContext)
{
   using namespace r::sexp;
   using namespace r::exec;
   Error error;
   
   // always ensure we emit a 'execution complete' event on exit
   ChunkExecCompletedScope execScope(docId, chunkId, code, chunkLabel);

   // connect to capture html file output
   ChunkExecDisconnectScope chunkExecDisconnectScope(htmlCaptureContext);
   
   // prepare cache folder
   FilePath cachePath = notebook::chunkOutputPath(
            docId,
            chunkId,
            notebook::ContextExact);
   error = cachePath.resetDirectory();
   if (error)
      return error;

   // save code for preview rendering
   htmlCaptureContext.onConsoleInput(code);
   
   // default to 'error=FALSE' when unset
   if (options.find("error") == options.end())
      options["error"] = false;
   
   // determine whether we want to emit warnings, errors in this chunk
   bool emitWarnings = true;
   core::json::readObject(options, "warning", emitWarnings);
   
   unsigned int ordinal = 1;
   
   // helper function for emitting console text
   auto emitText = [&](const std::string& text, int outputType) -> Error
   {
      Error error;

      FilePath targetPath = notebook::chunkOutputFile(
               docId,
               chunkId,
               nbCtxId,
               ChunkOutputText);
      
      error = targetPath.getParent().ensureDirectory();
      if (error)
         return error;

      error = writeConsoleOutput(
               outputType,
               text,
               targetPath,
               true);
      if (error)
         return error;

      enqueueChunkOutput(
               docId,
               chunkId,
               nbCtxId,
               ordinal++,
               ChunkOutputText,
               targetPath);

      return Success();
   };
   
   // helper function for emitting an image
   auto emitImage = [&](const std::string& path) -> Error
   {
      Error error;
      
      FilePath sourcePath = module_context::resolveAliasedPath(path);
      FilePath targetPath = notebook::chunkOutputFile(
               docId,
               chunkId,
               nbCtxId,
               ChunkOutputPlot);

      error = targetPath.getParent().ensureDirectory();
      if (error)
         return error;

      error = sourcePath.move(targetPath);
      if (error)
         return error;

      enqueueChunkOutput(
               docId,
               chunkId,
               nbCtxId,
               ordinal++,
               ChunkOutputPlot,
               targetPath);
      
      return Success();
   };
   
   // output will be captured by engine, but evaluation errors may be
   // emitted directly to console, so capture those. note that the reticulate
   // engine will automatically capture errors when 'error=TRUE', so if we
   // receive an error it implies we should emit it to the chunk and execution
   // will automatically stop
   auto consoleHandler = [&](
         module_context::ConsoleOutputType type,
         const std::string& output)
   {
      if (type == module_context::ConsoleOutputError)
      {
         std::string errorPrefix =
                "Error in py_run_string_impl(code, local, convert) : ";
               
         if (boost::algorithm::starts_with(output, errorPrefix))
         {
            emitText(output.substr(errorPrefix.size()), kChunkConsoleError);
         }
         else
         {
            emitText(output, kChunkConsoleError);
         }
      }
   };
   
   RSTUDIO_BOOST_SCOPED_CONNECTION handler =
         module_context::events().onConsoleOutput.connect(consoleHandler);
   
   // run the user-defined engine
   SEXP outputSEXP = R_NilValue;
   Protect protect;
   error = RFunction(".rs.runUserDefinedEngine")
         .addParam(engine)
         .addParam(code)
         .addParam(options)
         .call(&outputSEXP, &protect);
   
   // report errors during engine execution to user
   if (error)
   {
      FilePath targetPath = module_context::tempFile(
               "reticulate-engine-",
               ".txt");
      
      chunkConsoleOutputHandler(
               module_context::ConsoleOutputError,
               r::endUserErrorMessage(error),
               targetPath);
      return error;
   }
   
   // generic engine output (as a single string of console output)
   if (isString(outputSEXP))
   {
      std::string text = asUtf8String(outputSEXP);
      emitText(text, kChunkConsoleOutput);
   }
   else if (inherits(outputSEXP, "htmlwidget"))
   {
      // render htmlwidget to file
      SEXP pathSEXP = R_NilValue;
      Protect protect;
      error = RFunction("print")
         .addParam(outputSEXP)
         .call(&pathSEXP, &protect);
   }
   // evaluate-style (list) output
   else if (isList(outputSEXP))
   {
      int n = length(outputSEXP);
      for (int i = 0; i < n; i++)
      {
         SEXP elSEXP = VECTOR_ELT(outputSEXP, i);
         
         if (inherits(elSEXP, "condition") && emitWarnings)
         {
            // captured R error -- emit as error message
            std::string message;
            Error error = RFunction("base:::conditionMessage")
                  .addParam(elSEXP)
                  .call(&message);
            if (error)
            {
               LOG_ERROR(error);
               continue;
            }

            emitText(message, kChunkConsoleError);
         }
         else if (inherits(elSEXP, "knit_image_paths"))
         {
            // handle a plot provided by e.g. knitr::include_graphics()
            std::string path = asUtf8String(elSEXP);
            Error error = emitImage(path);
            if (error)
            {
               LOG_ERROR(error);
               continue;
            }
            
         }
         else if (inherits(elSEXP, "reticulate_matplotlib_plot"))
         {
            // matplotlib-generated plot -- forward the image path
            std::string path;
            Error error = getNamedListElement(elSEXP, "path", &path);
            if (error)
            {
               LOG_ERROR(error);
               continue;
            }
            
            error = emitImage(path);
            if (error)
            {
               LOG_ERROR(error);
               continue;
            }
         }
         else if (isString(elSEXP) || isNumeric(elSEXP))
         {
            // plain old console text output -- emit as-is
            std::string text = asUtf8String(elSEXP);
            Error error = emitText(text, kChunkConsoleOutput);
            if (error)
            {
               LOG_ERROR(error);
               continue;
            }
         }
         else
         {
            Rf_warning(
                     "don't know how to handle engine output of type '%s'",
                     r::sexp::typeAsString(elSEXP).c_str());
         }
      }
   }
   else
   {
      Rf_warning(
               "don't know how to handle '%s' engine output",
               engine.c_str());
   }
   
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
                                  const std::string& label,
                                  const std::string& nbCtxId,
                                  const core::FilePath& workingDir,
                                  const std::string& engine,
                                  const std::string& code,
                                  const ChunkOptions& chunkOptions,
                                  ExecScope execScope,
                                  int pixelWidth,
                                  int charWidth)
{
   json::Object jsonChunkOptions  = chunkOptions.mergedOptions();

   // read json chunk options
   std::map<std::string, std::string> options;
   for (json::Object::Iterator it = jsonChunkOptions.begin();
        it != jsonChunkOptions.end();
        ++it)
   {
      if ((*it).getValue().getType() == json::Type::STRING)
         options[(*it).getName()] = (*it).getValue().getString();
   }

   // set working directory
   DirCapture dir;
   dir.connectDir(docId, workingDir);
   
   // handle some engines with their own custom routines
   Error error = Success();
   if (engine == "Rcpp")
      error = executeRcppEngineChunk(docId, chunkId, label, nbCtxId, code, options);
   else if (engine == "stan")
      error = executeStanEngineChunk(docId, chunkId, label, nbCtxId, code, options);
   else if (engine == "sql")
      error = executeSqlEngineChunk(docId, chunkId, label, nbCtxId, code, jsonChunkOptions);
   else
   {
      // check to see if this is a known interpreter; if so, we'll
      // use own own shim to run the engine. if not, we'll just call
      // the engine as-is
      using namespace r::exec;
      using namespace r::sexp;
      
      bool isSystemInterpreter = false;
      Error error = RFunction(".rs.isSystemInterpreter")
            .addParam(engine)
            .call(&isSystemInterpreter);
      
      if (isSystemInterpreter)
      {
         runChunk(docId, chunkId, label, nbCtxId, engine, code, options);
      }
      else
      {
         // connect to capture html file output
         ChunkExecContext htmlCaptureContext(
            docId, chunkId, code, label, nbCtxId, engine, execScope,
            workingDir, chunkOptions, pixelWidth, charWidth);
         htmlCaptureContext.connect();

         runUserDefinedEngine(docId, chunkId, label, nbCtxId, engine, code, jsonChunkOptions,
            htmlCaptureContext);
      }
   }

   // release working directory
   dir.disconnect();

   return error;
}

Error initAlternateEngines()
{
   using namespace module_context;
   using boost::bind;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "NotebookAlternateEngines.R"))
      (bind(registerRpcMethod, "interrupt_chunk", interruptEngineChunk));
   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio


