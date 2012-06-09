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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div><p>This content covers features introduced in <a rel="custom" href="https://developer.mozilla.org/en/Firefox_3_for_developers">Firefox 3</a>.</p></div>
<p></p>
<p>This document provides a high-level overview of the overall database design of the <a title="en/Places" rel="internal" href="https://developer.mozilla.org/en/Places">Places</a> system. Places is designed to be a complete replacement for the Firefox bookmarks and history systems using <a title="en/Storage" rel="internal" href="https://developer.mozilla.org/en/Storage">Storage.</a></p>
<p>View the <a class=" external" rel="external" href="http://people.mozilla.org/~dietrich/places-erd.png" title="http://people.mozilla.org/~dietrich/places-erd.png" target="_blank">schema diagram</a>.</p>
  */
public interface Database {

  String getVersion();

  void changeVersion(String oldVersion, String newVersion);

  void changeVersion(String oldVersion, String newVersion, SQLTransactionCallback callback);

  void changeVersion(String oldVersion, String newVersion, SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback);

  void changeVersion(String oldVersion, String newVersion, SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback, VoidCallback successCallback);

  void readTransaction(SQLTransactionCallback callback);

  void readTransaction(SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback);

  void readTransaction(SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback, VoidCallback successCallback);

  void transaction(SQLTransactionCallback callback);

  void transaction(SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback);

  void transaction(SQLTransactionCallback callback, SQLTransactionErrorCallback errorCallback, VoidCallback successCallback);
}
