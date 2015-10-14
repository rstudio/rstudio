/*
 * SessionShiny.cpp
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

#include "SessionShiny.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>

#include <r/RExec.hpp>

#include <session/SessionRUtil.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#define kShinyTypeNone       "none"
#define kShinyTypeDirectory  "shiny-dir"
#define kShinyTypeSingleFile "shiny-single-file"
#define kShinyTypeSingleExe  "shiny-single-executable"
#define kShinyTypeDocument   "shiny-document"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace shiny {

namespace {

void onPackageLoaded(const std::string& pkgname)
{
   // we need an up to date version of shiny when running in server mode
   // to get the websocket protocol/path and port randomizing changes
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      if (pkgname == "shiny")
      {
         if (!module_context::isPackageVersionInstalled("shiny", "0.8"))
         {
            module_context::consoleWriteError("\nWARNING: To run Shiny "
              "applications with RStudio you need to install the "
              "latest version of the Shiny package from CRAN (version 0.8 "
              "or higher is required).\n\n");
         }
      }
   }
}



bool isShinyAppDir(const FilePath& filePath)
{
   bool hasServer = filePath.childPath("server.R").exists() ||
                    filePath.childPath("server.r").exists();
   if (hasServer)
   {
      bool hasUI = filePath.childPath("ui.R").exists() ||
                   filePath.childPath("ui.r").exists() ||
                   filePath.childPath("www").exists();

      return hasUI;
   }
   else
   {
      return false;
   }
}

std::string onDetectShinySourceType(
      boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!pDoc->path().empty())
   {
      FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
      ShinyFileType type = getShinyFileType(filePath, pDoc->contents());
      switch(type)
      {
         case ShinyNone:
            return kShinyTypeNone;
         case ShinyDirectory:
            return kShinyTypeDirectory;
         case ShinySingleFile:
            return kShinyTypeSingleFile;
         case ShinySingleExecutable:
            return kShinyTypeSingleExe;
         case ShinyDocument:
            return kShinyTypeDocument;
      }
   }

   return std::string();
}

Error getShinyCapabilities(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   json::Object capsJson;
   capsJson["installed"] = module_context::isPackageInstalled("shiny");
   pResponse->setResult(capsJson);

   return Success();
}


// when detecting single-file Shiny applications, we need to look for the last
// function in the file. to get this right all the time, we'd need to fully 
// parse the file, but since this code runs every time the file contents are
// saved, it needs to be fast, so we use this heuristic approach instead.
std::string getLastFunction(const std::string& fileContents)
{
   std::string function;

   // discard all the comments in the file
   std::string contents = 
      boost::regex_replace(fileContents, boost::regex("#[^\n]*\n"), "");

   // make sure there's nothing but space up to the last closing paren
   size_t lastParenPos = std::string::npos;
   for (size_t i = contents.size() - 1; i > 0; i--)
   {
      if (std::isspace(contents.at(i)))
         continue;
      if (contents.at(i) == ')')
         lastParenPos = i;
      break;
   }

   if (lastParenPos == std::string::npos)
      return function;
   
   // now find its match 
   int unbalanced = 0;
   size_t functionEndPos = std::string::npos;
   for (size_t i = lastParenPos - 1; i > 0; i--) 
   {
      if (contents.at(i) == ')')
      {
         unbalanced++;
      }
      else if (contents.at(i) == '(')
      {
         if (unbalanced == 0)
         {
            functionEndPos = i - 1;
            break;
         }
         else
            unbalanced--;
      }
   }

   // bail out if we rewound through the whole file without finding a match
   if (functionEndPos == std::string::npos)
      return function;

   // skip any whitespace between function paren and name
   while (std::isspace(contents.at(functionEndPos)) && functionEndPos > 0)
      functionEndPos--;

   // now work backward again to find the function name 
   size_t functionStartPos = functionEndPos - 1;
   for (size_t i = functionEndPos; i > 0; i--)
   {
      char ch = contents.at(i);
      if (!(std::isalnum(ch) || ch == '_' || ch == '.')) {
         functionStartPos = i + 1;
         break;
      }
   }

   // return the function
   function = contents.substr(functionStartPos, 
                              (functionEndPos - functionStartPos) + 1);

   return function;
}

} // anonymous namespace

ShinyFileType shinyTypeFromExtendedType(const std::string& extendedType)
{
   if (extendedType == kShinyTypeDirectory)
      return ShinyDirectory;
   else if (extendedType == kShinyTypeSingleFile)
      return ShinySingleFile;
   else if (extendedType == kShinyTypeSingleExe)
      return ShinySingleExecutable;
   else if (extendedType == kShinyTypeDocument)
      return ShinyDocument;
   return ShinyNone;
}

ShinyFileType getShinyFileType(const FilePath& filePath,
                     const std::string& contents)
{
   static const boost::regex reRuntimeShiny("runtime:\\s*shiny");
   
   // Check for 'runtime: shiny' in a YAML header.
   std::string yamlHeader = r_utils::extractYamlHeader(contents);
   if (boost::regex_search(yamlHeader.begin(), yamlHeader.end(), reRuntimeShiny))
      return ShinyDocument;
   
   std::string filename = filePath.filename();

   if (boost::algorithm::iequals(filename, "ui.r") &&
       boost::algorithm::icontains(contents, "shinyUI"))
   {
      return ShinyDirectory;
   }
   else if (boost::algorithm::iequals(filename, "server.r") &&
            boost::algorithm::icontains(contents, "shinyServer"))
   {
      return ShinyDirectory;
   }
   else if (boost::algorithm::iequals(filename, "app.r") && 
            boost::algorithm::icontains(contents, "shinyApp"))
   {
      return ShinyDirectory;
   }
   else if ((boost::algorithm::iequals(filename, "global.r") ||
             boost::algorithm::iequals(filename, "ui.r") ||
             boost::algorithm::iequals(filename, "server.r")) &&
            isShinyAppDir(filePath.parent()))
   {
      return ShinyDirectory;
   }
   else
   {
      // detect standalone single-file Shiny applications
      std::string lastFunction = getLastFunction(contents);
      if (lastFunction == "shinyApp")
         return ShinySingleFile;
      else if (lastFunction == "runApp")
         return ShinySingleExecutable;
   }

   return ShinyNone;
}

bool isShinyRMarkdownDocument(const FilePath& filePath)
{
   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   
   static const boost::regex reRuntimeShiny("runtime:\\s*shiny");
   
   std::string yamlHeader = r_utils::extractYamlHeader(contents);
   return boost::regex_search(yamlHeader.begin(), yamlHeader.end(), reRuntimeShiny);
}

ShinyFileType getShinyFileType(const FilePath& filePath)
{
   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return ShinyNone;
   }
   
   return getShinyFileType(filePath, contents);
}

Error initialize()
{
   using namespace module_context;
   events().onPackageLoaded.connect(onPackageLoaded);

   events().onDetectSourceExtendedType.connect(onDetectShinySourceType);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(registerRpcMethod, "get_shiny_capabilities", getShinyCapabilities));

   return initBlock.execute();
}


} // namespace crypto
} // namespace modules
} // namesapce session
} // namespace rstudio

