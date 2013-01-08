/*
 * Request.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef CORE_HTTP_REQUEST_HPP
#define CORE_HTTP_REQUEST_HPP

#include "Message.hpp"

#include <boost/date_time/posix_time/posix_time.hpp>

#include "Util.hpp"

namespace core {
namespace http {

class Request : public Message
{
public:
   Request() ; 
   virtual ~Request() ;
   // COPYING: boost::noncopyable

   void assign(const Request& request)
   {
      Message::assign(request);
      method_ = request.method_;
      uri_ = request.uri_;
      remoteUid_ = request.remoteUid_;
      parsedCookies_ = request.parsedCookies_;
      cookies_ = request.cookies_;
      parsedFormFields_ = request.parsedFormFields_;
      formFields_ = request.formFields_;
      files_ = request.files_;
      emptyFile_ = request.emptyFile_;
      parsedQueryParams_ = request.parsedQueryParams_;
      queryParams_ = request.queryParams_;
   }

public:
   const std::string& method() const { return method_; }
   void setMethod(const std::string& method) { method_ = method; }
   const std::string& uri() const { return uri_; }
   void setUri(const std::string& uri) { uri_ = uri; }

   std::string absoluteUri() const;
   
   bool acceptsContentType(const std::string& contentType) const;

   std::string acceptEncoding() const { return headerValue("Accept-Encoding"); }
   bool acceptsEncoding(const std::string& encoding) const;
   
   std::string host() const { return headerValue("Host"); }
   void setHost(const std::string& host) { setHeader("Host", host); }
   
   std::string userAgent() const { return headerValue("User-Agent"); }
   
   // only applies to local stream connections (returns -1 if unknown)
   int remoteUid() const { return remoteUid_; }
   
   boost::posix_time::ptime ifModifiedSince() const;
   
   std::string queryString() const;
   const Fields& queryParams() const;
   std::string queryParamValue(const std::string& name) const;
   
   template <typename T>
   T queryParamValue(const std::string& name, const T& defaultVal) const
   {
      return http::util::fieldValue(queryParams(), name, defaultVal);
   }
   
   template <typename T, typename Predicate>
   bool queryParamValue(const std::string& name, 
                        const Predicate& validator,
                        T* pValue) const
   {
      return http::util::fieldValue(queryParams(), name, validator, pValue);
   }
   

   std::string cookieValue(const std::string& name) const;
   
   const Fields& formFields() const;
   std::string formFieldValue(const std::string& name) const;
   
   template <typename T>
   T formFieldValue(const std::string& name, const T& defaultVal) const
   {
      ensureFormFieldsParsed();
      return http::util::fieldValue(formFields_, name, defaultVal);
   }
   
   template <typename T, typename Predicate>
   bool formFieldValue(const std::string& name, 
                       const Predicate& validator,
                       T* pValue) const
   {
      ensureFormFieldsParsed();
      return http::util::fieldValue(formFields_, name, validator, pValue);
   }
   
   const File& uploadedFile(const std::string& name) const;
   
   void setBody(const std::string& body);
   
   void debugPrintUri(const std::string& caption) const;

private:
   virtual void appendFirstLineBuffers(
         std::vector<boost::asio::const_buffer>& buffers) const ;
   
   virtual void resetMembers();

private:
   void ensureFormFieldsParsed() const;
   void scanHeaderForCookie(const std::string& name, 
                            const std::string& value) const;

private:

   // IMPORTANT NOTE: when adding data members be sure to update
   // the implementation of the assign method!!!!!


   std::string method_;
   std::string uri_;
   int remoteUid_;
   
   // cookies, form fields, and query string are parsed on demand
   mutable bool parsedCookies_ ;
   mutable Fields cookies_ ;
   mutable bool parsedFormFields_ ;
   mutable Fields formFields_;
   mutable Files files_;
   File emptyFile_;
   mutable bool parsedQueryParams_;
   mutable Fields queryParams_;

   friend class RequestParser ;
   friend class LocalStreamAsyncServer;
};

std::ostream& operator << (std::ostream& stream, const Request& r) ;

} // namespace http
} // namespace core

#endif // CORE_HTTP_REQUEST_HPP
