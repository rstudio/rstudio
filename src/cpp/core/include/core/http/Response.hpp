/*
 * Response.hpp
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

#ifndef CORE_HTTP_RESPONSE_HPP
#define CORE_HTTP_RESPONSE_HPP

#include <iostream>
#include <sstream>
#include <boost/type_traits/is_same.hpp>
#include <boost/iostreams/copy.hpp>
#include <boost/iostreams/concepts.hpp>
#include <boost/iostreams/filtering_stream.hpp>

#ifndef _WIN32
#include <boost/iostreams/filter/gzip.hpp>
#endif

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include "Message.hpp"
#include "Request.hpp"
#include "Util.hpp"

namespace core {
   
class ErrorLocation;
   
namespace http {

class Cookie ;
   
namespace status {
enum Code {
   Ok = 200,
   Created = 201,
   MovedPermanently = 301,
   MovedTemporarily = 302,
   SeeOther = 303,
   NotModified = 304,
   BadRequest = 400,
   Unauthorized = 401,
   Forbidden = 403,
   NotFound = 404,
   MethodNotAllowed = 405,
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
   
class Response : public Message
{
public:
   Response();
   virtual ~Response() {}

   // COPYING: boost::noncopyable (but see explicit assign method below)

   void assign(const Response& response)
   {
      Message::assign(response);
      statusCode_ = response.statusCode_;
      statusCodeStr_ = response.statusCodeStr_;
      statusMessage_ = response.statusMessage_;
   }

public:   
   int statusCode() const { return statusCode_; }
   void setStatusCode(int statusCode) { statusCode_ = statusCode; }
   
   const std::string& statusMessage() const; 
   void setStatusMessage(const std::string& statusMessage) ;
      
   std::string contentEncoding() const;
   void setContentEncoding(const std::string& encoding);
   
   void setCacheWithRevalidationHeaders();
   void setCacheForeverHeaders();
   void setPrivateCacheForeverHeaders();
   void setNoCacheHeaders();
   
   void setChromeFrameCompatible(const Request& request);

   void addCookie(const Cookie& cookie) ;
   
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
                 std::streamsize buffSize = 128) 
   {
      try
      {
         // set exception mask (required for proper reporting of errors)
         is.exceptions(std::istream::failbit | std::istream::badbit);
         
         // setup filtering stream for writing body
         boost::iostreams::filtering_ostream filteringStream ;
         
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
         setContentLength(body_.length());
         
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

   Error setBody(const FilePath& filePath, std::streamsize buffSize = 512)
   {
      NullOutputFilter nullFilter;
      return setBody(filePath, nullFilter, buffSize);
   }
   
   template <typename Filter>
   Error setBody(const FilePath& filePath, 
                 const Filter& filter,
                 std::streamsize buffSize = 128)
   {
      // open the file
      boost::shared_ptr<std::istream> pIfs;
      Error error = filePath.open_r(&pIfs);
      if (error)
         return error;
      
      // send the file from its stream
      try
      {
         return setBody(*pIfs, filter, buffSize);
      }
      catch(const std::exception& e)
      {
         Error error = systemError(boost::system::errc::io_error,
                                   ERROR_LOCATION);
         error.addProperty("what", e.what());
         error.addProperty("path", filePath.absolutePath());
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
         setError(http::status::NotFound, request.uri() + " not found");
         return;
      }
      
      // set content type
      setContentType(filePath.mimeContentType());
      
      // gzip if possible
      if (request.acceptsEncoding(kGzipEncoding))
         setContentEncoding(kGzipEncoding);
      
      // set body from file
      Error error = setBody(filePath, filter);
      if (error)
         setError(status::InternalServerError, error.code().message());
   }
   
   void setCacheableFile(const FilePath& filePath, const Request& request)
   {
      NullOutputFilter nullFilter;
      setCacheableFile(filePath, request, nullFilter);
   }
   
   template <typename Filter>
   void setCacheableFile(const FilePath& filePath, 
                         const Request& request, 
                         const Filter& filter)
   {
      // ensure that the file exists
      if (!filePath.exists())
      {
         setError(http::status::NotFound, request.uri() + " not found");
         return;
      }
      
      // set Last-Modified
      using namespace boost::posix_time;
      ptime lastModifiedDate = from_time_t(filePath.lastWriteTime());
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
   
   // these calls do no stream io or encoding so don't return errors
   void setBodyUnencoded(const std::string& body);
   void setError(int statusCode, const std::string& message);
   void setError(const Error& error);
   
   void setMovedPermanently(const http::Request& request, const std::string& location);
   void setMovedTemporarily(const http::Request& request, const std::string& location);
   
   
private:
   virtual void appendFirstLineBuffers(
         std::vector<boost::asio::const_buffer>& buffers) const ;

   virtual void resetMembers();
      
private:
   void ensureStatusMessage() const ;
   void removeCachingHeaders();
   void setCacheForeverHeaders(bool publicAccessiblity);
   std::string eTagForContent(const std::string& content);
  
private:

   // IMPORTANT NOTE: when adding data members be sure to update
   // the implementation of the assign method!!!!!


   int statusCode_ ;
   mutable std::string statusMessage_ ;

   // string storage for integer members (need for toBuffers)
   mutable std::string statusCodeStr_ ;
};

std::ostream& operator << (std::ostream& stream, const Response& r) ;

} // namespace http
} // namespace core

#endif // CORE_HTTP_RESPONSE_HPP
