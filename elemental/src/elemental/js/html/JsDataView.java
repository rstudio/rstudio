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
import elemental.html.DataView;
import elemental.html.ArrayBufferView;

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

public class JsDataView extends JsArrayBufferView  implements DataView {
  protected JsDataView() {}

  public final native float getFloat32(int byteOffset) /*-{
    return this.getFloat32(byteOffset);
  }-*/;

  public final native float getFloat32(int byteOffset, boolean littleEndian) /*-{
    return this.getFloat32(byteOffset, littleEndian);
  }-*/;

  public final native double getFloat64(int byteOffset) /*-{
    return this.getFloat64(byteOffset);
  }-*/;

  public final native double getFloat64(int byteOffset, boolean littleEndian) /*-{
    return this.getFloat64(byteOffset, littleEndian);
  }-*/;

  public final native short getInt16(int byteOffset) /*-{
    return this.getInt16(byteOffset);
  }-*/;

  public final native short getInt16(int byteOffset, boolean littleEndian) /*-{
    return this.getInt16(byteOffset, littleEndian);
  }-*/;

  public final native int getInt32(int byteOffset) /*-{
    return this.getInt32(byteOffset);
  }-*/;

  public final native int getInt32(int byteOffset, boolean littleEndian) /*-{
    return this.getInt32(byteOffset, littleEndian);
  }-*/;

  public final native Object getInt8() /*-{
    return this.getInt8();
  }-*/;

  public final native int getUint16(int byteOffset) /*-{
    return this.getUint16(byteOffset);
  }-*/;

  public final native int getUint16(int byteOffset, boolean littleEndian) /*-{
    return this.getUint16(byteOffset, littleEndian);
  }-*/;

  public final native int getUint32(int byteOffset) /*-{
    return this.getUint32(byteOffset);
  }-*/;

  public final native int getUint32(int byteOffset, boolean littleEndian) /*-{
    return this.getUint32(byteOffset, littleEndian);
  }-*/;

  public final native Object getUint8() /*-{
    return this.getUint8();
  }-*/;

  public final native void setFloat32(int byteOffset, float value) /*-{
    this.setFloat32(byteOffset, value);
  }-*/;

  public final native void setFloat32(int byteOffset, float value, boolean littleEndian) /*-{
    this.setFloat32(byteOffset, value, littleEndian);
  }-*/;

  public final native void setFloat64(int byteOffset, double value) /*-{
    this.setFloat64(byteOffset, value);
  }-*/;

  public final native void setFloat64(int byteOffset, double value, boolean littleEndian) /*-{
    this.setFloat64(byteOffset, value, littleEndian);
  }-*/;

  public final native void setInt16(int byteOffset, short value) /*-{
    this.setInt16(byteOffset, value);
  }-*/;

  public final native void setInt16(int byteOffset, short value, boolean littleEndian) /*-{
    this.setInt16(byteOffset, value, littleEndian);
  }-*/;

  public final native void setInt32(int byteOffset, int value) /*-{
    this.setInt32(byteOffset, value);
  }-*/;

  public final native void setInt32(int byteOffset, int value, boolean littleEndian) /*-{
    this.setInt32(byteOffset, value, littleEndian);
  }-*/;

  public final native void setInt8() /*-{
    this.setInt8();
  }-*/;

  public final native void setUint16(int byteOffset, int value) /*-{
    this.setUint16(byteOffset, value);
  }-*/;

  public final native void setUint16(int byteOffset, int value, boolean littleEndian) /*-{
    this.setUint16(byteOffset, value, littleEndian);
  }-*/;

  public final native void setUint32(int byteOffset, int value) /*-{
    this.setUint32(byteOffset, value);
  }-*/;

  public final native void setUint32(int byteOffset, int value, boolean littleEndian) /*-{
    this.setUint32(byteOffset, value, littleEndian);
  }-*/;

  public final native void setUint8() /*-{
    this.setUint8();
  }-*/;
}
