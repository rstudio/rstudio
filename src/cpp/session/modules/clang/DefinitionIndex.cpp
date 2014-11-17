/*
 * DefinitionIndex.cpp
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

#include "DefinitionIndex.hpp"

#include <deque>

#include <core/FilePath.hpp>
#include <core/PerformanceTimer.hpp>
#include <core/libclang/LibClang.hpp>
#include <core/system/ProcessArgs.hpp>
#include <session/IncrementalFileChangeHandler.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "RSourceIndex.hpp"
#include "RCompilationDatabase.hpp"

using namespace core;
using namespace core::libclang;

namespace session {
namespace modules { 
namespace clang {

namespace {

// flag indicating whether we are initialized
bool s_initialized = false;

// store definitions by file
typedef std::map<std::string,std::deque<CppDefinition> > DefinitionsByFile;
DefinitionsByFile s_definitionsByFile;

// visitor used to populate deque
bool insertDefinition(const CppDefinition& definition,
                      std::deque<CppDefinition>* pDefinitions)
{
   pDefinitions->push_back(definition);
   return true;
}

bool isIndexableFile(const FileInfo& fileInfo,
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

typedef boost::function<bool(const CppDefinition&)> DefinitionVisitor;

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
   CppDefinitionKind kind = CppInvalidDefinition;
   switch (cursor.getKind())
   {
      case CXCursor_Namespace:
         kind = CppNamespaceDefinition;
         break;
      case CXCursor_ClassDecl:
      case CXCursor_ClassTemplate:
         kind = CppClassDefinition;
         break;
      case CXCursor_StructDecl:
         kind = CppStructDefinition;
         break;
      case CXCursor_EnumDecl:
         kind = CppEnumDefinition;
         break;
      case CXCursor_FunctionDecl:
      case CXCursor_FunctionTemplate:
         kind = CppFunctionDefinition;
         break;
      case CXCursor_CXXMethod:
         kind = CppMemberFunctionDefinition;
         break;
      default:
         kind = CppInvalidDefinition;
         break;
   }

   // continue if this isn't a definition of interest
   if (kind == CppInvalidDefinition)
      return CXChildVisit_Continue;

   // build name (strip trailing parens)
   std::string name = cursor.displayName();
   boost::algorithm::replace_last(name, "()", "");

   // empty display name for a namespace === anonymous
   if ((kind == CppNamespaceDefinition) && name.empty())
   {
      name = "<anonymous>";
   }

   // qualify class members
   std::string parentName;
   if (kind == CppMemberFunctionDefinition)
   {
      Cursor parent = cursor.getSemanticParent();
      parentName = parent.displayName();
   }

   // get the source location
   SourceLocation loc = cursor.getSourceLocation();
   std::string file;
   unsigned line, column;
   loc.getSpellingLocation(&file, &line, &column);

   // create the definition
   CppDefinition definition(cursor.getUSR(),
                            kind,
                            parentName,
                            name,
                            FileLocation(FilePath(file), line, column));

   // yield the definition (break if requested)
   DefinitionVisitor& visitor = *((DefinitionVisitor*)clientData);
   if (!visitor(definition))
      return CXChildVisit_Break;

   // recurse if necessary
   if (kind == CppNamespaceDefinition ||
       kind == CppClassDefinition ||
       kind == CppStructDefinition)
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


         // create deque of definitions and wire visitor to it
         s_definitionsByFile[file] = std::deque<CppDefinition>();
         DefinitionVisitor visitor =
            boost::bind(insertDefinition, _1, &s_definitionsByFile[file]);

         // visit the cursors
         libclang::clang().visitChildren(
              libclang::clang().getTranslationUnitCursor(tu),
              cursorVisitor,
              (CXClientData)&visitor);

         // dispose translation unit and index
         libclang::clang().disposeTranslationUnit(tu);
         libclang::clang().disposeIndex(index);
      }
   }
}

} // anonymous namespace


std::ostream& operator<<(std::ostream& os, const CppDefinition& definition)
{
   // kind
   std::string kindStr;
   switch (definition.kind)
   {
      case CppNamespaceDefinition:
         kindStr = "N";
         break;
      case CppClassDefinition:
         kindStr = "C";
         break;
      case CppStructDefinition:
         kindStr = "S";
         break;
      case CppEnumDefinition:
         kindStr = "E";
         break;
      case CppFunctionDefinition:
         kindStr = "F";
         break;
      case CppMemberFunctionDefinition:
         kindStr = "M";
         break;
      default:
         kindStr = " ";
         break;
   }
   os << "[" << kindStr << "] ";

   // display name
   if (!definition.parentName.empty())
      os << definition.parentName << "::";
   os << definition.name << " ";

   // file location
   os << "(" << definition.location.filePath.filename() << ":"
      << definition.location.line << ":" << definition.location.column << ") ";

   // USR
   os << definition.USR;

   return os;
}

namespace {

bool findUSR(const std::string& USR,
             const CppDefinition& definition,
             CppDefinition* pFoundDefinition)
{
   if (definition.USR == USR)
   {
      *pFoundDefinition = definition;
      return false;
   }
   else
   {
      return true;
   }
}

typedef std::map<std::string,CXTranslationUnit> TranslationUnits;

} // anonymous namespace

FileLocation findDefinitionLocation(const FileLocation& location)
{
   // bail if we aren't initialized
   if (!s_initialized)
      return FileLocation();

   // get the translation unit
   std::string filename = location.filePath.absolutePath();
   TranslationUnit tu = rSourceIndex().getTranslationUnit(filename, true);
   if (tu.empty())
      return FileLocation();

   // get the cursor
   Cursor cursor = tu.getCursor(filename, location.line, location.column);
   if (!cursor.isValid())
      return FileLocation();

   // follow reference if we need to
   if (cursor.isReference() || cursor.isExpression())
   {
      cursor = cursor.getReferenced();
      if (!cursor.isValid())
         return FileLocation();
   }

   // get the USR for the cursor and search for it
   std::string USR = cursor.getUSR();
   if (!USR.empty())
   {
      // first inspect translation units we have an in-memory index for
      TranslationUnits units = rSourceIndex().getIndexedTranslationUnits();
      BOOST_FOREACH(const TranslationUnits::value_type& unit, units)
      {
         // search for the definition
         CppDefinition def;
         DefinitionVisitor visitor = boost::bind(findUSR, USR, _1, &def);

         // visit the cursors
         libclang::clang().visitChildren(
              libclang::clang().getTranslationUnitCursor(unit.second),
              cursorVisitor,
              (CXClientData)&visitor);

         // return the definition if we found it
         if (!def.empty())
            return def.location;
      }

      // if we didn't find it there then look for it in our index
      // of all saved files
      BOOST_FOREACH(const DefinitionsByFile::value_type& defs,
                    s_definitionsByFile)
      {
         BOOST_FOREACH(const CppDefinition& def, defs.second)
         {
            if (def.USR == USR)
               return def.location;
         }
      }
   }

   // see if we can resolve the cursor to a definition (if we can't
   // that's okay)
   if (!cursor.isDefinition())
   {
      Cursor cursorDef = cursor.getDefinition();
      if (cursorDef.isValid())
         cursor = cursorDef;
   }

   // return the location
   SourceLocation loc = cursor.getSourceLocation();
   unsigned line, column;
   loc.getSpellingLocation(&filename, &line, &column);
   return FileLocation(FilePath(filename), line, column);
}

namespace {

bool matches(const std::string& term,
             const boost::regex& pattern,
             const CppDefinition& definition)
{
   if (!pattern.empty())
      return regex_utils::textMatches(definition.name, pattern, false, false);
   else
      return string_utils::isSubsequence(definition.name, term, true);
}

bool insertMatching(const std::string& term,
                    const boost::regex& pattern,
                    const CppDefinition& definition,
                    std::vector<CppDefinition>* pDefinitions)
{
   if (matches(term, pattern, definition))
      pDefinitions->push_back(definition);
   return true;
}

} // anonymous namespace

void searchDefinitions(const std::string& term,
                       std::vector<CppDefinition>* pDefinitions)
{
   // bail if we aren't initialized
   if (!s_initialized)
      return;

   // get a pattern for the term (if it includes a wildcard '*')
   boost::regex pattern = regex_utils::regexIfWildcardPattern(term);

   // first search translation units we have an in-memory index for
   // (this will reflect unsaved changes in editor buffers)
   TranslationUnits units = rSourceIndex().getIndexedTranslationUnits();
   BOOST_FOREACH(const TranslationUnits::value_type& unit, units)
   {
      // search for matching definitions
      DefinitionVisitor visitor =
         boost::bind(insertMatching, term, pattern, _1, pDefinitions);

      // visit the cursors
      libclang::clang().visitChildren(
           libclang::clang().getTranslationUnitCursor(unit.second),
           cursorVisitor,
           (CXClientData)&visitor);
   }

   // now search the project index (excluding files we already searched
   // for within the in-memory index)
   // if we didn't find it there then look for it in our index
   // of all saved files
   BOOST_FOREACH(const DefinitionsByFile::value_type& defs, s_definitionsByFile)
   {
      // skip files we've already searched
      if (units.find(defs.first) != units.end())
         continue;

      BOOST_FOREACH(const CppDefinition& def, defs.second)
      {
         if (matches(term, pattern, def))
            pDefinitions->push_back(def);
      }
   }
}

Error initializeDefinitionIndex()
{
   using namespace projects;
   if (projectContext().config().buildType == r_util::kBuildTypePackage)
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
                  boost::bind(isIndexableFile, _1, srcPath, includePath),
                  fileChangeHandler,
                  boost::posix_time::milliseconds(200),
                  boost::posix_time::milliseconds(20),
                  true);
         pFileChangeHandler->subscribeToFileMonitor("Go to C/C++ Definition");
      }

      // set initialized flag
      s_initialized = true;
   }

   return Success();
}

} // namespace clang
} // namespace modules
} // namesapce session

