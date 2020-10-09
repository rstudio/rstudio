/*
 * FileSerializer.hpp
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

#ifndef CORE_FILE_SERIALIZER_HPP
#define CORE_FILE_SERIALIZER_HPP

#include <string>
#include <map>
#include <iterator>
#include <istream>
#include <sstream>

#include <boost/iostreams/copy.hpp>
#include <boost/iostreams/concepts.hpp>
#include <boost/iostreams/filtering_stream.hpp>

#include <boost/function.hpp>
#include <boost/algorithm/string/trim.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {

template <typename CollectionType>
Error writeCollectionToFile(
         const core::FilePath& filePath, 
         const CollectionType& collection,
         boost::function<std::string(
                                 const typename CollectionType::value_type&)>
                         stringifyFunction)
{
   using namespace boost::system::errc;
   
   // open the file stream
   std::shared_ptr<std::ostream> pOfs;
   Error error = filePath.openForWrite(pOfs, true);
   if (error)
      return error;

   try
   {
      // write each line
      for (typename CollectionType::const_iterator
            it = collection.begin();
            it != collection.end();
            ++it)
      {
         *pOfs << stringifyFunction(*it) << std::endl;

        if (pOfs->fail())
             return systemError(io_error, ERROR_LOCATION);
      }
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }

   return Success();
}

enum ReadCollectionAction
{
   ReadCollectionAddLine,
   ReadCollectionIgnoreLine,
   ReadCollectionTerminate
};
   
template <typename CollectionType>
Error readCollectionFromFile(
         const core::FilePath& filePath,
         CollectionType* pCollection,
         boost::function<ReadCollectionAction(const std::string& line, 
                                 typename CollectionType::value_type* pValue)>
                         parseFunction,
                         bool trimAndIgnoreBlankLines=true)
{
   using namespace boost::system::errc;
   
   // open the file stream
   std::shared_ptr<std::istream> pIfs;
   Error error = filePath.openForRead(pIfs);
   if (error)
      return error;
   
   // create insert iterator
   std::insert_iterator<CollectionType> insertIterator(*pCollection, 
                                                       pCollection->begin());

   try
   {
      // read each line
      std::string nextLine;
      while (true)
      {
         // read the next line
         std::getline(*pIfs, nextLine);

         if (pIfs->eof())
         {
            // only exit here if we have nothing to process
            // otherwise, exit after we have processed the data
            if (nextLine.empty())
               break;
         }
         else if (pIfs->fail())
            return systemError(io_error, ERROR_LOCATION);

         // trim whitespace then ignore it if it is a blank line
         if (trimAndIgnoreBlankLines)
         {
            boost::algorithm::trim(nextLine);
            if (nextLine.empty())
               continue;
         }

         // parse it and add it to the collection
         typename CollectionType::value_type value;
         ReadCollectionAction action = parseFunction(nextLine, &value);
         if (action == ReadCollectionAddLine)
         {
            *insertIterator++ = value;
         }
         else if (action == ReadCollectionIgnoreLine)
         {
            // do nothing
         }
         else if (action == ReadCollectionTerminate)
         {
            break; // exit read loop
         }

         // if we've hit the end of the file, we're done reading
         if (pIfs->eof())
            break;
      }
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }
   
   return Success();
}

template <typename ContentType>
Error appendToFile(const core::FilePath& filePath,
                       const ContentType& content)
{
   using namespace boost::system::errc;
   
   // open the file stream
   std::shared_ptr<std::ostream> pOfs;
   Error error = filePath.openForWrite(pOfs, false);
   if (error)
      return error;

   try
   {
      pOfs->seekp(0, std::ios_base::end);

      // append the content
      *pOfs << content;
      if (pOfs->fail())
         return systemError(io_error, ERROR_LOCATION);
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }

   return Success();
}

template <typename T>
Error appendStructToFile(const core::FilePath& filePath,
                         const T& data)
{
   using namespace boost::system::errc;

   // open the file stream
   std::shared_ptr<std::ostream> pOfs;
   Error error = filePath.openForWrite(pOfs, false);
   if (error)
      return error;

   try
   {
      pOfs->seekp(0, std::ios_base::end);

      // append the content
      pOfs->write((const char*)&data, sizeof(T));
      if (pOfs->fail())
         return systemError(io_error, ERROR_LOCATION);
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }

   return Success();
}

template <typename T>
Error readStructVectorFromFile(const core::FilePath& filePath,
                               std::vector<T>* pVector)
{
   using namespace boost::system::errc;

   // open the file stream
   std::shared_ptr<std::istream> pIfs;
   Error error = filePath.openForRead(pIfs);
   if (error)
      return error;

   try
   {
      while(true)
      {
         T data;
         pIfs->read((char*)&data, sizeof(T));
         if (pIfs->eof())
            break;
         else if (pIfs->fail())
            return systemError(io_error, ERROR_LOCATION);
         else
            pVector->push_back(data);
      }
   }
   catch(const std::exception& e)
   {
      error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }

   return Success();
}



// convenince methods for simple string collections
ReadCollectionAction parseString(const std::string& line, std::string* pStr);
std::string stringifyString(const std::string& str);

      
Error writeStringMapToFile(const core::FilePath& filePath,
                           const std::map<std::string,std::string>& map);

Error readStringMapFromFile(const core::FilePath& filePath,
                            std::map<std::string,std::string>* pMap);
   
Error writeStringVectorToFile(const core::FilePath& filePath,
                              const std::vector<std::string>& vector);
   
Error readStringVectorFromFile(const core::FilePath& filePath,
                               std::vector<std::string>* pVector,
                               bool trimAndIgnoreBlankLines=true);

// lineEnding is the type of line ending you want to end up on disk
//
// maxOpenRetrySeconds indicates whether or not we should retry attempts to open the file
// when it is in use by another process (common when using backup software), and if so
// how many seconds of elapsed time should we wait for the file to become available
// note: this only has an effect on Windows
Error writeStringToFile(const core::FilePath& filePath,
                        const std::string& str,
                        string_utils::LineEnding lineEnding=string_utils::LineEndingPassthrough,
                        bool truncate = true,
                        int maxOpenRetrySeconds = 0);

// lineEnding is the type of line ending you want the resulting string to have
Error readStringFromFile(const core::FilePath& filePath,
                         std::string* pStr,
                         string_utils::LineEnding lineEnding=string_utils::LineEndingPassthrough,
                         int startLine = 0,
                         int endLine = 0,
                         int startCharacter = 0,
                         int endCharacter = 0);

// read a string from a file with a filter
template <typename Filter>
Error readStringFromFile(
   const core::FilePath& filePath,
   const Filter& filter,
   std::string* pContents,
   string_utils::LineEnding lineEnding=string_utils::LineEndingPassthrough)
{
   try
   {
      // open the file stream (report errors with exceptions)
      std::shared_ptr<std::istream> pIfs;
      Error error = filePath.openForRead(pIfs);
      if (error)
         return error;
      pIfs->exceptions(std::istream::failbit | std::istream::badbit);

      // output string stream (report errors with exceptions)
      std::stringstream ostr;
      ostr.exceptions(std::ostream::failbit | std::ostream::badbit);

      // do the copy
      boost::iostreams::filtering_ostream filteringOStream;
      filteringOStream.push(filter);
      filteringOStream.push(ostr);
      boost::iostreams::copy(*pIfs, filteringOStream, 128);

      // return contents with requested line endings
      *pContents = ostr.str();
      string_utils::convertLineEndings(pContents, lineEnding);

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

    return Success();
}



bool stripBOM(std::string* pStr);

} // namespace core
} // namespace rstudio


#endif // CORE_FILE_SERIALIZER_HPP
