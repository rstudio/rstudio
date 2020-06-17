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

std::string pandocPath()
{
#ifndef WIN32
   std::string pandoc = "pandoc";
#else
   std::string pandoc = "pandoc.exe";
#endif
  FilePath pandocPath = FilePath(core::system::getenv("RSTUDIO_PANDOC")).completeChildPath(pandoc);
  return string_utils::utf8ToSystem(pandocPath.getAbsolutePath());
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

Error runPandocAsync(const std::vector<std::string>& args,
                     const std::string&input,
                     const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   core::system::ProcessOptions options;
   options.terminateChildren = true;

   return module_context::processSupervisor().runProgram(
      pandocPath(),
      args,
      input,
      pandocOptions(),
      onCompleted
   );
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
      setProcessErrorResponse(result, ERROR_LOCATION, &response);
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
      setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   std::vector<std::string> options;
   error = readOptionsParam(jsonOptions, &options);
   if (error)
   {
      setErrorResponse(error, &response);
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
      setErrorResponse(error, &response);
      cont(Success(), &response);
   }
}

bool readJsonAst(const std::string& output, json::Object* pAst, json::JsonRpcResponse* pResponse)
{
   using namespace json;
   json::Value jsonAst;
   Error error = jsonAst.parse(output);
   if (error)
   {
      Error parseError(boost::system::errc::state_not_recoverable,
                       errorMessage(error),
                       ERROR_LOCATION);
      setErrorResponse(parseError, pResponse);
      return false;
   }
   else if (!isType<json::Object>(jsonAst))
   {
      Error outputError(boost::system::errc::state_not_recoverable,
                        "Unexpected JSON output from pandoc",
                        ERROR_LOCATION);
      setErrorResponse(outputError, pResponse);
      return false;
   }
   else
   {
      *pAst = jsonAst.getObject();
      return true;
   }
}

void endMarkdownToAst(const json::JsonRpcFunctionContinuation& cont,
                      const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;
   if (result.exitStatus == EXIT_SUCCESS)
   {
      json::Object jsonAst;
      if (readJsonAst(result.stdOut, &jsonAst, &response))
        response.setResult(jsonAst);
   }
   else
   {
      setProcessErrorResponse(result, ERROR_LOCATION, &response);
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
      setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }
   std::vector<std::string> options;
   error = readOptionsParam(jsonOptions, &options);
   if (error)
   {
      setErrorResponse(error, &response);
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
   error = runPandocAsync(args, markdown, boost::bind(endMarkdownToAst, cont, _1));
   if (error)
   {
      setErrorResponse(error, &response);
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
      setErrorResponse(error, pResponse);
      return false;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      setProcessErrorResponse(result, ERROR_LOCATION, pResponse);
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
   if (!readJsonAst(apiOutput, &jsonAst, &response))
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
     setErrorResponse(error, &response);
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
