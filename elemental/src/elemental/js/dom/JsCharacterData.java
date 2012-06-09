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
import elemental.dom.Node;
import elemental.dom.CharacterData;

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

public class JsCharacterData extends JsNode  implements CharacterData {
  protected JsCharacterData() {}

  public final native String getData() /*-{
    return this.data;
  }-*/;

  public final native void setData(String param_data) /*-{
    this.data = param_data;
  }-*/;

  public final native int getLength() /*-{
    return this.length;
  }-*/;

  public final native void appendData(String data) /*-{
    this.appendData(data);
  }-*/;

  public final native void deleteData(int offset, int length) /*-{
    this.deleteData(offset, length);
  }-*/;

  public final native void insertData(int offset, String data) /*-{
    this.insertData(offset, data);
  }-*/;

  public final native void replaceData(int offset, int length, String data) /*-{
    this.replaceData(offset, length, data);
  }-*/;

  public final native String substringData(int offset, int length) /*-{
    return this.substringData(offset, length);
  }-*/;
}
