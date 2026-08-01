/*
 * Win32System.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <core/system/System.hpp>

#include <stdio.h>
#include <stdlib.h>
#include <io.h>

#include <iostream>
#include <sstream>
#include <vector>
#include <algorithm>

#include <windows.h>
#include <shlobj.h>
#include <shlwapi.h>
#include <tlhelp32.h>
#include <VersionHelpers.h>

#include <boost/system/windows_error.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/range.hpp>
#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/bind/bind.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/system/User.hpp>

#include <core/Algorithm.hpp>
#include <core/Log.hpp>
#include <core/FileInfo.hpp>
#include <core/DateTime.hpp>
#include <core/FileUtils.hpp>
#include <core/StringUtils.hpp>
#include <core/system/Environment.hpp>

#ifndef JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
#define JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE 0x2000
#endif
#ifndef JOB_OBJECT_LIMIT_BREAKAWAY_OK
#define JOB_OBJECT_LIMIT_BREAKAWAY_OK 0x00000800
#endif

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace system {

namespace {

// Upper bound on a Windows path once the MAX_PATH limit is lifted (see the
// longPathAware entry in our exe manifests). Used to bound grow-and-retry
// loops around Win32 APIs that report the buffer size they need.
const std::size_t kMaxPathLength = 32767;

// SHGetKnownFolderPath allocates its result, so unlike the deprecated
// MAX_PATH-bound SHGetFolderPathW it imposes no path length limit. The cost is
// that the buffer has to be released with CoTaskMemFree.
Error knownFolderPath(REFKNOWNFOLDERID folderId,
                      DWORD flags,
                      const std::string& description,
                      FilePath* pPath)
{
   wchar_t* path = nullptr;
   HRESULT hr = ::SHGetKnownFolderPath(folderId, flags, nullptr, &path);

   if (FAILED(hr) || path == nullptr)
   {
      if (path != nullptr)
         ::CoTaskMemFree(path);

      Error error = systemError(boost::system::errc::no_such_file_or_directory,
                                description,
                                ERROR_LOCATION);
      error.addProperty("error-code", safe_convert::numberToHexString(hr));
      return error;
   }

   *pPath = FilePath(std::wstring(path));
   ::CoTaskMemFree(path);

   return Success();
}

// Resolves a Windows system directory. GetSystemDirectoryW and
// GetWindowsDirectoryW return the length copied on success, or the size
// required (including the null terminator) when the buffer was too small.
FilePath win32Directory(UINT (WINAPI* getDirectory)(LPWSTR, UINT))
{
   std::vector<wchar_t> buffer(MAX_PATH);
   for (;;)
   {
      UINT length = getDirectory(&buffer[0], static_cast<UINT>(buffer.size()));
      if (length == 0)
      {
         LOG_ERROR(LAST_SYSTEM_ERROR());
         return FilePath();
      }

      if (length < buffer.size())
         return FilePath(std::wstring(&buffer[0], length));

      if (buffer.size() > kMaxPathLength)
      {
         LOG_ERROR_MESSAGE("System directory path exceeds " +
                           safe_convert::numberToString(kMaxPathLength) + " characters");
         return FilePath();
      }

      // a too-small buffer reports the size it needs, including the null, so length is
      // already larger than the current size -- but don't lean on the API for
      // termination: grow by at least one element, then clamp to the longest path
      // Windows can express. The size check above turns the clamped attempt into the
      // last one, so the loop always ends.
      std::size_t required = std::max(static_cast<std::size_t>(length), buffer.size() + 1);
      buffer.resize(std::min(required, kMaxPathLength + 1));
   }
}

Error initJobObject(bool* detachFromJob)
{
   /*
    * Create a Job object and assign this process to it. This will
    * cause all child processes to be assigned to the same job.
    * With JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE set, all the child
    * processes will be killed when this process terminates (since
    * it is the only one holding a handle to the job). With
    * JOB_OBJECT_LIMIT_BREAKAWAY_OK set it is possible to pass
    * CREATE_BREAKAWAY_FROM_JOB to CreateProcess (this is required
    * by Chrome for creating its sub-processes)
    */

   // If detachFromJob is true, it means we need to relaunch this
   // executable with CREATE_BREAKAWAY_FROM_JOB
   *detachFromJob = false;

   HANDLE hJob = ::CreateJobObject(nullptr, nullptr);
   if (!hJob)
   {
      return LAST_SYSTEM_ERROR();
   }

   JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli = { 0 };
   jeli.BasicLimitInformation.LimitFlags =
         JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE |
         JOB_OBJECT_LIMIT_BREAKAWAY_OK |
         JOB_OBJECT_LIMIT_DIE_ON_UNHANDLED_EXCEPTION;

   // apply the limits before assigning ourselves: a job without
   // KILL_ON_JOB_CLOSE reaps nothing on exit, and one without BREAKAWAY_OK
   // would prevent children (like Chrome) from breaking away, so joining a
   // half-configured job would be worse than running without one
   BOOL configured = ::SetInformationJobObject(hJob,
                                               JobObjectExtendedLimitInformation,
                                               &jeli,
                                               sizeof(jeli));
   if (!configured)
   {
      Error error = LAST_SYSTEM_ERROR();
      ::CloseHandle(hJob);
      return error;
   }

   // AssignProcessToJobObject returns nonzero on success
   BOOL assigned = ::AssignProcessToJobObject(hJob, ::GetCurrentProcess());
   if (!assigned)
   {
      auto lastErr = ::GetLastError();

      if (lastErr == ERROR_ACCESS_DENIED)
      {
         // Use an environment variable to prevent us from somehow
         // getting into an infinite loop of detaching (which would
         // otherwise occur if ERROR_ACCESS_DENIED is being returned
         // for some reason other than an existing job object being
         // attached). This works because environment variables are
         // inherited by our job-detached child process.
         if (getenv("_RSTUDIO_LEVEL").empty())
         {
            setenv("_RSTUDIO_LEVEL", "1");
            *detachFromJob = true;
         }
      }

      // the job never gained a member, so this just destroys it
      ::CloseHandle(hJob);
      return systemError(lastErr, ERROR_LOCATION);
   }

   // NOTE: hJob is deliberately left open. This process must hold the only
   // handle to the job for its whole lifetime so that KILL_ON_JOB_CLOSE
   // fires (via the handle closing) when the process exits.
   return Success();
}

