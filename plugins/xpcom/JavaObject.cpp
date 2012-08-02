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

#include "JavaObject.h"
#include "FFSessionHandler.h"
#include "SessionData.h"
#include "ServerMethods.h"
#include "Debug.h"
#include "XpcomDebug.h"
#include "HostChannel.h"
#include "InvokeMessage.h"
#include "ReturnMessage.h"
#include "scoped_ptr/scoped_ptr.h"

static JSClass JavaObjectClass = {
  "GWTJavaObject", /* class name */
  JSCLASS_HAS_PRIVATE | JSCLASS_HAS_RESERVED_SLOTS(1) | JSCLASS_NEW_ENUMERATE, /* flags */

  JS_PropertyStub, /* add property */
  JS_PropertyStub, /* delete property */
  JavaObject::getProperty, /* get property */
  JavaObject::setProperty, /* set property */

  reinterpret_cast<JSEnumerateOp>(JavaObject::enumerate), /* enumerate */
  JS_ResolveStub, /* resolve */
  JS_ConvertStub, // JavaObject::convert, /* convert */
  JavaObject::finalize, /* finalize */ //TODO

#if GECKO_VERSION < 13000
  NULL, /* object hooks */
#endif
  NULL, /* check access */
#if GECKO_VERSION < 2000
  JavaObject::call, /* call */ //TODO
#else
  JavaObject::call20, /* call: compatability wrapper for 2.0+ */
#endif //GECKO_VERSION
  NULL, /* construct */
  NULL, /* object serialization */
  NULL, /* has instance */
  NULL, /* mark */
  NULL /* reserve slots */
};

int JavaObject::getObjectId(JSContext* ctx, JSObject* obj) {
  jsval val;
  JSClass* jsClass = JS_GET_CLASS(ctx, obj);
  if (jsClass != &JavaObjectClass) {
    Debug::log(Debug::Error)
        << "JavaObject::getObjectId called on non-JavaObject: " << jsClass->name
        << Debug::flush;
    return -1;
  }
  if (JSCLASS_RESERVED_SLOTS(jsClass) < 1) {
    Debug::log(Debug::Error)
        << "JavaObject::getObjectId -- " << static_cast<void*>(obj)
        << " has only " << (JSCLASS_RESERVED_SLOTS(jsClass))
        << " reserved slots, no objectId present" << Debug::flush;
    return -1;
  }


#if GECKO_VERSION >= 13000
  val = JS_GetReservedSlot(obj, 0);
#else
  if (!JS_GetReservedSlot(ctx, obj, 0, &val)) {
    Debug::log(Debug::Error) << "Error getting reserved slot" << Debug::flush;
    return -1;
  }
#endif

  // TODO: assert JSVAL_IS_INT(val)
  return JSVAL_TO_INT(val);
}

SessionData* JavaObject::getSessionData(JSContext* ctx, JSObject* obj) {
  void* data = JS_GetInstancePrivate(ctx, obj, &JavaObjectClass, NULL);
  return static_cast<SessionData*>(data);
}


bool JavaObject::isJavaObject(JSContext* ctx, JSObject* obj) {
  return JS_GET_CLASS(ctx, obj) == &JavaObjectClass;
}

JSObject* JavaObject::construct(JSContext* ctx, SessionData* data, int objectRef) {
  // TODO: prototype? parent?
  Debug::log(Debug::Spam) << "JavaObject::construct objectId=" << objectRef << Debug::flush;
  JSObject* obj = JS_NewObject(ctx, &JavaObjectClass, NULL, NULL);
  Debug::log(Debug::Spam) << "  obj=" << obj << Debug::flush;
  if (!obj) {
    return NULL;
  }
  // set the session data
#if GECKO_VERSION >= 13000
  MOZ_JS_SetPrivate(ctx, obj, data);
#else
  if (!MOZ_JS_SetPrivate(ctx, obj, data)) {
    Debug::log(Debug::Error) << "Could not set private data" << Debug::flush;
    return NULL;
  }
#endif

  #if GECKO_VERSION >= 13000
  MOZ_JS_SetReservedSlot(ctx, obj, 0, INT_TO_JSVAL(objectRef));
  #else
  // set the objectId
  if (!MOZ_JS_SetReservedSlot(ctx, obj, 0, INT_TO_JSVAL(objectRef))) {
    Debug::log(Debug::Error) << "Could not set reserved slot" << Debug::flush;
    return NULL;
  }
  #endif

  // define toString (TODO: some way to avoid doing this each time)
#if GECKO_VERSION < 2000
  if (!JS_DefineFunction(ctx, obj, "toString", JavaObject::toString, 0, 0)) {
    Debug::log(Debug::Error) << "Could not define toString method on object"
        << Debug::flush;
  }
#else
  if (!JS_DefineFunction(ctx, obj, "toString", JavaObject::toString20, 0, 0)) {
    Debug::log(Debug::Error) << "Could not define toString method on object"
        << Debug::flush;
  }
#endif //GECKO_VERSION
  return obj;
}

