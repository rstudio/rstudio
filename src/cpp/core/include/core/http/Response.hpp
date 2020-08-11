/*
 * Response.hpp
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

#ifndef CORE_HTTP_RESPONSE_HPP
#define CORE_HTTP_RESPONSE_HPP

#include <iostream>
#include <sstream>

#include <boost/function.hpp>
#include <boost/optional.hpp>
#include <boost/type_traits/is_same.hpp>
#include <boost/make_shared.hpp>
#include <boost/iostreams/copy.hpp>
#include <boost/iostreams/concepts.hpp>
#include <boost/iostreams/filtering_stream.hpp>
#include <boost/iostreams/restrict.hpp>

#ifndef _WIN32
#include <boost/iostreams/filter/gzip.hpp>
#endif

#include <core/BrowserUtils.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileUtils.hpp>

#include "Message.hpp"
#include "Request.hpp"
#include "Util.hpp"

namespace rstudio {
namespace core {
   
class ErrorLocation;
   
namespace http {

// uri not found handler
typedef boost::function<void(const Request&, Response*)> NotFoundHandler;

class Cookie;
   
namespace status {
enum Code {
   SwitchingProtocols = 101,
   Ok = 200,
   Created = 201,
   PartialContent = 206,
   MovedPermanently = 301,
   MovedTemporarily = 302,
   SeeOther = 303,
   NotModified = 304,
   TooManyRedirects = 310,
   BadRequest = 400,
   Unauthorized = 401,
   Forbidden = 403,
   NotFound = 404,
   MethodNotAllowed = 405,
   Conflict = 409,
   RangeNotSatisfiable = 416,
   InternalServerError = 500 ,
   NotImplemented = 501, 
   BadGateway = 502,
   ServiceUnavailable = 503,
   GatewayTimeout = 504 
};
} 

class NullOutputFilter : public boost::iostreams::multichar_output_filter 
{   
public:
   template<typename Sink>
   std::streamsize write(Sink& dest, const char* s, std::streamsize n)
   {
      // this class exists only as a "null" tag for the setBody Filter
      // argument -- it should never actually be used as a filter!
      BOOST_ASSERT(false);
      return boost::iostreams::write(dest, s, n);
   }   
};

struct StreamBuffer
{
   char* data;
   size_t size;

   StreamBuffer(char* data, size_t size) :
      data(data), size(size)
   {
   }

   ~StreamBuffer()
   {
      delete [] data;
   }
};

class StreamResponse
{
public:
   virtual ~StreamResponse() {}

   virtual Error initialize() = 0;
   virtual std::shared_ptr<StreamBuffer> nextBuffer() = 0;
};

class Response : public Message
{
public:
   Response();
   virtual ~Response() {}

   // COPYING: boost::noncopyable (but see explicit assign method below)

   void assign(const Response& response,
               const Headers& extraHeaders = Headers())
   {
      Message::assign(response, extraHeaders);
      statusCode_ = response.statusCode_;
      statusCodeStr_ = response.statusCodeStr_;
      statusMessage_ = response.statusMessage_;
      streamResponse_ = response.streamResponse_;
   }

public:   
   int statusCode() const { return statusCode_; }
   void setStatusCode(int statusCode) { statusCode_ = statusCode; }
   
   const std::string& statusMessage() const;
   void setStatusMessage(const std::string& statusMessage);
      
   std::string contentEncoding() const;
   void setContentEncoding(const std::string& encoding);
   
   void setCacheWithRevalidationHeaders();
   void setCacheForeverHeaders();
   void setPrivateCacheForeverHeaders();
   void setNoCacheHeaders();

   void setFrameOptionHeaders(const std::string& options);
   
   void setBrowserCompatible(const Request& request);

   void addCookie(const Cookie& cookie);
   void clearCookies();
   Headers getCookies(const std::vector<std::string>& names = {}) const;
   
   Error setBody(const std::string& content);
   
   Error setCacheableBody(const std::string& content,
                          const Request& request)
   {
      NullOutputFilter nullFilter;
      return setCacheableBody(content, request, nullFilter);
   }
   
   template <typename Filter>
   Error setCacheableBody(const std::string& content,
                          const Request& request,
                          const Filter& filter)
   {
      // compute and set the eTag
      std::string eTag = eTagForContent(content);
      setHeader("ETag", eTag);
      
      if (eTag == request.headerValue("If-None-Match"))
      {
         removeHeader("Content-Type"); // upstream code may have set this
         setStatusCode(status::NotModified);
         return Success();
      }
      else
      {
         return setBody(content, filter);
      }
   }

   Error setCacheableBody(const FilePath& filePath, const Request& request);
   
   template <typename Filter>
   Error setBody(const std::string& content, 
                 const Filter& filter,
                 std::streamsize buffSize = 128)
   {
      std::istringstream is(content);
      return setBody(is, filter, buffSize);
   }   
      
   Error setBody(std::istream& is, std::streamsize buffSize = 128)
   {
      NullOutputFilter nullFilter;
      return setBody(is, nullFilter, buffSize);
   }
   
   template <typename Filter>
   Error setBody(std::istream& is, 
                 const Filter& filter, 
                 std::streamsize buffSize = 128,
                 bool padding = false)
   {
      try
      {
         // set exception mask (required for proper reporting of errors)
         is.exceptions(std::istream::failbit | std::istream::badbit);
         
         // setup filtering stream for writing body
         boost::iostreams::filtering_ostream filteringStream;
         
         // don't bother adding the filter if it is the NullOutputFilter
         if ( !boost::is_same<Filter, NullOutputFilter>::value )
            filteringStream.push(filter, buffSize);

         // handle gzip
         if (contentEncoding() == kGzipEncoding)
#ifdef _WIN32
            // never gzip on win32
            removeHeader("Content-Encoding");
#else
            // add gzip compressor on posix
            filteringStream.push(boost::iostreams::gzip_compressor(), buffSize);
#endif

         // buffer to write to
         std::ostringstream bodyStream;
         filteringStream.push(bodyStream, buffSize);
         
         // copy input stream
         boost::iostreams::copy(is, filteringStream, buffSize);
         
         // set body 
         body_ = bodyStream.str();

         if (padding && body_.length() < 1024)
         {
            body_ = body_ + std::string(1024 - body_.length(), ' ');
         }

         setContentLength(static_cast<int>(body_.length()));
         
         // return success
         return Success();
      }
      catch(const std::exception& e)
      {
         Error error = systemError(boost::system::errc::io_error, 
                                   ERROR_LOCATION);
         error.addProperty("what", e.what());
         return error;
      }
   }   

   void setStreamFile(const FilePath& filePath,
                      const Request& request,
                      std::streamsize buffSize = 65536);

   Error setBody(const FilePath& filePath, std::streamsize buffSize = 512)
   {
      NullOutputFilter nullFilter;
      return setBody(filePath, nullFilter, buffSize);
   }
   
   template <typename Filter>
   Error setBody(const FilePath& filePath, 
                 const Filter& filter,
                 std::streamsize buffSize = 128,
                 bool padding = false)
   {
      // open the file
      std::shared_ptr<std::istream> pIfs;
      Error error = filePath.openForRead(pIfs);
      if (error)
         return error;
      
      // send the file from its stream
      try
      {
         return setBody(*pIfs, filter, buffSize, padding);
      }
      catch(const std::exception& e)
      {
         Error error = systemError(boost::system::errc::io_error,
                                   ERROR_LOCATION);
         error.addProperty("what", e.what());
         error.addProperty("path", filePath.getAbsolutePath());
         return error;
      }
   }

   void setDynamicHtml(const std::string& html, const Request& request);
   
   void setFile(const FilePath& filePath, const Request& request)
   {
      NullOutputFilter nullFilter;
      setFile(filePath, request, nullFilter);
   }
   
   template <typename Filter>
   void setFile(const FilePath& filePath, 
                const Request& request, 
                const Filter& filter)
   {
      // ensure that the file exists
      if (!filePath.exists())
      {
         setNotFoundError(request);
         return;
      }
      
      // set content type
      setContentType(filePath.getMimeContentType());
      
      // gzip if possible
      if (request.acceptsEncoding(kGzipEncoding))
         setContentEncoding(kGzipEncoding);

      Error error = setBody(filePath, filter, 128, usePadding(request, filePath));
      if (error)
         setError(status::InternalServerError, error.getMessage());
   }

   bool usePadding(const Request& request,
                   const FilePath& filePath) const
   {
      return browser_utils::isQt(request.headerValue("User-Agent")) &&
             filePath.getMimeContentType() == "text/html";
   }
   
   /**
    * Sets the given file as the response to the request. Allows the file to be cached by the
    * browser, but ensures that the browser will check for new copies of the file every time (using
    * revalidation headers).
    *
    * @param filePath  The file to set as the response.
    * @param request   The HTTP request from the browser.
    */
   void setCacheableFile(const FilePath& filePath, const Request& request)
   {
      setCacheWithRevalidationHeaders();
      setIndefiniteCacheableFile(filePath, request);
   }

   /**
    * Sets the given file as the response to the request, filtering the file's contents through the
    * given output filter before returning the response. Allows the result to be cached by the
    * browser, but ensures that the browser will check for new copies of the file every time (using
    * revalidation headers).
    *
    * @param filePath  The file to set as the response.
    * @param request   The HTTP request from the browser.
    * @param filter    An output filter through which to process the file contents.
    */
   template <typename Filter>
   void setCacheableFile(const FilePath& filePath, 
                         const Request& request, 
                         const Filter& filter)
   {
      setCacheWithRevalidationHeaders();
      setIndefiniteCacheableFile(filePath, request, filter);
   }

   /**
    * Sets the given file as the response to the request. Allows the file to be cached by the
    * browser indefinitely. 
    *
    * @param filePath  The file to set as the response.
    * @param request   The HTTP request from the browser.
    */
   void setIndefiniteCacheableFile(const FilePath& filePath, const Request& request)
   {
      NullOutputFilter nullFilter;
      setIndefiniteCacheableFile(filePath, request, nullFilter);
   }
   
   /**
    * Sets the given file as the response to the request, filtering the file's contents through the
    * given output filter before returning the response. Allows the result to be cached by the
    * browser indefinitely. 
    *
    * @param filePath  The file to set as the response.
    * @param request   The HTTP request from the browser.
    * @param filter    An output filter through which to process the file contents.
    */
   template <typename Filter>
   void setIndefiniteCacheableFile(const FilePath& filePath, 
                                   const Request& request, 
                                   const Filter& filter)
   {
      // ensure that the file exists
      if (!filePath.exists())
      {
         setNotFoundError(request);
         return;
      }
      
      // set Last-Modified
      using namespace boost::posix_time;
      ptime lastModifiedDate = from_time_t(filePath.getLastWriteTime());
      setHeader("Last-Modified", util::httpDate(lastModifiedDate));
      
      // compare file modified time to If-Modified-Since
      if (lastModifiedDate == request.ifModifiedSince())
      {
         removeHeader("Content-Type"); // upstream code may have set this
         setStatusCode(status::NotModified);
      }
      else
      {
         setFile(filePath, request, filter);
      }
   }
   void setRangeableFile(const FilePath& filePath, const Request& request);

   void setRangeableFile(const std::string& contents,
                         const std::string& mimeType,
                         const Request& request);

   // these calls do no stream io or encoding so don't return errors
   void setBodyUnencoded(const std::string& body);
   void setError(int statusCode, const std::string& message);

   // request uri not found
   void setNotFoundError(const http::Request& request);

   // non-request uri not found
   void setNotFoundError(const std::string& uri, const http::Request& request);

   void setError(const Error& error);
   
   void setMovedPermanently(const http::Request& request, const std::string& location);
   void setMovedTemporarily(const http::Request& request, const std::string& location);
   
   void setNotFoundHandler(const NotFoundHandler& handler)
   {
      notFoundHandler_ = handler;
   }

   bool isStreamResponse() const
   {
      return static_cast<bool>(streamResponse_);
   }

   boost::shared_ptr<StreamResponse> getStreamResponse() const
   {
      return streamResponse_;
   }

private:
   virtual void appendFirstLineBuffers(
         std::vector<boost::asio::const_buffer>& buffers) const;

   virtual void resetMembers();
      
private:
   void ensureStatusMessage() const;
   void removeCachingHeaders();
   void setCacheForeverHeaders(bool publicAccessiblity);
   std::string eTagForContent(const std::string& content);
  
private:

   // IMPORTANT NOTE: when adding data members be sure to update
   // the implementation of the assign method!!!!!


   int statusCode_;
   mutable std::string statusMessage_;

   // string storage for integer members (need for toBuffers)
   mutable std::string statusCodeStr_;

   NotFoundHandler notFoundHandler_;

   boost::shared_ptr<StreamResponse> streamResponse_;
};

std::ostream& operator << (std::ostream& stream, const Response& r);

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_RESPONSE_HPP
