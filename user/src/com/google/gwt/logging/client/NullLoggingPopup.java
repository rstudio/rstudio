/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.logging.client;

import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;

/**
 * A class which can be substituted in for the BasicLoggingPopup in the
 * the gwt.xml file to disable the default popup log handler.
 */
public class NullLoggingPopup implements HasWidgets {

  public void add(Widget w) { }

  public void clear() { }

  public Iterator<Widget> iterator() {
    return null;
  }

  public boolean remove(Widget w) {
    return false;
  }

}
