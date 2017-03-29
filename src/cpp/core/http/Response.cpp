/*
 * Response.cpp
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

#include <core/http/Response.hpp>

#include <algorithm>

#include <boost/regex.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/asio/buffer.hpp>

#include <core/http/URL.hpp>
#include <core/http/Util.hpp>
#include <core/http/Cookie.hpp>
#include <core/Hash.hpp>
#include <core/RegexUtils.hpp>

#include <core/FileSerializer.hpp>

namespace rstudio {
namespace core {
namespace http {

Response::Response() 
   : Message(), statusCode_(status::Ok) 
{
}   
   
const std::string& Response::statusMessage() const    
{ 
   ensureStatusMessage();  
   return statusMessage_; 
} 

void Response::setStatusMessage(const std::string& statusMessage) 
{
   statusMessage_ = statusMessage ;
}
   
std::string Response::contentEncoding() const
{
   return headerValue("Content-Encoding");
}

void Response::setContentEncoding(const std::string& encoding)
{
   setHeader("Content-Encoding", encoding);
}

void Response::setCacheWithRevalidationHeaders()
{
   setHeader("Expires", http::util::httpDate());
   setHeader("Cache-Control", "public, max-age=0, must-revalidate");
}
   
void Response::setCacheForeverHeaders(bool publicAccessiblity)
{
   // set Expires header
   using namespace boost::posix_time;
   time_duration yearDuration = hours(365 * 24);
   ptime expireTime = second_clock::universal_time() + yearDuration;
   setHeader("Expires", http::util::httpDate(expireTime));
   
   // set Cache-Control header
   int durationSeconds = yearDuration.total_seconds();
   std::string accessibility = publicAccessiblity ? "public" : "private";
   std::string cacheControl(accessibility + ", max-age=" + 
                            safe_convert::numberToString(durationSeconds));
   setHeader("Cache-Control", cacheControl);   
}

void Response::setCacheForeverHeaders()
{
   setCacheForeverHeaders(true);
}
   
void Response::setPrivateCacheForeverHeaders()
{
   // NOTE: the Google article referenced above indicates that for the 
   // private scenario you should set the Expires header in the past so 
   // that HTTP 1.0 proxies never cache it. Unfortuantely when running 
   // against localhost in Firefox we observed that this prevented Firefox
   // from caching.
   setCacheForeverHeaders(false);
}

// WARNING: This appears to break IE8 if Content-Disposition: attachment
void Response::setNoCacheHeaders()
{
   setHeader("Expires", "Fri, 01 Jan 1990 00:00:00 GMT");
   setHeader("Pragma", "no-cache");
   setHeader("Cache-Control",
             "no-cache, no-store, max-age=0, must-revalidate");
}

void Response::setFrameOptionHeaders(const std::string& options)
{
   std::string option;

   if (options.empty() || options == "none")
   {
      // the default is to deny all framing
      option = "DENY";
   }
   else if (options == "same")
   {
      // this special string indicates that framing is permissible on the same
      // domain
      option = "SAMEORIGIN";
   }
   else
   {
      // the special string "any" means any origin
      if (options != "any")
         option = "ALLOW-FROM " + options;
   }

   if (!option.empty())
      setHeader("X-Frame-Options", option);
}

// mark this request's user agent compatibility
void Response::setBrowserCompatible(const Request& request)
{
   if (boost::algorithm::contains(request.userAgent(), "chromeframe"))
      setHeader("X-UA-Compatible", "chrome=1");
   else if (boost::algorithm::contains(request.userAgent(), "Trident"))
      setHeader("X-UA-Compatible", "IE=edge");
}

void Response::addCookie(const Cookie& cookie) 
{
	addHeader("Set-Cookie", cookie.cookieHeaderValue()) ;
}

   
Error Response::setBody(const std::string& content)
{
   std::istringstream is(content);
   return setBody(is);
}

Error Response::setCacheableBody(const FilePath& filePath,
                                 const Request& request)
{
   std::string content;
   Error error = core::readStringFromFile(filePath, &content);
   if (error)
      return error;

   return setCacheableBody(content, request);
}

void Response::setDynamicHtml(const std::string& html,
                              const Request& request)
{
   // dynamic html
   setContentType("text/html");
   setNoCacheHeaders();

   // gzip if possible
   if (request.acceptsEncoding(kGzipEncoding))
      setContentEncoding(kGzipEncoding);

   // set body
   setBody(html);
}

void Response::setRangeableFile(const FilePath& filePath,
                                const Request& request)
{
   // read the file in from disk
   std::string contents;
   Error error = core::readStringFromFile(filePath, &contents);
   if (error)
   {
      setError(error);
      return;
   }

   setRangeableFile(contents, filePath.mimeContentType(), request);
}

void Response::setRangeableFile(const std::string& contents,
                                const std::string& mimeType,
                                const Request& request)
{
   // set content type
   setContentType(mimeType);

   // parse the range field
   std::string range = request.headerValue("Range");
   boost::regex re("bytes=(\\d*)\\-(\\d*)");
   boost::smatch match;
   if (regex_utils::match(range, match, re))
   {
      // specify partial content
      setStatusCode(http::status::PartialContent);

      // determine the byte range
      const size_t kNone = -1;
      size_t begin = safe_convert::stringTo<size_t>(match[1], kNone);
      size_t end = safe_convert::stringTo<size_t>(match[2], kNone);
      size_t total = contents.length();

      if (end == kNone)
      {
         end = total-1;
      }
      if (begin == kNone)
      {
         begin = total - end;
         end = total-1;
      }

      // set the byte range
      addHeader("Accept-Ranges", "bytes");
      boost::format fmt("bytes %1%-%2%/%3%");
      std::string range = boost::str(fmt % begin % end % contents.length());
      addHeader("Content-Range", range);

      // always attempt gzip
      if (request.acceptsEncoding(http::kGzipEncoding))
         setContentEncoding(http::kGzipEncoding);

      // set body
      if (begin == 0 && end == (contents.length()-1))
         setBody(contents);
      else
         setBody(contents.substr(begin, end-begin+1));
   }
   else
   {
      setStatusCode(http::status::RangeNotSatisfiable);
      boost::format fmt("bytes */%1%");
      std::string range = boost::str(fmt % contents.length());
      addHeader("Content-Range", range);
   }
}
   
