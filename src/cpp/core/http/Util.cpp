/*
 * Util.cpp
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


#include <core/http/Util.hpp>

#include <ios>
#include <iostream>
#include <sstream>
#include <algorithm>

#include <boost/asio.hpp>
#include <boost/tokenizer.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/regex.hpp>
#include <boost/date_time/gregorian/gregorian.hpp>

#include <core/http/URL.hpp>
#include <core/http/Header.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/RegexUtils.hpp>
#include <core/system/System.hpp>

#include <core/http/BoostAsioSsl.hpp>

namespace rstudio {
namespace core {
namespace http {

namespace util {

   
Fields::const_iterator findField(const Fields& fields, const std::string& name)
{
   return std::find_if(fields.begin(), fields.end(), FieldPredicate(name));
}
   
std::string fieldValue(const Fields& fields, const std::string& name)
{
   Fields::const_iterator pos = findField(fields, name);
   if (pos != fields.end())
      return pos->second;
   else
      return std::string();
}
   
void parseFields(const std::string& fields, 
                 const char* fieldDelim, 
                 const char* valueDelim,
                 Fields* pFields, 
                 FieldDecodeType fieldDecode)
{
   // enable straightforward references to tokenizer class & helpers
   using namespace boost;

   // delimiters
   char_separator<char> fieldSeparator(fieldDelim);
   char_separator<char> valueSeparator(valueDelim);

   // iterate over the fields
   tokenizer<char_separator<char> > fieldTokens(fields, fieldSeparator);
   for (tokenizer<char_separator<char> >::iterator 
         fieldIter = fieldTokens.begin();
         fieldIter != fieldTokens.end();
         ++fieldIter)
   {
      // split into name and value
      std::string name;
      std::string value;
      tokenizer<char_separator<char> > valTokens(*fieldIter, valueSeparator);
      tokenizer<char_separator<char> >::iterator valIter = valTokens.begin();

      if ( valIter != valTokens.end() )
         name = *valIter++;
      if ( valIter != valTokens.end() )
         value = *valIter;

      if ( fieldDecode != FieldDecodeNone )
      {
         name = util::urlDecode(name);
         value = util::urlDecode(value);
      }

      if ( !name.empty() )
         pFields->push_back(std::make_pair(name,value));
   }
}
   
void buildQueryString(const Fields& fields, std::string* pQueryString)
{
   pQueryString->clear();
   
   for (Fields::const_iterator it = fields.begin();
        it != fields.end();
        ++it)
   {
      std::string encodedKey = urlEncode(it->first, true);
      pQueryString->append(encodedKey);
      pQueryString->append("=");
      std::string encodedValue = urlEncode(it->second, true);
      pQueryString->append(encodedValue);
      pQueryString->append("&");
   }
   
   // remove trailing &
   if (!pQueryString->empty())
      pQueryString->erase(pQueryString->length()-1);
}
   
void parseForm(const std::string& body, Fields* pFields)
{
   return parseFields(body, "&", "=", pFields, FieldDecodeForm);
}
   
   
void parseQueryString(const std::string& queryString, Fields* pFields)
{
   return parseFields(queryString, "&", "=", pFields, FieldDecodeQueryString);
}
      
void parseMultipartForm(const std::string& contentType,
                        const std::string& body, 
                        Fields* pFields,
                        Files* pFiles)
{
   // get the boundary token
   std::string boundaryPrefix("boundary=");
   std::string boundary;
   size_t prefixLoc = contentType.find(boundaryPrefix);
   if (prefixLoc != std::string::npos)
   {
      boundary = contentType.substr(prefixLoc+boundaryPrefix.size(),
                                    std::string::npos);
      boundary = "--" + boundary;
      boost::algorithm::trim(boundary);
   }
   
   // extract the fields
   size_t beginBoundaryLoc = body.find(boundary);
   size_t endBoundaryLoc = body.find("\r\n" + boundary,
                                     beginBoundaryLoc+boundary.size());
   while (endBoundaryLoc != std::string::npos)
   {
      // extract the part into a string stream
      size_t beginPart = beginBoundaryLoc + boundary.size();
      size_t partLength = endBoundaryLoc - beginPart;
      std::istringstream partStream(body.substr(beginPart, partLength));
      partStream.unsetf(std::ios::skipws);
    
      // read the headers
      Headers headers;
      http::parseHeaders(partStream, &headers);
      
      // check for content-disposition
      std::string cDisp = http::headerValue(headers,"Content-Disposition");
      if (!cDisp.empty())
      {
         // parse values out of content disposition
         std::string nameRegex("form-data; name=\"(.*)\"");
         boost::smatch nameMatch;
         if (regex_utils::match(cDisp, nameMatch, boost::regex(nameRegex)))
         {
            // read the rest of the stream
            std::ostringstream valueStream;
            std::copy(std::istream_iterator<char>(partStream),
                      std::istream_iterator<char>(),
                      std::ostream_iterator<char>(valueStream));
                       
            // check for filename
            std::string filenameRegex(nameRegex + "; filename=\"(.*)\"");
            boost::smatch fileMatch;
            if (regex_utils::match(cDisp, fileMatch, boost::regex(filenameRegex)))
            {
               std::string name(fileMatch[1]);
               
               File uploadedFile;
               uploadedFile.name = fileMatch[2];
               uploadedFile.contentType = http::headerValue(headers, 
                                                            "Content-Type");
               if (uploadedFile.contentType.empty())
                  uploadedFile.contentType = "application/octet-stream";
               
               uploadedFile.contents = valueStream.str();
               pFiles->insert(std::make_pair(name, uploadedFile));
            }
            // else process regular form field
            else
            {
               std::string name(nameMatch[1]);
               std::string value = valueStream.str();
               boost::algorithm::trim(value);
               pFields->push_back(std::make_pair(name, value));
            }
         }
         
      }
                
      // next boundary
      beginBoundaryLoc = endBoundaryLoc;
      endBoundaryLoc = body.find("\r\n" + boundary,
                                 beginBoundaryLoc+boundary.size());
   }
}   
   

std::string urlEncode(const std::string& in, bool queryStringSpaces)
{
   std::string encodedURL;
      
   size_t inputLength = in.length();
   for (size_t i=0; i<inputLength; i++)
   {
      char ch = in[i];
      
      if ( ('0' <= ch && ch <= '9') ||
           ('a' <= ch && ch <= 'z') ||
           ('A' <= ch && ch <= 'Z') ||
           (ch=='~' || ch=='!' || ch=='*' || ch=='(' || ch==')' || ch=='\'' ||
            ch=='.' || ch=='-' || ch=='_') )
      {
         encodedURL += ch;
      }
      else if ((ch == ' ') && queryStringSpaces)
      {
         encodedURL += '+';
      }
      else
      {
         std::ostringstream ostr;
         ostr << "%";
         ostr << std::setw(2) << std::setfill('0') << std::hex << std::uppercase
              << (int)(boost::uint8_t)ch;
         std::string charAsHex = ostr.str();
         encodedURL += charAsHex;
      }
   }
   
   return encodedURL;
}
   
std::string urlDecode(const std::string& in)
{
   std::string out;
   out.reserve(in.size());
   for (std::size_t i = 0; i < in.size(); ++i)
   {
    if (in[i] == '%')
    {
      if (i + 3 <= in.size())
      {
        int value;
        std::istringstream is(in.substr(i + 1, 2));
        if (is >> std::hex >> value)
        {
          out += static_cast<char>(value);
          i += 2;
        }
        else
        {
          out = in; // no decode performed
          return out;
        }
      }
      else
      {
         out = in; // no decode performned
         return out;
      }
    }
    else if (in[i] == '+')
    {
      out += ' ';
    }
    else
    {
      out += in[i];
    }
   }
   return out;
}
   
namespace {

const char * const kHttpDateFormat = "%a, %d %b %Y %H:%M:%S GMT";
const char * const kAtomDateFormat = "%Y-%m-%dT%H:%M:%S%F%Q";

// facet for http date (construct w/ a_ref == 1 so we manage memory)
// statically initialized because init is very expensive
boost::posix_time::time_facet s_httpDateFacet(kHttpDateFormat,
                                              boost::posix_time::time_facet::period_formatter_type(),
                                              boost::posix_time::time_facet::special_values_formatter_type(),
                                              boost::posix_time::time_facet::date_gen_formatter_type(),
                                              1);
   
boost::posix_time::ptime parseDate(const std::string& date, const char* format)
{
   using namespace boost::posix_time;
   
   // facet for date (construct w/ a_ref == 1 so we manage memory)
   time_input_facet dateFacet(1);
   dateFacet.format(format);
   
   // parse from string
   std::stringstream dateStream;
   dateStream.str(date);
   dateStream.imbue(std::locale(dateStream.getloc(), &dateFacet));
   ptime posixDate(not_a_date_time);
   dateStream >> posixDate;
   return posixDate;
}   

}
   
boost::posix_time::ptime parseAtomDate(const std::string& date)   
{
   return parseDate(date, kAtomDateFormat);
}
   

boost::posix_time::ptime parseHttpDate(const std::string& date)
{
   return parseDate(date, kHttpDateFormat);
}
   
std::string httpDate(const boost::posix_time::ptime& datetime)
{
   using namespace boost::posix_time;
   
   // output and return the date
   std::ostringstream dateStream;
   dateStream.imbue(std::locale(dateStream.getloc(), &s_httpDateFacet));
   dateStream << datetime;
   return dateStream.str();
}

bool isValidDate(const std::string& httpDate)
{
   std::string dateRegex = std::string() +
         "(Mon|Tue|Wed|Thu|Fri|Sat|Sun)" +                     // day of week
         "," +                                                 // comma
         "\\s" +                                               // space
         "\\d{2}" +                                            // date of month
         "\\s" +                                               // space
         "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)" + // month
         "\\s" +                                               // space
         "\\d{4}" +                                            // year
         "\\s" +                                               // space
         "\\d{2}" +                                            // hour
         ":" +                                                 // colon
         "\\d{2}" +                                            // minute
         ":" +                                                 // colon
         "\\d{2}" +                                            // second
         "\\s" +                                               // space
         "GMT";

   boost::regex reDate(dateRegex);
   return regex_utils::textMatches(httpDate, reDate, false, true);
}

std::string pathAfterPrefix(const Request& request,
                            const std::string& pathPrefix)
{
   // get the raw uri & strip its location prefix
   std::string uri = URL::cleanupPath(request.uri());
   
   if (!pathPrefix.empty() && !uri.compare(0, pathPrefix.length(), pathPrefix))
      uri = uri.substr(pathPrefix.length());

   // strip query string
   size_t pos = uri.find("?");
   if (pos != std::string::npos)
      uri.erase(pos);

   // uri has now been reduced to path. url decode it (we noted that R
   // was url encoding dashes in e.g. help for memory-limits)
   return  http::util::urlDecode(uri);
}

core::FilePath requestedFile(const std::string& wwwLocalPath,
                             const std::string& relativePath)
{
   // ensure that this path does not start with /
   if (relativePath.find('/') == 0)
      return FilePath();

   // ensure that this path does not contain ..
   if (relativePath.find("..") != std::string::npos)
      return FilePath();

#ifndef _WIN32

   // calculate "real" wwwPath
   FilePath wwwRealPath;
   Error error = core::system::realPath(wwwLocalPath, &wwwRealPath);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }

   // calculate "real" requested path
   FilePath realRequestedPath;
   FilePath requestedPath = wwwRealPath.completePath(relativePath);
   error = core::system::realPath(
      requestedPath.getAbsolutePath(),
                                  &realRequestedPath);
   if (error)
   {
      // log if this isn't file not found
      if (error != systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation()))
      {
         error.addProperty("requested-path", relativePath);
         LOG_ERROR(error);
      }
      return FilePath();
   }

   // validate that the requested path falls within the www path
   if ( (realRequestedPath != wwwRealPath) &&
      realRequestedPath.getRelativePath(wwwRealPath).empty() )
   {
      LOG_WARNING_MESSAGE("Non www-local-path URI requested: " +
                          relativePath);
      return FilePath();
   }

   // return the path
   return realRequestedPath;

#else

   // just complete the path straight away on Win32
   return FilePath(wwwLocalPath).completePath(relativePath);

#endif
}

void fileRequestHandler(const std::string& wwwLocalPath,
                        const std::string& baseUri,
                        const http::Request& request,
                        http::Response* pResponse)
{
   // get the uri and strip the query string
   std::string uri = request.uri();
   std::size_t pos = uri.find("?");
   if (pos != std::string::npos)
      uri.erase(pos);

   // request for one-character short of root location redirects to root
   if (uri == baseUri.substr(0, baseUri.size()-1))
   {
      pResponse->setMovedPermanently(request, baseUri);
      return;
   }

   // request for a URI not within our location scope
   if (uri.find(baseUri) != 0)
   {
      pResponse->setNotFoundError(request);
      return;
   }

   // auto-append index.htm to request for root location
   const char * const kIndexFile = "index.htm";
   if (uri == baseUri)
      uri += kIndexFile;

   // get path to the requested file requested file
   std::string relativePath = uri.substr(baseUri.length());
   FilePath filePath = http::util::requestedFile(wwwLocalPath, relativePath);
   if (filePath.isEmpty())
   {
      pResponse->setNotFoundError(request);
      return;
   }

   // return requested file
   pResponse->setCacheableFile(filePath, request);
}

std::string formatMessageAsHttpChunk(const std::string& message)
{
   // format message as an HTTP chunk
   // the format is <Chunk size (hex)>CRLF<Chunk data>CRLF
   std::stringstream sstr;
   sstr << std::hex << message.size() << "\r\n" << message << "\r\n";
   return sstr.str();
}

bool isIpAddress(const std::string& str)
{
   boost::system::error_code err;
   boost::asio::ip::address::from_string(str, err);
   return !err;
}

bool isNetworkAddress(const std::string& str)
{
   boost::asio::io_service io_service;

   // query DNS for this address
   boost::asio::ip::tcp::resolver resolver(io_service);
   boost::asio::ip::tcp::resolver::query query(str, "");

   boost::system::error_code ec;
   boost::asio::ip::tcp::resolver::iterator iter = resolver.resolve(query, ec);
   return (!ec && iter != boost::asio::ip::tcp::resolver::iterator());
}

bool isWSUpgradeRequest(const Request& request)
{
   // look for the Upgrade token in the Connection request header; in most cases it will be the
   // exact value of the the header, but some browsers (Firefox) include other tokens. (RFC 6455)
   boost::regex upgrade("\\<Upgrade\\>", boost::regex::icase);
   std::string connection = request.headerValue("Connection");
   return boost::regex_search(connection, upgrade);
}

#ifndef _WIN32
bool isSslShutdownError(const boost::system::error_code& ec)
{
   // boost returns "short_read" when the peer calls SSL_shutdown()
#ifdef SSL_R_SHORT_READ
   // OpenSSL 1.0.0
   return ec.category() == boost::asio::error::get_ssl_category() &&
          ec.value() == ERR_PACK(ERR_LIB_SSL, 0, SSL_R_SHORT_READ);
#else
   // OpenSSL 1.1.0
   return ec == boost::asio::ssl::error::stream_truncated;
#endif
}
#else
bool isSslShutdownError(const boost::system::error_code& ec)
{
   return ec == boost::asio::ssl::error::stream_truncated;
}
#endif

std::string addQueryParam(const std::string& uri,
                          const std::string& queryParam)
{
   if (uri.find('?') == std::string::npos)
   {
      return uri + "?" + queryParam;
   }
   else
   {
      return uri + "&" + queryParam;
   }
}

} // namespace util

} // namespace http
} // namespace core
} // namespace rstudio

