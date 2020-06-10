/*
 * SessionPanmirror.cpp
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

#include "SessionPanmirror.hpp"


#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/StringUtils.hpp>

#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {

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

std::string errorMessage(const Error& error)
{
   std::string msg = error.getMessage();
   if (msg.length() == 0)
   {
      msg = error.getProperty("category");
   }
   if (msg.length() == 0)
   {
      msg = error.getName();
   }
   return msg;
}


void setPandocErrorResponse(const Error& error,
                            json::JsonRpcResponse* pResponse)
{
   LOG_ERROR(error);
   pResponse->setError(error, errorMessage(error));
}

void setPandocErrorResponse(const core::system::ProcessResult& result,
                            json::JsonRpcResponse* pResponse)
{
   Error error = systemError(boost::system::errc::state_not_recoverable, result.stdErr, ERROR_LOCATION);
   LOG_ERROR(error);
   pResponse->setError(error, result.stdErr);
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
      setPandocErrorResponse(result, &response);
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
      setPandocErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   std::vector<std::string> options;
   error = readOptionsParam(jsonOptions, &options);
   if (error)
   {
      setPandocErrorResponse(error, &response);
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
      setPandocErrorResponse(error, &response);
      cont(Success(), &response);
   }
}

bool readJsonValue(const std::string& output, json::Value* pVal, json::JsonRpcResponse* pResponse)
{
   using namespace json;
   json::Value jsonValue;
   Error error = jsonValue.parse(output);
   if (error)
   {
      Error parseError(boost::system::errc::state_not_recoverable,
                       errorMessage(error),
                       ERROR_LOCATION);
      setPandocErrorResponse(parseError, pResponse);
      return false;
   }
   else
   {
      *pVal = jsonValue;
      return true;
   }
}

void endJsonRequest(const json::JsonRpcFunctionContinuation& cont,
                    const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;
   if (result.exitStatus == EXIT_SUCCESS)
   {
      json::Object jsonValue;
      if (readJsonValue(result.stdOut, &jsonValue, &response))
        response.setResult(jsonValue);
   }
   else
   {
      setPandocErrorResponse(result, &response);
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
      setPandocErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }
   std::vector<std::string> options;
   error = readOptionsParam(jsonOptions, &options);
   if (error)
   {
      setPandocErrorResponse(error, &response);
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
   error = runPandocAsync(args, markdown, boost::bind(endJsonRequest, cont, _1));
   if (error)
   {
      setPandocErrorResponse(error, &response);
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
      setPandocErrorResponse(error, pResponse);
      return false;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      setPandocErrorResponse(result, pResponse);
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
     setPandocErrorResponse(error, &response);
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

void pandocGetBibliography(const json::JsonRpcRequest& request,
                           const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract params
   std::string file;
   Error error = json::readParams(request.params, &file);
   if (error)
   {
      setPandocErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   // build args
   std::vector<std::string> args;
   const FilePath filePath = module_context::resolveAliasedPath(file);
   args.push_back(string_utils::utf8ToSystem(filePath.getAbsolutePath()));
   args.push_back("--bib2json");

   // run pandoc-citeproc
   core::system::ProcessResult result;
   error = runPandocCiteprocAsync(args, "", boost::bind(endJsonRequest, cont, _1));
   if (error)
   {
      setPandocErrorResponse(error, &response);
      cont(Success(), &response);
      return;
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

} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
