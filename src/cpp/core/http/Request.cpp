/*
 * Request.cpp
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

#include <core/http/Request.hpp>
#include <core/http/URL.hpp>

#include <gsl/gsl>

#include <boost/regex.hpp>
#include <boost/tokenizer.hpp>
#include <boost/asio/buffer.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/join.hpp>

#include <core/Log.hpp>
#include <core/Thread.hpp>

namespace rstudio {
namespace core {
namespace http {

Request::Request() 
   : remoteUid_(-1),
     parsedCookies_(false), 
     parsedFormFields_(false), 
     parsedQueryParams_(false)
{
}

Request::~Request()
{
}

bool Request::isSecure() const
{
   // the request is secure if the browser is using HTTPS to
   // reach the server which may depend on the presence of a proxy
   return URL(proxiedUri()).protocol() == "https";
}

std::string Request::baseUri(BaseUriUse use /*= BaseUriUse::Internal*/) const
{
   // When the root path is defined as other than the default ("/")
   // we can guess with precision the actual URL show by the browser's
   // address bar when we return the proxied URI. Without it either
   // it doesn't matter because there's no proxy and the internal
   // URI is the same address visible in the browser or the proxy is
   // taking care of the path-rewrite which is something we asked
   // customers to do before v1.4
   if (rootPath() != kRequestDefaultRootPath)
      return proxiedUri();
   if (use == BaseUriUse::Internal)
      return internalUri();
   // When BaseUriUse::External, we omit the internal URI
   return "";
}

std::string Request::internalUri() const
{
   // ignore the proxy for the most part except by the scheme
   std::string scheme = URL(proxiedUri()).protocol();
   return scheme + "://" + host() + uri();
}

std::string Request::rootPath() const
{
   // if there's no proxy defining the header resort
   // to the externally defined value for root path
   std::string rootPathHeader = headerValue("X-RStudio-Root-Path");
   if (rootPathHeader == "")
   {
      rootPathHeader = rootPath_;
   }

   // be sure the root path start with slash but doesn't end with one (unless literally only "/")
   if (rootPathHeader.empty() || rootPathHeader[0] != '/')
      rootPathHeader = '/' + rootPathHeader;
   if (rootPathHeader.length() > 1 && rootPathHeader[rootPathHeader.length() - 1] == '/')
      rootPathHeader = rootPathHeader.substr(0, rootPathHeader.length() - 1);

   return rootPathHeader;
}

std::string Request::proxiedUri() const
{
   // if using the product-specific header use it
   // it should contain the exact scheme/host/port/path
   // of the original request as send by the browser
   std::string overrideHeader = headerValue("X-RStudio-Request");
   if (!overrideHeader.empty())
   {
      return overrideHeader;
   }

   std::string root = rootPath();

   // multi-session includes additional path elements in the URL
   // this should show up only on internal requests
   std::string sessionContextHeader = headerValue("X-RStudio-SessionContext");
   if (sessionContextHeader != "")
   {
      root += sessionContextHeader;
   }

   // might be using new Forwarded header
   std::string forwarded = headerValue("Forwarded");
   if (!forwarded.empty())
   {
      std::string forwardedHost;
      boost::regex reHost("host=([\\w.:]+);?");
      boost::smatch matches;
      if (boost::regex_search(forwarded, matches, reHost))
         forwardedHost = matches[1];

      std::string protocol = "http";
      boost::regex reProto("proto=([\\w.:]+);?");
      if (boost::regex_search(forwarded, matches, reProto))
         protocol = matches[1];

      return URL::complete(protocol + "://" + forwardedHost, root + '/' + uri());
   }

   // get the protocol that was specified in the request
   // it might have been specified by rserver-http w/ ssl-enabled=1
   std::string protocol = headerValue("X-Forwarded-Proto");
   if (protocol.empty())
   {
      protocol = "http";
   }

   // might be using the legacy X-Forwarded headers
   std::string forwardedHost = headerValue("X-Forwarded-Host");
   if (!forwardedHost.empty())
   {
      // get the port that may be specified in the request
      std::string port = headerValue("X-Forwarded-Port");
      if (!port.empty())
      {
         std::size_t pos = forwardedHost.find(':');
         if (pos == std::string::npos)
         {
            forwardedHost += ":" + port;
         }
         else
         {
            forwardedHost = forwardedHost.substr(0, pos + 1) + port;
         }
      }

      return URL::complete(protocol + "://" + forwardedHost, root + '/' + uri());
   }

   // use the protocol that may have been set by X-Forwarded-Proto
   return URL::complete(protocol + "://" + host(), root + '/' + uri());
}

bool Request::acceptsContentType(const std::string& contentType) const
{
   return headerValue("Accept").find(contentType) != std::string::npos;
}

bool Request::acceptsEncoding(const std::string& encoding) const
{
   // read , separated fields
   using namespace boost;
   char_separator<char> comma(", ");
   std::string accepted = acceptEncoding();
   tokenizer<char_separator<char>> tokens(accepted, comma);
   return std::find(tokens.begin(), tokens.end(), encoding) != tokens.end();
}
   
boost::posix_time::ptime Request::ifModifiedSince() const
{
   using namespace boost::posix_time;
   
   std::string modifiedSinceDate = headerValue("If-Modified-Since");
   if (!modifiedSinceDate.empty())
   {
      return util::parseHttpDate(modifiedSinceDate);
   }
   else
   {
      return ptime(not_a_date_time);
   }
      
}