bool isHiddenFile(const std::string& path)
{
   DWORD attribs = ::GetFileAttributesA(path.c_str());
   if (attribs == INVALID_FILE_ATTRIBUTES)
      return false;
   else if (attribs & FILE_ATTRIBUTE_HIDDEN)
      return true;
   else
      return false;
}

} // anonymous namespace

void initHook()
{
   // Logging will NOT work in this function!!

   bool detachFromJob;
   Error error = initJobObject(&detachFromJob);
   if (!detachFromJob)
      return;

   // NOTE: we used to fetch our own module path here as a sanity check, but the
   // relaunch below re-uses GetCommandLine() and never consumed it -- so it was
   // only a MAX_PATH buffer waiting to truncate under a long install path

   STARTUPINFO startupInfo;
   memset(&startupInfo, 0, sizeof(startupInfo));
   startupInfo.cb = sizeof(startupInfo);
   PROCESS_INFORMATION procInfo;
   memset(&procInfo, 0, sizeof(procInfo));

   if (!::CreateProcess(nullptr,
                        ::GetCommandLine(),
                        nullptr,
                        nullptr,
                        TRUE,
                        CREATE_BREAKAWAY_FROM_JOB | ::GetPriorityClass(::GetCurrentProcess()),
                        nullptr,
                        nullptr,
                        &startupInfo,
                        &procInfo))
   {
      return;  // Couldn't execute
   }

   ::AllowSetForegroundWindow(procInfo.dwProcessId);
   ::WaitForSingleObject(procInfo.hProcess, INFINITE);

   DWORD exitCode;
   if (!::GetExitCodeProcess(procInfo.hProcess, &exitCode))
      exitCode = ::GetLastError();

   ::CloseHandle(procInfo.hProcess);
   ::CloseHandle(procInfo.hThread);

   ::ExitProcess(exitCode);
}

Error initializeSystemLog(const std::string& programIdentity,
                          log::LogLevel logLevel,
                          bool enableConfigReload)
{
   return Success();
}

void initializeLogConfigReload()
{
}

