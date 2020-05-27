/*
 * RequestParser.hpp
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

#ifndef CORE_HTTP_REQUEST_PARSER_HPP
#define CORE_HTTP_REQUEST_PARSER_HPP

#include <boost/algorithm/string.hpp>
#include <boost/function.hpp>
#include <boost/optional.hpp>

#include <core/http/Header.hpp>
#include <core/http/Request.hpp>

#include <core/Log.hpp>

namespace rstudio {
namespace core {
namespace http {

typedef boost::function<bool(const std::string&, bool)> FormHandler;

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
     headers_parsed,
     pause,
     complete,
     form_complete,
     error
  };

  void setFormHandler(const FormHandler& handler)
  {
     formHandler_ = handler;

     // reserve space in the form buffer
     formBuffer_.reserve(contentLength_ < MAX_BUFFER_SIZE ? contentLength_ : MAX_BUFFER_SIZE);
  }

  template <typename InputIterator>
  status parse(Request& req, InputIterator begin, InputIterator end)
  {
    InputIterator originalBegin = begin;

    if (bufferPos_.has_value())
    {
       if (parsingBody_)
       {
          if (contentLength_ == 0)
          {
             cleanup();
             return complete;
          }
       }

       begin += bufferPos_.get();
       bufferPos_ = boost::none;
    }

    if (parsingBody_ && isForm_ && formHandler_)
    {
       // if we are resuming from pause, we don't want to recount the body bytes
       // that we have already processed
       if (!paused_)
       {
          // bulk transfer all of the read bytes into the raw buffer
         copyRangeToBuffer(begin, end, formBuffer_, formBuffer_.size());
         bodyBytesRead_ += std::distance(begin, end);
       }
       else
          paused_ = false;

       // deliver body bytes to listener
       bool complete = bodyBytesRead_ >= contentLength_;
       if (formBuffer_.size() >= MAX_BUFFER_SIZE || complete)
       {
          bool keepGoing = formHandler_(formBuffer_, complete);
          if (!keepGoing)
          {
             paused_ = true;
             bufferPos_ = std::distance(originalBegin, begin);
             return status::pause;
          }

          formBuffer_.clear();

          if (complete)
          {
             cleanup();
             return status::form_complete;
          }
       }

       return status::incomplete;
    }

    while (begin != end)
    {
       // header parsing
       if (!parsingBody_)
       {
          status st = consume(req, *begin++);
          if ( st == error )
          {
             cleanup();
             return st;
          }
          else if ( st == complete  )
          {
             // if we have a body then continue parsing it
             if (contentLength_ > 0)
             {
                if (req.contentType().find("multipart/form-data") != std::string::npos)
                   isForm_ = true;
             }

             // save buffer position so we can continue parsing the body when reinvoked
             parsingBody_ = true;
             bufferPos_ = std::distance(originalBegin, begin);
             return headers_parsed;
          }
       }
       // body parsing
       else
       {
          if (contentLength_ == 0)
          {
             cleanup();
             return complete;
          }
          else
          {
             if (checkContentLength_)
             {
                if (contentLength_ > maxContentLength && !formHandler_)
                {
                   // request will be buffered entirely in memory and exceeds the max size we can accept
                   LOG_ERROR_MESSAGE("Max content length exceeded for request with uri " + req.uri_);
                   cleanup();
                   return error;
                }

                checkContentLength_ = false;
             }

             req.body_.push_back(*begin++);
             if (req.body_.size() == contentLength_)
             {
                cleanup();
                return complete;
             }
          }
       }
    }

    bufferPos_ = boost::none;
    return incomplete;
  }

private:
  static constexpr uint64_t defaultMaxBufferSize = 1024*1024; // 1 MB
  static constexpr uint64_t maxContentLength = 100*1024*1024; // 100 MB

  /// Handle the next character of input.
  status consume(Request& req, char input);

  void cleanup();

  /// Check if a byte is an HTTP character.
  static bool is_char(int c);

  /// Check if a byte is an HTTP control character.
  static bool is_ctl(int c);

  /// Check if a byte is defined as an HTTP tspecial character.
  static bool is_tspecial(int c);

  /// Check if a byte is a digit.
  static bool is_digit(int c);

  template <typename Buffer>
  void copyRangeToBuffer(Buffer* begin, Buffer* end, std::string& buffer, uintmax_t bufferOffset)
  {
     auto distance = std::distance(begin, end);
     auto neededCapacity = bufferOffset + distance;
     if (buffer.capacity() < neededCapacity)
        buffer.reserve(neededCapacity);

     buffer.resize(neededCapacity);
     std::memcpy(const_cast<char*>(buffer.c_str() + bufferOffset), begin, distance);
  }

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
    expecting_newline_3,
  } state_;
  
  uintmax_t contentLength_;
  bool parsingContentLength_;
  bool parsingBody_;
  bool checkContentLength_;

  bool isForm_;
  bool paused_;
  FormHandler formHandler_;
  boost::optional<size_t> bufferPos_;
  uintmax_t bodyBytesRead_;

  std::string formBuffer_;

  size_t MAX_BUFFER_SIZE;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_REQUEST_PARSER_HPP
