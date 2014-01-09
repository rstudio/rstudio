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
namespace shiny_viewer {

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

SEXP rs_shinyviewer(SEXP urlSEXP, SEXP pathSEXP)
{   
   try
   {
      if (!r::sexp::isString(urlSEXP) || (r::sexp::length(urlSEXP) != 1))
      {
         throw r::exec::RErrorException(
            "url must be a single element character vector.");
      }

      if (!r::sexp::isString(pathSEXP) || (r::sexp::length(pathSEXP) != 1))
      {
         throw r::exec::RErrorException(
            "path must be a single element character vector.");
      }

      loadApp(r::sexp::safeAsString(urlSEXP),
              r::sexp::safeAsString(pathSEXP));
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   return R_NilValue;
}

} // anonymous namespace


Error initialize()
{
   R_CallMethodDef methodDefViewer;
   methodDefViewer.name = "rs_shinyviewer";
   methodDefViewer.fun = (DL_FUNC) rs_shinyviewer;
   methodDefViewer.numArgs = 2;
   r::routines::addCallMethod(methodDefViewer);

   return Success();
}

} // namespace shiny_viewer
} // namespace modules
} // namesapce session

