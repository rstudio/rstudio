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

package com.google.gwt.event.shared;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.AbstractEvent.Type;

/**
 * Default JavaScript handler registry. This is in the shared package so we
 * don't have to make it public, should never be called outside of a GWT runtime
 * environment.
 * 
 * Th JsHandlerRegistry makes use of the fact that in the large majority of
 * cases, only one or two handlers are added for each event type. Therefore,
 * rather than storing handlers in a list of lists, we store then in a single
 * flattened array with an escape clause to handle the rare case where we have
 * more handlers then expected.
 */
class JsHandlerRegistry extends JavaScriptObject {

  public static JsHandlerRegistry create() {
    return (JsHandlerRegistry) JavaScriptObject.createObject();
  }

  /**
   * Required constructor.
   */
  protected JsHandlerRegistry() {
  }

  public final void addHandler(AbstractEvent.Type eventKey, EventHandler handler) {
    // The base is the equivalent to a c pointer into the flattened handler data
    // structure.
    int base = eventKey.hashCode();
    int count = getCount(base);

    // If we already have the maximum number of handlers we can store in the
    // flattened data structure, store the handlers in an external list instead.
    if (count == HandlerManager.EXPECTED_HANDLERS && isFlattened(base)) {
      unflatten(base);
    }
    setHandler(base, count, handler, isFlattened(base));
    setCount(base, count + 1);
  }

  public final void clearHandlers(Type<?, ?> type) {
    int base = type.hashCode();
    // Clearing handlers is relatively unusual, so the cost of unflattening the
    // handler list is justified by the smaller code.
    unflatten(base);

    // Replace the list of handlers.
    setHandlerList(base + 1, JavaScriptObject.createArray());
    setCount(base, 0);
  }

  public final void fireEvent(AbstractEvent event) {
    Type type = event.getType();
    int base = type.hashCode();
    int count = getCount(base);
    boolean isFlattened = isFlattened(base);

    for (int i = 0; i < count; i++) {
      // Gets the given handler to fire.
      EventHandler handler = getHandler(base, i, isFlattened);

      // Fires the handler.
      type.fire(handler, event);
    }
  }

  public final EventHandler getHandler(AbstractEvent.Type eventKey, int index) {
    int base = eventKey.hashCode();
    int count = getCount(base);
    if (index >= count) {
      throw new IndexOutOfBoundsException("index: " + index);
    }
    return getHandler(base, index, isFlattened(base));
  }

  public final int getHandlerCount(AbstractEvent.Type eventKey) {
    return getCount(eventKey.hashCode());
  }

  public final void removeHandler(AbstractEvent.Type eventKey,
      EventHandler handler) {
    int base = eventKey.hashCode();

    // Removing a handler is unusual, so smaller code is preferable then
    // handling both flat and dangling list of pointers.
    if (isFlattened(base)) {
      unflatten(base);
    }
    boolean result = removeHelper(base, handler);
    // Hiding this behind an assertion as we'd rather not force the compiler to
    // have to include all handler.toString() instances.
    assert result : handler + " did not exist";
  }

  private native int getCount(int index) /*-{
    var count = this[index];
    return count == null? 0:count;
  }-*/;

  private native EventHandler getHandler(int base, int index, boolean flattened) /*-{
    return flattened? this[base + 2 + index]: this[base + 1][index];
  }-*/;

  private native boolean isFlattened(int base) /*-{
    return this[base + 1] == null;
  }-*/;

  private native boolean removeHelper(int base, EventHandler handler) /*-{
    // Find the handler.
    var count = this[base];
    var handlerList = this[base + 1];
    var handlerIndex = -1;
    for(var index = 0;  index < count; index++){
      if(handlerList[index] == handler){
        handlerIndex = index;
        break;
      }
    }
    if(handlerIndex == -1) {
      return false;
    }

    // Remove the handler.
    var last = count -1;
    for(; handlerIndex < last; handlerIndex++){
      handlerList[handlerIndex] = handlerList[handlerIndex+1]
    }
    handlerList[last] = null;
    this[base] = this[base]-1;
    return true;
  }-*/;

  private native void setCount(int index, int count) /*-{
    this[index] = count;
  }-*/;

  private native void setHandler(int base, int index, EventHandler handler,
      boolean flattened) /*-{
    if(flattened) {
      this[base + 2 + index] = handler;
    } else {
      this[base + 1][index] = handler;
    }
  }-*/;

  private native void setHandlerList(int base, JavaScriptObject handlerList) /*-{
    this[base + 1] = handlerList;
  }-*/;

  private native void unflatten(int base) /*-{
    var handlerList = {};
    var count = this[base];
    var start = base + 2;
     for(var i = 0; i < count;i++){
       handlerList[i] = this[start + i];
       this[start + i] = null;
      }
     this[base + 1] = handlerList;
  }-*/;
}