Error findProgramOnPath(const std::string& program,
                        core::FilePath* pProgramPath)
{
   // nothing to search for; bail before probing extensions of "" against every
   // search directory
   if (program.empty())
      return fileNotFoundError(program, ERROR_LOCATION);

   // if the program supplied already has an extension,
   // then we'll skip searching any custom extensions.
   //
   // getExtension() is a bare rfind('.') with no separator awareness, so take the last
   // path component first. Otherwise a dotted *directory* reads as an extension and the
   // probing below is skipped for a name that has none -- and dotted directories are the
   // norm on Windows ("C:/R/R-4.4.1/bin/x64/R", "C:/Users/first.last/miktex/bin/pdflatex").
   std::string::size_type nameStart = program.find_last_of("/\\:");
   std::string programName = nameStart == std::string::npos
      ? program
      : program.substr(nameStart + 1);

   auto programExt = core::string_utils::getExtension(programName);

   auto defaultExts = { ".exe", ".com", ".bat", ".cmd" };
   auto noExts = { "" };
   auto exts = programExt.empty() ? defaultExts : noExts;

   // A path-qualified name isn't a PATH search; resolve it directly. It still gets the
   // extension probing a bare name would, so that "C:/tools/git" finds git.exe rather
   // than only an extensionless file of that exact name. ':' counts here because a
   // drive-relative name like "C:git.exe" is rooted as far as FilePath is concerned,
   // and completeChildPath would reject (and log) it below.
   if (program.find_first_of("/\\:") != std::string::npos)
   {
      // the name exactly as given first, then the extensions a bare name would get
      std::vector<std::string> candidateExts = { std::string() };
      if (programExt.empty())
         candidateExts.insert(candidateExts.end(), defaultExts.begin(), defaultExts.end());

      for (auto&& ext : candidateExts)
      {
         FilePath programPath(program + ext);
         if (!programPath.isRegularFile())
            continue;

         *pProgramPath = programPath;
         return Success();
      }

      return fileNotFoundError(program, ERROR_LOCATION);
   }

   // Search the Windows system directories ahead of PATH. PathFindOnPath, which this
   // replaces, is documented to search "standard directories such as System32 and the
   // directories specified in the PATH environment variable", so keeping the system
   // directories first preserves the part of its behavior we depend on: a stray cmd.exe
   // earlier on PATH cannot shadow the one in System32, which is how we launch batch
   // files. We do not search the current directory -- that is a deliberate hardening
   // against binary planting, not a continuation of the old behavior.
   //
   // We can no longer delegate to PathFindOnPath because it writes at most MAX_PATH
   // characters no matter how large a buffer it's given, so it cannot see a program
   // under a long path.
   std::vector<std::string> paths;
   for (auto&& systemDirectory : { win32Directory(&::GetSystemDirectoryW),
                                   win32Directory(&::GetWindowsDirectoryW) })
   {
      if (!systemDirectory.isEmpty())
         paths.push_back(systemDirectory.getAbsolutePath());
   }

   std::string pathEnv = core::system::getenv("PATH");
   auto pathEntries = core::algorithm::split(pathEnv, ";");
   paths.insert(paths.end(), pathEntries.begin(), pathEntries.end());

   for (auto&& path : paths)
   {
      // PATH entries are sometimes quoted, and a quote is never part of the path
      std::string directory = boost::algorithm::trim_copy(path);
      boost::algorithm::trim_if(directory, boost::algorithm::is_any_of("\""));
      if (directory.empty())
         continue;

      // Skip non-existent directories to avoid logging errors, matching the behavior of the
      // Posix implementation
      FilePath pathDir(directory);
      if (!pathDir.exists())
         continue;

      for (auto&& ext : exts)
      {
         // isRegularFile rather than exists, so a directory named e.g. "git.exe"
         // sitting on PATH isn't mistaken for the program
         FilePath programPath = pathDir.completeChildPath(program + ext);
         if (!programPath.isRegularFile())
            continue;

         *pProgramPath = programPath;
         return Success();
      }
   }

   return fileNotFoundError(program, ERROR_LOCATION);
}

bool isWin64()
{
   return !getenv("PROCESSOR_ARCHITEW6432").empty()
      || getenv("PROCESSOR_ARCHITECTURE") == "AMD64";
}

bool isCurrentProcessWin64()
{
   return getenv("PROCESSOR_ARCHITECTURE") == "AMD64";
}

bool isWin7OrLater()
{
   return IsWindows7OrGreater();
}

std::string username()
{
   return system::getenv("USERNAME");
}

unsigned int effectiveUserId()
{
   return 0; // no concept of this on Win32
}