void Response::setBodyUnencoded(const std::string& body)
{
   removeHeader("Content-Encoding");
   body_ = body;
   setContentLength(body_.length());
}
   
   
void Response::setError(int statusCode, const std::string& message)
{
   setStatusCode(statusCode);
   removeCachingHeaders();
   setContentType("text/html");
   setBodyUnencoded(string_utils::htmlEscape(message));
}

void Response::setNotFoundError(const std::string& uri)
{
   setError(http::status::NotFound, uri + " not found");
}
   
void Response::setError(const Error& error)
{
   setError(status::InternalServerError, error.code().message());
}

namespace {

// only take up to the first newline to prevent http response split
std::string safeLocation(const std::string& location)
{
   std::vector<std::string> lines;
   boost::algorithm::split(lines,
                           location,
                           boost::algorithm::is_any_of("\r\n"));
   return lines.size() > 0 ? lines[0] : "";
}

} // anonymous namespace


void Response::setMovedPermanently(const http::Request& request,
                                   const std::string& location)
{
   std::string uri = URL::complete(request.absoluteUri(),
                                   safeLocation(location));
   setError(http::status::MovedPermanently, uri);
   setHeader("Location", uri);
}

void Response::setMovedTemporarily(const http::Request& request,
                                   const std::string& location)
{
   std::string uri = URL::complete(request.absoluteUri(),
                                   safeLocation(location));
   setError(http::status::MovedTemporarily, uri);
   setHeader("Location", uri);
}
   
void Response::resetMembers() 
{
	statusCode_ = status::Ok ;
	statusCodeStr_.clear() ;
	statusMessage_.clear() ;
}
   
void Response::removeCachingHeaders()
{
   removeHeader("Expires");
   removeHeader("Pragma");
   removeHeader("Cache-Control");
   removeHeader("Last-Modified");
   removeHeader("ETag");
}
   
std::string Response::eTagForContent(const std::string& content)
{
   return core::hash::crc32Hash(content);
}   