std::string Request::path() const
{
   std::string::size_type pos = uri().find('?');
   if (pos != std::string::npos)
      return uri().substr(0, pos);
   else
      return uri();
}

std::string Request::queryString() const
{
   // find ? in uri()
   std::string::size_type pos = uri().find('?');
   if (pos != std::string::npos)
   {
      std::string::size_type qsPos = pos + 1;
      if (uri().length() > qsPos)
         return uri().substr(qsPos);
      else
         return std::string();
   }
   else
   {
      return std::string();
   }
}
   
const Fields& Request::queryParams() const
{
   if (!parsedQueryParams_)
   {
      util::parseQueryString(queryString(), &queryParams_);
      parsedQueryParams_ = true;
   }
   
   return queryParams_;
}
   
std::string Request::cookieValue(const std::string& name) const
{
   // parse cookies on demand
   if ( !parsedCookies_ )
   {
      for (Headers::const_iterator it =
            headers().begin(); it != headers().end(); ++it )
      {
         scanHeaderForCookie(it->name, it->value);
      }
      parsedCookies_ = true;
   }

   // lookup the cookie
   std::string cookie = util::fieldValue(cookies_, name);

   // when embedded into an iFrame a legacy cookie
   // may be present for old, non-conforming browsers
   if (cookie.empty())
   {
      cookie = util::fieldValue(cookies_, name + kLegacyCookieSuffix);
   }
   return cookie;
}

void Request::addCookie(const std::string& name, const std::string& value)
{
   cookies_.push_back(std::make_pair(name, value));
   std::vector<std::string> cookies;
   for (const auto& cookie : cookies_)
   {
      cookies.push_back(cookie.first + "=" + cookie.second);
   }
   setHeader("Cookie", boost::algorithm::join(cookies, "; "));
}

std::string Request::cookieValueFromHeader(const std::string& headerName) const
{
   std::string value = headerValue(headerName);

   Fields cookie;
   util::parseFields(value, ";, ", "= ", &cookie, util::FieldDecodeNone);

   if (cookie.size() > 0)
      return cookie.at(0).second;
   else
      return std::string();
}

std::string Request::formFieldValue(const std::string& name) const 
{
   ensureFormFieldsParsed();
   
   // lookup the form field
   return util::fieldValue(formFields_, name);
}
   
const Fields& Request::formFields() const
{
   ensureFormFieldsParsed();
   
   return formFields_;
}
   
const File& Request::uploadedFile(const std::string& name) const
{
   ensureFormFieldsParsed();
   
   // lookup the file
   for (Files::const_iterator it = files_.begin(); it != files_.end(); ++it)
   {
      if (it->first == name)
         return it->second;
   }
   
   // not found
   return emptyFile_;
}
   
std::string Request::queryParamValue(const std::string& name) const
{
   // lookup the query param
   return util::fieldValue(queryParams(), name);
}
   
void Request::setBody(const std::string& body)
{
   body_ = body;
   setContentLength(gsl::narrow_cast<int>(body_.length()));
}
   
void Request::debugPrintUri(const std::string& caption) const
{
   static boost::mutex printMutex;
   LOCK_MUTEX(printMutex)
   {
      std::cerr << caption << ": " << uri() << std::endl;
   }
   END_LOCK_MUTEX
}

void Request::resetMembers()
{
   method_.clear();
   uri_.clear();
   parsedCookies_ = false;
   cookies_.clear();
   parsedFormFields_ = false;
   formFields_.clear();
   parsedQueryParams_ = false;
   queryParams_.clear();
}

void Request::appendFirstLineBuffers(
      std::vector<boost::asio::const_buffer>& buffers) const 
{
   using boost::asio::buffer;
   
   // request line
   buffers.push_back(buffer(method_));
   appendSpaceBuffer(buffers);
   buffers.push_back(buffer(uri_));
   appendSpaceBuffer(buffers);
   appendHttpVersionBuffers(buffers);
}

void Request::ensureFormFieldsParsed() const
{
   // parase form fields on demand
   if ( !parsedFormFields_ )
   {
      std::string contentType = headerValue("Content-Type");
      if (contentType == "application/x-www-form-urlencoded")  
      {
         util::parseFields(body(), 
                           "&", 
                           "=", 
                           &formFields_, 
                           util::FieldDecodeForm);
      }
      else if (contentType.find("multipart/form-data") == 0)
      {
         util::parseMultipartForm(contentType, body(), &formFields_, &files_);
      }
      else
      {
         // no form fields available
      }
      
      parsedFormFields_ = true;
   }
}
   
void Request::scanHeaderForCookie(const std::string& name, 
                                  const std::string& value) const
{
   if (boost::iequals(name, "cookie"))
      util::parseFields(value, ";, ", "= ", &cookies_, util::FieldDecodeNone);
}

std::ostream& operator << (std::ostream& stream, const Request& r)
{
   // output request line
   stream << r.method() << " " 
          << r.uri() 
          << " HTTP/" << r.httpVersionMajor() << "." << r.httpVersionMinor()
          << std::endl;

   // output headers and body
   const Message& m = r;
   stream << m;

   return stream;
}

} // namespacce http
} // namespace core
} // namespace rstudio

