/*
 * Definitions.cpp
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

#include "Definitions.hpp"

#include <core/FilePath.hpp>
#include <core/libclang/LibClang.hpp>
#include <core/system/ProcessArgs.hpp>
#include <session/IncrementalFileChangeHandler.hpp>

#include <session/projects/SessionProjects.hpp>

#include "RCompilationDatabase.hpp"

using namespace core;
using namespace core::libclang;

namespace session {
namespace modules { 
namespace clang {
namespace definitions {

namespace {

// C++ symbol definition
struct Definition
{
   Definition()
      : kind(CXCursor_InvalidFile),
        line(0),
        column(0)
   {
   }

   Definition(CXCursorKind kind,
              const std::string& name,
              const FilePath& filePath,
              unsigned line,
              unsigned column)
      : kind(kind), name(name), filePath(filePath), line(line), column(column)
   {
   }

   bool empty() const { return name.empty(); }

   const CXCursorKind kind;
   const std::string name;
   const FilePath filePath;
   const unsigned line;
   const unsigned column;
};

// lookup definition by USR
typedef std::map<std::string,Definition> Definitions;

// definitions by file
std::map<std::string,Definitions> s_definitionsByFile;


bool isTranslationUnit(const FileInfo& fileInfo)
{
   return SourceIndex::isTranslationUnit(fileInfo.absolutePath());
}

CXChildVisitResult cursorVisitor(CXCursor cxCursor,
                                 CXCursor,
                                 CXClientData clientData)
{
   // file we are examining
   std::string file = *(std::string*)clientData;

   // get the cursor and check if it's in the right file
   Cursor cursor(cxCursor);
   SourceLocation location = cursor.getSourceLocation();
   std::string filename;
   unsigned line, column;
   location.getSpellingLocation(&filename, &line, &column);

   // if it isn't in the right file then bail
   if (file != filename)
      return CXChildVisit_Continue;

   // if it's a definition then note it
   if (cursor.isDefinition())
   {
      std::cerr << cursor.getUSR() << std::endl;
   }

   // keep recursing through cursors
   return CXChildVisit_Recurse;
}


void fileChangeHandler(const core::system::FileChangeEvent& event)
{
   // always remove existing definitions
   std::string file = event.fileInfo().absolutePath();
   s_definitionsByFile.erase(file);

   // if this is an add or an update then re-index
   if (event.type() == core::system::FileChangeEvent::FileAdded ||
       event.type() == core::system::FileChangeEvent::FileModified)
   {
      // get the compilation arguments for this file and use them to
      // create a translation unit
      std::vector<std::string> compileArgs =
         rCompilationDatabase().compileArgsForTranslationUnit(file);

      if (!compileArgs.empty())
      {
         // insert an entry for this file


         // create index
         CXIndex index = libclang::clang().createIndex(
                                             1 /* Exclude PCH */,
                                             0 /* No diagnostics */);

         // get args in form clang expects
         core::system::ProcessArgs argsArray(compileArgs);

         // parse the translation unit
         CXTranslationUnit tu = libclang::clang().parseTranslationUnit(
                               index,
                               file.c_str(),
                               argsArray.args(),
                               argsArray.argCount(),
                               NULL, 0, // no unsaved files
                               CXTranslationUnit_None |
                               CXTranslationUnit_SkipFunctionBodies);

         // visit all of the cursors
         libclang::clang().visitChildren(
              libclang::clang().getTranslationUnitCursor(tu),
              cursorVisitor,
              (CXClientData)&file);

         // dispose translation unit and index
         libclang::clang().disposeTranslationUnit(tu);
         libclang::clang().disposeIndex(index);
      }
   }
}

} // anonymous namespace


Error initialize()
{
   using namespace projects;
   if (projectContext().config().buildType == r_util::kBuildTypePackage)
   {
      /*
      // create an incremental file change handler (on the heap so that it
      // survives the call to this function and is never deleted)
      IncrementalFileChangeHandler* pFileChangeHandler =
        new IncrementalFileChangeHandler(isTranslationUnit,
                                         fileChangeHandler,
                                         boost::posix_time::milliseconds(1000),
                                         boost::posix_time::milliseconds(500),
                                         true);
      pFileChangeHandler->subscribeToFileMonitor("Go to C/C++ Definition");
      */
   }

   return Success();
}

} // namespace definitions
} // namespace clang
} // namespace modules
} // namesapce session

