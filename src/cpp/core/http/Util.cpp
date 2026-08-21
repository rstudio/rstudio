/*
 * Util.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include <core/http/Util.hpp>

#include <cstdio>
#include <cctype>
#include <ios>
#include <iostream>
#include <sstream>
#include <string_view>
#include <algorithm>

#include <boost/asio.hpp>
#include <boost/tokenizer.hpp>
#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/find_iterator.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/iostreams/filtering_stream.hpp>
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

struct SemiFinder
{
   template <typename ITER>
   boost::iterator_range<ITER> operator()(ITER begin, ITER end) const {
      bool inQuotes = false;
      bool escape = false;
      for (ITER iter = begin; iter != end; iter++)
      {
         if (escape)
         {
            escape = false;
         }
         else if (*iter == '\\')
         {
            escape = true;
         }
         else if (*iter == '"')
         {
            inQuotes = !inQuotes;
         }
         else if (!inQuotes && *iter == ';')
         {
            return boost::iterator_range<ITER>(iter, iter + 1);
         }
      }
      return boost::iterator_range<ITER>(end, end);
   }
} semiFinder;

struct BoundaryFinder
{
   BoundaryFinder(const std::string_view& boundary) : boundary(boundary) {}

   std::string boundary;

   enum State {
      NeedLeadingCR,
      NeedLeadingLF,
      NeedDash1,
      NeedDash2,
      NeedString,
      NeedTrailingCR,
      NeedTrailingLF,
      NeedTerminatorDash2,
   };

   template <typename ITER>
   boost::iterator_range<ITER> operator()(ITER begin, ITER end) const {
      using Range = boost::iterator_range<ITER>;

      // empty range matches nothing
      if (begin == end)
      {
         return Range(end, end);
      }

      State state = NeedLeadingCR;
      size_t matchPos = 0;
      ITER matchStart = begin;
      if (*begin == '-')
      {
         state = NeedDash2;
         ++begin;
      }
      for (ITER iter = begin; iter != end; iter++)
      {
         char ch = *iter;
         // Keep track of the start of the match
         if (state == NeedLeadingCR)
            matchStart = iter;
         // Reset the state machine on an unexpected \r
         if (state != NeedTrailingCR && ch == '\r')
         {
            state = NeedLeadingLF;
            matchStart = iter;
            continue;
         }
         switch (state)
         {
            case NeedLeadingCR:
               // ignore everything but \r, which is handled above
               break;
            case NeedLeadingLF:
               if (ch == '\n')
                  state = NeedDash1;
               else
                  state = NeedLeadingCR;
               break;
            case NeedDash1:
            case NeedDash2:
               matchPos = 0;
               if (ch == '-')
                  state = (state == NeedDash1) ? NeedDash2 : NeedString;
               else
                  state = NeedLeadingCR;
               break;
            case NeedString:
               if (ch == boundary[matchPos])
               {
                  matchPos++;
                  if (matchPos == boundary.size())
                     state = NeedTrailingCR;
               }
               else
                  state = NeedLeadingCR;
               break;
            case NeedTrailingCR:
               if (ch == '\r')
                  state = NeedTrailingLF;
               else if (ch == '-')
                  state = NeedTerminatorDash2;
               else if (ch != ' ' && ch != '\t')
                  state = NeedLeadingCR;
               break;
            case NeedTrailingLF:
               if (ch == '\n')
                  return Range(matchStart, iter + 1);
               state = NeedLeadingCR;
               break;
            case NeedTerminatorDash2:
               if (ch == '-')
                  return Range(matchStart, end);
               state = NeedLeadingCR;
               break;
         }
      }
      return Range(end, end);
   }
};

struct HeaderParams
{
   std::vector<std::string> values;
   std::map<std::string, std::string> params;

   template <typename RANGE>
   static HeaderParams parse(const RANGE& headerRange)
   {
      namespace ba = boost::algorithm;

      HeaderParams result;
      std::string_view header(&*headerRange.begin(), headerRange.size());

      auto fields = ba::make_split_iterator(header, semiFinder);

      for (; !fields.eof(); fields++)
      {
         auto start = fields->begin();
         auto end = fields->end();
         while (start != end && std::isspace(std::uint8_t(*start)))
            ++start;
         while (start != end && std::isspace(std::uint8_t(*(end - 1))))
            --end;

         auto eqPos = start;
         while (eqPos != end && *eqPos != '=')
            ++eqPos;
         if (eqPos == end)
         {
            if (end != start)
               result.values.emplace_back(&*start, end - start);
            continue;
         }
         std::string key(&*start, eqPos - start);
         boost::algorithm::to_lower(key);

         std::string_view value(&*eqPos + 1, end - eqPos - 1);
         // strip quotes
         if (value.size() >= 2 && value.front() == '"' && value.back() == '"')
         {
            value.remove_prefix(1);
            value.remove_suffix(1);
            std::string valueStr;
            valueStr.reserve(value.size());
            bool escape = false;
            for (char ch : value)
            {
               if (escape)
               {
                  escape = false;
               }
               else if (ch == '\\')
               {
                  escape = true;
                  continue;
               }
               valueStr.push_back(ch);
            }
            result.params.emplace(key, valueStr);
         }
         else
         {
            result.params.emplace(key, value);
         }
      }
      return result;
   }
};


void parseMultipartForm(const std::string& contentType,
                        const std::string& body,
                        Fields* pFields,
                        Files* pFiles)
{
   namespace ba = boost::algorithm;
   // get the boundary token
   HeaderParams ctParams = HeaderParams::parse(contentType);
   std::string boundary(ctParams.params["boundary"]);
   if (boundary.empty())
   {
      // No boundary token: malformed multipart/form-data
      LOG_WARNING_MESSAGE("Invalid multipart/form-data content-type: missing boundary");
      return;
   }

   // Per RFC 1341, multipart-body ends immediately after the `--` with no CRLF necessary.
   size_t terminatorPos = body.find("\r\n--" + boundary + "--");
   if (!terminatorPos || !body.size())
   {
      // No sections, just a terminating boundary
      LOG_WARNING_MESSAGE("Invalid multipart/form-data: no sections");
      return;
   }
   // Be permissinve beyond the strict requirements of RFC 1341:
   // Use best effort to read the last part even if the terminator is missing.
   if (terminatorPos == std::string::npos)
      terminatorPos = body.size();

   std::string_view multipart(&*body.begin(), terminatorPos);

   // iterate over the multipart sections
   BoundaryFinder finder(boundary);
   auto iter = ba::make_split_iterator(multipart, finder);
   auto endIter = ba::split_iterator<std::string_view::const_iterator>();
   bool isPreamble = true;
   for (; iter != endIter; ++iter)
   {
      auto partRange = *iter;
      if (isPreamble)
      {
         // Always ignore the preamble before the first multipart boundary
         isPreamble = false;
         continue;
      }

      // wrap in non-copying stream
      boost::iostreams::filtering_istream partStream(partRange);
      partStream.unsetf(std::ios::skipws);

      // read the headers
      Headers headers;
      http::parseHeaders(partStream, &headers);

      // get the content-disposition header
      std::string cDispStr = http::headerValue(headers, "Content-Disposition");
      if (cDispStr.empty())
         continue;

      // ignore sections that aren't form-data
      HeaderParams cDisp = HeaderParams::parse(cDispStr);
      if (cDisp.values.empty() || boost::algorithm::to_lower_copy(cDisp.values.front()) != "form-data")
         continue;

      auto nameIter = cDisp.params.find("name");
      if (nameIter == cDisp.params.end())
      {
         // name is required, so if it's missing, skip the entire section
         continue;
      }

      std::string formName(nameIter->second);

      auto filenameIter = cDisp.params.find("filename");
      if (filenameIter != cDisp.params.end() && pFiles->find(formName) != pFiles->end())
      {
         // If we've already processed a file upload for a given form field,
         // keep the first file and ignore the rest without allocating any
         // additional memory.
         continue;
      }

      std::string formValue(partRange.size(), '\0');
      partStream.read(formValue.data(), partRange.size());
      formValue.resize(partStream.gcount());

      if (filenameIter != cDisp.params.end())
      {
         File& uploadedFile = (*pFiles)[formName];
         uploadedFile.name = filenameIter->second;
         uploadedFile.contentType = http::headerValue(headers, "Content-Type");
         if (uploadedFile.contentType.empty())
            uploadedFile.contentType = "application/octet-stream";
         uploadedFile.contents.swap(formValue);
      }
      else
      {
         boost::algorithm::trim(formValue);
         pFields->emplace_back(formName, std::move(formValue));
      }
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

boost::posix_time::time_input_facet s_httpDateInputFacet(kHttpDateFormat, 1);

boost::posix_time::ptime parseDate(const std::string& date, const char* format)
{
   using namespace boost::posix_time;

   // Warning - creating the time_input_facet is fairly expensive so avoiding it for http date
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
   using namespace boost::posix_time;

   // parse from string
   std::stringstream dateStream;
   dateStream.str(date);
   dateStream.imbue(std::locale(dateStream.getloc(), &s_httpDateInputFacet));
   ptime posixDate(not_a_date_time);
   dateStream >> posixDate;

   return posixDate;
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

#ifndef _WIN32
   // To avoid the runtime CPU cost of compression, also check if there is a
   // precompressed version of the file available and substitute it in if so.
   // This is akin to the (gzip|brotli)_static directive for NGINX.
   FilePath precompressed = FilePath(filePath.getAbsolutePath() + ".br");
   if (request.acceptsEncoding(kBrotliEncoding) && precompressed.exists())
   {
      pResponse->setContentType(filePath.getMimeContentType());
      pResponse->setContentEncoding(kBrotliEncoding);
      pResponse->setCacheableFile(precompressed, request);
      return;
   }
   precompressed = FilePath(filePath.getAbsolutePath() + ".gz");
   if (request.acceptsEncoding(kGzipEncoding) && precompressed.exists())
   {
      pResponse->setContentType(filePath.getMimeContentType());
      pResponse->setContentEncoding(kGzipEncoding);
      pResponse->setCacheableFile(precompressed, request);
      return;
   }
#endif

   // return requested file
   pResponse->setCacheableFile(filePath, request);
}

namespace {

// Digits std::printf("%llx") / std::hex emit for this value: no leading zeros,
// and a single "0" for zero itself. Must agree exactly with the chunk-size line
// formatMessageAsHttpChunk() writes below -- see httpChunkSize().
std::size_t hexDigitCount(std::size_t value)
{
   std::size_t digits = 1;
   while (value >= 16)
   {
      value /= 16;
      ++digits;
   }
   return digits;
}

} // anonymous namespace

std::size_t httpChunkSize(std::size_t messageSize)
{
   // <Chunk size (hex)>CRLF<Chunk data>CRLF -- the two CRLFs are the + 4
   return hexDigitCount(messageSize) + 2 + messageSize + 2;
}

std::string formatMessageAsHttpChunk(const std::string& message)
{
   // format message as an HTTP chunk
   // the format is <Chunk size (hex)>CRLF<Chunk data>CRLF
   //
   // Assembled into one pre-sized std::string rather than through a
   // stringstream: this runs once per body piece on the streaming-proxy path,
   // where a stringstream copies every proxied byte twice over (once into its
   // internal buffer, again for str()).
   static_assert(sizeof(std::size_t) <= sizeof(unsigned long long),
                 "sizeLine below is sized for a 64-bit chunk size, and the cast "
                 "to unsigned long long must not truncate one");
   char sizeLine[/* 16 hex digits for a 64-bit size, plus NUL */ 17];
   int sizeLineLength = std::snprintf(sizeLine,
                                      sizeof(sizeLine),
                                      "%llx",
                                      static_cast<unsigned long long>(message.size()));

   std::string chunk;
   chunk.reserve(httpChunkSize(message.size()));
   chunk.append(sizeLine, sizeLineLength);
   chunk.append("\r\n");
   chunk.append(message);
   chunk.append("\r\n");
   return chunk;
}

