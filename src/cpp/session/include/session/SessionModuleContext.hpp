/*
 * SessionModuleContext.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_MODULE_CONTEXT_HPP
#define SESSION_MODULE_CONTEXT_HPP

#include <string>

#include <boost/utility.hpp>
#include <boost/function.hpp>
#include <boost/signals.hpp>

#include <core/system/System.hpp>
#include <core/http/UriHandler.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/Thread.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionClientEvent.hpp>

namespace core {
   class Error;
   class Success;
   class FilePath;
   class FileInfo;
   class Settings;
}

namespace session {   
namespace module_context {
    
// paths 
core::FilePath userHomePath();
std::string createAliasedPath(const core::FileInfo& fileInfo);
std::string createAliasedPath(const core::FilePath& path);
core::FilePath resolveAliasedPath(const std::string& aliasedPath);
core::FilePath userScratchPath();
bool isVisibleUserFile(const core::FilePath& filePath);

core::FilePath initialWorkingDirectory();

core::FilePath safeCurrentPath();

core::json::Object createFileSystemItem(const core::FileInfo& fileInfo);
core::json::Object createFileSystemItem(const core::FilePath& filePath);
   
// get a temp file
core::FilePath tempFile(const std::string& prefix, 
                        const std::string& extension);
 
// register a handler for rBrowseUrl
typedef boost::function<bool(const std::string&)> RBrowseUrlHandler;
core::Error registerRBrowseUrlHandler(const RBrowseUrlHandler& handler);
   
// register a handler for rBrowseFile
typedef boost::function<bool(const core::FilePath&)> RBrowseFileHandler;
core::Error registerRBrowseFileHandler(const RBrowseFileHandler& handler);
   
// register an inbound uri handler (include a leading slash)
core::Error registerUriHandler(
                        const std::string& name, 
                        const core::http::UriHandlerFunction& handlerFunction);
   
// register a local uri handler (scoped by a special prefix which indicates
// a local scope)
core::Error registerLocalUriHandler(
                        const std::string& name, 
                        const core::http::UriHandlerFunction& handlerFunction);
   
// register a postback handler. see docs in SessionPostback.cpp for 
// details on the requirements of postback handlers
typedef boost::function<void(const std::string&)> PostbackHandlerFunction;
core::Error registerPostbackHandler(
                              const std::string& name,
                              const PostbackHandlerFunction& handlerFunction,
                              std::string* pShellCommand); 
                        
// register an rpc method
core::Error registerRpcMethod(const std::string& name,
                              const core::json::JsonRpcFunction& function);


core::Error executeAsync(const core::json::JsonRpcFunction& function,
                         const core::json::JsonRpcRequest& request,
                         core::json::JsonRpcResponse* pResponse);

namespace {

template <typename T>
core::Error rpcAsyncCoupleRunner(
      boost::function<core::Error(const core::json::JsonRpcRequest&, T*)> initFunc,
      boost::function<core::Error(const core::json::JsonRpcRequest&, const T&, core::json::JsonRpcResponse*)> workerFunc,
      const core::json::JsonRpcRequest& request,
      core::json::JsonRpcResponse* pResponse)
{
   T state;
   core::Error error = initFunc(request, &state);
   if (error)
      return error;

   return executeAsync(boost::bind(workerFunc, _1, state, _2),
                       request,
                       pResponse);
}

} // anonymous namespace

// Registers a two-part request handler, where "initFunc" runs on the main
// thread (and has access to everything a normal handler does, like R) and
// "workerFunc" runs on a background thread (and must not touch anything
// that isn't threadsafe). It is a Good Idea to only use workerFunc functions
// that are declared in the "workers" sub-project.
//
// The T type parameter represents the type of a value that initFunc produces
// and workerFunc consumes. This can be used to pass context between the two.
template <typename T>
core::Error registerRpcAsyncCoupleMethod(
      const std::string& name,
      boost::function<core::Error(const core::json::JsonRpcRequest&, T*)> initFunc,
      boost::function<core::Error(const core::json::JsonRpcRequest&, const T&, core::json::JsonRpcResponse*)> workerFunc)
{
   return registerRpcMethod(name, boost::bind(rpcAsyncCoupleRunner<T>,
                                              initFunc,
                                              workerFunc,
                                              _1,
                                              _2));
}


enum ChangeSource
{
   ChangeSourceREPL,
   ChangeSourceRPC,
   ChangeSourceURI
};
   
// session events
struct Events : boost::noncopyable
{
   boost::signal<void ()>              onClientInit;
   boost::signal<void ()>              onBeforeExecute;
   boost::signal<void (ChangeSource)>  onDetectChanges;
   boost::signal<void(bool)>           onShutdown;
   boost::signal<void ()>              onSysSleep;
};

Events& events();

// launch a child process which can still be interrupted using the
// standard mechanisms
core::Error executeInterruptableChild(std::string path,
                                      core::system::Options args);

// source R files
core::Error sourceModuleRFile(const std::string& rSourceFile);   
   
// enque client events (note R methods can do this via .rs.enqueClientEvent)
void enqueClientEvent(const ClientEvent& event);
   
// write output to the console (convenience wrapper for enquing a 
// kConsoleWriteOutput event)
void consoleWriteOutput(const std::string& output);   
   
// write an error to the console (convenience wrapper for enquing a 
// kConsoleWriteOutput event)
void consoleWriteError(const std::string& message);
   
// show an error dialog (convenience wrapper for enquing kShowErrorMessage)
void showErrorMessage(const std::string& title, const std::string& message);

void showFile(const core::FilePath& filePath,
              const std::string& window = "_blank");


void showContent(const std::string& title, const core::FilePath& filePath);

bool isGoogleDocsIntegrationEnabled();
void setGoogleDocsIntegrationEnabled(bool enabled);


// persist state accross suspend and resume
   
typedef boost::function<void (core::Settings*)> SuspendFunction;
typedef boost::function<void(const core::Settings&)> ResumeFunction;

class SuspendHandler
{
public:
   SuspendHandler(const SuspendFunction& suspend,
                  const ResumeFunction& resume)
      : suspend_(suspend), resume_(resume)
   {
   }
   
   // COPYING: via compiler
   
   const SuspendFunction& suspend() const { return suspend_; }
   const ResumeFunction& resume() const { return resume_; }
   
private:
   SuspendFunction suspend_;
   ResumeFunction resume_;
};
   
void addSuspendHandler(const SuspendHandler& handler);

} // namespace module_context
} // namespace session

#endif // SESSION_MODULE_CONTEXT_HPP

