/*
 * SessionPanmirrorBibliography.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "SessionPanmirrorBibliogrpahy.hpp"


#include <shared_core/Error.hpp>
#include <core/Hash.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/StringUtils.hpp>

#include <core/system/Process.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace bibliography {

namespace {

// cache the last bibliography we returned (along w/ enough info to construct an etag for the cache)
class BiblioCache
{
public:
   static std::string etag(const std::vector<core::FileInfo>& biblioFiles,
                           const std::string& refBlock)
   {
      std::ostringstream ostr;
      for (auto file : biblioFiles)
          ostr << file.absolutePath() << ":" << file.lastWriteTime();
      ostr << hash::crc32HexHash(refBlock);
      return ostr.str();
   }

public:
   void update(const json::Object& biblioJson,
               const std::vector<core::FileInfo>& biblioFiles,
               const std::string& refBlock)
   {
      biblioJson_ = biblioJson;
      biblioFiles_ = biblioFiles;
      refBlock_ = refBlock;
   }

   std::string etag()
   {
      return etag(biblioFiles_, refBlock_);
   }

   void setResponse(json::JsonRpcResponse* pResponse)
   {
      json::Object result;
      result["etag"] = etag();
      result["bibliography"] = biblioJson_;
      pResponse->setResult(result);
   }

private:
   json::Object biblioJson_;
   std::vector<core::FileInfo> biblioFiles_;
   std::string refBlock_;
};
BiblioCache s_biblioCache;


void citeprocCompleted(const std::vector<core::FileInfo>& biblioFiles,
                       const std::string& refBlock,
                       const json::JsonRpcFunctionContinuation& cont,
                       const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;
   if (result.exitStatus == EXIT_SUCCESS)
   {
      json::Array jsonCitations;
      if (json::parseJsonForResponse(result.stdOut, &jsonCitations, &response))
      {
         // create bibliography
         json::Object biblio;
         biblio["sources"] = jsonCitations;

         // cache last successful bibliograpy
         s_biblioCache.update(biblio, biblioFiles, refBlock);

         // set response
         s_biblioCache.setResponse(&response);
      }
   }
   else
   {
      json::setProcessErrorResponse(result, ERROR_LOCATION, &response);
   }

   // call continuation
   cont(Success(), &response);
}

void pandocGetBibliography(const json::JsonRpcRequest& request,
                           const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract params
   std::string file, refBlock, etag;
   json::Array bibliographiesJson;
   Error error = json::readParams(request.params, &file, &bibliographiesJson, &refBlock, &etag);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }




   // determine biblio files
   std::vector<FileInfo> biblioFiles;

   // if there are bibliographies passed form the client then use those in preference to the
   // project bibliographies (b/c they will appear after the project bibliographies)
   if (bibliographiesJson.getSize() > 0)
   {
      std::vector<std::string> biblios;
      bibliographiesJson.toVectorString(biblios);
      for (auto biblio : biblios)
      {
         FilePath biblioPath = module_context::resolveAliasedPath(biblio);
         biblioFiles.push_back(FileInfo(biblioPath));
      }
   }
   // is this file part of the current project? if so then use the project bibliographies as the default
   else if (!file.empty() && projects::projectContext().hasProject())
   {
      FilePath filePath = module_context::resolveAliasedPath(file);
      if (filePath.isWithin(projects::projectContext().buildTargetPath()))
      {
         std::vector<FilePath> projectBibs = module_context::bookdownBibliographies();
         std::transform(projectBibs.begin(),
                        projectBibs.end(),
                        std::back_inserter(biblioFiles),
                        toFileInfo);
      }
   }

   // if the client, the filesystem, and the cache all agree on the etag then serve from cache
   if (etag == s_biblioCache.etag() && etag == BiblioCache::etag(biblioFiles, refBlock))
   {
      s_biblioCache.setResponse(&response);
      cont(Success(), &response);
      return;
   }

   // build args
   std::vector<std::string> args;

   // always pass the biblio files
   for (auto biblioFile : biblioFiles)
      args.push_back(string_utils::utf8ToSystem(biblioFile.absolutePath()));

   // if a ref block is provided then write it to a temporary file and pass it as well
   if (!refBlock.empty())
   {
      FilePath tempYaml = module_context::tempFile("biblio", "yaml");
      Error error = writeStringToFile(tempYaml, refBlock);
      if (error)
         LOG_ERROR(error);
      args.push_back(string_utils::utf8ToSystem(tempYaml.getAbsolutePath()));
   }

   // convert to json
   args.push_back("--bib2json");

   // run pandoc-citeproc
   core::system::ProcessResult result;
   error = module_context::runPandocCiteprocAsync(args, "", boost::bind(citeprocCompleted, biblioFiles, refBlock, cont, _1));
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
   }
}


} // end anonymous namespace

Error initialize()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
        (boost::bind(module_context::registerAsyncRpcMethod, "pandoc_get_bibliography", pandocGetBibliography))
   ;
   return initBlock.execute();
}


} // end namespace bibliography
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
