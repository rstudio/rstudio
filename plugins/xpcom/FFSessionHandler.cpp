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

#include "FFSessionHandler.h"
#include "HostChannel.h"
#include "JavaObject.h"
#include "JSRunner.h"
#include "Debug.h"
#include "XpcomDebug.h"
#include "scoped_ptr/scoped_ptr.h"
#include "RootedObject.h"
#include "InvokeMessage.h"
#include "ServerMethods.h"
#include "AllowedConnections.h"

#include "jsapi.h"
#include "nsCOMPtr.h"
#include "nsStringAPI.h"
#include "nsIJSContextStack.h"
#include "nsIPrincipal.h"
#include "nsServiceManagerUtils.h"

#if GECKO_VERSION >= 2000
#define JS_RemoveRootRT js_RemoveRoot
static inline bool INT_FITS_IN_JSVAL(int i) {
  return (i >= JSVAL_INT_MIN) && (i <= JSVAL_INT_MAX);
}
#endif //GECKO_VERSION

static JSContext* getJSContext() {
  // Get JSContext from stack.
  nsCOMPtr<nsIJSContextStack> stack =
      do_GetService("@mozilla.org/js/xpc/ContextStack;1");
  if (!stack) {
    return NULL;
  }

  JSContext *cx;
  if (NS_FAILED(stack->Peek(&cx))) {
    return NULL;
  }

  if (cx == nsnull) {
    // TODO(jat): figure out why this can be null at plugin unload time
    Debug::log(Debug::Error) << "GWT Dev Plugin: Null JS context" << Debug::flush;
  }

  return cx;
}

FFSessionHandler::FFSessionHandler(HostChannel* channel)
    : SessionData(channel, this, getJSContext()), jsObjectId(0),
    jsObjectsById(NULL), stringObjectClass(NULL) {
  Debug::log(Debug::Debugging) << "FFSessionHandler::FFSessionHandler(this="
      << this << ")" << Debug::flush;
  // TODO(jat): is there a way to avoid calling this twice, without keeping
  // JSContext in an instance field?
  JSContext* ctx = getJSContext();
  if (!JS_AddNamedObjectRoot(ctx, &jsObjectsById, "jsObjectsById")) {
    Debug::log(Debug::Error) << "Error rooting jsObjectsById" << Debug::flush;
  }
  jsObjectsById = JS_NewArrayObject(ctx, 0, NULL);
  if (!jsObjectsById) {
    Debug::log(Debug::Error) << "Error rooting jsObjectsById" << Debug::flush;
  }
  if (!JS_AddNamedValueRoot(ctx, &toStringTearOff, "toStringTearOff")) {
    Debug::log(Debug::Error) << "Error rooting toStringTearOff" << Debug::flush;
  }
  getStringObjectClass(ctx);
  getToStringTearOff(ctx);
}

void FFSessionHandler::getStringObjectClass(JSContext* ctx) {
  jsval str = JS_GetEmptyStringValue(ctx);
  JSObject* obj = 0;
  if (!JS_ValueToObject(ctx, str, &obj)) {
    return;
  }
  if (!obj) {
    return;
  }
  stringObjectClass = JS_GET_CLASS(ctx, obj);
}

void FFSessionHandler::getToStringTearOff(JSContext* ctx) {
  jsval funcVal;

  Debug::log(Debug::Debugging) << "Getting function \"__gwt_makeTearOff\""
        << Debug::flush;

  if (!JS_GetProperty(ctx, global, "__gwt_makeTearOff", &funcVal)
      || funcVal == JSVAL_VOID) {
    Debug::log(Debug::Error) << "Could not get function \"__gwt_makeTearOff\""
        << Debug::flush;
    return;
  }
  jsval jsargs[3] = {
    JSVAL_NULL,                                     // no proxy
    INT_TO_JSVAL(InvokeMessage::TOSTRING_DISP_ID),  // dispId
    JSVAL_ZERO                                      // arg count is zero
  };
  if (!JS_CallFunctionValue(ctx, global, funcVal, 3, jsargs, &toStringTearOff)) {
    jsval exc;
    if (JS_GetPendingException(ctx, &exc)) {
      Debug::log(Debug::Error)
          << "__gwt_makeTearOff(null,0,0) threw exception "
          << dumpJsVal(ctx, exc) << Debug::flush;
    } else {
      Debug::log(Debug::Error) << "Error creating toString tear-off"
          << Debug::flush;
    }
    // TODO(jat): show some crash page and die
  }
}

