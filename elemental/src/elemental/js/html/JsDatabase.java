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
import elemental.html.SQLTransactionErrorCallback;
import elemental.html.Database;
import elemental.html.SQLTransactionCallback;
import elemental.html.VoidCallback;

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

public class JsDatabase extends JsElementalMixinBase  implements Database {
  protected JsDatabase() {}

  public final native String getVersion() /*-{
    return this.version;
  }-*/;

  public final native void changeVersion(String oldVersion, String newVersion) /*-{
    this.changeVersion(oldVersion, newVersion);
  }-*/;

  public final native void changeVersion(String oldVersion, String newVersion, SQLTransactionCallback callback) /*-{
    this.changeVersion(oldVersion, newVersion, $entry(callback.@elemental.html.SQLTransactionCallback::onSQLTransactionCallback(Lelemental/html/SQLTransaction;)).bind(callback));
  }-*/;

  public final native void changeVersion(String oldVersion, String newVersion, SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback) /*-{
    this.changeVersion(oldVersion, newVersion, $entry(callback.@elemental.html.SQLTransactionCallback::onSQLTransactionCallback(Lelemental/html/SQLTransaction;)).bind(callback), $entry(errorCallback.@elemental.html.SQLTransactionErrorCallback::onSQLTransactionErrorCallback(Lelemental/html/SQLError;)).bind(errorCallback));
  }-*/;

  public final native void changeVersion(String oldVersion, String newVersion, SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback, VoidCallback successCallback) /*-{
    this.changeVersion(oldVersion, newVersion, $entry(callback.@elemental.html.SQLTransactionCallback::onSQLTransactionCallback(Lelemental/html/SQLTransaction;)).bind(callback), $entry(errorCallback.@elemental.html.SQLTransactionErrorCallback::onSQLTransactionErrorCallback(Lelemental/html/SQLError;)).bind(errorCallback), $entry(successCallback.@elemental.html.VoidCallback::onVoidCallback()).bind(successCallback));
  }-*/;

  public final native void readTransaction(SQLTransactionCallback callback) /*-{
    this.readTransaction($entry(callback.@elemental.html.SQLTransactionCallback::onSQLTransactionCallback(Lelemental/html/SQLTransaction;)).bind(callback));
  }-*/;

  public final native void readTransaction(SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback) /*-{
    this.readTransaction($entry(callback.@elemental.html.SQLTransactionCallback::onSQLTransactionCallback(Lelemental/html/SQLTransaction;)).bind(callback), $entry(errorCallback.@elemental.html.SQLTransactionErrorCallback::onSQLTransactionErrorCallback(Lelemental/html/SQLError;)).bind(errorCallback));
  }-*/;

  public final native void readTransaction(SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback, VoidCallback successCallback) /*-{
    this.readTransaction($entry(callback.@elemental.html.SQLTransactionCallback::onSQLTransactionCallback(Lelemental/html/SQLTransaction;)).bind(callback), $entry(errorCallback.@elemental.html.SQLTransactionErrorCallback::onSQLTransactionErrorCallback(Lelemental/html/SQLError;)).bind(errorCallback), $entry(successCallback.@elemental.html.VoidCallback::onVoidCallback()).bind(successCallback));
  }-*/;

  public final native void transaction(SQLTransactionCallback callback) /*-{
    this.transaction($entry(callback.@elemental.html.SQLTransactionCallback::onSQLTransactionCallback(Lelemental/html/SQLTransaction;)).bind(callback));
  }-*/;

  public final native void transaction(SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback) /*-{
    this.transaction($entry(callback.@elemental.html.SQLTransactionCallback::onSQLTransactionCallback(Lelemental/html/SQLTransaction;)).bind(callback), $entry(errorCallback.@elemental.html.SQLTransactionErrorCallback::onSQLTransactionErrorCallback(Lelemental/html/SQLError;)).bind(errorCallback));
  }-*/;

  public final native void transaction(SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback, VoidCallback successCallback) /*-{
    this.transaction($entry(callback.@elemental.html.SQLTransactionCallback::onSQLTransactionCallback(Lelemental/html/SQLTransaction;)).bind(callback), $entry(errorCallback.@elemental.html.SQLTransactionErrorCallback::onSQLTransactionErrorCallback(Lelemental/html/SQLError;)).bind(errorCallback), $entry(successCallback.@elemental.html.VoidCallback::onVoidCallback()).bind(successCallback));
  }-*/;
}
