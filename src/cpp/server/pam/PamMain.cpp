/*
 * PamMain.cpp
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

#include <security/pam_appl.h>

#include <iostream>
#include <stdio.h>

#include <boost/utility.hpp>
#include <boost/regex.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <core/system/PosixUser.hpp>

// NOTE: Mac OS X supports PAM but ships with it in a locked-down config
// which will cause all passwords to be rejected. To make it work run:
//
//   sudo cp /etc/pam.d/ftpd /etc/pam.d/rstudio
//
// That configures PAM to send rstudio through the same authentication
// stack as ftpd uses, which is similar to us.


using namespace core ;

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
            if (regex_search(msgText, match, passwordRegex))
            {
               resp[i]->resp_retcode = 0;
               char* password = static_cast<char*>(appdata_ptr);
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

// Low-level C++ wrapper around PAM API.
class PAMAuth
{
public:
    explicit PAMAuth(bool silent) :
          defaultFlags_(silent ? PAM_SILENT : 0),
          pamh_(NULL),
          status_(PAM_SUCCESS)
    {
    }

    virtual ~PAMAuth()
    {
       if (pamh_)
       {
          ::pam_end(pamh_, status_ | (defaultFlags_ & PAM_SILENT));
       }
    }

    std::pair<int, const std::string> lastError()
    {
       return std::pair<int, const std::string>(
             status_,
             std::string(::pam_strerror(pamh_, status_)));
    }

    int login(const std::string& username,
              const std::string& password)
    {
       struct pam_conv myConv;
       myConv.conv = conv;
       myConv.appdata_ptr = const_cast<void*>(static_cast<const void*>(password.c_str()));
       status_ = ::pam_start("rstudio",
                             username.c_str(),
                             &myConv,
                             &pamh_);
       if (status_ != PAM_SUCCESS)
       {
          LOG_ERROR_MESSAGE("pam_start failed: " + lastError().second);
          return status_;
       }

       status_ = ::pam_authenticate(pamh_, defaultFlags_);
       if (status_ != PAM_SUCCESS)
       {
          if (status_ != PAM_AUTH_ERR)
             LOG_ERROR_MESSAGE("pam_authenticate failed: " + lastError().second);
          return status_;
       }

       status_ = ::pam_acct_mgmt(pamh_, defaultFlags_);
       if (status_ != PAM_SUCCESS)
       {
          LOG_ERROR_MESSAGE("pam_acct_mgmt failed: " + lastError().second);
          return status_;
       }

       return PAM_SUCCESS;
 }

private:
    int defaultFlags_;
    pam_handle_t* pamh_;
    int status_;
};

int inappropriateUsage(const ErrorLocation& location)
{
   // log warning
   boost::format fmt("Inappropriate use of pam helper binary (user=%1%)");
   std::string msg = boost::str(
               fmt % core::system::user::currentUserIdentity().userId);
   core::log::logWarningMessage(msg, location);

   // additional notification to the user
   std::cerr << "\nThis binary is not designed for running this way\n"
                "-- the system administrator has been informed\n\n";

   // cause further annoyance
   ::sleep(10);

   return EXIT_FAILURE;
}

} // anonymous namespace


int main(int argc, char * const argv[]) 
{
   try
   { 
      // initialize log
      initializeSystemLog("rserver-pam", core::system::kLogLevelWarning);

      // ignore SIGPIPE
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      // ensure that we aren't being called inappropriately
      if (::isatty(STDIN_FILENO))
         return inappropriateUsage(ERROR_LOCATION);
      else if (::isatty(STDOUT_FILENO))
         return inappropriateUsage(ERROR_LOCATION);
      else if (argc != 2)
         return inappropriateUsage(ERROR_LOCATION);

      // read username from command line
      std::string username(argv[1]);

      // read password (up to 200 chars in length)
      std::string password;
      const int MAXPASS = 200;
      int ch = 0;
      int count = 0;
      while((ch = ::fgetc(stdin)) != EOF)
      {
         if (++count <= MAXPASS)
         {
            password.push_back(static_cast<char>(ch));
         }
         else
         {
            LOG_WARNING_MESSAGE("Password exceeded maximum length for "
                                "user " + username);
            return EXIT_FAILURE;
         }
      }

      // verify password
      if (PAMAuth(false).login(username, password) == PAM_SUCCESS)
         return EXIT_SUCCESS;
      else
         return EXIT_FAILURE;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

