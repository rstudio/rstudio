/*
 * Cookie.hpp
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

#ifndef CORE_HTTP_COOKIE_HPP
#define CORE_HTTP_COOKIE_HPP

#include <string>

#include <boost/date_time/gregorian/gregorian.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
namespace rstudio {
namespace core {
namespace http {

class Request;

#define kLegacyCookieSuffix "-legacy"

class Cookie
{
public:
   enum class SameSite
   {
      Undefined,
      None,
      Lax,
      Strict
   };

   Cookie(const Request& request,
          const std::string& name,
          const std::string& value, 
          const std::string& path,
          SameSite sameSite = SameSite::Undefined,
          bool httpOnly = false,
          bool secure = false);
   virtual ~Cookie();

   // COPYING: via compiler (copyable members)

   void setName(const std::string& name) { name_ = name; }
   const std::string& name() const { return name_; }

   void setValue(const std::string& value) { value_ = value; }
   const std::string& value() const { return value_; }

   void setDomain(const std::string& domain) { domain_ = domain; }
   const std::string& domain() const { return domain_; }

   void setPath(const std::string& path) { path_ = path; }
   const std::string& path() const { return path_; }

   void setExpires(const boost::posix_time::time_duration& expiresFromNow);
   void setExpiresDelete();
   const boost::posix_time::ptime& expires() const { return expires_; }
   
   void setHttpOnly();
   bool httpOnly() const { return httpOnly_; }

   void setSecure();
   bool secure() const { return secure_; }

   void setSameSite(SameSite sameSite);
   SameSite sameSite() const { return sameSite_; }

   std::string cookieHeaderValue() const;

private:
   std::string name_;
   std::string value_;
   std::string domain_;
   std::string path_;
   boost::posix_time::ptime expires_;
   SameSite sameSite_;
   bool httpOnly_;
   bool secure_;
};


} // namespace http
} // namespace core 
} // namespace rstudio


#endif // CORE_HTTP_COOKIE_HTTP
