/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#include <cstring>

#include "ScriptableInstance.h"
#include "InvokeMessage.h"
#include "ReturnMessage.h"
#include "ServerMethods.h"
#include "AllowedConnections.h"

#include "mozincludes.h"
#include "scoped_ptr/scoped_ptr.h"
#include "NPVariantWrapper.h"

using std::string;
using std::endl;
const static string BACKGROUND_PAGE_STR = "chrome-extension://jpjpnpmbddbjkfaccnmhnkdgjideieim/background.html";
const static string UNKNOWN_STR = "unknown";
const static string INCLUDE_STR = "include";
const static string EXCLUDE_STR = "exclude";

bool ScriptableInstance::jsIdentitySafe = false;

static inline string convertToString(const NPString& str) {
  return string(GetNPStringUTF8Characters(str), GetNPStringUTF8Length(str));
}

string ScriptableInstance::computeTabIdentity() {
  return "";
}

void ScriptableInstance::dumpObjectBytes(NPObject* obj) {
  char buf[20];
  Debug::log(Debug::Debugging) << "   object bytes:\n";
  const unsigned char* ptr = reinterpret_cast<const unsigned char*>(obj);
  for (int i = 0; i < 24; ++i) {
    snprintf(buf, sizeof(buf), " %02x", ptr[i]);
    Debug::log(Debug::Debugging) << buf;
  }
  NPVariant objVar;
  OBJECT_TO_NPVARIANT(obj, objVar);
  Debug::log(Debug::Debugging) << " obj.toString()="
      << NPVariantProxy::toString(objVar) << Debug::flush;
}

ScriptableInstance::ScriptableInstance(NPP npp) : NPObjectWrapper<ScriptableInstance>(npp),
    plugin(*reinterpret_cast<Plugin*>(npp->pdata)),
    _channel(new HostChannel()),
    localObjects(npp,ScriptableInstance::jsIdentitySafe),
    _connectId(NPN_GetStringIdentifier("connect")),
    initID(NPN_GetStringIdentifier("init")),
    toStringID(NPN_GetStringIdentifier("toString")),
    loadHostEntriesID(NPN_GetStringIdentifier("loadHostEntries")),
    locationID(NPN_GetStringIdentifier("location")),
    hrefID(NPN_GetStringIdentifier("href")),
    urlID(NPN_GetStringIdentifier("url")),
    includeID(NPN_GetStringIdentifier("include")),
    getHostPermissionID(NPN_GetStringIdentifier("getHostPermission")),
    testJsIdentityID(NPN_GetStringIdentifier("testJsIdentity")),
    connectedID(NPN_GetStringIdentifier("connected")),
    statsID(NPN_GetStringIdentifier("stats")),
    jsDisconnectedID(NPN_GetStringIdentifier("__gwt_disconnected")),
    jsInvokeID(NPN_GetStringIdentifier("__gwt_jsInvoke")),
    jsResultID(NPN_GetStringIdentifier("__gwt_makeResult")),
    jsTearOffID(NPN_GetStringIdentifier("__gwt_makeTearOff")),
    jsValueOfID(NPN_GetStringIdentifier("valueOf")),
    idx0(NPN_GetIntIdentifier(0)),
    idx1(NPN_GetIntIdentifier(1)) {
  savedValueIdx = -1;
  if (NPN_GetValue(npp, NPNVWindowNPObject, &window) != NPERR_NO_ERROR) {
    Debug::log(Debug::Error) << "Error getting window object" << Debug::flush;
  }

}

ScriptableInstance::~ScriptableInstance() {
  // TODO(jat): free any remaining Java objects held by JS, then make
  // the JS wrapper handle that situation gracefully
  if (window) {
    NPN_ReleaseObject(window);
  }
  for (hash_map<int, JavaObject*>::iterator it = javaObjects.begin(); it != javaObjects.end();
      ++it) {
    Debug::log(Debug::Spam) << "  disconnecting Java wrapper " << it->first << Debug::flush;
    it->second->disconnectPlugin();
  }
  if (_channel) {
    _channel->disconnectFromHost();
    delete _channel;
  }
}

