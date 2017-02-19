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

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

// Build an error string with user-supplied message, and append
// winpty error state, if any
std::string buildErrMsg(const std::string& msg, winpty_error_ptr_t pErr)
{
}

// Hold and release a winpty_error_ptr_t
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

      if (pErr_)
      {
         LPCWSTR pErrMsg = winpty_error_msg(pErr_);
         if (pErrMsg)
         {
            std::string ptyMsg = string_utils::wideToUtf8(pErrMsg);
            if (!ptyMsg.empty())
            {
               if (!newMsg.empty())
               {
                  newMsg += ": ";
               }
               newMsg += ptyMsg;
            }
         }
      }
      return newMsg;
   }

private:
   winpty_error_ptr_t pErr_;
};

// hold and release a winpty_config_t
class WinPtyConfig : boost::noncopyable
{
public:
   WinPtyConfig(UINT64 agentFlags,
                int cols, int rows,
                int mousemode,
                DWORD timeoutMs)
      : pConfig_(NULL)
   {
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

} // anonymous namespace

WinPty::~WinPty()
{
   stopAgent();
}

bool WinPty::startAgent(UINT64 agentFlags,
                        int cols, int rows,
                        int mousemode,
                        DWORD timeoutMs)
{
   if (pPty_)
      return true;

   WinPtyConfig ptyConfig(agentFlags, cols, rows, mousemode, timeoutMs);
   if (!ptyConfig.get())
   {
      LOG_ERROR_MESSAGE(ptyConfig.errMsg("Failed to create pty config"));
      return false;
   }

   WinPtyError err;
   pPty_ = winpty_open(ptyConfig.get(), err.ppErr());
   if (!pPty_)
   {
      LOG_ERROR_MESSAGE(err.errMsg("Failed to open agent"));
      return false;
   }

   return true;
}

void WinPty::stopAgent()
{
   if (pPty_)
   {
      winpty_free(pPty_);
      pPty_ = NULL;
   }
}

bool WinPty::agentRunning() const
{
   return pPty_ != NULL;
}

} // namespace system
} // namespace core
} // namespace rstudio

