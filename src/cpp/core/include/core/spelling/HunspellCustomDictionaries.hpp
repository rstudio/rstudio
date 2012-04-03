/*
 * HunspellCustomDictionaries.hpp
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

#ifndef CORE_SPELLING_HUNSPELL_CUSTOM_DICTIONARIES_HPP
#define CORE_SPELLING_HUNSPELL_CUSTOM_DICTIONARIES_HPP

#include <vector>
#include <string>

#include <core/FilePath.hpp>

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

   Error add(const FilePath& dicPath);
   Error remove(const std::string& name);

private:
   core::FilePath customDictionariesDir_;
};

} // namespace spelling
} // namespace core 


#endif // CORE_SPELLING_HUNSPELL_CUSTOM_DICTIONARIES_HPP

