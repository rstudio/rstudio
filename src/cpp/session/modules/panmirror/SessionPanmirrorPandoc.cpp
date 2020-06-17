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
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/StringUtils.hpp>

#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace pandoc {

namespace {

std::string pandocBinary(const std::string& binary)
{
#ifndef WIN32
   std::string target = binary;
#else
   std::string target = binary + ".exe";
#endif
  FilePath pandocPath = FilePath(core::system::getenv("RSTUDIO_PANDOC")).completeChildPath(target);
  return string_utils::utf8ToSystem(pandocPath.getAbsolutePath());
}

std::string pandocPath()
{
   return pandocBinary("pandoc");
}

std::string pandocCiteprocPath()
{
   return pandocBinary("pandoc-citeproc");
}

std::string resolvePandocInputFile(const std::string &file)
{
  const FilePath filePath = module_context::resolveAliasedPath(file);
  return string_utils::utf8ToSystem(filePath.getAbsolutePath());
}

core::system::ProcessOptions pandocOptions()
{
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   return options;
}

Error runPandoc(const std::vector<std::string>& args, const std::string& input, core::system::ProcessResult* pResult)
{
   core::system::ProcessOptions options;
   options.terminateChildren = true;

   return core::system::runProgram(
      pandocPath(),
      args,
      input,
      pandocOptions(),
      pResult
   );
}

Error runAsync(const std::string& executablePath,
                     const std::vector<std::string>& args,
                     const std::string&input,
                     const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   core::system::ProcessOptions options;
   options.terminateChildren = true;

   return module_context::processSupervisor().runProgram(
      executablePath,
      args,
      input,
      pandocOptions(),
      onCompleted
   );
}

Error runPandocAsync(const std::vector<std::string>& args,
                     const std::string&input,
                     const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   return runAsync(pandocPath(), args, input, onCompleted);
}

Error runPandocCiteprocAsync(const std::vector<std::string>& args,
                             const std::string&input,
                             const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   return runAsync(pandocCiteprocPath(), args, input, onCompleted);
}

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
   error = runPandocAsync(args, jsonAst.write(), boost::bind(endAstToMarkdown, cont, _1));
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
   error = runPandocAsync(args, markdown, boost::bind(endJsonObjectRequest, cont, _1));
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
   Error error = runPandoc(args, input, &result);
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

std::string readCitationId(const json::Value& jsonValue)
{
   if (jsonValue.isObject())
   {
      std::string id;
      Error error = json::readObject(jsonValue.getObject(), "id", id);
      if (error)
         LOG_ERROR(error);
      return id;
   }
   return "";
}


// cache the last bibliography we returned along w/ the timestamps of the bibliography and csl file
class BiblioCache
{
public:
   static std::string etag(const std::string& biblio, const std::string& csl)
   {
      return etag(fileInfo(biblio), fileInfo(csl));
   }

public:
   void update(const json::Object& biblioJson, const std::string& biblio, const std::string& csl)
   {
      biblioJson_ = biblioJson;
      biblioFile_ = fileInfo(biblio);
      if (!csl.empty())
         cslFile_ = fileInfo(csl);
      else
         cslFile_ = core::FileInfo();
   }

   std::string etag()
   {
      std::ostringstream ostr;
      ostr << biblioFile_.absolutePath() << ":" << biblioFile_.lastWriteTime();
      ostr << cslFile_.absolutePath() << ":" << cslFile_.lastWriteTime();
      return ostr.str();
   }

   void setResponse(json::JsonRpcResponse* pResponse)
   {
      json::Object result;
      result["etag"] = etag();
      result["bibliography"] = biblioJson_;
      pResponse->setResult(result);
   }

private:

   static std::string etag(const FileInfo& biblio, const FileInfo& csl)
   {
      std::ostringstream ostr;
      ostr << biblio.absolutePath() << ":" << biblio.lastWriteTime();
      ostr << csl.absolutePath() << ":" << csl.lastWriteTime();
      return ostr.str();
   }

