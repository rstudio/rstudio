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
#include <r/ROptions.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

#include <core/libclang/LibClang.hpp>

#include "Diagnostics.hpp"
#include "DefinitionIndex.hpp"
#include "FindReferences.hpp"
#include "GoToDefinition.hpp"
#include "CodeCompletion.hpp"
#include "RSourceIndex.hpp"

using namespace rstudio::core ;
using namespace rstudio::core::libclang;

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

namespace {


std::string embeddedLibClangPath()
{
#if defined(_WIN64)
   std::string libclang = "x86_64/libclang.dll";
#elif defined(_WIN32)
   std::string libclang = "x86/libclang.dll";
#elif defined(__APPLE__)
   std::string libclang = "libclang.dylib";
#else
   std::string libclang = "libclang.so";
#endif
   return options().libclangPath().childPath(libclang).absolutePath();
}

std::vector<std::string> embeddedLibClangCompileArgs(const LibraryVersion& version,
                                                     bool isCppFile)
{
   std::vector<std::string> compileArgs;

   // headers path
   FilePath headersPath = options().libclangHeadersPath();

   // add compiler headers
   std::string headersVersion = "3.5";
   if (version < LibraryVersion(3,5,0))
      headersVersion = "3.4";
   compileArgs.push_back("-I" + headersPath.childPath(headersVersion)
                                                   .absolutePath());

   // add libc++ for embedded clang 3.5
   if (isCppFile && (headersVersion == "3.5"))
   {
      compileArgs.push_back("-I" + headersPath.childPath("libc++/3.5")
                                                   .absolutePath());
   }

   return compileArgs;
}

EmbeddedLibrary embeddedLibClang()
{
   EmbeddedLibrary embedded;
   embedded.libraryPath = embeddedLibClangPath;
   embedded.compileArgs = embeddedLibClangCompileArgs;
   return embedded;
}

void onSourceDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore if the file doesn't have a path
   if (pDoc->path().empty())
      return;

   // resolve to a full path
   FilePath docPath = module_context::resolveAliasedPath(pDoc->path());
   std::string filename = docPath.absolutePath();

   // verify that it's a C/C++ file
   if (!SourceIndex::isSourceFile(filename))
      return;

   // update unsaved files
   rSourceIndex().unsavedFiles().update(filename,
                                        pDoc->contents(),
                                        pDoc->dirty());

   // dirty files indicate active user editing, prime if necessary
   if (pDoc->dirty())
   {
      module_context::scheduleDelayedWork(
            boost::posix_time::milliseconds(100),
            boost::bind(&SourceIndex::primeEditorTranslationUnit,
                        &(rSourceIndex()), filename),
            true); // require idle
   }

   // non dirty-files may be eligible for re-priming (i.e. process them again
   // only if they are already in the source index). the reason we don't do
   // this for all source doc updates is that it would expose us to an
   // unbounded number of update operations at IDE startup (based on how
   // many C++ files are open in the source editing pane)
   else
   {
      module_context::scheduleDelayedWork(
            boost::posix_time::milliseconds(100),
            boost::bind(&SourceIndex::reprimeEditorTranslationUnit,
                        &(rSourceIndex()), filename),
            true); // require idle
   }
}

void onSourceDocRemoved(const std::string& id, const std::string& path)
{
   // resolve source database path
   std::string resolvedPath = 
      module_context::resolveAliasedPath(path).absolutePath();

   // remove from unsaved files
   rSourceIndex().unsavedFiles().remove(resolvedPath);

   // remove the translation unit
   rSourceIndex().removeTranslationUnit(resolvedPath);
}

void onAllSourceDocsRemoved()
{
   rSourceIndex().unsavedFiles().removeAll();
   rSourceIndex().removeAllTranslationUnits();
}

bool cppIndexingDisabled()
{
   return ! r::options::getOption<bool>("rstudio.indexCpp", true, false);
}

// diagnostic function to assist in determine whether/where
// libclang was loaded from (and any errors which occurred
// that prevented loading, e.g. inadequate version, missing
// symbols, etc.)
SEXP rs_isLibClangAvailable()
{
   bool isAvailable = false;
   std::string diagnostics;

   // check for explicit disable
   if (cppIndexingDisabled())
   {
      diagnostics = "Libclang is disabled because the rstudio.indexCpp "
                    "option is set to FALSE\n";
   }
   else
   {
      LibClang lib;
      isAvailable = lib.load(embeddedLibClang(),
                             LibraryVersion(3,4,0),
                             &diagnostics);
   }

   // print diagnostics
   module_context::consoleWriteOutput(diagnostics);

   // return status
   r::sexp::Protect rProtect;
   return r::sexp::create(isAvailable, &rProtect);
}

SEXP rs_setClangDiagnostics(SEXP levelSEXP)
{
   int level = r::sexp::asInteger(levelSEXP);
   userSettings().setClangVerbose(level);
   return R_NilValue;
}

} // anonymous namespace
   
bool isAvailable()
{
   return libclang::clang().isLoaded();
}

Error initialize()
{
   // register diagnostics functions
   R_CallMethodDef methodDef1 ;
   methodDef1.name = "rs_isLibClangAvailable" ;
   methodDef1.fun = (DL_FUNC)rs_isLibClangAvailable;
   methodDef1.numArgs = 0;
   r::routines::addCallMethod(methodDef1);

   R_CallMethodDef methodDef2 ;
   methodDef2.name = "rs_setClangDiagnostics" ;
   methodDef2.fun = (DL_FUNC)rs_setClangDiagnostics;
   methodDef2.numArgs = 1;
   r::routines::addCallMethod(methodDef2);

   ExecBlock initBlock ;
   using boost::bind;
   using namespace module_context;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionClang.R"))
      (bind(registerRpcMethod, "get_cpp_diagnostics", getCppDiagnostics))
      (bind(registerRpcMethod, "go_to_cpp_definition", goToCppDefinition))
      (bind(registerRpcMethod, "get_cpp_completions", getCppCompletions))
      (bind(registerRpcMethod, "find_cpp_usages", findUsages));
   Error error = initBlock.execute();
   if (error)
      return error;

   // if we have disabled indexing then forget it
   if (cppIndexingDisabled())
      return Success();

   // attempt to load libclang
   if (!libclang::clang().load(embeddedLibClang(), LibraryVersion(3,4,0)))
      return Success();

   // enable crash recovery
   libclang::clang().toggleCrashRecovery(1);

   // initialize definition index
   error = initializeDefinitionIndex();
   if (error)
      return error;

   // subscribe to source docs events for maintaining the unsaved files list
   // main source index and the unsaved files list)
   source_database::events().onDocUpdated.connect(onSourceDocUpdated);
   source_database::events().onDocRemoved.connect(onSourceDocRemoved);
   source_database::events().onRemoveAll.connect(onAllSourceDocsRemoved);

   return Success();
}


} // namespace clang
} // namespace modules
} // namespace session
} // namespace rstudio

