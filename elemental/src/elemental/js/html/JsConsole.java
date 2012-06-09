/*
 * Copyright 2012 Google Inc.
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
package elemental.js.html;
import elemental.html.Console;
import elemental.html.MemoryInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsConsole extends JsElementalMixinBase  implements Console {
  protected JsConsole() {}

  public final native JsMemoryInfo getMemory() /*-{
    return this.memory;
  }-*/;

  public final native JsIndexable getProfiles() /*-{
    return this.profiles;
  }-*/;

  public final native void assertCondition(boolean condition, Object arg) /*-{
    this.assertCondition(condition, arg);
  }-*/;

  public final native void count() /*-{
    this.count();
  }-*/;

  public final native void debug(Object arg) /*-{
    this.debug(arg);
  }-*/;

  public final native void dir() /*-{
    this.dir();
  }-*/;

  public final native void dirxml() /*-{
    this.dirxml();
  }-*/;

  public final native void error(Object arg) /*-{
    this.error(arg);
  }-*/;

  public final native void group(Object arg) /*-{
    this.group(arg);
  }-*/;

  public final native void groupCollapsed(Object arg) /*-{
    this.groupCollapsed(arg);
  }-*/;

  public final native void groupEnd() /*-{
    this.groupEnd();
  }-*/;

  public final native void info(Object arg) /*-{
    this.info(arg);
  }-*/;

  public final native void log(Object arg) /*-{
    this.log(arg);
  }-*/;

  public final native void markTimeline() /*-{
    this.markTimeline();
  }-*/;

  public final native void profile(String title) /*-{
    this.profile(title);
  }-*/;

  public final native void profileEnd(String title) /*-{
    this.profileEnd(title);
  }-*/;

  public final native void time(String title) /*-{
    this.time(title);
  }-*/;

  public final native void timeEnd(String title, Object arg) /*-{
    this.timeEnd(title, arg);
  }-*/;

  public final native void timeStamp(Object arg) /*-{
    this.timeStamp(arg);
  }-*/;

  public final native void trace(Object arg) /*-{
    this.trace(arg);
  }-*/;

  public final native void warn(Object arg) /*-{
    this.warn(arg);
  }-*/;
}
