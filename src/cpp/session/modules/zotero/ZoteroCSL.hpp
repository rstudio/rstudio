/*
 * ZoteroCSL.hpp
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

#ifndef RSTUDIO_SESSION_MODULES_ZOTERO_CSL_HPP
#define RSTUDIO_SESSION_MODULES_ZOTERO_CSL_HPP

#include <string>
#include <vector>
#include <map>

namespace rstudio {
namespace core {
   class Error;
namespace json{
   class Object;
}
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {

struct ZoteroCreator
{
   std::string firstName;
   std::string lastName;
   std::string creatorType;
};

typedef std::map<std::string,std::vector<ZoteroCreator>> ZoteroCreatorsByKey;

core::json::Object sqliteItemToCSL(std::map<std::string,std::string> item, const ZoteroCreatorsByKey& creators);
void convertCheaterKeysToCSL(core::json::Object &csl);
void convertCheaterKeysToCSLForValue(core::json::Object &csl, const std::string &value);
void convertCheaterKeysToCSLForField(core::json::Object &csl, const std::string &fieldName);

} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_ZOTERO_CSL_HPP */
