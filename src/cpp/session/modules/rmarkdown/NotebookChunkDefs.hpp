/*
 * NotebookChunkDefs.hpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#ifndef SESSION_NOTEBOOK_CHUNK_DEFS_HPP
#define SESSION_NOTEBOOK_CHUNK_DEFS_HPP

#include <core/json/Json.hpp>
#include <ctime>

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

core::FilePath chunkDefinitionsPath(const std::string& docPath,
      const std::string& docId, const std::string& nbCtxId);

core::Error getChunkDefs(const std::string& docPath, const std::string& docId, 
      std::time_t *pDocTime, core::json::Value* pDefs);

core::Error getChunkDefs(const std::string& docPath, const std::string& docId, 
      const std::string& nbCtxId, std::time_t *pDocTime, 
      core::json::Value* pDefs);

core::Error setChunkDefs(const std::string& docPath, const std::string& docId, 
      std::time_t docTime, const core::json::Array& defs);

void extractChunkIds(const core::json::Array& chunkOutputs, 
                     std::vector<std::string> *pIds);

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
