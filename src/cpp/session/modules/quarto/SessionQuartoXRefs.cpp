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
#include <core/system/Process.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/IncrementalFileChangeHandler.hpp>

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

json::Array sourceDatabaseXRefIndex(const FilePath& srcPath, const std::string& filename)
{
   QuartoConfig config = quartoConfig();

   static FilePath xrefIndexingDir;
   if (xrefIndexingDir.isEmpty())
   {
      // generate and create dir
      xrefIndexingDir = module_context::tempDir();
      Error error = xrefIndexingDir.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
         return json::Array();
      }

      // write defaults file with filters
      FilePath defaultsFile = xrefIndexingDir.completePath("defaults.yml");
      FilePath resourcesPath(config.resources_path);
      FilePath filtersPath = resourcesPath.completePath("filters");
      boost::format fmt("filters:\n  - %1%\n  - %2%\n");
      std::string defaults = boost::str(fmt %
         string_utils::utf8ToSystem(filtersPath.completePath("init/init.lua").getAbsolutePath()) %
         string_utils::utf8ToSystem(filtersPath.completePath("crossref/crossref.lua").getAbsolutePath())
      );
      error = core::writeStringToFile(defaultsFile, defaults);
      if (error)
      {
         LOG_ERROR(error);
         return json::Array();
      }
   }

   json::Array xrefIndex;
   std::string id;
   Error error = source_database::getId(srcPath, &id);
   if (!error)
   {
      boost::shared_ptr<source_database::SourceDocument> pDoc(new source_database::SourceDocument());
      Error error = source_database::get(id, pDoc);
      if (!error)
      {
         // create index
         core::system::ProcessOptions options;
         options.workingDir = xrefIndexingDir;
         core::system::Options env;
         core::system::environment(&env);
         core::system::setenv(&env,
            "QUARTO_FILTER_PARAMS",
            "{ \"crossref-index-file\": \"index.json\", \"crossref-input-type\": \"qmd\" }"
         );
         options.environment = env;
         std::vector<std::string> args;
         args.push_back("--from");
         args.push_back("markdown");
         args.push_back("--to");
         args.push_back("native");
         args.push_back("--defaults");
         args.push_back("defaults.yml");
         core::system::ProcessResult result;
         error = module_context::runPandoc(
            FilePath(config.bin_path).completePath("pandoc").getAbsolutePath(),
            args,
            pDoc->contents(),
            options,
            &result
         );
         if (!error)
         {
            if (result.exitStatus == EXIT_SUCCESS)
            {
               return readXRefIndex(FilePath(xrefIndexingDir).completePath("index.json"), filename);
            }
            else
            {
               LOG_ERROR_MESSAGE(result.stdErr);
            }
         }
         else
         {
            LOG_ERROR(error);
         }

      }
      else
      {
         LOG_ERROR(error);
      }
   }
   else if (error != core::systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation()))
   {
      LOG_ERROR(error);
   }


   return xrefIndex;
}

void onSourceDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore if the file doesn't have a path
   if (pDoc->path().empty())
      return;

   // update unsaved
   FileInfo fileInfo(module_context::resolveAliasedPath(pDoc->path()));


}

void onSourceDocRemoved(const std::string&, const std::string& path)
{
   // ignore if the file has no path
   if (path.empty())
      return;

   // remove from unsaved if it's a bookdown rmd
   FileInfo fileInfo(module_context::resolveAliasedPath(path));

}

void onAllSourceDocsRemoved()
{

}


void onDeferredInit(bool)
{
   // book vs. standalone doc (bookdown doesn't do any source db monitoring
   // for standalone files -- possibly b/c crossrefs aren't supported in
   // standalone files -- need some resolution for this

   // TODO: need to check whether this is a quarto document or not
   // (or if its already in) and in that case keep an in-memory
   // cache of crossrefs for that document. should do this work
   // either incrementally or asynchronously (or both)

   // could also consider doing the whole file monitoring bit
   // and keeping the index on disk (incremental file change handler)


   // index docs
   std::vector<boost::shared_ptr<source_database::SourceDocument> > pDocs;
   Error error = source_database::list(&pDocs);
   if (error)
      LOG_ERROR(error);
   std::for_each(pDocs.begin(), pDocs.end(), onSourceDocUpdated);

   // hookup source doc events
   source_database::events().onDocUpdated.connect(onSourceDocUpdated);
   source_database::events().onDocRemoved.connect(onSourceDocRemoved);
   source_database::events().onRemoveAll.connect(onAllSourceDocsRemoved);

   // here is where bookdown had a file monitor that crawled everything
   // incrementally at the outset (which allowed it to only care about
   // unsaved files in the editor
}



json::Array readXRefIndex(const FilePath& indexPath, const FilePath& srcPath, const std::string& filename)
{
   // read the xref index we have on disk
   json::Array xrefs = readXRefIndex(indexPath, filename);
   if (xrefs.getSize() == 0)
      return xrefs;

   // check for xrefs in the source database that we don't know about
   // TODO: merging/replacement strategy
   json::Array srcDBIndex = sourceDatabaseXRefIndex(srcPath, filename);

   return xrefs;
}

json::Array readProjectXRefIndex(const FilePath& indexPath, const FilePath& srcPath, std::string filename)
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
         return readXRefIndex(mostRecentIndex, srcPath, filename);
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
   return readProjectXRefIndex(indexPath, srcFile, projRelative);

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
      json::Array xrefs = readProjectXRefIndex(FilePath(indexFile.absolutePath()),
                                               projectDir.completeChildPath(projRelative),
                                               projRelative);
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
         indexJson[kRefs] = readXRefIndex(indexPath, filePath, filePath.getFilename());
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
   // deferred init (build xref file index)
   module_context::events().onDeferredInit.connect(onDeferredInit);


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
