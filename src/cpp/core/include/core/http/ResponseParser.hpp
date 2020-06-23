/*
 * ResponseParser.hpp
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

#ifndef CORE_HTTP_RESPONSE_PARSER_HPP
#define CORE_HTTP_RESPONSE_PARSER_HPP

#include <iostream>
#include <sstream>

#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/asio/streambuf.hpp>
#include <boost/asio/read.hpp>
#include <boost/asio/read_until.hpp>

#include <shared_core/Error.hpp>
#include <core/http/Response.hpp>

namespace rstudio {
namespace core {
namespace http {

// we use a class rather than a namespace so we can grant friendship
// to template functions
class ResponseParser
{
public:

   static Error parseStatusLine(boost::asio::streambuf* pResponseBuffer,
                                Response* pResponse)
   {
      std::istream responseStream(pResponseBuffer);
      std::string httpVersion;
      responseStream >> httpVersion >> std::ws;

      // status code
      int statusCode;
      responseStream >> statusCode >> std::ws;
      pResponse->setStatusCode(statusCode);

      // status message
      std::string statusMessage;
      std::getline(responseStream, statusMessage);
      boost::algorithm::trim(statusMessage);
      pResponse->setStatusMessage(statusMessage);

      // validate that all elements required were in the response
      if ( !responseStream )
      {
         return systemError(boost::system::errc::protocol_error,
                            ERROR_LOCATION);
      }

      // validate that the http version was specified correctly
      if ( httpVersion.substr(0, 5) != "HTTP/" || httpVersion.size() != 8)
      {
         return systemError(boost::system::errc::protocol_error,
                            "Bad http version: " + httpVersion,
                            ERROR_LOCATION);

      }

      // parse out the major and minor version
      pResponse->setHttpVersion(
                           boost::lexical_cast<int>(httpVersion.substr(5,1)),
                           boost::lexical_cast<int>(httpVersion.substr(7,1)));

      return Success();
   }


   static void parseHeaders(boost::asio::streambuf* pResponseBuffer,
                            Response* pResponse)
   {
      std::istream responseStream(pResponseBuffer);

      Headers headers;
      http::parseHeaders(responseStream, &headers);
      std::for_each(headers.begin(),
                    headers.end(),
                    boost::bind(&Response::addHeader, pResponse, _1));
   }

   static void appendToBody(boost::asio::streambuf* pResponseBuffer,
                            Response* pResponse)
   {
      std::ostringstream bodyStream;
      if (pResponseBuffer->size() > 0)
         bodyStream << pResponseBuffer;
      pResponse->body_ += bodyStream.str();
   }

   static void appendToBody(const std::string& buff,
                            Response* pResponse)
   {
      pResponse->body_ += buff;
   }

   template <typename SyncReadStream>
   static Error parseFromStream(SyncReadStream& stream, Response* pResponse)
   {
      // declarations
      boost::system::error_code ec;
      boost::asio::streambuf response;

      // read the status line
      boost::asio::read_until(stream, response, "\r\n", ec);
      if (ec)
         return Error(ec, ERROR_LOCATION);

      // parse the status line
      Error error = ResponseParser::parseStatusLine(&response, pResponse);
      if(error)
         return error;

      // read the headers
      boost::asio::read_until(stream, response, "\r\n\r\n", ec);
      if (ec)
         return Error(ec, ERROR_LOCATION);

      // parse the headers
      ResponseParser::parseHeaders(&response, pResponse);

      // append any lefover buffer contents to the body
      if (response.size() > 0)
         ResponseParser::appendToBody(&response, pResponse);

      // read the body
      while (boost::asio::read(stream,
                               response, boost::asio::transfer_at_least(1),
                               ec))
      {
         ResponseParser::appendToBody(&response, pResponse);
      }

      if (ec != boost::asio::error::eof)
         return Error(ec, ERROR_LOCATION);

      return Success();
   }
};


} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_RESPONSE_PARSER_HPP
