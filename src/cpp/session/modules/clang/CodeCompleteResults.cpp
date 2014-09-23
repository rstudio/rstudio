/*
 * CodeCompleteResults.cpp
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

#include "CodeCompleteResults.hpp"

#include "SourceIndex.hpp"
#include "UnsavedFiles.hpp"
#include "Utils.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace clang {

namespace {


} // anonymous namespace

CodeCompleteResults:: ~CodeCompleteResults()
{
   try
   {
      clang().disposeCodeCompleteResults(results());
   }
   catch(...)
   {
   }
}

void CodeCompleteResults::sort()
{
   clang().sortCodeCompletionResults(results()->Results,
                                     results()->NumResults);
}

unsigned CodeCompleteResults::getNumChunks() const
{
   return clang().getNumCompletionChunks(results()->Results->CompletionString);
}

enum CXCompletionChunkKind CodeCompleteResults::getChunkKind(unsigned idx) const
{
   return clang().getCompletionChunkKind(results()->Results->CompletionString,
                                         idx);
}

std::string CodeCompleteResults::getChunkText(unsigned idx) const
{
   CXString cxText = clang().getCompletionChunkText(
                                       results()->Results->CompletionString,
                                       idx);
   return toStdString(cxText);
}

unsigned CodeCompleteResults::getNumDiagnostics() const
{
   return clang().codeCompleteGetNumDiagnostics(results());
}

Diagnostic CodeCompleteResults::getDiagnostic(unsigned index) const
{
   CXDiagnostic cxDiag = clang().codeCompleteGetDiagnostic(results(), index);
   return Diagnostic(cxDiag);
}

std::string CodeCompleteResults::getBriefComment() const
{
   return toStdString(clang().getCompletionBriefComment(
                                       results()->Results->CompletionString));
}

unsigned long long CodeCompleteResults::getContexts() const
{
   return clang().codeCompleteGetContexts(results());
}



} // namespace clang
} // namespace modules
} // namesapce session

