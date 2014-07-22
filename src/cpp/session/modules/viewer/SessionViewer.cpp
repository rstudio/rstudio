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
#include <r/ROptions.hpp>

#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>

#include "ViewerHistory.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace viewer {

namespace {

// track the current viewed url and whether it is a static widget
std::string s_currentUrl;
bool s_isHTMLWidget = false;

void viewerNavigate(const std::string& url,
                    int height,
                    bool isHTMLWidget)
{
   // record the url (for reloads)
   s_currentUrl = module_context::mapUrlPorts(url);
   s_isHTMLWidget = isHTMLWidget;

   // enque the event
   json::Object dataJson;
   dataJson["url"] = s_currentUrl;
   dataJson["height"] = height;
   dataJson["html_widget"] = isHTMLWidget;
   dataJson["has_next"] = isHTMLWidget && viewerHistory().hasNext();
   dataJson["has_previous"] = isHTMLWidget && viewerHistory().hasPrevious();
   ClientEvent event(client_events::kViewerNavigate, dataJson);
   module_context::enqueClientEvent(event);
}

void viewerNavigate(const std::string& url, bool isHTMLWidget)
{
   return viewerNavigate(url, 0, isHTMLWidget);
}

Error viewerStopped(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   // clear current state
   s_currentUrl.clear();
   s_isHTMLWidget = false;

   return Success();
}

Error viewerBack(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   if (viewerHistory().hasPrevious())
      viewerNavigate(viewerHistory().goBack().url(), true);
   return Success();
}

Error viewerForward(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   if (viewerHistory().hasNext())
      viewerNavigate(viewerHistory().goForward().url(), true);
   return Success();
}

Error viewerCurrent(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   module_context::ViewerHistoryEntry current = viewerHistory().current();
   if (!current.empty())
      viewerNavigate(current.url(), true);
   return Success();
}

Error viewerClearCurrent(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   viewerHistory().clearCurrent();
   return viewerCurrent(request, pResponse);
}


SEXP rs_viewer(SEXP urlSEXP, SEXP heightSEXP)
{
   try
   {
      // get the height parameter (0 if null)
      int height = 0;
      if (!r::sexp::isNull(heightSEXP))
         height = r::sexp::asInteger(heightSEXP);

      // transform the url to a localhost:<port>/session one if it's
      // a path to a file within the R session temporary directory
      std::string url = r::sexp::safeAsString(urlSEXP);
      if (!boost::algorithm::starts_with(url, "http"))
      {
         // get the path to the tempdir and the file
         FilePath tempDir = r::session::utils::tempDir();
         FilePath filePath = module_context::resolveAliasedPath(url);

         // if it's in the temp dir and we're running R >= 2.14 then
         // we can serve it via the help server, otherwise we need
         // to show it in an external browser
         if (filePath.isWithin(tempDir) && r::util::hasRequiredVersion("2.14"))
         {
            // calculate the relative path
            std::string path = filePath.relativePath(tempDir);

            // add it to our history
            viewerHistory().add(module_context::ViewerHistoryEntry(path));

            // view it
            viewerNavigate(viewerHistory().current().url(), height, true);
         }
         else
         {
            module_context::showFile(filePath);
         }
      }
      else
      {
         // in desktop mode make sure we have the right version of httpuv
         if (options().programMode() == kSessionProgramModeDesktop)
         {
            if (!module_context::isPackageVersionInstalled("httpuv", "1.2"))
            {
               module_context::consoleWriteError("\nWARNING: To run "
                 "applications within the RStudio Viewer pane you need to "
                 "install the latest version of the httpuv package from "
                 "CRAN (version 1.2 or higher is required).\n\n");
            }
         }

         // navigate the viewer
         viewerNavigate(url, height, false);
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
      viewerNavigate(s_currentUrl, s_isHTMLWidget);
}

} // anonymous namespace

Error initialize()
{
   R_CallMethodDef methodDefViewer ;
   methodDefViewer.name = "rs_viewer" ;
   methodDefViewer.fun = (DL_FUNC) rs_viewer ;
   methodDefViewer.numArgs = 2;
   r::routines::addCallMethod(methodDefViewer);

   // install event handlers
   using namespace module_context;
   events().onClientInit.connect(onClientInit);
   addSuspendHandler(SuspendHandler(onSuspend, onResume));

   // set ggvis.renderer to svg in desktop mode
   if ((session::options().programMode() == kSessionProgramModeDesktop) &&
       r::options::getOption<std::string>("ggvis.renderer", "", false).empty())
   {
      r::options::setOption("ggvis.renderer", "svg");
   }

   // install rpc methods
   using boost::bind;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "viewer_stopped", viewerStopped))
      (bind(registerRpcMethod, "viewer_current", viewerCurrent))
      (bind(registerRpcMethod, "viewer_clear_current", viewerClearCurrent))
      (bind(registerRpcMethod, "viewer_forward", viewerForward))
      (bind(registerRpcMethod, "viewer_back", viewerBack));
   return initBlock.execute();
}


} // namespace viewer
} // namespace modules
} // namesapce session

