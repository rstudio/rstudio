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
import elemental.html.DatabaseSync;
import elemental.html.FileSystemCallback;
import elemental.html.WorkerLocation;
import elemental.dom.TimeoutHandler;
import elemental.html.NotificationCenter;
import elemental.html.WorkerGlobalScope;
import elemental.html.EntryCallback;
import elemental.html.WorkerNavigator;
import elemental.html.EntrySync;
import elemental.html.DOMFileSystemSync;
import elemental.js.events.JsEvent;
import elemental.html.DatabaseCallback;
import elemental.events.EventListener;
import elemental.html.ErrorCallback;
import elemental.html.IDBFactory;
import elemental.html.Database;
import elemental.js.events.JsEventListener;
import elemental.events.Event;

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

public class JsWorkerGlobalScope extends JsElementalMixinBase  implements WorkerGlobalScope {
  protected JsWorkerGlobalScope() {}

  public final native JsWorkerLocation getLocation() /*-{
    return this.location;
  }-*/;

  public final native JsWorkerNavigator getNavigator() /*-{
    return this.navigator;
  }-*/;

  public final native EventListener getOnerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onerror);
  }-*/;

  public final native void setOnerror(EventListener listener) /*-{
    this.onerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native JsWorkerGlobalScope getSelf() /*-{
    return this.self;
  }-*/;

  public final native JsIDBFactory getWebkitIndexedDB() /*-{
    return this.webkitIndexedDB;
  }-*/;

  public final native JsNotificationCenter getWebkitNotifications() /*-{
    return this.webkitNotifications;
  }-*/;

  public final native void clearInterval(int handle) /*-{
    this.clearInterval(handle);
  }-*/;

  public final native void clearTimeout(int handle) /*-{
    this.clearTimeout(handle);
  }-*/;

  public final native void close() /*-{
    this.close();
  }-*/;

  public final native void importScripts() /*-{
    this.importScripts();
  }-*/;

  public final native JsDatabase openDatabase(String name, String version, String displayName, int estimatedSize, DatabaseCallback creationCallback) /*-{
    return this.openDatabase(name, version, displayName, estimatedSize, $entry(creationCallback.@elemental.html.DatabaseCallback::onDatabaseCallback(Ljava/lang/Object;)).bind(creationCallback));
  }-*/;

  public final native JsDatabase openDatabase(String name, String version, String displayName, int estimatedSize) /*-{
    return this.openDatabase(name, version, displayName, estimatedSize);
  }-*/;

  public final native JsDatabaseSync openDatabaseSync(String name, String version, String displayName, int estimatedSize, DatabaseCallback creationCallback) /*-{
    return this.openDatabaseSync(name, version, displayName, estimatedSize, $entry(creationCallback.@elemental.html.DatabaseCallback::onDatabaseCallback(Ljava/lang/Object;)).bind(creationCallback));
  }-*/;

  public final native JsDatabaseSync openDatabaseSync(String name, String version, String displayName, int estimatedSize) /*-{
    return this.openDatabaseSync(name, version, displayName, estimatedSize);
  }-*/;

  public final native int setInterval(TimeoutHandler handler, int timeout) /*-{
    return this.setInterval($entry(handler.@elemental.dom.TimeoutHandler::onTimeoutHandler()).bind(handler), timeout);
  }-*/;

  public final native int setTimeout(TimeoutHandler handler, int timeout) /*-{
    return this.setTimeout($entry(handler.@elemental.dom.TimeoutHandler::onTimeoutHandler()).bind(handler), timeout);
  }-*/;

  public final native void webkitRequestFileSystem(int type, double size, FileSystemCallback successCallback, ErrorCallback errorCallback) /*-{
    this.webkitRequestFileSystem(type, size, $entry(successCallback.@elemental.html.FileSystemCallback::onFileSystemCallback(Lelemental/html/DOMFileSystem;)).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;

  public final native void webkitRequestFileSystem(int type, double size, FileSystemCallback successCallback) /*-{
    this.webkitRequestFileSystem(type, size, $entry(successCallback.@elemental.html.FileSystemCallback::onFileSystemCallback(Lelemental/html/DOMFileSystem;)).bind(successCallback));
  }-*/;

  public final native void webkitRequestFileSystem(int type, double size) /*-{
    this.webkitRequestFileSystem(type, size);
  }-*/;

  public final native JsDOMFileSystemSync webkitRequestFileSystemSync(int type, double size) /*-{
    return this.webkitRequestFileSystemSync(type, size);
  }-*/;

  public final native JsEntrySync webkitResolveLocalFileSystemSyncURL(String url) /*-{
    return this.webkitResolveLocalFileSystemSyncURL(url);
  }-*/;

  public final native void webkitResolveLocalFileSystemURL(String url, EntryCallback successCallback) /*-{
    this.webkitResolveLocalFileSystemURL(url, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback));
  }-*/;

  public final native void webkitResolveLocalFileSystemURL(String url) /*-{
    this.webkitResolveLocalFileSystemURL(url);
  }-*/;

  public final native void webkitResolveLocalFileSystemURL(String url, EntryCallback successCallback, ErrorCallback errorCallback) /*-{
    this.webkitResolveLocalFileSystemURL(url, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;
}
