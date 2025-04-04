/*
 * DcfParser.cpp
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

#include <core/text/DcfParser.hpp>

#include <iostream>

#include <string>
#include <vector>
#include <map>

#include <boost/format.hpp>
#include <boost/function.hpp>
#include <boost/regex.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/RegexUtils.hpp>

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace text {

const char * const kDcfFieldRegex = "([^\\s]+?)\\s*\\:\\s*(.*)$";

Error parseDcfFile(const std::string& dcfFileContents,
                   bool preserveKeyCase,
                   DcfFieldRecorder recordField,
                   std::string* pUserErrMsg)
{
   // split into lines
   std::vector<std::string> dcfLines;
   boost::algorithm::split(dcfLines,
                           dcfFileContents,
                           boost::algorithm::is_any_of("\n"));

   // iterate over lines
   int lineNumber = 0;
   std::string currentKey;
   std::string currentValue;
   for(std::vector<std::string>::const_iterator it = dcfLines.begin();
       it != dcfLines.end();
       ++it)
   {
      lineNumber++;

      // report blank lines (so clients can see record delimiters)
      if (it->empty() || boost::algorithm::trim_copy(*it).empty())
      {
         // if we have a pending key & value then resolve it
         if (!currentKey.empty())
         {
            if (!recordField(std::make_pair(currentKey,currentValue)))
               return Success();

            currentKey.clear();
            currentValue.clear();
         }

         if (!recordField(std::make_pair(std::string(), std::string())))
            return Success();

         continue;
      }

      // skip comment lines
      if (it->at(0) == '#')
         continue;

      // define regexes
      boost::regex keyValueRegx(kDcfFieldRegex);
      boost::regex continuationRegex("[\t\\s](.*)");

       // look for a key-value pair line
      boost::smatch keyValueMatch, continuationMatch;
      if (regex_utils::match(*it, keyValueMatch, keyValueRegx))
      {
         // if we have a pending key & value then resolve it
         if (!currentKey.empty())
         {
            if (!recordField(std::make_pair(currentKey,currentValue)))
               return Success();

            currentKey.clear();
            currentValue.clear();
         }

         // update the current key and value
         currentKey = preserveKeyCase ?
                                 keyValueMatch[1] :
                                 string_utils::toLower(keyValueMatch[1]);
         currentValue = keyValueMatch[2];
      }

      // look for a continuation
      else if (!currentKey.empty() &&
               regex_utils::match(*it, continuationMatch, continuationRegex))
      {
         currentValue.append("\n");
         currentValue.append(continuationMatch[1]);
      }

      // invalid line
      else
      {
         Error error = systemError(boost::system::errc::protocol_error,
                                   ERROR_LOCATION);
         boost::format fmt("file line number %1% is invalid");
         *pUserErrMsg = boost::str(fmt % lineNumber);
         error.addProperty("parse-error", *pUserErrMsg);
         error.addProperty("line-contents", *it);
         return error;
      }
   }

   // resolve any pending key and value
   if (!currentKey.empty())
   {
      if (!recordField(std::make_pair(currentKey,currentValue)))
         return Success();
   }

   // always report a final blank line (so clients can see record delimiters)
   recordField(std::make_pair(std::string(), std::string()));

   return Success();
}

Error parseDcfFile(const FilePath& dcfFilePath,
                   bool preserveKeyCase,
                   DcfFieldRecorder recordField,
                   std::string* pUserErrMsg)
{
   // read the file
   std::string dcfFileContents;
   Error error = readStringFromFile(dcfFilePath,
                                    &dcfFileContents,
                                    string_utils::LineEndingPosix);
   if (error)
   {
      error.addProperty("dcf-file", dcfFilePath.getAbsolutePath());
      *pUserErrMsg = error.getSummary();
      return error;
   }

   return parseDcfFile(dcfFileContents,
                       preserveKeyCase,
                       recordField,
                       pUserErrMsg);
}


namespace {

bool mapInsert(std::map<std::string,std::string>* pMap,
               const std::pair<std::string,std::string>& field)
{
   // ignore empty records
   if (field.first.empty())
      return true;

   pMap->insert(field);
   return true;
}

} // anonymous namespace


Error parseDcfFile(const FilePath& dcfFilePath,
                   bool preserveKeyCase,
                   std::map<std::string,std::string>* pFields,
                   std::string* pUserErrMsg)
{
   return parseDcfFile(dcfFilePath,
                       preserveKeyCase,
                       boost::bind(mapInsert, pFields, _1),
                       pUserErrMsg);
}

Error parseDcfFile(const std::string& dcfFileContents,
                   bool preserveKeyCase,
                   std::map<std::string, std::string>* pFields,
                   std::string* pUserErrMsg)
{
   return parseDcfFile(dcfFileContents,
                       preserveKeyCase,
                       boost::bind(mapInsert, pFields, _1),
                       pUserErrMsg);
}

Error parseMultiDcfFile(const std::string& dcfFileContents,
                        bool preserveKeyCase,
                        const boost::function<Error(std::size_t, const std::map<std::string, std::string> &)>& handleEntry)
{
   // split out multiple DCF entries one at a time
   // entries are separated by a blank line
   // (consisting of a newline character, any amount of spaces or tabs, and another newline character)
   boost::sregex_token_iterator iter(dcfFileContents.begin(),
                                     dcfFileContents.end(),
                                     boost::regex("\n[\\t ]*\n"),
                                     -1);
   boost::sregex_token_iterator end;

   size_t lineCount = 1;
   for (; iter != end; ++iter)
   {
      // split dcf chunk into separate lines (delineated by newline character)
      std::vector<std::string> entryLines;
      boost::algorithm::split(entryLines, *iter, boost::is_any_of("\n"));

      // check if the lines are all comments or whitespace
      // if so, then we will skip this entry
      bool skipEntry = true;
      for(const std::string& line : entryLines)
      {
         std::string trimmedLine = string_utils::trimWhitespace(line);

         // check if line is a comment or purely whitespace
         if (trimmedLine.empty() || (trimmedLine.size() > 0 && trimmedLine.at(0) == '#'))
            continue;

         skipEntry = false;
         break;
      }

      if (!skipEntry)
      {
         std::map<std::string, std::string> fields;
         std::string err;

         Error error = text::parseDcfFile(*iter, preserveKeyCase, &fields, &err);
         if (error)
            return error;

         error = handleEntry(lineCount, fields);
         if (error)
            return error;
      }

      lineCount += entryLines.size() + 1; // each entry separated by a blank line
   }

   return Success();
}

Error parseMultiDcfFile(const FilePath& dcfFilePath,
                        bool preserveKeyCase,
                        const boost::function<Error(std::size_t, const std::map<std::string, std::string>&)>& handleEntry)
{
   std::string contents;
   Error error = readStringFromFile(dcfFilePath,
                                    &contents,
                                    string_utils::LineEndingPosix);
   if (error)
      return error;

   return parseMultiDcfFile(contents, preserveKeyCase, handleEntry);
}

std::string dcfMultilineAsFolded(const std::string& line)
{
   return boost::algorithm::trim_copy(
       boost::regex_replace(line, boost::regex("\\s*\r?\n\\s*"), " "));
}


} // namespace dcf
} // namespace core
} // namespace rstudio
