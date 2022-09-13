/*
 * SessionObjectExplorer.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <boost/bind/bind.hpp>

#include <core/Algorithm.hpp>
#include <core/RecursionGuard.hpp>
#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {      
namespace explorer {

namespace {

const char * const kExplorerCacheDir = "explorer-cache";

FilePath explorerCacheDir() 
{
   return module_context::sessionScratchPath().completeChildPath(kExplorerCacheDir);
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
   for (const FilePath& docPath : docPaths)
   {
      Document pDoc(new SourceDocument());
      Error error = source_database::get(docPath.getFilename(), false, pDoc);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      documents.push_back(pDoc);
   }
   
   // list objects in explorer cache
   std::vector<FilePath> cachedFiles;
   error = explorerCacheDir().getChildren(cachedFiles);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // remove any objects for which we don't have an associated
   // source document available
   for (const FilePath& cacheFile : cachedFiles)
   {
      std::string id = cacheFile.getFilename();
      
      bool foundId = false;
      for (Document pDoc : documents)
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
         .addUtf8Param(explorerCacheDir())
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

void removeFromRCache(const std::string& id)
{
   // also attempt to remove from R cache
   using namespace r::exec;
   Error error = RFunction(".rs.explorer.removeCacheEntry")
         .addParam(id)
         .call();

   if (error)
      LOG_ERROR(error);
}

void onDocPendingRemove(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   Error error;
   
   // if we have a cache item associated with this document, remove it
   std::string id = pDoc->getProperty("id");
   if (id.empty())
      return;
   
   FilePath cachePath = explorerCacheDir().completeChildPath(id);
   error = cachePath.removeIfExists();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   module_context::executeOnMainThread(boost::bind(removeFromRCache, id));
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
         .addUtf8Param(explorerCacheDir())
         .call();
   
   if (error)
      LOG_ERROR(error);
}

void onDetectChanges(module_context::ChangeSource source)
{
   DROP_RECURSIVE_CALLS;

   if (!core::thread::isMainThread())
      return;

   // unlikely that data will change outside of a REPL
   if (source != module_context::ChangeSourceREPL) 
      return;

   Error error;
   r::sexp::Protect rProtect;
   SEXP envCache = R_NilValue;
   error = r::exec::RFunction(".rs.explorer.getCache").call(&envCache, &rProtect);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   std::vector<std::string> cached;
   error = r::sexp::objects(envCache, false, &cached);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   for (const std::string& id: cached)
   {
      SEXP s = Rf_install(id.c_str());
      SEXP entry = Rf_findVarInFrame(envCache, s);

      // basic safety check on entry: make sure it's a 
      // list of 5 elements
      if (TYPEOF(entry) != VECSXP || Rf_length(entry) != 5)
         continue;
      
      SEXP object = VECTOR_ELT(entry, 0);
      SEXP name = VECTOR_ELT(entry, 1);
      SEXP title = VECTOR_ELT(entry, 2);
      SEXP envir = VECTOR_ELT(entry, 4);

      // when envir is the empty env, this indicates
      // that this was initially a View(<some code>)
      // i.e. `x` is not a named object from an environment
      // so don't update it
      if (envir == R_EmptyEnv)
         continue;

      SEXP symbol = Rf_install(CHAR(STRING_ELT(name, 0)));
      SEXP newObject = Rf_findVarInFrame(envir, symbol);

      // no object of that name, don't update
      if (newObject == R_UnboundValue)
         continue;
      
      // no change
      if (newObject == object)
         continue;

      // update the object
      SET_VECTOR_ELT(entry, 0, newObject);

      // Should the new object still use objcet explorer 
      bool shouldUseExplorer = true;
      error = r::exec::RFunction(".rs.dataViewer.shouldUseObjectExplorer")
         .addParam(newObject)
         .call(&shouldUseExplorer);
      
      if (shouldUseExplorer) 
      {
         // just refresh the View
         error = r::exec::RFunction(".rs.explorer.refresh")
            .addUtf8Param(id)
            .addParam(entry)
            .call();
         if (error)
         {
            LOG_ERROR(error);
            return;
         }
      }
      else 
      {
         // close it, because the object explorer is no longer 
         // the best way to show that object. 
         error = r::exec::RFunction(".rs.explorer.close")
            .addUtf8Param(id)
            .addParam(entry)
            .call();
         if (error)
         {
            LOG_ERROR(error);
            return;
         }

         // then just let View() show it
         error = r::exec::RFunction("View")
            .addParam(symbol)
            .addParam(title)
            .call(envir, true);
         if (error)
         {
            LOG_ERROR(error);
            return;
         }
      }
      
   }
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

SEXP rs_getRefCount(SEXP nameSEXP, SEXP envirSEXP)
{
   SEXP objectSEXP = ::Rf_findVarInFrame(envirSEXP, nameSEXP);
   if (objectSEXP == R_UnboundValue)
      return R_NilValue;
   objectSEXP = r::sexp::forcePromise(objectSEXP);
      
   r::sexp::Protect protect;
   return r::sexp::create(NAMED(objectSEXP), &protect);
}

SEXP rs_setRefCount(SEXP nameSEXP, SEXP envirSEXP, SEXP countSEXP)
{
   SEXP objectSEXP = ::Rf_findVarInFrame(envirSEXP, nameSEXP);
   if (objectSEXP == R_UnboundValue)
      return R_NilValue;
   objectSEXP = r::sexp::forcePromise(objectSEXP);
   
   int count = r::sexp::asInteger(countSEXP);
   SET_NAMED(objectSEXP, count);
   return countSEXP;
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
   return r::sexp::createUtf8(explorerCacheDir(), &protect);
}

} // end anonymous namespace

core::Error initialize()
{
   using namespace module_context;
   using boost::bind;
   
   module_context::events().onDeferredInit.connect(onDeferredInit);
   module_context::events().onShutdown.connect(onShutdown);
   module_context::events().onDetectChanges.connect(onDetectChanges);
   addSuspendHandler(SuspendHandler(onSuspend, onResume));
   
   source_database::events().onDocPendingRemove.connect(onDocPendingRemove);
   
   RS_REGISTER_CALL_METHOD(rs_getRefCount, 2);
   RS_REGISTER_CALL_METHOD(rs_setRefCount, 3);
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
