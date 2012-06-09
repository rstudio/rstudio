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
import elemental.html.VoidCallback;
import elemental.html.EntryCallback;
import elemental.html.Entry;
import elemental.html.DirectoryEntry;
import elemental.html.ErrorCallback;
import elemental.html.DirectoryReader;

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

public class JsDirectoryEntry extends JsEntry  implements DirectoryEntry {
  protected JsDirectoryEntry() {}

  public final native JsDirectoryReader createReader() /*-{
    return this.createReader();
  }-*/;

  public final native void getDirectory(String path) /*-{
    this.getDirectory(path);
  }-*/;

  public final native void getDirectory(String path, Object flags) /*-{
    this.getDirectory(path, flags);
  }-*/;

  public final native void getDirectory(String path, Object flags, EntryCallback successCallback) /*-{
    this.getDirectory(path, flags, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback));
  }-*/;

  public final native void getDirectory(String path, Object flags, EntryCallback successCallback, ErrorCallback errorCallback) /*-{
    this.getDirectory(path, flags, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;

  public final native void getFile(String path) /*-{
    this.getFile(path);
  }-*/;

  public final native void getFile(String path, Object flags) /*-{
    this.getFile(path, flags);
  }-*/;

  public final native void getFile(String path, Object flags, EntryCallback successCallback) /*-{
    this.getFile(path, flags, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback));
  }-*/;

  public final native void getFile(String path, Object flags, EntryCallback successCallback, ErrorCallback errorCallback) /*-{
    this.getFile(path, flags, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;

  public final native void removeRecursively(VoidCallback successCallback) /*-{
    this.removeRecursively($entry(successCallback.@elemental.html.VoidCallback::onVoidCallback()).bind(successCallback));
  }-*/;

  public final native void removeRecursively(VoidCallback successCallback, ErrorCallback errorCallback) /*-{
    this.removeRecursively($entry(successCallback.@elemental.html.VoidCallback::onVoidCallback()).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;
}
