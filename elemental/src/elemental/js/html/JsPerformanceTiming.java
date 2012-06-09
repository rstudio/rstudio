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
import elemental.html.PerformanceTiming;

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

public class JsPerformanceTiming extends JsElementalMixinBase  implements PerformanceTiming {
  protected JsPerformanceTiming() {}

  public final native double getConnectEnd() /*-{
    return this.connectEnd;
  }-*/;

  public final native double getConnectStart() /*-{
    return this.connectStart;
  }-*/;

  public final native double getDomComplete() /*-{
    return this.domComplete;
  }-*/;

  public final native double getDomContentLoadedEventEnd() /*-{
    return this.domContentLoadedEventEnd;
  }-*/;

  public final native double getDomContentLoadedEventStart() /*-{
    return this.domContentLoadedEventStart;
  }-*/;

  public final native double getDomInteractive() /*-{
    return this.domInteractive;
  }-*/;

  public final native double getDomLoading() /*-{
    return this.domLoading;
  }-*/;

  public final native double getDomainLookupEnd() /*-{
    return this.domainLookupEnd;
  }-*/;

  public final native double getDomainLookupStart() /*-{
    return this.domainLookupStart;
  }-*/;

  public final native double getFetchStart() /*-{
    return this.fetchStart;
  }-*/;

  public final native double getLoadEventEnd() /*-{
    return this.loadEventEnd;
  }-*/;

  public final native double getLoadEventStart() /*-{
    return this.loadEventStart;
  }-*/;

  public final native double getNavigationStart() /*-{
    return this.navigationStart;
  }-*/;

  public final native double getRedirectEnd() /*-{
    return this.redirectEnd;
  }-*/;

  public final native double getRedirectStart() /*-{
    return this.redirectStart;
  }-*/;

  public final native double getRequestStart() /*-{
    return this.requestStart;
  }-*/;

  public final native double getResponseEnd() /*-{
    return this.responseEnd;
  }-*/;

  public final native double getResponseStart() /*-{
    return this.responseStart;
  }-*/;

  public final native double getSecureConnectionStart() /*-{
    return this.secureConnectionStart;
  }-*/;

  public final native double getUnloadEventEnd() /*-{
    return this.unloadEventEnd;
  }-*/;

  public final native double getUnloadEventStart() /*-{
    return this.unloadEventStart;
  }-*/;
}
