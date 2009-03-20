/*
 * Copyright 2007 Google Inc.
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

import java.util.ArrayList;

/**
 * A helper class for implementers of the SourcesChangeEvents interface. This
 * subclass of {@link ArrayList} assumes that all objects added to it will be of
 * type {@link com.google.gwt.user.client.ui.ChangeListener}.
 * 
 * @deprecated Widgets should now manage their own handlers via {@link Widget#addDomHandler}
 */
@Deprecated
public class ChangeListenerCollection extends ArrayList<ChangeListener> {

  /**
   * Fires a change event to all listeners.
   * 
   * @param sender the widget sending the event.
   */
  public void fireChange(Widget sender) {
    for (ChangeListener listener : this) {
      listener.onChange(sender);
    }
  }
}
