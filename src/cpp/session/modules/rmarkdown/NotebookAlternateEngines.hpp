/*
 * NotebookAlternateEngines.hpp
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

#ifndef NOTEBOOK_ALTERNATE_ENGINES_HPP
#define NOTEBOOK_ALTERNATE_ENGINES_HPP

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

core::Error initAlternateEngines();

core::Error executeAlternateEngineChunk(const std::string& docId,
                                  const std::string& chunkId,
                                  const std::string& label,
                                  const std::string& nbCtxId,
                                  const core::FilePath& workingDir,
                                  const std::string& engine,
                                  const std::string& code,
                                  const ChunkOptions& chunkOptions,
                                  ExecScope execScope,
                                  int pixelWidth,
                                  int charWidth);
} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
