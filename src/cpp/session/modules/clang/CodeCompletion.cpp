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

#include <core/Debug.hpp>
#include <core/Error.hpp>
#include <core/system/Process.hpp>
#include <core/RegexUtils.hpp>

#include <r/RExec.hpp>

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
const int kCompletionFile = 12;
const int kCompletionDirectory = 13;

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

void discoverSystemIncludePaths(std::vector<std::string>* pIncludePaths)
{
   core::system::ProcessOptions processOptions;
   processOptions.redirectStdErrToStdOut = true;
   
   // get the CXX compiler by asking R
   std::string compilerPath;
   {
      core::system::ProcessResult result;
      std::string cmd = "R CMD config CXX";
      Error error = core::system::runCommand(cmd, processOptions, &result);
      if (error)
         LOG_ERROR(error);
      else if (result.exitStatus != EXIT_SUCCESS)
         LOG_ERROR_MESSAGE("Error querying CXX compiler: " + result.stdOut);
      else
         compilerPath = string_utils::trimWhitespace(result.stdOut);
   }
   if (compilerPath.empty())
      return;
   
   // ask the compiler what the system include paths are (note that both
   // gcc and clang accept the same command)
   std::string includePathOutput;
   {
      core::system::ProcessResult result;
      std::string cmd = compilerPath + " -E -x c++ - -v < /dev/null";
      Error error = core::system::runCommand(cmd, processOptions, &result);
      if (error)
         LOG_ERROR(error);
      else if (result.exitStatus != EXIT_SUCCESS)
         LOG_ERROR_MESSAGE("Error retrieving system include paths: " + result.stdOut);
      else
         includePathOutput = string_utils::trimWhitespace(result.stdOut);
   }
   if (includePathOutput.empty())
      return;
   
   // strip out the include paths from the output
   std::string startString = "#include <...> search starts here:";
   std::string endString   = "End of search list.";
   
   std::string::size_type startPos = includePathOutput.find(startString);
   if (startPos == std::string::npos)
      return;
   
   std::string::size_type endPos = includePathOutput.find(endString, startPos);
   if (endPos == std::string::npos)
      return;
   
   std::string includePathsString = string_utils::trimWhitespace(
            string_utils::substring(includePathOutput,
                                    startPos + startString.size(),
                                    endPos));
   
   // TODO: framework paths can be included here; how should we handle?
   
   // Split on newlines, and trim whitespace for each
   std::vector<std::string> includePaths = core::algorithm::split(includePathsString, "\n");
   for (std::size_t i = 0, n = includePaths.size(); i < n; ++i)
      includePaths[i] = string_utils::trimWhitespace(includePaths[i]);
   
   pIncludePaths->insert(
            pIncludePaths->end(),
            includePaths.begin(),
            includePaths.end());
}

json::Object jsonHeaderCompletionResult(const std::string& name,
                                        int completionType)
{
   json::Object completionJson;
   completionJson["type"]       = completionType;
   completionJson["typed_text"] = name;
   
   json::Array textJson;
   json::Object objectJson;
   objectJson["text"]    = "<File '" + name + "'>"; 
   objectJson["comment"] = "";
   textJson.push_back(objectJson);
   completionJson["text"] = textJson;

   return completionJson;
}

void discoverRPackageIncludePaths(std::vector<std::string>* pIncludePaths)
{
   std::vector<std::string> rPkgs;
   rPkgs.push_back("Rcpp");
   
   std::vector<FilePath> libPaths = module_context::getLibPaths();
   BOOST_FOREACH(const std::string& rPkg, rPkgs)
   {
      BOOST_FOREACH(const FilePath& libPath, libPaths)
      {
         FilePath pkgPath = libPath.complete(rPkg + "/include");
         if (pkgPath.exists())
         {
            pIncludePaths->push_back(pkgPath.absolutePath());
            break;
         }
      }
   }
   
   // TODO: add 'inst/include' path if the source file exists within
   // a project directory (e.g. 'src/')
}

