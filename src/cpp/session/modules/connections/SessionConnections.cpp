/*
 * SessionConnections.cpp
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

#include "SessionConnections.hpp"

#include <boost/foreach.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

// #define LOG_CONNECTION_EVENTS 1

#ifdef LOG_CONNECTION_EVENTS
#define LOG_CONNECTION_EVENT(event, type, host) \
   std::cout << "Connection " << event << ": " << type << " - " << host << std::endl;
#else
#define LOG_CONNECTION_EVENT(event, type, host)
#endif

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {


namespace {

SEXP rs_connectionOpened(SEXP typeSEXP,
                         SEXP hostSEXP,
                         SEXP finderSEXP,
                         SEXP connectCodeSEXP,
                         SEXP disconnectCodeSEXP)
{
   std::string type = r::sexp::safeAsString(typeSEXP);
   std::string host = r::sexp::safeAsString(hostSEXP);
   std::string finder = r::sexp::safeAsString(finderSEXP);
   std::string connectCode = r::sexp::safeAsString(connectCodeSEXP);
   std::string disconnectCode = r::sexp::safeAsString(connectCodeSEXP);
   LOG_CONNECTION_EVENT("Opened", type, host);
   return R_NilValue;
}

SEXP rs_connectionClosed(SEXP typeSEXP, SEXP hostSEXP)
{
   std::string type = r::sexp::safeAsString(typeSEXP);
   std::string host = r::sexp::safeAsString(hostSEXP);
   LOG_CONNECTION_EVENT("Closed", type, host);
   return R_NilValue;
}

SEXP rs_connectionUpdated(SEXP typeSEXP, SEXP hostSEXP)
{
   std::string type = r::sexp::safeAsString(typeSEXP);
   std::string host = r::sexp::safeAsString(hostSEXP);
   LOG_CONNECTION_EVENT("Updated", type, host);
   return R_NilValue;
}

// track last reported state of connections enabled
bool s_reportedConnectionsEnabled = false;

void onInstalledPackagesChanged()
{
   // reload the IDE if connections should be enabled but they were
   // last reported as disabled
   if (!s_reportedConnectionsEnabled && connectionsEnabled())
   {
      ClientEvent event(client_events::kReloadWithLastChanceSave);
      module_context::enqueClientEvent(event);
   }
}

} // anonymous namespace


bool connectionsEnabled()
{
   // track last reported state of connections enabled
   s_reportedConnectionsEnabled = module_context::isPackageInstalled("rspark");

   // return value
   return s_reportedConnectionsEnabled;
}

Error initialize()
{
   // register methods
   RS_REGISTER_CALL_METHOD(rs_connectionOpened, 5);
   RS_REGISTER_CALL_METHOD(rs_connectionClosed, 2);
   RS_REGISTER_CALL_METHOD(rs_connectionUpdated, 2);

   // connect to events to track whether we should enable connections
   module_context::events().onPackageLibraryMutated.connect(
                                             onInstalledPackagesChanged);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionConnections.R"));

   return initBlock.execute();
}


} // namespace connections
} // namespace modules
} // namesapce session
} // namespace rstudio