void Response::appendFirstLineBuffers(
      std::vector<boost::asio::const_buffer>& buffers) const 
{
	// create status code string (needs to be a member so memory is still valid
   // for use of buffers)
	std::ostringstream statusCodeStream ;
	statusCodeStream << statusCode_ ;
	statusCodeStr_ = statusCodeStream.str() ;

	// status line 
	appendHttpVersionBuffers(buffers) ;
	appendSpaceBuffer(buffers) ;
	buffers.push_back(boost::asio::buffer(statusCodeStr_)) ;
	appendSpaceBuffer(buffers) ;
	ensureStatusMessage() ;
	buffers.push_back(boost::asio::buffer(statusMessage_)) ;
}

namespace status {
namespace Message {
   const char * const SwitchingProtocols = "SwitchingProtocols";
	const char * const Ok = "OK" ;
   const char * const Created = "Created";
   const char * const PartialContent = "Partial Content";
	const char * const MovedPermanently = "Moved Permanently" ;
	const char * const MovedTemporarily = "Moved Temporarily" ;
   const char * const TooManyRedirects = "Too Many Redirects";
	const char * const SeeOther = "See Other" ;
	const char * const NotModified = "Not Modified" ;
	const char * const BadRequest = "Bad Request" ;
	const char * const Unauthorized = "Unauthorized" ;
	const char * const Forbidden = "Forbidden" ;
	const char * const NotFound = "Not Found" ;
	const char * const MethodNotAllowed = "Method Not Allowed" ;
   const char * const RangeNotSatisfiable = "Range Not Satisfyable";
	const char * const InternalServerError = "Internal Server Error" ;
	const char * const NotImplemented = "Not Implemented" ;
	const char * const BadGateway = "Bad Gateway" ;
	const char * const ServiceUnavailable = "Service Unavailable" ;
	const char * const GatewayTimeout = "Gateway Timeout" ;
} // namespace Message
} // namespace status


void Response::ensureStatusMessage() const 
{
	if ( statusMessage_.empty() )
	{
		using namespace status ;

		switch(statusCode_)
		{
         case SwitchingProtocols:
            statusMessage_ = status::Message::SwitchingProtocols;
            break;

			case Ok:
				statusMessage_ = status::Message::Ok ;
				break;

         case Created:
            statusMessage_ = status::Message::Created;
            break;

         case PartialContent:
            statusMessage_ = status::Message::PartialContent;
            break;

         case MovedPermanently:
				statusMessage_ = status::Message::MovedPermanently ;
				break;

			case MovedTemporarily:
				statusMessage_ = status::Message::MovedTemporarily ;
				break;

         case TooManyRedirects:
            statusMessage_ = status::Message::TooManyRedirects ;
            break;

			case SeeOther:
				statusMessage_ = status::Message::SeeOther ;
				break;

			case NotModified:
				statusMessage_ = status::Message::NotModified ;
				break;

			case BadRequest:
				statusMessage_ = status::Message::BadRequest ;
				break;

			case Unauthorized:
				statusMessage_ = status::Message::Unauthorized ;
				break;

			case Forbidden:
				statusMessage_ = status::Message::Forbidden ;
				break;

			case NotFound:
				statusMessage_ = status::Message::NotFound ;
				break;

			case MethodNotAllowed:
				statusMessage_ = status::Message::MethodNotAllowed ;
				break;

         case RangeNotSatisfiable:
            statusMessage_ = status::Message::RangeNotSatisfiable;
            break;

			case InternalServerError:
				statusMessage_ = status::Message::InternalServerError ;
				break;

			case NotImplemented:
				statusMessage_ = status::Message::NotImplemented ;
				break;

			case BadGateway:
				statusMessage_ = status::Message::BadGateway ;
				break;

			case ServiceUnavailable:
				statusMessage_ = status::Message::ServiceUnavailable ;
				break;

			case GatewayTimeout:
				statusMessage_ = status::Message::GatewayTimeout ;
				break;
		}
	}
}


std::ostream& operator << (std::ostream& stream, const Response& r)
{
	// output status line
	stream << "HTTP/" << r.httpVersionMajor() << "." << r.httpVersionMinor() 
          << " " << r.statusCode() << " " << r.statusMessage() 
          << std::endl ;

	// output headers and body
	const Message& m = r ;
	stream << m ;

	return stream ;
}


} // namespacc http
} // namespace core
} // namespace rstudio

