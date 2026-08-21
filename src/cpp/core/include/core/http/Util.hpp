/*
 * Util.hpp
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

#ifndef CORE_HTTP_UTIL_HPP
#define CORE_HTTP_UTIL_HPP

#include <string>
#include <vector>
#include <map>

#include <boost/asio/buffer.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/system/error_code.hpp>

namespace rstudio {
namespace core {
   
class Error;
class FilePath;

namespace http {
      
class Request;
class Response;

typedef std::pair<std::string,std::string> Field;
typedef std::vector<Field> Fields;
   
class FieldPredicate
{
public:
   FieldPredicate(const std::string& name) 
      : name_(name) 
   {
   }
   bool operator()(const Field& field) 
   { 
      return name_.compare(field.first) == 0;
   }
private:
   std::string name_;
};
   
struct File
{
   bool empty() const { return name.empty(); }
   std::string name;
   std::string contentType;
   std::string contents;
};

typedef std::map<std::string,File> Files;
   
namespace util {
      
Fields::const_iterator findField(const Fields& fields, const std::string& name);
std::string fieldValue(const Fields& fields, const std::string& name);
   
template <typename T>
T fieldValue(const Fields& fields, const std::string& name, const T& defaultVal)
{
   Fields::const_iterator pos = findField(fields, name);
   if (pos != fields.end())
   {
      try
      {
         return boost::lexical_cast<T>(pos->second);
      }
      catch(boost::bad_lexical_cast&)
      {
         return defaultVal;
      }
   }
   else // not found, return default
   {
      return defaultVal;
   }
}

template <typename T, typename Predicate>
bool fieldValue(const Fields& fields, 
                const std::string& name, 
                const Predicate& validator,
                T* pValue)
{
   Fields::const_iterator pos = findField(fields, name);
   if (pos != fields.end())
   {
      try
      {
         *pValue = boost::lexical_cast<T>(pos->second);
         return validator(*pValue);
      }
      catch(boost::bad_lexical_cast&)
      {
         return false;
      }
   }
   else 
   {
      return false;
   }
}
   
   

enum FieldDecodeType
{
   FieldDecodeNone,
   FieldDecodeForm,
   FieldDecodeQueryString
};
   
void parseFields(const std::string& fields, 
                 const char* fieldDelim, 
                 const char* valueDelim, 
                 Fields* pFields, 
                 FieldDecodeType fieldDecode);
   
void parseForm(const std::string& body, Fields* pFields);
   
void parseMultipartForm(const std::string& contentType,
                        const std::string& body, 
                        Fields* pFields,
                        Files* pFiles);

void buildQueryString(const Fields& fields, std::string* pQueryString);
void parseQueryString(const std::string& queryString, Fields* pFields);
   
std::string urlEncode(const std::string& in, bool queryStringSpaces = false);
std::string urlDecode(const std::string& in);
   
   
boost::posix_time::ptime parseHttpDate(const std::string& date);
   
boost::posix_time::ptime parseAtomDate(const std::string& date);
   
std::string httpDate(const boost::posix_time::ptime& datetime = 
                           boost::posix_time::second_clock::universal_time());

bool isValidDate(const std::string& httpDate);


std::string pathAfterPrefix(const Request& request,
                            const std::string& pathPrefix);

core::FilePath requestedFile(const std::string& wwwLocalPath,
                             const std::string& relativePath);

void fileRequestHandler(const std::string& wwwLocalPath,
                        const std::string& baseUri,
                        const core::http::Request& request,
                        core::http::Response* pResponse);

std::string formatMessageAsHttpChunk(const std::string& message);

// determines if the given string is a well-formed IP address
bool isIpAddress(const std::string& addr);

// determines if the given string is a network address by
// querying the DNS system
bool isNetworkAddress(const std::string& str);

// determines if the given request is request to upgrade the connection to a websocket
bool isWSUpgradeRequest(const Request& request);

// does the given error represent SSL truncation/shutdown?
bool isSslShutdownError(const boost::system::error_code& code);

#ifndef _WIN32

bool isSslCertificateVerifyFailedError(const rstudio::core::Error& error);

#endif

std::string addQueryParam(const std::string& uri,
                          const std::string& queryParam);

// Is this string usable as the Path attribute of a Set-Cookie header?
// Requires an origin-absolute path made only of characters that cannot break
// out of the header (no CTLs -- including CR/LF -- and no space, ';', ',',
// '\', '?', '#', or non-ASCII), and with no "//", "." or ".." segments. A
// browser compares the path against a request path whose dot segments have
// already been removed, so "." and ".." could never match; an empty segment
// would match, but only ever arrives from something upstream that failed to
// normalize, so it is refused as well. Use this before putting any
// client-reported value in a cookie path.
bool isValidCookiePath(const std::string& path);

// The path component of a base URL reported by the client, normalized to a
// single trailing slash, or empty when there is none or it could not be used
// as a cookie path. Accepts an absolute URL or an origin-absolute path.
// Use this to compare a client-reported base against a server-derived cookie
// path: a base that does not end with that path is not one the session is
// served under.
std::string cookiePathFromClientBaseUrl(const std::string& clientBaseUrl);

// Prefix a server-derived cookie path with an external proxy prefix reported
// by the client. When the browser reaches the server through a path-prefixing
// reverse proxy the server was never told about, the browser-visible path is
// <prefix> + <serverPath>; a cookie scoped to serverPath alone is then never
// sent. clientBaseUrl is the client's own view of its base URL (absolute URL
// or origin-absolute path). If its path ends with serverPath, the external
// prefix is prepended to serverPath (preserving serverPath's exact trailing
// slash style); in every other case -- including when the two paths are equal,
// when clientBaseUrl is empty or malformed, when it fails isValidCookiePath,
// or when serverPath is not itself origin-absolute -- serverPath is returned
// unchanged, so deployments without an unknown prefix keep byte-identical
// cookie paths.
//
// The result is never broader than serverPath, but it is a sibling subtree
// rather than a subpath of it, so callers must trust clientBaseUrl: a caller
// that accepts it from an unauthenticated source lets a crafted value scope
// the cookie to a route of the attacker's choosing on the same host.
std::string cookiePathWithExternalPrefix(const std::string& serverPath,
                                         const std::string& clientBaseUrl);

} // namespace util

} // namespace http
} // namespace core 
} // namespace rstudio

#endif // CORE_HTTP_UTIL_HPP
