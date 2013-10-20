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


void viewerNavigate(const std::string& url, int height = 0)
{
   // if we are in server mode then we need to do port mapping
   std::string mappedUrl = url;
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      // see if we can form a portmap path for this url
      std::string path;
      if (module_context::portmapPathForLocalhostUrl(url, &path))
         mappedUrl = path;
   }


   // record the url (for reloads)
   s_currentUrl = mappedUrl;

   // enque the event
   json::Object dataJson;
   dataJson["url"] = s_currentUrl;
   dataJson["height"] = height;
   ClientEvent event(client_events::kViewerNavigate, dataJson);
   module_context::enqueClientEvent(event);
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
         FilePath filePath(url);

         // if it's in the temp dir and we're running R >= 2.14 then
         // we can serve it via the help server, otherwise we need
         // to show it in an external browser
         if (filePath.isWithin(tempDir) && r::util::hasRequiredVersion("2.14"))
         {
            std::string path = filePath.relativePath(tempDir);
            boost::format fmt("session/%1%");
            url = boost::str(fmt % path);
            viewerNavigate(url, height);
         }
         else
         {
            module_context::showFile(filePath);
         }
      }
      else
      {
         viewerNavigate(url, height);
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
       r::options::getOption<std::string>("ggvis.renderer").empty())
   {
      r::options::setOption("ggvis.renderer", "svg");
   }

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