void ScriptableInstance::dumpJSresult(const char* js) {
  NPString npScript;
  dupString(js, npScript);
  NPVariantWrapper wrappedRetVal(*this);
  if (!NPN_Evaluate(getNPP(), window, &npScript, wrappedRetVal.addressForReturn())) {
    Debug::log(Debug::Error) << "   *** dumpJSresult failed" << Debug::flush;
    return;
  }
  Debug::log(Debug::Info) << "dumpWindow=" << wrappedRetVal.toString() << Debug::flush;
}

bool ScriptableInstance::tryGetStringPrimitive(NPObject* obj, NPVariant& result) {
  if (NPN_HasMethod(getNPP(), obj, jsValueOfID)) {
    if (NPN_Invoke(getNPP(), obj, jsValueOfID, 0, 0, &result)
        && NPVariantUtil::isString(result)) {
      return true;
    }
    NPVariantProxy::release(result);
  }
  return false;
}

bool ScriptableInstance::makeResult(bool isException, const Value& value, NPVariant* result) {
  Debug::log(Debug::Debugging) << "makeResult(" << isException << ", " << value << ")"
      << Debug::flush;
  Value temp;
  if (value.getType() == Value::JAVA_OBJECT) {
    int javaId = value.getJavaObjectId();
    // We may have previously released the proxy for the same object id,
    // but have not yet sent a free message back to the server.
    javaObjectsToFree.erase(javaId);
  }
  NPVariantArray varArgs(*this, 3);
  varArgs[0] = isException ? 1 : 0;
  varArgs[1] = value;
  NPVariantWrapper retVal(*this);
  return NPN_Invoke(getNPP(), window, jsResultID, varArgs.getArray(), varArgs.getSize(), result);
}

//=============================================================================
// NPObject methods
//=============================================================================

bool ScriptableInstance::hasProperty(NPIdentifier name) {
  if (!NPN_IdentifierIsString(name)) {
    // all numeric properties are ok, as we assume only JSNI code is making
    // the field access via dispatchID
    return true;
  }
  // TODO: special-case toString tear-offs
  return name == statsID || name == connectedID;
}

bool ScriptableInstance::getProperty(NPIdentifier name, NPVariant* variant) {
  Debug::log(Debug::Debugging) << "ScriptableInstance::getProperty(name="
      << NPN_UTF8FromIdentifier(name) << ")" << Debug::flush;
  bool retVal = false;
  VOID_TO_NPVARIANT(*variant);
  if (name == connectedID) {
    BOOLEAN_TO_NPVARIANT(_channel->isConnected(), *variant);
    retVal = true;
  } else if (name == statsID) {
    NPVariantProxy::assignFrom(*variant, "<stats data>");
    retVal = true;
  }
  if (retVal) {
    // TODO: testing
    Debug::log(Debug::Debugging) << " return value " << *variant
        << Debug::flush;
  }
  return retVal;
}

bool ScriptableInstance::setProperty(NPIdentifier name, const NPVariant* variant) {
  Debug::log(Debug::Debugging) << "ScriptableInstance::setProperty(name="
      << NPN_UTF8FromIdentifier(name) << ", value=" << *variant << ")"
      << Debug::flush; 
  return false;
}

bool ScriptableInstance::hasMethod(NPIdentifier name) {
  Debug::log(Debug::Debugging) << "ScriptableInstance::hasMethod(name=" << NPN_UTF8FromIdentifier(name) << ")"
      << Debug::flush; 
  if (name == _connectId ||
      name == initID ||
      name == toStringID ||
      name == loadHostEntriesID ||
      name == getHostPermissionID ||
      name == testJsIdentityID ) {
    return true;
  }
  return false;
}

bool ScriptableInstance::invoke(NPIdentifier name, const NPVariant* args, unsigned argCount,
    NPVariant* result) {
  NPUTF8* uname = NPN_UTF8FromIdentifier(name);
  Debug::log(Debug::Debugging) << "ScriptableInstance::invoke(name=" << uname << ",#args=" << argCount << ")"
      << Debug::flush;
  VOID_TO_NPVARIANT(*result);
  if (name == _connectId) {
    connect(args, argCount, result);
  } else if (name == initID) {
    init(args, argCount, result);
  } else if (name == toStringID) {
    // TODO(jat): figure out why this doesn't show in Firebug
    string val("[GWT OOPHM Plugin: connected=");
    val += _channel->isConnected() ? 'Y' : 'N';
    val += ']';
    NPVariantProxy::assignFrom(*result, val);
  } else if (name == loadHostEntriesID) {
    loadHostEntries(args, argCount, result);
  } else if (name == getHostPermissionID) {
    getHostPermission(args, argCount, result);
  } else if (name == testJsIdentityID) {
    testJsIdentity(args, argCount, result);
  }
  return true;
}

