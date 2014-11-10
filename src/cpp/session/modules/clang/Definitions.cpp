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

// lookup definition by USR
typedef std::map<std::string,Definition> Definitions;

// definitions by file
std::map<std::string,Definitions> s_definitionsByFile;


bool isTranslationUnit(const FileInfo& fileInfo,
                       const FilePath& pkgSrcDir,
                       const FilePath& pkgIncludeDir)
{
   FilePath filePath(fileInfo.absolutePath());

   if (pkgSrcDir.exists() &&
       filePath.isWithin(pkgSrcDir) &&
       SourceIndex::isSourceFile(filePath) &&
       !boost::algorithm::starts_with(filePath.stem(), kCompilationDbPrefix) &&
       (filePath.filename() != "RcppExports.cpp"))
   {
      return true;
   }
   else if (pkgIncludeDir.exists() &&
            filePath.isWithin(pkgIncludeDir) &&
            SourceIndex::isSourceFile(filePath) &&
            !boost::algorithm::ends_with(filePath.stem(), "_RcppExports"))
   {
      return true;
   }
   else
   {
      return false;
   }
}

CXChildVisitResult cursorVisitor(CXCursor cxCursor,
                                 CXCursor,
                                 CXClientData clientData)
{
   // get the cursor and check if it's in the right file
   Cursor cursor(cxCursor);
   SourceLocation location = cursor.getSourceLocation();
   if (!location.isFromMainFile())
      return CXChildVisit_Continue;

   // ensure it's a definition with external linkage
   if (!cursor.isDefinition() || !cursor.hasExternalLinkage())
      return CXChildVisit_Continue;

   // determine kind
   DefinitionKind kind = InvalidDefinition;
   switch (cursor.getKind())
   {
      case CXCursor_Namespace:
         kind = NamespaceDefinition;
         break;
      case CXCursor_ClassDecl:
      case CXCursor_ClassTemplate:
         kind = ClassDefinition;
         break;
      case CXCursor_StructDecl:
         kind = StructDefinition;
         break;
      case CXCursor_EnumDecl:
         kind = EnumDefinition;
         break;
      case CXCursor_FunctionDecl:
      case CXCursor_FunctionTemplate:
         kind = FunctionDefinition;
         break;
      case CXCursor_CXXMethod:
         kind = MemberFunctionDefinition;
         break;
      default:
         kind = InvalidDefinition;
         break;
   }

   // continue if this isn't a definition of interest
   if (kind == InvalidDefinition)
      return CXChildVisit_Continue;

   // build display name (strip trailing parens)
   std::string displayName = cursor.displayName();
   boost::algorithm::replace_last(displayName, "()", "");

   // empty display name for a namespace === anonymous
   if ((kind == NamespaceDefinition) && displayName.empty())
   {
      displayName = "<anonymous>";
   }

   // qualify class members
   if (kind == MemberFunctionDefinition)
   {
      Cursor parent = cursor.getSemanticParent();
      displayName = parent.displayName() + "::" + displayName;
   }

   // get the source location
   SourceLocation loc = cursor.getSourceLocation();
   std::string file;
   unsigned line, column;
   loc.getSpellingLocation(&file, &line, &column);

   // create the definition
   Definition definition(cursor.getUSR(),
                         kind,
                         displayName,
                         FilePath(file),
                         line,
                         column);

   // print the definition
   std::cout << "   " << definition << std::endl;

   // recurse if necessary
   if (kind == NamespaceDefinition ||
       kind == ClassDefinition ||
       kind == StructDefinition)
   {
      return CXChildVisit_Recurse;
   }
   else
   {
      return CXChildVisit_Continue;
   }
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
         std::cout << file << std::endl;

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
                               CXTranslationUnit_Incomplete);

         // visit all of the cursors
         libclang::clang().visitChildren(
              libclang::clang().getTranslationUnitCursor(tu),
              cursorVisitor,
              NULL);

         // dispose translation unit and index
         libclang::clang().disposeTranslationUnit(tu);
         libclang::clang().disposeIndex(index);
      }
   }
}

} // anonymous namespace


std::ostream& operator<<(std::ostream& os, const Definition& definition)
{
   // kind
   std::string kindStr;
   switch (definition.kind)
   {
      case NamespaceDefinition:
         kindStr = "N";
         break;
      case ClassDefinition:
         kindStr = "C";
         break;
      case StructDefinition:
         kindStr = "S";
         break;
      case EnumDefinition:
         kindStr = "E";
         break;
      case FunctionDefinition:
         kindStr = "F";
         break;
      case MemberFunctionDefinition:
         kindStr = "M";
         break;
      default:
         kindStr = " ";
         break;
   }
   os << "[" << kindStr << "] ";

   // display name
   os << definition.displayName << " ";

   // file location
   os << "(" << definition.filePath.filename() << ":"
      << definition.line << ":" << definition.column << ") ";

   // USR
   os << definition.USR;

   return os;
}

Error initialize()
{
   using namespace projects;
   //if (projectContext().config().buildType == r_util::kBuildTypePackage)
   if (false)
   {
      // check for src and inst/include dirs
      FilePath pkgPath = projects::projectContext().buildTargetPath();
      FilePath srcPath = pkgPath.childPath("src");
      FilePath includePath = pkgPath.childPath("inst/include");
      if (srcPath.exists() || includePath.exists())
      {
         // create an incremental file change handler (on the heap so that it
         // survives the call to this function and is never deleted)
         IncrementalFileChangeHandler* pFileChangeHandler =
           new IncrementalFileChangeHandler(
                  boost::bind(isTranslationUnit, _1, srcPath, includePath),
                  fileChangeHandler,
                  boost::posix_time::milliseconds(1000),
                  boost::posix_time::milliseconds(500),
                  true);
         pFileChangeHandler->subscribeToFileMonitor("Go to C/C++ Definition");
      }
   }

   return Success();
}

} // namespace definitions
} // namespace clang
} // namespace modules
} // namesapce session

