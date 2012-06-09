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
package elemental.html;
import elemental.dom.TimeoutHandler;
import elemental.events.EventListener;
import elemental.events.EventTarget;
import elemental.events.Event;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * 
  */
public interface WorkerGlobalScope extends EventTarget {

    static final int PERSISTENT = 1;

    static final int TEMPORARY = 0;

  WorkerLocation getLocation();

  WorkerNavigator getNavigator();

  EventListener getOnerror();

  void setOnerror(EventListener arg);

  WorkerGlobalScope getSelf();

  IDBFactory getWebkitIndexedDB();

  NotificationCenter getWebkitNotifications();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  void clearInterval(int handle);

  void clearTimeout(int handle);

  void close();

  boolean dispatchEvent(Event evt);

  void importScripts();

  Database openDatabase(String name, String version, String displayName, int estimatedSize, DatabaseCallback creationCallback);

  Database openDatabase(String name, String version, String displayName, int estimatedSize);

  DatabaseSync openDatabaseSync(String name, String version, String displayName, int estimatedSize, DatabaseCallback creationCallback);

  DatabaseSync openDatabaseSync(String name, String version, String displayName, int estimatedSize);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);

  int setInterval(TimeoutHandler handler, int timeout);

  int setTimeout(TimeoutHandler handler, int timeout);

  void webkitRequestFileSystem(int type, double size, FileSystemCallback successCallback, ErrorCallback errorCallback);

  void webkitRequestFileSystem(int type, double size, FileSystemCallback successCallback);

  void webkitRequestFileSystem(int type, double size);

  DOMFileSystemSync webkitRequestFileSystemSync(int type, double size);

  EntrySync webkitResolveLocalFileSystemSyncURL(String url);

  void webkitResolveLocalFileSystemURL(String url, EntryCallback successCallback);

  void webkitResolveLocalFileSystemURL(String url);

  void webkitResolveLocalFileSystemURL(String url, EntryCallback successCallback, ErrorCallback errorCallback);
}