bool ScriptableInstance::invokeDefault(const NPVariant* args, unsigned argCount,
      NPVariant* result) {
  Debug::log(Debug::Debugging) << "ScriptableInstance::invokeDefault(#args=" << argCount << ")"
      << Debug::flush;
  VOID_TO_NPVARIANT(*result);
  return true;
}

bool ScriptableInstance::enumeration(NPIdentifier** propReturn, uint32_t* count) {
  Debug::log(Debug::Debugging) << "ScriptableInstance::enumeration()" << Debug::flush;
  int n = 2;
  NPIdentifier* props = static_cast<NPIdentifier*>(NPN_MemAlloc(sizeof(NPIdentifier) * n));
  props[0] = connectedID;
  props[1] = statsID;
  *propReturn = props;
  *count = n;
  return true;
}

//=============================================================================
// internal methods
//=============================================================================

void ScriptableInstance::init(const NPVariant* args, unsigned argCount, NPVariant* result) {
  if (argCount != 1 || !NPVariantUtil::isObject(args[0])) {
    // TODO: better failure?
    Debug::log(Debug::Error) << "ScriptableInstance::init called with incorrect arguments:\n";
    for (unsigned i = 0; i < argCount; ++i) {
      Debug::log(Debug::Error) << " " << i << " " << NPVariantProxy::toString(args[i]) << "\n";
    }
    Debug::log(Debug::Error) << Debug::flush;
    result->type = NPVariantType_Void;
    return;
  }
  if (window) {
    NPN_ReleaseObject(window);
  }
  // replace our window object with that passed by the caller
  window = NPVariantUtil::getAsObject(args[0]);
  NPN_RetainObject(window);
  BOOLEAN_TO_NPVARIANT(true, *result);
  result->type = NPVariantType_Bool;
}

string ScriptableInstance::getLocationHref() {
  NPVariantWrapper locationVariant(*this);
  NPVariantWrapper hrefVariant(*this);

  // window.location
  NPN_GetProperty(getNPP(), window, locationID, locationVariant.addressForReturn());
  //window.location.href
  NPN_GetProperty(getNPP(), locationVariant.getAsObject(), hrefID, hrefVariant.addressForReturn());

  const NPString* locationHref = NPVariantUtil::getAsNPString(hrefVariant);
  return convertToString(*locationHref);
}


void ScriptableInstance::loadHostEntries(const NPVariant* args, unsigned argCount, NPVariant* result) {
  string locationHref = getLocationHref();
  if (locationHref.compare(BACKGROUND_PAGE_STR) == 0) {
    AllowedConnections::clearRules();
    for (unsigned i = 0; i < argCount; ++i) {
      //Get the host entry object {url: "somehost.net", include: true/false}
      NPObject* hostEntry = NPVariantUtil::getAsObject(args[i]);
      if (!hostEntry) {
        Debug::log(Debug::Error) << "Got a host entry that is not an object.\n";
        break;
      }

      //Get the url
      NPVariantWrapper urlVariant(*this);
      if (!NPN_GetProperty(getNPP(), hostEntry, urlID, urlVariant.addressForReturn()) ||
          !urlVariant.isString()) {
        Debug::log(Debug::Error) << "Got a host.url entry that is not a string.\n";
        break;
      }
      const NPString* urlNPString = urlVariant.getAsNPString();
      string urlString = convertToString(*urlNPString);

      //Include/Exclude?
      NPVariantWrapper includeVariant(*this);
      if (!NPN_GetProperty(getNPP(), hostEntry, includeID, includeVariant.addressForReturn()) || 
          !includeVariant.isBoolean()) {
        Debug::log(Debug::Error) << "Got a host.include entry that is not a boolean.\n";
        break;
      }
      bool include = includeVariant.getAsBoolean();
      Debug::log(Debug::Info) << "Adding " << urlString << "(" << (include ? "include" : "exclude") << ")\n";

      int slash = urlString.find( '/' );
      if( slash == std::string::npos ) {
        AllowedConnections::addRule(urlString, "localhost", !include);
      } else {
        AllowedConnections::addRule(urlString.substr( 0, slash), urlString.substr(slash+1), !include);
      }
    }
  } else {
    Debug::log(Debug::Error) << "ScriptableInstance::loadHostEntries called from outside the background page: " <<
                             locationHref << "\n";
  }
}

