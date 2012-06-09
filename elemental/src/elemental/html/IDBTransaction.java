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
import elemental.dom.DOMError;
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
  * <p>The <code>IDBTransaction</code> interface of the <a title="en/IndexedDB" rel="internal" href="https://developer.mozilla.org/en/IndexedDB">IndexedDB&nbsp;API</a> provides a static, asynchronous transaction on a database using event handler attributes. All reading and writing of data are done within transactions. You actually use <code><a title="en/IndexedDB/IDBDatabase" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabase">IDBDatabase</a></code> to start transactions and use <code>IDBTransaction</code> to set the mode of the transaction and access an object store and make your request. You can also use it to abort transactions.</p>
<p>Inherits from: <a title="en/DOM/EventTarget" rel="internal" href="https://developer.mozilla.org/en/DOM/EventTarget">EventTarget</a></p>
  */
public interface IDBTransaction extends EventTarget {

  /**
    * Allows data to be read but not changed.&nbsp;
    */

    static final int READ_ONLY = 0;

  /**
    * Allows reading and writing of data in existing data stores to be changed.
    */

    static final int READ_WRITE = 1;

  /**
    * Allows any operation to be performed, including ones that delete and create object stores and indexes. This mode is for updating the version number of transactions that were started using the <a title="en/IndexedDB/IDBDatabase#setVersion" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabase#setVersion"><code>setVersion()</code></a> method of <a title="en/IndexedDB/IDBDatabase" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabase">IDBDatabase</a> objects. Transactions of this mode cannot run concurrently with other transactions.
    */

    static final int VERSION_CHANGE = 2;


  /**
    * The database connection that this transaction is associated with.
    */
  IDBDatabase getDb();

  DOMError getError();


  /**
    * The mode for isolating access to data in the object stores that are in the scope of the transaction. For possible values, see Constants. The default value is <code><a href="#const_read_only" title="#const read only">READ_ONLY</a></code>.
    */
  String getMode();

  EventListener getOnabort();

  void setOnabort(EventListener arg);

  EventListener getOncomplete();

  void setOncomplete(EventListener arg);

  EventListener getOnerror();

  void setOnerror(EventListener arg);


  /**
    * <p>Returns immediately, and undoes all the changes to objects in the database associated with this transaction. If this transaction has been aborted or completed, then this method throws an <a title="en/IndexedDB/IDBErrorEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent">error event</a>, with its <a title="en/IndexedDB/IDBErrorEvent#attr code" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_code">code</a> set to <code><a title="en/IndexedDB/IDBDatabaseException#ABORT ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#ABORT_ERR">ABORT_ERR</a></code> and a suitable <a title="en/IndexedDB/IDBEvent#attr message" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBEvent#attr_message">message</a>.</p>

<p>All pending <a title="IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest"><code>IDBRequest</code></a> objects created during this transaction have their <code>errorCode</code> set to <code>ABORT_ERR</code>.</p>
<div id="section_12"><span id="Exceptions"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a>, with the following code:</p>
<dl> <dt><code><a title="en/IndexedDB/IDBDatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>The transaction has already been committed or aborted.</dd>
</dl>
</div>
    */
  void abort();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  boolean dispatchEvent(Event evt);


  /**
    * <p>Returns an object store that has already been added to the scope of this transaction. Every call to this method on the same transaction object, with the same name, returns the same <a title="en/IndexedDB/IDBObjectStore" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBObjectStore">IDBObjectStore</a> instance. If this method is called on a different transaction object, a different IDBObjectStore instance is returned.</p>

<div id="section_14"><span id="Parameters"></span><h5 class="editable">Parameters</h5>
<dl> <dt>name</dt> <dd>The name of the requested object store.</dd>
</dl>
</div><div id="section_15"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="IDBObjectStore" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBObjectStore">IDBObjectStore</a></code></dt> <dd>An object for accessing the requested object store.</dd>
</dl>
</div><div id="section_16"><span id="Exceptions_2"></span><h5 class="editable">Exceptions</h5>
<p>The method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following code:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code></dt> <dd>The requested object store is not in this transaction's scope.</dd> <dt><code><a title="en/IndexedDB/DatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>request is made on a source object that has been deleted or removed..</dd>
</dl>
</div>
    */
  IDBObjectStore objectStore(String name);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);
}
