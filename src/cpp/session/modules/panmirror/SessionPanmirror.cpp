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

Error runPandoc(const std::vector<std::string>& args, const std::string& input, core::system::ProcessResult* pResult)
{
#ifndef WIN32
   std::string pandoc = "pandoc";
#else
   std::string pandoc = "pandoc.exe";
#endif
   FilePath pandocPath = FilePath(core::system::getenv("RSTUDIO_PANDOC")).completeChildPath(pandoc);
   return core::system::runProgram(
      string_utils::utf8ToSystem(pandocPath.getAbsolutePath()),
      args,
      input,
      core::system::ProcessOptions(),
      pResult
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

Error pandocAstToMarkdown(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // extract params
   json::Object jsonAst;
   std::string format;
   json::Array jsonOptions;
   Error error = json::readParams(request.params, &jsonAst, &format, &jsonOptions);
   if (error)
   {
      setPandocErrorResponse(error, pResponse);
      return Success();
   }
   std::vector<std::string> options;
   error = readOptionsParam(jsonOptions, &options);
   if (error)
   {
      setPandocErrorResponse(error, pResponse);
      return Success();
   }

   // build args
   std::vector<std::string> args;
   args.push_back("--from");
   args.push_back("json");
   args.push_back("--to");
   args.push_back(format);
   std::copy(options.begin(), options.end(), std::back_inserter(args));

   // run pandoc
   core::system::ProcessResult result;
   error = runPandoc(args, jsonAst.write(), &result);
   if (error)
   {
      setPandocErrorResponse(error, pResponse);
      return Success();
   }

   if (result.exitStatus == EXIT_SUCCESS)
   {
      pResponse->setResult(result.stdOut);
   }
   else
   {
      setPandocErrorResponse(result, pResponse);
   }

   return Success();
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
      setPandocErrorResponse(parseError, pResponse);
      return false;
   }
   else if (!isType<json::Object>(jsonAst))
   {
      Error outputError(boost::system::errc::state_not_recoverable,
                        "Unexpected JSON output from pandoc",
                        ERROR_LOCATION);
      setPandocErrorResponse(outputError, pResponse);
      return false;
   }
   else
   {
      *pAst = jsonAst.getObject();
      return true;
   }
}

Error pandocMarkdownToAst(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // extract params
   json::Array jsonOptions;
   std::string markdown, format;
   Error error = json::readParams(request.params, &markdown, &format, &jsonOptions);
   if (error)
   {
      setPandocErrorResponse(error, pResponse);
      return Success();
   }
   std::vector<std::string> options;
   error = readOptionsParam(jsonOptions, &options);
   if (error)
   {
      setPandocErrorResponse(error, pResponse);
      return Success();
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
   error = runPandoc(args, markdown, &result);
   if (error)
   {
      setPandocErrorResponse(error, pResponse);
      return Success();
   }

   // return output on success
   if (result.exitStatus == EXIT_SUCCESS)
   {
      json::Object jsonAst;
      if (readJsonAst(result.stdOut, &jsonAst, pResponse))
        pResponse->setResult(jsonAst);
   }
   else
   {
      setPandocErrorResponse(result, pResponse);
   }

   return Success();
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

Error pandocGetCapabilities(const json::JsonRpcRequest&,
                            json::JsonRpcResponse* pResponse)
{
   std::string version;
   if (!pandocCaptureOutput("--version", &version, pResponse))
      return Success();

   std::vector<std::string> apiArgs;
   apiArgs.push_back("--to");
   apiArgs.push_back("json");
   std::string apiOutput;
   if (!pandocCaptureOutput(apiArgs, " ", &apiOutput, pResponse))
      return Success();
   json::Object jsonAst;
   if (!readJsonAst(apiOutput, &jsonAst, pResponse))
      return Success();
   json::Array apiVersion = jsonAst["pandoc-api-version"].getArray();

   std::string outputFormats;
   if (!pandocCaptureOutput("--list-output-formats", &outputFormats, pResponse))
      return Success();

   std::string highlightLanguages;
   if (!pandocCaptureOutput("--list-highlight-languages", &highlightLanguages, pResponse))
      return Success();

   json::Object capabilities;
   capabilities["version"] = version;
   capabilities["api_version"] = apiVersion;
   capabilities["output_formats"] = outputFormats;
   capabilities["highlight_languages"] = highlightLanguages;
   pResponse->setResult(capabilities);
   return Success();
}


Error pandocListExtensions(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   // extract format
   std::string format;
   Error error = json::readParams(request.params, &format);
   if (error)
   {
      setPandocErrorResponse(error, pResponse);
      return Success();
   }

   // build arg
   std::string arg =  "--list-extensions";
   if (!format.empty())
      arg += ('=' + format);


   std::string extensions;
   if (pandocCaptureOutput(arg, &extensions, pResponse))
   {
      pResponse->setResult(extensions);
   }
   return Success();
}



} // end anonymous namespace

Error initialize()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::registerRpcMethod, "pandoc_get_capabilities", pandocGetCapabilities))
      (boost::bind(module_context::registerRpcMethod, "pandoc_ast_to_markdown", pandocAstToMarkdown))
      (boost::bind(module_context::registerRpcMethod, "pandoc_markdown_to_ast", pandocMarkdownToAst))
      (boost::bind(module_context::registerRpcMethod, "pandoc_list_extensions", pandocListExtensions))
   ;
   return initBlock.execute();
}

} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