void ScriptableInstance::getHostPermission(const NPVariant* args, unsigned argCount, NPVariant* result) {
  if (argCount != 1 || !NPVariantUtil::isString(args[0])) {
    Debug::log(Debug::Error) << "ScriptableInstance::getHostPermission called with incorrect arguments.\n";
  }

  const NPString url = args[0].value.stringValue;
  const string urlStr = convertToString(url);
  bool allowed = false;

  Debug::log(Debug::Info) << "getHostPermission() url " << urlStr << Debug::flush;
  bool matches = AllowedConnections::matchesRule(
      AllowedConnections::getHostFromUrl(urlStr),
      AllowedConnections::getCodeServerFromUrl(urlStr),
      &allowed);
  string retStr;
  if (!matches) {
    retStr = UNKNOWN_STR;
  } else if (allowed) {
    retStr = INCLUDE_STR;
  } else {
    retStr = EXCLUDE_STR;
  }

  NPVariantProxy(*this, *result) = retStr;
}

void ScriptableInstance::testJsIdentity(const NPVariant* args, unsigned argCount, NPVariant* result) {
  if (argCount != 2 || !NPVariantUtil::isObject(args[0]) ||
      !NPVariantUtil::isObject(args[1]) ) {
    Debug::log(Debug::Error) << "ScriptableInstance::testJsIdentity called"
        << " with incorrect arguments.\n";
  }
  NPObject* obj1 = NPVariantUtil::getAsObject(args[0]);
  NPObject* obj2 = NPVariantUtil::getAsObject(args[1]);
  Debug::log(Debug::Info) << "obj1:" << obj1 << " obj2:" << obj2
      << Debug::flush;
  if( obj1 == obj2 ) {
    Debug::log(Debug::Info) << "Idenity check passed; not using expando!"
        << Debug::flush;
    ScriptableInstance::jsIdentitySafe = true;
  } else {
    Debug::log(Debug::Info) << "Idenity check failed; using expando"
        << Debug::flush;
    ScriptableInstance::jsIdentitySafe = false;
  }
}


void ScriptableInstance::connect(const NPVariant* args, unsigned argCount, NPVariant* result) {
  if (argCount != 5 || !NPVariantUtil::isString(args[0])
      || !NPVariantUtil::isString(args[1])
      || !NPVariantUtil::isString(args[2])
      || !NPVariantUtil::isString(args[3])
      || !NPVariantUtil::isString(args[4])) {
    // TODO: better failure?
    Debug::log(Debug::Error) << "ScriptableInstance::connect called with incorrect arguments:\n";
    for (unsigned i = 0; i < argCount; ++i) {
      Debug::log(Debug::Error) << " " << i << " " << NPVariantProxy::toString(args[i]) << "\n";
    }
    Debug::log(Debug::Error) << Debug::flush;
    result->type = NPVariantType_Void;
    return;
  }

  // application provided URL string used for user facing things like the
  // devmode tab title
  const NPString appUrl = args[0].value.stringValue;
  const string appUrlStr = convertToString(appUrl);

  // window.location.href provided URL. (used for security)
  const string urlStr = getLocationHref();

  const NPString sessionKey = args[1].value.stringValue;
  const NPString hostAddr = args[2].value.stringValue;
  const NPString moduleName = args[3].value.stringValue;
  const NPString hostedHtmlVersion = args[4].value.stringValue;
  Debug::log(Debug::Info) << "ScriptableInstance::connect(url=" << NPVariantProxy::toString(args[0])
      << ",sessionKey=" << NPVariantProxy::toString(args[1]) << ",host=" << NPVariantProxy::toString(args[2])
      << ",module=" << NPVariantProxy::toString(args[3]) << ",hostedHtmlVers=" << NPVariantProxy::toString(args[4])
      << ")" << Debug::flush;

  bool allowed = false;
  AllowedConnections::matchesRule(
      AllowedConnections::getHostFromUrl(urlStr),
      AllowedConnections::getCodeServerFromUrl(appUrlStr),
      &allowed);
  if (!allowed) {
    BOOLEAN_TO_NPVARIANT(false, *result);
    result->type = NPVariantType_Bool;
    return;
  }

  bool connected = false;
  unsigned port = 9997;  // TODO(jat): should there be a default?
  int n = GetNPStringUTF8Length(hostAddr);
  scoped_ptr<char> host(new char[n + 1]);
  const char* s = GetNPStringUTF8Characters(hostAddr);
  char* d = host.get();
  while (n > 0 && *s != ':') {
    n--;
    *d++ = *s++;
  }
  *d = 0;
  if (n > 0) {
    port = atoi(s + 1);
  }
  Debug::log(Debug::Info) << "  host=" << host.get() << ",port=" << port << Debug::flush;


  if (!_channel->connectToHost(host.get(), port)) {
    BOOLEAN_TO_NPVARIANT(false, *result);
    result->type = NPVariantType_Bool;
  }

  string hostedHtmlVersionStr = convertToString(hostedHtmlVersion);
  if (!_channel->init(this, BROWSERCHANNEL_PROTOCOL_VERSION,
      BROWSERCHANNEL_PROTOCOL_VERSION, hostedHtmlVersionStr)) {
    BOOLEAN_TO_NPVARIANT(false, *result);
    result->type = NPVariantType_Bool;
  }

  string moduleNameStr = convertToString(moduleName);
  string userAgent(NPN_UserAgent(getNPP()));
  string tabKeyStr = computeTabIdentity();
  string sessionKeyStr = convertToString(sessionKey);
  Debug::log(Debug::Debugging) << "  connected, sending loadModule" << Debug::flush;
  connected = LoadModuleMessage::send(*_channel, appUrlStr, tabKeyStr, sessionKeyStr,
      moduleNameStr, userAgent, this);
  BOOLEAN_TO_NPVARIANT(connected, *result);
  result->type = NPVariantType_Bool;
}

