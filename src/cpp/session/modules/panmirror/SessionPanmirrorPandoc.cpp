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
#include <core/Algorithm.hpp>
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

Error readOptionsParam(const json::Array& options, std::vector<std::string>* pOptions)
{
   for(json::Array::Iterator
         it = options.begin();
         it != options.end();
         ++it)
   {
      if ((*it).getType() != json::Type::STRING)
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);

      std::string option = (*it).getString();
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

void endHeadingIds(json::Object astJson,
                   const core::system::ProcessResult& result,
                   const json::JsonRpcFunctionContinuation& cont)
{
   json::JsonRpcResponse response;
   if (result.exitStatus == EXIT_SUCCESS)
   {
      std::vector<std::string> lines;
      boost::algorithm::split(lines, result.stdOut, boost::algorithm::is_any_of("\n\r"));
      json::Array jsonHeadingsIds;
      std::for_each(lines.begin(), lines.end(), [&jsonHeadingsIds](std::string line) {
         boost::algorithm::trim(line);
         if (!line.empty())
            jsonHeadingsIds.push_back(line);
      });
      astJson["heading_ids"] = jsonHeadingsIds;
      response.setResult(astJson);
      cont(Success(), &response);
   }
   else
   {
      json::setProcessErrorResponse(result, ERROR_LOCATION, &response);
      cont(Success(), &response);
   }
}

void endMarkdownToAst(std::string markdown,
                      std::string format,
                      const core::system::ProcessResult& result,
                      const json::JsonRpcFunctionContinuation& cont)
{
   json::JsonRpcResponse response;
   if (result.exitStatus == EXIT_SUCCESS)
   {
      json::Object jsonObject;
      if (json::parseJsonForResponse(result.stdOut, &jsonObject, &response))
      {
         // got ast, now extract heading ids

         // disable auto identifiers so we can discover *only* explicit ids
         format += "-auto_identifiers-gfm_auto_identifiers";

         // path to lua filter
         FilePath resPath = session::options().rResourcesPath();
         FilePath headingIdsLuaPath = resPath.completePath("heading_ids.lua");
         std::string headingIdsLua = string_utils::utf8ToSystem(headingIdsLuaPath.getAbsolutePath());

         // build args
         std::vector<std::string> args;
         args.push_back("--from");
         args.push_back(format);
         args.push_back("--to");
         args.push_back(headingIdsLua);

         // run pandoc
         core::system::ProcessResult result;
         Error error = module_context::runPandocAsync(args, markdown, boost::bind(endHeadingIds, jsonObject, _1, cont));
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
   error = module_context::runPandocAsync(args, markdown, boost::bind(endMarkdownToAst, markdown, format, _1, cont));
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
   if (!json::parseJsonForResponse(apiOutput, &jsonAst, &response))
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

   }
   cont(Success(), &response);
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
   ;
   return initBlock.execute();
}


} // end namespace pandoc
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
