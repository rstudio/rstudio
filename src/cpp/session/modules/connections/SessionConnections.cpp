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
#include <r/RExec.hpp>


#include <session/SessionModuleContext.hpp>

#include "ActiveConnections.hpp"
#include "ConnectionHistory.hpp"

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
   // read params
   std::string type = r::sexp::safeAsString(typeSEXP);
   std::string host = r::sexp::safeAsString(hostSEXP);
   std::string finder = r::sexp::safeAsString(finderSEXP);
   std::string connectCode = r::sexp::safeAsString(connectCodeSEXP);
   std::string disconnectCode = r::sexp::safeAsString(disconnectCodeSEXP);

   // create connection object
   Connection connection(ConnectionId(type, host),
                         finder,
                         connectCode,
                         disconnectCode,
                         date_time::millisecondsSinceEpoch());

   // update connection history
   connectionHistory().update(connection);

   // update active connections
   activeConnections().add(connection.id);

   return R_NilValue;
}

SEXP rs_connectionClosed(SEXP typeSEXP, SEXP hostSEXP)
{
   std::string type = r::sexp::safeAsString(typeSEXP);
   std::string host = r::sexp::safeAsString(hostSEXP);

   // update active connections
   activeConnections().remove(ConnectionId(type, host));

   return R_NilValue;
}

SEXP rs_connectionUpdated(SEXP typeSEXP, SEXP hostSEXP)
{
   std::string type = r::sexp::safeAsString(typeSEXP);
   std::string host = r::sexp::safeAsString(hostSEXP);
   ConnectionId id(type, host);

   ClientEvent event(client_events::kConnectionUpdated,
                     connectionIdJson(id));
   module_context::enqueClientEvent(event);

   return R_NilValue;
}

Error removeConnection(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // read params
   std::string type, host;
   Error error = json::readObjectParam(request.params, 0,
                                       "type", &type,
                                       "host", &host);
   if (error)
      return error;

   // remove connection
   ConnectionId id(type, host);
   connectionHistory().remove(id);

   return Success();
}

Error getDisconnectCode(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   // read params
   json::Object idJson;
   std::string finder, disconnectCode;
   Error error = json::readObjectParam(request.params, 0,
                                       "id", &idJson,
                                       "finder", &finder,
                                       "disconnect_code", &disconnectCode);
   if (error)
      return error;
   std::string type, host;
   error = json::readObject(idJson, "type", &type, "host", &host);
   if (error)
      return error;

   // call R function to determine disconnect code
   r::exec::RFunction func(".rs.getDisconnectCode");
   func.addParam(finder);
   func.addParam(host);
   func.addParam(disconnectCode);
   std::string code;
   error = func.call(&code);
   if (error)
      return error;

   pResponse->setResult(code);

   return Success();
}

// track whether connections were enabled at the start of this R session
bool s_connectionsInitiallyEnabled = false;

void onInstalledPackagesChanged()
{
   if (activateConnections())
   {
      ClientEvent event(client_events::kEnableConnections);
      module_context::enqueClientEvent(event);
   }
}

} // anonymous namespace


bool connectionsEnabled()
{
   return module_context::isPackageInstalled("rspark");
}

bool activateConnections()
{
   return !s_connectionsInitiallyEnabled && connectionsEnabled();
}

json::Array connectionsAsJson()
{
   return connectionHistory().connectionsAsJson();
}

json::Array activeConnectionsAsJson()
{
   return activeConnections().activeConnectionsAsJson();
}

Error initialize()
{
   // register methods
   RS_REGISTER_CALL_METHOD(rs_connectionOpened, 5);
   RS_REGISTER_CALL_METHOD(rs_connectionClosed, 2);
   RS_REGISTER_CALL_METHOD(rs_connectionUpdated, 2);

   // initialize connection history
   Error error = connectionHistory().initialize();
   if (error)
      return error;

   // connect to events to track whether we should enable connections
   s_connectionsInitiallyEnabled = connectionsEnabled();
   module_context::events().onPackageLibraryMutated.connect(
                                             onInstalledPackagesChanged);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "remove_connection", removeConnection))
      (bind(registerRpcMethod, "get_disconnect_code", getDisconnectCode))
      (bind(sourceModuleRFile, "SessionConnections.R"));

   return initBlock.execute();
}


} // namespace connections
} // namespace modules
} // namesapce session
} // namespace rstudio

