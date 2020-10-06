/*
 * NotebookCache.cpp
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
#include "NotebookPaths.hpp"
#include "NotebookOutput.hpp"
#include "NotebookHtmlWidgets.hpp"

#include <boost/bind.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>

#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>

// The version identifier for the cache format. Changing this invalidates old
// caches, and should be done only when making breaking changes to the 
// cache format.
#define kCacheVersion "1"

#define kCacheAgeThresholdMs 1000 * 60 * 60 * 24 * 2

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

// it's much faster to load a notebook from its cache than it is to rehydrate
// it from its .Rnb, so we keep it around even if the document is closed (as
// it's somewhat common to open and close a document periodically over the 
// course of working on a project, and it's nice when it opens quickly).
//
// however, we don't want to keep the cache around *forever* just in case we
// might need it, as it can be quite large. as a compromise, an unused cache
// hangs around for a couple of days, then gets automatically swept up by this
// function.
void cleanUnusedCaches()
{
   FilePath cacheRoot = notebookCacheRoot();
   if (!cacheRoot.exists())
      return;

   std::vector<FilePath> caches;
   Error error = cacheRoot.getChildren(caches);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   std::string nbCtxId = notebookCtxId();
   for (const FilePath& cache : caches)
   {
      // make sure this looks like a notebook cache
      if (!cache.isDirectory())
         continue;
      std::vector<std::string> parts = core::algorithm::split(
            cache.getStem(), "-");
      if (parts.size() < 2)
         continue;

      // get the path of the notebook associated with the cache
      FilePath path;
      error = notebookIdToPath(parts[0], &path);
      if (error)
      {
         if (error == systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation()))
         {
            // we have no idea what notebook this cache is for, so it's 
            // unusable; delete it
            error = cache.remove();
            if (error)
               LOG_ERROR(error);
         }
         else 
         {
            LOG_ERROR(error);
         }

         continue;
      }

      // is this document still open? if so, leave the cache alone.
      std::string id;
      source_database::getId(module_context::createAliasedPath(
               FileInfo(path)), &id);
      if (!id.empty())
      {
         continue;
      }

      std::vector<FilePath> contexts;
      error = cache.completePath(kCacheVersion).getChildren(contexts);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      for (const FilePath& context : contexts)
      {
         // skip if not our context or the saved context
         if (context.getFilename() != kSavedCtx &&
             context.getFilename() != nbCtxId)
            continue;

         // check the write time on the chunk defs file (updated when the doc is
         // mutated or saved)
         FilePath chunkDefs = context.completePath(kNotebookChunkDefFilename);
         if (!chunkDefs.exists())
            continue;
         if ((std::time(nullptr) - chunkDefs.getLastWriteTime()) > kCacheAgeThresholdMs) 
         {
            // the cache is old and the document hasn't been opened in a while --
            // remove it.
            error = context.remove();
            if (error)
               LOG_ERROR(error);
         }
      }
   }
}

Error notebookContentMatches(const FilePath& nbPath, const FilePath& rmdPath, 
      bool *pMatches, std::string* pContents)
{
   // extract content from notebook
   std::string nbRmdContents;
   r::exec::RFunction extractRmdFromNotebook(
            ".rs.extractRmdFromNotebook",
            string_utils::utf8ToSystem(nbPath.getAbsolutePath()));
   Error error = extractRmdFromNotebook.call(&nbRmdContents);
   if (error) 
      return error;
   if (pContents)
      *pContents = nbRmdContents;

   // extract contents from Rmd, if present
   std::string rmdContents;
   if (rmdPath.exists())
   {
      error = core::readStringFromFile(rmdPath, &rmdContents);
      if (error)
         return error;
   }

   // calculate match if requested
   if (pMatches) 
   {
      // remove whitespace noise from end of file
      boost::algorithm::trim_right(rmdContents);
      boost::algorithm::trim_right(nbRmdContents);

      *pMatches = rmdContents == nbRmdContents;
   }
      
   return Success();
}

Error removeStaleSavedChunks(FilePath& docPath, FilePath& cachePath)
{
   Error error;
   if (!cachePath.exists())
      return Success();

   // extract the set of chunk IDs from the definition files
   json::Array oldDefs;
   json::Array newDefs;
   error = getChunkValue(docPath, kSavedCtx, kChunkDefs, &oldDefs);
   if (error)
      return error;
   error = getChunkValue(docPath, notebookCtxId(), kChunkDefs, &newDefs);
   if (error)
      return error;

   cleanChunks(cachePath, oldDefs, newDefs);
   return Success();
}

void onDocPendingRemove(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore if doc is unsaved (no path)
   if (pDoc->path().empty())
      return;

   // check for a contextual (uncommitted) chunk definitions file
   FilePath chunkDefsFile = chunkDefinitionsPath(pDoc->path(), pDoc->id(),
         notebookCtxId());
   if (!chunkDefsFile.exists())
      return;

   // if the document's contents match what's on disk, commit the chunk
   // definition file to the saved branch
   bool matches = false;
   Error error = pDoc->contentsMatchDisk(&matches);
   if (error)
      LOG_ERROR(error);
   if (matches)
   {
      FilePath target = chunkDefinitionsPath(
               pDoc->path(), pDoc->id(), kSavedCtx);

      // only perform the copy if the saved branch is stale (older than the
      // uncomitted branch)
      if (target.getLastWriteTime() < chunkDefsFile.getLastWriteTime())
      {
         // remove the old chunk definition file to make way for the new one 
         error = target.remove();
         if (error)
         {
            // can't remove the old definition file, so leave it alone
            LOG_ERROR(error);
         }
         else
         {
            error = chunkDefsFile.copy(target);
            if (error)
            {
               // removed the old file, but could not copy the new one; this
               // should never happen. ideally we'd back up the old file and
               // restore it if we can't copy the new one, but since restoring
               // the backup and copying the new file are effectively the same
               // operation it's unlikely to offer any true improvements in
               // robustness.
               LOG_ERROR(error);
            }
         }
      }
   }
}

void onDocRemoved(const std::string& docId, const std::string& docPath)
{
   // always remove the uncommitted cache when the doc is closed; if it's 
   // opened again, the committed cache will be used to supply its outputs
   FilePath cacheFolder = chunkCacheFolder(docPath, docId, notebookCtxId());
   Error error = cacheFolder.removeIfExists();
   if (error)
      LOG_ERROR(error);
}

void onDocRenamed(const std::string& oldPath, 
                  boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   Error error;
   bool removeOldDir = false;

   // compute cache folders and ignore if we can't safely adjust them
   FilePath oldCacheDir = chunkCacheFolder(oldPath, pDoc->id(), kSavedCtx);
   FilePath newCacheDir = chunkCacheFolder(pDoc->path(), pDoc->id(), kSavedCtx);
   if (!oldCacheDir.exists() || newCacheDir.exists())
      return;

   // if the doc was previously unsaved, we can just move the whole folder 
   // to its newly saved location
   if (oldPath.empty())
   {
      error = oldCacheDir.move(newCacheDir);
      if (error) 
      {
         // if we can't move the cache to the new location, we'll fall back to
         // copy/remove
         removeOldDir = true;
      }
      else
         return;
   }

   error = oldCacheDir.copyDirectoryRecursive(newCacheDir);
   if (error)
   {
      LOG_ERROR(error);
   }
   else if (removeOldDir) 
   {
      // remove old dir if we couldn't move the folder above
      error = oldCacheDir.remove();
      if (error)
         LOG_ERROR(error);
   }
}

void onDocAdded(const std::string& id)
{
   std::string path;
   Error error = source_database::getPath(id, &path);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // ignore empty paths and non-R Markdown files
   if (path.empty())
      return;
   FilePath docPath = module_context::resolveAliasedPath(path);
   if (docPath.getExtensionLowerCase() != ".rmd")
      return;

   // find the cache (test for saved) 
   FilePath cachePath = chunkCacheFolder(path, id, notebookCtxId());
   if (!cachePath.exists())
      cachePath = chunkCacheFolder(path, id, kSavedCtx);

   FilePath notebookPath = docPath.getParent().completePath(docPath.getStem() +
         kNotebookExt);

   // if the cache doesn't exist but we have a notebook file, hydrate from that
   // file
   if (!cachePath.exists() && notebookPath.exists())
   {
      error = r::exec::RFunction(
               ".rs.hydrateCacheFromNotebook",
               string_utils::utf8ToSystem(notebookPath.getAbsolutePath())).call();
      if (error)
         LOG_ERROR(error);
      return;
   }

   // if both the local cache and the notebook cache exist, we need to decide
   // which to load
   if (cachePath.exists() && notebookPath.exists())
   {
      // get the dates first--in no case will we use the notebook over the
      // local cache if the local cache is newer (this will be the case most
      // of the time). we want to check this first because it's cheap; further
      // tests will require us to test the caches for compatibility, which is
      // more expensive.
     
      // find the chunk definition file 
      FilePath chunkDefs = cachePath.completePath(kNotebookChunkDefFilename);
      if (!chunkDefs.exists())
         return;

      std::time_t localCacheTime = chunkDefs.getLastWriteTime();
      std::time_t nbCacheTime = notebookPath.getLastWriteTime();

      if (localCacheTime >= nbCacheTime)
         return;

      // if we got this far, it means that the notebook cache looks newer than
      // our cache -- test to see whether it's compatible
      bool matches = false;
      error = notebookContentMatches(notebookPath, docPath, &matches, nullptr);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
            
      if (!matches)
      {
         // the notebook cache looks newer but it isn't aligned with the .Rmd,
         // so we can't use it
         return;
      }

      // if we got this far, we have matching newer notebook cache. blow away
      // our stale local cache and rehydrate from the notebook cache.
      error = cachePath.remove();
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      error = r::exec::RFunction(
               ".rs.hydrateCacheFromNotebook", 
               string_utils::utf8ToSystem(notebookPath.getAbsolutePath())).call();
      
      if (error)
         LOG_ERROR(error);
   }
}

void onDocSaved(FilePath path)
{
   Error error;
   // ignore non-R Markdown saves
   if (!path.hasExtensionLowerCase(".rmd"))
      return;

   // find cache folder (bail out if it doesn't exist)
   FilePath cache = chunkCacheFolder(path, "", notebookCtxId());
   if (!cache.exists())
      return;

   FilePath saved = chunkCacheFolder(path, "", kSavedCtx);
   if (saved.exists())
   {
      // tidy up: remove any saved chunks that no longer exist
      error = removeStaleSavedChunks(path, saved);
      if (error)
         LOG_ERROR(error);
   }
   else
   {
      // no saved context yet; ensure we have a place to put it
      saved.ensureDirectory();
   }

   // move all the chunk definitions over to the saved context
   std::vector<FilePath> children;
   error = cache.getChildren(children);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   for (const FilePath& source : children)
   {
      // compute the target path 
      FilePath target = saved.completePath(source.getFilename());

      if (source.getFilename() == kNotebookChunkDefFilename) 
      {
         // the definitions should be copied (we always want them in both
         // contexts)
         error = target.removeIfExists();
         if (!error)
         {
            error = source.copy(target);

            // copying the definitions file doesn't copy the ACL under some
            // configurations, so have our helper fix it up
            module_context::events().onPermissionsChanged(target);
         }
      }
      else if (source.isDirectory())
      {
         // library folders should be merged and then removed, so we don't
         // lose library contents 
         if (source.getFilename() == kChunkLibDir)
         {
            error = mergeLib(source, target);
            if (!error)
               error = source.remove();
         }
         else
         {
            // the chunk output folders should be moved; destroy the old copy
            error = target.removeIfExists();
            if (!error)
               error = source.move(target);
         }
      }
      else
      {
         // nothing besides the chunks.json and chunk folders should be here,
         // so ignore other files/content
         continue;
      }

      if (error)
         LOG_ERROR(error);
   }
}

FilePath unsavedNotebookCache()
{
   return module_context::sessionScratchPath().completeChildPath("unsaved-notebooks");
}

SEXP rs_chunkCacheFolder(SEXP fileSEXP)
{
   std::string file = r::sexp::safeAsString(fileSEXP);
   FilePath cacheFolder = chunkCacheFolder(file, "", kSavedCtx);
   
   r::sexp::Protect protect;
   return r::sexp::create(cacheFolder.getAbsolutePath(), &protect);
}

Error createNotebookFromCache(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   std::string rmdPath, outputPath;
   Error error = json::readParams(request.params, &rmdPath, &outputPath);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   SEXP resultSEXP = R_NilValue;
   r::sexp::Protect protect;
   r::exec::RFunction createNotebook(".rs.createNotebookFromCache");
   createNotebook.addParam(string_utils::utf8ToSystem(rmdPath));
   createNotebook.addParam(string_utils::utf8ToSystem(outputPath));
   error = createNotebook.call(&resultSEXP, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // bump the write time on our local chunk definition file so that it matches
   // the notebook file; this prevents us from thinking that the .nb.html file
   // we just wrote is ahead of the local cache.
   FilePath outputFile = module_context::resolveAliasedPath(outputPath);
   FilePath chunkDefsFile = chunkDefinitionsPath(
         module_context::resolveAliasedPath(rmdPath), kSavedCtx);
   if (chunkDefsFile.exists() && 
       chunkDefsFile.getLastWriteTime() < outputFile.getLastWriteTime())
      chunkDefsFile.setLastWriteTime(outputFile.getLastWriteTime());

   // convert the result into JSON for the client
   json::Value result;
   error = r::json::jsonValueFromList(resultSEXP, &result);
   if (error)
      LOG_ERROR(error);
   else
      pResponse->setResult(result);
   
   return Success();
}

Error extractRmdFromNotebook(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   std::string nbPathVal;
   Error error = json::readParams(request.params, &nbPathVal);

   std::string nbRmdContents;
   FilePath nbPath = module_context::resolveAliasedPath(nbPathVal);

   // form the stem name (a little extra work since .nb.html isn't a simple
   // extension)
   std::string stem = nbPath.getFilename().substr(0, 
         (nbPath.getFilename().length() - sizeof(kNotebookExt)) + 1);

   // if the Rmd file exists on disk, see if it matches. check both upper/lower
   // case variants; check the canonical version last so we'll hydrate to that
   // file.
   FilePath rmdPath = nbPath.getParent().completePath(stem + ".rmd");
   if (!rmdPath.exists())
      rmdPath = nbPath.getParent().completePath(stem + ".Rmd");

   // set up values to send to the server
   std::string docId;
   std::string docPath;

   bool matches = false;
   error = notebookContentMatches(nbPath, rmdPath, &matches, &nbRmdContents);
   if (error)
      return error;
      
   FilePath cacheFolder;
   if (rmdPath.exists() && !matches)
   {
      // if it doesn't match, we need to hydrate into an untitled document
      using namespace source_database;

      boost::shared_ptr<SourceDocument> pDoc(
            new SourceDocument(kSourceDocumentTypeRMarkdown));
      pDoc->setContents(nbRmdContents);
      error = put(pDoc);
      if (error)
         return error;

      cacheFolder = chunkCacheFolder(pDoc->path(), pDoc->id(), 
            notebookCtxId());
      docId = pDoc->id();
   }

   // if we didn't select a cache folder already, use one based on the R
   // Markdown document path
   if (cacheFolder.isEmpty())
   {
      // hydrate the R markdown document and record
      error = core::writeStringToFile(rmdPath, nbRmdContents);
      if (error)
         return error;
      docPath = module_context::createAliasedPath(rmdPath);
      
      // assign the cache folder (remove any existing folder) 
      cacheFolder = chunkCacheFolder(rmdPath, "", kSavedCtx);
      error = cacheFolder.removeIfExists();
      if (error)
         return error;
   }

   // perform the cache hydration
   error = r::exec::RFunction(
            ".rs.hydrateCacheFromNotebook",
            string_utils::utf8ToSystem(nbPath.getAbsolutePath()),
            string_utils::utf8ToSystem(cacheFolder.getAbsolutePath())).call();
   if (error)
      return error;

   // format result for client and emit
   json::Object result;
   result["doc_id"]   = docId;
   result["doc_path"] = docPath;
   pResponse->setResult(result);

   return Success();
}

} // anonymous namespace

FilePath notebookCacheRoot()
{ 
   return module_context::sharedScratchPath().completeChildPath("notebooks");
}

FilePath chunkCacheFolder(const FilePath& path, const std::string& docId,
      const std::string& nbCtxId)
{
   FilePath folder;
   std::string stem;

   if (path.isEmpty())
   {
      // the doc hasn't been saved, so keep its chunk output in the scratch
      // path
      folder = unsavedNotebookCache().completeChildPath(docId)
                                     .completeChildPath(kCacheVersion);
   }
   else
   {
      std::string id;
      Error error = notebookPathToId(path, &id);
      if (error)
         LOG_ERROR(error);
      
      folder = notebookCacheRoot().completeChildPath(id + "-" + path.getStem())
                                  .completeChildPath(kCacheVersion)
                                  .completeChildPath(nbCtxId);
   }

   return folder;
}

FilePath chunkCacheFolder(const std::string& docPath, const std::string& docId,
      const std::string& nbCtxId)
{
   return chunkCacheFolder(
         docPath.empty() ? FilePath() : 
                           module_context::resolveAliasedPath(docPath),
         docId, nbCtxId);
}

FilePath chunkCacheFolder(const std::string& docPath, const std::string& docId)
{
   return chunkCacheFolder(docPath, docId, notebookCtxId());
}

Error initCache()
{
   using namespace module_context;
   using boost::bind;

   source_database::events().onDocRenamed.connect(onDocRenamed);
   source_database::events().onDocPendingRemove.connect(onDocPendingRemove);
   source_database::events().onDocRemoved.connect(onDocRemoved);
   source_database::events().onDocAdded.connect(onDocAdded);

   module_context::events().onSourceEditorFileSaved.connect(onDocSaved);

   RS_REGISTER_CALL_METHOD(rs_chunkCacheFolder, 1);

   module_context::scheduleDelayedWork(boost::posix_time::seconds(30),
      cleanUnusedCaches, true);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "create_notebook_from_cache", 
            createNotebookFromCache))
      (bind(registerRpcMethod, "extract_rmd_from_notebook", 
            extractRmdFromNotebook));
   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

