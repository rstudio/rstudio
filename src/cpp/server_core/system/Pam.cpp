/*
 * Pam.cpp
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

#include <server_core/system/Pam.hpp>

#include <boost/utility.hpp>
#include <boost/regex.hpp>

#include <core/Log.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

class MemoryPool : boost::noncopyable {

   typedef boost::function<void*(size_t)> Alloc;
   typedef boost::function<void(void*)> Free;

public:
   MemoryPool(Alloc allocFunc = ::malloc, Free freeFunc = ::free) :
      alloc_(allocFunc),
      free_(freeFunc)
   {}

   ~MemoryPool()
   {
      try
      {
         for (size_t i = 0; i < buffers_.size(); i++)
         {
            free_(buffers_.at(i));
         }
      }
      catch(...)
      {
      }
   }

   void* alloc(size_t size)
   {
      void* p = alloc_(size);
      if (p)
         buffers_.push_back(p);
      return p;
   }

   void relinquishOwnership()
   {
      buffers_.clear();
   }

private:
   std::vector<void*> buffers_;
   Alloc alloc_;
   Free free_;
};

} // anonymous namespace

int conv(int num_msg,
         const struct pam_message** msg,
         struct pam_response** resp,
         void * appdata_ptr)
{
   try
   {
      MemoryPool pool;

      // resp will be freed by the caller
      *resp = static_cast<pam_response*>(pool.alloc(sizeof(pam_response) * num_msg));
      if (!*resp)
         return PAM_BUF_ERR;

      ::memset(*resp, 0, sizeof(pam_response) * num_msg);

      for (int i = 0; i < num_msg; i++)
      {
         const pam_message* input = msg[i];
         std::string msgText = input->msg;

         switch (input->msg_style)
         {
         case PAM_PROMPT_ECHO_OFF:
         {
            boost::regex passwordRegex("\\bpassword:\\s*$",
                                       boost::regex_constants::icase);
            boost::smatch match;
            PAM* pPam = static_cast<PAM*>(appdata_ptr);
            if (!pPam->requirePasswordPrompt_ || regex_search(msgText, match, passwordRegex))
            {
               resp[i]->resp_retcode = 0;

               char* password = const_cast<char*>(pPam->password_.c_str());
               // respBuf will be freed by the caller
               char* respBuf = static_cast<char*>(pool.alloc(strlen(password) + 1));
               resp[i]->resp = ::strcpy(respBuf, password);
            }
            else
               return PAM_CONV_ERR;
            break;
         }
         case PAM_TEXT_INFO:
         {
            resp[i]->resp_retcode = 0;
            char* respBuf = static_cast<char*>(pool.alloc(1));
            respBuf[0] = '\0';
            resp[i]->resp = respBuf;
            break;
         }
         case PAM_PROMPT_ECHO_ON:
         case PAM_ERROR_MSG:
         default:
            return PAM_CONV_ERR;
         }
      }

      // The caller will free all the memory we allocated
      pool.relinquishOwnership();

      return PAM_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   return PAM_CONV_ERR;
}

PAM::PAM(const std::string& service,
         bool silent,
         bool closeOnDestroy,
         bool requirePasswordPrompt) :
      service_(service),
      defaultFlags_(silent ? PAM_SILENT : 0),
      pamh_(nullptr),
      status_(PAM_SUCCESS),
      closeOnDestroy_(closeOnDestroy),
      requirePasswordPrompt_(requirePasswordPrompt)
{
}

PAM::~PAM()
{
   try
   {
      if (closeOnDestroy_)
         close();
   }
   catch(...)
   {
   }
}

std::string PAM::lastError()
{
   return std::string(::pam_strerror(pamh_, status_));
}

int PAM::login(const std::string& username,
               const std::string& password)
{
   password_ = password;

   struct pam_conv myConv;
   myConv.conv = conv;
   myConv.appdata_ptr = const_cast<void*>(static_cast<const void*>(this));
   status_ = ::pam_start(service_.c_str(),
                         username.c_str(),
                         &myConv,
                         &pamh_);
   if (status_ != PAM_SUCCESS)
   {
      LOG_ERROR_MESSAGE("pam_start failed: " + lastError());
      return status_;
   }

   status_ = ::pam_authenticate(pamh_, defaultFlags_);
   if (status_ != PAM_SUCCESS)
   {
      if (status_ != PAM_AUTH_ERR)
         LOG_ERROR_MESSAGE("pam_authenticate failed: " + lastError());
      return status_;
   }

   status_ = ::pam_acct_mgmt(pamh_, defaultFlags_);
   if (status_ != PAM_SUCCESS)
   {
      LOG_ERROR_MESSAGE("pam_acct_mgmt failed: " + lastError());
      return status_;
   }

   return PAM_SUCCESS;
}

void PAM::close()
{
   if (pamh_)
   {
      ::pam_end(pamh_, status_);
      
      // TODO (gary) conflicting versions; determine if we should have the former, or this:
      // ::pam_end(pamh_, status_ | (defaultFlags_ & PAM_SILENT));
      pamh_ = nullptr;
   }
}

} // namespace system
} // namespace core
} // namespace rstudio
