/*
 * HunspellDictionaryManager.cpp
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

#include <core/spelling/HunspellDictionaryManager.hpp>
#include <core/system/Xdg.hpp>

#include <boost/bind.hpp>

#include <core/Algorithm.hpp>
#include <core/Log.hpp>

namespace rstudio {
namespace core {
namespace spelling {

namespace {

struct KnownDictionary
{
   const char* id;
   const char* name;
};

KnownDictionary s_knownDictionaries[] =
{
   { "bg_BG",     "Bulgarian"                },
   { "ca_ES",     "Catalan"                  },
   { "cs_CZ",     "Czech"                    },
   { "da_DK",     "Danish"                   },
   { "de_DE",     "German"                   },
   { "de_DE_neu", "German (New)"             },
   { "el_GR",     "Greek"                    },
   { "en_AU",     "English (Australia)"      },
   { "en_CA",     "English (Canada)"         },
   { "en_GB",     "English (United Kingdom)" },
   { "en_US",     "English (United States)"  },
   { "es_ES",     "Spanish"                  },
   { "fr_FR",     "French"                   },
   { "hr_HR",     "Croatian"                 },
   { "hu-HU",     "Hungarian"                },
   { "id_ID",     "Indonesian"               },
   { "it_IT",     "Italian"                  },
   { "lt_LT",     "Lithuanian"               },
   { "lv_LV",     "Latvian"                  },
   { "nb_NO",     "Norwegian"                },
   { "nl_NL",     "Dutch"                    },
   { "pl_PL",     "Polish"                   },
   { "pt_BR",     "Portuguese (Brazil)"      },
   { "pt_PT",     "Portuguese (Portugal)"    },
   { "ro_RO",     "Romanian"                 },
   { "ru_RU",     "Russian"                  },
   { "sh",        "Serbo-Croatian"           },
   { "sk_SK",     "Slovak"                   },
   { "sl_SI",     "Slovenian"                },
   { "sr",        "Serbian"                  },
   { "sv_SE",     "Swedish"                  },
   { "uk_UA",     "Ukrainian"                },
   { "vi_VN",     "Vietnamese"               },
   { nullptr, nullptr }
};

FilePath dicPathForAffPath(const FilePath& affPath)
{
   return affPath.getParent().completeChildPath(affPath.getStem() + ".dic");
}

bool isDictionaryAff(const FilePath& filePath)
{
   return (filePath.getExtensionLowerCase() == ".aff") &&
          dicPathForAffPath(filePath).exists();
}

HunspellDictionary fromAffFile(const FilePath& filePath)
{
   return HunspellDictionary(filePath);
}

Error listAffFiles(const FilePath& baseDir, std::vector<FilePath>* pAffFiles)
{
   if (!baseDir.exists())
      return Success();

   std::vector<FilePath> children;
   Error error = baseDir.getChildren(children);
   if (error)
      return error;

   core::algorithm::copy_if(children.begin(),
                            children.end(),
                            std::back_inserter(*pAffFiles),
                            isDictionaryAff);

   return Success();
}

bool compareByName(const HunspellDictionary& dict1,
                   const HunspellDictionary& dict2)
{
   return dict1.name() < dict2.name();
}

} // anonymous namespace


std::string HunspellDictionary::name() const
{
   std::string dictId = id();
   for (KnownDictionary* dict = s_knownDictionaries; dict->name; ++dict)
   {
      if (dictId == dict->id)
         return dict->name;
   }

   return dictId;
}

FilePath HunspellDictionary::dicPath() const
{
   return dicPathForAffPath(affPath_);
}

Error HunspellDictionaryManager::availableLanguages(
              std::vector<HunspellDictionary>* pDictionaries) const
{
   // first try the user languages dir
   std::vector<FilePath> affFiles;

   if (allLanguagesInstalled())
   {
      Error error = listAffFiles(allLanguagesDir(), &affFiles);
      if (error)
         return error;
   }
   else
   {
      Error error = listAffFiles(coreLanguagesDir_, &affFiles);
      if (error)
         return error;
   }

   // always check the custom directory as well (and auto-create
   // it so users who look for it will see it)
   FilePath customLangsDir = customLanguagesDir();
   Error error = customLangsDir.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   error = listAffFiles(customLangsDir, &affFiles);
   if (error)
      LOG_ERROR(error);

   // convert to dictionaries
   std::transform(affFiles.begin(),
                  affFiles.end(),
                  std::back_inserter(*pDictionaries),
                  fromAffFile);

   // sort them by name
   std::sort(pDictionaries->begin(), pDictionaries->end(), compareByName);

   return Success();
}

HunspellDictionary HunspellDictionaryManager::dictionaryForLanguageId(
                                       const std::string& langId) const
{
   std::string affFile = langId + ".aff";

   // first check to see whether it exists in the user languages directory
   FilePath customLangsAff = customLanguagesDir().completePath(affFile);
   if (customLangsAff.exists())
      return HunspellDictionary(customLangsAff);
   else if (allLanguagesInstalled())
      return HunspellDictionary(allLanguagesDir().completePath(affFile));
   else
      return HunspellDictionary(coreLanguagesDir_.completePath(affFile));
}

const HunspellCustomDictionaries&  HunspellDictionaryManager::custom() const
{
   return customDicts_;
}

/*
 * \deprecated
 * For getting all languages from pre-1.3 RStudio
 * */
FilePath HunspellDictionaryManager::legacyAllLanguagesDir() const
{
   return userDir_.completeChildPath("languages-system");
}

/*
 * \deprecated
 * For getting user languages from pre-1.3 RStudio
 * */
FilePath HunspellDictionaryManager::legacyCustomLanguagesDir() const
{
   return userDir_.completeChildPath("custom");
}

FilePath HunspellDictionaryManager::allLanguagesDir() const
{
   return core::system::xdg::userConfigDir().completeChildPath("dictionaries/languages-system");
}

FilePath HunspellDictionaryManager::customLanguagesDir() const
{
   return core::system::xdg::userConfigDir().completeChildPath("dictionaries/custom");
}

} // namespace spelling
} // namespace core
} // namespace rstudio