bool effectiveUserIsRoot()
{
   // on Windows, treat built-in administrator account, or elevation to it, to be the
   // equivalent of Posix "root"

   HANDLE hProcessToken = nullptr;
   if (!OpenProcessToken(::GetCurrentProcess(), TOKEN_QUERY, &hProcessToken))
   {
      auto lastErr = ::GetLastError();
      LOG_ERROR(systemError(lastErr, ERROR_LOCATION));
      return false;
   }
   core::system::CloseHandleOnExitScope processTokenScope(&hProcessToken, ERROR_LOCATION);

   bool isAdmin = false;
   DWORD bytesUsed = 0;
   TOKEN_ELEVATION_TYPE tokenElevationType;
   if (!::GetTokenInformation(hProcessToken, TokenElevationType, &tokenElevationType,
                              sizeof(tokenElevationType), &bytesUsed))
   {
      auto lastErr = ::GetLastError();
      LOG_ERROR(systemError(lastErr, ERROR_LOCATION));
      return false;
   }

   if (tokenElevationType == TokenElevationTypeLimited)
   {
      HANDLE hUnfiltered;
      if (!::GetTokenInformation(hProcessToken, TokenLinkedToken, &hUnfiltered, sizeof(HANDLE), &bytesUsed))
      {
         auto lastErr = ::GetLastError();
         LOG_ERROR(systemError(lastErr, ERROR_LOCATION));
         return false;
      }
      core::system::CloseHandleOnExitScope unfilteredHandle(&hUnfiltered, ERROR_LOCATION);

      BYTE adminSID[SECURITY_MAX_SID_SIZE];
      DWORD sidSize = sizeof(adminSID);

      if (!::CreateWellKnownSid(WinBuiltinAdministratorsSid, 0, &adminSID, &sidSize))
      {
         auto lastErr = ::GetLastError();
         LOG_ERROR(systemError(lastErr, ERROR_LOCATION));
         return false;
      }

      BOOL isMember = FALSE;
      if (::CheckTokenMembership(hUnfiltered, &adminSID, &isMember))
      {
         auto lastErr = ::GetLastError();
         LOG_ERROR(systemError(lastErr, ERROR_LOCATION));
         return false;
      }
      isAdmin = (isMember != FALSE);
   }
   else
   {
      isAdmin = ::IsUserAnAdmin();
   }
   return isAdmin;
}


FilePath userHomePath(std::string envOverride)
{
   return User::getUserHomePath(envOverride);
}

FilePath userSettingsPath(const FilePath& userHomeDirectory,
                          const std::string& appName,
                          bool ensureDirectory)
{
   FilePath localAppData;
   Error error = knownFolderPath(FOLDERID_LocalAppData,
                                 0,
                                 "unable to compute user settings (LocalAppData) path",
                                 &localAppData);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }

   FilePath settingsPath = localAppData.completeChildPath(appName);
   if (ensureDirectory)
   {
      error = settingsPath.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }

   return settingsPath;
}

FilePath systemSettingsPath(const std::string& appName, bool create)
{
   // KF_FLAG_DONT_VERIFY matches the SHGetFolderPathW(SHGFP_TYPE_CURRENT) call this
   // replaces, which returned the registered path without checking that it exists.
   // Only relevant when we aren't creating: KF_FLAG_CREATE makes the folder, so
   // there is nothing left to verify. Same trap as knownFolderHomePath().
   FilePath settingsPath;
   Error error = knownFolderPath(FOLDERID_ProgramData,
                                 create ? KF_FLAG_CREATE : KF_FLAG_DONT_VERIFY,
                                 "unable to retrieve per machine configuration path",
                                 &settingsPath);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }

   FilePath completePath = settingsPath.completePath(appName);

   // NOTE: we used to create the appName subdirectory via SHGetFolderPathAndSubDirW,
   // but that writes into a caller-supplied MAX_PATH buffer, so the combined path
   // could overrun it. The ACLs are unaffected by the switch: a known folder's
   // predefined security descriptor applies to the known folder itself (ProgramData,
   // which already exists), while a caller-named subdirectory under it is created
   // with inherited attributes either way.
   if (create)
   {
      error = completePath.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
         return FilePath();
      }
   }

   return completePath;
}

bool currentUserIsPrivilleged(unsigned int minimumUserId)
{
   return false;
}

