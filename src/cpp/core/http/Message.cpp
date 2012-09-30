/*
 * Message.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/http/Message.hpp>

#include <algorithm>

#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/function.hpp>
#include <boost/asio/buffer.hpp>

#include <core/SafeConvert.hpp>

namespace core {
namespace http {
  
// encodings
const char * const kGzipEncoding = "gzip";     
   
void Message::setHttpVersion(int httpVersionMajor, int httpVersionMinor) 
{
   httpVersionMajor_ = httpVersionMajor ;
   httpVersionMinor_ = httpVersionMinor ;
}

void Message::setContentType(const std::string& contentType) 
{
   setHeader("Content-Type", contentType) ;
}

std::string Message::contentType() const 
{
   return headerValue("Content-Type") ;
}
   
void Message::setContentLength(int contentLength)
{
   setHeader("Content-Length", contentLength);
}
   
void Message::addHeader(const std::string& name, const std::string& value) 
{
   Header header ;
   header.name = name ;
   header.value = value ;
   addHeader(header);
}
   
void Message::addHeader(const Header& header)
{
   headers_.push_back(header);
}
   
void Message::addHeaders(const std::vector<Header>& headers)
{
   std::copy(headers.begin(), headers.end(), std::back_inserter(headers_));
}

std::string Message::headerValue(const std::string& name) const
{
   return http::headerValue(headers_, name);
}

bool Message::containsHeader(const std::string& name) const
{
   return http::containsHeader(headers_, name);
}
   
void Message::setHeaderLine(const std::string& line)
{
   Header header;
   if (http::parseHeader(line, &header))
      setHeader(header);
}
   
void Message::setHeader(const Header& header)
{
   setHeader(header.name, header.value);
}

void Message::setHeader(const std::string& name, const std::string& value) 
{
   Headers::iterator it = std::find_if(headers_.begin(), 
                                       headers_.end(),
                                       HeaderNamePredicate(name));
   if ( it != headers_.end() )
   {
      Header hdr ;
      hdr.name = name ;
      hdr.value = value ;
      *it = hdr ;
   }
   else
   {
      addHeader(name, value) ;
   }
}
   
void Message::setHeader(const std::string& name, int value)
{
   setHeader(name, safe_convert::numberToString(value));
}

void Message::replaceHeader(const std::string& name, const std::string& value) 
{
   Header hdr ;
   hdr.name = name ;
   hdr.value = value ; 
   std::replace_if(headers_.begin(), 
                   headers_.end(), 
                   HeaderNamePredicate(name), 
                   hdr) ;
}


void Message::removeHeader(const std::string& name) 
{
   headers_.erase(std::remove_if(headers_.begin(), 
                                 headers_.end(),
                                 HeaderNamePredicate(name)), 
                  headers_.end())  ;
}
 
   
void Message::reset() 
{
   setHttpVersion(1,1) ;
   httpVersion_.clear() ;
   headers_.clear() ;
   body_.clear() ;
   
   // allow additional reseting by subclasses
   resetMembers() ;
}

namespace { 
const char Space[] = { ' ' } ;
const char HeaderSeparator[] = { ':', ' ' } ;
const char CrLf[] = { '\r', '\n' } ;
   
void appendHeader(const Header& header,
                  std::vector<boost::asio::const_buffer>* pBuffers)
{
   pBuffers->push_back(boost::asio::buffer(header.name)) ;
   pBuffers->push_back(boost::asio::buffer(HeaderSeparator)) ;
   pBuffers->push_back(boost::asio::buffer(header.value)) ;
   pBuffers->push_back(boost::asio::buffer(CrLf)) ;   
}

}

std::vector<boost::asio::const_buffer> Message::toBuffers(
                                          const Header& overrideHeader) const
{  
   // buffers to return
   std::vector<boost::asio::const_buffer> buffers ;

   // call subclass to append first line
   appendFirstLineBuffers(buffers) ;
   buffers.push_back(boost::asio::buffer(CrLf)) ;

   // copy override header (for stable storage)
   overrideHeader_ = overrideHeader;
   
   // headers
   for (Headers::const_iterator 
        it = headers_.begin(); it != headers_.end(); ++it)
   {
      // add the header if it isn't being overriden
      if (it->name != overrideHeader_.name)
         appendHeader(*it, &buffers);
   }
   
   // add override header
   if (!overrideHeader_.empty())
      appendHeader(overrideHeader_, &buffers);

   // empty line
   buffers.push_back(boost::asio::buffer(CrLf)) ;

   // body
   buffers.push_back(boost::asio::buffer(body_)) ;

   // return the buffers
   return buffers ;
}

   
void Message::appendSpaceBuffer(
                     std::vector<boost::asio::const_buffer>& buffers) const
{
   buffers.push_back(boost::asio::buffer(Space)) ;
}

void Message::appendHttpVersionBuffers(
      std::vector<boost::asio::const_buffer>& buffers) const
{
   std::ostringstream httpVersionStream;
   httpVersionStream << "HTTP/" << httpVersionMajor_ << "." << httpVersionMinor_ ;
   httpVersion_ = httpVersionStream.str() ;
   buffers.push_back(boost::asio::buffer(httpVersion_)) ;
}


std::ostream& operator << (std::ostream& stream, const Message& m)
{
   // headers
   for (Headers::const_iterator it = 
         m.headers().begin(); it != m.headers().end(); ++ it)
   {
      stream << it->name << ": " << it->value << std::endl ;
   }

   // empty line
   stream << std::endl ;

   // body
   stream << m.body() << std::endl ;

   return stream ;
}


} // namespace http
} // namespace core


