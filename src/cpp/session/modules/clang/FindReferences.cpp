/*
 * FindReferences.cpp
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

#include "FindReferences.hpp"

#include <boost/foreach.hpp>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/split.hpp>

#include <core/FileSerializer.hpp>
#include <core/libclang/LibClang.hpp>

#include <core/system/ProcessArgs.hpp>

#include <session/SessionModuleContext.hpp>

#include "RSourceIndex.hpp"
#include "RCompilationDatabase.hpp"

using namespace rstudio::core;
using namespace rstudio::core::libclang;

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

namespace {

struct FindReferencesData
{
   FindReferencesData(CXTranslationUnit tu, const std::string& USR)
      : tu(tu), USR(USR)
   {
   }
   CXTranslationUnit tu;
   std::string USR;
   std::string spelling;
   std::vector<FileRange> references;
};

// if the USR's differ only by an ending # consider them equal
// we do this because references to function declarations seem to
// accue an extra # within their USRs
bool equalUSR(const std::string& USR1, const std::string& USR2)
{
   using namespace boost::algorithm;
   bool endHashUSR1 = ends_with(USR1, "#");
   bool endHashUSR2 = ends_with(USR2, "#");
   if ((endHashUSR1 && endHashUSR2) || (!endHashUSR1 && !endHashUSR2))
   {
      return USR1 == USR2;
   }
   else if (endHashUSR1)
   {
      return USR1.substr(0, USR1.length() - 1) == USR2;
   }
   else
   {
      return USR2.substr(0, USR2.length() - 1) == USR1;
   }
}

CXChildVisitResult findReferencesVisitor(CXCursor cxCursor,
                                         CXCursor,
                                         CXClientData data)
{
   using namespace rstudio::core::libclang;

   // get pointer to data struct
   FindReferencesData* pData = (FindReferencesData*)data;

   // reference to the cursor (ensure valid)
   Cursor cursor(cxCursor);
   if (!cursor.isValid())
      return CXChildVisit_Continue;

   // continue with sibling if it's not from the main file
   SourceLocation location = cursor.getSourceLocation();
   if (!location.isFromMainFile())
      return CXChildVisit_Continue;

   // get referenced cursor
   Cursor referencedCursor = cursor.getReferenced();
   if (referencedCursor.isValid() && referencedCursor.isDeclaration())
   {
      // check for matching USR
      if (equalUSR(referencedCursor.getUSR(), pData->USR))
      {
         // tokenize to extract identifer location for cursors that
         // represent larger source constructs
         FileRange foundRange;
         libclang::Tokens tokens(pData->tu, cursor.getExtent());
         std::vector<unsigned> indexes;

         // for constructors & destructors we search backwards so that the
         // match is for the constructor identifier rather than the class
         // identifer
         unsigned numTokens = tokens.numTokens();
         if (referencedCursor.getKind() == CXCursor_Constructor ||
             referencedCursor.getKind() == CXCursor_Destructor)
         {
            for (unsigned i = 0; i < numTokens; i++)
               indexes.push_back(numTokens - i - 1);
         }
         else
         {
            for (unsigned i = 0; i < numTokens; i++)
               indexes.push_back(i);
         }

         // cycle through the tokens
         BOOST_FOREACH(unsigned i, indexes)
         {
            Token token = tokens.getToken(i);
            if (token.kind() == CXToken_Identifier &&
                token.spelling() == cursor.spelling())
            {
               // record spelling if necessary
               if (pData->spelling.empty())
                  pData->spelling = cursor.spelling();

               // record the range
               foundRange = token.extent().getFileRange();

               break;
            }
         }

         // if we didn't find an identifier that matches use the
         // original match (i.e. important for constructors where
         // the 'spelling' of the invocation is the name of the
         // variable declared)
         if (foundRange.empty())
            foundRange = cursor.getExtent().getFileRange();

         // record the range if it's not a duplicate of the previous range
         if (pData->references.empty() ||
             (pData->references.back() != foundRange))
         {
            pData->references.push_back(foundRange);
         }
      }
   }

   // recurse into namespaces, classes, etc.
   return CXChildVisit_Recurse;
}

class SourceMarkerGenerator
{
public:
   std::vector<module_context::SourceMarker> markersForCursorLocations(
              const std::vector<core::libclang::FileRange>& locations)
   {
      using namespace module_context;
      std::vector<SourceMarker> markers;

      BOOST_FOREACH(const libclang::FileRange& loc, locations)
      {
         FileLocation startLoc = loc.start;

         // get file contents and use it to create the message
         std::size_t line = startLoc.line - 1;
         std::string message;
         const std::vector<std::string>& lines = fileContents(
                                 startLoc.filePath.absolutePath());
         if (line < lines.size())
            message = htmlMessage(loc, lines[line]);


         // create marker
         SourceMarker marker(SourceMarker::Usage,
                             startLoc.filePath,
                             startLoc.line,
                             startLoc.column,
                             core::html_utils::HTML(message, true),
                             true);

         // add it to the list
         markers.push_back(marker);
      }

      return markers;
   }

private:

   static std::string htmlMessage(const libclang::FileRange& loc,
                                  const std::string& message)
   {
      FileLocation startLoc = loc.start;
      FileLocation endLoc = loc.end;

      unsigned extent = 0;
      if (startLoc.line == endLoc.line)
         extent = endLoc.column - startLoc.column;

      // attempt to highlight the location
      using namespace string_utils;
      unsigned col = startLoc.column - 1;
      if ((col + extent) < message.length())
      {
         if (extent == 0)
         {
            return "<strong>" + htmlEscape(message) + "</strong>";
         }
         else
         {
            std::ostringstream ostr;
            ostr << htmlEscape(message.substr(0, col));
            ostr << "<strong>";
            ostr << htmlEscape(message.substr(col, extent));
            ostr << "</strong>";
            ostr << htmlEscape(message.substr(col + extent));
            return ostr.str();
         }
      }
      else
      {
         return string_utils::htmlEscape(message);
      }
   }

   typedef std::map<std::string,std::vector<std::string> > SourceFileContentsMap;

   const std::vector<std::string>& fileContents(const std::string& filename)
   {
      // check cache
      SourceFileContentsMap::const_iterator it =
                                       sourceFileContents_.find(filename);
      if (it == sourceFileContents_.end())
      {
         // check unsaved files
         UnsavedFiles& unsavedFiles = rSourceIndex().unsavedFiles();
         unsigned numFiles = unsavedFiles.numUnsavedFiles();
         for (unsigned i = 0; i<numFiles; ++i)
         {
            CXUnsavedFile unsavedFile = unsavedFiles.unsavedFilesArray()[i];
            if (unsavedFile.Filename != NULL &&
                std::string(unsavedFile.Filename) == filename &&
                unsavedFile.Contents != NULL)
            {
               std::string contents(unsavedFile.Contents, unsavedFile.Length);
               std::vector<std::string> lines;
               boost::algorithm::split(lines,
                                       contents,
                                       boost::is_any_of("\n"));


               sourceFileContents_.insert(std::make_pair(filename, lines));
               it = sourceFileContents_.find(filename);
               break;
            }
         }

         // if we didn't get one then read it from disk
         if (it == sourceFileContents_.end())
         {
            std::vector<std::string> lines;
            Error error = readStringVectorFromFile(FilePath(filename),
                                                   &lines,
                                                   false);
            if (error)
               LOG_ERROR(error);

            // insert anyway to ensure it->second below works
            sourceFileContents_.insert(std::make_pair(filename, lines));
            it = sourceFileContents_.find(filename);
         }
      }

      // return reference to contents
      return it->second;
   }

private:
   SourceFileContentsMap sourceFileContents_;
};

void findReferences(std::string USR,
                    CXTranslationUnit tu,
                    std::string* pSpelling,
                    std::vector<core::libclang::FileRange>* pRefs)
{
   FindReferencesData findReferencesData(tu, USR);
   libclang::clang().visitChildren(
               libclang::clang().getTranslationUnitCursor(tu),
               findReferencesVisitor,
               (CXClientData)&findReferencesData);

   // copy the locations to the out parameter
   if (!findReferencesData.spelling.empty())
      *pSpelling = findReferencesData.spelling;
   std::copy(findReferencesData.references.begin(),
             findReferencesData.references.end(),
             std::back_inserter(*pRefs));
}

} // anonymous namespace



core::Error findReferences(const core::libclang::FileLocation& location,
                           std::string* pSpelling,
                           std::vector<core::libclang::FileRange>* pRefs)
{
   Cursor cursor = rSourceIndex().referencedCursorForFileLocation(location);
   if (!cursor.isValid() || !cursor.isDeclaration())
      return Success();

   // get it's USR (bail if it doesn't have one)
   std::string USR = cursor.getUSR();
   if (USR.empty())
      return Success();

   // determine what translation units to look in -- if this is a package
   // then we look throughout all the source code in the package.
   if (rCompilationDatabase().hasTranslationUnit(
                                         location.filePath.absolutePath()))
   {
      // get all translation units to search
      std::vector<std::string> files = rCompilationDatabase()
                                                .translationUnits();

      // get translation units we've already indexed
      std::map<std::string,TranslationUnit> indexedUnits =
                           rSourceIndex().getIndexedTranslationUnits();

      BOOST_FOREACH(const std::string& filename, files)
      {
         // first look in already indexed translation units
         // (this will pickup unsaved files)
         std::map<std::string,TranslationUnit>::iterator it =
                                                   indexedUnits.find(filename);
         if (it != indexedUnits.end())
         {
            findReferences(USR,
                           it->second.getCXTranslationUnit(),
                           pSpelling,
                           pRefs);
         }
         else
         {
            // get the compilation arguments for this file and use them to
            // create a temporary translation unit to search
            std::vector<std::string> compileArgs =
               rCompilationDatabase().compileArgsForTranslationUnit(filename,
                                                                    true);

            if (compileArgs.empty())
               continue;

            // create temporary index
            CXIndex index = libclang::clang().createIndex(
                              1 /* Exclude PCH */,
                              (rSourceIndex().verbose() > 0) ? 1 : 0);

            // get args in form clang expects
            core::system::ProcessArgs argsArray(compileArgs);

            // parse the translation unit
            CXTranslationUnit tu = libclang::clang().parseTranslationUnit(
                                  index,
                                  filename.c_str(),
                                  argsArray.args(),
                                  argsArray.argCount(),
                                  NULL, 0, // no unsaved files
                                  CXTranslationUnit_None |
                                  CXTranslationUnit_Incomplete);

            // find references
            findReferences(USR, tu, pSpelling, pRefs);

            // dispose translation unit and index
            libclang::clang().disposeTranslationUnit(tu);
            libclang::clang().disposeIndex(index);
         }
      }
   }
   // not a package, just search locally
   else
   {
      TranslationUnit tu = rSourceIndex().getTranslationUnit(
                                             location.filePath.absolutePath(),
                                             true);
      if (!tu.empty())
         findReferences(USR, tu.getCXTranslationUnit(), pSpelling, pRefs);
   }

   return Success();

}

Error findUsages(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // get params
   std::string docPath;
   int line, column;
   Error error = json::readParams(request.params,
                                  &docPath,
                                  &line,
                                  &column);
   if (error)
      return error;

   // resolve the docPath if it's aliased
   FilePath filePath = module_context::resolveAliasedPath(docPath);

   // get the declaration cursor for this file location
   core::libclang::FileLocation location(filePath, line, column);

   // find the references
   std::string spelling;
   std::vector<core::libclang::FileRange> usageLocations;
   error = findReferences(location, &spelling, &usageLocations);
   if (error)
      return error;

   // produce source markers from cursor locations
   using namespace module_context;
   std::vector<SourceMarker> markers = SourceMarkerGenerator()
                                 .markersForCursorLocations(usageLocations);

   if (markers.size() > 0)
   {
      SourceMarkerSet markerSet("C++ Find Usages: " + spelling, markers);
      showSourceMarkers(markerSet, MarkerAutoSelectNone);
   }

   return Success();
}


} // namespace clang
} // namespace modules
} // namespace session
} // namespace rstudio