Error captureCommand(const std::string& command, std::string* pOutput)
{
   // WIN32 popen docs:
   // http://msdn.microsoft.com/en-us/library/96ayss4b(VS.80).aspx

   // NOTE: note that popen only works from win32 console applications!

   // start process
   FILE* fp = ::_popen(command.c_str(), "r");
   if (fp == nullptr)
      return systemError(errno, ERROR_LOCATION);

   // collect output
   const int kBuffSize = 1024;
   char buffer[kBuffSize];
   while (::fgets(buffer, kBuffSize, fp) != nullptr)
      *pOutput += buffer;

   // check if an error terminated our output
   Error error;
   if (::ferror(fp))
      error = systemError(boost::system::errc::io_error, ERROR_LOCATION);

   // close file
   if (::_pclose(fp) == -1)
   {
      // log existing error before overwriting it
      if (error)
         LOG_ERROR(error);

      error = systemError(errno, ERROR_LOCATION);
   }

   // return status
   return error;
}

Error realPath(const FilePath& filePath, FilePath* pRealPath)
{
   std::wstring wPath = filePath.getAbsolutePathW();
   std::vector<wchar_t> buffer(512);
   DWORD res = ::GetFullPathNameW(wPath.c_str(),
                                  static_cast<DWORD>(buffer.size()),
                                  &(buffer[0]),
                                  nullptr);
   if (res == 0)
   {
      Error error = LAST_SYSTEM_ERROR();
      error.addProperty("path", filePath);
      return error;
   }
   else if (res > buffer.size())
   {
      buffer.resize(res);
      res = ::GetFullPathNameW(wPath.c_str(),
                               static_cast<DWORD>(buffer.size()),
                               &(buffer[0]),
                               nullptr);
      if (res == 0)
      {
         return LAST_SYSTEM_ERROR();
      }
      else if (res > buffer.size())
         return systemError(boost::system::windows_error::bad_length,
                            ERROR_LOCATION);
   }

   wPath = std::wstring(&(buffer[0]), res);
   *pRealPath = FilePath(wPath);
   return Success();
}

Error realPath(const std::string& path, FilePath* pRealPath)
{
   return realPath(FilePath(path), pRealPath);
}

bool isHiddenFile(const FilePath& filePath)
{
   return isHiddenFile(filePath.getAbsolutePath());
}

bool isHiddenFile(const FileInfo& fileInfo)
{
   return isHiddenFile(fileInfo.absolutePath());
}

bool isReadOnly(const FilePath& filePath)
{
   // TODO: readonly detection for windows
   return false;
}

bool isRemotePath(const FilePath& filePath)
{
   std::wstring path = filePath.getAbsolutePathW();
   if (path.empty())
      return false;

   // PathIsNetworkPath recognizes UNC paths (\\server\share) as well as drive
   // letters mapped to network shares. Note that it does not detect drives
   // mapped via SUBST or DefineDosDevice, and it does not verify that the
   // resource exists -- but best-effort detection is sufficient here.
   if (::PathIsNetworkPathW(path.c_str()))
      return true;

   // As a fallback, classify the volume directly via its drive letter. This
   // can catch some network drives that PathIsNetworkPath does not recognize.
   if (path.length() >= 2 && path[1] == L':')
   {
      wchar_t root[] = { path[0], L':', L'\\', L'\0' };
      if (::GetDriveTypeW(root) == DRIVE_REMOTE)
         return true;
   }

   return false;
}

Error makeFileHidden(const FilePath& path)
{
   std::wstring filePath = path.getAbsolutePathW();
   LPCWSTR lpszPath = filePath.c_str();

   DWORD attribs = ::GetFileAttributesW(lpszPath);
   if (attribs == INVALID_FILE_ATTRIBUTES)
   {
      return LAST_SYSTEM_ERROR();
   }

   if (!::SetFileAttributesW(lpszPath, attribs | FILE_ATTRIBUTE_HIDDEN))
   {
      return LAST_SYSTEM_ERROR();
   }

   return Success();
}




bool stderrIsTerminal()
{
   return _isatty(_fileno(stderr));
}

bool stdoutIsTerminal()
{
   return _isatty(_fileno(stdout));
}

// uuid
std::string generateUuid(bool includeDashes)
{
   // create the uuid
   UUID uuid = {0};
   ::UuidCreate(&uuid);
   PUCHAR pChar = nullptr;
   ::UuidToStringA(&uuid, &pChar);
   std::string uuidStr((char*)pChar);
   ::RpcStringFreeA(&pChar);

   // remove dashes if requested
   if (!includeDashes)
      boost::algorithm::replace_all(uuidStr, "-", "");

   // return
   return uuidStr;
}

