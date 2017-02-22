/*
 * Win32Pty.cpp
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

#include "Win32Pty.hpp"

#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

// Obtain text description of winpty error (can return empty string)
std::string winptyErrorMsg(winpty_error_ptr_t pErr)
{
   if (!pErr)
      return std::string();

   LPCWSTR pErrMsg = winpty_error_msg(pErr);
   if (!pErrMsg)
      return std::string();

   return string_utils::wideToUtf8(pErrMsg);
}

// Holder for winpty_error_ptr_t
class WinPtyError : boost::noncopyable
{
public:
   WinPtyError()
      : pErr_(NULL)
   {}

   virtual ~WinPtyError()
   {
      if (pErr_)
         winpty_error_free(pErr_);
   }

   // Return ptr to our winpty_error_ptr_t, which is then passed to the
   // winpty functions so they can set an error condition.
   winpty_error_ptr_t* ppErr()
   {
      if (pErr_)
      {
         winpty_error_free(pErr_);
         pErr_ = NULL;
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
      : pConfig_(NULL)
   {
      // winpty DLL aborts if cols and/or rows are zero
      if (cols < 1)
         cols = 80;
      if (rows < 1)
         rows = 25;

      // winpty DLL aborts if timeoutMs is zero
      if (timeoutMs == 0)
         timeoutMs = 500;

      pConfig_ = winpty_config_new(agentFlags, err_.ppErr());
      if (pConfig_)
      {
         winpty_config_set_initial_size(pConfig_, cols, rows);
         winpty_config_set_mouse_mode(pConfig_, mousemode);
         winpty_config_set_agent_timeout(pConfig_, timeoutMs);
      }
   }

   virtual ~WinPtyConfig()
   {
      if (pConfig_)
         winpty_config_free(pConfig_);
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
      : pSpawnConfig_(NULL)
   {
      // Build wchar_t environment
      LPCWSTR lpEnv = NULL;
      std::vector<wchar_t> envBlock;
      if (options.environment)
      {
         const Options& env = options.environment.get();
         BOOST_FOREACH(const Option& envVar, env)
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

      std::wstring workingDir(options.workingDir.absolutePathW());

      pSpawnConfig_ = winpty_spawn_config_new(
               spawnFlags,
               string_utils::utf8ToWide(appName, "WinPtySpawnConfig::appName").c_str(),
               string_utils::utf8ToWide(cmdLine, "WinPtySpawnConfig::cmdLine").c_str(),
               workingDir.c_str(),
               lpEnv,
               err_.ppErr());
   }

   virtual ~WinPtySpawnConfig()
   {
      if (pSpawnConfig_)
         winpty_spawn_config_free(pSpawnConfig_);
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

void WinPty::init(
      const std::string& exe,
      const std::vector<std::string> args,
      const ProcessOptions& options)
{
   exe_ = exe;
   args_ = args;
   options_ = options;
}

Error WinPty::runProcess(HANDLE* pProcess, HANDLE* pThread)
{
   if (pProcess)
      *pProcess = INVALID_HANDLE_VALUE;
   if (pThread)
      *pThread = INVALID_HANDLE_VALUE;

   if (!ptyRunning())
   {
      return systemError(ERROR_SERVICE_NOT_FOUND,
                         "Pty not running",
                         ERROR_LOCATION);
   }

   // TODO: combine args into one string
   WinPtySpawnConfig spawnConfig(
            WINPTY_SPAWN_FLAG_EXIT_AFTER_SHUTDOWN,
            exe_ /*appName*/,
            "" /*cmdline*/,
            options_);
   if (!spawnConfig.get())
   {
      return systemError(ERROR_INVALID_FLAGS,
                         spawnConfig.errMsg("Failed to create pty spawn config"),
                         ERROR_LOCATION);
   }

   DWORD createProcError;
   WinPtyError err;
   if (!winpty_spawn(pPty_,
                     spawnConfig.get(),
                     pProcess,
                     pThread,
                     &createProcError,
                     err.ppErr()))
   {
      return systemError(createProcError,
                         err.errMsg("runProcess"),
                         ERROR_LOCATION);
   }

   return Success();
}