JSBool JavaObject::getProperty(JSContext* ctx, JSObject* obj, jsid id,
    jsval* rval) {
  Debug::log(Debug::Spam) << "JavaObject::getProperty obj=" << obj << Debug::flush;
  SessionData* data = JavaObject::getSessionData(ctx, obj);
  if (!data) {
    // TODO: replace the frame with an error page instead?
    *rval = JSVAL_VOID;
    return JS_TRUE;
  }
  int objectRef = JavaObject::getObjectId(ctx, obj);
  if (JSID_IS_STRING(id)) {
    JSString* str = JSID_TO_STRING(id);
    if ((JS_GetStringEncodingLength(ctx, str) == 8) && !strncmp("toString",
          JS_EncodeString(ctx, str), 8)) {
      *rval = data->getToStringTearOff();
      return JS_TRUE;
    }
    if ((JS_GetStringEncodingLength(ctx, str) == 2) && !strncmp("id",
          JS_EncodeString(ctx, str), 2)) {
      *rval = INT_TO_JSVAL(objectRef);
      return JS_TRUE;
    }
    if ((JS_GetStringEncodingLength(ctx, str) == 16) && !strncmp("__noSuchMethod__",
          JS_EncodeString(ctx, str), 16)) {
      // Avoid error spew if we are disconnected
      *rval = JSVAL_VOID;
      return JS_TRUE;
    }
    // TODO: dumpJsVal can no longer handle this case
    //Debug::log(Debug::Error) << "Getting unexpected string property "
    //    << dumpJsVal(ctx, id) << Debug::flush;
    // TODO: throw a better exception here
    return JS_FALSE;
  }
  if (!JSID_IS_INT(id)) {
    // TODO: dumpJsVal can no longer handle this case
    //Debug::log(Debug::Error) << "Getting non-int/non-string property "
    //      << dumpJsVal(ctx, id) << Debug::flush;
    // TODO: throw a better exception here
    return JS_FALSE;
  }
  int dispId = JSID_TO_INT(id);

  HostChannel* channel = data->getHostChannel();
  SessionHandler* handler = data->getSessionHandler();

  gwt::Value value = ServerMethods::getProperty(*channel, handler, objectRef, dispId);
  data->makeJsvalFromValue(*rval, ctx, value);
  return JS_TRUE;
}