PidType currentProcessId()
{
   return ::GetCurrentProcessId();
}

Error executablePath(const char *argv0,
                     FilePath* pExecutablePath)
{
   // grow the buffer until the path fits. GetModuleFileNameW signals truncation
   // by returning the buffer size it was given, so a MAX_PATH buffer silently
   // yields a truncated path when the install lives below a long path
   std::vector<wchar_t> buffer(MAX_PATH);
   for (;;)
   {
      DWORD length = ::GetModuleFileNameW(nullptr,
                                          &buffer[0],
                                          static_cast<DWORD>(buffer.size()));
      if (length == 0)
         return LAST_SYSTEM_ERROR();

      if (length < buffer.size())
      {
         *pExecutablePath = FilePath(std::wstring(&buffer[0], length));
         return Success();
      }

      // the buffer already held the longest path Windows can express, so a further
      // grow would be pointless
      if (buffer.size() > kMaxPathLength)
      {
         return systemError(boost::system::errc::filename_too_long,
                            "executable path exceeds " +
                               safe_convert::numberToString(kMaxPathLength) + " characters",
                            ERROR_LOCATION);
      }

      // clamp, so the final attempt is exactly that longest path rather than
      // overshooting it and rejecting paths that are actually reachable
      buffer.resize(std::min(buffer.size() * 2, kMaxPathLength + 1));
   }
}

// installation path
Error installPath(const std::string& relativeToExecutable,
                  const char * argv0,
                  FilePath* pInstallationPath)
{
   // get full executable path
   FilePath exePath;
   Error error = executablePath(argv0, &exePath);
   if (error)
      return error;

   // resolve to install path using given relative path
   if (relativeToExecutable == "..") // common case
     *pInstallationPath = exePath.getParent().getParent();
   else
     *pInstallationPath = exePath.getParent().completePath(relativeToExecutable);

   return Success();
}

void fixupExecutablePath(FilePath* pExePath)
{
   if (pExePath->getExtension().empty())
     *pExePath = pExePath->getParent().completePath(pExePath->getFilename() + ".exe");
}

void abort()
{
   ::exit(1);
}

 
////////////////////////////////////////////////////////////////////////////
//
//  No signals on Win32 so all of these are no-ops
//
//


Error ignoreTerminalSignals()
{
   return Success();
}
      
Error ignoreChildExits()
{
   return Success();
}
     
Error reapChildren()
{
   return Success();
}
   
struct SignalBlocker::Impl
{
};
   
SignalBlocker::SignalBlocker()
   : pImpl_(new Impl())
{
}
   
   
Error SignalBlocker::block(SignalType signal)
{
   return Success();
}

Error SignalBlocker::blockAll()
{
   return Success();
}
      
SignalBlocker::~SignalBlocker()
{
   try
   {
   }
   catch(...)
   {
   }
}
   
Error clearSignalMask()
{
   return Success();
}

namespace signal_safe {

int clearSignalMask()
{
   return 0;
}

}
   
Error handleSignal(SignalType signal, void (*handler)(int))
{
  return Success();
}
   
core::Error ignoreSignal(SignalType signal)
{
   return Success();
}   


Error useDefaultSignalHandler(SignalType signal)
{
   return Success();
}

void sendSignalToSelf(SignalType signal)
{
}

class ClipboardScope : boost::noncopyable
{
public:
   ClipboardScope() : opened_(false) {}

   Error open()
   {
      if (!::OpenClipboard(nullptr))
      {
         return LAST_SYSTEM_ERROR();
      }
      else
      {
         opened_ = true;
         return Success();
      }
   }

   ~ClipboardScope()
   {
      try
      {
         if (opened_)
         {
            if (!::CloseClipboard())
            {
               LOG_ERROR(LAST_SYSTEM_ERROR());
            }
         }
      }
      catch(...)
      {
      }
   }

private:
   bool opened_;
};

class EnhMetaFile : boost::noncopyable
{
public:
   EnhMetaFile() : hMF_(nullptr) {}

   Error open(const FilePath& path)
   {
      hMF_ = ::GetEnhMetaFileW(path.getAbsolutePathW().c_str());
      if (hMF_ == nullptr)
      {
         return LAST_SYSTEM_ERROR();
      }
      else
         return Success();
   }

