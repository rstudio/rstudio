/*
 * Win32Pty.cpp
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

#include "Win32Pty.hpp"

#include <shared_core/Error.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>
#include <core/system/LibraryLoader.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

// Load winpty.dll via LoadLibrary/GetProcAddress so it can be located
// via run-time logic and isn't loaded until needed.

#define LOAD_WINPTY_SYMBOL(name) \
   error = core::system::loadSymbol(hMod, "winpty_" #name, reinterpret_cast<void**>(&name)); \
   if (error) \
   { \
      Error unloadError = unload(); \
      if (unloadError) \
         LOG_ERROR(unloadError); \
      return error; \
   }

HMODULE hMod = nullptr;

winpty_result_t (*error_code)(winpty_error_ptr_t) = nullptr;
LPCWSTR (*error_msg)(winpty_error_ptr_t) = nullptr;
void (*error_free)(winpty_error_ptr_t) = nullptr;

winpty_config_t *(*config_new)(UINT64, winpty_error_ptr_t *) = nullptr;
void (*config_free)(winpty_config_t *) = nullptr;
void (*config_set_initial_size)(winpty_config_t *, int, int) = nullptr;
void (*config_set_mouse_mode)(winpty_config_t *, int) = nullptr;
void (*config_set_agent_timeout)(winpty_config_t *, DWORD) = nullptr;

winpty_t *(*open)(const winpty_config_t *, winpty_error_ptr_t *) = nullptr;
HANDLE (*agent_process)(winpty_t *) = nullptr;

LPCWSTR (*conin_name)(winpty_t *) = nullptr;
LPCWSTR (*conout_name)(winpty_t *) = nullptr;
LPCWSTR (*conerr_name)(winpty_t *) = nullptr;

winpty_spawn_config_t *(*spawn_config_new)(UINT64, LPCWSTR, LPCWSTR,
                                           LPCWSTR, LPCWSTR,
                                           winpty_error_ptr_t *) = nullptr;
void *(*spawn_config_free)(winpty_spawn_config_t *) = nullptr;
BOOL (*spawn)(winpty_t *, const winpty_spawn_config_t *, HANDLE *,
              HANDLE *, DWORD *, winpty_error_ptr_t *) = nullptr;

BOOL (*set_size)(winpty_t *, int, int, winpty_error_ptr_t *) = nullptr;
void (*free)(winpty_t *) = nullptr;

Error unload()
{
   error_code = nullptr;
   error_msg = nullptr;
   error_free = nullptr;
   config_new = nullptr;
   config_free = nullptr;
   config_set_initial_size = nullptr;
   config_set_mouse_mode = nullptr;
   config_set_agent_timeout = nullptr;
   open = nullptr;
   agent_process = nullptr;
   conin_name = nullptr;
   conout_name = nullptr;
   conerr_name = nullptr;
   spawn_config_new = nullptr;
   spawn_config_free = nullptr;
   spawn = nullptr;
   set_size = nullptr;
   free = nullptr;

   if (hMod)
   {
      Error error = core::system::closeLibrary(hMod);
      hMod = nullptr;
      if (error)
         return error;
   }
   return Success();
}

Error tryLoad(const core::FilePath& libraryPath)
{
   // Once DLL is loaded we keep it loaded, i.e. no refcount/unload
   if (hMod)
      return Success();

   Error error = core::system::loadLibrary(
            libraryPath.getAbsolutePath(),
            reinterpret_cast<void**>(&hMod));
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   LOAD_WINPTY_SYMBOL(error_code);
   LOAD_WINPTY_SYMBOL(error_msg);
   LOAD_WINPTY_SYMBOL(error_free);
   LOAD_WINPTY_SYMBOL(config_new);
   LOAD_WINPTY_SYMBOL(config_free);
   LOAD_WINPTY_SYMBOL(config_set_initial_size);
   LOAD_WINPTY_SYMBOL(config_set_mouse_mode);
   LOAD_WINPTY_SYMBOL(config_set_agent_timeout);
   LOAD_WINPTY_SYMBOL(open);
   LOAD_WINPTY_SYMBOL(agent_process);
   LOAD_WINPTY_SYMBOL(conin_name);
   LOAD_WINPTY_SYMBOL(conout_name);
   LOAD_WINPTY_SYMBOL(conerr_name);
   LOAD_WINPTY_SYMBOL(spawn_config_new);
   LOAD_WINPTY_SYMBOL(spawn_config_free);
   LOAD_WINPTY_SYMBOL(spawn);
   LOAD_WINPTY_SYMBOL(set_size);
   LOAD_WINPTY_SYMBOL(free);

   return Success();
}

// Obtain text description of winpty error (can return empty string)
std::string winptyErrorMsg(winpty_error_ptr_t pErr)
{
   if (!pErr || !error_msg)
      return std::string();

   LPCWSTR pErrMsg = error_msg(pErr);
   if (!pErrMsg)
      return std::string();

   return string_utils::wideToUtf8(pErrMsg);
}

// Holder for winpty_error_ptr_t
class WinPtyError : boost::noncopyable
{
public:
   WinPtyError()
      : pErr_(nullptr)
   {}

   virtual ~WinPtyError()
   {
      if (pErr_ && error_free)
         error_free(pErr_);
   }

   // Return ptr to our winpty_error_ptr_t, which is then passed to the
   // winpty functions so they can set an error condition.
   winpty_error_ptr_t* ppErr()
   {
      if (pErr_ && error_free)
      {
         error_free(pErr_);
         pErr_ = nullptr;
      }
      return &pErr_;
   }

   std::string errMsg(const std::string& msg) const
   {
      std::string newMsg(msg);
      std::string ptyMsg(winptyErrorMsg(pErr_));
      if (!ptyMsg.empty())
      {
         if (!newMsg.empty())
         {
            newMsg += ": ";
         }
         newMsg += ptyMsg;
      }
      return newMsg;
   }

private:
   winpty_error_ptr_t pErr_;
};

// Holder for winpty_config_t
class WinPtyConfig : boost::noncopyable
{
public:
   WinPtyConfig(UINT64 agentFlags,
                int cols, int rows,
                int mousemode,
                DWORD timeoutMs)
      : pConfig_(nullptr)
   {
      // winpty DLL aborts if cols and/or rows are zero
      if (cols < 1)
         cols = 80;
      if (rows < 1)
         rows = 25;

      // winpty DLL aborts if timeoutMs is zero
      if (timeoutMs == 0)
         timeoutMs = 500;

      if (config_new)
         pConfig_ = config_new(agentFlags, err_.ppErr());
      if (pConfig_)
      {
         if (config_set_initial_size)
            config_set_initial_size(pConfig_, cols, rows);
         if (config_set_mouse_mode)
            config_set_mouse_mode(pConfig_, mousemode);
         if (config_set_agent_timeout)
            config_set_agent_timeout(pConfig_, timeoutMs);
      }
   }

   virtual ~WinPtyConfig()
   {
      if (pConfig_ && config_free)
         config_free(pConfig_);
   }

   const winpty_config_t* get() const
   {
      return pConfig_;
   }

   std::string errMsg(const std::string& msg) const
   {
      return err_.errMsg(msg);
   }

private:
     winpty_config_t* pConfig_;
     WinPtyError err_;
};

// Holder for winpty_spawn_config_t
class WinPtySpawnConfig : boost::noncopyable
{
public:
   WinPtySpawnConfig(
         UINT64 spawnFlags,
         const std::string& appName,
         const std::string& cmdLine,
         const ProcessOptions& options)
      : pSpawnConfig_(nullptr)
   {
      // Build wchar_t environment
      LPCWSTR lpEnv = nullptr;
      std::vector<wchar_t> envBlock;
      if (options.environment)
      {
         const Options& env = options.environment.get();
         for (const Option& envVar : env)
         {
            std::wstring key = string_utils::utf8ToWide(envVar.first);
            std::wstring value = string_utils::utf8ToWide(envVar.second);
            std::copy(key.begin(), key.end(), std::back_inserter(envBlock));
            envBlock.push_back(L'=');
            std::copy(value.begin(), value.end(), std::back_inserter(envBlock));
            envBlock.push_back(L'\0');
         }
         envBlock.push_back(L'\0');
         lpEnv = &envBlock[0];
      }

      std::wstring workingDir(options.workingDir.getAbsolutePathW());

      if (spawn_config_new)
      {
         pSpawnConfig_ = spawn_config_new(
                  spawnFlags,
                  string_utils::utf8ToWide(appName, "WinPtySpawnConfig::appName").c_str(),
                  string_utils::utf8ToWide(cmdLine, "WinPtySpawnConfig::cmdLine").c_str(),
                  workingDir.c_str(),
                  lpEnv,
                  err_.ppErr());
      }
   }

   virtual ~WinPtySpawnConfig()
   {
      if (pSpawnConfig_ && spawn_config_free)
         spawn_config_free(pSpawnConfig_);
   }

   const winpty_spawn_config_t* get() const
   {
      return pSpawnConfig_;
   }

   std::string errMsg(const std::string& msg) const
   {
      return err_.errMsg(msg);
   }

private:
     winpty_spawn_config_t* pSpawnConfig_;
     WinPtyError err_;
};

} // anonymous namespace

WinPty::~WinPty()
{
   stopPty();
}

Error WinPty::start(const std::string& exe,
                    const std::vector<std::string> args,
                    const ProcessOptions& options,
                    HANDLE* pStdInWrite,
                    HANDLE* pStdOutRead,
                    HANDLE* pStdErrRead,
                    HANDLE* pProcess)
{
   exe_ = exe;
   args_ = args;
   options_ = options;

   CloseHandleOnExitScope closeStdInWrite(pStdInWrite, ERROR_LOCATION);
   CloseHandleOnExitScope closeStdOutRead(pStdOutRead, ERROR_LOCATION);
   CloseHandleOnExitScope closeStdErrRead(pStdErrRead, ERROR_LOCATION);
   CloseHandleOnExitScope closeProcessHandle(pProcess, ERROR_LOCATION);

   // Startup the pty agent process
   Error err = startPty(pStdInWrite, pStdOutRead, pStdErrRead);
   if (err)
      return err;


   // Start the user's process inside the agent's hidden console
   err = runProcess(pProcess);
   if (err)
   {
      stopPty();
      return err;
   }

   closeStdInWrite.detach();
   closeStdOutRead.detach();
   closeStdErrRead.detach();
   closeProcessHandle.detach();
   return Success();
}

Error WinPty::startPty(HANDLE* pStdInWrite, HANDLE* pStdOutRead, HANDLE* pStdErrRead)
{
   CloseHandleOnExitScope closeStdInWrite(pStdInWrite, ERROR_LOCATION);
   CloseHandleOnExitScope closeStdOutRead(pStdOutRead, ERROR_LOCATION);
   CloseHandleOnExitScope closeStdErrRead(pStdErrRead, ERROR_LOCATION);

   // We return nullptr handles on error, for consistency with calling code. That
   // requires changing error results from INVALID_HANDLE_VALUE to nullptr.
   if (pStdErrRead)
      *pStdErrRead = nullptr;
   if (pStdInWrite)
      *pStdInWrite = nullptr;
   if (pStdOutRead)
      *pStdOutRead = nullptr;

   if (!options_.pseudoterminal)
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Pseudoterminal options not provided",
                         ERROR_LOCATION);
   }

   Error err = tryLoad(options_.pseudoterminal.get().winptyPath);
   if (err)
      return err;

   if (ptyRunning())
      return systemError(boost::system::errc::already_connected,
                         "WinPty already running",
                         ERROR_LOCATION);

   UINT64 agentFlags = 0x00;

   if (isWin7OrLater()) // See WinPty constant for details
      agentFlags |= WINPTY_FLAG_ALLOW_CURPROC_DESKTOP_CREATION;
   if (options_.pseudoterminal.get().plainText)
      agentFlags |= WINPTY_FLAG_PLAIN_OUTPUT; // no ESC sequences
   if (options_.pseudoterminal.get().conerr)
      agentFlags |= WINPTY_FLAG_CONERR;

   int mousemode = WINPTY_MOUSE_MODE_AUTO;
   DWORD timeoutMs = 1000;

   WinPtyConfig ptyConfig(agentFlags,
                          options_.pseudoterminal.get().cols,
                          options_.pseudoterminal.get().rows,
                          mousemode,
                          timeoutMs);
   if (!ptyConfig.get())
   {
      return systemError(boost::system::errc::invalid_argument,
                         ptyConfig.errMsg("Failed to create pty config"),
                         ERROR_LOCATION);
   }

   WinPtyError ptyerr;
   if (open)
      pPty_ = open(ptyConfig.get(), ptyerr.ppErr());
   if (!pPty_)
   {
      return systemError(boost::system::errc::not_connected,
                         ptyerr.errMsg("Failed to start pty"),
                         ERROR_LOCATION);
   }

   if (pStdInWrite && conin_name && conin_name(pPty_))
   {
      *pStdInWrite = ::CreateFileW(conin_name(pPty_),
                                   GENERIC_WRITE,
                                   0 /*dwShareMode*/,
                                   nullptr /*lpSecurityAttributed*/,
                                   OPEN_EXISTING,
                                   FILE_FLAG_OVERLAPPED,
                                   nullptr /*hTemplateFile*/);
      if (*pStdInWrite == INVALID_HANDLE_VALUE)
      {
         auto lastErr = ::GetLastError();
         stopPty();
         *pStdInWrite = nullptr;
         return systemError(lastErr,
                            "Failed to connect to pty conin pipe",
                            ERROR_LOCATION);
      }
   }

   if (pStdOutRead && conout_name && conout_name(pPty_))
   {
      *pStdOutRead = ::CreateFileW(conout_name(pPty_),
                                   GENERIC_READ,
                                   0 /*dwShareMode*/,
                                   nullptr /*lpSecurityAttributed*/,
                                   OPEN_EXISTING,
                                   FILE_FLAG_OVERLAPPED,
                                   nullptr /*hTemplateFile*/);
      if (*pStdOutRead == INVALID_HANDLE_VALUE)
      {
         auto lastErr = ::GetLastError();
         stopPty();
         *pStdOutRead = nullptr;
         return systemError(lastErr,
                            "Failed to connect to pty conout pipe",
                            ERROR_LOCATION);
      }
   }

   if (options_.pseudoterminal.get().conerr &&
       pStdErrRead && conerr_name && conerr_name(pPty_))
   {
      *pStdErrRead = ::CreateFileW(conerr_name(pPty_),
                                   GENERIC_READ,
                                   0 /*dwShareMode*/,
                                   nullptr /*lpSecurityAttributed*/,
                                   OPEN_EXISTING,
                                   FILE_FLAG_OVERLAPPED,
                                   nullptr /*hTemplateFile*/);
      if (*pStdErrRead == INVALID_HANDLE_VALUE)
      {
         auto lastErr = ::GetLastError();
         stopPty();
         *pStdErrRead = nullptr;
         return systemError(lastErr,
                            "Failed to connect to pty conerr pipe",
                            ERROR_LOCATION);
      }
   }

   closeStdInWrite.detach();
   closeStdOutRead.detach();
   closeStdErrRead.detach();
   return Success();
}

