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
#include <session/IncrementalFileChangeHandler.hpp>

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

bool isMakefile(const FilePath& filePath, const std::string& pkgSrcDir)
{
   if (filePath.parent().absolutePath() == pkgSrcDir)
   {
      std::string filename = filePath.filename();
      return filename == "Makevars" ||
             filename == "Makevars.win" ||
             filename == "Makefile";
   }
   else
   {
      return false;
   }
}

bool isPackageBuildFile(const FilePath& filePath)
{
   using namespace projects;
   FilePath buildTargetPath = projectContext().buildTargetPath();
   FilePath descPath = buildTargetPath.childPath("DESCRIPTION");
   FilePath srcPath = buildTargetPath.childPath("src");
   if (filePath == descPath)
   {
      return true;
   }
   else if (isMakefile(filePath, srcPath.absolutePath()))
   {
      return true;
   }
   else
   {
      return false;
   }
}

bool packageCppFileFilter(const std::string& pkgSrcDir,
                          const std::string& pkgDescFile,
                          const FileInfo& fileInfo)
{
   // DESCRIPTION file
   if (fileInfo.absolutePath() == pkgDescFile)
   {
      return true;
   }
   // otherwise must be an appropriate file type within the src directory
   else if (boost::algorithm::starts_with(fileInfo.absolutePath(), pkgSrcDir))
   {
      FilePath filePath(fileInfo.absolutePath());
      if (isCppSourceDoc(filePath))
      {
         return true;
      }
      else if (isMakefile(filePath, pkgSrcDir))
      {
         return true;
      }
      else
      {
         return false;
      }
   }
   else
   {
      return false;
   }
}

void fileChangeHandler(const core::system::FileChangeEvent& event)
{
   using namespace core::system;

   FilePath filePath(event.fileInfo().absolutePath());

   // is this a source file? if so updated the source index
   if (isCppSourceDoc(filePath))
   {
      // new files mean we need to update the package compilation database
      if (event.type() == FileChangeEvent::FileAdded)
      {
         compilationDatabase().updateForPackageCppAddition(filePath);
         sourceIndex().updateTranslationUnit(filePath.absolutePath());
      }
      else if (event.type() == FileChangeEvent::FileModified)
      {
         sourceIndex().updateTranslationUnit(filePath.absolutePath());
      }
      else if (event.type() == FileChangeEvent::FileRemoved)
      {
         sourceIndex().removeTranslationUnit(filePath.absolutePath());
      }
   }

   // is this a build related file? if so update the compilation database
   else if (isPackageBuildFile(filePath))
   {
      compilationDatabase().updateForCurrentPackage();
   }
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

   // if the file isn't dirty of if we've never seen it before then
   // update the compilation database and/or source index
   std::string path = docPath.absolutePath();
   if (!pDoc->dirty() || !sourceIndex().hasTranslationUnit(path))
   {
      // if this is a standalone c++ file outside of a project then
      // we need to update it's compilation database
      projects::ProjectContext& projectContext = projects::projectContext();
      if ((projectContext.config().buildType != r_util::kBuildTypePackage) ||
          !docPath.isWithin(projectContext.buildTargetPath().childPath("src")))
      {
         compilationDatabase().updateForStandaloneCpp(docPath);
      }

      // update the source index for this file
      sourceIndex().updateTranslationUnit(path);
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

// incremental file change handler
boost::scoped_ptr<IncrementalFileChangeHandler> pFileChangeHandler;

} // anonymous namespace
   
bool isAvailable()
{
   return clang().isLoaded();
}

Error initialize()
{
   // we don't even attempt to use these features if R < 3.0.1
   // (we need that version of R in able to turn off the processing
   // of site and user Makevars to force make --dry-run)
   if (!module_context::hasMinimumRVersion("3.0.1"))
      return Success();

   // attempt to load clang interface
   loadLibClang();

   // register diagnostics function
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_isLibClangAvailable" ;
   methodDef.fun = (DL_FUNC)rs_isLibClangAvailable;
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

   // if this is a pakcage with a src directory then initialize various
   // things related to maintaining the package src index
   using namespace projects;
   if ((projectContext().config().buildType == r_util::kBuildTypePackage) &&
       projectContext().buildTargetPath().childPath("src").exists())
   {
      FilePath buildTargetPath = projectContext().buildTargetPath();
      FilePath descPath = buildTargetPath.childPath("DESCRIPTION");
      FilePath srcPath = projectContext().buildTargetPath().childPath("src");

      // update the compilation database for this package
      compilationDatabase().updateForCurrentPackage();

      // filter file change notifications to files of interest
      IncrementalFileChangeHandler::Filter filter =
         boost::bind(packageCppFileFilter,
                     srcPath.absolutePath(),
                     descPath.absolutePath(),
                    _1);

      // create incremental file change handler (this is used for updating
      // the main source index)
      pFileChangeHandler.reset(new IncrementalFileChangeHandler(
                filter,
                fileChangeHandler,
                boost::posix_time::milliseconds(200),
                boost::posix_time::milliseconds(20),
                false)); /* allow indexing during idle time */

      // subscribe to the file monitor
      pFileChangeHandler->subscribeToFileMonitor("C++ Code Completion");
   }

   ExecBlock initBlock ;
   using boost::bind;
   using namespace module_context;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "print_cpp_completions", printCppCompletions));
   return initBlock.execute();

   // return success
   return Success();
}


} // namespace clang
} // namespace modules
} // namesapce session

