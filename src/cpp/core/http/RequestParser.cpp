/*
 * RequestParser.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 * Copyright (c) 2003-2008 Christopher M. Kohlhoff (chris at kohlhoff dot com)
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

#include <core/http/RequestParser.hpp>

#include <boost/lexical_cast.hpp>

namespace rstudio {
namespace core {
namespace http {

RequestParser::RequestParser()
   :state_(method_start),
     contentLength_(0),
     parsingContentLength_(false),
     parsingBody_(false),
     checkContentLength_(false),
     isForm_(false),
     paused_(false),
     bufferPos_(boost::none),
     bodyBytesRead_(0),
     MAX_BUFFER_SIZE(defaultMaxBufferSize)
{
}

void RequestParser::reset()
{
  state_ = method_start;
  contentLength_ = 0;
  parsingContentLength_ = false;
  parsingBody_ = false;
  checkContentLength_ = false;
  isForm_ = paused_ = false;
  bufferPos_ = boost::none;
  bodyBytesRead_ = 0;
}

RequestParser::status RequestParser::consume(Request& req, char input)
{
  switch (state_)
  {
  case method_start:
    if (!is_char(input) || is_ctl(input) || is_tspecial(input))
    {
      return error;
    }
    else
    {
      state_ = method;
      req.method_.push_back(input);
      return incomplete;
    }
  case method:
    if (input == ' ')
    {
      state_ = uri;
      return incomplete;
    }
    else if (!is_char(input) || is_ctl(input) || is_tspecial(input))
    {
      return error;
    }
    else
    {
      req.method_.push_back(input);
      return incomplete;
    }
  case uri_start:
    if (is_ctl(input))
    {
      return error;
    }
    else
    {
      state_ = uri;
      req.uri_.push_back(input);
      return incomplete;
    }
  case uri:
    if (input == ' ')
    {
      state_ = http_version_h;
      return incomplete;
    }
    else if (is_ctl(input))
    {
      return error;
    }
    else
    {
      req.uri_.push_back(input);
      return incomplete;
    }
  case http_version_h:
    if (input == 'H')
    {
      state_ = http_version_t_1;
      return incomplete;
    }
    else
    {
      return error;
    }
  case http_version_t_1:
    if (input == 'T')
    {
      state_ = http_version_t_2;
      return incomplete;
    }
    else
    {
      return error;
    }
  case http_version_t_2:
    if (input == 'T')
    {
      state_ = http_version_p;
      return incomplete;
    }
    else
    {
      return error;
    }
  case http_version_p:
    if (input == 'P')
    {
      state_ = http_version_slash;
      return incomplete;
    }
    else
    {
      return error;
    }
  case http_version_slash:
    if (input == '/')
    {
      req.httpVersionMajor_ = 0;
      req.httpVersionMinor_ = 0;
      state_ = http_version_major_start;
      return incomplete;
    }
    else
    {
      return error;
    }
  case http_version_major_start:
    if (is_digit(input))
    {
      req.httpVersionMajor_ = req.httpVersionMajor_ * 10 + input - '0';
      state_ = http_version_major;
      return incomplete;
    }
    else
    {
      return error;
    }
  case http_version_major:
    if (input == '.')
    {
      state_ = http_version_minor_start;
      return incomplete;
    }
    else if (is_digit(input))
    {
      req.httpVersionMajor_ = req.httpVersionMajor_ * 10 + input - '0';
      return incomplete;
    }
    else
    {
      return error;
    }
  case http_version_minor_start:
    if (is_digit(input))
    {
      req.httpVersionMinor_ = req.httpVersionMinor_ * 10 + input - '0';
      state_ = http_version_minor;
      return incomplete;
    }
    else
    {
      return error;
    }
  case http_version_minor:
    if (input == '\r')
    {
      state_ = expecting_newline_1;
      return incomplete;
    }
    else if (is_digit(input))
    {
      req.httpVersionMinor_ = req.httpVersionMinor_ * 10 + input - '0';
      return incomplete;
    }
    else
    {
      return error;
    }
  case expecting_newline_1:
    if (input == '\n')
    {
      state_ = header_line_start;
      return incomplete;
    }
    else
    {
      return error;
    }
  case header_line_start:
    if (input == '\r')
    {
      state_ = expecting_newline_3;
      return incomplete;
    }
    else if (!req.headers_.empty() && (input == ' ' || input == '\t'))
    {
      state_ = header_lws;
      return incomplete;
    }
    else if (!is_char(input) || is_ctl(input) || is_tspecial(input))
    {
      return error;
    }
    else
    {
      req.headers_.push_back(Header());
      req.headers_.back().name.push_back(input);
      state_ = header_name;
      return incomplete;
    }
  case header_lws:
    if (input == '\r')
    {
      state_ = expecting_newline_2;
      return incomplete;
    }
    else if (input == ' ' || input == '\t')
    {
      return incomplete;
    }
    else if (is_ctl(input))
    {
      return error;
    }
    else
    {
      state_ = header_value;
      req.headers_.back().value.push_back(input);
      return incomplete;
    }
  case header_name:
    if (input == ':')
    {
      state_ = space_before_header_value;
      
      // look for special content-length state
      if ( boost::iequals(req.headers_.back().name, "Content-Length") )
         parsingContentLength_ = true;

      return incomplete;
    }
    else if (!is_char(input) || is_ctl(input) || is_tspecial(input))
    {
      return error;
    }
    else
    {
      req.headers_.back().name.push_back(input);
      return incomplete;
    }
  case space_before_header_value:
    if (input == ' ')
    {
      state_ = header_value;
      return incomplete;
    }
    else
    {
      return error;
    }
  case header_value:
    if (input == '\r')
    {
      state_ = expecting_newline_2;

      // if this header was Content-Length then save it
      if (parsingContentLength_)
      {
         contentLength_ = boost::lexical_cast<uintmax_t>(req.headers_.back().value);
         parsingContentLength_ = false;
      }

      return incomplete;
    }
    else if (is_ctl(input))
    {
      return error;
    }
    else
    {
      req.headers_.back().value.push_back(input);
      return incomplete;
    }
  case expecting_newline_2:
    if (input == '\n')
    {
      state_ = header_line_start;
      return incomplete;
    }
    else
    {
      return error;
    }
  case expecting_newline_3:
    if ( input == '\n' )
    {
      return complete;
    }
    else
    {
      return error;
    }
  default:
    return error;
  }
}

bool RequestParser::is_char(int c)
{
  return c >= 0 && c <= 127;
}

bool RequestParser::is_ctl(int c)
{
  return (c >= 0 && c <= 31) || c == 127;
}

bool RequestParser::is_tspecial(int c)
{
  switch (c)
  {
  case '(': case ')': case '<': case '>': case '@':
  case ',': case ';': case ':': case '\\': case '"':
  case '/': case '[': case ']': case '?': case '=':
  case '{': case '}': case ' ': case '\t':
    return true;
  default:
    return false;
  }
}

bool RequestParser::is_digit(int c)
{
  return c >= '0' && c <= '9';
}

void RequestParser::cleanup()
{
   formHandler_ = FormHandler();
}

} // namespace http
} // namespace core
} // namespace rstudio
