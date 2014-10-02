/*
 * EmbeddedLibrary.hpp
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

#ifndef SESSION_MODULES_CLANG_LIBCLANG_EMBEDDED_LIBRARY_HPP
#define SESSION_MODULES_CLANG_LIBCLANG_EMBEDDED_LIBRARY_HPP

#include <string>

#include <boost/function.hpp>

namespace session {
namespace modules {      
namespace clang {
namespace libclang {

struct EmbeddedLibrary
{
   bool empty() const { return ! libraryPath; }
   boost::function<std::string()> libraryPath;
   boost::function<std::vector<std::string>(const LibraryVersion&, bool)>
                                                                compileArgs;
};

} // namespace libclang
} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_LIBCLANG_EMBEDDED_LIBRARY_HPP
