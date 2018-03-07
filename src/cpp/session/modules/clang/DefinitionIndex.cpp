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
#include <core/DateTime.hpp>
#include <core/PerformanceTimer.hpp>
#include <core/FileSerializer.hpp>
#include <core/libclang/LibClang.hpp>
#include <core/system/ProcessArgs.hpp>
#include <session/IncrementalFileChangeHandler.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "RSourceIndex.hpp"
#include "RCompilationDatabase.hpp"

using namespace rstudio::core;
using namespace rstudio::core::libclang;

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

namespace {

// flag indicating whether we are initialized
bool s_initialized = false;

struct CppDefinitions
{
   std::string file;
   std::time_t fileLastWrite;
   std::deque<CppDefinition> definitions;
};

// store definitions by file
typedef std::map<std::string,CppDefinitions> DefinitionsByFile;
DefinitionsByFile s_definitionsByFile;

// visitor used to populate deque
bool insertDefinition(const CppDefinition& definition,
                      CppDefinitions* pDefinitions)
{
   pDefinitions->definitions.push_back(definition);
   return true;
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

   // get kind
   CXCursorKind cursorKind = cursor.getKind();

   // ensure it's a definition with linkage or a typedef
   if ((!cursor.isDefinition() || !cursor.hasLinkage()) &&
       cursorKind != CXCursor_TypedefDecl)
   {
      return CXChildVisit_Continue;
   }

   // determine kind
   CppDefinitionKind kind = CppInvalidDefinition;
   switch (cursorKind)
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
      case CXCursor_EnumConstantDecl:
         kind = CppEnumValue;
         break;
      case CXCursor_FunctionDecl:
      case CXCursor_FunctionTemplate:
         kind = CppFunctionDefinition;
         break;
      case CXCursor_CXXMethod:
         kind = CppMemberFunctionDefinition;
         break;
      case CXCursor_TypedefDecl:
         kind = CppTypedefDefinition;
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

   // create the definition
   CppDefinition definition(cursor.getUSR(),
                            kind,
                            parentName,
                            name,
                            cursor.getSourceLocation().getSpellingLocation());

   // yield the definition if it's not a namespace (break if requested)
   if (kind != CppNamespaceDefinition)
   {
      DefinitionVisitor& visitor = *((DefinitionVisitor*)clientData);
      if (!visitor(definition))
         return CXChildVisit_Break;
   }

   // recurse if necessary
   if (kind == CppNamespaceDefinition ||
       kind == CppClassDefinition ||
       kind == CppStructDefinition ||
       kind == CppEnumDefinition)
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
   // alias the filename
   std::string file = event.fileInfo().absolutePath();

   // special case: we write all definitions to disk at shutdown, when
   // we come back up all of the files will come back in as "add" events,
   // for this case we need to ignore the add if we already have a fresh
   // enough index of the file
   if (event.type() == core::system::FileChangeEvent::FileAdded)
   {
      // if we have a definition
      DefinitionsByFile::const_iterator it = s_definitionsByFile.find(file);
      if (it != s_definitionsByFile.end())
      {
         // if the definition is fresh enough then bail
         if (it->second.fileLastWrite >= event.fileInfo().lastWriteTime())
            return;
      }
   }

   // always remove existing definitions
   s_definitionsByFile.erase(file);

   // if this is an add or an update then re-index
   if (event.type() == core::system::FileChangeEvent::FileAdded ||
       event.type() == core::system::FileChangeEvent::FileModified)
   {    
      // get the compilation arguments for this file and use them to
      // create a translation unit
      std::vector<std::string> compileArgs =
         rCompilationDatabase().compileArgsForTranslationUnit(file, true);

      if (!compileArgs.empty())
      {
         // create index
         CXIndex index = libclang::clang().createIndex(
                   1 /* Exclude PCH */,
                   (rSourceIndex().verbose() > 0) ? 1 : 0);

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


         // create definitions and wire visitor to it
         CppDefinitions definitions;
         definitions.file = file;
         definitions.fileLastWrite = event.fileInfo().lastWriteTime();
         s_definitionsByFile[file] = definitions;
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
      case CppEnumValue:
         kindStr = "V";
         break;
      case CppFunctionDefinition:
         kindStr = "F";
         break;
      case CppMemberFunctionDefinition:
         kindStr = "M";
         break;
      case CppTypedefDefinition:
         kindStr = "T";
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

typedef std::map<std::string,TranslationUnit> TranslationUnits;

} // anonymous namespace

FileLocation findDefinitionLocation(const FileLocation& location)
{
   // bail if we aren't initialized
   if (!s_initialized)
      return FileLocation();

   // get the definition cursor for this file location
   Cursor cursor = rSourceIndex().referencedCursorForFileLocation(location);
   if (cursor.isNull())
      return FileLocation();

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
              libclang::clang().getTranslationUnitCursor(
                     unit.second.getCXTranslationUnit()),
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
         BOOST_FOREACH(const CppDefinition& def, defs.second.definitions)
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
   std::string filename = location.filePath.absolutePath();
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


json::Object cppDefinitionToJson(const CppDefinition& definition)
{
   using namespace safe_convert;
   json::Object definitionJson;
   definitionJson["usr"] = definition.USR;
   definitionJson["kind"] = numberTo<int>(definition.kind, 0);
   definitionJson["parent_name"] = definition.parentName;
   definitionJson["name"] = definition.name;
   definitionJson["file"] = definition.location.filePath.absolutePath();
   definitionJson["line"] = numberTo<int>(definition.location.line, 1);
   definitionJson["column"] = numberTo<int>(definition.location.column, 1);
   return definitionJson;
}

CppDefinition cppDefinitionFromJson(const json::Object& object)
{
   // read json
   int kind;
   int line, column;
   std::string file;
   CppDefinition definition;
   Error error = json::readObject(object,
                                  "usr", &definition.USR,
                                  "kind", &kind,
                                  "parent_name", &definition.parentName,
                                  "name", &definition.name,
                                  "file", &file,
                                  "line", &line,
                                  "column", &column);
   if (error)
   {
      LOG_ERROR(error);
      return CppDefinition();
   }

   // required data transforms
   using namespace safe_convert;
   definition.kind = static_cast<CppDefinitionKind>(kind);
   definition.location.filePath = FilePath(file);
   definition.location.line = numberTo<unsigned>(line, 1);
   definition.location.column = numberTo<unsigned>(column, 1);

   // return definition
   return definition;
}

FilePath definitionIndexFilePath()
{
   return module_context::scopedScratchPath().childPath("cpp-definition-cache");
}

void loadDefinitionIndex()
{
   using namespace safe_convert;

   FilePath indexFilePath = definitionIndexFilePath();
   if (!indexFilePath.exists())
      return;

   std::string contents;
   Error error = readStringFromFile(indexFilePath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   json::Value indexValueJson;
   if (!json::parse(contents, &indexValueJson) ||
       !json::isType<json::Array>(indexValueJson))
   {
      LOG_ERROR_MESSAGE("Error parsing definition index: " + contents);
      return;
   }

   json::Array indexJson = indexValueJson.get_array();
   BOOST_FOREACH(const json::Value& definitionsJson, indexJson)
   {
      if (!json::isType<json::Object>(definitionsJson))
      {
         LOG_ERROR_MESSAGE("Unexpected non-object type in definition index");
         continue;
      }

      json::Array defsArrayJson;
      double fileLastWrite;
      CppDefinitions definitions;
      Error error = json::readObject(definitionsJson.get_obj(),
                                     "file", &definitions.file,
                                     "file_last_write", &fileLastWrite,
                                     "definitions", &defsArrayJson);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      definitions.fileLastWrite = numberTo<std::time_t>(fileLastWrite, 0);

      // if the file doesn't exist then bail
      if (!FilePath::exists(definitions.file))
         continue;

      BOOST_FOREACH(const json::Value& defJson, defsArrayJson)
      {
         if (!json::isType<json::Object>(defJson))
         {
            LOG_ERROR_MESSAGE("Unexpected non-object type in definition index");
            continue;
         }

         CppDefinition definition = cppDefinitionFromJson(defJson.get_obj());
         if (!definition.empty())
            definitions.definitions.push_back(definition);
      }

      s_definitionsByFile[definitions.file] = definitions;
   }
}


void saveDefinitionIndex()
{
   using namespace safe_convert;

   json::Array indexJson;
   BOOST_FOREACH(const DefinitionsByFile::value_type& defs, s_definitionsByFile)
   {
      const CppDefinitions& definitions = defs.second;
      json::Object definitionsJson;
      definitionsJson["file"] = definitions.file;
      definitionsJson["file_last_write"] = numberTo<double>(
                                               definitions.fileLastWrite, 0);
      json::Array defsArrayJson;
      std::transform(definitions.definitions.begin(),
                     definitions.definitions.end(),
                     std::back_inserter(defsArrayJson),
                     cppDefinitionToJson);
      definitionsJson["definitions"] = defsArrayJson;

      indexJson.push_back(definitionsJson);
   }

   std::ostringstream ostr;
   json::writeFormatted(indexJson, ostr);
   Error error = writeStringToFile(definitionIndexFilePath(), ostr.str());
   if (error)
      LOG_ERROR(error);
}

void onShutdown(bool terminatedNormally)
{
   if (terminatedNormally)
      saveDefinitionIndex();
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
           libclang::clang().getTranslationUnitCursor(
                                 unit.second.getCXTranslationUnit()),
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

      BOOST_FOREACH(const CppDefinition& def, defs.second.definitions)
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
      // read in any index saved on disk
      loadDefinitionIndex();

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
                  boost::posix_time::seconds(3),
                  boost::posix_time::milliseconds(500),
                  true);
         pFileChangeHandler->subscribeToFileMonitor("Go to C/C++ Definition");
      }

      // set initialized flag
      s_initialized = true;

      // setup handler to save index at shutdown
      module_context::events().onShutdown.connect(onShutdown);
   }

   return Success();
}

} // namespace clang
} // namespace modules
} // namespace session
} // namespace rstudio

