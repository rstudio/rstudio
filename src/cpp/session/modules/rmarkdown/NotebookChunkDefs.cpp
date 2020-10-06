/*
 * NotebookChunkDefs.cpp
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

#include "SessionRmdNotebook.hpp"
#include "NotebookCache.hpp"
#include "NotebookChunkDefs.hpp"

#include <shared_core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/Exec.hpp>
#include <shared_core/FilePath.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

SEXP rs_getRmdWorkingDir(SEXP rmdFileSEXP, SEXP docIdSEXP)
{
   r::sexp::Protect protect;
   FilePath dir;

   // extract the document's path and ID
   std::string docPath = r::sexp::safeAsString(rmdFileSEXP, "");
   if (docPath.empty()) 
      return R_NilValue;
   std::string docId = r::sexp::safeAsString(docIdSEXP, "");
   if (docId.empty()) 
      return R_NilValue;

   // attempt to look up the desired working directory of this document
   std::string workingDir;
   getChunkValue(docPath, docId, kChunkWorkingDir, &workingDir);
   if (!workingDir.empty())
      dir = module_context::resolveAliasedPath(workingDir);

   // if we found a valid working directory, return it
   if (dir.exists())
      return r::sexp::create(dir.getAbsolutePath(), &protect);

   // otherwise, return nothing
   return R_NilValue;
}

} // anonymous namespace

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
   for (const std::string& staleId : staleIds)
   {
      error = cacheDir.completePath(staleId).removeIfExists();
   }
}

FilePath chunkDefinitionsPath(const core::FilePath& docPath,
                              const std::string& nbCtxId)
{
   std::string fileName = std::string() + kNotebookChunkDefFilename;
   return chunkCacheFolder(docPath, "", nbCtxId).completeChildPath(fileName);
}

FilePath chunkDefinitionsPath(const std::string& docPath,
                              const std::string& docId,
                              const std::string& nbCtxId)
{
   std::string fileName = std::string() + kNotebookChunkDefFilename;
   return chunkCacheFolder(docPath, docId, nbCtxId).completeChildPath(fileName);
}

FilePath chunkDefinitionsPath(const std::string& docPath, 
      const std::string docId)
{
   // try local context first
   FilePath defs = chunkDefinitionsPath(docPath, docId, notebookCtxId());

   // if no definitions, try the saved context
   if (!defs.exists())
      defs = chunkDefinitionsPath(docPath, docId, kSavedCtx);

   return defs;
}

Error getChunkJson(const FilePath& defs, json::Object *pJson)
{
   // read the defs file 
   std::string contents;
   Error error = readStringFromFile(defs, &contents);
   if (error)
      return error;

   // pull out the contents
   json::Value defContents;
   if (defContents.parse(contents) ||
       defContents.getType() != json::Type::OBJECT)
      return Error(json::errc::ParseError, ERROR_LOCATION);

   *pJson = defContents.getValue<json::Object>();

   return Success();
}

Error getChunkValues(const std::string& docPath, const std::string& docId, 
      json::Object* pValues)

{
   FilePath defs = chunkDefinitionsPath(docPath, docId);
   if (!defs.exists())
      return Success();

   return getChunkJson(defs, pValues);
}

Error setChunkDefs(boost::shared_ptr<source_database::SourceDocument> pDoc,
                   const json::Array& newDefs)
{
   // ensure we have a place to write the sidecar file
   FilePath defFile = chunkDefinitionsPath(pDoc->path(), pDoc->id(), 
         notebookCtxId());

   // make sure the parent folder exists
   Error error = defFile.getParent().ensureDirectory();
   if (error)
      return error;

   // get the old set of chunk IDs so we can clean up any not in the new set 
   // of chunks
   json::Object defContents;
   if (defFile.exists())
   {
      error = getChunkJson(defFile, &defContents);
      if (error)
         LOG_ERROR(error);
      else
      {
         json::Array oldDefs;
         error = json::readObject(defContents, kChunkDefs, oldDefs);
         if (!error)
         {
            if (oldDefs == newDefs) 
            {
               // definitions not changing; no work to do
               return Success();
            }

            // clean up stale chunks
            cleanChunks(chunkCacheFolder(pDoc->path(), pDoc->id()), 
                        oldDefs, newDefs);
         }
      }
   }

   // update the contents of the file with the new chunk definitions and 
   // write time
   time_t docTime = pDoc->dirty() ? std::time(nullptr) : 
                                    pDoc->lastKnownWriteTime();
   defContents[kChunkDefs] = newDefs;
   defContents[kChunkDocWriteTime] = static_cast<boost::int64_t>(docTime);

   error = writeStringToFile(defFile, defContents.write());
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   return Success();
}

void extractChunkIds(const json::Array& chunkOutputs, 
                     std::vector<std::string> *pIds)
{
   for (const json::Value& chunkOutput : chunkOutputs)
   {
      if (chunkOutput.getType() != json::Type::OBJECT)
         continue;
      std::string chunkId;
      if (!json::readObject(chunkOutput.getObject(), kChunkId, chunkId))
      {
         pIds->push_back(chunkId);
      }
   }
}

core::Error initChunkDefs()
{
   RS_REGISTER_CALL_METHOD(rs_getRmdWorkingDir, 2);
   return Success();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
