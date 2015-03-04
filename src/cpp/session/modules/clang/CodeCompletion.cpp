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

#include <iostream>

#include <core/Error.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "RSourceIndex.hpp"
#include "Diagnostics.hpp"

using namespace rstudio::core ;
using namespace rstudio::core::libclang;

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

namespace {


json::Object friendlyCompletionText(const CodeCompleteResult& result)
{
   // transform text
   std::string text = result.getText();
   boost::algorithm::replace_all(
     text,
     "std::basic_string<char, std::char_traits<char>, std::allocator<char> >",
     "std::string");

   // creat text object
   json::Object textJson;
   textJson["text"] = text;
   textJson["comment"] = result.getComment();

   return textJson;
}

const int kCompletionUnknown = 0;
const int kCompletionVariable = 1;
const int kCompletionFunction = 2;
const int kCompletionConstructor = 3;
const int kCompletionDestructor = 4;
const int kCompletionClass = 5;
const int kCompletionStruct = 6;
const int kCompletionNamespace = 7;
const int kCompletionEnum = 8;
const int kCompletionEnumValue = 9;
const int kCompletionKeyword = 10;
const int kCompletionMacro = 11;

int completionType(CXCursorKind kind)
{
   switch(kind)
   {
   case CXCursor_UnexposedDecl:
      return kCompletionVariable;
   case CXCursor_StructDecl:
   case CXCursor_UnionDecl:
      return kCompletionStruct;
   case CXCursor_ClassDecl:
      return kCompletionClass;
   case CXCursor_EnumDecl:
      return kCompletionEnum;
   case CXCursor_FieldDecl:
      return kCompletionVariable;
   case CXCursor_EnumConstantDecl:
      return kCompletionEnumValue;
   case CXCursor_FunctionDecl:
      return kCompletionFunction;
   case CXCursor_VarDecl:
   case CXCursor_ParmDecl:
      return kCompletionVariable;
   case CXCursor_ObjCInterfaceDecl:
   case CXCursor_ObjCCategoryDecl:
   case CXCursor_ObjCProtocolDecl:
      return kCompletionClass;
   case CXCursor_ObjCPropertyDecl:
   case CXCursor_ObjCIvarDecl:
      return kCompletionVariable;
   case CXCursor_ObjCInstanceMethodDecl:
   case CXCursor_ObjCClassMethodDecl:
      return kCompletionFunction;
   case CXCursor_ObjCImplementationDecl:
   case CXCursor_ObjCCategoryImplDecl:
      return kCompletionClass;
   case CXCursor_TypedefDecl: // while these are typically classes, we don't
                              // have access to the underlying cursor for the
                              // completion (just a CXCursorKind) so there is
                              // no way to know for sure

      return kCompletionClass;
   case CXCursor_CXXMethod:
      return kCompletionFunction;
   case CXCursor_Namespace:
      return kCompletionNamespace;
   case CXCursor_LinkageSpec:
      return kCompletionKeyword;
   case CXCursor_Constructor:
      return kCompletionConstructor;
   case CXCursor_Destructor:
      return kCompletionDestructor;
   case CXCursor_ConversionFunction:
      return kCompletionFunction;
   case CXCursor_TemplateTypeParameter:
   case CXCursor_NonTypeTemplateParameter:
      return kCompletionVariable;
   case CXCursor_FunctionTemplate:
      return kCompletionFunction;
   case CXCursor_ClassTemplate:
   case CXCursor_ClassTemplatePartialSpecialization:
      return kCompletionClass;
   case CXCursor_NamespaceAlias:
   case CXCursor_UsingDirective:
   case CXCursor_UsingDeclaration:
   case CXCursor_TypeAliasDecl:
      return kCompletionVariable;
   case CXCursor_ObjCSynthesizeDecl:
   case CXCursor_ObjCDynamicDecl:
   case CXCursor_CXXAccessSpecifier:
      return kCompletionKeyword;
   case CXCursor_MacroDefinition:
      return kCompletionMacro;
   default:
      return kCompletionUnknown;
   }
}

core::json::Object toJson(const CodeCompleteResult& result)
{
   json::Object resultJson;
   resultJson["type"] = completionType(result.getKind());
   resultJson["typed_text"] = result.getTypedText();
   json::Array textJson;
   textJson.push_back(friendlyCompletionText(result));
   resultJson["text"] = textJson;
   return resultJson;
}


} // anonymous namespace


Error getCppCompletions(const core::json::JsonRpcRequest& request,
                        core::json::JsonRpcResponse* pResponse)
{
   // get params
   std::string docPath, userText;
   int line, column;
   Error error = json::readParams(request.params,
                                  &docPath,
                                  &line,
                                  &column,
                                  &userText);
   if (error)
      return error;

   // resolve the docPath if it's aliased
   FilePath filePath = module_context::resolveAliasedPath(docPath);

   // get the translation unit and do the code completion
   std::string filename = filePath.absolutePath();
   TranslationUnit tu = rSourceIndex().getTranslationUnit(filename);

   if (!tu.empty())
   {
      std::string lastTypedText;
      json::Array completionsJson;
      boost::shared_ptr<CodeCompleteResults> pResults =
                              tu.codeCompleteAt(filename, line, column);
      if (!pResults->empty())
      {
         // get results
         for (unsigned i = 0; i<pResults->getNumResults(); i++)
         {
            CodeCompleteResult result = pResults->getResult(i);

            // filter on user text if we have it
            if (!userText.empty() &&
                !boost::algorithm::starts_with(result.getTypedText(), userText))
            {
               continue;
            }

            // check whether this completion is valid and bail if not
            if (result.getAvailability() != CXAvailability_Available)
            {
               continue;
            }

            std::string typedText = result.getTypedText();

            // if we have the same typed text then just ammend previous result
            if ((typedText == lastTypedText) && !completionsJson.empty())
            {
               json::Object& res = completionsJson.back().get_obj();
               json::Array& text = res["text"].get_array();
               text.push_back(friendlyCompletionText(result));
            }
            else
            {
               completionsJson.push_back(toJson(result));
            }

            lastTypedText = typedText;
         }
      }

      json::Object resultJson;
      resultJson["completions"] = completionsJson;
      pResponse->setResult(resultJson);
   }
   else
   {
      // set null result indicating this file doesn't support completions
      pResponse->setResult(json::Value());
   }

   return Success();
}

} // namespace clang
} // namespace modules
} // namesapce session
} // namespace rstudio

