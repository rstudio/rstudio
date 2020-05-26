/*
 * ConfigProfile.hpp
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

#ifndef CORE_CONFIG_PROFILE_HPP
#define CORE_CONFIG_PROFILE_HPP

#include <map>

#include <boost/any.hpp>
#include <boost/function.hpp>
#include <boost/lexical_cast.hpp>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {

// used to define configuration profiles in ini format with hierarchical sections
// see unit tests for example
class ConfigProfile
{
public:
   typedef std::pair<uint32_t, std::string> Level;
   typedef boost::function<bool(const std::string&, std::string*)> ValidatorFunc;

   template <typename T>
   void addParams(const std::string& name,
                  const T& defaultValue)
   {
      defaultValues_[name] = defaultValue;
   }

   template <typename T, typename ... Params>
   void addParams(const std::string& name,
                  const T& defaultValue,
                  Params... params)
   {
      addParams(name, defaultValue);
      addParams(params...);
   }

   void addValidators(const std::string& name,
                      const ValidatorFunc& validator)
   {
      validators_[name] = validator;
   }

   template <typename ... Params>
   void addValidators(const std::string& name,
                      const ValidatorFunc& validator,
                      Params... params)
   {
      addValidators(name, validator);
      addValidators(params...);
   }

   void addSection(const Level& sectionLevel)
   {
      sections_[sectionLevel.first] = sectionLevel.second;
   }

   // add sections, keyed by 0-based level index and the name of the section
   // which should be the prefix in the section header
   // for example: [@users] would indicate the section "@" with a value of "users"
   void addSections(const std::vector<Level>& sectionLevels)
   {
      for (const Level& sectionLevel : sectionLevels)
         addSection(sectionLevel);
   }

   core::Error load(const core::FilePath& filePath);

   core::Error parseString(const std::string& profileStr);

   // gets the names defined for a particular level in the configuration file
   // must only be called after a call to load()
   std::vector<std::string> getLevelNames(uint32_t level) const;

   // gets a param's value given the level values for each level
   // see unit tests for more examples
   template <typename T>
   core::Error getParam(const std::string& paramName,
                        T* pValue,
                        std::vector<Level> levels) const
   {
      // initialize the param to the default value
      DefaultParamValuesMap::const_iterator defaultValuesIter = defaultValues_.find(paramName);
      if (defaultValuesIter == defaultValues_.end())
      {
         return systemError(boost::system::errc::invalid_argument,
                            "Parameter not found",
                            ERROR_LOCATION);
      }
      *pValue = boost::any_cast<T>(defaultValuesIter->second);

      // sort the levels in ascending level order
      std::sort(levels.begin(),
                levels.end(),
                [](const Level& a, const Level& b) { return a.first < b.first; });

      // apply param value overrides in ascending level order
      for (const LevelValues& configLevel : levels_)
      {
         for (const Level& suppliedLevel : levels)
         {
            if (suppliedLevel == configLevel.first)
            {
               // levels match - apply configuration
               ValuesMap::const_iterator iter = configLevel.second.find(paramName);
               if (iter != configLevel.second.end())
               {
                  try
                  {
                     *pValue = boost::lexical_cast<T>(iter->second);
                  }
                  catch (const boost::bad_lexical_cast&)
                  {
                     return systemError(boost::system::errc::invalid_argument,
                                        "Invalid type requested for param " + paramName,
                                        ERROR_LOCATION);
                  }
               }
            }
         }
      }

      return Success();
   }

private:
   typedef std::map<std::string, boost::any> DefaultParamValuesMap;
   DefaultParamValuesMap defaultValues_;

   typedef std::map<std::string, ValidatorFunc> ValidatorMap;
   ValidatorMap validators_;

   std::map<uint32_t, std::string> sections_;

   typedef std::map<std::string, std::string> ValuesMap;
   typedef std::pair<Level, ValuesMap> LevelValues;
   std::vector<LevelValues> levels_;

   core::Error validateParam(const std::string& paramName,
                             const std::string& paramValue) const;
};

} // namespace core
} // namespace rstudio

#endif // CORE_CONFIG_PROFILE_HPP
