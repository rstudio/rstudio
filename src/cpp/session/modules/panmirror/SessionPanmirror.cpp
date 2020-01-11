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

// "pandoc_ast_to_markdown";
// "pandoc_markdown_to_ast";
// "pandoc_list_extensions";

namespace {


Error runPandoc(const std::vector<std::string>& args, const std::string& input, core::system::ProcessResult* pResult)
{
   FilePath pandocPath = FilePath(core::system::getenv("RSTUDIO_PANDOC")).completeChildPath("pandoc");
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

void setPandocErrorResponse(const Error& error,
                            json::JsonRpcResponse* pResponse)
{
   LOG_ERROR(error);
   pResponse->setError(error);
}

void setPandocErrorResponse(const core::system::ProcessResult& result,
                            json::JsonRpcResponse* pResponse)
{
   const std::string errMsg = "Error executing pandoc: " + result.stdErr;
   Error error(json::errc::ExecutionError, errMsg, ERROR_LOCATION);
   setPandocErrorResponse(error, pResponse);
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
      return error;
   std::vector<std::string> options;
   error = readOptionsParam(jsonOptions, &options);
   if (error)
      return error;

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
      return error;

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

Error pandocMarkdownToAst(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // extract params
   json::Array jsonOptions;
   std::string markdown, format;
   Error error = json::readParams(request.params, &markdown, &format, &jsonOptions);
   if (error)
      return error;
   std::vector<std::string> options;
   error = readOptionsParam(jsonOptions, &options);
   if (error)
      return error;

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
      return error;

   // return output on success
   if (result.exitStatus == EXIT_SUCCESS)
   {
      using namespace json;
      json::Value jsonAst;
      Error parseError = jsonAst.parse(result.stdOut);
      if (parseError)
      {
         setPandocErrorResponse(parseError, pResponse);
      }
      else if (!isType<json::Object>(jsonAst))
      {
         Error outputError(json::errc::ExecutionError,
                           "Unexpected JSON output from pandoc",
                           ERROR_LOCATION);
         setPandocErrorResponse(outputError, pResponse);
      }
      else
      {
         pResponse->setResult(jsonAst);
      }
   }
   else
   {
      setPandocErrorResponse(result, pResponse);
   }

   return Success();
}

Error pandocListExtensions(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   // extract format
   std::string format;
   Error error = json::readParams(request.params, &format);
   if (error)
      return error;

   // build args
   std::vector<std::string> args;
   std::string extensions =  "--list-extensions";
   if (!format.empty())
      extensions += ('=' + format);
   args.push_back(extensions);

   // run pandoc
   core::system::ProcessResult result;
   error = runPandoc(args, "", &result);
   if (error)
      return error;

   // return output on success
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



} // end anonymous namespace

Error initialize()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(module_context::registerRpcMethod, "pandoc_ast_to_markdown", pandocAstToMarkdown))
      (bind(module_context::registerRpcMethod, "pandoc_markdown_to_ast", pandocMarkdownToAst))
      (bind(module_context::registerRpcMethod, "pandoc_list_extensions", pandocListExtensions))
   ;
   return initBlock.execute();
}

} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
