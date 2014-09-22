/*
 * CodeCompletion.cpp
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

#include "CodeCompletion.hpp"

#include <boost/make_shared.hpp>

#include <core/Error.hpp>

#include "Clang.hpp"
#include "SourceIndex.hpp"
#include "UnsavedFiles.hpp"

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
      clang().disposeCodeCompleteResults(pResults_);
   }
   catch(...)
   {
   }
}

void CodeCompleteResults::sort()
{
   clang().sortCodeCompletionResults(pResults_->Results, pResults_->NumResults);
}

unsigned long long CodeCompleteResults::getContexts()
{
   return clang().codeCompleteGetContexts(pResults_);
}

boost::shared_ptr<CodeCompleteResults> codeCompleteAt(
                                                  const std::string& filename,
                                                  unsigned line,
                                                  unsigned column)
{
   // get the translation unit for this file
   CXTranslationUnit tu = sourceIndex().getTranslationUnit(filename);
   CXCodeCompleteResults* pResults = clang().codeCompleteAt(
                                 tu,
                                 filename.c_str(),
                                 line,
                                 column,
                                 unsavedFiles().unsavedFilesArray(),
                                 unsavedFiles().numUnsavedFiles(),
                                 clang().defaultCodeCompleteOptions());

   // return a shared pointer that wraps the results
   return boost::make_shared<CodeCompleteResults>(pResults);
}

} // namespace clang
} // namespace modules
} // namesapce session

