/*
 * HunspellDictionaryManager.cpp
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

#include <core/spelling/HunspellDictionaryManager.hpp>

#include <core/Algorithm.hpp>

namespace core {
namespace spelling {
namespace hunspell {

namespace {

struct KnownDictionary
{
   const char* id;
   const char* name;
} ;

KnownDictionary s_knownDictionaries[] =
{
   { "en_US",   "English (US)" },
   { "en_GB",   "English (GB)" },
   { "en_AU",   "English (AU)" },
   { "en_CA",   "English (CA)" },

   { NULL, NULL }
};

bool isDicFile(const FilePath& filePath)
{
   return filePath.extensionLowerCase() == ".dic";
}

Dictionary fromDicFile(const FilePath& filePath)
{
   return Dictionary(filePath);
}

Error listDicFiles(const FilePath& baseDir, std::vector<FilePath>* pDicFiles)
{
   if (!baseDir.exists())
      return Success();

   std::vector<FilePath> children;
   Error error = baseDir.children(&children);
   if (error)
      return error;

   core::algorithm::copy_if(children.begin(),
                            children.end(),
                            std::back_inserter(*pDicFiles),
                            isDicFile);

   return Success();
}

} // anonymous namespace


std::string Dictionary::name() const
{
   std::string dictId = id();
   for (KnownDictionary* dict = s_knownDictionaries; dict->name; ++dict)
   {
      if (dictId == dict->id)
         return dict->name;
   }

   return dictId;
}

FilePath Dictionary::affPath() const
{
   return dicPath_.parent().childPath(dicPath_.stem() + ".aff");
}

Error DictionaryManager::availableLanguages(
                        std::vector<Dictionary>* pDictionaries) const
{
   // first try the user languages dir
   std::vector<FilePath> dicFiles;
   Error error = listDicFiles(userLanguagesDir(), &dicFiles);
   if (error)
      return error;

   // check the base languages dir if we didn't get any
   if (dicFiles.empty())
   {
      error = listDicFiles(languagesDir_, &dicFiles);
      if (error)
         return error;
   }

   // convert to dictionaries
   std::transform(dicFiles.begin(),
                  dicFiles.end(),
                  std::back_inserter(*pDictionaries),
                  fromDicFile);

   return Success();
}

Dictionary DictionaryManager::dictionaryForLanguageId(
                                       const std::string& langId) const
{
   std::string dicFile = langId + ".dic";
   if (userLanguagesInstalled())
      return Dictionary(userLanguagesDir().complete(dicFile));
   else
      return Dictionary(languagesDir_.complete(dicFile));
}

FilePath DictionaryManager::userLanguagesDir() const
{
   return userDir_.childPath("languages");
}

} // namespace hunspell
} // namespace spelling
} // namespace core
