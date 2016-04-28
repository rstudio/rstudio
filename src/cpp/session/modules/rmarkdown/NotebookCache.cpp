/*
 * NotebookCache.cpp
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
#include "SessionRnbParser.hpp"
#include "NotebookCache.hpp"
#include "NotebookChunkDefs.hpp"
#include "NotebookPaths.hpp"

#include <boost/foreach.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>

#include <core/Algorithm.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

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
   Error error = cacheRoot.children(&caches);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   std::string nbCtxId = notebookCtxId();
   BOOST_FOREACH(const FilePath cache, caches)
   {
      // make sure this looks like a notebook cache
      if (!cache.isDirectory())
         continue;
      std::vector<std::string> parts = core::algorithm::split(
            cache.stem(), "-");
      if (parts.size() < 2)
         continue;

      // get the path of the notebook associated with the cache
      FilePath path;
      error = notebookIdToPath(parts[0], nbCtxId, &path);
      if (error)
      {
         if (error.code() == boost::system::errc::no_such_file_or_directory)
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
      error = cache.complete(kCacheVersion).children(&contexts);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      BOOST_FOREACH(const FilePath context, contexts)
      {
         // skip if not our context or the saved context
         if (context.filename() != kSavedCtx &&
             context.filename() != nbCtxId)
            continue;

         // check the write time on the chunk defs file (updated when the doc is
         // mutated or saved)
         FilePath chunkDefs = context.complete(kNotebookChunkDefFilename);
         if (!chunkDefs.exists())
            continue;
         if ((std::time(NULL) - chunkDefs.lastWriteTime()) > kCacheAgeThresholdMs) 
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

Error removeStaleSavedChunks(FilePath& docPath, FilePath& cachePath)
{
   Error error;
   if (!cachePath.exists())
      return Success();

   // extract the set of chunk IDs from the definition files
   json::Value oldDefs;
   json::Value newDefs;
   error = getChunkDefs(docPath, kSavedCtx, NULL, &oldDefs);
   if (error)
      return error;
   error = getChunkDefs(docPath, notebookCtxId(), NULL, &newDefs);
   if (error)
      return error;

   // ensure we got the arrays we expected
   if (oldDefs.type() != json::ArrayType ||
       newDefs.type() != json::ArrayType)
      return Error(json::errc::ParseError, ERROR_LOCATION);

   cleanChunks(cachePath, oldDefs.get_array(), newDefs.get_array());
   return Success();
}

void onDocRemoved(const std::string& docId, const std::string& docPath)
{
   Error error;

   FilePath cacheFolder = chunkCacheFolder(docPath, docId);
   FilePath defFile = chunkDefinitionsPath(docPath, docId, 
         notebookCtxId());
   if (!docPath.empty() && defFile.exists())
   {
      // for saved documents, we want to keep the cache folder around even when
      // the document is closed, but only if the chunk definitions aren't out
      // of sync.
      FilePath docFile = module_context::resolveAliasedPath(docPath);
      std::time_t writeTime;
      error = getChunkDefs(docPath, docId, &writeTime, NULL);

      if (writeTime <= docFile.lastWriteTime())
      {
         // the doc has been saved since the last time the chunks defs were
         // updated, so no work to do here
         return;
      }
   }
   error = cacheFolder.removeIfExists();
   if (error)
      LOG_ERROR(error);
}

void onDocRenamed(const std::string& oldPath, 
                  boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   Error error;
   bool removeOldDir = false;

   // compute cache folders and ignore if we can't safely adjust them
   FilePath oldCacheDir = chunkCacheFolder(oldPath, pDoc->id());
   FilePath newCacheDir = chunkCacheFolder(pDoc->path(), pDoc->id());
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
   if (docPath.extensionLowerCase() != ".rmd")
      return;

   FilePath cachePath = chunkCacheFolder(path, id);

   // clean up incompatible cache versions (as we're about to invalidate them
   // by mutating the document without updating them) 
   if (cachePath.exists())
   {
      std::vector<FilePath> versions;
      cachePath.children(&versions);
      BOOST_FOREACH(const FilePath& version, versions)
      {
         if (version.isDirectory() && version.filename() != kCacheVersion)
         {
            error = version.remove();
            if (error)
               LOG_ERROR(error);
         }
      }
   }

   // TODO: consider write times of document, cache, and .Rnb -- are there
   // combinations which would suggest we should overwrite the cache with the
   // contents of the notebook?
}

void onDocSaved(FilePath &path)
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
   error = cache.children(&children);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   BOOST_FOREACH(const FilePath source, children)
   {
      // compute the target path 
      FilePath target = saved.complete(source.filename());

      if (source.filename() == kNotebookChunkDefFilename) 
      {
         // the definitions should be copied (we always want them in both
         // contexts)
         error = target.removeIfExists();
         if (!error)
            error = source.copy(target);
      }
      else if (source.isDirectory())
      {
         // the chunk output folders should be moved; destroy the old copy
         error = target.removeIfExists();
         if (!error)
            error = source.move(target);
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
   return module_context::sessionScratchPath().childPath("unsaved-notebooks");
}

SEXP rs_chunkCacheFolder(SEXP fileSEXP)
{
   std::string file = r::sexp::safeAsString(fileSEXP);
   FilePath cacheFolder =
         chunkCacheFolder(file, "", userSettings().contextId());
   
   r::sexp::Protect protect;
   return r::sexp::create(cacheFolder.absolutePath(), &protect);
}

} // anonymous namespace

FilePath notebookCacheRoot()
{ 
   return module_context::sharedScratchPath().childPath("notebooks");
}

FilePath chunkCacheFolder(const FilePath& path, const std::string& docId,
      const std::string& nbCtxId)
{
   FilePath folder;
   std::string stem;

   if (path.empty()) 
   {
      // the doc hasn't been saved, so keep its chunk output in the scratch
      // path
      folder = unsavedNotebookCache().childPath(docId)
                                     .childPath(kCacheVersion);
   }
   else
   {
      std::string id;
      Error error = notebookPathToId(path, nbCtxId, &id);
      if (error)
         LOG_ERROR(error);
      
      folder = notebookCacheRoot().childPath(id + "-" + path.stem())
                                  .childPath(kCacheVersion)
                                  .childPath(nbCtxId);
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
   source_database::events().onDocRenamed.connect(onDocRenamed);
   source_database::events().onDocRemoved.connect(onDocRemoved);
   source_database::events().onDocAdded.connect(onDocAdded);

   module_context::events().onSourceEditorFileSaved.connect(onDocSaved);

   RS_REGISTER_CALL_METHOD(rs_chunkCacheFolder, 1);

   module_context::scheduleDelayedWork(boost::posix_time::seconds(30),
      cleanUnusedCaches, true);

   return Success();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

