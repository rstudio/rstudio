/*
 * HunspellDictionaryManager.hpp
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

#ifndef CORE_SPELLING_HUNSPELL_DICTIONARY_MANAGER_HPP
#define CORE_SPELLING_HUNSPELL_DICTIONARY_MANAGER_HPP

#include <vector>
#include <string>

#include <shared_core/FilePath.hpp>

#include <core/spelling/HunspellCustomDictionaries.hpp>

namespace rstudio {
namespace core {

class Error;

namespace spelling {

class HunspellDictionary
{
public:
   HunspellDictionary()
   {
   }

   explicit HunspellDictionary(const FilePath& affPath)
      : affPath_(affPath)
   {
   }

   ~HunspellDictionary()
   {
   }

   // COPYING: via compiler

   bool empty() const { return affPath_.isEmpty(); }

   bool operator==(const HunspellDictionary& other) const
   {
      return affPath_ == other.affPath_;
   }

   std::string id() const { return affPath_.getStem(); }
   std::string name() const;

   FilePath dicPath() const;
   FilePath affPath() const { return affPath_; }

private:
   FilePath affPath_;
};

class HunspellDictionaryManager
{
public:
   HunspellDictionaryManager(const FilePath& coreLanguagesDir,
                             const FilePath& userDir)
      : coreLanguagesDir_(coreLanguagesDir),
        userDir_(userDir),
        customDicts_(customLanguagesDir())
   {
   }

   ~HunspellDictionaryManager()
   {
   }

   // COPYING: via compiler

   bool allLanguagesInstalled() const { return allLanguagesDir().exists(); }

   core::Error availableLanguages(
                     std::vector<HunspellDictionary>* pDictionaries) const;

   HunspellDictionary dictionaryForLanguageId(const std::string& langId) const;

   const HunspellCustomDictionaries& custom() const;

private:
   core::FilePath allLanguagesDir() const;
   core::FilePath customLanguagesDir() const;
   core::FilePath legacyAllLanguagesDir() const;
   core::FilePath legacyCustomLanguagesDir() const;

private:
   core::FilePath coreLanguagesDir_;
   core::FilePath userDir_;
   HunspellCustomDictionaries customDicts_;
};

} // namespace spelling
} // namespace core 
} // namespace rstudio


#endif // CORE_SPELLING_HUNSPELL_DICTIONARY_MANAGER_HPP

