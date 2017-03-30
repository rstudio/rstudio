/*
 * ConfigUtils.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/ConfigUtils.hpp>

#include <algorithm>

#include <boost/regex.hpp>
#include <boost/bind.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/replace.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

namespace rstudio {
namespace core {
namespace config_utils {

namespace {

void extractToMap(const std::string& keyAndValue,
                  std::map<std::string,std::string>* pMap)
{
   std::string::size_type pos = keyAndValue.find("=") ;
   if ( pos != std::string::npos )
   {
      std::string key = keyAndValue.substr(0, pos) ;
      boost::algorithm::trim(key);
      std::string value = keyAndValue.substr(pos + 1) ;
      boost::algorithm::trim(value) ;
      boost::algorithm::replace_all(value, "\"", "");
      pMap->operator[](key) = value;
   }
}

}

void extractVariables(const std::string& vars, Variables* pVariables)
{
   // scan for variables via regex iterator
   try
   {
      boost::regex var("^([A-Za-z0-9_]+=[^\n]+)$");
      boost::sregex_token_iterator it(vars.begin(), vars.end(), var, 0);
      boost::sregex_token_iterator end;
      std::for_each(it, end, boost::bind(extractToMap, _1, pVariables));
   }
   CATCH_UNEXPECTED_EXCEPTION;
}

Error extractVariables(const FilePath& file, Variables* pVariables)
{
   // return path not found if necessary
   if (!file.exists())
      return core::pathNotFoundError(file.absolutePath(), ERROR_LOCATION);

   // read in the file
   std::string contents;
   Error error = readStringFromFile(file,
                                    &contents,
                                    string_utils::LineEndingPosix);
   if (error)
      return error;

   extractVariables(contents, pVariables);

   return Success();
}

} // namespace config_utils
} // namespace core 
} // namespace rstudio



