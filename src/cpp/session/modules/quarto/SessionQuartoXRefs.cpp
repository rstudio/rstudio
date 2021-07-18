/*
 * SessionQuartoXRefs.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include "SessionQuartoXRefs.hpp"

#include <algorithm>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/FileScanner.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionQuarto.hpp>

#include "SessionQuarto.hpp"

using namespace rstudio::core;
using namespace rstudio::session::module_context;

namespace rstudio {
namespace session {

using namespace quarto;

namespace {

const char * const kBaseDir = "baseDir";
const char * const kRefs = "refs";
const char * const kFile = "file";
const char * const kType = "type";
const char * const kId = "id";
const char * const kSuffix = "suffix";
const char * const kTitle = "title";

FilePath quartoCrossrefDir(const FilePath& projectDir)
{
   return projectDir
       .completeChildPath(".quarto")
       .completeChildPath("crossref");
}


json::Array readXRefIndex(const FilePath& indexPath, const std::string& filename)
{
   // read the index as a string
   std::string index;
   Error error = core::readStringFromFile(indexPath, &index);
   if (error)
   {
      LOG_ERROR(error);
      return json::Array();
   }

   // tolerate an empty file
   if (boost::algorithm::trim_copy(index).empty())
      return json::Array();

   // parse json w/ validation
   json::Object quartoIndexJson;
   error = quartoIndexJson.parseAndValidate(
      index,
      resourceFileAsString("schema/quarto-xref.json")
   );
   if (error)
   {
      LOG_ERROR(error);
      return json::Array();
   }

   // read xrefs (already validated so don't need to dance around types/existence)
   json::Array xrefs;
   boost::regex keyRegex("^(\\w+)-(.*?)(-\\d+)?$");
   json::Array entries = quartoIndexJson["entries"].getArray();
   for (const json::Value& entry : entries)
   {
      json::Object valObject = entry.getObject();
      std::string key, caption;
      json::readObject(valObject, "key", key, "caption", caption);
      boost::smatch match;
      if (boost::regex_search(key, match, keyRegex))
      {
         json::Object xref;
         xref[kFile] = filename;
         xref[kType] = match[1].str();
         xref[kId] = match[2].str();
         xref[kSuffix] = (match.length() > 3) ? match[3].str() : "";
         xref[kTitle] = caption;
         xrefs.push_back(xref);
      }
   }
   return xrefs;
}

json::Array readProjectXRefIndex(const FilePath& indexPath, std::string filename)
{
   if (indexPath.isDirectory())
   {
      // there will be one or more json files in here (for each format). just
      // pick the most recently written one
      std::vector<FilePath> indexFiles;
      Error error = indexPath.getChildren(indexFiles);
      if (error)
      {
         LOG_ERROR(error);
         return json::Array();
      }
      FilePath mostRecentIndex;
      for (auto indexFile : indexFiles)
      {
         if (indexFile.getExtensionLowerCase() == ".json")
         {
            if (mostRecentIndex.isEmpty())
            {
               mostRecentIndex = indexFile;
            }
            else if (indexFile.getLastWriteTime() > mostRecentIndex.getLastWriteTime())
            {
               mostRecentIndex = indexFile;
            }
         }
      }
      if (!mostRecentIndex.isEmpty())
      {
         return readXRefIndex(mostRecentIndex, filename);
      }
      else
      {
         return json::Array();
      }
   }
   else
   {
      return json::Array();
   }
}

json::Array readProjectXRefIndex(const FilePath& projectDir, const FilePath& srcFile)
{
   std::string projRelative = srcFile.getRelativePath(projectDir);
   FilePath indexPath = quartoCrossrefDir(projectDir).completeChildPath(projRelative);
   return readProjectXRefIndex(indexPath, projRelative);

}

bool projectXRefIndexFilter(const FilePath& projectDir,
                            const FilePath& crossrefDir,
                            const FileInfo& fileInfo)
{
   if (fileInfo.isDirectory())
   {
      // see if this corresponds to an actual source file
      std::string relativePath = FilePath(fileInfo.absolutePath()).getRelativePath(crossrefDir);
      FilePath srcFilePath = projectDir.completeChildPath(relativePath);
      return srcFilePath.exists();
   }
   else
   {
      return false;
   }
}

json::Array readAllProjectXRefIndexes(const core::FilePath& projectDir)
{
   FilePath crossrefDir = quartoCrossrefDir(projectDir);
   if (!crossrefDir.exists())
      return json::Array();

   core::system::FileScannerOptions options;
   options.recursive = true;
   options.filter = boost::bind(projectXRefIndexFilter, projectDir, crossrefDir, _1);

   // scan for directories
   tree<FileInfo> indexFiles;
   Error error = scanFiles(FileInfo(crossrefDir), options, &indexFiles);
   if (error)
   {
      LOG_ERROR(error);
      return json::Array();
   }

   // now read the indexes
   json::Array projectXRefs;
   for (auto indexFile : indexFiles)
   {
      FilePath indexFilePath(indexFile.absolutePath());
      std::string projRelative = indexFilePath.getRelativePath(crossrefDir);
      json::Array xrefs = readProjectXRefIndex(FilePath(indexFile.absolutePath()), projRelative);
      std::copy(xrefs.begin(), xrefs.end(), std::back_inserter(projectXRefs));
   }

   return projectXRefs;
}

} // anonymous namespace

namespace modules {
namespace quarto {
namespace xrefs {

namespace {


Error xrefIndexForFile(const FilePath& filePath, json::Object& indexJson)
{
   indexJson[kRefs] = json::Array();

   // is this file in a project and is it a book project?
   FilePath projectDir;
   bool isBook = false;
   FilePath projectConfig = quartoProjectConfigFile(filePath);
   if (!projectConfig.isEmpty())
   {
      // set base dir
      projectDir = projectConfig.getParent();
      indexJson[kBaseDir] = createAliasedPath(projectDir);

      // check whether this is a booo short circuit for this being in the current project
      // (since we already have the config)
      if (isFileInSessionQuartoProject(filePath))
      {
         isBook = quartoConfig().project_type == kQuartoProjectBook;
      }
      else
      {
         std::string type;
         readQuartoProjectConfig(projectConfig, &type);
         isBook = type == kQuartoProjectBook;
      }

      // books get the entire index, non-books get just the file
      if (isBook)
      {
         indexJson[kRefs] = readAllProjectXRefIndexes(projectDir);
      }
      else
      {
         indexJson[kRefs] = readProjectXRefIndex(projectDir, filePath);
      }
   }
   else
   {
      // basedir is this file's parent dir
      indexJson[kBaseDir] = createAliasedPath(filePath.getParent());

      // get storage for this file
      FilePath indexPath;
      Error error = perFilePathStorage(kQuartoCrossrefScope, filePath, false, &indexPath);
      if (error)
      {
         LOG_ERROR(error);
         return error;
      }
      if (indexPath.exists())
      {
         indexJson[kRefs] = readXRefIndex(indexPath, filePath.getFilename());
      }
   }
   return Success();
}


Error quartoXRefIndexForFile(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // read params
   std::string file;
   Error error = json::readParams(request.params, &file);
   if (error)
      return error;

   // resolve path
   FilePath filePath = resolveAliasedPath(file);

   // read index
   json::Object indexJson;
   error = xrefIndexForFile(filePath, indexJson);
   if (error)
      return error;

   // return success
   pResponse->setResult(indexJson);
   return Success();
}

Error quartoXRefForId(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   // read params
   std::string file, id;
   Error error = json::readParams(request.params, &file, &id);
   if (error)
      return error;

   // resolve path
   FilePath filePath = resolveAliasedPath(file);

   // read index
   json::Object indexJson;
   error = xrefIndexForFile(filePath, indexJson);
   if (error)
      return error;

   // search it the id
   const json::Array& xrefs = indexJson[kRefs].getArray();
   auto it = std::find_if(xrefs.begin(), xrefs.end(), [&id](const json::Value& xref) {
      json::Object xrefJson = xref.getObject();
      std::string xrefId = xrefJson[kType].getString() + "-" +
                           xrefJson[kId].getString() +
                           xrefJson[kSuffix].getString();
      return xrefId == id;
   });
   if (it != xrefs.end())
   {
      json::Array xrefArray;
      xrefArray.push_back(*it);
      indexJson[kRefs] = xrefArray;
   }
   else
   {
      indexJson[kRefs] = json::Array();
   }

   pResponse->setResult(indexJson);

   return Success();
}

} // anonymous namespace

Error initialize()
{
   // register rpc functions
   ExecBlock initBlock;
   initBlock.addFunctions()
     (boost::bind(registerRpcMethod, "quarto_xref_index_for_file", quartoXRefIndexForFile))
     (boost::bind(registerRpcMethod, "quarto_xref_for_id", quartoXRefForId))
   ;
   return initBlock.execute();
}

} // namespace xrefs
} // namespace quarto
} // namespace modules

namespace quarto {

core::json::Value quartoXRefIndex()
{
   QuartoConfig config = quarto::quartoConfig();
   if (config.is_project)
   {
      json::Object indexJson;
      indexJson[kBaseDir] = config.project_dir;
      indexJson[kRefs] =  readAllProjectXRefIndexes(
         module_context::resolveAliasedPath(config.project_dir)
      );
      json::Value resultValue = indexJson;
      return resultValue;
   }
   else
   {
      return json::Value();
   }
}

} // namespace quarto

} // namespace session
} // namespace rstudio
