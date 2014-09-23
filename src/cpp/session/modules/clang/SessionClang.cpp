/*
 * SessionClang.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "SessionClang.hpp"

#include <core/system/System.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/IncrementalFileChangeHandler.hpp>

#include "Clang.hpp"
#include "UnsavedFiles.hpp"
#include "SourceIndex.hpp"


// TODO: consider generalizing a bit and moving into libclang

// TODO: error and null/empty value handling


using namespace core ;

namespace session {
namespace modules { 
namespace clang {

namespace {

bool isCppSourceDoc(const FilePath& docPath)
{
   std::string ex = docPath.extensionLowerCase();
   if (ex == ".c" || ex == ".cc" || ex == ".cpp" || ex == ".m" ||
       ex == ".mm" || ex == ".h" || ex == ".hpp")
   {
      return module_context::isUserFile(docPath);
   }
   else
   {
      return false;
   }
}

bool translationUnitFilter(const FileInfo& fileInfo)
{
   return isCppSourceDoc(FilePath(fileInfo.absolutePath()));
}

void translationUnitChangeHandler(const core::system::FileChangeEvent& event)
{
   using namespace core::system;

   switch(event.type())
   {
      case FileChangeEvent::FileAdded:
      case FileChangeEvent::FileModified:
         sourceIndex().updateTranslationUnit(event.fileInfo().absolutePath());
         break;
      case FileChangeEvent::FileRemoved:
         sourceIndex().removeTranslationUnit(event.fileInfo().absolutePath());
         break;
      case FileChangeEvent::None:
         break;
   }
}

void onSourceDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore if the file doesn't have a path
   if (pDoc->path().empty())
      return;

   // resolve to a full path
   FilePath docPath = module_context::resolveAliasedPath(pDoc->path());

   // verify that it's an indexable C/C++ file
   if (!isCppSourceDoc(docPath))
      return;

   // update unsaved files (we do this even if the document is dirty
   // as even in this case it will need to be removed from the list
   // of unsaved files)
   unsavedFiles().update(pDoc);

   // update the main index (but only if it's not dirty as unsaved
   // edits are tracked separately)
   if (!pDoc->dirty())
      sourceIndex().updateTranslationUnit(docPath.absolutePath());
}


// diagnostic function to assist in determine whether/where
// libclang was loaded from (and any errors which occurred
// that prevented loading, e.g. inadequate version, missing
// symbols, etc.)
SEXP rs_isClangAvailable()
{
   // check availability
   std::string diagnostics;
   bool isAvailable = isClangAvailable(&diagnostics);

   // print diagnostics
   module_context::consoleWriteOutput(diagnostics);

   // return status
   r::sexp::Protect rProtect;
   return r::sexp::create(isAvailable, &rProtect);
}

// incremental file change handler
boost::scoped_ptr<IncrementalFileChangeHandler> pFileChangeHandler;

} // anonymous namespace
   
bool isClangAvailable()
{
   return clang().isLoaded();
}

Error initialize()
{
   // attempt to load clang interface
   loadClang();

   // register diagnostics function
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_isClangAvailable" ;
   methodDef.fun = (DL_FUNC)rs_isClangAvailable;
   methodDef.numArgs = 0;
   r::routines::addCallMethod(methodDef);

   // subscribe to onSourceDocUpdated (used for maintaining both the
   // main source index and the unsaved files list)
   source_database::events().onDocUpdated.connect(onSourceDocUpdated);

   // connect source doc removed events to unsaved files list
   source_database::events().onDocRemoved.connect(
             boost::bind(&UnsavedFiles::remove, &unsavedFiles(), _1));
   source_database::events().onRemoveAll.connect(
             boost::bind(&UnsavedFiles::removeAll, &unsavedFiles()));

   // create incremental file change handler (this is used for updating
   // the main source index). also subscribe it to the file monitor
   pFileChangeHandler.reset(new IncrementalFileChangeHandler(
             translationUnitFilter,
             translationUnitChangeHandler,
             boost::posix_time::milliseconds(200),
             boost::posix_time::milliseconds(20),
             false)); /* allow indexing during idle time */
   pFileChangeHandler->subscribeToFileMonitor("C++ Code Completion");

   // return success
   return Success();
}


} // namespace clang
} // namespace modules
} // namesapce session

