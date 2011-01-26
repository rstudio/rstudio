/*
 * SessionDiff.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionDiff.hpp"

#include <boost/function.hpp>
#include <boost/format.hpp>
#include <boost/regex.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>

#include <core/http/Util.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace diff {

namespace {

const char * const kCaption = "caption";
const char * const kCommand = "command";
const char * const kTarget = "target";
const char * const kDiffView = "/diff/view";
const char * const kDiffViewRel = "diff/view";

bool getDiffViewParams(const http::Request& request,
                       http::Response* pResponse,
                       std::string* pCaption,
                       std::string* pCommand,
                       FilePath* pTargetPath)
{
   // determine caption
   *pCaption = request.queryParamValue(kCaption);
   if (pCaption->empty())
   {
      pResponse->setError(http::status::BadRequest, "caption not specified");
      return false;
   }

   // determine command
   *pCommand = request.queryParamValue(kCommand);
   if (pCommand->empty())
   {
      pResponse->setError(http::status::BadRequest, "command not specified");
      return false;
   }

   // determine target path (default to current path)
   std::string target = request.queryParamValue(kTarget);
   if (target.empty())
      *pTargetPath = module_context::safeCurrentPath();
   else
      *pTargetPath = module_context::resolveAliasedPath(target);

   // verify it exists
   if (!pTargetPath->exists())
   {
      pResponse->setError(http::status::BadRequest,  "target path does not exist");
      return false;
   }

   return true;
}

void handleDiffViewRequest(const http::Request& request, http::Response* pResponse)
{
   // get parameters
   std::string caption, command;
   FilePath targetPath;
   if (!getDiffViewParams(request, pResponse, &caption, &command, &targetPath))
      return;

   // switch to the target directory (but switch back before existing scope)
   RestoreCurrentPathScope restoreCurrentPath(module_context::safeCurrentPath());
   Error error = targetPath.makeCurrentPath();
   if (error)
   {
      pResponse->setError(error);
      return;
   }

   // execute the command and capture the diff
   std::string diff;
   error = core::system::captureCommand(command, &diff);
   if (error)
   {
      pResponse->setError(error);
      return;
   }

   diff = core::string_utils::textToHtml(diff);
   const boost::regex index("^(Index: [^\n]*\n=+)$");
   const boost::regex plus("^(\\+[^\n]*)$");
   const boost::regex minus("^(\\-[^\n]*)$");
   const boost::regex space("^( [^\n]*)$");
   const boost::regex group("^(@@[^\n]*)$");
   const boost::regex comment("^([@\\\\][^\n]*)$");
   diff = regex_replace(diff, index, "<div class=\"header proportional\">\\1</div>");
   diff = regex_replace(diff, plus, "<div class=\"added\">\\1</div>");
   diff = regex_replace(diff, minus, "<div class=\"deleted\">\\1</div>");
   diff = regex_replace(diff, space, "<div class=\"unchanged\">\\1</div>");
   diff = regex_replace(diff, group, "<div class=\"group\">\\1</div>");
   diff = regex_replace(diff, comment, "<div class=\"comment\">\\1</div>");

   // define html template
   boost::format htmlFmt(
      "<html>\n"
      "  <head>\n"
      "     <title>RStudio: %1%</title>\n"
      "     <script type='text/javascript' src='../js/diff.js'></script>\n"
      "     <style type='text/css'>\n"
      "     .proportional { font-family: Segoe UI, Lucida Grande, Verdana, Helvetica; }\n"
      "     body      { font-size: 12px; }\n"
      "     .header   { font-size: 14px; font-weight: bold; margin: 1.5em 0 0.5em -20pt }\n"
      "     .added    { background-color: #cfc; color: #080 }\n"
      "     .deleted  { background-color: #fcc; color: #800 }\n"
      "     .group    { background-color: #eee; border-top: 1px solid #888 }\n"
      "     .comment  { background-color: #eee; color: #888 }\n"
      "     #diff     { font-family: Consolas, Lucida Console, Monaco, monospace; margin-left: 20pt }\n"
      "     #diff *   { white-space: pre }\n"
      "     </style>\n"
      "  </head>\n"
      "  <body class=\"proportional\">\n"
      "     <h2>Path: %3%</h2>\n"
      "     <div id=\"diff\">%4%</div>\n"
      "  </body>\n"
      "</html>"
   );

   // build dynamic html and return it
   std::string path = module_context::createAliasedPath(targetPath);
   std::string html = boost::str(htmlFmt % caption % command % path % diff);
   pResponse->setDynamicHtml(html, request);
}

SEXP rs_diff(SEXP captionSEXP, SEXP commandSEXP, SEXP targetSEXP)
{
   // build url
   http::Fields queryParams;
   queryParams.push_back(std::make_pair(kCaption, r::sexp::asString(captionSEXP)));
   queryParams.push_back(std::make_pair(kCommand, r::sexp::asString(commandSEXP)));
   queryParams.push_back(std::make_pair(kTarget, r::sexp::asString(targetSEXP)));
   std::string queryString;
   http::util::buildQueryString(queryParams, &queryString);
   std::string url = std::string(kDiffViewRel) + "?" + queryString;

   // fire browse url event
   ClientEvent event = browseUrlEvent(url, "_rstudio_diff_view");
   module_context::enqueClientEvent(event);

   return R_NilValue;
}


} // anonymous namespace

Error initialize()
{
   R_CallMethodDef diffMethodDef ;
   diffMethodDef.name = "rs_diff" ;
   diffMethodDef.fun = (DL_FUNC) rs_diff ;
   diffMethodDef.numArgs = 3;
   r::routines::addCallMethod(diffMethodDef);

   using boost::bind;
   using namespace session::module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerUriHandler, kDiffView, handleDiffViewRequest))
      (bind(sourceModuleRFile, "SessionDiff.R"));
   return initBlock.execute();

}
   
   
} // namepsace diff
} // namespace modules
} // namesapce session

