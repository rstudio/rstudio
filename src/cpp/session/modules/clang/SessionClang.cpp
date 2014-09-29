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

#include <core/Exec.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/System.hpp>
#include <core/system/Process.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include "libclang/LibClang.hpp"
#include "libclang/UnsavedFiles.hpp"
#include "libclang/SourceIndex.hpp"
#include "libclang/CompilationDatabase.hpp"

#include "CodeCompletion.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace clang {

using namespace libclang;

namespace {

bool isCppSourceDoc(const FilePath& filePath)
{
   std::string ex = filePath.extensionLowerCase();
   return (ex == ".c" || ex == ".cc" || ex == ".cpp" ||
           ex == ".m" || ex == ".mm");
}

void onSourceDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore if the file doesn't have a path
   if (pDoc->path().empty())
      return;

   // resolve to a full path
   FilePath docPath = module_context::resolveAliasedPath(pDoc->path());

   // verify that it's an indexable C/C++ file (we allow any and all
   // files into the database here since these files are open within
   // the source editor)
   if (!isCppSourceDoc(docPath))
      return;

   // update unsaved files (we do this even if the document is dirty
   // as even in this case it will need to be removed from the list
   // of unsaved files)
   unsavedFiles().update(pDoc);

   // update the index (later) if this was a full save
   if (!pDoc->dirty())
   {
      module_context::scheduleDelayedWork(
            boost::posix_time::milliseconds(500),
            boost::bind(&SourceIndex::getTranslationUnit,
                        &(sourceIndex()), docPath.absolutePath()),
            true); // require idle
   }
}


// diagnostic function to assist in determine whether/where
// libclang was loaded from (and any errors which occurred
// that prevented loading, e.g. inadequate version, missing
// symbols, etc.)
SEXP rs_isLibClangAvailable()
{
   // check availability
   std::string diagnostics;
   bool isAvailable = isLibClangAvailable(&diagnostics);

   // print diagnostics
   module_context::consoleWriteOutput(diagnostics);

   // return status
   r::sexp::Protect rProtect;
   return r::sexp::create(isAvailable, &rProtect);
}

} // anonymous namespace
   
bool isAvailable()
{
   return clang().isLoaded();
}

Error initialize()
{
   // if we don't have a recent version of Rcpp (that can do dryRun with
   // sourceCpp) then forget it
   if (!module_context::isPackageVersionInstalled("Rcpp", "0.11.2.7"))
      return Success();

   // attempt to load clang interface
   loadLibClang();

   // register diagnostics function
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_isLibClangAvailable" ;
   methodDef.fun = (DL_FUNC)rs_isLibClangAvailable;
   methodDef.numArgs = 0;
   r::routines::addCallMethod(methodDef);

   // subscribe to source docs events for maintaining the unsaved files list
   // main source index and the unsaved files list)
   source_database::events().onDocUpdated.connect(onSourceDocUpdated);
   source_database::events().onDocRemoved.connect(
             boost::bind(&UnsavedFiles::remove, &unsavedFiles(), _1));
   source_database::events().onRemoveAll.connect(
             boost::bind(&UnsavedFiles::removeAll, &unsavedFiles()));

   ExecBlock initBlock ;
   using boost::bind;
   using namespace module_context;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionClang.R"))
      (bind(registerRpcMethod, "print_cpp_completions", printCppCompletions));
   return initBlock.execute();

   // return success
   return Success();
}


} // namespace clang
} // namespace modules
} // namesapce session