int ScriptableInstance::getLocalObjectRef(NPObject* obj) {
  int id = localObjects.getObjectId(obj);
  if(id == -1) {
    id = localObjects.add(obj);
  }
  return id;
}

void ScriptableInstance::fatalError(HostChannel& channel, const string& message) {
  // TODO(jat): better error handling
  Debug::log(Debug::Error) << "Fatal error: " << message << Debug::flush;
}

void ScriptableInstance::dupString(const char* str, NPString& npString) {
  npString.UTF8Length = static_cast<uint32_t>(strlen(str));
  NPUTF8* chars = static_cast<NPUTF8*>(NPN_MemAlloc(npString.UTF8Length));
  memcpy(chars, str, npString.UTF8Length);
  npString.UTF8Characters = chars;
}

// SessionHandler methods
void ScriptableInstance::freeValue(HostChannel& channel, int idCount, const int* const ids) {
  Debug::log(Debug::Debugging) << "freeValue(#ids=" << idCount << ")" << Debug::flush;
  for (int i = 0; i < idCount; ++i) {
    Debug::log(Debug::Spam) << " id=" << ids[i] << Debug::flush;
    localObjects.free(ids[i]);
  }
}

void ScriptableInstance::loadJsni(HostChannel& channel, const string& js) {
  NPString npScript;
  dupString(js.c_str(), npScript);
  NPVariantWrapper npResult(*this);
  Debug::log(Debug::Spam) << "loadJsni - \n" << js << Debug::flush;
  if (!NPN_Evaluate(getNPP(), window, &npScript, npResult.addressForReturn())) {
    Debug::log(Debug::Error) << "loadJsni failed\n" << js << Debug::flush;
  }
}

Value ScriptableInstance::clientMethod_getProperty(HostChannel& channel, int numArgs, const Value* const args) {
  if (numArgs != 2 || !args[0].isInt() || (!args[1].isString() && !args[1].isInt())) {
    Debug::log(Debug::Error) << "Incorrect invocation of getProperty: #args=" << numArgs << ":";
    for (int i = 0; i < numArgs; ++i) {
      Debug::log(Debug::Error) << " " << i << "=" << args[i].toString();
    }
    Debug::log(Debug::Error) << Debug::flush;
    return Value();
  }
  int id = args[0].getInt();
  NPObject* obj = localObjects.getById(id);
  NPIdentifier propID;
  if (args[1].isString()) {
    string propName = args[1].getString();
    propID = NPN_GetStringIdentifier(propName.c_str());
  } else {
    int propNum = args[1].getInt();
    propID = NPN_GetIntIdentifier(propNum);
  }
  NPVariantWrapper npResult(*this);
  if (!NPN_GetProperty(getNPP(), obj, propID, npResult.addressForReturn())) {
    Debug::log(Debug::Warning) << "getProperty(id=" << id << ", prop="
        << NPN_UTF8FromIdentifier(propID) << ") failed" << Debug::flush;
    return Value();
  }
  return npResult.getAsValue(*this);
}

