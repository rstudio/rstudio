/*
 * CodeCompletion.cpp
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

#include "CodeCompletion.hpp"

#include <iostream>

#include <core/Debug.hpp>
#include <shared_core/Error.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Process.hpp>
#include <core/RegexUtils.hpp>

#include <r/RExec.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "RCompilationDatabase.hpp"
#include "RSourceIndex.hpp"
#include "Diagnostics.hpp"

using namespace rstudio::core;
using namespace rstudio::core::libclang;

#ifndef _WIN32
# define kDevNull "/dev/null"
#else
# define kDevNull "NUL"
#endif

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

namespace {

// cache our system include paths
std::vector<std::string> s_systemIncludePaths;

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

void discoverTranslationUnitIncludePaths(const FilePath& filePath,
                                         std::vector<std::string>* pIncludePaths)
{
   std::vector<std::string> args =
         rCompilationDatabase().compileArgsForTranslationUnit(
            filePath.getAbsolutePathNative(), false);
   
   for (const std::string& arg : args)
   {
      if (boost::algorithm::starts_with(arg, "-I"))
      {
         // skip RStudio's libclang builtin header paths
         if (arg.find("libclang/builtin-headers") != std::string::npos)
            continue;
         
         pIncludePaths->push_back(
                  string_utils::strippedOfQuotes(arg.substr(2)));
      }
   }
}

} // end anonymous namespace

void discoverSystemIncludePaths(std::vector<std::string>* pIncludePaths)
{
   // if we've cached results already, just use that
   if (!s_systemIncludePaths.empty())
   {
      pIncludePaths->insert(
               pIncludePaths->end(),
               s_systemIncludePaths.begin(),
               s_systemIncludePaths.end());
      return;
   }
   
   core::system::ProcessOptions processOptions;
   processOptions.redirectStdErrToStdOut = true;
   
   // add Rtools to PATH if necessary
   core::system::Options environment;
   core::system::environment(&environment);

   std::string warning;
   module_context::addRtoolsToPathIfNecessary(&environment, &warning);
   processOptions.environment = environment;
   
   // get the CXX compiler by asking R
   std::string compilerPath;

#ifdef _WIN32
   {
      core::system::ProcessResult result;
      Error error = core::system::runCommand("where.exe gcc.exe", processOptions, &result);
      if (error)
         LOG_ERROR(error);
      else if (result.exitStatus != EXIT_SUCCESS)
         LOG_ERROR_MESSAGE("Error querying CXX compiler: " + result.stdOut);
      else
         compilerPath = string_utils::trimWhitespace(result.stdOut);
   }
#else
   {
      Error error;
      
      // resolve R CMD location for shell command
      FilePath rBinDir;
      error = module_context::rBinDir(&rBinDir);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      shell_utils::ShellCommand rCmd = module_context::rCmd(rBinDir);

      core::system::ProcessResult result;
      rCmd << "config";
      rCmd << "CXX";
      error = core::system::runCommand(rCmd, processOptions, &result);
      if (error)
         LOG_ERROR(error);
      else if (result.exitStatus != EXIT_SUCCESS)
         LOG_ERROR_MESSAGE("Error querying CXX compiler: " + result.stdOut);
      else
         compilerPath = string_utils::trimWhitespace(result.stdOut);
   }
#endif

   if (compilerPath.empty())
      return;

   // it is likely that R is configured to use a compiler that exists
   // on the PATH; however, when invoked through 'runCommand()' this
   // can fail unless we explicitly find and resolve that path. also
   // note that one can configure CXX with multiple arguments
   // (e.g. as 'g++ -std=c++17') so we must also tear off only the
   // compiler name. we hence assume that the supplemental arguments
   // to CXX do not influence the compiler include paths.
#ifndef _WIN32
   {
       std::string compilerName = compilerPath;
       std::size_t index = compilerPath.find(' ');
       if (index != std::string::npos)
       {
           compilerName = compilerPath.substr(0, index);
       }
       else
       {
           compilerName = compilerPath;
       }

       core::system::ProcessResult result;
       std::vector<std::string> args = { compilerName };
       Error error = core::system::runProgram("/usr/bin/which", args, "", processOptions, &result);
       if (error)
          LOG_ERROR(error);
       else if (result.exitStatus != EXIT_SUCCESS)
          LOG_ERROR_MESSAGE("Error qualifying CXX compiler path: " + result.stdOut);
       else
          compilerPath = string_utils::trimWhitespace(result.stdOut);
   }
#endif
   
   // ask the compiler what the system include paths are (note that both
   // gcc and clang accept the same command)
   std::string includePathOutput;
   {
      core::system::ProcessResult result;
      std::string cmd = compilerPath + " -E -x c++ - -v < " kDevNull;

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
   
   // split on newlines
   std::vector<std::string> includePaths = core::algorithm::split(includePathsString, "\n");
   
   // remove framework directories
   core::algorithm::expel_if(includePaths, string_utils::Contains("(framework directory)"));
   
   // trim whitespace
   for (std::size_t i = 0, n = includePaths.size(); i < n; ++i)
      includePaths[i] = string_utils::trimWhitespace(includePaths[i]);
   
   // update cache
   s_systemIncludePaths = includePaths;
   
   // fill result vector
   pIncludePaths->insert(
            pIncludePaths->end(),
            includePaths.begin(),
            includePaths.end());
}

namespace {

void discoverRelativeIncludePaths(const FilePath& filePath,
                                  const std::string& parentDir,
                                  std::vector<std::string>* pIncludePaths)
{
   // Construct the directory in which to search for includes
   FilePath targetPath = filePath.getParent().completePath(parentDir);
   if (!targetPath.exists())
      return;
   
   pIncludePaths->push_back(targetPath.getAbsolutePath());
}

json::Object jsonHeaderCompletionResult(const std::string& name,
                                        const std::string& source,
                                        int completionType)
{
   json::Object completionJson;
   completionJson["type"]       = completionType;
   completionJson["typed_text"] = name;
   
   json::Array textJson;
   json::Object objectJson;
   objectJson["text"]    = name;
   objectJson["comment"] = source;
   textJson.push_back(objectJson);
   completionJson["text"] = textJson;

   return completionJson;
}

Error getHeaderCompletionsImpl(const std::string& token,
                               const std::string& parentDir,
                               const FilePath& filePath,
                               const std::string& docId,
                               bool systemHeadersOnly,
                               const core::json::JsonRpcRequest& request,
                               core::json::JsonRpcResponse* pResponse)
{
   std::vector<std::string> includePaths;
   
   // discover the system headers
   discoverSystemIncludePaths(&includePaths);
   
   // discover TU-related include paths
   discoverTranslationUnitIncludePaths(filePath, &includePaths);
   
   // discover local include paths
   if (!systemHeadersOnly)
      discoverRelativeIncludePaths(filePath, parentDir, &includePaths);
   
   // remove dupes
   std::sort(includePaths.begin(), includePaths.end());
   includePaths.erase(std::unique(includePaths.begin(), includePaths.end()),
                      includePaths.end());
   
   // loop through header include paths and return paths that match token
   std::set<std::string> discoveredEntries;
   json::Array completionsJson;
   
   for (const std::string& path : includePaths)
   {
      FilePath includePath(path);
      if (!includePath.exists())
         continue;
      
      FilePath targetPath = includePath.completePath(parentDir);
      if (!targetPath.exists())
         continue;
      
      std::vector<FilePath> children;
      Error error = targetPath.getChildren(children);
      if (error)
         LOG_ERROR(error);
      
      for (const FilePath& childPath : children)
      {
         std::string name = childPath.getFilename();
         if (discoveredEntries.count(name))
            continue;
         
         std::string extension = childPath.getExtensionLowerCase();
         if (!(extension == ".h" || extension == ".hpp" || extension == ""))
            continue;
         
         if (string_utils::isSubsequence(name, token, true))
         {
            int type = childPath.isDirectory() ? kCompletionDirectory : kCompletionFile;
            completionsJson.push_back(jsonHeaderCompletionResult(name,
                                                                 childPath.getAbsolutePath(),
                                                                 type));
         }
         
         discoveredEntries.insert(name);
      }
   }
   
   // construct completion result
   json::Object resultJson;
   resultJson["completions"] = completionsJson;
   pResponse->setResult(resultJson);
   return Success();
}

Error getHeaderCompletions(std::string line,
                           const FilePath& filePath,
                           const std::string& docId,
                           const core::json::JsonRpcRequest& request,
                           core::json::JsonRpcResponse* pResponse)
{
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
   
   return getHeaderCompletionsImpl(token,
                                   parentDir,
                                   filePath,
                                   docId,
                                   isSystemHeader,
                                   request,
                                   pResponse);
}


} // anonymous namespace


Error getCppCompletions(const core::json::JsonRpcRequest& request,
                        core::json::JsonRpcResponse* pResponse)
{
   // empty response by default
   pResponse->setResult(json::Value());
   
   // get params
   std::string line, docPath, docId, userText;
   int row, column;
   Error error = json::readParams(request.params,
                                  &line,
                                  &docPath,
                                  &docId,
                                  &row,
                                  &column,
                                  &userText);
   if (error)
      return error;
   
   // resolve the docPath if it's aliased
   FilePath filePath = module_context::resolveAliasedPath(docPath);

   // if it looks like we're requesting autocompletions for an '#include'
   // statement, take a separate completion path
   static const boost::regex reInclude("^\\s*#+\\s*include");
   if (regex_utils::textMatches(line, reInclude, true, true))
      return getHeaderCompletions(line, filePath, docId, request, pResponse);

   // get the translation unit and do the code completion
   std::string filename = filePath.getAbsolutePath();
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
            if ((typedText == lastTypedText) && !completionsJson.isEmpty())
            {
               json::Object res = completionsJson.getBack().getObject();
               json::Array text = res["text"].getArray();
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
      resultJson["completions"] = completionsJson.clone();
      pResponse->setResult(resultJson);
   }

   return Success();
}

} // namespace clang
} // namespace modules
} // namespace session
} // namespace rstudio

