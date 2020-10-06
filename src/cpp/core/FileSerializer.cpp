/*
 * FileSerializer.cpp
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

#include <core/FileSerializer.hpp>

#include <utility>
#include <iostream>
#include <sstream>
#include <algorithm>
#include <gsl/gsl>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/iostreams/copy.hpp>
#include <boost/thread.hpp>

#include <shared_core/FilePath.hpp>
#include <core/DateTime.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {

namespace {

bool isFileLockedError(const Error& error)
{
   // exclusive file access is only present on Windows
#ifndef _WIN32
   return false;
#else
   return (error && error.getCode() == ERROR_SHARING_VIOLATION);
#endif
}

Error openFileForWritingWithRetry(const FilePath& filePath,
                                  bool truncate,
                                  int maxOpenRetrySeconds,
                                  std::shared_ptr<std::ostream>* pOfs)
{
   using namespace boost::posix_time;

   ptime startTime = second_clock::universal_time();
   Error lastError;

   // do not allow negative values - regular signed int was chosen here for
   // easier integration with other parts of the codebase
   if (maxOpenRetrySeconds < 0)
      maxOpenRetrySeconds = 0;

   int numTries = 0;
   while (true)
   {
      lastError = filePath.openForWrite(*pOfs, truncate);

      // if the error is a non file lock error, then we should just return it
      if (!isFileLockedError(lastError))
      {
         if (lastError)
            LOG_ERROR(lastError);

         return lastError;
      }

      lastError.addOrUpdateProperty("open-attempts", ++numTries);

      // stop retrying if we've spent more than the requested amount of time
      if ((second_clock::universal_time() - startTime) >= seconds(maxOpenRetrySeconds))
      {
         lastError.addProperty("description", "Timed out while attempting to reopen the file");
         break;
      }

      // wait a moment before retrying
      boost::this_thread::sleep(milliseconds(500));
   }

   if (lastError)
      LOG_ERROR(lastError);

   return lastError;
}

} // anonymous namespace

std::string stringifyStringPair(const std::pair<std::string,std::string>& pair)
{
   return pair.first + "=\"" + string_utils::jsonLiteralEscape(pair.second) + "\"";
}

Error writeStringMapToFile(const core::FilePath& filePath,
                           const std::map<std::string,std::string>& map)
{
   return writeCollectionToFile<std::map<std::string,std::string> >(
                                                      filePath, 
                                                      map, 
                                                      stringifyStringPair);
}

ReadCollectionAction parseStringPair(
                     const std::string& line, 
                     std::pair<const std::string,std::string>* pPair)
{
   std::string::size_type pos = line.find("=");
   if ( pos != std::string::npos )
   {
      std::string name = line.substr(0, pos);
      boost::algorithm::trim(name);
      std::string value = line.substr(pos + 1);
      boost::algorithm::trim(value);
      if (value.length() >= 2 && value[0] == '"' && value[value.length() - 1] == '"')
      {
         value = string_utils::jsonLiteralUnescape(value);
      }
    
      // HACK: workaround the fact that std::map uses const for the Key
      std::string* pFirst = const_cast<std::string*>(&(pPair->first));
      *pFirst = name;

      pPair->second = value;

      return ReadCollectionAddLine;
   } 
   else
   {
      return ReadCollectionIgnoreLine;
   }
}


Error readStringMapFromFile(const core::FilePath& filePath,
                            std::map<std::string,std::string>* pMap)
{
   return readCollectionFromFile<std::map<std::string,std::string> >(
                                                      filePath,
                                                      pMap,
                                                      parseStringPair);
}

   
std::string stringifyString(const std::string& str)
{
   return str;
}
   
   
Error writeStringVectorToFile(const core::FilePath& filePath,
                              const std::vector<std::string>& vector)
{  
   return writeCollectionToFile<std::vector<std::string> >(filePath,
                                                           vector,
                                                           stringifyString);
   
}
   
   
ReadCollectionAction parseString(const std::string& line, std::string* pStr)
{
   *pStr = line;
   return ReadCollectionAddLine;
}
   
Error readStringVectorFromFile(const core::FilePath& filePath,
                               std::vector<std::string>* pVector,
                               bool trimAndIgnoreBlankLines)
{
   return readCollectionFromFile<std::vector<std::string> > (
         filePath, pVector, parseString, trimAndIgnoreBlankLines);
   
}

Error writeStringToFile(const FilePath& filePath,
                        const std::string& str,
                        string_utils::LineEnding lineEnding,
                        bool truncate,
                        int maxOpenRetrySeconds)
{
   using namespace boost::system::errc;
   
   // open file
   std::shared_ptr<std::ostream> pOfs;
   Error error = openFileForWritingWithRetry(filePath, truncate, maxOpenRetrySeconds, &pOfs);
   if (error)
      return error;

   try
   {
      // set exception mask (required for proper reporting of errors)
      pOfs->exceptions(std::ostream::failbit | std::ostream::badbit);
      
      // copy string to file
      std::string normalized = str;
      string_utils::convertLineEndings(&normalized, lineEnding);
      std::istringstream istr(normalized);
      boost::iostreams::copy(istr, *pOfs);
      
      // return success
      return Success();
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error, 
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }
}

Error readStringFromFile(const FilePath& filePath,
                         std::string* pStr,
                         string_utils::LineEnding lineEnding,
                         int startLine,
                         int endLine,
                         int startCharacter,
                         int endCharacter)
{
   using namespace boost::system::errc;
   
   // open file
   std::shared_ptr<std::istream> pIfs;
   Error error = filePath.openForRead(pIfs);
   if (error)
      return error;

   try
   {
      // if a line region was specified, read that region instead of the
      // entire file.
      if (endLine > startLine)
      {
         // set exception mask; note that we can't let failbit create an
         // exception here because reading eof can trigger failbit in our case.
         pIfs->exceptions(std::istream::badbit);

         int currentLine = 0;
         std::string content;
         std::string line;
         // loop over each line in the file. (consider: is there a more
         // performant way to seek past the first N lines?)
         while (++currentLine <= endLine
                && !pIfs->eof())
         {
            std::getline(*pIfs, line);
            if (currentLine >= startLine)
            {
               // compute the portion of the line to be read; if this is the
               // start or end of the region to be read, use the character
               // offsets supplied
               int lineLength = gsl::narrow_cast<int>(line.length());
               content += line.substr(
                        currentLine == startLine ?
                           std::min(
                              std::max(startCharacter - 1,  0),
                              lineLength) :
                           0,
                        currentLine == endLine ?
                           std::min(endCharacter, lineLength) :
                           lineLength);
               if (currentLine != endLine)
               {
                  content += "\n";
               }
            }
         }
         *pStr = content;
      }
      // reading the entire file
      else
      {
         // set exception mask (required for proper reporting of errors)
         pIfs->exceptions(std::istream::failbit | std::istream::badbit);

         // copy file to string stream
         std::ostringstream ostr;
         boost::iostreams::copy(*pIfs, ostr);
         *pStr = ostr.str();
      }

      string_utils::convertLineEndings(pStr, lineEnding);

      // return success
      return Success();
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error, 
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }
}

bool stripBOM(std::string* pStr)
{
   if (boost::algorithm::starts_with(*pStr, "\xEF\xBB\xBF"))
   {
      pStr->erase(0, 3);
      return true;
   }
   else if (boost::algorithm::starts_with(*pStr, "\xFF\xFE"))
   {
      pStr->erase(0, 2);
      return true;
   }
   else if (boost::algorithm::starts_with(*pStr, "\xFE\xFF"))
   {
      pStr->erase(0, 2);
      return true;
   }
   return false;
}

} // namespace core
} // namespace rstudio

