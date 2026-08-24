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

#include <cstddef>
#include <string>
#include <vector>
#include <map>

#include <boost/asio/buffer.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/system/error_code.hpp>

#include <core/http/Header.hpp>

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

// Number of bytes formatMessageAsHttpChunk() will produce for a message of
// the given length, computed arithmetically rather than by formatting it.
//
// Exists so a caller deciding whether an enveloped piece *fits* somewhere can
// ask before paying to build it -- FixedBufferProxy runs this check once per
// body piece on the streaming path, and declines-then-redelivers pieces under
// backpressure, so formatting first would build and discard the envelope on
// every declined attempt. Kept beside formatMessageAsHttpChunk() (and pinned
// by a test) because a proxy sizing its write buffer with one and filling it
// with the other needs the two to agree exactly.
std::size_t httpChunkSize(std::size_t messageSize);

std::string formatMessageAsHttpChunk(const std::string& message);

// Strips the headers a proxy must never forward end-to-end unmodified: the
// fixed RFC 7230 6.1 hop-by-hop set (Connection, Proxy-Connection, Keep-Alive,
// Proxy-Authenticate, Proxy-Authorization, TE, Trailer, Transfer-Encoding,
// Upgrade), plus any additional header names nominated by any of the
// response's Connection header field(s). Matches Go's
// net/http/httputil.removeHopByHopHeaders. Callers that need to add their own
// headers back afterward (e.g. re-stamping a cookie) must do so only *after*
// calling this, so a malicious or misbehaving upstream can't use Connection
// to nominate away headers the caller re-adds.
void removeHopByHopHeaders(Response* pResponse);

// The transfer codings a message declares, parsed from its Transfer-Encoding
// header field(s) per RFC 7230 3.3.1. Produced by parseTransferEncoding().
struct TransferEncoding
{
   // at least one non-empty transfer-coding was declared
   bool present = false;

   // the *last* coding in the list is "chunked", i.e. the body on the wire is
   // chunk-framed. This -- not "the field equals the string chunked" -- is what
   // decides whether a body needs de-chunking, since "gzip, chunked" is also
   // chunk-framed and "Chunked" is the same coding as "chunked".
   bool chunkedIsFinal = false;

   // True when the declared codings are ones this process can actually undo:
   // either none at all, or exactly one "chunked" applied last. Everything else
   // leaves bytes that are still transfer-encoded after we have done all we can
   // -- a coding we cannot decode ("gzip, chunked"), chunked applied before
   // some other coding ("chunked, gzip"), or chunked applied more than once
   // ("chunked, chunked", which RFC 7230 3.3.1 forbids a sender from producing
   // at all). Handing those on as if they were the decoded payload corrupts the
   // response, so callers must refuse the message instead.
   //
   // This verdict belongs here rather than being reassembled from the flags
   // above at each call site: rebuilding the same judgement in more than one
   // place is what let those places disagree in the first place.
   bool isDecodable = true;
};

// Parses a message's Transfer-Encoding header field(s) into the summary above.
// Handles what a bare string comparison against "chunked" does not: the field
// is a comma-separated list (1#transfer-coding), may appear more than once,
// coding names are case-insensitive, and each may carry parameters. The
// obsolete "identity" coding (dropped as a transfer coding in RFC 7230 4) is
// ignored rather than reported as a coding we cannot handle.
//
// Callers on both sides of a body handoff must use this rather than comparing
// the raw field themselves: two independent comparisons that disagree about
// whether a body is chunked is what lets an already-chunked body be chunked a
// second time, which RFC 7230 3.3.1 forbids outright.
TransferEncoding parseTransferEncoding(const Headers& headers);


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

// Add a leading slash and remove a trailing one, unless the path is just "/".
// Request::rootPath() does this to whatever it reads, so code using the
// configured root path directly must do the same before comparing the two.
std::string normalizeRootPath(const std::string& rootPath);

// Is this usable as the Path attribute of a Set-Cookie header?
//
// Requires a path starting with "/", with nothing in it that could break out
// of the header (CTLs, space, ';', '\', '?', '#', non-ASCII) and no "." or
// ".." segments, which a browser would never match. "//" is allowed; browsers
// really do send it. Call this before putting any client-reported value in a
// cookie path.
bool isValidCookiePath(const std::string& path);

} // namespace util

} // namespace http
} // namespace core 
} // namespace rstudio

#endif // CORE_HTTP_UTIL_HPP
