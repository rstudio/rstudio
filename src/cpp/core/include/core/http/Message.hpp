/*
 * Message.hpp
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

#ifndef CORE_HTTP_MESSAGE_HPP
#define CORE_HTTP_MESSAGE_HPP

#include <string>
#include <vector>
#include <algorithm>

#include <boost/bind.hpp>
#include <boost/utility.hpp>

namespace RSTUDIO_BOOST_NAMESPACE {
namespace asio {
   class const_buffer ;
}
}

#include "Header.hpp"

namespace rstudio {
namespace core {

class Error;
class FilePath;
   
namespace http {

// encodings
extern const char * const kGzipEncoding;         
   
class Response;
   
class Message : boost::noncopyable
{
public:
   Message() : httpVersionMajor_(1), httpVersionMinor_(1) {}
   virtual ~Message() {}
   // COPYING: boost::noncopyable

public:
   int httpVersionMajor() const { return httpVersionMajor_; } 
   int httpVersionMinor() const { return httpVersionMinor_; }
   void setHttpVersion(int httpVersionMajor, int httpVersionMinor) ;
   
   bool isHttp10() const 
   { 
      return httpVersionMajor() == 1 && httpVersionMinor() == 0; 
   }

   std::string contentType() const ;
   void setContentType(const std::string& contentType) ;
   
   std::size_t contentLength() const;
   void setContentLength(int contentLength);
  
   bool containsHeader(const std::string& name) const ;
   std::string headerValue(const std::string& name) const ;

   // add a header to the message
   void addHeader(const Header& header);
   void addHeader(const std::string& name, const std::string& value) ; 
   void addHeaders(const std::vector<Header>& headers);
   
   // replace the existing value of a header (won't ever insert a new header)
   void replaceHeader(const std::string& name, const std::string& value) ;

   // set the value of a header (replace existing, or add new if necessary
   void setHeaderLine(const std::string& line);
   void setHeader(const Header& header);
   void setHeader(const std::string& name, const std::string& value) ;
   void setHeader(const std::string& name, int value);

   void removeHeader(const std::string& name) ;

   const Headers& headers() const  { return headers_; }
   
   const std::string& body() const { return body_; }
   
   void reset();
   
   std::vector<boost::asio::const_buffer> toBuffers(
                              const Header& overrideHeader = Header()) const ;
   
protected:
   // body_ is protected so that sub-classes set it directly (facilitating the 
   // RVO for potentially large buffers). note this means that you MUST always
   // remember to call setContentLength after setting the body!
   std::string body_;
   
   void appendSpaceBuffer(
         std::vector<boost::asio::const_buffer>& buffers) const ;
   
   void appendHttpVersionBuffers(
         std::vector<boost::asio::const_buffer>& buffers) const ;

   void assign(const Message& message, const Headers& extraHeaders)
   {
      body_ = message.body_;
      httpVersionMajor_ = message.httpVersionMajor_;
      httpVersionMinor_ = message.httpVersionMinor_;
      headers_ = message.headers_;
      overrideHeader_ = message.overrideHeader_;
      httpVersion_ = message.httpVersion_;

      std::for_each(extraHeaders.begin(), extraHeaders.end(),
                    boost::bind(&Message::setExtraHeader, this, _1));
   }

private:
   
   virtual void appendFirstLineBuffers(
         std::vector<boost::asio::const_buffer>& buffers) const = 0;

   virtual void resetMembers() = 0;

   void setExtraHeader(const Header& header)
   {
      setHeader(header);
   }
   
private:

   // IMPORTANT NOTE: when adding data members be sure to update
   // the implementation of the assign method!!!!!


   int httpVersionMajor_;
   int httpVersionMinor_;
   std::vector<Header> headers_;
   
   // storage for override header (used by toBuffers to override a header
   // when asking for the message bytes)
   mutable Header overrideHeader_ ;
   
   // string storage for integer members (need for to_buffers)
   mutable std::string httpVersion_ ;

   // grant friendship to subclasses and parsers so they can 
   // direclty manipulate fields
   friend class Response; // done to ensure body_ can be assigned to directly
                          // with no intermediate std::string copies made
   friend class RequestParser;
   friend class ResponseParser;
};

std::ostream& operator << (std::ostream& stream, const Message& m) ;

} // namespace http
} // namespace core 
} // namespace rstudio

#endif // CORE_HTTP_MESSAGE_HPP
