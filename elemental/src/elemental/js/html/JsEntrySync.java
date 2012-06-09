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
import elemental.html.Metadata;
import elemental.html.DirectoryEntrySync;
import elemental.html.EntrySync;
import elemental.html.DOMFileSystemSync;

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

public class JsEntrySync extends JsElementalMixinBase  implements EntrySync {
  protected JsEntrySync() {}

  public final native JsDOMFileSystemSync getFilesystem() /*-{
    return this.filesystem;
  }-*/;

  public final native String getFullPath() /*-{
    return this.fullPath;
  }-*/;

  public final native boolean isDirectory() /*-{
    return this.isDirectory;
  }-*/;

  public final native boolean isFile() /*-{
    return this.isFile;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native JsEntrySync copyTo(DirectoryEntrySync parent, String name) /*-{
    return this.copyTo(parent, name);
  }-*/;

  public final native JsMetadata getMetadata() /*-{
    return this.getMetadata();
  }-*/;

  public final native JsEntrySync getParent() /*-{
    return this.getParent();
  }-*/;

  public final native JsEntrySync moveTo(DirectoryEntrySync parent, String name) /*-{
    return this.moveTo(parent, name);
  }-*/;

  public final native void remove() /*-{
    this.remove();
  }-*/;

  public final native String toURL() /*-{
    return this.toURL();
  }-*/;
}
