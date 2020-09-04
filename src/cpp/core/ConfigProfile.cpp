/*
 * ConfigProfile.cpp
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

#include <boost/algorithm/string.hpp>
#include <boost/property_tree/ini_parser.hpp>
#include <boost/property_tree/ptree.hpp>

#include <core/FileSerializer.hpp>

#include <core/ConfigProfile.hpp>

namespace rstudio {
namespace core {

using namespace boost::property_tree;

Error ConfigProfile::validateParam(const std::string& paramName,
                                   const std::string& paramValue) const
{
   ValidatorMap::const_iterator iter = validators_.find(paramName);

   if (iter == validators_.end())
      return Success();

   const ValidatorFunc& validator = iter->second;

   std::string errorMessage;
   if (!validator(paramValue, &errorMessage))
   {
      return systemError(boost::system::errc::protocol_error,
                         "Param " + paramName + " is not valid: " + errorMessage,
                         ERROR_LOCATION);
   }

   return Success();
}

Error ConfigProfile::load(const FilePath& filePath)
{
   std::string contents;

   Error error = readStringFromFile(filePath, &contents);
   if (error)
      return error;

   error = parseString(contents);
   if (error)
   {
      std::string description = error.getProperty("description");
      description += " in file " + filePath.getAbsolutePath();
      error.addOrUpdateProperty("description", description);
   }

   return error;
}

Error ConfigProfile::parseString(const std::string& profileStr)
{
   std::istringstream stream{profileStr};

   // parse the profile (ini syntax)
   ptree profileTree;
   try
   {
      ini_parser::read_ini(stream, profileTree);
   }
   catch (const ini_parser_error& error)
   {
      return systemError(boost::system::errc::protocol_error,
                         "Could not parse config profile: " + error.message(),
                         ERROR_LOCATION);
   }

   // build section overrides
   std::vector<LevelValues> levels;
   for (const ptree::value_type& child : profileTree)
   {
      boost::optional<Level> matchingLevel;
      for (Level level : sections_)
      {
         // if the section level starts with the defined section name
         // from a prior call to addSections, we have a matching section level
         if (boost::algorithm::starts_with(child.first, level.second))
         {
            matchingLevel = level;
            break;
         }
      }

      if (!matchingLevel)
      {
         // no matching section level - it was not specified and is thus
         // an erroneous section
         return systemError(boost::system::errc::protocol_error,
                            "Invalid config section " + child.first,
                            ERROR_LOCATION);
      }

      std::map<std::string, std::string> values;
      for (const ptree::value_type& val : child.second)
      {
         // check to see if the parameter within the section has been registered
         const std::string& paramName = val.first;

         DefaultParamValuesMap::const_iterator defaultIter = defaultValues_.find(paramName);
         if (defaultIter == defaultValues_.end())
         {
            // we require every param to have a default value / be registered so error out here
            return systemError(boost::system::errc::protocol_error,
                               "Unknown param " + paramName + " specified",
                               ERROR_LOCATION);
         }

         std::string paramValue = val.second.get_value<std::string>();

         Error error = validateParam(paramName, paramValue);
         if (error)
            return error;

         values[paramName] = paramValue;
      }

      std::string levelValue = child.first.substr(matchingLevel.get().second.size());
      levels.push_back({{matchingLevel.get().first, levelValue}, values});
   }

   // stable sort the levels in ascending level number
   // this will preserve the order of same-level section overrides
   std::stable_sort(
            levels.begin(),
            levels.end(),
            [](const LevelValues& a, const LevelValues& b) { return a.first.first < b.first.first; });

   // assign the temporary variable levels to the class member
   // this assures that we can safely call this method multiple times, preserving the last
   // good configuration in case we error out above due to invalid configuration
   levels_ = levels;

   return Success();
}

std::vector<std::string> ConfigProfile::getLevelNames(uint32_t level) const
{
   std::vector<std::string> levelNames;

   for (const LevelValues& value : levels_)
   {
      if (value.first.first == level)
         levelNames.push_back(value.first.second);
   }

   return levelNames;
}

} // core
} // namespace rstudio
