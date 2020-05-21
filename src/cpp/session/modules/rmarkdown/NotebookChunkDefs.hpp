/*
 * NotebookChunkDefs.hpp
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

#ifndef SESSION_NOTEBOOK_CHUNK_DEFS_HPP
#define SESSION_NOTEBOOK_CHUNK_DEFS_HPP

#include <shared_core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <ctime>

#include <session/SessionSourceDatabase.hpp>

#include "NotebookCache.hpp"
#include "SessionRmdNotebook.hpp"

#define kNotebookChunkDefFilename "chunks.json"

#define kChunkDefs           "chunk_definitions"
#define kChunkDocWriteTime   "doc_write_time"
#define kChunkId             "chunk_id"
#define kChunkDefaultOptions "default_chunk_options"
#define kChunkWorkingDir     "working_dir"
#define kChunkExternals      "external_chunks"

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

core::Error getChunkJson(const core::FilePath& defs, core::json::Object *pJson);

namespace {

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
   return core::json::readObject(defContents, key, *pValue);
}
} // anonymous namespace

void cleanChunks(const core::FilePath& cacheDir,
                 const core::json::Array &oldDefs, 
                 const core::json::Array &newDefs);

core::FilePath chunkDefinitionsPath(const core::FilePath& docPath,
      const std::string& nbCtxId);

core::FilePath chunkDefinitionsPath(const std::string& docPath,
      const std::string& docId, const std::string& nbCtxId);

core::FilePath chunkDefinitionsPath(const std::string& docPath, 
      const std::string docId);

core::Error setChunkDefs(
      boost::shared_ptr<source_database::SourceDocument> pDoc, 
      const core::json::Array& defs);

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
   core::FilePath defs = chunkDefinitionsPath(docPath, docId);
   if (!defs.exists())
   {
      return core::Success();
   }
   return getChunkDefsValue(defs, key, pValue);
}

core::Error getChunkValues(const std::string& docPath, const std::string& docId, 
      core::json::Object* pValues);

template<typename T>
core::Error setChunkValue(const std::string& docPath, 
                          const std::string& docId,
                          const std::string& key, T value)
{
   // find the file path to write 
   core::Error error;
   core::FilePath defFile = chunkDefinitionsPath(docPath, docId);
   if (!defFile.exists())
   {
      defFile = chunkDefinitionsPath(docPath, docId, notebookCtxId());
      error = defFile.getParent().ensureDirectory();
      if (error)
         return error;
   }

   // extract existing definitions if we have them
   core::json::Object defs;
   if (defFile.exists())
   {
      error = getChunkJson(defFile, &defs);
      if (error)
         return error;
   }

   // update key and write out new contents
   defs[key] = value;
   return core::writeStringToFile(defFile, defs.write());
}

core::Error initChunkDefs();

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
