/*
 * Pam.hpp
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

#ifndef SERVER_CORE_SYSTEM_PAM_HPP
#define SERVER_CORE_SYSTEM_PAM_HPP

#include <security/pam_appl.h>

#include <string>

#include <boost/utility.hpp>

namespace rstudio {
namespace core {
namespace system {

int conv(int num_msg,
         const struct pam_message** msg,
         struct pam_response** resp,
         void * appdata_ptr);

// NOTE: Mac OS X supports PAM but ships with it in a locked-down config
// which will cause all passwords to be rejected. To make it work run:
//
//   sudo cp /etc/pam.d/ftpd /etc/pam.d/rstudio
//
// That configures PAM to send rstudio through the same authentication
// stack as ftpd uses, which is similar to us.

// Low-level C++ wrapper around PAM API.
class PAM : boost::noncopyable
{
public:
   PAM(const std::string& service,
       bool silent,
       bool closeOnDestroy = true,
       bool requirePasswordPrompt = true);

   virtual ~PAM();

   std::string lastError();

   int status() const { return status_; }

   virtual int login(const std::string& username,
                     const std::string& password);

   virtual void close();

protected:
    std::string service_;
    int defaultFlags_;
    pam_handle_t* pamh_;
    int status_;
    bool closeOnDestroy_;
    bool requirePasswordPrompt_;

    std::string password_;
    friend int core::system::conv(int num_msg,
                                  const struct pam_message** msg,
                                  struct pam_response** resp,
                                  void * appdata_ptr);
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // namespace SERVER_CORE_SYSTEM_PAM_HPP
