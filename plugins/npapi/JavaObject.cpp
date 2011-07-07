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

#include "Debug.h"

#include "JavaObject.h"
#include "Plugin.h"
#include "NPVariantUtil.h"
#include "NPVariantWrapper.h"

using std::string;

static string IdentifierName(NPIdentifier name) {
  string iname;
  if (NPN_IdentifierIsString(name)) {
    iname = NPN_UTF8FromIdentifier(name);
  } else {
    char buf[50];
    snprintf(buf, sizeof(buf), "%d", NPN_IntFromIdentifier(name));
    iname = buf;
  }
  return iname;
}

JavaObject* JavaObject::create(ScriptableInstance* plugin, int id) {
  Debug::log(Debug::Spam) << "Creating Java object id=" << id;
  NPClass* npClass = GetNPClass<JavaObject>();
  NPObject* obj = NPN_CreateObject(plugin->getNPP(), npClass);
  Debug::log(Debug::Spam) << " addr=" << obj << Debug::flush;
  JavaObject* jObj = static_cast<JavaObject*>(obj);
  jObj->setObjectId(id);
  return jObj;
}

bool JavaObject::isInstance(NPObject* obj) {
  return obj->_class == GetNPClass<JavaObject>();
}

JavaObject::~JavaObject() {
  if (plugin) {
    plugin->destroyJavaWrapper(this);
  } else {
    Debug::log(Debug::Spam) << "Destroying JavaObject " << objectId << " after plugin destroyed"
        << Debug::flush;
  }
}

bool JavaObject::enumeration(NPIdentifier** propReturn, uint32_t* count) {
  Debug::log(Debug::Debugging) << "JavaObject::enumeration(" << objectId << ")"
      << Debug::flush;
  int n = 1;
  NPIdentifier* props = static_cast<NPIdentifier*>(NPN_MemAlloc(sizeof(NPIdentifier) * n));
  props[0] = idID;
  *propReturn = props;
  *count = n;
  return true;
}

bool JavaObject::getProperty(NPIdentifier prop, NPVariant *result) {
  if (!plugin) {
    Debug::log(Debug::Spam) << "Ignoring getProperty on " << objectId << " after plugin destroyed"
        << Debug::flush;
    VOID_TO_NPVARIANT(*result);
    return true;
  }
  Debug::log(Debug::Spam) << "JavaObject::getProperty(" << objectId << ")" << Debug::flush;
  if (NPN_IdentifierIsString(prop)) {
    if (prop == plugin->toStringID) {
      return plugin->JavaObject_getToStringTearOff(result);
    }
    if (prop == idID) {
      INT32_TO_NPVARIANT(objectId, *result);
      return true;
    }
    // all other properties are numeric dispatchIDs
    return false;
  }
  int dispId = NPN_IntFromIdentifier(prop);
  // JavaObject_getProperty will retain the return value if needed.
  return plugin->JavaObject_getProperty(objectId, dispId, result);
}

bool JavaObject::hasMethod(NPIdentifier method) {
  if (!plugin) {
    Debug::log(Debug::Spam) << "Ignoring hasMethod on " << objectId << " after plugin destroyed"
        << Debug::flush;
    return true;
  }
  Debug::log(Debug::Spam) << "JavaObject::hasMethod(" << objectId << ", method="
      << IdentifierName(method) << ")" << Debug::flush;
  return false;
//  return !NPN_IdentifierIsString(method);
}

bool JavaObject::hasProperty(NPIdentifier prop) {
  if (!plugin) {
    Debug::log(Debug::Spam) << "Ignoring hasProperty on " << objectId << " after plugin destroyed"
        << Debug::flush;
    return true;
  }
  Debug::log(Debug::Spam) << "JavaObject::hasProperty(" << objectId << ", prop="
      << IdentifierName(prop) << ")" << Debug::flush;
  return !NPN_IdentifierIsString(prop) || prop == idID || prop == plugin->toStringID;
}

bool JavaObject::invokeDefault(const NPVariant *args, uint32_t argCount, NPVariant *result) {
  if (argCount < 2 || !NPVariantUtil::isInt(args[0])
      || (!NPVariantUtil::isNull(args[1]) && !NPVariantUtil::isObject(args[1]))) {
    Debug::log(Debug::Error) << "incorrect arguments to invokeDefault" << Debug::flush;
    return false;
  }
  if (!plugin) {
    Debug::log(Debug::Spam) << "Ignoring invokeDefault on " << objectId << " after plugin destroyed"
        << Debug::flush;
    VOID_TO_NPVARIANT(*result);
    return true;
  }
  Debug::log(Debug::Debugging) << "JavaObject::invokeDefault(" << objectId;
  for (uint32_t i = 0; i < argCount; ++i) {
    Debug::log(Debug::Debugging) << ", " << NPVariantProxy::toString(args[i]);
  }
  Debug::log(Debug::Debugging) << ")" << Debug::flush;
  int dispId = NPVariantUtil::getAsInt(args[0]);
  int objId = objectId;
  if (!NPVariantUtil::isNull(args[1])) {
    NPObject* thisObj = NPVariantUtil::getAsObject(args[1]);
    if (isInstance(thisObj)) {
      JavaObject* thisJavaObj = static_cast<JavaObject*>(thisObj);
      objId = thisJavaObj->objectId;
    }
  }
  // JavaObject_invoke will retain the return value if needed.
  return plugin->JavaObject_invoke(objId, dispId, args + 2, argCount - 2, result);
}

bool JavaObject::invoke(NPIdentifier name, const NPVariant *args,
    uint32_t argCount, NPVariant *result) {
  VOID_TO_NPVARIANT(*result);
  if (!plugin) {
    Debug::log(Debug::Spam) << "Ignoring invoke on " << objectId << " after plugin destroyed"
        << Debug::flush;
    return true;
  }
  string methodName(NPN_UTF8FromIdentifier(name));
  Debug::log(Debug::Spam) << "JavaObject::invoke(" << objectId << ", method="
      << methodName << ")" << Debug::flush;
  if (name == plugin->toStringID) {
    // -1 is magic and means a raw toString().
    return plugin->JavaObject_invoke(objectId, -1, args, argCount, result);
  }
  // toString is the only method we support invoking directly on a Java wrapper
  return false;
}

bool JavaObject::setProperty(NPIdentifier prop, const NPVariant *value) {
  if (!plugin) {
    Debug::log(Debug::Spam) << "Ignoring setProperty on " << objectId << " after plugin destroyed"
        << Debug::flush;
    return true;
  }
  Debug::log(Debug::Spam) << "JavaObject::setProperty(" << objectId << ", val="
      << NPVariantProxy::toString(*value) << ")" << Debug::flush;
  if (NPN_IdentifierIsString(prop)) {
    // any non-numeric properties are read-only
    return false;
  }
  int dispId = NPN_IntFromIdentifier(prop); 
  return plugin->JavaObject_setProperty(objectId, dispId, value);
}
