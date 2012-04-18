/*
 * Cookie.hpp
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

#ifndef CORE_HTTP_COOKIE_HPP
#define CORE_HTTP_COOKIE_HPP

#include <string>

#include <boost/date_time/gregorian/gregorian.hpp>
#include "Request.hpp"

namespace core {
namespace http {

class Cookie
{
public:
   Cookie(const Request& request,
          const std::string& name,
          const std::string& value, 
          const std::string& path,
          bool httpOnly = false) ;
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

   void setExpires(const boost::gregorian::date& expires) { expires_ = expires; }
   void setExpires(const boost::gregorian::days& expiresDays) ;
   void setExpiresDelete() ;
   const boost::gregorian::date& expires() const { return expires_; }
   
   void setHttpOnly();

   std::string cookieHeaderValue() const ;

private:
   std::string name_ ;
   std::string value_ ;
   std::string domain_ ;
   std::string path_ ;
   boost::gregorian::date expires_ ;
   bool httpOnly_;
};


} // namespace http
} // namespace core 


#endif // CORE_HTTP_COOKIE_HTTP
