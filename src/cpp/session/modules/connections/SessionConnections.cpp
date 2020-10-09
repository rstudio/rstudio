/*
 * SessionConnections.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <boost/format.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
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
#include <session/SessionPackageProvidedExtension.hpp>
#include <session/SessionUrlPorts.hpp>

#include <session/prefs/UserPrefs.hpp>

#include "ActiveConnections.hpp"
#include "ConnectionHistory.hpp"
#include "ConnectionsIndexer.hpp"
#include "Connection.hpp"

#define kConnectionsPath "connections"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {


namespace {

SEXP rs_connectionOpened(SEXP connectionSEXP)
{
   // read params -- note that these attributes are already guaranteed to
   // exist as we validate the S3 object on the R side
   std::string type, host, connectCode, displayName, connectIcon;
   r::sexp::getNamedListElement(connectionSEXP, "type", &type);
   r::sexp::getNamedListElement(connectionSEXP, "host", &host);
   r::sexp::getNamedListElement(connectionSEXP, "connectCode", &connectCode);
   r::sexp::getNamedListElement(connectionSEXP, "displayName", &displayName);
   r::sexp::getNamedListElement(connectionSEXP, "icon", &connectIcon);

   // extract actions -- marshal R list (presuming we have one, as some
   // connections won't) into internal representation
   SEXP actionList = R_NilValue;
   Error error = r::sexp::getNamedListSEXP(connectionSEXP, "actions",
         &actionList);
   if (error)
      LOG_ERROR(error);
   std::vector<ConnectionAction> actions;
   if (!r::sexp::isNull(actionList))
   {
      std::vector<std::string> actionNames;
      r::sexp::getNames(actionList, &actionNames);
      for (const std::string& actionName : actionNames)
      {
         std::string icon;
         SEXP action;

         // extract the action object from the list
         error = r::sexp::getNamedListSEXP(actionList, actionName, &action);
         if (error)
         {
            LOG_ERROR(error);
            continue;
         }

         // extract the icon
         error = r::sexp::getNamedListElement(action, "icon", &icon);
         if (error)
         {
            LOG_ERROR(error);
            continue;
         }
         actions.push_back(ConnectionAction(actionName, icon));
      }
   }

   // extract object type
   SEXP objectTypeList = R_NilValue;
   error = r::sexp::getNamedListSEXP(connectionSEXP, "objectTypes",
         &objectTypeList);
   if (error)
      LOG_ERROR(error);
   std::vector<ConnectionObjectType> objectTypes;
   if (!r::sexp::isNull(objectTypeList))
   {
      int n = r::sexp::length(objectTypeList);
      for (int i = 0; i < n; i++)
      {
         std::string name;
         std::string icon;
         std::string contains;
         SEXP objectType = VECTOR_ELT(objectTypeList, i);

         error = r::sexp::getNamedListElement(objectType, "name", &name);
         if (error)
         {
            LOG_ERROR(error);
            continue;
         }

         // these fields are optional
         r::sexp::getNamedListElement(objectType, "contains", &contains);
         r::sexp::getNamedListElement(objectType, "icon", &icon);

         objectTypes.push_back(ConnectionObjectType(name, contains, icon));
      }
   }
   // create connection object
   Connection connection(ConnectionId(type, host), connectCode, displayName,
                         connectIcon, actions, objectTypes,
                         date_time::millisecondsSinceEpoch());

   // update connection history
   connectionHistory().update(connection);

   // update active connections
   activeConnections().add(connection.id);

   // fire connection opened event
   ClientEvent event(client_events::kConnectionOpened,
                     connectionJson(connection));
   module_context::enqueClientEvent(event);

   return R_NilValue;
}

void addObjectSpecifiers(const json::Array& specifiers, 
                         r::exec::RFunction* pFunction)
{
   for (unsigned i = 0; i < specifiers.getSize(); i++)
   {
      // make sure we're dealing with a json object
      const json::Value& val = specifiers[i];
      if (val.getType() != json::Type::OBJECT)
         continue;

      // extract the name and type of the specifier
      std::string name, type;
      Error error = json::readObject(val.getObject(),
            "name", name, "type", type);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      // add as a named argument to the function
      pFunction->addParam(type, name);
   }
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

SEXP rs_availableRemoteServers()
{
   // get list of previous connections and extract unique remote servers
   std::vector<std::string> remoteServers;
   json::Array connectionsJson = connectionHistory().connectionsAsJson();
   for (const json::Value connectionJson : connectionsJson)
   {
      // don't inspect if not an object -- this should never happen
      // but we screen it anyway to prevent a crash on corrupt data
      if (!json::isType<json::Object>(connectionJson))
         continue;

      // get the host
      json::Object idJson;
      Error error = json::readObject(connectionJson.getObject(), "id", idJson);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      std::string host;
      error = json::readObject(idJson, "host", host);
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

SEXP rs_availableConnections()
{
   std::string data = connectionsRegistryAsJson().write();

   r::sexp::Protect rProtect;
   return r::sexp::create(data, &rProtect);
}

SEXP rs_connectionIcon(SEXP iconNameSEXP)
{
   std::string iconName = r::sexp::safeAsString(iconNameSEXP);

   std::string data = iconData("drivers", iconName, "");

   r::sexp::Protect rProtect;
   return r::sexp::create(data, &rProtect);
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

Error connectionDisconnect(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   // read params
   std::string finder, disconnectCode;
   std::string type, host;
   Error error = json::readObjectParam(request.params, 0, 
         "type", &type, "host", &host);
   if (error)
      return error;

   // call R function to perform disconnection
   r::exec::RFunction func(".rs.connectionDisconnect");
   func.addParam(type);
   func.addParam(host);
   error = func.call();
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   return Success();
}

Error readConnectionIdParam(const json::JsonRpcRequest& request,
                            ConnectionId* pConnectionId)
{
   // read param
   json::Object connectionIdJson;
   Error error = json::readParam(request.params, 0, &connectionIdJson);
   if (error)
      return error;

   return connectionIdFromJson(connectionIdJson, pConnectionId);
}

Error readConnectionIdAndObjectParams(const json::JsonRpcRequest& request,
                                      ConnectionId* pConnectionId,
                                      json::Array* pObjectSpecifier)
{
   // get connection param
   Error error = readConnectionIdParam(request, pConnectionId);
   if (error)
      return error;

   // get object param
   return json::readParam(request.params, 1, pObjectSpecifier);
}

Error connectionExecuteAction(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   ConnectionId connectionId;
   std::string action;
   Error error = readConnectionIdParam(request, &connectionId);
   if (error)
      return error;
   error = json::readParam(request.params, 1, &action);
   if (error)
      return error;
   
   return r::exec::RFunction(".rs.connectionExecuteAction",
                                 connectionId.type,
                                 connectionId.host,
                                 action).call();
}

void connectionListObjects(const json::JsonRpcRequest& request,
                           const json::JsonRpcFunctionContinuation& continuation)
{
   // get connection param
   ConnectionId connectionId;
   json::Array objectSpecifier;
   Error error = readConnectionIdAndObjectParams(request, &connectionId,
         &objectSpecifier);
   if (error)
   {
      json::JsonRpcResponse response;
      continuation(error, &response);
      return;
   }

   // response
   json::JsonRpcResponse response;

   // get the list of objects
   SEXP objects;
   r::sexp::Protect protect;
   r::exec::RFunction listObjects(".rs.connectionListObjects",
                                 connectionId.type,
                                 connectionId.host);
   addObjectSpecifiers(objectSpecifier, &listObjects);
   error = listObjects.call(&objects, &protect);
   if (error)
   {
      continuation(error, &response);
   }
   else
   {
      json::Value result;
      error = r::json::jsonValueFromObject(objects, &result);
      if (error)
      {
         continuation(error, &response);
      }
      else
      {
         response.setResult(result);
         continuation(Success(), &response);
      }
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
   ConnectionId connectionId;
   json::Array objectSpecifier;
   Error error = readConnectionIdAndObjectParams(request, &connectionId,
         &objectSpecifier);
   if (error)
   {
      json::JsonRpcResponse response;
      continuation(error, &response);
      return;
   }

   // get the list of fields
   r::sexp::Protect rProtect;
   SEXP sexpResult;
   r::exec::RFunction listCols(".rs.connectionListColumns",
                                 connectionId.type,
                                 connectionId.host);
   
   addObjectSpecifiers(objectSpecifier, &listCols);
   error = listCols.call(&sexpResult, &rProtect);

   // send the response
   sendResponse(error, sexpResult, continuation, ERROR_LOCATION);
}

void connectionPreviewObject(const json::JsonRpcRequest& request,
                             const json::JsonRpcFunctionContinuation& continuation)
{
   // get connection and table params
   ConnectionId connectionId;
   json::Array objectSpecifier;
   Error error = readConnectionIdAndObjectParams(request, &connectionId,
         &objectSpecifier);
   if (error)
   {
      json::JsonRpcResponse response;
      continuation(error, &response);
      return;
   }

   // view the table
   r::sexp::Protect rProtect;
   SEXP sexpResult;
   r::exec::RFunction previewObject(".rs.connectionPreviewObject",
                                 connectionId.type,
                                 connectionId.host,
                                 1000);
   addObjectSpecifiers(objectSpecifier, &previewObject);
   error = previewObject.call(&sexpResult, &rProtect);

   // send the response
   sendResponse(error, sexpResult, continuation, ERROR_LOCATION);
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
      rstudioWinutils = session::options().winutilsPath().getAbsolutePath();
   r::exec::RFunction sysSetenv("Sys.setenv");
   sysSetenv.addParam(kRStudioWinutils, rstudioWinutils);

   // call Sys.setenv
   Error error = sysSetenv.call();
   if (error)
      LOG_ERROR(error);
}


Error handleConnectionsResourceRequest(const http::Request& request,
                               http::Response* pResponse)
{
   std::string path = http::util::pathAfterPrefix(
         request, "/" kConnectionsPath "/");
   core::FilePath res = options().rResourcesPath().completePath(kConnectionsPath)
                                 .completeChildPath(path);
   pResponse->setCacheableFile(res, request);
   return Success();
}


} // anonymous namespace

SEXP rs_connectionAddPackage(SEXP packageSEXP, SEXP extensionSEXP)
{
   std::string packageName = r::sexp::safeAsString(packageSEXP);
   std::string extensionPath = r::sexp::safeAsString(extensionSEXP);

   registerConnectionsRegistryEntry(packageName, extensionPath);

   return R_NilValue;
}

SEXP rs_embeddedViewer(SEXP urlSEXP)
{
   std::string url = r::sexp::safeAsString(urlSEXP);

   std::string mappedUrl = url_ports::mapUrlPorts(url);

   json::Object dataJson;
   dataJson["url"] = mappedUrl;

   ClientEvent event(client_events::kNavigateShinyFrame, dataJson);
   module_context::enqueClientEvent(event);

   return R_NilValue;
}

SEXP rs_connectionOdbcInstallPath()
{
   r::sexp::Protect rProtect;
   std::string path = module_context::userScratchPath().getAbsolutePath();
   return r::sexp::create(path, &rProtect);
}

bool connectionsEnabled()
{
   return true;
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

Error installOdbcDriver(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   // get parameters
   std::string driverName;
   std::string installPath;
   Error error = json::readParams(request.params, &driverName, &installPath);
   if (error)
      return error;

   // find the tools module
   FilePath rPath = session::options().coreRSourcePath();
   std::string toolsPath = core::string_utils::utf8ToSystem(
      rPath.completeChildPath("Tools.R").getAbsolutePath());

   // find connection installer module
   FilePath modulesPath = session::options().modulesRSourcePath();
   std::string scriptPath = core::string_utils::utf8ToSystem(
      modulesPath.completePath("SessionConnectionsInstaller.R").getAbsolutePath());

   // source the command
   std::string cmd;
   cmd.append("source('");
   cmd.append(string_utils::singleQuotedStrEscape(toolsPath));
   cmd.append("');");
   cmd.append("source('");
   cmd.append(string_utils::singleQuotedStrEscape(scriptPath));
   cmd.append("');");
   cmd.append("");

   // build installation command
   r::sexp::Protect rProtect;
   SEXP commandSEXP = R_NilValue;
   r::exec::RFunction func(".rs.connectionInstallerCommand");
   func.addParam(driverName);
   func.addParam(installPath);
   error = func.call(&commandSEXP, &rProtect);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   std::string command = r::sexp::asString(commandSEXP);

   // append installation command
   cmd.append(command);

   // R binary
   FilePath rProgramPath;
   error = module_context::rScriptPath(&rProgramPath);
   if (error)
      return error;

   // options
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   options.redirectStdErrToStdOut = true;

   // build args
   std::vector<std::string> args;
   args.push_back("--slave");
   args.push_back("--vanilla");

   // for windows we need to forward setInternet2
#ifdef _WIN32
   if (!r::session::utils::isR3_3() && prefs::userPrefs().useInternet2())
      args.push_back("--internet2");
#endif

   args.push_back("-e");
   args.push_back(cmd);

   boost::shared_ptr<console_process::ConsoleProcessInfo> pCPI =
         boost::make_shared<console_process::ConsoleProcessInfo>(
            "Installing Odbc Driver", console_process::InteractionNever);

   // create and execute console process
   boost::shared_ptr<console_process::ConsoleProcess> pCP;
   pCP = console_process::ConsoleProcess::create(
            string_utils::utf8ToSystem(rProgramPath.getAbsolutePath()),
            args,
            options,
            pCPI);

   // return console process
   pResponse->setResult(pCP->toJson(console_process::ClientSerialization));
   return Success();
}

Error initialize()
{
   // register methods
   RS_REGISTER_CALL_METHOD(rs_connectionOpened, 1);
   RS_REGISTER_CALL_METHOD(rs_connectionClosed, 2);
   RS_REGISTER_CALL_METHOD(rs_connectionUpdated, 3);
   RS_REGISTER_CALL_METHOD(rs_availableRemoteServers, 0);
   RS_REGISTER_CALL_METHOD(rs_availableConnections, 0);
   RS_REGISTER_CALL_METHOD(rs_connectionIcon, 1);
   RS_REGISTER_CALL_METHOD(rs_connectionAddPackage, 2);
   RS_REGISTER_CALL_METHOD(rs_embeddedViewer, 1);
   RS_REGISTER_CALL_METHOD(rs_connectionOdbcInstallPath, 0);

   // initialize environment
   initEnvironment();

   // initialize connection history
   Error error = connectionHistory().initialize();
   if (error)
      return error;

   // connect to events to track whether we should enable connections
   s_connectionsInitiallyEnabled = connectionsEnabled();
   module_context::events().onPackageLibraryMutated.connect(
                                             onInstalledPackagesChanged);

   // initialize events for package indexer
   module_context::events().onDeferredInit.connect(onDeferredInit);
   
   // initialize connections index worker
   registerConnectionsWorker();

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "remove_connection", removeConnection))
      (bind(registerRpcMethod, "connection_disconnect", connectionDisconnect))
      (bind(registerRpcMethod, "connection_execute_action", connectionExecuteAction))
      (bind(registerIdleOnlyAsyncRpcMethod, "connection_list_objects", connectionListObjects))
      (bind(registerIdleOnlyAsyncRpcMethod, "connection_list_fields", connectionListFields))
      (bind(registerIdleOnlyAsyncRpcMethod, "connection_preview_object", connectionPreviewObject))
      (bind(module_context::registerUriHandler, "/" kConnectionsPath, 
            handleConnectionsResourceRequest))
      (bind(sourceModuleRFile, "SessionConnectionsInstaller.R"))
      (bind(sourceModuleRFile, "SessionConnections.R"))
      (bind(registerRpcMethod, "install_odbc_driver", installOdbcDriver));

   return initBlock.execute();
}


} // namespace connections
} // namespace modules
} // namespace session
} // namespace rstudio