   ~EnhMetaFile()
   {
      try
      {
         if (hMF_ != nullptr)
         {
            if (!::DeleteEnhMetaFile(hMF_))
            {
               LOG_ERROR(LAST_SYSTEM_ERROR());
            }
         }
      }
      catch(...)
      {
      }
   }

   HENHMETAFILE handle() const { return hMF_; }

   void release()
   {
      hMF_ = nullptr;
   }

private:
  HENHMETAFILE hMF_;
};


Error copyMetafileToClipboard(const FilePath& path)
{
   // open metafile
   EnhMetaFile enhMetaFile;
   Error error = enhMetaFile.open(path);
   if (error)
      return error;

   // open the clipboard
   ClipboardScope clipboardScope;
   error = clipboardScope.open();
   if (error)
      return error;

   // empty the clipboard
   if (!::EmptyClipboard())
   {
      return LAST_SYSTEM_ERROR();
   }

   // set the clipboard data
   if (!::SetClipboardData(CF_ENHMETAFILE, enhMetaFile.handle()))
   {
      return LAST_SYSTEM_ERROR();
   }

   // release the handle (because the clipboard now owns it)
   enhMetaFile.release();

   // return success
   return Success();
}

void ensureLongPath(FilePath* pFilePath)
{
   // Compare in backslash form. FilePath keeps whatever separators it was constructed
   // from, and most of ours round-trip through generic (forward slash) strings, so
   // getAbsolutePathNative() is not on its own a guarantee of native separators --
   // normalize explicitly. That makes the guard below a comparison of long-vs-short
   // rather than of separator style, whichever separators GetLongPathNameW hands back.
   // longPathName() returns its input unchanged when the path can't be expanded, so
   // this only reassigns when we actually learned something.
   std::string path = pFilePath->getAbsolutePathNative();
   std::replace(path.begin(), path.end(), '/', '\\');

   std::string longPath = file_utils::longPathName(path);
   if (longPath != path)
      *pFilePath = FilePath(longPath);
}

Error expandEnvironmentVariables(std::string value, std::string* pResult)
{
   if (value.empty())
   {
      *pResult = value;
      return Success();
   }

   DWORD sizeRequired = ::ExpandEnvironmentStrings(value.c_str(), nullptr, 0);
   if (!sizeRequired)
   {
      return LAST_SYSTEM_ERROR();
   }

   std::vector<char> buffer(sizeRequired);
   auto result = ::ExpandEnvironmentStrings(value.c_str(),
                                           &buffer[0],
                                           static_cast<DWORD>(buffer.capacity()));

   if (!result)
   {
      return LAST_SYSTEM_ERROR();
   }
   else if (result > buffer.capacity())
      return systemError(ERROR_MORE_DATA, ERROR_LOCATION); // not expected

   *pResult = std::string(&buffer[0]);
   return Success();
}

FilePath expandComSpec()
{
   std::string result;
   Error err = expandEnvironmentVariables("%COMSPEC%", &result);
   if (err)
      return FilePath();
   return FilePath(result);
}

Error terminateProcess(PidType pid)
{
   HANDLE hProc = ::OpenProcess(PROCESS_TERMINATE, false, pid);
   if (!hProc)
   {
      return LAST_SYSTEM_ERROR();
   }
   CloseHandleOnExitScope closeHandle(&hProc, ERROR_LOCATION);
   if (!::TerminateProcess(hProc, 1))
   {
      return LAST_SYSTEM_ERROR();
   }
   return Success();
}

std::vector<SubprocInfo> getSubprocesses(PidType pid)
{
   std::vector<SubprocInfo> subprocs;

   HANDLE hSnapShot;
   CloseHandleOnExitScope closeSnapShot(&hSnapShot, ERROR_LOCATION);

   hSnapShot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
   if (hSnapShot == INVALID_HANDLE_VALUE)
   {
      // err on the side of assuming child processes, so we don't kill
      // a job unintentionally
      LOG_ERROR(LAST_SYSTEM_ERROR());
      return subprocs;
   }

   PROCESSENTRY32 pe32;
   pe32.dwSize = sizeof(pe32);
   if (!Process32First(hSnapShot, &pe32))
   {
      LOG_ERROR(LAST_SYSTEM_ERROR());
      return subprocs;
   }

   do
   {
      if (pe32.th32ParentProcessID == pid)
      {
         // Found a child process
         SubprocInfo info;
         info.pid = pe32.th32ProcessID;
         info.exe = pe32.szExeFile;

         subprocs.push_back(info);
      }
   } while (Process32Next(hSnapShot, &pe32));

   return subprocs;
}