FFSessionHandler::~FFSessionHandler(void) {
  Debug::log(Debug::Debugging) << "FFSessionHandler::~FFSessionHandler(this="
      << this << ")" << Debug::flush;
  disconnect();
  if (runtime) {
    JS_RemoveRootRT(runtime, &jsObjectsById);
    jsObjectsById = NULL;
    JS_RemoveRootRT(runtime, &toStringTearOff);
    runtime = NULL;
  }
}

void FFSessionHandler::disconnectDetectedImpl() {
  JSContext* ctx = getJSContext();
  if (!ctx) {
    return;
  }

  Debug::log(Debug::Debugging) << "Getting function \"__gwt_disconnected\""
        << Debug::flush;

  jsval funcVal;
  if (!JS_GetProperty(ctx, global, "__gwt_disconnected", &funcVal)
      || funcVal == JSVAL_VOID) {
    Debug::log(Debug::Error) << "Could not get function \"__gwt_disconnected\""
        << Debug::flush;
    return;
  }
  jsval rval;
  JS_CallFunctionValue(ctx, global, funcVal, 0, 0, &rval);
}

void FFSessionHandler::freeValue(HostChannel& channel, int idCount, const int* ids) {
  Debug::DebugStream& dbg = Debug::log(Debug::Spam)
      << "FFSessionHandler::freeValue [ ";
  JSContext* ctx = getJSContext();

  for (int i = 0; i < idCount; ++i) {
    int objId = ids[i];
    dbg << objId << " ";
    jsval toRemove;
    if (JS_GetElement(ctx, jsObjectsById, objId, &toRemove) && JSVAL_IS_OBJECT(toRemove)) {
      jsIdsByObject.erase(identityFromObject(JSVAL_TO_OBJECT(toRemove)));
      JS_DeleteElement(ctx, jsObjectsById, objId);
    } else {
      Debug::log(Debug::Error) << "Error deleting js objId=" << objId << Debug::flush;
    }
  }

  dbg << "]" << Debug::flush;
}

void FFSessionHandler::loadJsni(HostChannel& channel, const std::string& js) {
  Debug::log(Debug::Spam) << "FFSessionHandler::loadJsni " << js << "(EOM)" << Debug::flush;
  JSContext* ctx = getJSContext();
  if (!JSRunner::eval(ctx, global, js)) {
    Debug::log(Debug::Error) << "Error executing script" << Debug::flush;
  }
}

