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

using namespace core ;
using namespace core::libclang;

namespace session {
namespace modules { 
namespace clang {

namespace {


std::string friendlyCompletionText(std::string text)
{
   boost::algorithm::replace_all(
     text,
     "std::basic_string<char, std::char_traits<char>, std::allocator<char> >",
     "std::string");

   return text;
}

core::json::Object toJson(const CodeCompleteResult& result)
{
   json::Object resultJson;
   resultJson["kind"] = result.getKind();
   resultJson["typed_text"] = result.getTypedText();
   json::Array textJson;
   textJson.push_back(friendlyCompletionText(result.getText()));
   resultJson["text"] = textJson;
   return resultJson;
}


} // anonymous namespace


Error getCppCompletions(const core::json::JsonRpcRequest& request,
                        core::json::JsonRpcResponse* pResponse)
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

   // get the translation unit and do the code completion
   std::string filename = filePath.absolutePath();
   TranslationUnit tu = rSourceIndex().getTranslationUnit(filename);
   if (!tu.empty())
   {
      std::string lastTypedText;
      json::Array completionsJson;
      CodeCompleteResults results = tu.codeCompleteAt(filename, line, column);
      if (!results.empty())
      {
         for (unsigned i = 0; i<results.getNumResults(); i++)
         {
            CodeCompleteResult result = results.getResult(i);

            // check whether this completion is valid and bail if not
            if (result.getAvailability() != CXAvailability_Available &&
                result.getAvailability() != CXAvailability_Deprecated)
            {
               continue;
            }

            std::string typedText = result.getTypedText();

            // if we have the same typed text then just ammend previous result
            if ((typedText == lastTypedText) && !completionsJson.empty())
            {
               json::Object& res = completionsJson.back().get_obj();
               json::Array& text = res["text"].get_array();
               text.push_back(friendlyCompletionText(result.getText()));
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

