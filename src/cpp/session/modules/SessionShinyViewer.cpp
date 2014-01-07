/*
 * SessionShinyViewer.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#include "SessionShinyViewer.hpp"

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
namespace shinyViewer {

namespace {

// track the current viewed url and path
std::string s_currentAppUrl;
std::string s_currentAppPath;

void loadApp(const std::string& url, const std::string& path)
{
   // record the url and path
   s_currentAppUrl = url;
   s_currentAppPath = path;

   // enque the event
   json::Object dataJson;
   dataJson["url"] = s_currentAppUrl;
   dataJson["path"] = s_currentAppPath;
   ClientEvent event(client_events::kShinyViewer, dataJson);
   module_context::enqueClientEvent(event);
}

SEXP rs_Shinyviewer(SEXP urlSEXP, SEXP pathSEXP)
{
   loadApp(r::sexp::safeAsString(urlSEXP),
           r::sexp::safeAsString(pathSEXP));

   return R_NilValue;
}

} // anonymous namespace


Error initialize()
{
   R_CallMethodDef methodDefViewer ;
   methodDefViewer.name = "rs_shinyViewer" ;
   methodDefViewer.fun = (DL_FUNC) rs_viewer ;
   methodDefViewer.numArgs = 2;
   r::routines::addCallMethod(methodDefViewer);
}

} // namespace shinyViewer
} // namespace modules
} // namesapce session