Error getSystemHeaderCompletions(const std::string& token,
                                 const std::string& parentDir,
                                 const core::json::JsonRpcRequest& request,
                                 core::json::JsonRpcResponse* pResponse)
{
   // TODO: parse Rcpp::depends
   // TODO: parse LinkingTo, etc. for files within package
   
   
   // discover the system headers
   std::vector<std::string> includePaths;
   discoverSystemIncludePaths(&includePaths);
   
   // add R package include paths in library
   discoverRPackageIncludePaths(&includePaths);
   
   // loop through header include paths and return paths that match token
   json::Array completionsJson;
   
   BOOST_FOREACH(const std::string& path, includePaths)
   {
      FilePath includePath(path);
      if (!includePath.exists())
         continue;
      
      FilePath targetPath = includePath.complete(parentDir);
      if (!targetPath.exists())
         continue;
      
      std::vector<FilePath> children;
      Error error = targetPath.children(&children);
      if (error)
         LOG_ERROR(error);
      
      BOOST_FOREACH(const FilePath& childPath, children)
      {
         std::string name = childPath.filename();
         if (string_utils::isSubsequence(name, token))
         {
            int type = childPath.isDirectory() ? kCompletionDirectory : kCompletionFile;
            completionsJson.push_back(jsonHeaderCompletionResult(name, type));
         }
      }
   }
   
   // construct completion result
   json::Object resultJson;
   resultJson["completions"] = completionsJson;
   pResponse->setResult(resultJson);
   return Success();
}

Error getLocalHeaderCompletions(const std::string& token,
                                const std::string& parentDir,
                                const core::json::JsonRpcRequest& request,
                                core::json::JsonRpcResponse* pResponse)
{
   // TODO
   return Success();
}

Error getHeaderCompletions(std::string line,
                           int column,
                           const core::json::JsonRpcRequest& request,
                           core::json::JsonRpcResponse* pResponse)
{
   // trim line to cursor position
   line = line.substr(0, column + 2);
   
   // extract portion of the path that the user has produced
   std::string::size_type idx = line.find_first_of("<\"");
   if (idx == std::string::npos)
      return Success();
   
   bool isSystemHeader = line[idx] == '<';
   std::string pathString = line.substr(idx + 1);
   
   // split path into 'parentDir' + 'token', e.g.
   //
   //    #include <foo/bar/ba
   //              ^^^^^^^|^^
   std::string parentDir, token;
   std::string::size_type pathDelimIdx = pathString.find_last_of("/");
   if (pathDelimIdx == std::string::npos)
   {
      token = pathString;
   }
   else
   {
      parentDir = pathString.substr(0, pathDelimIdx);
      token     = pathString.substr(pathDelimIdx + 1);
   }
   
   if (isSystemHeader)
      return getSystemHeaderCompletions(token, parentDir, request, pResponse);
   else
      return getLocalHeaderCompletions(token, parentDir, request, pResponse);
}


} // anonymous namespace


Error getCppCompletions(const core::json::JsonRpcRequest& request,
                        core::json::JsonRpcResponse* pResponse)
{
   // empty response by default
   pResponse->setResult(json::Value());
   
   // get params
   std::string line, docPath, userText;
   int row, column;
   Error error = json::readParams(request.params,
                                  &line,
                                  &docPath,
                                  &row,
                                  &column,
                                  &userText);
   if (error)
      return error;
   
   // if it looks like we're requesting autocompletions for an '#include'
   // statement, take a separate completion path
   static const boost::regex reInclude("^#\\s*include");
   if (regex_utils::textMatches(line, reInclude, true, true))
      return getHeaderCompletions(line, column, request, pResponse);

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
                              tu.codeCompleteAt(filename, row, column);
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

   return Success();
}

} // namespace clang
} // namespace modules
} // namesapce session
} // namespace rstudio