void removeHopByHopHeaders(Response* pResponse)
{
   static const char* const hopByHopHeaders[] = {
      "Connection",
      "Proxy-Connection", // non-standard but still sent by some clients/proxies
      "Keep-Alive",
      "Proxy-Authenticate",
      "Proxy-Authorization",
      "TE",
      "Trailer",
      "Transfer-Encoding",
      "Upgrade"
   };

   // The message can have more than one Connection header field (each of
   // which can itself be a comma-separated list), and any of those values
   // can nominate additional header names that are hop-by-hop for this
   // particular message (RFC 7230 6.1); collect all of them before removing
   // Connection. headerValue() would only see the first field.
   std::vector<std::string> connectionTokens;
   for (const std::string& value : headerValues(pResponse->headers(), "Connection"))
   {
      std::vector<std::string> tokens;
      boost::algorithm::split(tokens, value, boost::is_any_of(","));
      connectionTokens.insert(connectionTokens.end(), tokens.begin(), tokens.end());
   }

   for (const char* name : hopByHopHeaders)
      pResponse->removeHeader(name);

   for (std::string token : connectionTokens)
   {
      boost::algorithm::trim(token);
      if (!token.empty())
         pResponse->removeHeader(token);
   }
}

TransferEncoding parseTransferEncoding(const Headers& headers)
{
   TransferEncoding result;

   // The field is 1#transfer-coding and may be split across repeated header
   // fields, so flatten every field's comma-separated list into one ordered
   // list of coding names before judging it -- only the final coding decides
   // whether the body on the wire is chunk-framed.
   std::vector<std::string> codings;
   for (const std::string& value : headerValues(headers, kTransferEncoding))
   {
      std::vector<std::string> tokens;
      boost::algorithm::split(tokens, value, boost::is_any_of(","));

      for (std::string token : tokens)
      {
         // a transfer-coding may carry parameters (token *( OWS ";" OWS
         // transfer-parameter )); only the name identifies the coding
         std::string::size_type semi = token.find(';');
         if (semi != std::string::npos)
            token.erase(semi);

         boost::algorithm::trim(token);
         if (token.empty())
            continue;

         boost::algorithm::to_lower(token);

         // "identity" was dropped as a transfer coding in RFC 7230 4 and means
         // "nothing was applied" anyway, so treat it as absent rather than as a
         // coding we would have to refuse for not being able to decode it.
         if (token == "identity")
            continue;

         codings.push_back(token);
      }
   }

   if (codings.empty())
      return result;

   std::size_t chunkedCount =
      std::count(codings.begin(), codings.end(), kChunkedTransferEncoding);

   result.present = true;
   result.chunkedIsFinal = codings.back() == kChunkedTransferEncoding;

   // The one shape we can undo is a single "chunked" applied last -- which is
   // also the only shape a conforming sender can produce, since RFC 7230 3.3.1
   // both forbids applying chunked more than once and requires it to be the
   // final coding when anything else is applied. Note "chunked, chunked" would
   // otherwise slip through every other flag here looking like ordinary
   // chunking, while leaving a second chunk layer on the body.
   result.isDecodable = result.chunkedIsFinal &&
                        chunkedCount == 1 &&
                        codings.size() == 1;

   return result;
}

