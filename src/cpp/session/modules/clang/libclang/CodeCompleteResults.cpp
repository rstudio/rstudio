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


#include "SharedLibrary.hpp"
#include "SourceIndex.hpp"
#include "UnsavedFiles.hpp"
#include "Utils.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace clang {
namespace libclang {

namespace {


} // anonymous namespace

// NOTE: this is a toy version of inspecting completion chunks just
// to see if we can get completion working -- we need to decompose
// and analyze at a much higher fidelity to do "real" completion

std::string CodeCompleteResult::getText() const
{
   std::string text;

   unsigned chunks = clang().getNumCompletionChunks(result_.CompletionString);
   for (unsigned i = 0; i < chunks; i++)
   {
      CXCompletionString cs = result_.CompletionString;
      if (clang().getCompletionChunkKind(cs, i) != CXCompletionChunk_TypedText)
         continue;

      std::string chunk = toStdString(clang().getCompletionChunkText(cs, i));

      text += chunk;
   }

   return text;
}

CodeCompleteResults:: ~CodeCompleteResults()
{
   try
   {
      if (!empty())
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

CodeCompleteResult CodeCompleteResults::getResult(unsigned index) const
{
   return CodeCompleteResult(results()->Results[index]);
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

unsigned long long CodeCompleteResults::getContexts() const
{
   return clang().codeCompleteGetContexts(results());
}


} // namespace libclang
} // namespace clang
} // namespace modules
} // namesapce session

