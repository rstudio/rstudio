/*
 * CodeCompleteResults.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <core/libclang/CodeCompleteResults.hpp>

#include <core/libclang/LibClang.hpp>
#include <core/libclang/SourceIndex.hpp>
#include <core/libclang/UnsavedFiles.hpp>
#include <core/libclang/Utils.hpp>

namespace rstudio {
namespace core {
namespace libclang {

namespace  {

std::string completionText(CXCompletionString cs, unsigned i)
{
   std::string text;

   CXCompletionChunkKind kind = clang().getCompletionChunkKind(cs, i);
   switch(kind)
   {
      case CXCompletionChunk_Optional:
         {
         CXCompletionString optCS = clang().getCompletionChunkCompletionString(
                                                                         cs, i);
         unsigned chunks = clang().getNumCompletionChunks(optCS);
         for (unsigned c = 0; c < chunks; c++)
            text += completionText(optCS, c);
         }
         break;

      case CXCompletionChunk_Text:
      case CXCompletionChunk_Placeholder:
      case CXCompletionChunk_Informative:
         text += toStdString(clang().getCompletionChunkText(cs, i));
         break;
      case CXCompletionChunk_CurrentParameter:
         // (not currently doing anything with CurrentParameter)
         break;
      case CXCompletionChunk_LeftParen:
         text += "(";
         break;
      case CXCompletionChunk_RightParen:
         text += ")";
         break;
      case CXCompletionChunk_LeftBracket:
         text += "]";
         break;
      case CXCompletionChunk_RightBracket:
         text += "]";
         break;
      case CXCompletionChunk_LeftBrace:
         text += "{";
         break;
      case CXCompletionChunk_RightBrace:
         text += "}";
         break;
      case CXCompletionChunk_LeftAngle:
         text += "<";
         break;
      case CXCompletionChunk_RightAngle:
         text += ">";
         break;
      case CXCompletionChunk_Comma:
         text += ", ";
         break;
      case CXCompletionChunk_Colon:
         text += ":";
         break;
      case CXCompletionChunk_SemiColon:
         text += ";";
         break;
      case CXCompletionChunk_Equal:
         text += "=";
         break;
      case CXCompletionChunk_HorizontalSpace:
         text += " ";
         break;
      case CXCompletionChunk_VerticalSpace:
         text += "\n";
         break;
      default:
         break;
   }

   return text;
}

} // anonymous namespace

CodeCompleteResult::CodeCompleteResult(CXCompletionResult result)
{
   kind_ = result.CursorKind;

   std::string resultType;

   CXCompletionString cs = result.CompletionString;
   unsigned chunks = clang().getNumCompletionChunks(cs);
   for (unsigned i = 0; i < chunks; i++)
   {
      CXCompletionChunkKind kind = clang().getCompletionChunkKind(cs, i);
      switch(kind)
      {
         case CXCompletionChunk_TypedText:
            typedText_ = toStdString(clang().getCompletionChunkText(cs, i));
            text_ += typedText_;
            break;
         case CXCompletionChunk_ResultType:
            resultType = toStdString(clang().getCompletionChunkText(cs, i));
            break;
         default:
            text_ += completionText(cs, i);
      }
   }

   if (!resultType.empty())
      text_ = resultType + " " + text_;

   availability_ = clang().getCompletionAvailability(cs);
   priority_ = clang().getCompletionPriority(cs);
   comment_ = toStdString(clang().getCompletionBriefComment(cs));
}

CodeCompleteResults:: ~CodeCompleteResults()
{
   try
   {
      if (!empty())
         clang().disposeCodeCompleteResults(pResults_);
   }
   catch(...)
   {
   }
}

void CodeCompleteResults::sort()
{
   clang().sortCodeCompletionResults(pResults_->Results,
                                     pResults_->NumResults);
}

CodeCompleteResult CodeCompleteResults::getResult(unsigned index) const
{
   return CodeCompleteResult(pResults_->Results[index]);
}

unsigned long long CodeCompleteResults::getContexts() const
{
   return clang().codeCompleteGetContexts(pResults_);
}


} // namespace libclang
} // namespace core
} // namespace rstudio


