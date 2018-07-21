/*
 * SessionAsyncRProcess.cpp
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

#include <boost/foreach.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/SessionModuleContext.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>

#include <r/RExec.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionAsyncRProcess.hpp>

using namespace rstudio;
using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace async_r {

AsyncRProcess::AsyncRProcess():
   isRunning_(false),
   terminationRequested_(false)
{
}

void AsyncRProcess::start(const std::string& command,
                          core::system::Options environment,
                          const core::FilePath& workingDir,
                          AsyncRProcessOptions rOptions,
                          std::vector<core::FilePath> rSourceFiles,
                          const std::string& input)
{
   // R binary
   core::FilePath rProgramPath;
   core::Error error = module_context::rScriptPath(&rProgramPath);
   if (error)
   {
      LOG_ERROR(error);
      onCompleted(EXIT_FAILURE);
      return;
   }
   
   // core R files for augmented async processes
   if (rOptions & R_PROCESS_AUGMENTED)
   {
      // R files we wish to source to provide functionality to async process
      const core::FilePath modulesPath =
            session::options().modulesRSourcePath();
      
      const core::FilePath rPath =
            session::options().coreRSourcePath();
      
      const core::FilePath rTools =  rPath.childPath("Tools.R");
      
      // insert at begin as Tools.R needs to be sourced first
      rSourceFiles.insert(rSourceFiles.begin(), rTools);
   }

   // args
   std::vector<std::string> args;
   args.push_back("--slave");
   if (rOptions & R_PROCESS_VANILLA)
      args.push_back("--vanilla");
   if (rOptions & R_PROCESS_NO_RDATA)
   {
      args.push_back("--no-save");
      args.push_back("--no-restore");
   }

   // for windows we need to forward setInternet2
#ifdef _WIN32
   if (!r::session::utils::isR3_3() && userSettings().useInternet2())
      args.push_back("--internet2");
#endif

   args.push_back("-f");
   
   // generate path to temporary file
   std::string path;
   error = r::exec::RFunction("base:::tempfile")
         .addParam("pattern", "rstudio-async-process-")
         .addParam("tmpdir", workingDir.absolutePathNative())
         .addParam("fileext", ".R")
         .call(&path);
   if (error)
   {
      LOG_ERROR(error);
      onCompleted(EXIT_FAILURE);
      return;
   }
   scriptPath_ = module_context::resolveAliasedPath(path);
   
   // set this as the file to run
   args.push_back(scriptPath_.absolutePathNative());
   
   // start generating code
   std::stringstream ss;
   
   // emit source statements to the file
   if (!rSourceFiles.empty())
   {
      ss << "# RStudio Source Files ----" << std::endl;
      for (const FilePath& filePath : rSourceFiles)
         ss << "source('" << filePath.absolutePathNative() << "')" << std::endl;
      ss << std::endl;
   }
   
   // emit code
   ss << "# Script ----" << std::endl;
   ss << command << std::endl;
   
   // write it to file
   error = core::writeStringToFile(scriptPath_, ss.str());
   if (error)
      LOG_ERROR(error);
   
   // options
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   if (rOptions & R_PROCESS_REDIRECTSTDERR)
      options.redirectStdErrToStdOut = true;

   // if a working directory was specified, use it
   if (!workingDir.empty())
   {
      options.workingDir = workingDir;
   }

   // forward R_LIBS so the child process has access to the same libraries
   // we do
   core::system::Options childEnv;
   core::system::environment(&childEnv);
   std::string libPaths = module_context::libPathsString();
   if (!libPaths.empty())
   {
      core::system::setenv(&childEnv, "R_LIBS", libPaths);
   }
   // forward passed environment variables
   BOOST_FOREACH(const core::system::Option& var, environment)
   {
      core::system::setenv(&childEnv, var.first, var.second);
   }
   options.environment = childEnv;

   core::system::ProcessCallbacks cb;
   using namespace module_context;
   cb.onStarted = boost::bind(&AsyncRProcess::onProcessStarted,
                              AsyncRProcess::shared_from_this(),
                              _1);
   cb.onContinue = boost::bind(&AsyncRProcess::onProcessContinue,
                               AsyncRProcess::shared_from_this());
   cb.onStdout = boost::bind(&AsyncRProcess::onProcessStdout,
                             AsyncRProcess::shared_from_this(),
                             _2);
   cb.onStderr = boost::bind(&AsyncRProcess::onProcessStderr,
                             AsyncRProcess::shared_from_this(),
                             _2);
   cb.onExit =  boost::bind(&AsyncRProcess::onProcessCompleted,
                             AsyncRProcess::shared_from_this(),
                             _1);

   // forward input if requested
   input_ = input;

   error = module_context::processSupervisor().runProgram(
            rProgramPath.absolutePath(),
            args,
            options,
            cb);
   if (error)
   {
      LOG_ERROR(error);
      onCompleted(EXIT_FAILURE);
   }
   else
   {
      isRunning_ = true;
   }
}

void AsyncRProcess::onStarted(core::system::ProcessOperations& operations)
{
}

void AsyncRProcess::onStdout(const std::string& output)
{
}

void AsyncRProcess::onStderr(const std::string& output)
{
}

bool AsyncRProcess::onContinue()
{
   return true;
}

void AsyncRProcess::onCompleted(int exitStatus)
{
}

bool AsyncRProcess::terminationRequested()
{
   return terminationRequested_;
}

void AsyncRProcess::onProcessStarted(system::ProcessOperations& operations)
{
   if (!input_.empty())
   {
      core::Error error = operations.writeToStdin(input_, true);
      if (error)
      {
         LOG_ERROR(error);
         error = operations.terminate();
         if (error)
            LOG_ERROR(error);
      }
   }
   
   onStarted(operations);
}

bool AsyncRProcess::onProcessContinue()
{
   if (terminationRequested())
      return false;
   
   return onContinue();
}

void AsyncRProcess::onProcessStdout(const std::string& output)
{
   onStdout(output);
}

void AsyncRProcess::onProcessStderr(const std::string& output)
{
   onStderr(output);
}

void AsyncRProcess::onProcessCompleted(int exitStatus)
{
   markCompleted();
   
   if (exitStatus)
   {
      LOG_ERROR_MESSAGE(
               "Error executing background script " +
               scriptPath_.absolutePath());
   }
   else
   {
      scriptPath_.removeIfExists();
   }
   
   onCompleted(exitStatus);
}

bool AsyncRProcess::isRunning()
{
   return isRunning_;
}

void AsyncRProcess::terminate()
{
   terminationRequested_ = true;
}

void AsyncRProcess::markCompleted() 
{
   isRunning_ = false;
}

AsyncRProcess::~AsyncRProcess()
{
}

} // namespace async_r
} // namespace session
} // namespace rstudio

