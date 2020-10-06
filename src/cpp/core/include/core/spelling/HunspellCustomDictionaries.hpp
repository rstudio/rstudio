/*
 * HunspellCustomDictionaries.hpp
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

#ifndef CORE_SPELLING_HUNSPELL_CUSTOM_DICTIONARIES_HPP
#define CORE_SPELLING_HUNSPELL_CUSTOM_DICTIONARIES_HPP

#include <vector>
#include <string>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {

class Error;

namespace spelling {

class HunspellCustomDictionaries
{
public:
   HunspellCustomDictionaries(const FilePath& customDictionariesDir)
      : customDictionariesDir_(customDictionariesDir)
   {
   }

   ~HunspellCustomDictionaries()
   {
   }

   // COPYING: via compiler

   std::vector<std::string> dictionaries() const;
   FilePath dictionaryPath(const std::string& name) const;

   Error add(const FilePath& dicPath) const;
   Error remove(const std::string& name) const;

private:
   core::FilePath customDictionariesDir_;
};

} // namespace spelling
} // namespace core 
} // namespace rstudio


#endif // CORE_SPELLING_HUNSPELL_CUSTOM_DICTIONARIES_HPP

