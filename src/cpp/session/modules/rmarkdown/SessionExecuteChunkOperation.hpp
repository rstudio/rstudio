/*
 * SessionExecuteChunkOperation.hpp
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
#ifndef SESSION_MODULES_RMARKDOWN_SESSION_EXECUTE_CHUNK_OPERATIONR_HPP
#define SESSION_MODULES_RMARKDOWN_SESSION_EXECUTE_CHUNK_OPERATIONR_HPP

#include <boost/bind.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/noncopyable.hpp>
#include <boost/shared_ptr.hpp>

#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
#include <core/text/CsvParser.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/system/System.hpp>

#include "NotebookOutput.hpp"
#include "NotebookExec.hpp"
#include "SessionRmdNotebook.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

core::shell_utils::ShellCommand shellCommandForEngine(
      const std::string& engine,
      const core::FilePath& scriptPath,
      const std::map<std::string, std::string>& options)
{
   using namespace core;
   using namespace core::string_utils;
   using namespace core::shell_utils;
   
   // determine engine path -- respect chunk option 'engine.path' if supplied
   std::string enginePath = engine;
   if (options.count("engine.path"))
   {
      std::string path = options.at("engine.path");
      enginePath = module_context::resolveAliasedPath(path).absolutePath();
   }
   
   ShellCommand command(enginePath);
   
   // pass along 'engine.opts' if supplied
   if (options.count("engine.opts"))
      command << EscapeFilesOnly << options.at("engine.opts") << EscapeAll;
   
   // pass path to file
   command << utf8ToSystem(scriptPath.absolutePathNative());
   
   return command;
}

class ExecuteChunkOperation : boost::noncopyable,
                              public boost::enable_shared_from_this<ExecuteChunkOperation>
{
   typedef core::shell_utils::ShellCommand ShellCommand;
   typedef core::system::ProcessCallbacks ProcessCallbacks;
   typedef core::system::ProcessOperations ProcessOperations;
   
public:
   static boost::shared_ptr<ExecuteChunkOperation> create(const std::string& docId,
                                                          const std::string& chunkId,
                                                          const std::string& nbCtxId,
                                                          const ShellCommand& command,
                                                          const core::FilePath& scriptPath)
   {
      boost::shared_ptr<ExecuteChunkOperation> pProcess =
            boost::shared_ptr<ExecuteChunkOperation>(new ExecuteChunkOperation(
                                                        docId,
                                                        chunkId,
                                                        nbCtxId,
                                                        command,
                                                        scriptPath));
      pProcess->registerProcess();
      return pProcess;
   }
   
private:
   
   ExecuteChunkOperation(const std::string& docId,
                         const std::string& chunkId,
                         const std::string& nbCtxId,
                         const ShellCommand& command,
                         const core::FilePath& scriptPath)
      : terminationRequested_(false),
        docId_(docId),
        chunkId_(chunkId),
        nbCtxId_(nbCtxId),
        command_(command),
        scriptPath_(scriptPath)
   {
      using namespace core;
      Error error = Success();
      
      // ensure regular directory
      FilePath outputPath = chunkOutputPath(
               docId_,
               chunkId_,
               ContextExact);
      
      error = outputPath.removeIfExists();
      if (error)
         LOG_ERROR(error);
      
      error = outputPath.ensureDirectory();
      if (error)
         LOG_ERROR(error);
      
      // clean old chunk output
      error = cleanChunkOutput(docId_, chunkId_, nbCtxId_, true);
      if (error)
         LOG_ERROR(error);
   }
   
public:
   
   ProcessCallbacks processCallbacks()
   {
      ProcessCallbacks callbacks;
      
      callbacks.onStarted = boost::bind(
               &ExecuteChunkOperation::onStarted,
               shared_from_this());
      
      callbacks.onContinue = boost::bind(
               &ExecuteChunkOperation::onContinue,
               shared_from_this());
      
      callbacks.onStdout = boost::bind(
               &ExecuteChunkOperation::onStdout,
               shared_from_this(),
               _2);
      
      callbacks.onStderr = boost::bind(
               &ExecuteChunkOperation::onStderr,
               shared_from_this(),
               _2);
      
      callbacks.onExit = boost::bind(
               &ExecuteChunkOperation::onExit,
               shared_from_this(),
               _1);
      
      return callbacks;
   }
   
private:
   
   enum OutputType { OUTPUT_STDOUT, OUTPUT_STDERR };
   
   void onStarted()
   {
   }
   
   bool onContinue()
   {
      return !terminationRequested_;
   }
   
   void onExit(int exitStatus)
   {
      events().onChunkExecCompleted(docId_, chunkId_, notebookCtxId());
      deregisterProcess();
      scriptPath_.removeIfExists();
   }
   
   void onStdout(const std::string& output)
   {
      onText(output, OUTPUT_STDOUT);
   }
   
   void onStderr(const std::string& output)
   {
      onText(output, OUTPUT_STDERR);
   }
   
   void onText(const std::string& output, OutputType outputType)
   {
      using namespace core;
      
      // get path to cache file
      FilePath target = chunkOutputFile(docId_, chunkId_, nbCtxId_,
            ChunkOutputText);
      
      // append console data (for notebook cache)
      notebook::appendConsoleOutput(
               outputType == OUTPUT_STDOUT ? kChunkConsoleOutput : kChunkConsoleError,
               output,
               target);
      
      // write to temporary file (for streaming output)
      FilePath tempFile = module_context::tempFile("chunk-output-", "");
      RemoveOnExitScope scope(tempFile, ERROR_LOCATION);
      notebook::appendConsoleOutput(
               outputType == OUTPUT_STDOUT ? kChunkConsoleOutput : kChunkConsoleError,
               output,
               tempFile);
      
      // emit client event
      enqueueChunkOutput(
               docId_,
               chunkId_,
               nbCtxId_,
               0,  // no ordinals needed for alternate engines
               ChunkOutputText,
               tempFile);
   }
   
public:
   
   void terminate() { terminationRequested_ = true; }
   bool terminationRequested() const { return terminationRequested_; }
   const std::string& chunkId() const { return chunkId_; }
   
private:
   bool terminationRequested_;
   std::string docId_;
   std::string chunkId_;
   std::string nbCtxId_;
   ShellCommand command_;
   core::FilePath scriptPath_;
   
private:
   
   typedef std::map<
      std::string,
      boost::shared_ptr<ExecuteChunkOperation>
   > ProcessRegistry;
   
   static ProcessRegistry& registry()
   {
      static ProcessRegistry instance;
      return instance;
   }
   
   void registerProcess()
   {
      registry()[docId_ + "-" + chunkId_] = shared_from_this();
   }
   
   void deregisterProcess()
   {
      registry().erase(docId_ + "-" + chunkId_);
   }
   
public:
   static boost::shared_ptr<ExecuteChunkOperation> getProcess(
         const std::string& docId,
         const std::string& chunkId)
   {
      return registry()[docId + "-" + chunkId];
   }
};

core::Error runChunk(const std::string& docId,
                     const std::string& chunkId,
                     const std::string& nbCtxId,
                     const std::string& engine,
                     const std::string& code,
                     const std::map<std::string, std::string>& chunkOptions)
{
   using namespace core;
   typedef core::shell_utils::ShellCommand ShellCommand;
   
   // write code to temporary file
   FilePath scriptPath = module_context::tempFile("chunk-code", "");
   Error error = core::writeStringToFile(scriptPath, code);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // get command
   ShellCommand command = notebook::shellCommandForEngine(engine, scriptPath, chunkOptions);

   // create process
   boost::shared_ptr<ExecuteChunkOperation> operation =
         ExecuteChunkOperation::create(docId, chunkId, nbCtxId, command, 
               scriptPath);

   // write input code to cache
   FilePath cacheFilePath = notebook::chunkOutputFile(
            docId,
            chunkId,
            nbCtxId,
            ChunkOutputText);
   
   error = notebook::appendConsoleOutput(
            kChunkConsoleInput,
            code,
            cacheFilePath);
   
   if (error)
      LOG_ERROR(error);
   
   // generate process options
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   
   core::system::Options env;
   core::system::environment(&env);
   
   // if we're using python in a virtual environment, then
   // set the VIRTUAL_ENV + PATH environment variables appropriately
   if (engine == "python")
   {
      // determine engine path -- respect chunk option 'engine.path' if supplied
      FilePath enginePath;
      if (chunkOptions.count("engine.path"))
      {
         enginePath = module_context::resolveAliasedPath(chunkOptions.at("engine.path"));
      }
      else
      {
         core::system::realPath(engine, &enginePath);
      }
      
      // if we discovered the engine path, then look for an 'activate' script
      // in the same directory -- if it exists, this is a virtual env
      if (enginePath.exists())
      {
         FilePath activatePath = enginePath.parent().childPath("activate");
         if (activatePath.exists())
         {
            FilePath binPath = enginePath.parent();
            FilePath venvPath = binPath.parent();
            core::system::setenv(&env, "VIRTUAL_ENV", venvPath.absolutePath());
            core::system::addToPath(&env, binPath.absolutePath(), true);
         }
      }
   }
   
   options.environment = env;
   
   // run it
   error = module_context::processSupervisor().runCommand(
            command,
            options,
            operation->processCallbacks());
   
   if (error)
      LOG_ERROR(error);
   
   return Success();
}

void interruptChunk(const std::string& docId,
                    const std::string& chunkId)
{
   boost::shared_ptr<ExecuteChunkOperation> pOperation =
         ExecuteChunkOperation::getProcess(docId, chunkId);
   
   if (pOperation)
      pOperation->terminate();
}

} // end namespace notebook
} // end namespace rmarkdown
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* SESSION_MODULES_RMARKDOWN_SESSION_EXECUTE_CHUNK_OPERATIONR_HPP */
