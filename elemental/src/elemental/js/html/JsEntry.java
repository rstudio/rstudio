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
import elemental.html.MetadataCallback;
import elemental.html.DOMFileSystem;

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

public class JsEntry extends JsElementalMixinBase  implements Entry {
  protected JsEntry() {}

  public final native JsDOMFileSystem getFilesystem() /*-{
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

  public final native void copyTo(DirectoryEntry parent) /*-{
    this.copyTo(parent);
  }-*/;

  public final native void copyTo(DirectoryEntry parent, String name) /*-{
    this.copyTo(parent, name);
  }-*/;

  public final native void copyTo(DirectoryEntry parent, String name, EntryCallback successCallback) /*-{
    this.copyTo(parent, name, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback));
  }-*/;

  public final native void copyTo(DirectoryEntry parent, String name, EntryCallback successCallback, ErrorCallback errorCallback) /*-{
    this.copyTo(parent, name, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;

  public final native void getMetadata(MetadataCallback successCallback) /*-{
    this.getMetadata($entry(successCallback.@elemental.html.MetadataCallback::onMetadataCallback(Lelemental/html/Metadata;)).bind(successCallback));
  }-*/;

  public final native void getMetadata(MetadataCallback successCallback, ErrorCallback errorCallback) /*-{
    this.getMetadata($entry(successCallback.@elemental.html.MetadataCallback::onMetadataCallback(Lelemental/html/Metadata;)).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;

  public final native void getParent() /*-{
    this.getParent();
  }-*/;

  public final native void getParent(EntryCallback successCallback) /*-{
    this.getParent($entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback));
  }-*/;

  public final native void getParent(EntryCallback successCallback, ErrorCallback errorCallback) /*-{
    this.getParent($entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;

  public final native void moveTo(DirectoryEntry parent) /*-{
    this.moveTo(parent);
  }-*/;

  public final native void moveTo(DirectoryEntry parent, String name) /*-{
    this.moveTo(parent, name);
  }-*/;

  public final native void moveTo(DirectoryEntry parent, String name, EntryCallback successCallback) /*-{
    this.moveTo(parent, name, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback));
  }-*/;

  public final native void moveTo(DirectoryEntry parent, String name, EntryCallback successCallback, ErrorCallback errorCallback) /*-{
    this.moveTo(parent, name, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;

  public final native void remove(VoidCallback successCallback) /*-{
    this.remove($entry(successCallback.@elemental.html.VoidCallback::onVoidCallback()).bind(successCallback));
  }-*/;

  public final native void remove(VoidCallback successCallback, ErrorCallback errorCallback) /*-{
    this.remove($entry(successCallback.@elemental.html.VoidCallback::onVoidCallback()).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;

  public final native String toURL() /*-{
    return this.toURL();
  }-*/;
}
