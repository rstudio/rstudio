/*
 * ZoteroCSL.cpp
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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

#include "ZoteroCSL.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {

namespace {

// (helper functions go here)




} // end anonymous namespace


json::Object sqliteItemToCSL(std::map<std::string,std::string> item, const ZoteroCreatorsByKey& creators)
{
   json::Object cslJson;
   for (auto field : item)
   {
      cslJson[field.first] = field.second;

      //
      // TODO: more field mapping
      //
   }

   // get the item creators
   ZoteroCreatorsByKey::const_iterator it = creators.find(item["key"]);
   if (it != creators.end())
   {
     std::map<std::string,json::Array> creatorsByType;
     std::for_each(it->second.begin(), it->second.end(), [&creatorsByType](const ZoteroCreator& creator) {

        if (!creator.creatorType.empty())
        {
           json::Object jsonCreator;
           if (!creator.firstName.empty())
              jsonCreator["given"] = creator.firstName;
           if (!creator.lastName.empty())
              jsonCreator["family"] = creator.lastName;

           creatorsByType[creator.creatorType].push_back(jsonCreator);
        }
     });

     // set author
     for (auto typeCreators : creatorsByType)
     {
         std::string type = typeCreators.first;
         json::Array authorsArray = typeCreators.second;
         if (type == "author" || type == "editor")
            cslJson[type] = authorsArray;

         //
         // TODO: more creator types
         //
     }
   }

   return cslJson;
}


} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
