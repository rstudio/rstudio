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
import elemental.dom.DataTransferItemList;
import elemental.js.html.JsImageElement;
import elemental.util.Indexable;
import elemental.js.html.JsFileList;
import elemental.html.FileList;
import elemental.js.util.JsIndexable;
import elemental.dom.Clipboard;
import elemental.html.ImageElement;

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

public class JsClipboard extends JsElementalMixinBase  implements Clipboard {
  protected JsClipboard() {}

  public final native String getDropEffect() /*-{
    return this.dropEffect;
  }-*/;

  public final native void setDropEffect(String param_dropEffect) /*-{
    this.dropEffect = param_dropEffect;
  }-*/;

  public final native String getEffectAllowed() /*-{
    return this.effectAllowed;
  }-*/;

  public final native void setEffectAllowed(String param_effectAllowed) /*-{
    this.effectAllowed = param_effectAllowed;
  }-*/;

  public final native JsFileList getFiles() /*-{
    return this.files;
  }-*/;

  public final native JsDataTransferItemList getItems() /*-{
    return this.items;
  }-*/;

  public final native JsIndexable getTypes() /*-{
    return this.types;
  }-*/;

  public final native void clearData() /*-{
    this.clearData();
  }-*/;

  public final native void clearData(String type) /*-{
    this.clearData(type);
  }-*/;

  public final native String getData(String type) /*-{
    return this.getData(type);
  }-*/;

  public final native boolean setData(String type, String data) /*-{
    return this.setData(type, data);
  }-*/;

  public final native void setDragImage(ImageElement image, int x, int y) /*-{
    this.setDragImage(image, x, y);
  }-*/;
}
