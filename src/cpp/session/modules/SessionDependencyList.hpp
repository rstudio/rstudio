/*
 * SessionDependencies.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_DEPENDENCY_LIST_HPP
#define SESSION_DEPENDENCY_LIST_HPP

#include <string>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace shared_core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules { 
namespace dependency_list {

core::Error getDependencyList(core::json::Object *pList);
core::Error initialize();

} // namespace dependency_list
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_DEPENDENCY_LIST_HPP