Error WinPty::runProcess(HANDLE* pProcess)
{
   if (pProcess)
      *pProcess = nullptr;

   if (!ptyRunning())
   {
      return systemError(boost::system::errc::no_such_process,
                         "Pty not running",
                         ERROR_LOCATION);
   }

   // process command line arguments (copy of approach done by non-pseudoterm
   // code path in ChildProcess::run for Win32)
   std::string cmdLine;
   for (std::string& arg : args_)
   {
      cmdLine.push_back(' ');

      // This is kind of gross. Ideally we would be more deterministic
      // than this.
      bool quot = std::string::npos != arg.find(' ')
            && std::string::npos == arg.find('"');

      if (quot)
         cmdLine.push_back('"');
      std::copy(arg.begin(), arg.end(), std::back_inserter(cmdLine));
      if (quot)
         cmdLine.push_back('"');
   }
   cmdLine.push_back('\0');

   WinPtySpawnConfig spawnConfig(
            WINPTY_SPAWN_FLAG_AUTO_SHUTDOWN,
            exe_ /*appName*/,
            cmdLine,
            options_);
   if (!spawnConfig.get())
   {
      return systemError(boost::system::errc::invalid_argument,
                         spawnConfig.errMsg("Failed to create pty spawn config"),
                         ERROR_LOCATION);
   }

   DWORD createProcError;
   WinPtyError err;
   if (!spawn)
   {
      return systemError(boost::system::errc::function_not_supported,
                         "spawn function not loaded", ERROR_LOCATION);
   }
   if (!spawn(pPty_,
              spawnConfig.get(),
              pProcess,
              nullptr /*pThread*/,
              &createProcError,
              err.ppErr()))
   {
      if (pProcess)
         *pProcess = nullptr;
      return systemError(createProcError,
                         err.errMsg("runProcess"),
                         ERROR_LOCATION);
   }

   return Success();
}

