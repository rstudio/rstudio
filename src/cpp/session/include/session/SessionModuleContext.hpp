/*
 * SessionModuleContext.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include <boost/shared_ptr.hpp>

#include <core/system/System.hpp>
#include <core/system/FileChangeEvent.hpp>
#include <core/system/ShellUtils.hpp>
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
   namespace system {
      class ProcessSupervisor;
   }
}

namespace session {   
namespace module_context {
    
// paths 
core::FilePath userHomePath();
std::string createAliasedPath(const core::FileInfo& fileInfo);
std::string createAliasedPath(const core::FilePath& path);
std::string createFileUrl(const core::FilePath& path);
core::FilePath resolveAliasedPath(const std::string& aliasedPath);
core::FilePath userScratchPath();
core::FilePath scopedScratchPath();
core::FilePath oldScopedScratchPath();
bool isVisibleUserFile(const core::FilePath& filePath);

core::FilePath safeCurrentPath();

core::json::Object createFileSystemItem(const core::FileInfo& fileInfo);
core::json::Object createFileSystemItem(const core::FilePath& filePath);
   
// get a temp file
core::FilePath tempFile(const std::string& prefix, 
                        const std::string& extension);

// find out the location of a binary
core::FilePath findProgram(const std::string& name);

// find the location of the R script
core::Error rBinDir(core::FilePath* pRBinDirPath);
core::Error rScriptPath(core::FilePath* pRScriptPath);
core::shell_utils::ShellCommand rCmd(const core::FilePath& rBinDir);


// register a handler for rBrowseUrl
typedef boost::function<bool(const std::string&)> RBrowseUrlHandler;
core::Error registerRBrowseUrlHandler(const RBrowseUrlHandler& handler);
   
// register a handler for rBrowseFile
typedef boost::function<bool(const core::FilePath&)> RBrowseFileHandler;
core::Error registerRBrowseFileHandler(const RBrowseFileHandler& handler);
   
// register an inbound uri handler (include a leading slash)
core::Error registerAsyncUriHandler(
                   const std::string& name,
                   const core::http::UriAsyncHandlerFunction& handlerFunction);

// register an inbound uri handler (include a leading slash)
core::Error registerUriHandler(
                        const std::string& name,
                        const core::http::UriHandlerFunction& handlerFunction);

// register a local uri handler (scoped by a special prefix which indicates
// a local scope)
core::Error registerAsyncLocalUriHandler(
                   const std::string& name,
                   const core::http::UriAsyncHandlerFunction& handlerFunction);

// register a local uri handler (scoped by a special prefix which indicates
// a local scope)
core::Error registerLocalUriHandler(
                        const std::string& name,
                        const core::http::UriHandlerFunction& handlerFunction);

typedef boost::function<void(int, const std::string&)> PostbackHandlerContinuation;

// register a postback handler. see docs in SessionPostback.cpp for 
// details on the requirements of postback handlers
typedef boost::function<void(const std::string&, const PostbackHandlerContinuation&)>
                                                      PostbackHandlerFunction;
core::Error registerPostbackHandler(
                              const std::string& name,
                              const PostbackHandlerFunction& handlerFunction,
                              std::string* pShellCommand); 
                        
// register an rpc method
core::Error registerAsyncRpcMethod(
                              const std::string& name,
                              const core::json::JsonRpcAsyncFunction& function);

// register an rpc method
core::Error registerRpcMethod(const std::string& name,
                              const core::json::JsonRpcFunction& function);


core::Error executeAsync(const core::json::JsonRpcFunction& function,
                         const core::json::JsonRpcRequest& request,
                         core::json::JsonRpcResponse* pResponse);


// create a waitForMethod function -- when called this function will:
//
//   (a) enque the passed event
//   (b) wait for the specified methodName to be returned from the client
//   (c) automatically re-issue the event after a client-init
//
typedef boost::function<bool(core::json::JsonRpcRequest*, const ClientEvent&)> WaitForMethodFunction;
WaitForMethodFunction registerWaitForMethod(const std::string& methodName);

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
   boost::signal<void()>               onDeferredInit;
   boost::signal<void(bool)>           onBackgroundProcessing;
   boost::signal<void(bool)>           onShutdown;
   boost::signal<void ()>              onSysSleep;
};

Events& events();

// launch a child process which can still be interrupted using the
// standard mechanisms
core::Error executeInterruptableChild(const std::string& path,
                                      const std::vector<std::string>& args,
                                      int* pExitStatus);


// ProcessSupervisor
core::system::ProcessSupervisor& processSupervisor();

// schedule incremental work. execute will be called back periodically
// (up to every 25ms if the process is completely idle). if execute
// returns true then it will be called back again, if it returns false
// then it won't ever be called again. in a given period of work the
// execute method will be called multiple times (consecutively) for up
// to the specified incrementalDuration. if you want to implement a
// stateful worker simply create a shared_ptr to your worker object
// and then bind one of its members as the execute parameter. passing
// true as the idleOnly parameter (the default) means that the execute
// function will only be called back during idle time (when the session
// is waiting for user input)
void scheduleIncrementalWork(
         const boost::posix_time::time_duration& incrementalDuration,
         const boost::function<bool()>& execute,
         bool idleOnly = true);

// variation of scheduleIncrementalWork which performs a configurable
// amount of work immediately. this work occurs synchronously with the
// call and will consist of execute being called back repeatedly for
// up to the specified initialDuration
void scheduleIncrementalWork(
         const boost::posix_time::time_duration& initialDuration,
         const boost::posix_time::time_duration& incrementalDuration,
         const boost::function<bool()>& execute,
         bool idleOnly = true);


// schedule work to done every time the specified period elapses.
// if the execute function returns true then the worker will be called
// again after the specified period. pass idleOnly = true to restrict
// periodic work to idle time.
void schedulePeriodicWork(const boost::posix_time::time_duration& period,
                          const boost::function<bool()> &execute,
                          bool idleOnly = true);


core::Error readAndDecodeFile(const core::FilePath& filePath,
                              const std::string& encoding,
                              bool allowSubstChars,
                              std::string* pContents);

core::Error convertToUtf8(const std::string& encodedContent,
                          const std::string& encoding,
                          bool allowSubstChars,
                          std::string* pDecodedContent);

// source R files
core::Error sourceModuleRFile(const std::string& rSourceFile);   
   
// enque client events (note R methods can do this via .rs.enqueClientEvent)
void enqueClientEvent(const ClientEvent& event);

// check whether a directory is currently being monitored by one of our subsystems
bool isDirectoryMonitored(const core::FilePath& directory);

// convenience method for filtering out file listing and changes
bool fileListingFilter(const core::FileInfo& fileInfo);

// enque file changed events
void enqueFileChangedEvent(const core::system::FileChangeEvent& event);
void enqueFileChangedEvents(const core::FilePath& vcsStatusRoot,
                            const std::vector<core::system::FileChangeEvent>& events);


// register a scratch path which is monitored.
typedef boost::function<void(const core::system::FileChangeEvent&)> OnFileChange;
core::FilePath registerMonitoredUserScratchDir(const std::string& dirName,
                                               const OnFileChange& onFileChange);

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

int saveWorkspaceAction();
void syncRSaveAction();

struct VcsContext
{
   std::string detectedVcs;
   std::vector<std::string> applicableVcs;
   std::string svnRepositoryRoot;
   std::string gitRemoteOriginUrl;
};
VcsContext vcsContext(const core::FilePath& workingDir);

std::string normalizeVcsOverride(const std::string& vcsOverride);

core::FilePath shellWorkingDirectory();

std::string generateShortenedUuid();

core::FilePath uniqueFilePath(const core::FilePath& parent,
                              const std::string& prefix = "");

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

