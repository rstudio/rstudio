/*
 * HunspellCustomDictionaries.cpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/spelling/HunspellCustomDictionaries.hpp>

#include <boost/bind/bind.hpp>

#include <core/Algorithm.hpp>
#include <core/Log.hpp>

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace spelling {

std::vector<std::string> HunspellCustomDictionaries::dictionaries() const
{
   std::vector<std::string> dictionaries;
   Error error = customDictionariesDir_.ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return dictionaries;
   }

   std::vector<FilePath> children;
   error = customDictionariesDir_.getChildren(children);
   if (error)
   {
      LOG_ERROR(error);
      return dictionaries;
   }

   algorithm::copy_transformed_if(
         children.begin(),
         children.end(),
         std::back_inserter(dictionaries),
         boost::bind(&FilePath::hasExtensionLowerCase, _1, ".dic"),
         boost::bind(&FilePath::getStem, _1));

   return dictionaries;
}

FilePath HunspellCustomDictionaries::dictionaryPath(
                                          const std::string& name) const
{
   return customDictionariesDir_.completeChildPath(name + ".dic");
}

Error HunspellCustomDictionaries::add(const FilePath& dicPath) const
{
   // validate .dic extension
   if (!dicPath.hasExtensionLowerCase(".dic"))
   {
      return systemError(boost::system::errc::invalid_argument,
                         ERROR_LOCATION);
   }

   // remove existing with same name
   std::string name = dicPath.getStem();
   Error error = remove(name);
   if (error)
      LOG_ERROR(error);

   // add it
   return dicPath.copy(dictionaryPath(name));
}

Error HunspellCustomDictionaries::remove(const std::string& name) const
{
   return dictionaryPath(name).removeIfExists();
}


} // namespace spelling
} // namespace core
} // namespace rstudio
