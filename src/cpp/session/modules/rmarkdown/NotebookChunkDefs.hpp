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
#include <core/json/JsonRpc.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <ctime>

#include "NotebookCache.hpp"
#include "SessionRmdNotebook.hpp"

#define kNotebookChunkDefFilename "chunks.json"

#define kChunkDefs         "chunk_definitions"
#define kChunkDocWriteTime "doc_write_time"
#define kChunkId           "chunk_id"
#define kChunkKnitDefaults "knit_defaults"


namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

core::Error getChunkJson(const core::FilePath& defs, core::json::Object *pJson)
{
   // read the defs file 
   std::string contents;
   core::Error error = core::readStringFromFile(defs, &contents);
   if (error)
      return error;

   // pull out the contents
   core::json::Value defContents;
   if (!core::json::parse(contents, &defContents) || 
       defContents.type() != core::json::ObjectType)
      return core::Error(core::json::errc::ParseError, ERROR_LOCATION);

   *pJson = defContents.get_obj();

   return core::Success();
}

template <typename T>
core::Error getChunkDefsValue(const core::FilePath& defs, 
      const std::string& key, T* pValue)
{
   // read the defs file 
   core::json::Object defContents;
   core::Error error = getChunkJson(defs, &defContents);
   if (error)
      return error;

   // extract the chunk definitions
   return core::json::readObject(defContents, key, pValue);
}
} // anonymous namespace

void cleanChunks(const core::FilePath& cacheDir,
                 const core::json::Array &oldDefs, 
                 const core::json::Array &newDefs);

core::FilePath chunkDefinitionsPath(const core::FilePath& docPath,
      const std::string& nbCtxId);

core::FilePath chunkDefinitionsPath(const std::string& docPath,
      const std::string& docId, const std::string& nbCtxId);

core::Error setChunkDefs(const std::string& docPath, const std::string& docId, 
      std::time_t docTime, const core::json::Array& defs);

void extractChunkIds(const core::json::Array& chunkOutputs, 
                     std::vector<std::string> *pIds);

template<typename T>
core::Error getChunkValue(const core::FilePath& docPath, 
                          const std::string& nbCtxId, const std::string& key, 
                          T* pValue)
{
   core::FilePath defs = chunkDefinitionsPath(docPath, nbCtxId);
   if (!defs.exists())
      return core::Success();

   return getChunkDefsValue(defs, key, pValue);
}

template<typename T>
core::Error getChunkValue(const std::string& docPath, const std::string& docId,
                          const std::string& nbCtxId, const std::string& key,
                          T *pValue)
{
   core::FilePath defs = chunkDefinitionsPath(docPath, docId, nbCtxId);
   if (!defs.exists())
      return core::Success();

   return getChunkDefsValue(defs, key, pValue);
}

template<typename T>
core::Error getChunkValue(const std::string& docPath, const std::string& docId,
                          const std::string& key, T *pValue)
{
   // try local context first
   core::FilePath defs = chunkDefinitionsPath(docPath, docId, notebookCtxId());
   if (!defs.exists())
   {
      // if no definitions, try the saved context
      defs = chunkDefinitionsPath(docPath, docId, kSavedCtx);
      if (!defs.exists())
         return core::Success();
   }
   return getChunkDefsValue(defs, key, pValue);
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
