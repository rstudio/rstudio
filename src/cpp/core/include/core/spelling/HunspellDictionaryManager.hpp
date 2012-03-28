/*
 * HunspellDictionaryManager.hpp
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

#ifndef CORE_SPELLING_HUNSPELL_DICTIONARY_MANAGER_HPP
#define CORE_SPELLING_HUNSPELL_DICTIONARY_MANAGER_HPP

#include <vector>
#include <string>

#include <core/FilePath.hpp>

namespace core {

class Error;

namespace spelling {
namespace hunspell {

class Dictionary
{
public:
   Dictionary()
   {
   }

   explicit Dictionary(const FilePath& dicPath)
      : dicPath_(dicPath)
   {
   }

   ~Dictionary()
   {
   }

   // COPYING: via compiler

   bool empty() const { return dicPath_.empty(); }

   bool operator==(const Dictionary& other) const
   {
      return dicPath_ == other.dicPath_;
   }

   std::string id() const { return dicPath_.stem(); }
   std::string name() const;

   FilePath dicPath() const { return dicPath_; }
   FilePath affPath() const;

private:
   FilePath dicPath_;
};

class DictionaryManager
{
public:
   DictionaryManager(const FilePath& coreLanguagesDir,
                     const FilePath& userDir)
      : coreLanguagesDir_(coreLanguagesDir), userDir_(userDir)
   {
   }

   ~DictionaryManager()
   {
   }

   // COPYING: via compiler

   bool allLanguagesInstalled() const { return allLanguagesDir().exists(); }

   core::Error availableLanguages(std::vector<Dictionary>* pDictionaries) const;

   Dictionary dictionaryForLanguageId(const std::string& langId) const;

private:
   core::FilePath allLanguagesDir() const;
   core::FilePath userLanguagesDir() const;

private:
   core::FilePath coreLanguagesDir_;
   core::FilePath userDir_;
};


} // namespace hunspell
} // namespace spelling
} // namespace core 


#endif // CORE_SPELLING_HUNSPELL_DICTIONARY_MANAGER_HPP

