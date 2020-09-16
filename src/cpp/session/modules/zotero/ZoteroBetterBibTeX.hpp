/*
 * ZoteroBetterBibTeX.hpp
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

#ifndef RSTUDIO_SESSION_MODULES_ZOTERO_BETTER_BIBTEX_HPP
#define RSTUDIO_SESSION_MODULES_ZOTERO_BETTER_BIBTEX_HPP

#include <string>
#include <vector>
#include <map>

#include <boost/function.hpp>

#include "ZoteroCollections.hpp"

namespace rstudio {

namespace core {
   class Error;
   namespace json {
      struct JsonRpcRequest;
      class JsonRpcResponse;
   }
}

namespace session {
namespace modules {
namespace zotero {

bool betterBibtexInConfig(const std::string& config);

bool betterBibtexEnabled();

void betterBibtexProvideIds(const collections::ZoteroCollections& collections,
                            collections::ZoteroCollectionsHandler handler);

core::Error betterBibtexExport(const core::json::JsonRpcRequest&,
                               core::json::JsonRpcResponse* pResponse);

core::Error betterBibtexInit();

} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_ZOTERO_BETTER_BIBTEX_HPP */