void WinPty::stopPty()
{
   if (!ptyRunning())
      return;
   if (free)
      free(pPty_);
   pPty_ = nullptr;
}

bool WinPty::ptyRunning() const
{
   return pPty_ != nullptr;
}

Error WinPty::setSize(int cols, int rows)
{
   if (!ptyRunning())
   {
      return systemError(ERROR_SERVICE_NOT_FOUND,
                         "Pty not running",
                         ERROR_LOCATION);
   }

   if (cols < 1 || rows < 1)
   {
      return systemError(ERROR_INVALID_FLAGS,
                         "Resize passed invalid terminal size",
                         ERROR_LOCATION);
   }

   WinPtyError err;
   if (!set_size || !set_size(pPty_, cols, rows, err.ppErr()))
   {
      return systemError(ERROR_CAN_NOT_COMPLETE,
                         err.errMsg("setSize"),
                         ERROR_LOCATION);
   }
   return Success();
}

Error WinPty::interrupt()
{
  // TODO (gary)
   return Success();
}

Error WinPty::writeToPty(HANDLE hPipe, const std::string& input)
{
   if (input.empty())
      return Success();

   OVERLAPPED over;
   memset(&over, 0, sizeof(over));

   DWORD dwWritten;
   BOOL bSuccess = ::WriteFile(hPipe,
                               input.data(),
                               static_cast<DWORD>(input.length()),
                               &dwWritten,
                               &over);
   auto lastErr = ::GetLastError();
   if (!bSuccess && lastErr == ERROR_IO_PENDING)
   {
      bSuccess = GetOverlappedResult(hPipe,
                                     &over,
                                     &dwWritten,
                                     TRUE /*wait*/);
      lastErr = ::GetLastError();
   }
   if (!bSuccess)
      return systemError(lastErr, ERROR_LOCATION);

   return Success();
}


