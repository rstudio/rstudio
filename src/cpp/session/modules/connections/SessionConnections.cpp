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
#include <boost/format.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Process.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionConsoleProcess.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

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
                         SEXP disconnectCodeSEXP,
                         SEXP listTablesCodeSEXP,
                         SEXP listColumnsCodeSEXP,
                         SEXP previewTableCodeSEXP)
{
   // read params
   std::string type = r::sexp::safeAsString(typeSEXP);
   std::string host = r::sexp::safeAsString(hostSEXP);
   std::string finder = r::sexp::safeAsString(finderSEXP);
   std::string connectCode = r::sexp::safeAsString(connectCodeSEXP);
   std::string disconnectCode = r::sexp::safeAsString(disconnectCodeSEXP);
   std::string listTablesCode = r::sexp::safeAsString(listTablesCodeSEXP);
   std::string listColumnsCode = r::sexp::safeAsString(listColumnsCodeSEXP);
   std::string previewTableCode = r::sexp::safeAsString(previewTableCodeSEXP);

   // create connection object
   Connection connection(ConnectionId(type, host),
                         finder,
                         connectCode,
                         disconnectCode,
                         listTablesCode,
                         listColumnsCode,
                         previewTableCode,
                         date_time::millisecondsSinceEpoch());

   // update connection history
   connectionHistory().update(connection);

   // update active connections
   activeConnections().add(connection.id);

   // fire connection opended event
   ClientEvent event(client_events::kConnectionOpened,
                     connectionJson(connection));
   module_context::enqueClientEvent(event);

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

SEXP rs_connectionUpdated(SEXP typeSEXP, SEXP hostSEXP, SEXP hintSEXP)
{
   std::string type = r::sexp::safeAsString(typeSEXP);
   std::string host = r::sexp::safeAsString(hostSEXP);
   std::string hint = r::sexp::safeAsString(hintSEXP);
   ConnectionId id(type, host);

   json::Object updatedJson;
   updatedJson["id"] = connectionIdJson(id);
   updatedJson["hint"] = hint;

   ClientEvent event(client_events::kConnectionUpdated, updatedJson);
   module_context::enqueClientEvent(event);

   return R_NilValue;
}

SEXP rs_defaultSparkClusterUrl()
{
   std::string clusterUrl = "spark://local:7077";

   // for rstudio server do some extra detection
   if (options().programMode() == kSessionProgramModeServer)
   {
      FilePath clusterUrlPath("/root/spark-ec2/cluster-url");
      if (clusterUrlPath.exists())
      {
         std::string ec2ClusterUrl;
         Error error = core::readStringFromFile(clusterUrlPath, &ec2ClusterUrl);
         if (!error)
         {
            boost::algorithm::trim(ec2ClusterUrl);
            if (!ec2ClusterUrl.empty())
               clusterUrl = ec2ClusterUrl;
         }
         else if (error.code() != boost::system::errc::permission_denied)
         {
            LOG_ERROR(error);
         }
      }
   }
   r::sexp::Protect rProtect;
   return r::sexp::create(clusterUrl, &rProtect);
}

SEXP rs_availableRemoteServers()
{
   // get list of previous connections and extract unique remote servers
   std::vector<std::string> remoteServers;
   json::Array connectionsJson = connectionHistory().connectionsAsJson();
   BOOST_FOREACH(const json::Value connectionJson, connectionsJson)
   {
      // don't inspect if not an object -- this should never happen
      // but we screen it anyway to prevent a crash on corrupt data
      if (!json::isType<json::Object>(connectionJson))
         continue;

      // get the host
      json::Object idJson;
      Error error = json::readObject(connectionJson.get_obj(), "id", &idJson);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      std::string host;
      error = json::readObject(idJson, "host", &host);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      // add it if necessary
      if(std::find(remoteServers.begin(), remoteServers.end(), host) ==
                                                         remoteServers.end())
      {
         if (host != "local" && !boost::algorithm::starts_with(host, "local["))
            remoteServers.push_back(host);
      }
   }

   r::sexp::Protect rProtect;
   return r::sexp::create(remoteServers, &rProtect);
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
   {
      LOG_ERROR(error);
      return error;
   }

   pResponse->setResult(code);

   return Success();
}

Error readConnectionParam(const json::JsonRpcRequest& request,
                          Connection* pConnection)
{
   // read param
   json::Object connectionJson;
   Error error = json::readParam(request.params, 0, &connectionJson);
   if (error)
      return error;

   return connectionFromJson(connectionJson, pConnection);
}

Error readConnectionAndTableParams(const json::JsonRpcRequest& request,
                                   Connection* pConnection,
                                   std::string* pTable)
{
   // get connection param
   Error error = readConnectionParam(request, pConnection);
   if (error)
      return error;

   // get table param
   std::string table;
   return json::readParam(request.params, 1, pTable);
}


Error showSparkLog(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   // get connection param
   Connection connection;
   Error error = readConnectionParam(request, &connection);
   if (error)
      return error;

   // get the log file
   std::string log;
   error = r::exec::RFunction(".rs.getSparkLogFile",
                                 connection.finder,
                                 connection.id.host).call(&log);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // show the file
   module_context::showFile(FilePath(log));

   return Success();
}

Error showSparkUI(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   // get connection param
   Connection connection;
   Error error = readConnectionParam(request, &connection);
   if (error)
      return error;

   // get the url
   std::string url;
   error = r::exec::RFunction(".rs.getSparkWebUrl",
                                 connection.finder,
                                 connection.id.host).call(&url);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // portmap if necessary
   url = module_context::mapUrlPorts(url);

   // show the ui
   ClientEvent event = browseUrlEvent(url);
   module_context::enqueClientEvent(event);

   return Success();
}


void connectionListTables(const json::JsonRpcRequest& request,
                          const json::JsonRpcFunctionContinuation& continuation)
{
   // get connection param
   Connection connection;
   Error error = readConnectionParam(request, &connection);
   if (error)
   {
      json::JsonRpcResponse response;
      continuation(error, &response);
      return;
   }

   // response
   json::JsonRpcResponse response;

   // get the list of tables
   std::vector<std::string> tables;
   error = r::exec::RFunction(".rs.connectionListTables",
                                 connection.finder,
                                 connection.id.host,
                                 connection.listTablesCode).call(&tables);
   if (error)
   {
      continuation(error, &response);
   }
   else
   {
      response.setResult(json::toJsonArray((tables)));
      continuation(Success(), &response);
   }
}

void sendResponse(const Error& error,
                  SEXP sexpResult,
                  const json::JsonRpcFunctionContinuation& continuation,
                  const ErrorLocation& errorLocation)
{
   // response
   json::JsonRpcResponse response;

   if (error)
   {
      core::log::logError(error, errorLocation);
      continuation(error, &response);
   }
   else
   {
      core::json::Value jsonResult;
      Error error = r::json::jsonValueFromObject(sexpResult, &jsonResult);
      if (error)
      {
         core::log::logError(error, errorLocation);
         continuation(error, &response);
      }
      else
      {
         response.setResult(jsonResult);
         continuation(Success(), &response);
      }
   }
}


void connectionListFields(const json::JsonRpcRequest& request,
                          const json::JsonRpcFunctionContinuation& continuation)
{
   // get connection and table params
   Connection connection;
   std::string table;
   Error error = readConnectionAndTableParams(request, &connection, &table);
   if (error)
   {
      json::JsonRpcResponse response;
      continuation(error, &response);
      return;
   }

   // get the list of fields
   r::sexp::Protect rProtect;
   SEXP sexpResult;
   error = r::exec::RFunction(".rs.connectionListColumns",
                                 connection.finder,
                                 connection.id.host,
                                 connection.listColumnsCode,
                                 table).call(&sexpResult, &rProtect);


   // send the response
   sendResponse(error, sexpResult, continuation, ERROR_LOCATION);
}

void connectionPreviewTable(const json::JsonRpcRequest& request,
                          const json::JsonRpcFunctionContinuation& continuation)
{
   // get connection and table params
   Connection connection;
   std::string table;
   Error error = readConnectionAndTableParams(request, &connection, &table);
   if (error)
   {
      json::JsonRpcResponse response;
      continuation(error, &response);
      return;
   }

   // view the table
   r::sexp::Protect rProtect;
   SEXP sexpResult;
   error = r::exec::RFunction(".rs.connectionPreviewTable",
                                 connection.finder,
                                 connection.id.host,
                                 connection.previewTableCode,
                                 table,
                                 1000).call(&sexpResult, &rProtect);

   // send the response
   sendResponse(error, sexpResult, continuation, ERROR_LOCATION);
}


Error installSpark(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   // get params
   std::string sparkVersion, hadoopVersion;
   Error error = json::readParams(request.params,
                                  &sparkVersion,
                                  &hadoopVersion);
   if (error)
      return error;

   // R binary
   FilePath rProgramPath;
   error = module_context::rScriptPath(&rProgramPath);
   if (error)
      return error;

   // options
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   options.redirectStdErrToStdOut = true;

   // build install command
   boost::format fmt("sparklyr::spark_install('%1%', hadoop_version = '%2%', "
                     "verbose = TRUE)");
   std::string cmd = boost::str(fmt %
                                 sparkVersion %
                                 hadoopVersion);

   // build args
   std::vector<std::string> args;
   args.push_back("--slave");
   args.push_back("--vanilla");

   // propagate R_LIBS
   core::system::Options childEnv;
   core::system::environment(&childEnv);
   std::string libPaths = module_context::libPathsString();
   if (!libPaths.empty())
      core::system::setenv(&childEnv, "R_LIBS", libPaths);
   options.environment = childEnv;


   // for windows we need to forward setInternet2
#ifdef _WIN32
   if (!r::session::utils::isR3_3() && userSettings().useInternet2())
      args.push_back("--internet2");
#endif

   args.push_back("-e");
   args.push_back(cmd);

   // create and execute console process
   boost::shared_ptr<console_process::ConsoleProcess> pCP;
   pCP = console_process::ConsoleProcess::create(
            string_utils::utf8ToSystem(rProgramPath.absolutePath()),
            args,
            options,
            "Installing Spark " + sparkVersion,
            true,
            console_process::InteractionNever);

   // return console process
   pResponse->setResult(pCP->toJson());
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

void onDeferredInit(bool newSession)
{
   if (!newSession && connectionsEnabled())
   {
      activeConnections().broadcastToClient();
   }
}

void initEnvironment()
{
   // set RSTUDIO_WINUTILS (leave existing value alone)
   const char * const kRStudioWinutils = "RSTUDIO_WINUTILS";
   std::string rstudioWinutils = core::system::getenv(kRStudioWinutils);
   if (rstudioWinutils.empty())
      rstudioWinutils = session::options().winutilsPath().absolutePath();
   r::exec::RFunction sysSetenv("Sys.setenv");
   sysSetenv.addParam(kRStudioWinutils, rstudioWinutils);

   // call Sys.setenv
   Error error = sysSetenv.call();
   if (error)
      LOG_ERROR(error);
}


} // anonymous namespace


bool connectionsEnabled()
{
   return module_context::isPackageVersionInstalled("sparklyr", "0.2.5");
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

bool isSuspendable()
{
   return activeConnections().empty();
}

Error initialize()
{
   // register methods
   RS_REGISTER_CALL_METHOD(rs_connectionOpened, 8);
   RS_REGISTER_CALL_METHOD(rs_connectionClosed, 2);
   RS_REGISTER_CALL_METHOD(rs_connectionUpdated, 3);
   RS_REGISTER_CALL_METHOD(rs_availableRemoteServers, 0);
   RS_REGISTER_CALL_METHOD(rs_defaultSparkClusterUrl, 0);

   // initialize environment
   initEnvironment();

   // initialize connection history
   Error error = connectionHistory().initialize();
   if (error)
      return error;

   // deferrred init for updating active connections
   module_context::events().onDeferredInit.connect(onDeferredInit);

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
      (bind(registerRpcMethod, "show_spark_log", showSparkLog))
      (bind(registerRpcMethod, "show_spark_ui", showSparkUI))
      (bind(registerIdleOnlyAsyncRpcMethod, "connection_list_tables", connectionListTables))
      (bind(registerIdleOnlyAsyncRpcMethod, "connection_list_fields", connectionListFields))
      (bind(registerIdleOnlyAsyncRpcMethod, "connection_preview_table", connectionPreviewTable))
      (bind(registerRpcMethod, "install_spark", installSpark))
      (bind(sourceModuleRFile, "SessionConnections.R"));

   return initBlock.execute();
}


} // namespace connections
} // namespace modules
} // namesapce session
} // namespace rstudio

