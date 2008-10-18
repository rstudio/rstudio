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

import com.google.gwt.event.shared.AbstractEvent.Type;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The default Handler manager's handler registry.
 */
class JavaHandlerRegistry extends
    HashMap<AbstractEvent.Type, ArrayList<EventHandler>> {

  public void addHandler(AbstractEvent.Type eventKey, EventHandler handler) {
    ArrayList<EventHandler> l = super.get(eventKey);
    if (l == null) {
      l = new ArrayList<EventHandler>();
      super.put(eventKey, l);
    }
    l.add(handler);
  }

  public void clearHandlers(Type<?, ?> type) {
    super.remove(type);
  }

  public void fireEvent(AbstractEvent event) {
    Type type = event.getType();
    int count = getHandlerCount(type);
    for (int i = 0; i < count; i++) {
      EventHandler handler = getHandler(type, i);
      type.fire(handler, event);
    }
  }

  public EventHandler getHandler(AbstractEvent.Type eventKey, int index) {
    assert (index < getHandlerCount(eventKey));
    ArrayList<EventHandler> l = super.get(eventKey);
    return l.get(index);
  }

  public int getHandlerCount(AbstractEvent.Type eventKey) {
    ArrayList<EventHandler> l = super.get(eventKey);
    if (l == null) {
      return 0;
    } else {
      return l.size();
    }
  }

  public void removeHandler(AbstractEvent.Type eventKey, EventHandler handler) {
    ArrayList<EventHandler> l = super.get(eventKey);
    if (l != null) {
      l.remove(handler);
    }
  }
}