#if GECKO_VERSION < 2000
JSBool JavaObject::setProperty(JSContext* ctx, JSObject* obj, jsid id,
    jsval* vp) {
#else
JSBool JavaObject::setProperty(JSContext* ctx, JSObject* obj, jsid id,
    JSBool strict, jsval* vp) {
#endif //GECKO_VERSION

  Debug::log(Debug::Spam) << "JavaObject::setProperty obj=" << obj << Debug::flush;
  if (!JSID_IS_INT(id)) {
    Debug::log(Debug::Error) << "  Error: setting string property id" << Debug::flush;
    // TODO: throw a better exception here
    return JS_FALSE;
  }

  SessionData* data = JavaObject::getSessionData(ctx, obj);
  if (!data) {
    return JS_TRUE;
  }

  int objectRef = JavaObject::getObjectId(ctx, obj);
  int dispId = JSID_TO_INT(id);

  gwt::Value value;
  data->makeValueFromJsval(value, ctx, *vp);

  HostChannel* channel = data->getHostChannel();
  SessionHandler* handler = data->getSessionHandler();

  if (!ServerMethods::setProperty(*channel, handler, objectRef, dispId, value)) {
    // TODO: throw a better exception here
    return JS_FALSE;
  }
  return JS_TRUE;
}

// TODO: can this be removed now?
JSBool JavaObject::convert(JSContext* ctx, JSObject* obj, JSType type, jsval* vp) {
  Debug::log(Debug::Spam) << "JavaObject::convert obj=" << obj
      << " type=" << type << Debug::flush;
  switch (type) {
    case JSTYPE_STRING:
      return toString(ctx, obj, 0, NULL, vp);
    case JSTYPE_VOID:
      *vp = JSVAL_VOID;
      return JS_TRUE;
    case JSTYPE_NULL:
      *vp = JSVAL_NULL;
      return JS_TRUE;
    case JSTYPE_OBJECT:
      *vp = OBJECT_TO_JSVAL(obj);
      return JS_TRUE;
    default:
      break;
  }
  return JS_FALSE;
}

/**
 * List of property names we want to fake on wrapped Java objects.
 */
static const char* propertyNames[] = {
    "toString",
    "id",
};
#define NUM_PROPERTY_NAMES (sizeof(propertyNames) / sizeof(propertyNames[0]))

JSBool JavaObject::enumerate(JSContext* ctx, JSObject* obj, JSIterateOp op,
    jsval* statep, jsid* idp) {
  int objectId = JavaObject::getObjectId(ctx, obj);
  switch (op) {
    case JSENUMERATE_INIT:
      Debug::log(Debug::Spam) << "JavaObject::enumerate(oid=" << objectId
          << ", INIT)" << Debug::flush;
      *statep = JSVAL_ZERO;
      if (idp) {
        *idp = INT_TO_JSID(NUM_PROPERTY_NAMES);
      }
      break;
    case JSENUMERATE_NEXT:
    {
      int idNum = JSVAL_TO_INT(*statep);
      Debug::log(Debug::Spam) << "JavaObject::enumerate(oid=" << objectId
          << ", NEXT " << idNum << ")" << Debug::flush;
      *statep = INT_TO_JSVAL(idNum + 1);
      if (idNum >= NUM_PROPERTY_NAMES) {
        *statep = JSVAL_NULL;
#if GECKO_VERSION < 2000
	//TODO(jat): do we need to do this?
        *idp = JSVAL_NULL;
#endif //GECKO_VERSION
      } else {
        const char* propName = propertyNames[idNum];
        JSString* str = JS_NewStringCopyZ(ctx, propName);
        return JS_ValueToId(ctx, STRING_TO_JSVAL(str), idp);
      }
      break;
    }
    case JSENUMERATE_DESTROY:
      Debug::log(Debug::Spam) << "JavaObject::enumerate(oid=" << objectId
          << ", DESTROY)" << Debug::flush;
      *statep = JSVAL_NULL;
      break;
    default:
      Debug::log(Debug::Error) << "Unknown Enumerate op " <<
          static_cast<int>(op) << Debug::flush;
      return JS_FALSE;
  }
  return JS_TRUE;
}

#if GECKO_VERSION >= 1400
void JavaObject::finalize(JSFreeOp* fop, JSObject* obj) {
  // In general use of JS_GetPrivate is not safe but it is OK in the finalizer
  // according to:
  // https://developer.mozilla.org/en/SpiderMonkey/JSAPI_Reference/JS_GetPrivate
  // We will not be using getSession for that reason.
  SessionData * data = static_cast<SessionData*>(JS_GetPrivate(obj));
  if (data) {
    jsval val = JS_GetReservedSlot(obj, 0);
    int objectId = JSVAL_TO_INT(val);
    data->freeJavaObject(objectId);
    MOZ_JS_SetPrivate(/** Post-FF13 requires no ctx anyways*/ NULL, obj, NULL);
  }
}
#else
void JavaObject::finalize(JSContext* ctx, JSObject* obj) {
  Debug::log(Debug::Spam) << "JavaObject::finalize obj=" << obj
      << " objId=" << JavaObject::getObjectId(ctx, obj) << Debug::flush;
  SessionData* data = JavaObject::getSessionData(ctx, obj);
  if (data) {
    int objectId = JavaObject::getObjectId(ctx, obj);
    data->freeJavaObject(objectId);
    MOZ_JS_SetPrivate(ctx, obj, NULL);
  }
}
#endif

JSBool JavaObject::toString(JSContext* ctx, JSObject* obj, uintN argc,
    jsval* argv, jsval* rval) {
  SessionData* data = JavaObject::getSessionData(ctx, obj);
  if (!data) {
    *rval = JSVAL_VOID;
    return JS_TRUE;
  }
  int oid = getObjectId(ctx, obj);
  Debug::log(Debug::Spam) << "JavaObject::toString(id=" << oid << ")"
      << Debug::flush;
  gwt::Value javaThis;
  javaThis.setJavaObject(oid);
  // we ignore any supplied parameters
  return invokeJava(ctx, data, javaThis, InvokeMessage::TOSTRING_DISP_ID, 0,
      NULL, rval);
}

/**
 * Called when the JavaObject is invoked as a function.
 * We ignore the JSObject* argument, which is the 'this' context, which is
 * usually the window object. The JavaObject instance is in argv[-2].
 *
 * Returns a JS array, with the first element being a boolean indicating that
 * an exception occured, and the second element is either the return value or
 * the exception which was thrown.  In this case, we always return false and
 * raise the exception ourselves.
 */
JSBool JavaObject::call(JSContext* ctx, JSObject*, uintN argc, jsval* argv,
    jsval* rval) {
  // Get the JavaObject called as a function
  JSObject* obj = JSVAL_TO_OBJECT(argv[-2]);
  if (argc < 2 || !JSVAL_IS_INT(argv[0]) || !JSVAL_IS_OBJECT(argv[1])) {
    Debug::log(Debug::Error) << "JavaObject::call incorrect arguments" << Debug::flush;
    return JS_FALSE;
  }
  int dispId = JSVAL_TO_INT(argv[0]);
  if (Debug::level(Debug::Spam)) {
    Debug::DebugStream& dbg = Debug::log(Debug::Spam) << "JavaObject::call oid="
        << JavaObject::getObjectId(ctx, obj) << ",dispId=" << dispId << " (";
    for (unsigned i = 2; i < argc; ++i) {
      if (i > 2) {
        dbg << ", ";
      }
      dbg << dumpJsVal(ctx, argv[i]);
    }
    dbg << ")" << Debug::flush;
  }

  SessionData* data = JavaObject::getSessionData(ctx, obj);
  if (!data) {
    *rval = JSVAL_VOID;
    return JS_TRUE;
  }
  Debug::log(Debug::Spam) << "Data = " << data << Debug::flush;

  gwt::Value javaThis;
  if (!JSVAL_IS_NULL(argv[1])) {
    JSObject* thisObj = JSVAL_TO_OBJECT(argv[1]);
    if (isJavaObject(ctx, thisObj)) {
      javaThis.setJavaObject(getObjectId(ctx, thisObj));
    } else {
      data->makeValueFromJsval(javaThis, ctx, argv[1]);
    }
  } else {
    int oid = getObjectId(ctx, obj);
    javaThis.setJavaObject(oid);
  }
  return invokeJava(ctx, data, javaThis, dispId, argc - 2, &argv[2], rval);
}

#if GECKO_VERSION >= 2000
/**
 * Compatability wrapper for Gecko 2.0+
 */
JSBool JavaObject::toString20(JSContext* ctx, uintN argc, jsval* vp) {
  jsval rval = JSVAL_VOID;
  JSBool success;
  success = toString(ctx, JS_THIS_OBJECT(ctx, vp), argc, JS_ARGV(ctx, vp),
                       &rval);
  JS_SET_RVAL(cx, vp, rval);
  return success;
}

/**
 * Compatability wrapper method for Gecko 2.0+
 */
JSBool JavaObject::call20(JSContext* ctx, uintN argc, jsval* vp) {
  jsval rval = JSVAL_VOID;
  JSBool success;
  success = call(ctx, NULL, argc, JS_ARGV(ctx, vp), &rval);
  JS_SET_RVAL(ctx, vp, rval);
  return success;
}
#endif //GECKO_VERSION

/**
 * Calls a method on a Java object and returns a two-element JS array, with
 * the first element being a boolean flag indicating an exception was thrown,
 * and the second element is the actual return value or exception.
 */
JSBool JavaObject::invokeJava(JSContext* ctx, SessionData* data,
    const gwt::Value& javaThis, int dispId, int numArgs, const jsval* jsargs,
    jsval* rval) {
  HostChannel* channel = data->getHostChannel();
  SessionHandler* handler = data->getSessionHandler();
  scoped_array<gwt::Value> args(new gwt::Value[numArgs]);
  for (int i = 0; i < numArgs; ++i) {
    data->makeValueFromJsval(args[i], ctx, jsargs[i]);
  }

  bool isException = false;
  gwt::Value returnValue;
  if (!InvokeMessage::send(*channel, javaThis, dispId, numArgs, args.get())) {
    Debug::log(Debug::Debugging) << "JavaObject::call failed to send invoke message" << Debug::flush;
  } else {
    Debug::log(Debug::Spam) << " return from invoke" << Debug::flush;
    scoped_ptr<ReturnMessage> retMsg(channel->reactToMessagesWhileWaitingForReturn(handler));
    if (!retMsg.get()) {
      Debug::log(Debug::Debugging) << "JavaObject::call failed to get return value" << Debug::flush;
    } else {
      isException = retMsg->isException();
      returnValue = retMsg->getReturnValue();
    }
  }
  // Since we can set exceptions normally, we always return false to the
  // wrapper function and set the exception ourselves if one occurs.
  // TODO: cleanup exception case
  jsval retvalArray[] = {JSVAL_FALSE, JSVAL_VOID};
  JSObject* retval = JS_NewArrayObject(ctx, 2, retvalArray);
  *rval = OBJECT_TO_JSVAL(retval);
  jsval retJsVal;
  Debug::log(Debug::Spam) << "  result is " << returnValue << Debug::flush;
  data->makeJsvalFromValue(retJsVal, ctx, returnValue);
  if (isException) {
    JS_SetPendingException(ctx, retJsVal);
    return false;
  }
  if (!JS_SetElement(ctx, retval, 1, &retJsVal)) {
    Debug::log(Debug::Error) << "Error setting return value element in array"
        << Debug::flush;
    return false;
  }
  return true;
}