Value ScriptableInstance::clientMethod_setProperty(HostChannel& channel, int numArgs, const Value* const args) {
  if (numArgs != 2 || !args[0].isInt() || (!args[1].isString() && !args[1].isInt())) {
    Debug::log(Debug::Error) << "Incorrect invocation of setProperty: #args="
        << numArgs << ":";
    for (int i = 0; i < numArgs; ++i) {
      Debug::log(Debug::Error) << " " << i << "=" << args[i].toString();
    }
    Debug::log(Debug::Error) << Debug::flush;
    return Value();
  }
  int id = args[0].getInt();
  NPObject* obj = localObjects.getById(id);
  NPIdentifier propID;
  if (args[1].isString()) {
    string propName = args[1].getString();
    propID = NPN_GetStringIdentifier(propName.c_str());
  } else {
    int propNum = args[1].getInt();
    propID = NPN_GetIntIdentifier(propNum);
  }
  NPVariantWrapper npValue(*this);
  npValue.operator=(args[2]);
  if (!NPN_SetProperty(getNPP(), obj, propID, npValue.address())) {
    Debug::log(Debug::Warning) << "setProperty(id=" << id << ", prop="
        << NPN_UTF8FromIdentifier(propID) << ", val=" << args[2].toString()
        << ") failed" << Debug::flush;
    return Value();
  }
  return Value();
}

/**
 * SessionHandler.invoke - used by LoadModule and reactToMessages* to process server-side
 * requests to invoke methods in Javascript or the plugin.
 */
bool ScriptableInstance::invokeSpecial(HostChannel& channel, SpecialMethodId dispatchId,
    int numArgs, const Value* const args, Value* returnValue) {
  switch (dispatchId) {
  case SessionHandler::HasMethod:
  case SessionHandler::HasProperty:
    break;
  case SessionHandler::SetProperty:
    *returnValue = clientMethod_setProperty(channel, numArgs, args);
    return false;
  case SessionHandler::GetProperty:
    *returnValue = clientMethod_getProperty(channel, numArgs, args);
    return false;
  default:
    break;
  }
  Debug::log(Debug::Error) << "Unexpected special method " << dispatchId
      << " called on plugin; #args=" << numArgs << ":";
  for (int i = 0; i < numArgs; ++i) {
    Debug::log(Debug::Error) << " " << i << "=" << args[i].toString();
  }
  Debug::log(Debug::Error) << Debug::flush;
  // TODO(jat): should we create a real exception object?
  string buf("unexpected invokeSpecial(");
  buf += static_cast<int>(dispatchId);
  buf += ")";
  returnValue->setString(buf);
  return true;
}