   static core::FileInfo fileInfo(const std::string& file)
   {
      return file.empty() ? FileInfo() : FileInfo(module_context::resolveAliasedPath(file));
   }

private:
   json::Object biblioJson_;
   core::FileInfo biblioFile_;
   core::FileInfo cslFile_;
};
BiblioCache s_biblioCache;


void pandocBiblioCompleted(const std::string& file,
                           const std::string& csl,
                           const json::Array& jsonCitations,
                           const json::JsonRpcFunctionContinuation& cont,
                           const core::system::ProcessResult& result)
{   
   json::JsonRpcResponse response;

   // read the html
   if (result.exitStatus == EXIT_SUCCESS)
   {
      // create bibliography
      json::Object biblio;
      biblio["sources"] = jsonCitations;
      biblio["html"] = result.stdOut;

      // cache last successful bibliograpy
      s_biblioCache.update(biblio, file, csl);

      // set response
      s_biblioCache.setResponse(&response);
   }
   else
   {
      json::setProcessErrorResponse(result, ERROR_LOCATION, &response);
   }

   cont(Success(), &response);
}


void citeprocCompleted(const std::string& commandLine,
                       const std::string& file,
                       json::Array& refBlocks,
                       const std::string& csl,
                       const json::JsonRpcFunctionContinuation& cont,
                       const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;
   if (result.exitStatus == EXIT_SUCCESS)
   {
      json::Array jsonCitations;
      if (readJsonValue(result.stdOut, &jsonCitations, &response))
      {
         // get the ids
         std::vector<std::string> ids;
         std::transform(jsonCitations.begin(), jsonCitations.end(), std::back_inserter(ids), readCitationId);

         // build a document to send to pandoc
         std::vector<std::string> lines;
         lines.push_back("---");
         lines.push_back("bibliography: " + resolvePandocInputFile(file));
         if (!csl.empty())
            lines.push_back("csl: " + resolvePandocInputFile(csl));
         if (ids.size() > 0)
         {
            lines.push_back("nocite: |");
            std::string nocite = "  ";
            for (std::vector<std::string>::size_type i = 0; i<ids.size(); i++)
            {
               const std::string& id = ids[i];
               if (!id.empty())
               {
                  if (i>0)
                    nocite += ", ";
                  nocite += ("@" + id);
               }
            }
            lines.push_back(nocite);
         }
         lines.push_back("---");
          
         // TODO: include any refBlocks (also nocite)
         std::string doc = boost::algorithm::join(lines, "\n");

         // run pandoc
         std::vector<std::string> args;
     
         // If we've received a command line bibliography file, include it
         // in the args
         if (commandLine.size() > 0) {
             args.push_back("--bibliography");
             args.push_back(commandLine);
         }
                   
         args.push_back("--to");
         args.push_back("html");
         args.push_back("--filter");
         args.push_back(pandocCiteprocPath());
         Error error = runPandocAsync(args, doc, boost::bind(pandocBiblioCompleted, file, csl, jsonCitations, cont, _1));
         if (error)
         {
            json::setErrorResponse(error, &response);
            cont(Success(), &response);
         }
      }
      else
      {
         cont(Success(), &response);
      }
   }
   else
   {
      json::setProcessErrorResponse(result, ERROR_LOCATION, &response);
      cont(Success(), &response);
   }
}

void pandocGetBibliography(const json::JsonRpcRequest& request,
                           const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract params
   std::string commandLine, file, csl, etag;
   json::Array refBlocks;
   Error error = json::readParams(request.params, &commandLine, &file, &refBlocks, &csl, &etag);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   // if the client, the filesystem, and the cache all agree on the etag then serve from cache
   if (etag == s_biblioCache.etag() && etag == BiblioCache::etag(file, csl))
   {
      s_biblioCache.setResponse(&response);
      cont(Success(), &response);
      return;
   }

   // TODO: We could now call this without a bibliography file (either with a command line only)
   // or refBlocks only. Need to deal with that.
    
   // build args
   std::vector<std::string> args;
   const FilePath filePath = module_context::resolveAliasedPath(file);
   args.push_back(string_utils::utf8ToSystem(filePath.getAbsolutePath()));
   args.push_back("--bib2json");

   // run pandoc-citeproc
   core::system::ProcessResult result;
   error = runPandocCiteprocAsync(args, "", boost::bind(citeprocCompleted, commandLine, file, refBlocks, csl, cont, _1));
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
