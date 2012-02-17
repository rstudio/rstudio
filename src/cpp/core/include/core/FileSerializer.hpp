/*
 * FileSerializer.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

#include <boost/function.hpp>
#include <boost/algorithm/string/trim.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>

namespace core {

template <typename CollectionType>
Error writeCollectionToFile(
         const core::FilePath& filePath, 
         const CollectionType& collection,
         boost::function<std::string(
                                 const typename CollectionType::value_type&)>
                         stringifyFunction)
{
   using namespace boost::system::errc ;
   
   // open the file stream
   boost::shared_ptr<std::ostream> pOfs;
   Error error = filePath.open_w(&pOfs, true);
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
         *pOfs << stringifyFunction(*it) << std::endl ;

        if (pOfs->fail())
             return systemError(io_error, ERROR_LOCATION);
      }
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.absolutePath());
      return error;
   }

   return Success() ;
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
   using namespace boost::system::errc ;
   
   // open the file stream
   boost::shared_ptr<std::istream> pIfs;
   Error error = filePath.open_r(&pIfs);
   if (error)
      return error;
   
   // create insert iterator
   std::insert_iterator<CollectionType> insertIterator(*pCollection, 
                                                       pCollection->begin());

   try
   {
      // read each line
      std::string nextLine ;
      while (true)
      {
         // read the next line
         std::getline(*pIfs, nextLine) ;
         if (pIfs->eof())
            break;
         else if (pIfs->fail())
            return systemError(io_error, ERROR_LOCATION);

         // trim whitespace then ignore it if it is a blank line
         if (trimAndIgnoreBlankLines)
         {
            boost::algorithm::trim(nextLine) ;
            if (nextLine.empty())
               continue ;
         }

         // parse it and add it to the collection
         typename CollectionType::value_type value ;
         ReadCollectionAction action = parseFunction(nextLine, &value);
         if (action == ReadCollectionAddLine)
         {
            *insertIterator++ = value ;
         }
         else if (action == ReadCollectionIgnoreLine)
         {
            // do nothing
         }
         else if (action == ReadCollectionTerminate)
         {
            break; // exit read loop
         }
      }
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.absolutePath());
      return error;
   }
   
   return Success() ;
}

template <typename ContentType>
Error appendToFile(const core::FilePath& filePath,
                       const ContentType& content)
{
   using namespace boost::system::errc ;
   
   // open the file stream
   boost::shared_ptr<std::ostream> pOfs;
   Error error = filePath.open_w(&pOfs, false);
   if (error)
      return error;

   try
   {
      pOfs->seekp(0, std::ios_base::end);

      // append the content
      *pOfs << content  ;
      if (pOfs->fail())
         return systemError(io_error, ERROR_LOCATION);
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.absolutePath());
      return error;
   }

   return Success() ;
}

// convenince methods for simple string collections
ReadCollectionAction parseString(const std::string& line, std::string* pStr);
std::string stringifyString(const std::string& str);

      
Error writeStringMapToFile(const core::FilePath& filePath,
                           const std::map<std::string,std::string>& map) ;

Error readStringMapFromFile(const core::FilePath& filePath,
                            std::map<std::string,std::string>* pMap) ;
   
Error writeStringVectorToFile(const core::FilePath& filePath,
                              const std::vector<std::string>& vector);
   
Error readStringVectorFromFile(const core::FilePath& filePath,
                               std::vector<std::string>* pVector,
                               bool trimAndIgnoreBlankLines=true);

// lineEnding is the type of line ending you want to end up on disk
Error writeStringToFile(const core::FilePath& filePath,
                        const std::string& str,
                        string_utils::LineEnding lineEnding=string_utils::LineEndingPassthrough);

// lineEnding is the type of line ending you want the resulting string to have
Error readStringFromFile(const core::FilePath& filePath,
                         std::string* pStr,
                         string_utils::LineEnding lineEnding=string_utils::LineEndingPassthrough);

bool stripBOM(std::string* pStr);

} // namespace core


#endif // CORE_FILE_SERIALIZER_HPP
