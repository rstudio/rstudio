/*
 * SessionPanmirrorPandoc.cpp
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

#include "SessionPanmirrorPandoc.hpp"


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
namespace pandoc {

namespace {

Error readOptionsParam(const json::Array& options, std::vector<std::string>* pOptions)
{
   for(json::Array::Iterator
         it = options.begin();
         it != options.end();
         ++it)
   {
      if ((*it).getType() != json::Type::STRING)
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);

      std::string option = (*it).getString() ;
      pOptions->push_back(option);
   }
   return Success();
}

void endAstToMarkdown(const json::JsonRpcFunctionContinuation& cont,
                      const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;
   if (result.exitStatus == EXIT_SUCCESS)
   {
      response.setResult(result.stdOut);
   }
   else
   {
      json::setProcessErrorResponse(result, ERROR_LOCATION, &response);
   }
   cont(Success(), &response);
}

void pandocAstToMarkdown(const json::JsonRpcRequest& request,
                         const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract params
   json::Object jsonAst;
   std::string format;
   json::Array jsonOptions;
   Error error = json::readParams(request.params, &jsonAst, &format, &jsonOptions);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   std::vector<std::string> options;
   error = readOptionsParam(jsonOptions, &options);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   // build args
   std::vector<std::string> args;
   args.push_back("--from");
   args.push_back("json");
   args.push_back("--to");
   args.push_back(format);
   std::copy(options.begin(), options.end(), std::back_inserter(args));

   // run pandoc (async)
   error = module_context::runPandocAsync(args, jsonAst.write(), boost::bind(endAstToMarkdown, cont, _1));
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
   }
}

template <typename T>
bool readJsonValue(const std::string& output, T* pVal, json::JsonRpcResponse* pResponse)
{
   using namespace json;
   T jsonValue;
   Error error = jsonValue.parse(output);
   if (error)
   {
      Error parseError(boost::system::errc::state_not_recoverable,
                       errorMessage(error),
                       ERROR_LOCATION);
      json::setErrorResponse(parseError, pResponse);
      return false;
   }
   else if (!isType<T>(jsonValue))
   {
      Error outputError(boost::system::errc::state_not_recoverable,
                       "Unexpected JSON output from pandoc",
                       ERROR_LOCATION);
      json::setErrorResponse(outputError, pResponse);
      return false;
   }
   else
   {
      *pVal = jsonValue;
      return true;
   }
}


void endJsonObjectRequest(const json::JsonRpcFunctionContinuation& cont,
                          const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;
   if (result.exitStatus == EXIT_SUCCESS)
   {
      json::Object jsonObject;
      if (readJsonValue(result.stdOut, &jsonObject, &response))
        response.setResult(jsonObject);
   }
   else
   {
      json::setProcessErrorResponse(result, ERROR_LOCATION, &response);
   }
   cont(Success(), &response);
}

void pandocMarkdownToAst(const json::JsonRpcRequest& request,
                         const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract params
   json::Array jsonOptions;
   std::string markdown, format;
   Error error = json::readParams(request.params, &markdown, &format, &jsonOptions);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }
   std::vector<std::string> options;
   error = readOptionsParam(jsonOptions, &options);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   // build args
   std::vector<std::string> args;
   args.push_back("--from");
   args.push_back(format);
   args.push_back("--to");
   args.push_back("json");
   std::copy(options.begin(), options.end(), std::back_inserter(args));

   // run pandoc
   core::system::ProcessResult result;
   error = module_context::runPandocAsync(args, markdown, boost::bind(endJsonObjectRequest, cont, _1));
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }
}


bool pandocCaptureOutput(const std::vector<std::string>& args,
                         const std::string& input,
                         std::string* pOutput,
                         json::JsonRpcResponse* pResponse)
{
   // run pandoc
   core::system::ProcessResult result;
   Error error = module_context::runPandoc(args, input, &result);
   if (error)
   {
      json::setErrorResponse(error, pResponse);
      return false;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      json::setProcessErrorResponse(result, ERROR_LOCATION, pResponse);
      return false;
   }
   else
   {
      *pOutput = result.stdOut;
      return true;
   }
}

bool pandocCaptureOutput(const std::string& arg, std::string* pOutput, json::JsonRpcResponse* pResponse)
{
   std::vector<std::string> args;
   args.push_back(arg);
   return pandocCaptureOutput(args, "", pOutput, pResponse);
}

void pandocGetCapabilities(const json::JsonRpcRequest&,
                           const json::JsonRpcFunctionContinuation& cont)
{

   // response object
   json::JsonRpcResponse response;

   // version
   std::string version;
   if (!pandocCaptureOutput("--version", &version, &response))
   {
      cont(Success(), &response);
      return;
   }

   // try for hit from cache of capabilities by version
   static std::map<std::string,json::Object> s_capabilitiesCache;
   std::map<std::string,json::Object>::const_iterator it = s_capabilitiesCache.find(version);
   if (it != s_capabilitiesCache.end())
   {
      response.setResult(it->second);
      cont(Success(), &response);
      return;
   }

   // api version
   std::vector<std::string> apiArgs;
   apiArgs.push_back("--to");
   apiArgs.push_back("json");
   std::string apiOutput;
   if (!pandocCaptureOutput(apiArgs, " ", &apiOutput, &response))
   {
      cont(Success(), &response);
      return;
   }
   json::Object jsonAst;
   if (!readJsonValue(apiOutput, &jsonAst, &response))
   {
      cont(Success(), &response);
      return;
   }

   // output formats
   json::Array apiVersion = jsonAst["pandoc-api-version"].getArray();
   std::string outputFormats;
   if (!pandocCaptureOutput("--list-output-formats", &outputFormats, &response))
   {
      cont(Success(), &response);
      return;
   }

   // highlight languages
   std::string highlightLanguages;
   if (!pandocCaptureOutput("--list-highlight-languages", &highlightLanguages, &response))
   {
      cont(Success(), &response);
      return;
   }

   // build capabilities response
   json::Object capabilities;
   capabilities["version"] = version;
   capabilities["api_version"] = apiVersion;
   capabilities["output_formats"] = outputFormats;
   capabilities["highlight_languages"] = highlightLanguages;

   // cache by version
   s_capabilitiesCache[version] = capabilities;

   // set response
   response.setResult(capabilities);
   cont(Success(), &response);
}


void pandocListExtensions(const json::JsonRpcRequest& request,
                          const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract format
   std::string format;
   Error error = json::readParams(request.params, &format);
   if (error)
   {
     json::setErrorResponse(error, &response);
     cont(Success(), &response);
     return;
   }

   // build arg
   std::string arg =  "--list-extensions";
   if (!format.empty())
      arg += ('=' + format);


   std::string extensions;
   if (pandocCaptureOutput(arg, &extensions, &response))
   {
      response.setResult(extensions);
      cont(Success(), &response);
   }

}

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
      if (readJsonValue(result.stdOut, &jsonCitations, &response))
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
      (boost::bind(module_context::registerAsyncRpcMethod, "pandoc_get_capabilities", pandocGetCapabilities))
      (boost::bind(module_context::registerAsyncRpcMethod, "pandoc_ast_to_markdown", pandocAstToMarkdown))
      (boost::bind(module_context::registerAsyncRpcMethod, "pandoc_markdown_to_ast", pandocMarkdownToAst))
      (boost::bind(module_context::registerAsyncRpcMethod, "pandoc_list_extensions", pandocListExtensions))
      (boost::bind(module_context::registerAsyncRpcMethod, "pandoc_get_bibliography", pandocGetBibliography))
   ;
   return initBlock.execute();
}


} // end namespace pandoc
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
