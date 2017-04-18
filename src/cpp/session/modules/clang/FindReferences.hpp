/*
 * FindReferences.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef SESSION_MODULES_CLANG_FIND_REFERENCES_HPP
#define SESSION_MODULES_CLANG_FIND_REFERENCES_HPP

#include <vector>

#include <core/Error.hpp>

#include <core/json/JsonRpc.hpp>
 
namespace rstudio {

namespace core {
namespace libclang {
   struct FileLocation;
   struct FileRange;
}
}

namespace session {
namespace modules {      
namespace clang {

core::Error findReferences(const core::libclang::FileLocation& location,
                           std::string* pSpelling,
                           std::vector<core::libclang::FileRange>* pRefs);

core::Error findUsages(const core::json::JsonRpcRequest& request,
                       core::json::JsonRpcResponse* pResponse);
   
} // namespace clang
} // namepace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULES_CLANG_FIND_REFERENCES_HPP
