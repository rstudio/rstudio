/*
 * ZoteroWebAPI.hpp
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

#ifndef RSTUDIO_SESSION_MODULES_ZOTERO_REST_HPP
#define RSTUDIO_SESSION_MODULES_ZOTERO_REST_HPP

#include <boost/function.hpp>

namespace rstudio {
namespace core {
   class Error;
   namespace json {
      class Value;
   }
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace web_api {

typedef boost::function<void(const core::Error&,int,core::json::Value)> ZoteroJsonResponseHandler;

void zoteroKeyInfo(const ZoteroJsonResponseHandler& handler);
void zoteroCollections(int userID, const ZoteroJsonResponseHandler& handler);
void zoteroItemsForCollection(int userID,
                              const std::string& collectionID,
                              const ZoteroJsonResponseHandler& handler);
void zoteroDeleted(int userID, int since, const ZoteroJsonResponseHandler& handler);


} // end namespace web_api
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_ZOTERO_REST_HPP */