bool isIpAddress(const std::string& str)
{
   boost::system::error_code err;
   boost::asio::ip::make_address(str, err);
   return !err;
}

bool isNetworkAddress(const std::string& str)
{
   // initialize resolver
   boost::asio::io_context ioContext;
   boost::asio::ip::tcp::resolver resolver(ioContext);

   // query DNS for this address
   boost::system::error_code ec;
   auto endpoint = resolver.resolve(str, "", ec);
   if (ec)
      return false;

   return endpoint.size();
}

namespace {

// look for the Upgrade token in the Connection request header; in most cases it will be the
// exact value of the the header, but some browsers (Firefox) include other tokens. (RFC 6455)
boost::regex s_webSocketUpgradePattern("\\<Upgrade\\>", boost::regex::icase);

}


bool isWSUpgradeRequest(const Request& request)
{
   std::string connection = request.headerValue("Connection");
   if (connection.empty())
      return false;
   return boost::regex_search(connection, s_webSocketUpgradePattern);
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

#ifndef _WIN32

bool isSslCertificateVerifyFailedError(const core::Error& error)
{
   return error.getName() == boost::asio::error::get_ssl_category().name() &&
              ERR_GET_LIB(error.getCode()) == ERR_LIB_SSL &&
              ERR_GET_REASON(error.getCode()) == SSL_R_CERTIFICATE_VERIFY_FAILED;
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