FilePath currentWorkingDir(PidType pid)
{
   // NYI for Win32; commonly accepted technique for this is to use
   // CreateRemoteThread to inject code to run GetCurrentDirectory in the
   // context of the target program. That is ugly and we aren't
   // likely to ever do it.
   return FilePath();
}

Error closeHandle(HANDLE* pHandle, const ErrorLocation& location)
{
   if (*pHandle != nullptr)
   {
      BOOL result = ::CloseHandle(*pHandle);
      *pHandle = nullptr;

      if (!result)
      {
         return LAST_SYSTEM_ERROR();
      }
      else
         return Success();
   }
   else
   {
      return Success();
   }
}

CloseHandleOnExitScope::~CloseHandleOnExitScope()
{
   try
   {
      // A "null" handle can contain INVALID_HANDLE or NULL, depending
      // on the context. This is a painful inconsistency in Windows, see:
      // https://blogs.msdn.microsoft.com/oldnewthing/20040302-00/?p=40443
      if (!pHandle_ || *pHandle_ == INVALID_HANDLE_VALUE || *pHandle_ == nullptr)
         return;

      Error error = closeHandle(pHandle_, location_);
      if (error)
         LOG_ERROR(error);
   }
   catch(...)
   {
   }
}

Error getProcesses(std::vector<ProcessInfo> *pOutProcesses)
{
   PROCESSENTRY32 processEntry;
   memset(&processEntry, 0, sizeof(PROCESSENTRY32));
   processEntry.dwSize = sizeof(PROCESSENTRY32);

   HANDLE hSnap = ::CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
   if (hSnap == INVALID_HANDLE_VALUE)
   {
      return LAST_SYSTEM_ERROR();
   }
   CloseHandleOnExitScope closeSnap(&hSnap, ERROR_LOCATION);

   if (Process32First(hSnap, &processEntry))
   {
      BOOL moreProcesses = TRUE;

      while (moreProcesses)
      {
         ProcessInfo process;
         process.pid = processEntry.th32ProcessID;
         process.ppid = processEntry.th32ParentProcessID;
         pOutProcesses->push_back(process);

         moreProcesses = ::Process32Next(hSnap, &processEntry);
      }
   }

   return Success();
}

Error getChildProcesses(std::vector<ProcessInfo> *pOutProcesses)
{
   if (!pOutProcesses)
      return systemError(EINVAL, ERROR_LOCATION);

   // get all processes
   std::vector<ProcessInfo> processes;
   Error error = getProcesses(&processes);
   if (error) return error;

   // build a process tree of the processes
   ProcessTreeT tree;
   createProcessTree(processes, &tree);

   // return just the children of this process
   ProcessTreeT::const_iterator iter = tree.find(::GetCurrentProcessId());
   if (iter == tree.end())
      return Success();

   const boost::shared_ptr<ProcessTreeNode>& rootNode = iter->second;
   getChildren(rootNode, pOutProcesses);

   return Success();
}

Error terminateChildProcesses()
{
   std::vector<ProcessInfo> childProcesses;
   Error error = getChildProcesses(&childProcesses);
   if (error)
      return error;

   for (const ProcessInfo& process : childProcesses)
   {
      HANDLE hChildProc = ::OpenProcess(PROCESS_ALL_ACCESS, FALSE, process.pid);
      if (hChildProc)
      {
         if (!::TerminateProcess(hChildProc, 1))
         {
            LOG_ERROR(LAST_SYSTEM_ERROR());
         }

         if (!::CloseHandle(hChildProc))
         {
            LOG_ERROR(LAST_SYSTEM_ERROR());
         }
      }
      else
      {
         LOG_ERROR(LAST_SYSTEM_ERROR());
      }
   }

   // the actual kill is best effort
   // so return success regardless
   return Success();
}

void setHomeToUserProfile(core::system::Options* pChildEnv)
{
   std::string userProfile = core::system::getenv(*pChildEnv, "USERPROFILE");
   core::system::setenv(pChildEnv, "HOME", userProfile);
}

} // namespace system
} // namespace core
} // namespace rstudio