bool ScriptableInstance::invoke(HostChannel& channel, const Value& thisRef,
    const string& methodName, int numArgs, const Value* const args,
    Value* returnValue) {
  Debug::log(Debug::Debugging) << "invokeJS(" << methodName << ", this=" 
      << thisRef.toString() << ", numArgs=" << numArgs << ")" << Debug::flush;
  NPVariantArray varArgs(*this, numArgs + 2);
  varArgs[0] = thisRef;
  varArgs[1] = methodName;
  for (int i = 0; i < numArgs; ++i) {
    varArgs[i + 2] = args[i];
  }
  const NPVariant* argArray = varArgs.getArray();
  if (Debug::level(Debug::Spam)) {
    for (int i = 0; i < varArgs.getSize(); ++i) {
      Debug::log(Debug::Spam) << "  arg " << i << " is "
          << NPVariantProxy::toString(argArray[i]) << Debug::flush;
    }
  }
  NPVariantWrapper wrappedRetVal(*this);
  if (!NPN_Invoke(getNPP(), window, jsInvokeID, argArray, varArgs.getSize(),
      wrappedRetVal.addressForReturn())) {
    Debug::log(Debug::Error) << "*** invokeJS(" << methodName << ", this="
        << thisRef.toString() << ", numArgs=" << numArgs << ") failed"
        << Debug::flush;
    // TODO(jat): should we create a real exception object?
    returnValue->setString("invoke of " + methodName + " failed");
    return true;
  }
  Debug::log(Debug::Spam) << "  wrapped return is " << wrappedRetVal.toString() << Debug::flush;
  NPVariantWrapper exceptFlag(*this);
  NPVariantWrapper retval(*this);
  NPObject* wrappedArray = wrappedRetVal.getAsObject();
  if (!NPN_GetProperty(getNPP(), wrappedArray, idx0, exceptFlag.addressForReturn())) {
    Debug::log(Debug::Error) << " Error getting element 0 of wrapped return value ("
        << wrappedRetVal << ") on call to " << methodName << Debug::flush;
  }
  if (!NPN_GetProperty(getNPP(), wrappedArray, idx1, retval.addressForReturn())) {
    Debug::log(Debug::Error) << " Error getting element 1 of wrapped return value ("
        << wrappedRetVal << ") on call to " << methodName << Debug::flush;
  }
  Debug::log(Debug::Debugging) << "  return value " << retval.toString() << Debug::flush;
  *returnValue = retval.getAsValue(*this);
  if (exceptFlag.isInt() && exceptFlag.getAsInt() != 0) {
    Debug::log(Debug::Debugging) << "  exception: " << retval << Debug::flush;
    return true;
  }
  return false;
}

bool ScriptableInstance::JavaObject_invoke(int objectId, int dispId,
    const NPVariant* args, uint32_t numArgs, NPVariant* result) {
  Debug::log(Debug::Debugging) << "JavaObject_invoke(dispId= " << dispId << ", numArgs=" << numArgs << ")" << Debug::flush;
  if (Debug::level(Debug::Spam)) {
    for (uint32_t i = 0; i < numArgs; ++i) {
      Debug::log(Debug::Spam) << "  " << i << " = " << args[i] << Debug::flush;
    }
  }

  bool isRawToString = false;
  if (dispId == -1) {
    dispId = 0;
    isRawToString = true;
  }

  Value javaThis;
  javaThis.setJavaObject(objectId);
  scoped_array<Value> vargs(new Value[numArgs]);
  for (unsigned i = 0; i < numArgs; ++i) {
    vargs[i] = NPVariantProxy::getAsValue(args[i], *this);
  }
  bool isException = false;
  Value returnValue;
  if (!InvokeMessage::send(*_channel, javaThis, dispId, numArgs, vargs.get())) {
    Debug::log(Debug::Error) << "JavaObject_invoke: failed to send invoke message" << Debug::flush;
  } else {
    Debug::log(Debug::Debugging) << " return from invoke" << Debug::flush;
    scoped_ptr<ReturnMessage> retMsg(_channel->reactToMessagesWhileWaitingForReturn(this));
    if (!retMsg.get()) {
      Debug::log(Debug::Error) << "JavaObject_invoke: failed to get return value" << Debug::flush;
    } else {
      if (isRawToString) {
        // toString() needs the raw value
        NPVariantProxy::assignFrom(*this, *result, retMsg->getReturnValue());
        return !retMsg->isException();
      }
      isException = retMsg->isException();
      returnValue = retMsg->getReturnValue();
    }
  }
  // Wrap the result
  return makeResult(isException, returnValue, result);
}

bool ScriptableInstance::JavaObject_getProperty(int objectId, int dispId,
    NPVariant* result) {
  Debug::log(Debug::Debugging) << "JavaObject_getProperty(objectid="
      << objectId << ", dispId=" << dispId << ")" << Debug::flush;
  VOID_TO_NPVARIANT(*result);
  Value propertyValue = ServerMethods::getProperty(*_channel, this, objectId, dispId);
  if (propertyValue.isJsObject()) {
    // TODO(jat): special-case for testing
    NPObject* npObj = localObjects.getById(propertyValue.getJsObjectId());
    OBJECT_TO_NPVARIANT(npObj, *result);
    NPN_RetainObject(npObj);
  } else {
    NPVariantProxy::assignFrom(*this, *result, propertyValue);
  }
  Debug::log(Debug::Debugging) << " return val=" << propertyValue
      << ", NPVariant=" << *result << Debug::flush;
  if (NPVariantUtil::isObject(*result)) {
    dumpObjectBytes(NPVariantUtil::getAsObject(*result));
  }
  if (NPVariantUtil::isObject(*result)) {
    Debug::log(Debug::Debugging) << "  final return refcount = "
        << NPVariantUtil::getAsObject(*result)->referenceCount << Debug::flush;
  }
  return true;
}

