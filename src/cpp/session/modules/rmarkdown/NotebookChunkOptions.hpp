/*
 * NotebookChunkOptions.hpp
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

#ifndef SESSION_NOTEBOOK_CHUNK_OPTIONS_HPP
#define SESSION_NOTEBOOK_CHUNK_OPTIONS_HPP

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
 
namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

class ChunkOptions
{
public:
   ChunkOptions(const core::json::Object& defaultOptions, 
                const core::json::Object& chunkOptions);

   template<typename T> T getOverlayOption(const std::string& key, 
                                           T defaultValue) const
   {
      // check overlay first
      core::Error error = core::json::readObject(chunkOptions_, key, 
            defaultValue);

      // no overlay option, check base
      if (error)
         core::json::readObject(defaultOptions_, key, defaultValue);

      return defaultValue;
   }

   // return overlay only
   const core::json::Object& chunkOptions() const;
   
   // return defaults (from setup chunk)
   const core::json::Object& defaultOptions() const;

   // returned merged object with all options
   core::json::Object mergedOptions() const;
   
private:
   core::json::Object defaultOptions_;
   core::json::Object chunkOptions_;
};

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_NOTEBOOK_CHUNK_OPTIONS_HPP
