/*
 * NotebookChunkDefs.cpp
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

#include "SessionRmdNotebook.hpp"
#include "NotebookCache.hpp"
#include "NotebookChunkDefs.hpp"

#include <boost/foreach.hpp>

#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <session/SessionUserSettings.hpp>

#define kChunkDefs         "chunk_definitions"
#define kChunkDocWriteTime "doc_write_time"
#define kChunkId           "chunk_id"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

// given and old and new set of chunk definitions, cleans up all the chunks
// files in the old set but not in the new set
void cleanChunks(const FilePath& cacheDir,
                 const json::Array &oldDefs, 
                 const json::Array &newDefs)
{
   Error error;
   std::vector<std::string> oldIds, newIds;

   // extract chunk IDs from JSON objects
   extractChunkIds(oldDefs, &oldIds);
   extractChunkIds(newDefs, &newIds);

   // compute the set of stale IDs
   std::vector<std::string> staleIds;
   std::sort(oldIds.begin(), oldIds.end());
   std::sort(newIds.begin(), newIds.end());
   std::set_difference(oldIds.begin(), oldIds.end(),
                       newIds.begin(), newIds.end(), 
                       std::back_inserter(staleIds));

   // remove each stale folder from the system
   BOOST_FOREACH(const std::string& staleId, staleIds)
   {
      error = cacheDir.complete(staleId).removeIfExists();
   }
}

} // anonymous namespace

FilePath chunkDefinitionsPath(const std::string& docPath,
                              const std::string& docId,
                              const std::string& contextId)
{
   std::string fileName = std::string() + "chunks.json";
   return chunkCacheFolder(docPath, docId, contextId).childPath(fileName);
}

Error setChunkDefs(const std::string& docPath, const std::string& docId,
                   std::time_t docTime, const json::Array& newDefs)
{
   // create JSON object wrapping 
   json::Object chunkDefs;
   chunkDefs[kChunkDefs] = newDefs;
   chunkDefs[kChunkDocWriteTime] = static_cast<boost::int64_t>(docTime);

   // ensure we have a place to write the sidecar file
   FilePath defFile = chunkDefinitionsPath(docPath, docId, 
         userSettings().contextId());

   // if there are no old chunk definitions and we aren't adding any new ones,
   // no work to do
   if (!defFile.exists() && newDefs.size() < 1) 
      return Success();

   // we're going to write something; make sure the parent folder exists
   Error error = ensureCacheFolder(defFile.parent());
   if (error)
      return error;

   // get the old set of chunk IDs so we can clean up any not in the new set 
   // of chunks
   std::vector<std::string> chunkIds;
   json::Value oldDefs;
   std::string oldContent;
   error = getChunkDefs(docPath, docId, userSettings().contextId(), NULL, 
         &oldDefs);
   if (error)
      LOG_ERROR(error);
   else if (oldDefs.type() == json::ArrayType)
   {
      if (oldDefs.get_array() == newDefs) 
      {
         // definitions not changing; no work to do
         return Success();
      }
      cleanChunks(chunkCacheFolder(docPath, docId),
                  oldDefs.get_array(), newDefs);
   }

   std::ostringstream oss;
   json::write(chunkDefs, oss);

   error = writeStringToFile(defFile, oss.str());
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   return Success();
}

Error getChunkDefs(const std::string& docPath, const std::string& docId,
                   const std::string& contextId, time_t *pDocTime, 
                   core::json::Value* pDefs)
{
   Error error;
   FilePath defs = chunkDefinitionsPath(docPath, docId, contextId);
   if (!defs.exists())
      return Success();

   // read the defs file 
   std::string contents;
   error = readStringFromFile(defs, &contents);
   if (error)
      return error;

   // pull out the contents
   json::Value defContents;
   if (!json::parse(contents, &defContents) || 
       defContents.type() != json::ObjectType)
      return Error(json::errc::ParseError, ERROR_LOCATION);

   // extract the chunk definitions
   if (pDefs)
   {
      json::Array chunkDefs;
      error = json::readObject(defContents.get_obj(), kChunkDefs, &chunkDefs);
      if (error)
         return error;

      // return to caller
      *pDefs = chunkDefs;
   }

   // extract the doc write time 
   if (pDocTime)
   {
      json::Object::iterator it = 
         defContents.get_obj().find(kChunkDocWriteTime);
      if (it != defContents.get_obj().end() &&
          it->second.type() == json::IntegerType)
      {
         *pDocTime = static_cast<std::time_t>(it->second.get_int64());
      }
      else
      {
         return Error(json::errc::ParamMissing, ERROR_LOCATION);
      }
   }
   return Success();
}

Error getChunkDefs(const std::string& docPath, const std::string& docId,
                   time_t *pDocTime, core::json::Value* pDefs)
{
   return getChunkDefs(docPath, docId, userSettings().contextId(), 
                       pDocTime, pDefs);
}

void extractChunkIds(const json::Array& chunkOutputs, 
                     std::vector<std::string> *pIds)
{
   BOOST_FOREACH(const json::Value& chunkOutput, chunkOutputs)
   {
      if (chunkOutput.type() != json::ObjectType)
         continue;
      std::string chunkId;
      if (json::readObject(chunkOutput.get_obj(), kChunkId, &chunkId) ==
            Success()) 
      {
         pIds->push_back(chunkId);
      }
   }
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
