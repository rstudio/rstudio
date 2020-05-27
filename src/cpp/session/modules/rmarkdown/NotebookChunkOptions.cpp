/*
 * NotebookChunkOptions.cpp
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

#include "NotebookChunkOptions.hpp"

using namespace rstudio::core;
 
namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

ChunkOptions::ChunkOptions(const json::Object& defaultOptions, 
                           const json::Object& chunkOptions) :
   defaultOptions_(defaultOptions),
   chunkOptions_(chunkOptions)
{
}

const core::json::Object& ChunkOptions::chunkOptions() const
{
   return chunkOptions_;
}

const core::json::Object& ChunkOptions::defaultOptions() const
{
   return defaultOptions_;
}


core::json::Object ChunkOptions::mergedOptions() const
{
   json::Object merged(defaultOptions_);
   for (json::Object::Iterator it = chunkOptions_.begin();
        it != chunkOptions_.end();
        it ++)
   {
      merged.insert(*it);
   }

   return merged;
}
   
} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

