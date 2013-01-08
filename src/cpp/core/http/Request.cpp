/*
 * Request.cpp
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

#include <core/http/Request.hpp>

#include <boost/tokenizer.hpp>
#include <boost/asio/buffer.hpp>

#include <core/Log.hpp>
#include <core/Thread.hpp>

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

std::string Request::absoluteUri() const
{
   return "http://" + host() + uri();
}
   
bool Request::acceptsContentType(const std::string& contentType) const
{
   return headerValue("Accept").find(contentType) != std::string::npos;
}

bool Request::acceptsEncoding(const std::string& encoding) const
{
   // read , separated fields
   using namespace boost ;
   char_separator<char> comma(",");
   tokenizer<char_separator<char> > tokens(acceptEncoding(), comma);
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
         scanHeaderForCookie(it->name, it->value) ;
      }
      parsedCookies_ = true ;
   }

   // lookup the cookie
   return util::fieldValue(cookies_, name);
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
   
   return formFields_ ;
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
   setContentLength(body_.length());
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
   method_.clear() ;
   uri_.clear() ;
   parsedCookies_ = false ;
   cookies_.clear() ;
   parsedFormFields_ = false ;
   formFields_.clear() ;
   parsedQueryParams_ = false;
   queryParams_.clear();
}

void Request::appendFirstLineBuffers(
      std::vector<boost::asio::const_buffer>& buffers) const 
{
   using boost::asio::buffer ;
   
   // request line
   buffers.push_back(buffer(method_)) ;
   appendSpaceBuffer(buffers) ;
   buffers.push_back(buffer(uri_)) ;
   appendSpaceBuffer(buffers) ;
   appendHttpVersionBuffers(buffers) ;
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
      
      parsedFormFields_ = true ;
   }
}
   
void Request::scanHeaderForCookie(const std::string& name, 
                                  const std::string& value) const
{
   if ( name == "Cookie" )
      util::parseFields(value, ";, ", "= ", &cookies_, util::FieldDecodeNone) ;
}

std::ostream& operator << (std::ostream& stream, const Request& r)
{
   // output request line
   stream << r.method() << " " 
          << r.uri() 
          << " HTTP/" << r.httpVersionMajor() << "." << r.httpVersionMinor()
          << std::endl ;

   // output headers and body
   const Message& m = r ;
   stream << m ;

   return stream ;
}

} // namespacce http
} // namespace core

