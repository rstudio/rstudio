/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.user.client.ui;

import java.util.Iterator;
import java.util.Vector;

/**
 * A helper class for implementers of the SourcesLoadEvents interface. This
 * subclass of Vector assumes that all objects added to it will be of type
 * {@link com.google.gwt.user.client.ui.LoadListener}.
 */
public class LoadListenerCollection extends Vector {

  /**
   * Fires an error event to all listeners.
   * 
   * @param sender the widget sending the event.
   */
  public void fireError(Widget sender) {
    for (Iterator it = iterator(); it.hasNext();) {
      LoadListener listener = (LoadListener) it.next();
      listener.onError(sender);
    }
  }

  /**
   * Fires a load event to all listeners.
   * 
   * @param sender the widget sending the event.
   */
  public void fireLoad(Widget sender) {
    for (Iterator it = iterator(); it.hasNext();) {
      LoadListener listener = (LoadListener) it.next();
      listener.onLoad(sender);
    }
  }
}
