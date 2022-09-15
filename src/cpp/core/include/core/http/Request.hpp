/*
 * Request.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_HTTP_REQUEST_HPP
#define CORE_HTTP_REQUEST_HPP

#include "HeaderCookieConstants.hpp"
#include "Message.hpp"
#include "Util.hpp"
#include "Cookie.hpp"

#define kRequestDefaultRootPath "/"

namespace rstudio {
namespace core {
namespace http {

enum class BaseUriUse {
   Internal, // The default, includes internal addresses
   External  // Omit internal addresses, returning blank
};

class Request : public Message
{
public:
   Request();
   virtual ~Request();
   // COPYING: boost::noncopyable

   void assign(const Request& request, const Headers& extraHeaders = Headers())
   {
      Message::assign(request, extraHeaders);
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

   // Use this URI to obtain the internally visited path
   // The path of this URI does not contain other components
   // defined during processing such as virtual or root paths
   const std::string& uri() const { return uri_; }
   void setUri(const std::string& uri) { uri_ = uri; }

   // Whether or not the request is server via HTTPS
   // either directly by the server or via a proxy
   bool isSecure() const;

   // Use the proxied URI when you need the closest guessing
   // of the address shown in the browser's address bar.
   // The host/port/protocol information returned here is always
   // correct but to get the path 100% right, a root path must
   // have been defined in the request or in an external header
   std::string proxiedUri() const;

   // The base URI will return:
   // - The internal URI when the root path is the default ("/")
   //   Empty is returned when `use = BaseUriUse::External`
   // - The proxied URI when the root path is defined as something else
   std::string baseUri(BaseUriUse use = BaseUriUse::Internal) const;

   // The path of the server as seen by the browsers, by default "/"
   // This is the path to be used for cookies or along with other path
   std::string rootPath() const;
   void setRootPath(const std::string& path) { rootPath_ = path; }
   
   // Virtual path of the request as seen by the browsers, by default empty
   // This path can be used for separating distinct contexts under a sub path
   // Note: Affects the proxied URI or the base URI (when root path is set)
   // For example, if the virtual path is `/s/1234567890` and RStudio is served on
   // `example.com` then the proxied URI could be `http://example.com/s/1234567890`
   // Implemented as a header so it carries over when proxied downstream
   std::string virtualPath() const { return headerValue(kVirtualPathHeader); }
   void setVirtualPath(const std::string& path) { setHeader(kVirtualPathHeader, path); }

   bool acceptsContentType(const std::string& contentType) const;

   std::string acceptEncoding() const { return headerValue("Accept-Encoding"); }
   bool acceptsEncoding(const std::string& encoding) const;
   
   std::string host() const { return headerValue("Host"); }
   void setHost(const std::string& host) { setHeader("Host", host); }
   
   std::string userAgent() const { return headerValue("User-Agent"); }
   
   // only applies to local stream connections (returns -1 if unknown)
   int remoteUid() const { return remoteUid_; }
   
   boost::posix_time::ptime ifModifiedSince() const;
   
   std::string path() const;

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
   std::string cookieValueFromHeader(const std::string& headerName) const;
   void addCookie(const std::string& name, const std::string& value);
   
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
   // Use the internal URI when you need the "behind the proxy" URI
   std::string internalUri() const;

   virtual void appendFirstLineBuffers(
         std::vector<boost::asio::const_buffer>& buffers) const;
   
   virtual void resetMembers();

private:
   void ensureFormFieldsParsed() const;
   void scanHeaderForCookie(const std::string& name, 
                            const std::string& value) const;

private:
   std::string rootPath_;

   // IMPORTANT NOTE: when adding data members be sure to update
   // the implementation of the assign method!!!!!


   std::string method_;
   std::string uri_;
   int remoteUid_;
   
   // cookies, form fields, and query string are parsed on demand
   mutable bool parsedCookies_;
   mutable Fields cookies_;
   mutable bool parsedFormFields_;
   mutable Fields formFields_;
   mutable Files files_;
   File emptyFile_;
   mutable bool parsedQueryParams_;
   mutable Fields queryParams_;

   friend class RequestParser;
   friend class LocalStreamAsyncServer;
};

std::ostream& operator << (std::ostream& stream, const Request& r);

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_REQUEST_HPP
