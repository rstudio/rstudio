/*
 * NotebookCache.hpp
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


// A notebook .Rmd is accompanied by a cache folder, which has the following
// structure:
//
// - 490293-foo
//   + s
//     - chunks.json
//     + cwiaiw9i4f0
//       - 00001.csv
//   + u-93029-i81023
//     - chunks.json
//     + cwiaiw9i4f0
//       - 00001.png
//       - 00002.csv
//     + lib
//       + htmlwidgets
//         - htmlwidget.js
// 
// That is:
// - the name of the cache folder is formed by its id and the base name of
//   the document
// - there are two types of cache folders -- one for content which has been
//   committed ("s") and one for content which has not been committed ("u");
//   there may be many uncomitted folders, but only one committed folder.
// - the folder contains a sequence of files which represent the content inside
//   the chunk -- textual output, plots, and HTML; this content's output order
//   is implied in the filenames 
// - the special file "chunks.json" indicates the location of the chunks
//   in the source .Rmd
// - the special folder "lib" is used for shared libraries (e.g. scripts upon
//   which several htmlwidget chunks depend)


#ifndef SESSION_NOTEBOOK_CACHE_HPP
#define SESSION_NOTEBOOK_CACHE_HPP

#define kSavedCtx "s"

#include <string>

namespace rstudio {
namespace core {
   class FilePath;
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

core::FilePath notebookCacheRoot();

core::FilePath chunkCacheFolder(const std::string& docPath, 
      const std::string& docId, const std::string& nbCtxId);

core::FilePath chunkCacheFolder(const std::string& docPath, 
      const std::string& docId);

core::FilePath chunkCacheFolder(const core::FilePath& path, 
      const std::string& docId, const std::string& nbCtxId);

core::Error initCache();

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
