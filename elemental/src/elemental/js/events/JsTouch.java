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
package elemental.js.events;
import elemental.events.Touch;
import elemental.events.EventTarget;

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

public class JsTouch extends JsElementalMixinBase  implements Touch {
  protected JsTouch() {}

  public final native int getClientX() /*-{
    return this.clientX;
  }-*/;

  public final native int getClientY() /*-{
    return this.clientY;
  }-*/;

  public final native int getIdentifier() /*-{
    return this.identifier;
  }-*/;

  public final native int getPageX() /*-{
    return this.pageX;
  }-*/;

  public final native int getPageY() /*-{
    return this.pageY;
  }-*/;

  public final native int getScreenX() /*-{
    return this.screenX;
  }-*/;

  public final native int getScreenY() /*-{
    return this.screenY;
  }-*/;

  public final native EventTarget getTarget() /*-{
    return this.target;
  }-*/;

  public final native float getWebkitForce() /*-{
    return this.webkitForce;
  }-*/;

  public final native int getWebkitRadiusX() /*-{
    return this.webkitRadiusX;
  }-*/;

  public final native int getWebkitRadiusY() /*-{
    return this.webkitRadiusY;
  }-*/;

  public final native float getWebkitRotationAngle() /*-{
    return this.webkitRotationAngle;
  }-*/;
}
