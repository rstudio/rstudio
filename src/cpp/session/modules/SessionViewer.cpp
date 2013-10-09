/*
 * SessionViewer.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "SessionViewer.hpp"

#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>

#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace viewer {

namespace {

// track the current viewed url
std::string s_currentUrl;

// viewer stopped means clear the url
Error viewerStopped(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   s_currentUrl.clear();
   return Success();
}


void viewerNavigate(const std::string& url, bool maximize = TRUE)
{
   // record the url (for reloads)
   s_currentUrl = url;

   // enque the event
   json::Object dataJson;
   dataJson["url"] = s_currentUrl;
   dataJson["maximize"] = maximize;
   ClientEvent event(client_events::kViewerNavigate, dataJson);
   module_context::enqueClientEvent(event);
}

SEXP rs_viewApp(SEXP urlSEXP, SEXP maximizeSEXP)
{
   try
   {
      // get the maximize parameter
      bool maximize = r::sexp::asLogical(maximizeSEXP);

      // transform the url to a localhost:<port>/session one if it's
      // a path to a file within the R session temporary directory
      std::string url = r::sexp::safeAsString(urlSEXP);
      if (!boost::algorithm::starts_with(url, "http"))
      {
         // get the path to the tempdir and the file
         FilePath tempDir = r::session::utils::tempDir();
         FilePath filePath(url);

         // if it's in the temp dir and we're running R >= 2.14 then
         // we can serve it via the help server, otherwise we need
         // to show it in an external browser
         if (filePath.isWithin(tempDir) && r::util::hasRequiredVersion("2.14"))
         {
            std::string path = filePath.relativePath(tempDir);
            boost::format fmt("http://localhost:%1%/session/%2%");
            url = boost::str(fmt % module_context::rLocalHelpPort() % path);
            viewerNavigate(url, maximize);
         }
         else
         {
            module_context::showFile(filePath);
         }
      }
      else
      {
         viewerNavigate(url, maximize);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}

void onSuspend(const r::session::RSuspendOptions&, Settings*)
{
}

void onResume(const Settings&)
{
   viewerNavigate("", false);
}

void onClientInit()
{
   if (!s_currentUrl.empty())
      viewerNavigate(s_currentUrl);
}

} // anonymous namespace


Error initialize()
{
   R_CallMethodDef methodDefViewApp ;
   methodDefViewApp.name = "rs_viewApp" ;
   methodDefViewApp.fun = (DL_FUNC) rs_viewApp ;
   methodDefViewApp.numArgs = 2;
   r::routines::addCallMethod(methodDefViewApp);

   // install event handlers
   using namespace module_context;
   events().onClientInit.connect(onClientInit);
   addSuspendHandler(SuspendHandler(onSuspend, onResume));

   // install rpc methods
   using boost::bind;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "viewer_stopped", viewerStopped));
   return initBlock.execute();
}


} // namespace viewer
} // namespace modules
} // namesapce session

