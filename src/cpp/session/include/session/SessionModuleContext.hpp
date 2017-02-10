/*
 * SessionModuleContext.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.

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

#ifndef SESSION_MODULE_CONTEXT_HPP
#define SESSION_MODULE_CONTEXT_HPP

#include <string>

#include <boost/utility.hpp>
#include <boost/function.hpp>
#include <boost/signals.hpp>
#include <boost/shared_ptr.hpp>

#include <core/HtmlUtils.hpp>
#include <core/system/System.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/system/FileChangeEvent.hpp>
#include <core/http/UriHandler.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/r_util/RToolsInfo.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <core/Thread.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionClientEvent.hpp>
#include <session/SessionSourceDatabase.hpp>

namespace rstudio {
namespace core {
   class Error;
   class Success;
   class FilePath;
   class FileInfo;
   class Settings;
   namespace system {
      class ProcessSupervisor;
      struct ProcessResult;
   }
   namespace shell_utils {
      class ShellCommand;
   }
}
}

namespace rstudio {
namespace r {
namespace session {
   struct RSuspendOptions;
}
}
}

namespace rstudio {
namespace session {   

class DistributedEvent;

namespace module_context {

enum PackageCompatStatus
{
   COMPAT_OK      = 0,
   COMPAT_MISSING = 1,
   COMPAT_TOO_OLD = 2,
   COMPAT_TOO_NEW = 3,
   COMPAT_UNKNOWN = 4
};
    
// paths 
core::FilePath userHomePath();
std::string createAliasedPath(const core::FileInfo& fileInfo);
std::string createAliasedPath(const core::FilePath& path);
std::string createFileUrl(const core::FilePath& path);
core::FilePath resolveAliasedPath(const std::string& aliasedPath);
core::FilePath userScratchPath();
core::FilePath scopedScratchPath();
core::FilePath sharedScratchPath();
core::FilePath sessionScratchPath();
core::FilePath oldScopedScratchPath();
bool isVisibleUserFile(const core::FilePath& filePath);

core::FilePath safeCurrentPath();

core::json::Object createFileSystemItem(const core::FileInfo& fileInfo);
core::json::Object createFileSystemItem(const core::FilePath& filePath);
   
// r session info
std::string rVersion();
std::string rHomeDir();

// active sessions
core::r_util::ActiveSession& activeSession();
core::r_util::ActiveSessions& activeSessions();

// get a temp file
core::FilePath tempFile(const std::string& prefix, 
                        const std::string& extension);

core::FilePath tempDir();

// find out the location of a binary
core::FilePath findProgram(const std::string& name);

bool isPdfLatexInstalled();

// is the file a text file
bool isTextFile(const core::FilePath& targetPath);

// find the location of the R script
core::Error rBinDir(core::FilePath* pRBinDirPath);
core::Error rScriptPath(core::FilePath* pRScriptPath);
core::shell_utils::ShellCommand rCmd(const core::FilePath& rBinDir);

// get the R local help port
std::string rLocalHelpPort();

// get current libpaths
std::vector<core::FilePath> getLibPaths();

// is the packages pane disabled
bool disablePackages();

// check if a package is installed
bool isPackageInstalled(const std::string& packageName);

// check if a package is installed with a specific version
bool isPackageVersionInstalled(const std::string& packageName,
                               const std::string& version);

// check if the required versions of various packages are installed
bool isMinimumDevtoolsInstalled();
bool isMinimumRoxygenInstalled();

std::string packageVersion(const std::string& packageName);

bool hasMinimumRVersion(const std::string& version);

// check if a package is installed with a specific version and RStudio protocol
// version (used to allow packages to disable compatibility with older RStudio
// releases)
PackageCompatStatus getPackageCompatStatus(
      const std::string& packageName,
      const std::string& packageVersion,
      int protocolVersion);

core::Error installPackage(const std::string& pkgPath,
                           const std::string& libPath = std::string());

core::Error installEmbeddedPackage(const std::string& name);

// find the package name for a source file
std::string packageNameForSourceFile(const core::FilePath& sourceFilePath);

// is this R or C++ source file part of another (unmonitored) package?
bool isUnmonitoredPackageSourceFile(const core::FilePath& filePath);

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
                        
// register an async rpc method
core::Error registerAsyncRpcMethod(
                              const std::string& name,
                              const core::json::JsonRpcAsyncFunction& function);

// register an idle-only async rpc method
core::Error registerIdleOnlyAsyncRpcMethod(
                              const std::string& name,
                              const core::json::JsonRpcAsyncFunction& function);

// register an rpc method
core::Error registerRpcMethod(const std::string& name,
                              const core::json::JsonRpcFunction& function);

void registerRpcMethod(const core::json::JsonRpcAsyncMethod& method);

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

enum ConsoleOutputType
{
   ConsoleOutputNormal,
   ConsoleOutputError
};

enum ChangeSource
{
   ChangeSourceREPL,
   ChangeSourceRPC,
   ChangeSourceURI
};
   

// custom slot combiner that takes the first non empty value
template<typename T>
struct firstNonEmpty
{
  typedef T result_type;

  template<typename InputIterator>
  T operator()(InputIterator first, InputIterator last) const
  {
     for (InputIterator it = first; it != last; ++it)
     {
        if (!it->empty())
           return *it;
     }
     return T();
  }
};


// session events
struct Events : boost::noncopyable
{
   boost::signal<void (core::json::Object*)> onSessionInfo;
   boost::signal<void ()>                    onClientInit;
   boost::signal<void ()>                    onBeforeExecute;
   boost::signal<void(const std::string&)>   onConsolePrompt;
   boost::signal<void(const std::string&)>   onConsoleInput;
   boost::signal<void(const std::string&, const std::string&)>  
                                             onActiveConsoleChanged;
   boost::signal<void (ConsoleOutputType, const std::string&)>
                                             onConsoleOutput;
   boost::signal<void()>                     onUserInterrupt;
   boost::signal<void (ChangeSource)>        onDetectChanges;
   boost::signal<void (core::FilePath)>      onSourceEditorFileSaved;
   boost::signal<void(bool)>                 onDeferredInit;
   boost::signal<void(bool)>                 afterSessionInitHook;
   boost::signal<void(bool)>                 onBackgroundProcessing;
   boost::signal<void(bool)>                 onShutdown;
   boost::signal<void ()>                    onQuit;
   boost::signal<void (const std::vector<std::string>&)>
                                             onLibPathsChanged;
   boost::signal<void (const std::string&)>  onPackageLoaded;
   boost::signal<void ()>                    onPackageLibraryMutated;
   boost::signal<void ()>                    onPreferencesSaved;
   boost::signal<void (const DistributedEvent&)>
                                             onDistributedEvent;
   boost::signal<void (core::FilePath)>      onPermissionsChanged;

   // signal for detecting extended type of documents
   boost::signal<std::string(boost::shared_ptr<source_database::SourceDocument>),
                 firstNonEmpty<std::string> > onDetectSourceExtendedType;
};

Events& events();

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
                          bool idleOnly = true,
                          bool immediate = true);


// schedule work to be done after a fixed delay
void scheduleDelayedWork(const boost::posix_time::time_duration& period,
                         const boost::function<void()> &execute,
                         bool idleOnly = true);


core::string_utils::LineEnding lineEndings(const core::FilePath& filePath);

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
core::Error sourceModuleRFileWithResult(const std::string& rSourceFile,
                                        const core::FilePath& workingDir,
                                        core::system::ProcessResult* pResult);
   
// enque client events (note R methods can do this via .rs.enqueClientEvent)
void enqueClientEvent(const ClientEvent& event);

// check whether a directory is currently being monitored by one of our subsystems
bool isDirectoryMonitored(const core::FilePath& directory);

// check whether an R source file belongs to the package under development
bool isRScriptInPackageBuildTarget(const core::FilePath& filePath);

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

std::string resourceFileAsString(const std::string& fileName);

bool portmapPathForLocalhostUrl(const std::string& url, std::string* pPath);

std::string mapUrlPorts(const std::string& url);

std::string pathRelativeTo(const core::FilePath& sourcePath,
                           const core::FilePath& targetPath);

void activatePane(const std::string& pane);

int saveWorkspaceAction();
void syncRSaveAction();

std::string libPathsString();
bool canBuildCpp();
bool installRBuildTools(const std::string& action);
bool haveRcppAttributes();
bool isRtoolsCompatible(const core::r_util::RToolsInfo& rTools);
bool addRtoolsToPathIfNecessary(std::string* pPath,
                                std::string* pWarningMessage);
bool addRtoolsToPathIfNecessary(core::system::Options* pEnvironment,
                                std::string* pWarningMessage);

#ifdef __APPLE__
bool isOSXMavericks();
bool hasOSXMavericksDeveloperTools();
core::Error copyImageToCocoaPasteboard(const core::FilePath& filePath);
#else
inline bool isOSXMavericks()
{
   return false;
}
inline bool hasOSXMavericksDeveloperTools()
{
   return false;
}
inline core::Error copyImageToCocoaPasteboard(const core::FilePath& filePath)
{
   return core::systemError(boost::system::errc::not_supported, ERROR_LOCATION);
}
#endif

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

// persist state accross suspend and resume
   
typedef boost::function<void (const r::session::RSuspendOptions&,
                              core::Settings*)> SuspendFunction;
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

bool rSessionResumed();

const int kCompileOutputCommand = 0;
const int kCompileOutputNormal = 1;
const int kCompileOutputError = 2;

struct CompileOutput
{
   CompileOutput(int type, const std::string& output)
      : type(type), output(output)
   {
   }

   int type;
   std::string output;
};

core::json::Object compileOutputAsJson(const CompileOutput& compileOutput);


std::string previousRpubsUploadId(const core::FilePath& filePath);

std::string CRANReposURL();

std::string rstudioCRANReposURL();

std::string downloadFileMethod(const std::string& defaultMethod = "");

std::string CRANDownloadOptions();

bool haveSecureDownloadFileMethod();

void reconcileSecureDownloadConfiguration();

struct UserPrompt
{
   enum Type { Info = 0, Warning = 1, Error = 2, Question = 3 };
   enum Response { ResponseYes = 0, ResponseNo = 1, ResponseCancel = 2 };

   UserPrompt(int type,
              const std::string& caption,
              const std::string& message,
              bool includeCancel = false)
   {
      commonInit(type, caption, message, "", "", includeCancel, true);
   }

   UserPrompt(int type,
              const std::string& caption,
              const std::string& message,
              bool includeCancel,
              bool yesIsDefault)
   {
      commonInit(type, caption, message, "", "", includeCancel, yesIsDefault);
   }

   UserPrompt(int type,
              const std::string& caption,
              const std::string& message,
              const std::string& yesLabel,
              const std::string& noLabel,
              bool includeCancel,
              bool yesIsDefault)
   {
      commonInit(type,
                 caption,
                 message,
                 yesLabel,
                 noLabel,
                 includeCancel,
                 yesIsDefault);
   }

   int type ;
   std::string caption;
   std::string message;
   std::string yesLabel;
   std::string noLabel;
   bool includeCancel;
   bool yesIsDefault;

private:
   void commonInit(int type,
                   const std::string& caption,
                   const std::string& message,
                   const std::string& yesLabel,
                   const std::string& noLabel,
                   bool includeCancel,
                   bool yesIsDefault)
   {
      this->type = type;
      this->caption = caption;
      this->message = message;
      this->yesLabel = yesLabel;
      this->noLabel = noLabel;
      this->includeCancel = includeCancel;
      this->yesIsDefault = yesIsDefault;
   }
};

UserPrompt::Response showUserPrompt(const UserPrompt& userPrompt);

struct PackratContext
{
   PackratContext() :
      available(false),
      applicable(false),
      packified(false),
      modeOn(false)
   {
   }

   bool available;
   bool applicable;
   bool packified;
   bool modeOn;
};

bool isRequiredPackratInstalled();

PackratContext packratContext();
core::json::Object packratContextAsJson();

core::json::Object packratOptionsAsJson();

// R command invocation -- has two representations, one to be submitted
// (shellCmd_) and one to show the user (cmdString_)
class RCommand
{
public:
   explicit RCommand(const core::FilePath& rBinDir)
      : shellCmd_(buildRCmd(rBinDir))
   {
#ifdef _WIN32
      cmdString_ = "Rcmd.exe";
#else
      cmdString_ = "R CMD";
#endif

      // set escape mode to files-only. this is so that when we
      // add the group of extra arguments from the user that we
      // don't put quotes around it.
      shellCmd_ << core::shell_utils::EscapeFilesOnly;
   }

   RCommand& operator<<(const std::string& arg)
   {
      if (!arg.empty())
      {
         cmdString_ += " " + arg;
         shellCmd_ << arg;
      }
      return *this;
   }

   RCommand& operator<<(const core::FilePath& arg)
   {
      cmdString_ += " " + arg.absolutePath();
      shellCmd_ << arg;
      return *this;
   }


   const std::string& commandString() const
   {
      return cmdString_;
   }

   const core::shell_utils::ShellCommand& shellCommand() const
   {
      return shellCmd_;
   }

private:
   static core::shell_utils::ShellCommand buildRCmd(
                                 const core::FilePath& rBinDir);

private:
   std::string cmdString_;
   core::shell_utils::ShellCommand shellCmd_;
};


class ViewerHistoryEntry
{
public:
   ViewerHistoryEntry() {}
   explicit ViewerHistoryEntry(const std::string& sessionTempPath)
      : sessionTempPath_(sessionTempPath)
   {
   }

   bool empty() const { return sessionTempPath_.empty(); }

   std::string url() const;

   const std::string& sessionTempPath() const { return sessionTempPath_; }

   core::Error copy(const core::FilePath& sourceDir,
                    const core::FilePath& destinationDir) const;

private:
   std::string sessionTempPath_;
};

void addViewerHistoryEntry(const ViewerHistoryEntry& entry);

core::Error recursiveCopyDirectory(const core::FilePath& fromDir,
                                   const core::FilePath& toDir);

std::string sessionTempDirUrl(const std::string& sessionTempPath);

core::Error uniqueSaveStem(const core::FilePath& directoryPath,
                           const std::string& base,
                           std::string* pStem);

core::json::Object plotExportFormat(const std::string& name,
                                    const std::string& extension);


core::Error createSelfContainedHtml(const core::FilePath& sourceFilePath,
                                    const core::FilePath& targetFilePath);

bool isUserFile(const core::FilePath& filePath);


struct SourceMarker
{
   enum Type {
      Error = 0, Warning = 1, Box = 2, Info = 3, Style = 4, Usage = 5
   };

   SourceMarker(Type type,
                const core::FilePath& path,
                int line,
                int column,
                const core::html_utils::HTML& message,
                bool showErrorList)
      : type(type), path(path), line(line), column(column), message(message),
        showErrorList(showErrorList)
   {
   }

   Type type;
   core::FilePath path;
   int line;
   int column;
   core::html_utils::HTML message;
   bool showErrorList;
};

SourceMarker::Type sourceMarkerTypeFromString(const std::string& type);

core::json::Array sourceMarkersAsJson(const std::vector<SourceMarker>& markers);

struct SourceMarkerSet
{  
   SourceMarkerSet() {}

   SourceMarkerSet(const std::string& name,
                   const std::vector<SourceMarker>& markers)
      : name(name),
        markers(markers)
   {
   }

   SourceMarkerSet(const std::string& name,
                   const core::FilePath& basePath,
                   const std::vector<SourceMarker>& markers)
      : name(name),
        basePath(basePath),
        markers(markers)
   {
   }

   bool empty() const { return name.empty(); }

   std::string name;
   core::FilePath basePath;
   std::vector<SourceMarker> markers;
};

enum MarkerAutoSelect
{
   MarkerAutoSelectNone = 0,
   MarkerAutoSelectFirst = 1,
   MarkerAutoSelectFirstError = 2
};

void showSourceMarkers(const SourceMarkerSet& markerSet,
                       MarkerAutoSelect autoSelect);


bool isLoadBalanced();

bool usingMingwGcc49();

bool isWebsiteProject();
bool isBookdownWebsite();
std::string websiteOutputDir();

core::FilePath extractOutputFileCreated(const core::FilePath& inputFile,
                                        const std::string& output);

void onBackgroundProcessing(bool isIdle);

} // namespace module_context
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULE_CONTEXT_HPP