void FFSessionHandler::sendFreeValues(HostChannel& channel) {
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

void FFSessionHandler::fatalError(HostChannel& channel,
    const std::string& message) {
  // TODO(jat): implement
}

bool FFSessionHandler::invoke(HostChannel& channel, const gwt::Value& thisObj, const std::string& methodName,
    int numArgs, const gwt::Value* const args, gwt::Value* returnValue) {
  Debug::log(Debug::Debugging) << "FFSessionHandler::invoke " << thisObj.toString()
      << "::" << methodName << Debug::flush;
  JSContext* ctx = getJSContext();

  // Used to root JSthis and args while making the JS call
  // TODO(jat): keep one object and just keep a "stack pointer" into that
  // object on the native stack so we don't keep allocating/rooting/freeing
  // an object
  RootedObject argsRoot(ctx, "FFSessionhandler::invoke");
  argsRoot = JS_NewArrayObject(ctx, 0, NULL);
  if (!JS_SetArrayLength(ctx, argsRoot.get(), numArgs + 1)) {
    Debug::log(Debug::Error)
        << "FFSessionhandler::invoke - could not set argsRoot length"
        << Debug::flush;
    return true;
  }

  jsval jsThis;
  if (thisObj.isNull()) {
    jsThis = OBJECT_TO_JSVAL(global);
    Debug::log(Debug::Error) << " using global object for this" << Debug::flush;
  } else {
    makeJsvalFromValue(jsThis, ctx, thisObj);
    if (Debug::level(Debug::Spam)) {
      Debug::log(Debug::Spam) << "  obj=" << dumpJsVal(ctx, jsThis)
          << Debug::flush;
    }
  }
  if (!JS_SetElement(ctx, argsRoot.get(), 0, &jsThis)) {
    Debug::log(Debug::Error)
        << "FFSessionhandler::invoke - could not set argsRoot[0] to this"
        << Debug::flush;
    return true;
  }

  jsval funcVal;
  // TODO: handle non-ASCII method names
  if (!JS_GetProperty(ctx, global, methodName.c_str(), &funcVal)
      || funcVal == JSVAL_VOID) {
    Debug::log(Debug::Error) << "Could not get function " << methodName
        << Debug::flush;
    return true;
  } 

  scoped_array<jsval> jsargs(new jsval[numArgs]);
  for (int i = 0; i < numArgs; ++i) {
    makeJsvalFromValue(jsargs[i], ctx, args[i]);
    if (Debug::level(Debug::Spam)) {
      Debug::log(Debug::Spam) << "  arg[" << i << "] = " << dumpJsVal(ctx,
          jsargs[i]) << Debug::flush;
    }
    if (!JS_SetElement(ctx, argsRoot.get(), i + 1, &jsargs[i])) {
      Debug::log(Debug::Error)
          << "FFSessionhandler::invoke - could not set args[" << (i + 1) << "]"
          << Debug::flush;
      return true;
    }
  }

  if (JS_IsExceptionPending(ctx)) {
    JS_ClearPendingException(ctx);
  }

  jsval rval;
  JSBool ok = JS_CallFunctionValue(ctx, JSVAL_TO_OBJECT(jsThis), funcVal,
      numArgs, jsargs.get(), &rval);

  if (!ok) {
    if (JS_GetPendingException(ctx, &rval)) {
      makeValueFromJsval(*returnValue, ctx, rval);
      Debug::log(Debug::Debugging) << "FFSessionHandler::invoke "
          << thisObj.toString() << "::" << methodName << " threw exception "
          << dumpJsVal(ctx, rval) << Debug::flush;
    } else {
      Debug::log(Debug::Error) << "Non-exception failure invoking "
          << methodName << Debug::flush;
      returnValue->setUndefined();
    }
  } else {
    makeValueFromJsval(*returnValue, ctx, rval);
  }
  Debug::log(Debug::Spam) << "  return= " << *returnValue << Debug::flush;
  return !ok;
}

/**
 * Invoke a plugin-provided method with the given args.  As above, this method does not own
 * any of its args.
 *
 * Returns true if an exception occurred.
 */
bool FFSessionHandler::invokeSpecial(HostChannel& channel, SpecialMethodId method, int numArgs,
    const gwt::Value* const args, gwt::Value* returnValue)  {
  Debug::log(Debug::Spam) << "FFSessionHandler::invokeSpecial" << Debug::flush;
  return false;
}

/**
 * Convert UTF16 string to UTF8-encoded std::string.
 *
 * This is implemented here because the Mozilla libraries mangle certain UTF8
 * strings.
 *
 * @return UTF8-encoded string.
 */
static std::string utf8String(const jschar* str, unsigned len) {
  std::string utf8str;
  while (len-- > 0) {
    unsigned ch = *str++;
    // check for paired surrogates first, leave unpaired surrogates as-is
    if (ch >= 0xD800 && ch < 0xDC00 && len > 0 && *str >= 0xDC00 && *str < 0xE000) {
      ch = ((ch & 1023) << 10) + (*str++ & 1023) + 0x10000;
      len--;
    }
    if (ch < 0x80) {          // U+0000 - U+007F as 0xxxxxxx
      utf8str.append(1, ch);
    } else if (ch < 0x800) {  // U+0080 - U+07FF as 110xxxxx 10xxxxxx
      utf8str.append(1, 0xC0 + ((ch >> 6) & 31));
      utf8str.append(1, 0x80 + (ch & 63));
    } else if (ch < 0x10000) { // U+0800 - U+FFFF as 1110xxxx 10xxxxxx 10xxxxxx
      utf8str.append(1, 0xE0 + ((ch >> 12) & 15));
      utf8str.append(1, 0x80 + ((ch >> 6) & 63));
      utf8str.append(1, 0x80 + (ch & 63));
    } else {  // rest as 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
      utf8str.append(1, 0xF0 + ((ch >> 18) & 7));
      utf8str.append(1, 0x80 + ((ch >> 12) & 63));
      utf8str.append(1, 0x80 + ((ch >> 6) & 63));
      utf8str.append(1, 0x80 + (ch & 63));
    }
  }
  return utf8str;
}

/**
 * Creates a JSString from a UTF8-encoded std::string.
 *
 * This is implemented here because the Mozilla libraries mangle certain UTF8
 * strings.
 *
 * @return the JSString object, which owns its memory buffer.
 */
static JSString* stringUtf8(JSContext* ctx, const std::string& utf8str) {
  unsigned len = 0;
  for (unsigned i = 0; i < utf8str.length(); ++i) {
    char ch = utf8str[i];
    switch (ch & 0xF8) {
      // continuation & invalid chars
      default:
      // ASCII characters
      case 0x00: case 0x08: case 0x10: case 0x18:
      case 0x20: case 0x28: case 0x30: case 0x38:
      case 0x40: case 0x48: case 0x50: case 0x58:
      case 0x60: case 0x68: case 0x70: case 0x78:
      // 2-byte UTF8 characters
      case 0xC0: case 0xC8: case 0xD0: case 0xD8:
      // 3-byte UTF8 characters
      case 0xE0: case 0xE8:
        ++len;
        break;
      case 0xF0:
        len += 2;
        break;
    }
  }
  // Account for null terminator even if it isn't included in the string length
  // Note that buf becomes owned by the JSString and must not be freed here.
  jschar* buf = static_cast<jschar*>(JS_malloc(ctx, (len + 1) * sizeof(jschar)));
  if (!buf) {
    return NULL;
  }
  jschar* p = buf;
  unsigned codePoint;
  int charsLeft = -1;
  for (unsigned i = 0; i < utf8str.length(); ++i) {
    char ch = utf8str[i];
    if (charsLeft >= 0) {
      if ((ch & 0xC0) != 0x80) {
        // invalid, missing continuation character
        *p++ = static_cast<jschar>(0xFFFD);
        charsLeft = -1;
      } else {
        codePoint = (codePoint << 6) | (ch & 63);
        if (!--charsLeft) {
          if (codePoint >= 0x10000) {
            codePoint -= 0x10000;
            *p++ = static_cast<jschar>(0xD800 + ((codePoint >> 10) & 1023));
            *p++ = static_cast<jschar>(0xDC00 + (codePoint & 1023));
          } else {
            *p++ = static_cast<jschar>(codePoint);
          }
          charsLeft = -1;
        }
      }
      continue;
    }
    // Look at the top 5 bits to determine how many bytes are in this character.
    switch (ch & 0xF8) {
      default: // skip invalid and continuation chars
        break;
      case 0x00: case 0x08: case 0x10: case 0x18:
      case 0x20: case 0x28: case 0x30: case 0x38:
      case 0x40: case 0x48: case 0x50: case 0x58:
      case 0x60: case 0x68: case 0x70: case 0x78:
        *p++ = static_cast<jschar>(ch);
        break;
      case 0xC0: case 0xC8: case 0xD0: case 0xD8:
        charsLeft = 1;
        codePoint = ch & 31;
        break;
      case 0xE0: case 0xE8:
        charsLeft = 2;
        codePoint = ch & 15;
        break;
      case 0xF0:
        charsLeft = 3;
        codePoint = ch & 7;
        break;
    }
  }
  // null terminator, apparently some code expects a terminator even though
  // the strings are counted.  Note that this null word should not be included
  // in the length, and that the buffer becomes owned by the JSString object.
  *p = 0;
  return JS_NewUCString(ctx, buf, p - buf);
}

void FFSessionHandler::makeValueFromJsval(gwt::Value& retVal, JSContext* ctx,
    const jsval& value) {
  if (JSVAL_IS_VOID(value)) {
    retVal.setUndefined();
  } else if (JSVAL_IS_NULL(value)) {
    retVal.setNull();
  } else if (JSVAL_IS_INT(value)) {
    retVal.setInt(JSVAL_TO_INT(value));
  } else if (JSVAL_IS_BOOLEAN(value)) {
    retVal.setBoolean(JSVAL_TO_BOOLEAN(value));
  } else if (JSVAL_IS_STRING(value)) {
    JSString* str = JSVAL_TO_STRING(value);
#if GECKO_VERSION < 2000
    retVal.setString(utf8String(JS_GetStringChars(str),
        JS_GetStringLength(str)));
#else
    retVal.setString(utf8String(JS_GetStringCharsZ(ctx, str),
        JS_GetStringLength(str)));
#endif //GECKO_VERSION
  } else if (JSVAL_IS_DOUBLE(value)) {
    retVal.setDouble(JSVAL_TO_DOUBLE(value));
  } else if (JSVAL_IS_OBJECT(value)) {
    JSObject* obj = JSVAL_TO_OBJECT(value);
    if (JavaObject::isJavaObject(ctx, obj)) {
      retVal.setJavaObject(JavaObject::getObjectId(ctx, obj));
    } else if (JS_GET_CLASS(ctx, obj) == stringObjectClass) {
      // JS String wrapper object, treat as a string primitive
      JSString* str = JS_ValueToString(ctx, value);
#if GECKO_VERSION < 2000
    retVal.setString(utf8String(JS_GetStringChars(str),
        JS_GetStringLength(str)));
#else
    retVal.setString(utf8String(JS_GetStringCharsZ(ctx, str),
        JS_GetStringLength(str)));
#endif //GECKO_VERSION
      // str will be garbage-collected, does not need to be freed
    } else {
      // It's a plain-old JavaScript Object
      void* objKey = identityFromObject(obj);
      std::map<void*, int>::iterator it = jsIdsByObject.find(objKey);
      if (it != jsIdsByObject.end()) {
        retVal.setJsObjectId(it->second);
      } else {
        // Allocate a new id
        int objId = ++jsObjectId;
        JS_SetElement(ctx, jsObjectsById, objId, const_cast<jsval*>(&value));
        jsIdsByObject[objKey] = objId;
        retVal.setJsObjectId(objId);
      }
    }
  } else {
    Debug::log(Debug::Error) << "Unhandled jsval type " << Debug::flush;
    retVal.setString("Unhandled jsval type");
  }
}

void FFSessionHandler::makeJsvalFromValue(jsval& retVal, JSContext* ctx,
    const gwt::Value& value) {
  switch (value.getType()) {
    case gwt::Value::NULL_TYPE:
      retVal = JSVAL_NULL;
      break;
    case gwt::Value::BOOLEAN:
      retVal = BOOLEAN_TO_JSVAL(value.getBoolean());
      break;
    case gwt::Value::BYTE:
      retVal = INT_TO_JSVAL((int) value.getByte());
      break;
    case gwt::Value::CHAR:
      retVal = INT_TO_JSVAL((int) value.getChar());
      break;
    case gwt::Value::SHORT:
      retVal = INT_TO_JSVAL((int) value.getShort());
      break;
    case gwt::Value::INT: {
      int intValue = value.getInt();
      if (INT_FITS_IN_JSVAL(intValue)) {
        retVal = INT_TO_JSVAL(intValue);
      } else {
        JS_NewNumberValue(ctx, (jsdouble) intValue, &retVal);
      }
      break;
    }
    // TODO(jat): do we still need long support in the wire format and Value?
//    case gwt::Value::LONG:
//      retVal = value.getLong();
//      break;
    case gwt::Value::FLOAT:
      JS_NewNumberValue(ctx, (jsdouble) value.getFloat(), &retVal);
      break;
    case gwt::Value::DOUBLE:
      JS_NewNumberValue(ctx, (jsdouble) value.getDouble(), &retVal);
      break;
    case gwt::Value::STRING:
      {
        JSString* str = stringUtf8(ctx, value.getString());
        retVal = STRING_TO_JSVAL(str);
      }
      break;
    case gwt::Value::JAVA_OBJECT:
      {
        int javaId = value.getJavaObjectId();
        std::map<int, JSObject*>::iterator i = javaObjectsById.find(javaId);
        if (i == javaObjectsById.end()) {
          JSObject* obj = JavaObject::construct(ctx, this, javaId);
          javaObjectsById[javaId] = obj;
          // We may have previously released the proxy for the same object id,
          // but have not yet sent a free message back to the server.
          javaObjectsToFree.erase(javaId);
          retVal = OBJECT_TO_JSVAL(obj);
        } else {
          retVal = OBJECT_TO_JSVAL(i->second);
        }
      }
      break;
    case gwt::Value::JS_OBJECT:
      {
        int jsId = value.getJsObjectId();
        if (!JS_GetElement(ctx, jsObjectsById, jsId, &retVal)) {
          Debug::log(Debug::Error) << "Error getting jsObject with id " << jsId << Debug::flush;
        }
        if (!JSVAL_IS_OBJECT(retVal)) {
          Debug::log(Debug::Error) << "Missing jsObject with id " << jsId << Debug::flush;
        }
      }
      break;
    case gwt::Value::UNDEFINED:
      retVal = JSVAL_VOID;
      break;
    default:
      Debug::log(Debug::Error) << "Unknown Value type " << value.toString() << Debug::flush;
  }
}

void FFSessionHandler::freeJavaObject(int objectId) {
  if (!javaObjectsById.erase(objectId)) {
    Debug::log(Debug::Error) << "Trying to free unknown JavaObject: " << objectId << Debug::flush;
    return;
  }
  javaObjectsToFree.insert(objectId);
}

void FFSessionHandler::disconnect() {
  Debug::log(Debug::Debugging) << "FFSessionHandler::disconnect" << Debug::flush;
  JSContext* ctx = getJSContext();
  bool freeCtx = false;
  if (!ctx) {
    Debug::log(Debug::Debugging) << "  creating temporary context"
        << Debug::flush;
    freeCtx = true;
    ctx = JS_NewContext(runtime, 8192);
    if (ctx) {
      JS_SetOptions(ctx, JSOPTION_VAROBJFIX);
#ifdef JSVERSION_LATEST
      JS_SetVersion(ctx, JSVERSION_LATEST);
#endif
    }
  }
  if (ctx) {
    JS_BeginRequest(ctx);
    for (std::map<int, JSObject*>::iterator it = javaObjectsById.begin();
        it != javaObjectsById.end(); ++it) {
      int javaId = it->first;
      JSObject* obj = it->second;
      if (JavaObject::isJavaObject(ctx, obj)) {
        // clear the SessionData pointer -- JavaObject knows it is
        // disconnected if this is null
        MOZ_JS_SetPrivate(ctx, obj, NULL);
        javaObjectsToFree.erase(javaId);
      }
    }
    JS_EndRequest(ctx);
    if (freeCtx) {
      JS_DestroyContext(ctx);
    }
  } else {
    Debug::log(Debug::Warning)
        << "FFSessionHandler::disconnect - no context available"
        << Debug::flush;
  }
  HostChannel* channel = getHostChannel();
  if (channel->isConnected()) {
    channel->disconnectFromHost();
  }
}

void* FFSessionHandler::identityFromObject(JSObject* obj) {
  JSContext* ctx = getJSContext();
  jsval rval;
  void* returnValue = obj;
  if (JS_GetProperty(ctx, obj, "wrappedJSObject", &rval)
      && JSVAL_IS_OBJECT(rval)) {
    returnValue = JSVAL_TO_OBJECT(rval);
    Debug::log(Debug::Info) << "identityFromObject mapped " << obj << " to "
        << returnValue << Debug::flush;
  }
  return returnValue;
}
