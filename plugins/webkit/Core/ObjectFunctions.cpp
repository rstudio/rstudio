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

#include "ObjectFunctions.h"
#include "WebScriptSessionHandler.h"
#include "scoped_ptr.h"

JSValueRef JavaObjectCallAsFunction(JSContextRef ctx, JSObjectRef function, JSObjectRef thisObject,
                                            size_t argumentCount, const JSValueRef arguments[], JSValueRef* exception) {
  TrackingDataRef tracker = static_cast<TrackingDataRef>(JSObjectGetPrivate(function));
  WebScriptSessionHandler* sessionHandler = static_cast<WebScriptSessionHandler*>(tracker->getSessionData());
  
  *exception = NULL;
  int dispatchId = JSValueToNumber(ctx, arguments[0], exception);
  if (*exception) {
    return JSValueMakeUndefined(ctx);
  }
  
  bool explicitThis = JSValueToBoolean(ctx, arguments[1]);
  if (explicitThis) {
    thisObject = JSValueToObject(ctx, arguments[1], exception);
    if (*exception) {
      return JSValueMakeUndefined(ctx);
    }
  } else {
    thisObject = function;
  }
  
  return sessionHandler->javaFunctionCallbackImpl(dispatchId, thisObject,
                                                  argumentCount - 2, &arguments[2], exception);  
}

/*
 * Static-dispatch function to clean up a Java object proxy.  This will
 * deallocate the TrackingData as well as remove the objectId mapping.  This
 * function is called from JavaScriptCore as well as ~WebScriptSessionHandler.
 *
 * The JavaScriptCore documentation indicates that finalizers may be called
 * from any thread, so we can't see javaFree messages directly from this
 * method.  Instead, we'll accumulate a list of ids to free. 
 */
void JavaObjectFinalize (JSObjectRef object) {
  TrackingDataRef tracker = static_cast<TrackingDataRef>(JSObjectGetPrivate(object));
  if (!tracker) {
    // The tracker may have already been deleted in ~WebScriptSessionHandler()
    return;
  }
  Debug::log(Debug::Spam) << "Finalizing Java object " << tracker->getObjectId() << Debug::flush;
  JSObjectSetPrivate(object, NULL);
  int id = tracker->getObjectId();  
  WebScriptSessionHandler* sessionHandler = static_cast<WebScriptSessionHandler*>(tracker->getSessionData());
  delete tracker;
  sessionHandler->javaObjectFinalizeImpl(id);
}

/*
 * Static-dispatch function to check for the presence of a property on a Java
 * object proxy.
 */
bool JavaObjectHasProperty (JSContextRef ctx, JSObjectRef object, JSStringRef propertyName) {
  TrackingDataRef tracker = static_cast<TrackingDataRef>(JSObjectGetPrivate(object));
  WebScriptSessionHandler* sessionHandler = static_cast<WebScriptSessionHandler*>(tracker->getSessionData());
  return sessionHandler->javaObjectHasPropertyImpl(tracker, object, propertyName);
}

/*
 * Static-dispatch function to retrieve the value of a property on a Java
 * object proxy.
 */
JSValueRef JavaObjectGetProperty (JSContextRef ctx, JSObjectRef object,
                                         JSStringRef propertyName, JSValueRef* exception) {
  TrackingDataRef tracker = static_cast<TrackingDataRef>(JSObjectGetPrivate(object));
  WebScriptSessionHandler* sessionHandler = static_cast<WebScriptSessionHandler*>(tracker->getSessionData());
  return sessionHandler->javaObjectGetPropertyImpl(tracker, object, propertyName, exception);
}

/*
 * Static-dispatch function to set the value of a property an a Java object
 * proxy.
 */
bool JavaObjectSetProperty (JSContextRef ctx, JSObjectRef object,
                                   JSStringRef propertyName, JSValueRef value,
                                   JSValueRef* exception) {
  TrackingDataRef tracker = static_cast<TrackingDataRef>(JSObjectGetPrivate(object));
  WebScriptSessionHandler* sessionHandler = static_cast<WebScriptSessionHandler*>(tracker->getSessionData());
  return sessionHandler->javaObjectSetPropertyImpl(tracker, object, propertyName, value, exception);
}
