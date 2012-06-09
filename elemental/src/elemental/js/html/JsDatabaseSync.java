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
import elemental.html.SQLTransactionSyncCallback;
import elemental.html.DatabaseSync;

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

public class JsDatabaseSync extends JsElementalMixinBase  implements DatabaseSync {
  protected JsDatabaseSync() {}

  public final native String getLastErrorMessage() /*-{
    return this.lastErrorMessage;
  }-*/;

  public final native String getVersion() /*-{
    return this.version;
  }-*/;

  public final native void changeVersion(String oldVersion, String newVersion) /*-{
    this.changeVersion(oldVersion, newVersion);
  }-*/;

  public final native void changeVersion(String oldVersion, String newVersion, SQLTransactionSyncCallback callback) /*-{
    this.changeVersion(oldVersion, newVersion, $entry(callback.@elemental.html.SQLTransactionSyncCallback::onSQLTransactionSyncCallback(Lelemental/html/SQLTransactionSync;)).bind(callback));
  }-*/;

  public final native void readTransaction(SQLTransactionSyncCallback callback) /*-{
    this.readTransaction($entry(callback.@elemental.html.SQLTransactionSyncCallback::onSQLTransactionSyncCallback(Lelemental/html/SQLTransactionSync;)).bind(callback));
  }-*/;

  public final native void transaction(SQLTransactionSyncCallback callback) /*-{
    this.transaction($entry(callback.@elemental.html.SQLTransactionSyncCallback::onSQLTransactionSyncCallback(Lelemental/html/SQLTransactionSync;)).bind(callback));
  }-*/;
}
