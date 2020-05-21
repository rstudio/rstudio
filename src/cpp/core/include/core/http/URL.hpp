/*
 * URL.hpp
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

#ifndef CORE_HTTP_URL_HPP
#define CORE_HTTP_URL_HPP

#include <string>
#include <iosfwd>

#include <shared_core/SafeConvert.hpp>

namespace rstudio {
namespace core {
namespace http {

// NOTE: The URL class is a part of shared endpoint and association caches
// in our open-id implemetnation. we therefore need to make sure that
// copying a URL or accessing its members never falls prey to ref-counted 
// strings which are (potentially) not threadsafe. To work around this we do
// manual assignment of all strings during copy and assignment and have 
// all accessors return copies of new strings
//
    
class URL
{
public:
   URL() {}
   URL(const std::string& absoluteURL);
   
   // implement copying and assignment to prevent any behind our back
   // ref-counting of strings (which would cause thread-safety problems)
    
   URL(const URL& rhs)
   {
      assign(rhs);
   }
   
   URL& operator=(const URL& rhs)
   {
      if (&rhs != this)
      {
         assign(rhs);
      }
      return *this;
   }
   
   std::string absoluteURL() const { return std::string(absoluteURL_.c_str()); }
   bool isValid() const { return !protocol_.empty() && !host_.empty(); }
   bool empty() const { return absoluteURL_.empty(); }

   std::string protocol() const { return std::string(protocol_.c_str()); }
   std::string host() const { return std::string(host_.c_str()); }
   std::string path() const { return std::string(path_.c_str()); }
   std::string hostname() const { return host_.substr(0, host_.find(':')); }

   int port() const
   {
      return safe_convert::stringTo(portStr(), 80);
   }

   std::string portStr() const
   {
      size_t idx = host_.find(':');
      if (idx != std::string::npos)
      {
         std::string port = host_.substr(idx + 1);
         return port;
      }
      return (protocol_ == "http") ? "80" : "443";
   }
   
   void split(std::string* pBaseURL, std::string* pQueryParams) const;
   
   bool operator < (const URL& other) const
   {
      return absoluteURL_ < other.absoluteURL_;
   }
  
   bool operator > (const URL& other) const
   {
      return absoluteURL_ > other.absoluteURL_;
   }
   
   bool operator == (const URL& other) const
   {
      return absoluteURL_ == other.absoluteURL_;
   }
   
   bool operator != (const URL& other) const
   {
      return absoluteURL_ != other.absoluteURL_;
   }
   
   static std::string cleanupPath(std::string path);
   static std::string complete(std::string absoluteUri, std::string targetUri);
   static std::string uncomplete(std::string baseUri, std::string targetUri);

private:
  
   void assign(const URL& rhs)
   {
      assign(rhs.absoluteURL_, rhs.protocol_, rhs.host_, rhs.path_);
   }
   
   void assign(const std::string& absoluteURL, 
               const std::string& protocol,
               const std::string& host,
               const std::string& path)
   {
      absoluteURL_.assign(absoluteURL.data(), absoluteURL.size());
      protocol_.assign(protocol.data(), protocol.size());
      host_.assign(host.data(), host.size());
      path_.assign(path.data(), path.size());
   }
   
   std::string absoluteURL_;
   std::string protocol_;
   std::string host_;
   std::string path_;
};
   
std::ostream& operator << (std::ostream& stream, const URL& url);
   
   

} // namespace http
} // namespace core
} // namespace rstudio


#endif // CORE_HTTP_URL_HPP
