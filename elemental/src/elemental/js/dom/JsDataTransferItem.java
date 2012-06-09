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
package elemental.js.dom;
import elemental.js.html.JsBlob;
import elemental.dom.StringCallback;
import elemental.dom.DataTransferItem;
import elemental.html.Blob;

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

public class JsDataTransferItem extends JsElementalMixinBase  implements DataTransferItem {
  protected JsDataTransferItem() {}

  public final native String getKind() /*-{
    return this.kind;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;

  public final native JsBlob getAsFile() /*-{
    return this.getAsFile();
  }-*/;

  public final native void getAsString() /*-{
    this.getAsString();
  }-*/;

  public final native void getAsString(StringCallback callback) /*-{
    this.getAsString($entry(callback.@elemental.dom.StringCallback::onStringCallback(Ljava/lang/String;)).bind(callback));
  }-*/;
}