bool ScriptableInstance::JavaObject_setProperty(int objectId, int dispId,
    const NPVariant* npValue) {
  Debug::log(Debug::Debugging) << "JavaObject_setProperty(objectid="
      << objectId << ", dispId=" << dispId << ", value=" << *npValue << ")" << Debug::flush;
  if (NPVariantUtil::isObject(*npValue)) {
    Debug::log(Debug::Debugging) << "  before localObj: refcount = "
        << NPVariantUtil::getAsObject(*npValue)->referenceCount << Debug::flush;
  }
  Value value = NPVariantProxy::getAsValue(*npValue, *this, true);
  if (NPVariantUtil::isObject(*npValue)) {
    Debug::log(Debug::Debugging) << "  after localObj: refcount = "
        << NPVariantUtil::getAsObject(*npValue)->referenceCount << Debug::flush;
  }
  if (NPVariantUtil::isObject(*npValue)) {
    dumpObjectBytes(NPVariantUtil::getAsObject(*npValue));
  }
  Debug::log(Debug::Debugging) << "  as value: " << value << Debug::flush;
  // TODO: no way to set an actual exception object! (Could ClassCastException on server.)
  return ServerMethods::setProperty(*_channel, this, objectId, dispId, value);
}

bool ScriptableInstance::JavaObject_getToStringTearOff(NPVariant* result) {
  Debug::log(Debug::Debugging) << "JavaObject_getToStringTearOff()" << Debug::flush;
  VOID_TO_NPVARIANT(*result);

  Value temp;
  NPVariantArray varArgs(*this, 3);
  temp.setNull();  varArgs[0] = temp; // proxy: no proxy needed
  temp.setInt(0);  varArgs[1] = temp; // dispId: always 0 for toString()
  temp.setInt(0);  varArgs[2] = temp; // argCount: always 0 for toString()

  if (!NPN_Invoke(getNPP(), window, jsTearOffID, varArgs.getArray(), 3, result)) {
    Debug::log(Debug::Error) << "*** JavaObject_getToStringTearOff() failed"
        << Debug::flush;
    return true;
  }
  return true;
}

JavaObject* ScriptableInstance::createJavaWrapper(int objectId) {
  Debug::log(Debug::Debugging) << "createJavaWrapper(objectId=" << objectId <<  ")" << Debug::flush;
  JavaObject* jObj;
  hash_map<int, JavaObject*>::iterator it = javaObjects.find(objectId);
  if (it != javaObjects.end()) {
    jObj = it->second;
    NPN_RetainObject(jObj);
    return jObj;
  }
  jObj = JavaObject::create(this, objectId);
  javaObjects[objectId] = jObj;
  return jObj;
}

void ScriptableInstance::destroyJavaWrapper(JavaObject* jObj) {
  int objectId = jObj->getObjectId();
  if (!javaObjects.erase(objectId)) {
    Debug::log(Debug::Error) << "destroyJavaWrapper(id=" << objectId
        << "): trying to free unknown JavaObject" << Debug::flush;
  }
  Debug::log(Debug::Debugging) << "destroyJavaWrapper(id=" << objectId << ")" << Debug::flush;
  javaObjectsToFree.insert(objectId);
}

void ScriptableInstance::disconnectDetectedImpl() {
  NPVariantWrapper result(*this);
  NPN_Invoke(getNPP(), window, jsDisconnectedID, 0, 0, result.addressForReturn());
}

void ScriptableInstance::sendFreeValues(HostChannel& channel) {
  unsigned n = javaObjectsToFree.size();
  if (n) {
    scoped_array<int> ids(new int[n]);
    int i = 0;
    for (std::set<int>::iterator it = javaObjectsToFree.begin();
        it != javaObjectsToFree.end(); ++it) {
      ids[i++] = *it;
    }
    if (ServerMethods::freeJava(channel, this, n, ids.get())) {
      javaObjectsToFree.clear();
    }
  }
}
