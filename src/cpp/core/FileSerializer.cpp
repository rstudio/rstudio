/*
 * FileSerializer.cpp
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

#include <core/FileSerializer.hpp>

#include <utility>

#include <iostream>
#include <sstream>

#include <boost/algorithm/string/trim.hpp>
#include <boost/iostreams/copy.hpp>

#include <core/FilePath.hpp>

namespace core {

std::string stringifyStringPair(const std::pair<std::string,std::string>& pair)
{
   return pair.first + "=" + pair.second ;
}

Error writeStringMapToFile(const core::FilePath& filePath,
                           const std::map<std::string,std::string>& map)
{
   return writeCollectionToFile<std::map<std::string,std::string> >(
                                                      filePath, 
                                                      map, 
                                                      stringifyStringPair) ; 
}

ReadCollectionAction parseStringPair(
                     const std::string& line, 
                     std::pair<const std::string,std::string>* pPair)
{
   std::string::size_type pos = line.find("=") ;
   if ( pos != std::string::npos )
   {
      std::string name = line.substr(0, pos) ;
      boost::algorithm::trim(name);
      std::string value = line.substr(pos + 1) ;
      boost::algorithm::trim(value) ;
    
      // HACK: workaround the fact that std::map uses const for the Key
      std::string* pFirst = const_cast<std::string*>(&(pPair->first)) ;
      *pFirst = name ;

      pPair->second = value ;

      return ReadCollectionAddLine ;
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
                                                      parseStringPair) ;
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
   *pStr = line ;
   return ReadCollectionAddLine ;
}
   
Error readStringVectorFromFile(const core::FilePath& filePath,
                               std::vector<std::string>* pVector)
{
   return readCollectionFromFile<std::vector<std::string> > (filePath,
                                                             pVector,
                                                             parseString);
   
}
   

Error writeStringToFile(const FilePath& filePath, const std::string& str)
{
   using namespace boost::system::errc ;
   
   // open file
   std::string file = filePath.absolutePath();
   std::ofstream ofs(file.c_str(), std::ios_base::out | std::ios_base::binary);
   if (!ofs)
   {
      Error error = systemError(no_such_file_or_directory,ERROR_LOCATION);
      error.addProperty("path", file);
      return error;
   }
   
   try
   {
      // set exception mask (required for proper reporting of errors)
      ofs.exceptions(std::istream::failbit | std::istream::badbit);
      
      // copy string to file
      std::istringstream istr(str);
      boost::iostreams::copy(istr, ofs);
      
      // return success
      return Success();
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error, 
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", file);
      return error;
   }
}
   
Error readStringFromFile(const FilePath& filePath, std::string* pStr)
{
   using namespace boost::system::errc ;
   
   // open file
   std::string file = filePath.absolutePath();
   std::ifstream ifs(file.c_str(), std::ios_base::in | std::ios_base::binary) ;
   if (!ifs)
   {
      Error error = systemError(no_such_file_or_directory,ERROR_LOCATION);
      error.addProperty("path", file);
      return error;
   }
   

   try
   {
      // set exception mask (required for proper reporting of errors)
      ifs.exceptions(std::istream::failbit | std::istream::badbit);
      
      // copy file to string stream
      std::ostringstream ostr;
      boost::iostreams::copy(ifs, ostr);
      *pStr = ostr.str();
      
      // return success
      return Success();
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error, 
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", file);
      return error;
   }
}

} // namespace core

