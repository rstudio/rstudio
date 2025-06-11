/*
 * SessionModuleContext.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/utility.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/BoostSignals.hpp>
#include <core/HtmlUtils.hpp>
#include <core/Thread.hpp>
#include <core/Version.hpp>
#include <core/http/UriHandler.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <core/r_util/RToolsInfo.hpp>
#include <core/system/Environment.hpp>
#include <core/system/FileChangeEvent.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/system/System.hpp>

#include <session/SessionClientEvent.hpp>
#include <session/SessionConsoleOutput.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionSourceDatabase.hpp>

#include "../SessionClientEventQueue.hpp"

using namespace boost::placeholders;

namespace rstudio {
namespace core {
   class DistributedEvent;
   class Error;
   class Success;
   class FilePath;
   class FileInfo;
   class Settings;
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

ClientEventQueue& clientEventQueue();

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
core::FilePath userUploadedFilesScratchPath();
core::FilePath scopedScratchPath();
core::FilePath sharedScratchPath();
core::FilePath sharedProjectScratchPath();
core::FilePath sessionScratchPath();
core::FilePath oldScopedScratchPath();
bool isVisibleUserFile(const core::FilePath& filePath);

core::FilePath safeCurrentPath();

core::json::Object createFileSystemItem(const core::FileInfo& fileInfo);
core::json::Object createFileSystemItem(const core::FilePath& filePath);

// postback helpers
core::FilePath rPostbackPath();
core::FilePath rPostbackScriptsDir();
core::FilePath rPostbackScriptsPath(const std::string& scriptName);
   
// r session info
std::string rVersion();
std::string rVersionLabel();
std::string rHomeDir();
std::string rVersionModule();

// active sessions
core::r_util::ActiveSession& activeSession();
core::r_util::ActiveSessions& activeSessions();

// get a temp file
core::FilePath tempFile(const std::string& prefix, 
                        const std::string& extension);

core::FilePath tempDir();

std::string rLibsUser();

// find out the location of a binary
core::FilePath findProgram(const std::string& name);

bool addTinytexToPathIfNecessary();
bool isPdfLatexInstalled();

// is the file a text file
bool isTextFile(const core::FilePath& targetPath);

// edit a file
void editFile(const core::FilePath& targetPath, int lineNumber = -1);

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
core::Error packageVersion(const std::string& packageName,
                           core::Version* pVersion);

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

// register an inbound upload handler (include a leading slash)
core::Error registerUploadHandler(const std::string& name,
                                  const core::http::UriAsyncUploadHandlerFunction& handlerFunction);

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
   
struct DocumentDiff
{
   // This is a chunk of text inserted into the specified document.
   // It replaces the subrange [offset, offset+length).
   explicit DocumentDiff(std::string docId,
                         std::string replacement,
                         int offset,
                         int length)
      : docId(std::move(docId)),
        replacement(std::move(replacement)),
        offset(offset),
        length(length)
   {
   }

   std::string toString() const
   {
      return "DocumentDiff(docId=" + docId +
             ", replacement=[" + replacement + "]"
             ", offset=" + std::to_string(offset) +
             ", length=" + std::to_string(length) + ")";
   }

   std::string docId;
   std::string replacement;
   int offset, length;
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
   // NOTE: The onConsoleOutput signal is potentially not fired from the main thread!!
   RSTUDIO_BOOST_SIGNAL<void()>                                       onBeforeClientInit;
   RSTUDIO_BOOST_SIGNAL<void(core::json::Object*)>                    onSessionInfo;
   RSTUDIO_BOOST_SIGNAL<void()>                                       onClientInit;
   RSTUDIO_BOOST_SIGNAL<void()>                                       onInitComplete;
   RSTUDIO_BOOST_SIGNAL<void(bool)>                                   onDeferredInit;
   RSTUDIO_BOOST_SIGNAL<void(bool)>                                   afterSessionInitHook;
   RSTUDIO_BOOST_SIGNAL<void()>                                       onBeforeExecute;
   RSTUDIO_BOOST_SIGNAL<void(const std::string&)>                     onConsolePrompt;
   RSTUDIO_BOOST_SIGNAL<void(const std::string&)>                     onConsoleInput;
   RSTUDIO_BOOST_SIGNAL<void(bool)>                                   onBusy;
   RSTUDIO_BOOST_SIGNAL<void(const std::string&, const std::string&)> onActiveConsoleChanged;
   RSTUDIO_BOOST_SIGNAL<void(ConsoleOutputType, const std::string&)>  onConsoleOutput;
   RSTUDIO_BOOST_SIGNAL<void()>                                       onUserInterrupt;
   RSTUDIO_BOOST_SIGNAL<void(ChangeSource)>                           onDetectChanges;
   RSTUDIO_BOOST_SIGNAL<void(core::FilePath)>                         onSourceEditorFileSaved;
   RSTUDIO_BOOST_SIGNAL<void(DocumentDiff)>                           onSourceFileDiff;
   RSTUDIO_BOOST_SIGNAL<void(bool)>                                   onBackgroundProcessing;
   RSTUDIO_BOOST_SIGNAL<void(const std::vector<std::string>&)>        onLibPathsChanged;
   RSTUDIO_BOOST_SIGNAL<void(const std::string&)>                     onPackageLoaded;
   RSTUDIO_BOOST_SIGNAL<void()>                                       onPackageLibraryMutated;
   RSTUDIO_BOOST_SIGNAL<void()>                                       onPreferencesSaved;
   RSTUDIO_BOOST_SIGNAL<void()>                                       onProjectConfigUpdated;
   RSTUDIO_BOOST_SIGNAL<void()>                                       onProjectOptionsUpdated;
   RSTUDIO_BOOST_SIGNAL<void(const core::DistributedEvent&)>          onDistributedEvent;
   RSTUDIO_BOOST_SIGNAL<void(core::FilePath)>                         onPermissionsChanged;
   RSTUDIO_BOOST_SIGNAL<void(bool)>                                   onShutdown;
   RSTUDIO_BOOST_SIGNAL<void()>                                       onQuit;
   RSTUDIO_BOOST_SIGNAL<void()>                                       onDestroyed;

   // signal for detecting extended type of documents
   RSTUDIO_BOOST_SIGNAL<std::string(boost::shared_ptr<source_database::SourceDocument>),
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

void executeOnMainThread(const boost::function<void()> &execute);

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
bool fileListingFilter(const core::FileInfo& fileInfo, bool hideObjectFiles);

// enque file changed events
void enqueFileChangedEvent(const core::system::FileChangeEvent& event);
void enqueFileChangedEvents(const core::FilePath& vcsStatusRoot,
                            const std::vector<core::system::FileChangeEvent>& events);


// register a scratch path which is monitored.
typedef boost::function<void(const core::system::FileChangeEvent&)> OnFileChange;
core::FilePath registerMonitoredUserScratchDir(const std::string& dirName,
                                               const OnFileChange& onFileChange);

// enqueue new console input
core::Error enqueueConsoleInput(const std::string& input);

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

bool isMacOS();
bool hasMacOSDeveloperTools();
bool hasMacOSCommandLineTools();
void checkXcodeLicense();

#ifdef __APPLE__
core::Error copyImageToCocoaPasteboard(const core::FilePath& filePath);
#else
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

// persist state across suspend and resume
   
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

   int type;
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

// implemented in SessionPackrat.cpp
bool isRequiredPackratInstalled();
PackratContext packratContext();
core::json::Object packratContextAsJson();
core::json::Object packratOptionsAsJson();

// implemented in SessionRenv.cpp
bool isRequiredRenvInstalled();
bool isRenvActive();
core::json::Value renvContextAsJson();
core::json::Value renvOptionsAsJson();

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
      cmdString_ += " " + arg.getAbsolutePath();
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

struct QuartoNavigate
{
   QuartoNavigate()
      : website(false)
   {}
   
   bool empty() const
   {
      return !website && source.empty();
   }
   
   static QuartoNavigate navigate(
         const std::string& source,
         const std::string& output,
         const std::string& jobId,
         bool isWebsite)
   {
      QuartoNavigate nav;
      nav.source = source;
      nav.output = output;
      nav.job_id = jobId;
      nav.website = isWebsite;
      return nav;
   }
   
   std::string source;
   std::string output;
   std::string job_id;
   bool website;
};

core::json::Value quartoNavigateAsJson(const QuartoNavigate& quartoNav);


void viewer(const std::string& url,
            int height = 0, // pass 0 for no height change, // pass -1 for maximize
            const QuartoNavigate& quartoNav = QuartoNavigate());

void clearViewerCurrentUrl();
std::string viewerCurrentUrl(bool mapped = true);

core::Error recursiveCopyDirectory(const core::FilePath& fromDir,
                                   const core::FilePath& toDir);

bool isSessionTempPath(core::FilePath filePath);

std::string sessionTempDirUrl(const std::string& sessionTempPath);

core::Error uniqueSaveStem(const core::FilePath& directoryPath,
                           const std::string& base,
                           std::string* pStem);

core::Error uniqueSaveStem(const core::FilePath& directoryPath,
                           const std::string& base,
                           const std::string& delimiter,
                           std::string* pStem);

core::json::Object plotExportFormat(const std::string& name,
                                    const std::string& extension);


core::Error createSelfContainedHtml(const core::FilePath& sourceFilePath,
                                    const core::FilePath& targetFilePath);

bool isUserFile(const core::FilePath& filePath);


struct SourceMarker
{
   enum Type {
      Error   = 0,
      Warning = 1,
      Box     = 2,
      Info    = 3,
      Style   = 4, 
      Usage   = 5,
      Empty   = 99
   };

   SourceMarker()
      : type(Empty),
      isCustom(false)
   {
   }

   SourceMarker(Type type,
                const core::FilePath& path,
                int line,
                int column,
                const core::html_utils::HTML& message,
                bool showErrorList)
      : type(type),
        path(path),
        line(line),
        column(column),
        message(message),
        showErrorList(showErrorList),
        isCustom(false)
   {
   }

   SourceMarker(Type type,
                const core::FilePath& path,
                int line,
                int column,
                const core::html_utils::HTML& message,
                bool showErrorList,
                bool isCustom)
      : type(type),
        path(path),
        line(line),
        column(column),
        message(message),
        showErrorList(showErrorList),
        isCustom(isCustom)
   {
   }
   
   explicit operator bool() const
   {
      return type != Empty;
   }

   Type type;
   core::FilePath path;
   int line;
   int column;
   core::html_utils::HTML message;
   bool showErrorList;
   bool isCustom;
};

SourceMarker::Type sourceMarkerTypeFromString(const std::string& type);

core::json::Array sourceMarkersAsJson(const std::vector<SourceMarker>& markers);

struct SourceMarkerSet
{  
   SourceMarkerSet() {}

   SourceMarkerSet(const std::string& name,
                   const std::vector<SourceMarker>& markers)
      : name(name),
        markers(markers),
        isDiagnostics(false)
   {
   }

   SourceMarkerSet(const std::string& name,
                   const std::vector<SourceMarker>& markers,
                   bool isDiagnostics)
      : name(name),
        markers(markers),
        isDiagnostics(isDiagnostics)
   {
   }

   SourceMarkerSet(const std::string& name,
                   const core::FilePath& basePath,
                   const std::vector<SourceMarker>& markers)
      : name(name),
        basePath(basePath),
        markers(markers),
        isDiagnostics(false)
   {
   }

   SourceMarkerSet(const std::string& name,
                   const core::FilePath& basePath,
                   const std::vector<SourceMarker>& markers,
                   bool isDiagnostics)
      : name(name),
        basePath(basePath),
        markers(markers),
        isDiagnostics(isDiagnostics)
   {
   }

   bool empty() const { return name.empty(); }

   std::string name;
   core::FilePath basePath;
   std::vector<SourceMarker> markers;
   bool isDiagnostics;
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

bool isWebsiteProject();
bool isBookdownWebsite();
bool isBookdownProject();
bool isBlogdownProject();
bool isDistillProject();
std::string websiteOutputDir();
std::vector<core::FilePath> bookdownBibliographies();
std::vector<std::string> bookdownBibliographiesRelative();
std::vector<std::string> bookdownZoteroCollections();
core::json::Value bookdownXRefIndex();
core::FilePath bookdownCSL();

core::FilePath extractOutputFileCreated(const core::FilePath& inputDir,
                                        const std::string& output,
                                        bool ignoreHugo = true);

bool isPathViewAllowed(const core::FilePath& path);

void onBackgroundProcessing(bool isIdle);

void initializeConsoleCtrlHandler();

bool isPythonReplActive();


core::Error perFilePathStorage(const std::string& scope,
                               const core::FilePath& filePath,
                               bool directory,
                               core::FilePath* pStorage);

// returns -1 if no error was found in the output
int jupyterErrorLineNumber(const std::vector<std::string>& srcLines,
                           const std::string& output);
bool navigateToRenderPreviewError(const core::FilePath& previewFile,
                                  const std::vector<std::string>& previewFileLines,
                                  const std::string& output,
                                  const std::string& allOutput);

std::vector<core::FilePath> ignoreContentDirs();
bool isIgnoredContent(const core::FilePath& filePath, const std::vector<core::FilePath>& ignoreDirs);

std::string getActiveLanguage();
core::Error adaptToLanguage(const std::string& language);

// paths to pandoc and pandoc-citeproc suitable for passing to the shell
// (string_utils::utf8ToSystem has been called on them)
std::string pandocPath();

core::Error runPandoc(const std::string& pandocPath,
                      const std::vector<std::string>& args,
                      const std::string& input,
                      core::system::ProcessOptions options,
                      core::system::ProcessResult* pResult);
core::Error runPandoc(const std::vector<std::string>& args,
                      const std::string& input,
                      core::system::ProcessResult* pResult);

core::Error runPandocAsync(const std::string& pandocPath,
                           const std::vector<std::string>& args,
                           const std::string&input,
                           core::system::ProcessOptions options,
                           const boost::function<void(const core::system::ProcessResult&)>& onCompleted);
core::Error runPandocAsync(const std::vector<std::string>& args,
                           const std::string& input,
                           const boost::function<void(const core::system::ProcessResult&)>& onCompleted);

core::Error runPandocCiteproc(const std::vector<std::string>& args, core::system::ProcessResult* pResult);

core::Error runPandocCiteprocAsync(const std::vector<std::string>& args,
                                   const boost::function<void(const core::system::ProcessResult&)>& onCompleted);

core::Error sendSessionRequest(const std::string& uri,
                               const std::string& body,
                               core::http::Response* pResponse);

} // namespace module_context
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULE_CONTEXT_HPP

