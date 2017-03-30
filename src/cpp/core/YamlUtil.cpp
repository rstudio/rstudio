/*
 * YamlUtil.cpp
 *
 * Copyright (C) 2009-2015 by RStudio, Inc.
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

#include <core/YamlUtil.hpp>

#include <boost/regex.hpp>

#include <core/Error.hpp>
#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/RegexUtils.hpp>


namespace rstudio {
namespace core {
namespace yaml {

namespace {

const boost::regex& reYaml()
{
   static boost::regex instance("^[\\s\\n]*---\\s*(.*?)---\\s*(?:$|\\n)");
   return instance;
}

} // anonymous namespace

bool hasYamlHeader(const FilePath& filePath)
{
   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
      LOG_ERROR(error);
   
   return hasYamlHeader(contents);
}

bool hasYamlHeader(const std::string& content)
{
   return regex_utils::search(content.begin(), content.end(), reYaml());
}

std::string extractYamlHeader(const FilePath& filePath)
{
   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
      LOG_ERROR(error);
   
   return extractYamlHeader(contents);
}

std::string extractYamlHeader(const std::string& content)
{
   std::string result;
   boost::smatch match;
   
   if (regex_utils::search(content.begin(), content.end(), match, reYaml()))
      if (match.size() >= 1)
         result = match[1];
   
   return result;
}

} // namespace yaml
} // namespace core
} // namespace rstudio