Error WinPty::startPty(HANDLE* pStdInWrite, HANDLE* pStdOutRead, HANDLE* pStdErrRead)
{
   if (pStdErrRead)
      *pStdErrRead = INVALID_HANDLE_VALUE;
   if (pStdInWrite)
      *pStdInWrite = INVALID_HANDLE_VALUE;
   if (pStdOutRead)
      *pStdOutRead = INVALID_HANDLE_VALUE;

   if (ptyRunning())
      return systemError(ERROR_SERVICE_EXISTS,
                         "WinPty already running",
                         ERROR_LOCATION);

  if (!options_.pseudoterminal)
   {
      return systemError(ERROR_INVALID_FLAGS,
                         "Pseudoterminal dimensions not provided",
                         ERROR_LOCATION);
   }

   UINT64 agentFlags = 0x00;

   // TODO (gary) don't set this for Windows Vista or earlier
   agentFlags |=WINPTY_FLAG_ALLOW_CURPROC_DESKTOP_CREATION;

   int mousemode = WINPTY_MOUSE_MODE_AUTO;
   DWORD timeoutMs = 1000;

   WinPtyConfig ptyConfig(agentFlags,
                          options_.pseudoterminal.get().cols,
                          options_.pseudoterminal.get().rows,
                          mousemode,
                          timeoutMs);
   if (!ptyConfig.get())
   {
      return systemError(ERROR_INVALID_FLAGS,
                         ptyConfig.errMsg("Failed to create pty config"),
                         ERROR_LOCATION);
   }

   WinPtyError ptyerr;
   pPty_ = winpty_open(ptyConfig.get(), ptyerr.ppErr());
   if (!pPty_)
   {
      return systemError(ERROR_SERVICE_NEVER_STARTED,
                         ptyerr.errMsg("Failed to start pty"),
                         ERROR_LOCATION);
   }

   if (pStdInWrite && winpty_conin_name(pPty_))
   {
      *pStdInWrite = ::CreateFileW(winpty_conin_name(pPty_),
                                   GENERIC_WRITE,
                                   0 /*dwShareMode*/,
                                   NULL /*lpSecurityAttributed*/,
                                   OPEN_EXISTING,
                                   0 /*dwFlagsAndAttributes*/,
                                   NULL /*hTemplateFile*/);
      if (*pStdInWrite == INVALID_HANDLE_VALUE)
      {
         DWORD err = ::GetLastError();
         stopPty();
         ::SetLastError(err);
         return systemError(::GetLastError(),
                            "Failed to connect to pty conin pipe",
                            ERROR_LOCATION);
      }
   }

   if (pStdOutRead && winpty_conout_name(pPty_))
   {
      *pStdOutRead = ::CreateFileW(winpty_conout_name(pPty_),
                                   GENERIC_READ,
                                   0 /*dwShareMode*/,
                                   NULL /*lpSecurityAttributed*/,
                                   OPEN_EXISTING,
                                   0 /*dwFlagsAndAttributes*/,
                                   NULL /*hTemplateFile*/);
      if (*pStdOutRead == INVALID_HANDLE_VALUE)
      {
         DWORD err = ::GetLastError();
         if (pStdInWrite && *pStdInWrite != INVALID_HANDLE_VALUE)
         {
            ::CloseHandle(*pStdInWrite);
            *pStdInWrite = INVALID_HANDLE_VALUE;
         }
         stopPty();
         ::SetLastError(err);
         return systemError(::GetLastError(),
                            "Failed to connect to pty conout pipe",
                            ERROR_LOCATION);
      }
   }
   if (pStdErrRead && winpty_conerr_name(pPty_))
   {
      *pStdErrRead = ::CreateFileW(winpty_conerr_name(pPty_),
                                   GENERIC_READ,
                                   0 /*dwShareMode*/,
                                   NULL /*lpSecurityAttributed*/,
                                   OPEN_EXISTING,
                                   0 /*dwFlagsAndAttributes*/,
                                   NULL /*hTemplateFile*/);
      if (*pStdOutRead == INVALID_HANDLE_VALUE)
      {
         DWORD err = ::GetLastError();
         if (pStdInWrite && *pStdInWrite != INVALID_HANDLE_VALUE)
         {
            ::CloseHandle(*pStdInWrite);
            *pStdInWrite = INVALID_HANDLE_VALUE;
         }
         if (pStdOutRead && *pStdOutRead != INVALID_HANDLE_VALUE)
         {
            ::CloseHandle(*pStdOutRead);
            *pStdOutRead = INVALID_HANDLE_VALUE;
         }
         stopPty();
         ::SetLastError(err);
         return systemError(::GetLastError(),
                            "Failed to connect to pty conerr pipe",
                            ERROR_LOCATION);
      }
   }
   return Success();
}

void WinPty::stopPty()
{
   if (!ptyRunning())
      return;
   winpty_free(pPty_);
   pPty_ = NULL;
}

bool WinPty::ptyRunning() const
{
   return pPty_ != NULL;
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
   if (!winpty_set_size(pPty_, cols, rows, err.ppErr()))
   {
      return systemError(ERROR_CAN_NOT_COMPLETE,
                         err.errMsg("setSize"),
                         ERROR_LOCATION);
   }
   return Success();
}

} // namespace system
} // namespace core
} // namespace rstudio
