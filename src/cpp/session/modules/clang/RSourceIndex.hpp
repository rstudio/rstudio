/*
 * RSourceIndex.hpp
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

#ifndef SESSION_MODULES_CLANG_R_SOURCE_INDEX_HPP
#define SESSION_MODULES_CLANG_R_SOURCE_INDEX_HPP

#include <core/libclang/SourceIndex.hpp>

namespace rstudio {

namespace core {
   class FileInfo;
   class FilePath;
}

namespace session {
namespace modules {      
namespace clang {

core::libclang::SourceIndex& rSourceIndex();

bool isIndexableFile(const core::FileInfo& fileInfo,
                     const core::FilePath& pkgSrcDir,
                     const core::FilePath& pkgIncludeDir);

} // namespace clang
} // namepace handlers
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULES_CLANG_R_SOURCE_INDEX_HPP