Error WinPty::readFromPty(HANDLE hPipe, std::string* pOutput)
{
   // check for available bytes
   DWORD dwAvail = 0;
   if (!::PeekNamedPipe(hPipe, nullptr, 0, nullptr, &dwAvail, nullptr))
   {
      auto lastErr = ::GetLastError();
      if (lastErr == ERROR_BROKEN_PIPE)
         return Success();
      else
         return systemError(lastErr, ERROR_LOCATION);
   }

   // no data available
   if (dwAvail == 0)
      return Success();

   // read data which is available
   DWORD nBytesRead = dwAvail;
   std::vector<CHAR> buffer(dwAvail, 0);
   OVERLAPPED over;
   memset(&over, 0, sizeof(over));
   BOOL bSuccess = ::ReadFile(hPipe, &(buffer[0]), dwAvail, nullptr, &over);
   auto lastErr = ::GetLastError();
   if (!bSuccess && lastErr == ERROR_IO_PENDING)
   {
      bSuccess = GetOverlappedResult(hPipe,
                                     &over,
                                     &nBytesRead,
                                     TRUE /*wait*/);
      lastErr = ::GetLastError();
   }

   if (!bSuccess)
      return systemError(lastErr, ERROR_LOCATION);

   // append to output
   pOutput->append(&(buffer[0]), nBytesRead);

   // success
   return Success();
}

} // namespace system
} // namespace core
} // namespace rstudio
