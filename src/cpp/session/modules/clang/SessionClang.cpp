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

#include "libclang/LibClang.hpp"
#include "libclang/UnsavedFiles.hpp"
#include "libclang/SourceIndex.hpp"

#include "CodeCompletion.hpp"
#include "RCompilationDatabase.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace clang {

using namespace libclang;

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

std::vector<std::string> embeddedLibClangCompileArgs(const Version& version,
                                                     bool isCppFile)
{
   std::vector<std::string> compileArgs;

   // headers path
   FilePath headersPath = options().libclangHeadersPath();

   // add compiler headers
   std::string headersVersion = "3.5";
   if (version < Version(3,5,0))
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

Embedded embeddedLibClang()
{
   Embedded embedded;
   embedded.libraryPath = embeddedLibClangPath;
   embedded.compileArgs = embeddedLibClangCompileArgs;
   return embedded;
}

CompilationDatabase compilationDatabase()
{
   CompilationDatabase compilationDB;
   compilationDB.compileArgsForTranslationUnit =
      boost::bind(&RCompilationDatabase::compileArgsForTranslationUnit,
                  &rCompilationDatabase(), _1);
   compilationDB.translationUnits =
      boost::bind(&RCompilationDatabase::translationUnits,
                  &rCompilationDatabase());
   return compilationDB;
}

typedef std::map<std::string,std::string> IdToFile;

void onSourceDocUpdated(boost::shared_ptr<IdToFile> pIdToFile,
                        boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore if the file doesn't have a path
   if (pDoc->path().empty())
      return;

   // resolve to a full path
   FilePath docPath = module_context::resolveAliasedPath(pDoc->path());
   std::string filename = docPath.absolutePath();

   // verify that it's an indexable C/C++ file (we allow any and all
   // files into the database here since these files are open within
   // the source editor)
   if (!SourceIndex::isTranslationUnit(filename))
      return;

   // track the mapping between id and filename
   (*pIdToFile)[pDoc->id()] = filename;

   // update unsaved files (we do this even if the document is dirty
   // as even in this case it will need to be removed from the list
   // of unsaved files)
   unsavedFiles().update(filename, pDoc->contents(), pDoc->dirty());

   // dirty files indicate active user editing, prime if necessary
   if (pDoc->dirty())
   {
      module_context::scheduleDelayedWork(
            boost::posix_time::milliseconds(100),
            boost::bind(&SourceIndex::primeTranslationUnit,
                        &(sourceIndex()), filename),
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
            boost::bind(&SourceIndex::reprimeTranslationUnit,
                        &(sourceIndex()), filename),
            true); // require idle
   }
}

void onSourceDocRemoved(boost::shared_ptr<IdToFile> pIdToFile,
                        const std::string& id)
{
   // get the filename for this id
   IdToFile::iterator it = pIdToFile->find(id);
   if (it != pIdToFile->end())
   {
      // remove from unsaved file
      unsavedFiles().remove(it->second);

      // remove the translation unit
      sourceIndex().removeTranslationUnit(it->second);

      // remove the id from the map
      pIdToFile->erase(it);
   }
}

void onAllSourceDocsRemoved(boost::shared_ptr<IdToFile> pIdToFile)
{
   unsavedFiles().removeAll();
   sourceIndex().removeAllTranslationUnits();
   pIdToFile->clear();
}

const char * const kRequiredRcpp = "0.11.3";

bool haveRequiredRcpp()
{
   return module_context::isPackageVersionInstalled("Rcpp", kRequiredRcpp);
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
   // check for required Rcpp
   else if (haveRequiredRcpp())
   {
      LibClang lib;
      isAvailable = lib.load(embeddedLibClang(), Version(3,4,0), &diagnostics);
   }
   else
   {
      diagnostics = "Rcpp version " + std::string(kRequiredRcpp) + " or "
                    "greater is required in order to use libclang.\n";
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
   return clang().isLoaded();
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
      (bind(registerRpcMethod, "print_cpp_completions", printCppCompletions));
   Error error = initBlock.execute();
   if (error)
      return error;

   // if we have disabled indexing then forget it
   if (cppIndexingDisabled())
      return Success();

   // if we don't have a recent version of Rcpp (that can do dryRun with
   // sourceCpp) then forget it
   if (!haveRequiredRcpp())
      return Success();

   // attempt to initialize libclang interface
   if (!libclang::initialize(compilationDatabase(),
                             embeddedLibClang(),
                             Version(3,4,0),
                             userSettings().clangVerbose()))
   {
      return Success();
   }

   // keep a map of id to filename for source database event forwarding
   boost::shared_ptr<IdToFile> pIdToFile = boost::make_shared<IdToFile>();

   // subscribe to source docs events for maintaining the unsaved files list
   // main source index and the unsaved files list)
   source_database::events().onDocUpdated.connect(
             boost::bind(onSourceDocUpdated, pIdToFile, _1));
   source_database::events().onDocRemoved.connect(
             boost::bind(onSourceDocRemoved, pIdToFile, _1));
   source_database::events().onRemoveAll.connect(
             boost::bind(onAllSourceDocsRemoved, pIdToFile));

   return Success();
}


} // namespace clang
} // namespace modules
} // namesapce session

