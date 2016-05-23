/*
 * ConnectionList.cpp
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

#include "ConnectionList.hpp"

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {


namespace {

json::Object connectionIdJson(const std::string& type, const std::string& host)
{
   json::Object idJson;
   idJson["type"] = type;
   idJson["host"] = host;
   return idJson;
}

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
   std::string disconnectCode = r::sexp::safeAsString(disconnectCodeSEXP);

   json::Object connectionJson;
   connectionJson["id"] = connectionIdJson(type, host);
   connectionJson["finder"] = finder;
   connectionJson["connectCode"] = connectCode;
   connectionJson["disconnectCode"] = disconnectCode;

   ClientEvent event(client_events::kConnectionOpened, connectionJson);
   module_context::enqueClientEvent(event);

   return R_NilValue;
}

SEXP rs_connectionClosed(SEXP typeSEXP, SEXP hostSEXP)
{
   std::string type = r::sexp::safeAsString(typeSEXP);
   std::string host = r::sexp::safeAsString(hostSEXP);

   ClientEvent event(client_events::kConnectionClosed,
                     connectionIdJson(type, host));
   module_context::enqueClientEvent(event);

   return R_NilValue;
}

SEXP rs_connectionUpdated(SEXP typeSEXP, SEXP hostSEXP)
{
   std::string type = r::sexp::safeAsString(typeSEXP);
   std::string host = r::sexp::safeAsString(hostSEXP);

   ClientEvent event(client_events::kConnectionUpdated,
                     connectionIdJson(type, host));
   module_context::enqueClientEvent(event);

   return R_NilValue;
}




} // anonymous namespace



ConnectionList& connectionList()
{
   static ConnectionList instance;
   return instance;
}

ConnectionList::ConnectionList()
{
}

void initializeConnectionList()
{
   // register methods
   RS_REGISTER_CALL_METHOD(rs_connectionOpened, 5);
   RS_REGISTER_CALL_METHOD(rs_connectionClosed, 2);
   RS_REGISTER_CALL_METHOD(rs_connectionUpdated, 2);
}


} // namespace connections
} // namespace modules
} // namesapce session
} // namespace rstudio

