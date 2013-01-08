/*
 * RequestParser.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
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

#ifndef CORE_HTTP_REQUEST_PARSER_HPP
#define CORE_HTTP_REQUEST_PARSER_HPP

#include <core/http/Request.hpp>

namespace core {
namespace http {

/// Parser for incoming requests.
class RequestParser
{
public:
  /// Construct ready to parse the request method.
  RequestParser();

  /// Reset to initial parser state.
  void reset();

  // enum for parse results
  enum status
  {
     incomplete,
     complete,
     error
  };

  template <typename InputIterator>
  status parse(Request& req, InputIterator begin, InputIterator end)
  {
    while (begin != end)
    {
       // header parsing
      if (!parsing_body_)
      {
         status st = consume(req, *begin++);
         if ( st == error )
         {
            return st ;
         }
         else if ( st == complete  )
         {
            // if we have a body then continue parsing it
            if (content_length_ > 0)
            {
               parsing_body_ = true ;
               continue ;
            }
            else
            {
               return st ;
            }
         }
      }
      // body parsing
      else
      {
         req.body_.push_back(*begin++) ;
         if (req.body_.size() == content_length_)
            return complete ;
      }
    }
    return incomplete ;
  }

private:
  /// Handle the next character of input.
  status consume(Request& req, char input);

  /// Check if a byte is an HTTP character.
  static bool is_char(int c);

  /// Check if a byte is an HTTP control character.
  static bool is_ctl(int c);

  /// Check if a byte is defined as an HTTP tspecial character.
  static bool is_tspecial(int c);

  /// Check if a byte is a digit.
  static bool is_digit(int c);

  /// The current state of the parser.
  enum state
  {
    method_start,
    method,
    uri_start,
    uri,
    http_version_h,
    http_version_t_1,
    http_version_t_2,
    http_version_p,
    http_version_slash,
    http_version_major_start,
    http_version_major,
    http_version_minor_start,
    http_version_minor,
    expecting_newline_1,
    header_line_start,
    header_lws,
    header_name,
    space_before_header_value,
    header_value,
    expecting_newline_2,
    expecting_newline_3
  } state_;
  
  std::size_t content_length_ ;
  bool parsing_content_length_ ;
  bool parsing_body_ ;
};

} // namespace http
} // namespace core

#endif // CORE_HTTP_REQUEST_PARSER_HPP
