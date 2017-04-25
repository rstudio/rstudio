/*
 * SessionObjectExplorer.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#define R_INTERNAL_FUNCTIONS

#include "SessionObjectExplorer.hpp"

#include <boost/bind.hpp>
#include <boost/foreach.hpp>

#include <core/Algorithm.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {      
namespace explorer {

namespace {

const char * const kExplorerCacheDir = "explorer-cache";

FilePath explorerCacheDir() 
{
   return module_context::sessionScratchPath()
         .childPath(kExplorerCacheDir);
}

std::string explorerCacheDirSystem()
{
   return string_utils::utf8ToSystem(explorerCacheDir().absolutePath());
}

void removeOrphanedCacheItems()
{
   Error error;
   
   // if we don't have a cache, nothing to do
   if (!explorerCacheDir().exists())
      return;
   
   // list source documents
   std::vector<FilePath> docPaths;
   error = source_database::list(&docPaths);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // read their properties
   typedef source_database::SourceDocument SourceDocument;
   typedef boost::shared_ptr<SourceDocument> Document;
   
   std::vector<Document> documents;
   BOOST_FOREACH(const FilePath& docPath, docPaths)
   {
      Document pDoc(new SourceDocument());
      Error error = source_database::get(docPath.filename(), false, pDoc);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      documents.push_back(pDoc);
   }
   
   // list objects in explorer cache
   std::vector<FilePath> cachedFiles;
   error = explorerCacheDir().children(&cachedFiles);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // remove any objects for which we don't have an associated
   // source document available
   BOOST_FOREACH(const FilePath& cacheFile, cachedFiles)
   {
      std::string id = cacheFile.filename();
      
      bool foundId = false;
      BOOST_FOREACH(Document pDoc, documents)
      {
         if (id == pDoc->getProperty("id"))
         {
            foundId = true;
            break;
         }
      }
      
      if (!foundId)
      {
         error = cacheFile.remove();
         if (error)
            LOG_ERROR(error);
      }
   }
}

void onShutdown(bool terminatedNormally)
{
   if (!terminatedNormally)
      return;
   
   using namespace r::exec;
   Error error = RFunction(".rs.explorer.saveCache")
         .addParam(explorerCacheDirSystem())
         .call();
   
   if (error)
      LOG_ERROR(error);
}

void onSuspend(const r::session::RSuspendOptions&,
               core::Settings*)
{
   onShutdown(true);
}

void onResume(const Settings&)
{
   
}

void onDocPendingRemove(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   Error error;
   
   // if we have a cache item associated with this document, remove it
   std::string id = pDoc->getProperty("id");
   if (id.empty())
      return;
   
   FilePath cachePath = explorerCacheDir().childPath(id);
   error = cachePath.removeIfExists();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // also attempt to remove from R cache
   using namespace r::exec;
   error = RFunction(".rs.explorer.removeCachedObject")
         .addParam(id)
         .call();
   
   if (error)
      LOG_ERROR(error);
}

void onDeferredInit(bool)
{
   Error error;
   
   error = explorerCacheDir().ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   removeOrphanedCacheItems();
   
   using namespace r::exec;
   error = RFunction(".rs.explorer.restoreCache")
         .addParam(explorerCacheDirSystem())
         .call();
   
   if (error)
      LOG_ERROR(error);
}

SEXP rs_objectClass(SEXP objectSEXP)
{
   SEXP attribSEXP = ATTRIB(objectSEXP);
   if (attribSEXP == R_NilValue)
      return R_NilValue;
   
   while (attribSEXP != R_NilValue)
   {
      SEXP tagSEXP = TAG(attribSEXP);
      if (TYPEOF(tagSEXP) == SYMSXP)
      {
         const char* tag = CHAR(PRINTNAME(tagSEXP));
         if (::strcmp(tag, "class") == 0)
            return CAR(attribSEXP);
      }
      
      attribSEXP = CDR(attribSEXP);
   }
   
   return R_NilValue;
}

SEXP rs_objectAddress(SEXP objectSEXP)
{
   std::stringstream ss;
   ss << std::hex << (void*) objectSEXP;
   
   r::sexp::Protect protect;
   return r::sexp::create(ss.str(), &protect);
}

SEXP rs_objectAttributes(SEXP objectSEXP)
{
   return ATTRIB(objectSEXP);
}

SEXP rs_explorerCacheDir()
{
   r::sexp::Protect protect;
   return r::sexp::create(explorerCacheDirSystem(), &protect);
}

} // end anonymous namespace

core::Error initialize()
{
   using namespace module_context;
   using boost::bind;
   
   module_context::events().onDeferredInit.connect(onDeferredInit);
   module_context::events().onShutdown.connect(onShutdown);
   addSuspendHandler(SuspendHandler(onSuspend, onResume));
   
   source_database::events().onDocPendingRemove.connect(onDocPendingRemove);
   
   RS_REGISTER_CALL_METHOD(rs_objectAddress, 1);
   RS_REGISTER_CALL_METHOD(rs_objectClass, 1);
   RS_REGISTER_CALL_METHOD(rs_objectAttributes, 1);
   RS_REGISTER_CALL_METHOD(rs_explorerCacheDir, 0);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionObjectExplorer.R"));
   
   return initBlock.execute();
}
   
} // namespace explorer
} // namespace modules
} // namespace session
} // namespace rstudio
