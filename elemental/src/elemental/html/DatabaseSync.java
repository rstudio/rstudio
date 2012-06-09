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
  * <div><strong>DRAFT</strong>
<div>This page is not complete.</div>
</div>

<p></p>
<p>The <code>DatabaseSync</code> interface in the <a title="en/IndexedDB" rel="internal" href="https://developer.mozilla.org/en/IndexedDB">IndexedDB API</a> represents a synchronous <a title="en/IndexedDB#gloss database connection" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_database_connection">connection to a database</a>.</p>
  */
public interface DatabaseSync {

  String getLastErrorMessage();


  /**
    * The version of the connected database. Has the null value when the database is first created.
    */
  String getVersion();

  void changeVersion(String oldVersion, String newVersion);

  void changeVersion(String oldVersion, String newVersion, SQLTransactionSyncCallback callback);

  void readTransaction(SQLTransactionSyncCallback callback);


  /**
    * <p>Creates and returns a transaction, acquiring locks on the given database objects, within the specified timeout duration, if possible.</p>
<pre>IDBTransactionSync transaction (
  in optional DOMStringList storeNames,
  in optional unsigned int timeout
) raises (IDBDatabaseException);
</pre>
<div id="section_20"><span id="Parameters_5"></span><h5 class="editable">Parameters</h5>
<dl> <dt>storeNames</dt> <dd>The names of object stores and indexes in the scope of the new transaction.</dd> <dt>timeout</dt> <dd>The interval that this operation is allowed to take to acquire locks on all the objects stores and indexes identified in <code>storeNames</code>.</dd>
</dl>
</div><div id="section_21"><span id="Returns_5"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/TransactionSync" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionSync">IDBTransactionSync</a></code></dt> <dd>An object to access the newly created transaction.</dd>
</dl>
</div><div id="section_22"><span id="Exceptions_4"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an IDBDatabaseException with the following code:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#TIMEOUT ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#TIMEOUT_ERR">TIMEOUT_ERR</a></code></dt> <dd>If reserving all the database objects identified in <code>storeNames</code> takes longer than the <code>timeout</code> interval.</dd>
</dl></div>
    */
  void transaction(SQLTransactionSyncCallback callback);
}
