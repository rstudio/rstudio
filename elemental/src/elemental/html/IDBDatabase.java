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
import elemental.util.Mappable;
import elemental.util.Indexable;
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
  * <p>The <code>IDBDatabase</code> interface of the IndexedDB&nbsp;API provides asynchronous access to a <a title="en/IndexedDB#database connection" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#database_connection">connection to a database</a>. Use it to create, manipulate, and delete objects in that database. The interface also provides the only way to get a <a title="en/IndexedDB#gloss transaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_transaction">transaction</a>&nbsp;and manage versions on that database.</p>
<p>Inherits from: <a title="en/DOM/EventTarget" rel="internal" href="https://developer.mozilla.org/en/DOM/EventTarget">EventTarget</a></p>
  */
public interface IDBDatabase extends EventTarget {


  /**
    * Name of the connected database.
    */
  String getName();


  /**
    * A list of the names of the <a title="en/IndexedDB#gloss object store" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_object_store">object stores</a> currently in the connected database.
    */
  Indexable getObjectStoreNames();

  EventListener getOnabort();

  void setOnabort(EventListener arg);

  EventListener getOnerror();

  void setOnerror(EventListener arg);

  EventListener getOnversionchange();

  void setOnversionchange(EventListener arg);


  /**
    * The version of the connected database. When a database is first created, this attribute is the empty string.
    */
  String getVersion();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);


  /**
    * <p>Returns immediately and closes the connection in a separate thread. The connection is not actually closed until all transactions created using this connection are complete. No new transactions can be created for this connection once this method is called. Methods that create transactions throw an exception if a closing operation is pending.</p>
<pre>void close();
</pre>
    */
  void close();


  /**
    * <p>Creates and returns a new object store or index. The method takes the name of the store as well as a parameter object. The parameter object lets you define important optional properties. You can use the property to uniquely identify individual objects in the store. As the property is an identifier, it should be unique to every object, and every object should have that property.</p>
<p>But before you can create any object store or index,&nbsp;you must first call the <code><a href="#setVersion()">setVersion()</a></code><a href="#setVersion()"> method</a>.</p>

<div id="section_11"><span id="Parameters"></span><h5 class="editable">Parameters</h5>
<dl> <dt>name</dt> <dd>The name of the new object store.</dd> <dt>optionalParameters</dt> <dd> <div class="warning"><strong>Warning:</strong> The latest draft of the specification changed this to <code>IDBDatabaseOptionalParameters</code>, which is not yet recognized by any browser</div> <p><em>Optional</em>. Options object whose attributes are optional parameters to the method. It includes the following properties:</p> <table class="standard-table"> <thead> <tr> <th scope="col" width="131">Attribute</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code>keyPath</code></td> <td>The <a title="en/IndexedDB#gloss key path" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_path">key path</a> to be used by the new object store. If empty or not specified, the object store is created without a key path and uses <a title="en/IndexedDB#gloss out-of-line key" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_out-of-line_key">out-of-line keys</a>.</td> </tr> <tr> <td><code>autoIncrement</code></td> <td>If true, the object store has a <a title="en/IndexedDB#gloss key generator" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_generator">key generator</a>. Defaults to <code>false</code>.</td> </tr> </tbody> </table> <p>Unknown parameters are ignored.</p> </dd>
</dl>
</div><div id="section_12"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBObjectStore" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBObjectStore">IDBObjectStore</a></code></dt> <dd>The newly created object store.</dd>
</dl>
</div><div id="section_13"><span id="Exceptions"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The method was not called from a <code><a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE">VERSION_CHANGE</a></code> transaction callback. You must call <code>setVersion()</code> first.</td> </tr> <tr> <td><code><a title="en/IndexedDB/DatabaseException#CONSTRAINT ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#CONSTRAINT_ERR">CONSTRAINT_ERR</a></code></td> <td>An object store with the given name (based on case-sensitive comparison) already exists in the connected database.</td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NON_TRANSIENT_ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NON_TRANSIENT_ERR">NON_TRANSIENT_ERR</a></code></td> <td><code>optionalParameters</code> has attributes other than <code>keyPath</code> and <code>autoIncrement</code>.</td> </tr> </tbody>
</table>
</div>
    */
  IDBObjectStore createObjectStore(String name);


  /**
    * <p>Creates and returns a new object store or index. The method takes the name of the store as well as a parameter object. The parameter object lets you define important optional properties. You can use the property to uniquely identify individual objects in the store. As the property is an identifier, it should be unique to every object, and every object should have that property.</p>
<p>But before you can create any object store or index,&nbsp;you must first call the <code><a href="#setVersion()">setVersion()</a></code><a href="#setVersion()"> method</a>.</p>

<div id="section_11"><span id="Parameters"></span><h5 class="editable">Parameters</h5>
<dl> <dt>name</dt> <dd>The name of the new object store.</dd> <dt>optionalParameters</dt> <dd> <div class="warning"><strong>Warning:</strong> The latest draft of the specification changed this to <code>IDBDatabaseOptionalParameters</code>, which is not yet recognized by any browser</div> <p><em>Optional</em>. Options object whose attributes are optional parameters to the method. It includes the following properties:</p> <table class="standard-table"> <thead> <tr> <th scope="col" width="131">Attribute</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code>keyPath</code></td> <td>The <a title="en/IndexedDB#gloss key path" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_path">key path</a> to be used by the new object store. If empty or not specified, the object store is created without a key path and uses <a title="en/IndexedDB#gloss out-of-line key" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_out-of-line_key">out-of-line keys</a>.</td> </tr> <tr> <td><code>autoIncrement</code></td> <td>If true, the object store has a <a title="en/IndexedDB#gloss key generator" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_generator">key generator</a>. Defaults to <code>false</code>.</td> </tr> </tbody> </table> <p>Unknown parameters are ignored.</p> </dd>
</dl>
</div><div id="section_12"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBObjectStore" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBObjectStore">IDBObjectStore</a></code></dt> <dd>The newly created object store.</dd>
</dl>
</div><div id="section_13"><span id="Exceptions"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The method was not called from a <code><a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE">VERSION_CHANGE</a></code> transaction callback. You must call <code>setVersion()</code> first.</td> </tr> <tr> <td><code><a title="en/IndexedDB/DatabaseException#CONSTRAINT ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#CONSTRAINT_ERR">CONSTRAINT_ERR</a></code></td> <td>An object store with the given name (based on case-sensitive comparison) already exists in the connected database.</td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NON_TRANSIENT_ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NON_TRANSIENT_ERR">NON_TRANSIENT_ERR</a></code></td> <td><code>optionalParameters</code> has attributes other than <code>keyPath</code> and <code>autoIncrement</code>.</td> </tr> </tbody>
</table>
</div>
    */
  IDBObjectStore createObjectStore(String name, Mappable options);


  /**
    * <p>Destroys the object store with the given name in the connected database, along with any indexes that reference it.&nbsp;</p>
<p>As with <code>createObjectStore()</code>, this method can be called <em>only</em> within a <code><a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE">VERSION_CHANGE</a></code> transaction. So you must call the <code>setVersion()</code> method first before you can remove any object store or index.</p>

<div id="section_15"><span id="Parameters_2"></span><h5 class="editable">Parameters</h5>
<dl> <dt>name</dt> <dd>The name of the data store to delete.</dd>
</dl>
</div><div id="section_16"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<p><code>void</code></p>
</div><div id="section_17"><span id="Exceptions_2"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The method was not called from a <code><a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE">VERSION_CHANGE</a></code> transaction callback. You must call <code>setVersion()</code> first.</td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code></td> <td>You are trying to delete an object store that does not exist. Names are case sensitive.</td> </tr> </tbody>
</table>
</div>
    */
  void deleteObjectStore(String name);

  boolean dispatchEvent(Event evt);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction">IDBTransaction</a> object, and starts a transaction in a separate thread. &nbsp;The method returns a transaction object (<a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction"><code>IDBTransaction</code></a>) containing the <a title="en/IndexedDB/IDBTransaction#objectStore()" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#objectStore()">objectStore()</a> method, which you can use to access your object store.&nbsp;</p>

<div id="section_22"><span id="Parameters_4"></span><h5 class="editable">Parameters</h5>
<dl> <dt>storeNames</dt> <dd>The names of object stores and indexes that are in the scope of the new transaction. Specify only the object stores that you need to access.</dd> <dt>mode</dt> <dd><em>Optional</em>. The types of access that can be performed in the transaction. Transactions are opened in one of three modes: <code>READ_ONLY</code>, <code>READ_WRITE</code>, and <code>VERSION_CHANGE</code>. If you don't provide the parameter, the default access mode is <code>READ_ONLY</code>. To avoid slowing things down, don't open a <code>READ_WRITE</code> transaction, unless you actually need to write into the database.</dd>
</dl>
</div><div id="section_23"><span id="Sample_code"></span><h5 class="editable">Sample code</h5>
<p>To start a transaction with the following scope, you can use the code snippets in the table. As noted earlier:</p>
<ul> <li>Add prefixes to the methods in WebKit browsers, (that is, instead of <code>IDBTransaction.READ_ONLY</code>, use <code>webkitIDBTransaction.READ_ONLY</code>).</li> <li>The default mode is <code>READ_ONLY</code>, so you don't really have to specify it. Of course, if you need to write into the object store, you can open the transaction in the <code>READ_WRITE</code> mode.</li>
</ul>
<table class="standard-table"> <thead> <tr> <th scope="col" width="185">Scope</th> <th scope="col" width="1018">Code</th> </tr> <tr> <td>Single object store</td> <td> <p><code>var transaction = db.transaction(['my-store-name'], IDBTransaction.READ_ONLY); </code></p> <p>Alternatively:</p> <p><code>var transaction = db.transaction('my-store-name', IDBTransaction.READ_ONLY);</code></p> </td> </tr> <tr> <td>Multiple object stores</td> <td><code>var transaction = db.transaction(['my-store-name', 'my-store-name2'], IDBTransaction.READ_ONLY);</code></td> </tr> <tr> <td>All object stores</td> <td> <p><code>var transaction = db.transaction(db.objectStoreNames, IDBTransaction.READ_ONLY);</code></p> <p>You cannot pass an empty array into the storeNames parameter, such as in the following: <code>var transaction = db.transaction([], IDBTransaction.READ_ONLY);.</code></p> <div class="warning"><strong>Warning:</strong>&nbsp; Accessing all obejct stores under the <code>READ_WRITE</code> mode means that you can run only that transaction. You cannot have writing transactions with overlapping scopes.</div> </td> </tr> </thead> <tbody> </tbody>
</table>
</div><div id="section_24"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBVersionChangeRequest">IDBTransaction</a></code></dt> <dd>The transaction object.</dd>
</dl>
</div><div id="section_25"><span id="Exceptions_3"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The error is thrown for one of two reasons: <ul> <li>The <code>close()</code> method has been called on this IDBDatabase instance.</li> <li>The object store has been deleted or removed.</li> </ul> </td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code></td> <td>One of the object stores doesn't exist in the connected database.</td> </tr> </tbody>
</table>
</div>
    */
  IDBTransaction transaction(Indexable storeNames);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction">IDBTransaction</a> object, and starts a transaction in a separate thread. &nbsp;The method returns a transaction object (<a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction"><code>IDBTransaction</code></a>) containing the <a title="en/IndexedDB/IDBTransaction#objectStore()" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#objectStore()">objectStore()</a> method, which you can use to access your object store.&nbsp;</p>

<div id="section_22"><span id="Parameters_4"></span><h5 class="editable">Parameters</h5>
<dl> <dt>storeNames</dt> <dd>The names of object stores and indexes that are in the scope of the new transaction. Specify only the object stores that you need to access.</dd> <dt>mode</dt> <dd><em>Optional</em>. The types of access that can be performed in the transaction. Transactions are opened in one of three modes: <code>READ_ONLY</code>, <code>READ_WRITE</code>, and <code>VERSION_CHANGE</code>. If you don't provide the parameter, the default access mode is <code>READ_ONLY</code>. To avoid slowing things down, don't open a <code>READ_WRITE</code> transaction, unless you actually need to write into the database.</dd>
</dl>
</div><div id="section_23"><span id="Sample_code"></span><h5 class="editable">Sample code</h5>
<p>To start a transaction with the following scope, you can use the code snippets in the table. As noted earlier:</p>
<ul> <li>Add prefixes to the methods in WebKit browsers, (that is, instead of <code>IDBTransaction.READ_ONLY</code>, use <code>webkitIDBTransaction.READ_ONLY</code>).</li> <li>The default mode is <code>READ_ONLY</code>, so you don't really have to specify it. Of course, if you need to write into the object store, you can open the transaction in the <code>READ_WRITE</code> mode.</li>
</ul>
<table class="standard-table"> <thead> <tr> <th scope="col" width="185">Scope</th> <th scope="col" width="1018">Code</th> </tr> <tr> <td>Single object store</td> <td> <p><code>var transaction = db.transaction(['my-store-name'], IDBTransaction.READ_ONLY); </code></p> <p>Alternatively:</p> <p><code>var transaction = db.transaction('my-store-name', IDBTransaction.READ_ONLY);</code></p> </td> </tr> <tr> <td>Multiple object stores</td> <td><code>var transaction = db.transaction(['my-store-name', 'my-store-name2'], IDBTransaction.READ_ONLY);</code></td> </tr> <tr> <td>All object stores</td> <td> <p><code>var transaction = db.transaction(db.objectStoreNames, IDBTransaction.READ_ONLY);</code></p> <p>You cannot pass an empty array into the storeNames parameter, such as in the following: <code>var transaction = db.transaction([], IDBTransaction.READ_ONLY);.</code></p> <div class="warning"><strong>Warning:</strong>&nbsp; Accessing all obejct stores under the <code>READ_WRITE</code> mode means that you can run only that transaction. You cannot have writing transactions with overlapping scopes.</div> </td> </tr> </thead> <tbody> </tbody>
</table>
</div><div id="section_24"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBVersionChangeRequest">IDBTransaction</a></code></dt> <dd>The transaction object.</dd>
</dl>
</div><div id="section_25"><span id="Exceptions_3"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The error is thrown for one of two reasons: <ul> <li>The <code>close()</code> method has been called on this IDBDatabase instance.</li> <li>The object store has been deleted or removed.</li> </ul> </td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code></td> <td>One of the object stores doesn't exist in the connected database.</td> </tr> </tbody>
</table>
</div>
    */
  IDBTransaction transaction(Indexable storeNames, String mode);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction">IDBTransaction</a> object, and starts a transaction in a separate thread. &nbsp;The method returns a transaction object (<a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction"><code>IDBTransaction</code></a>) containing the <a title="en/IndexedDB/IDBTransaction#objectStore()" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#objectStore()">objectStore()</a> method, which you can use to access your object store.&nbsp;</p>

<div id="section_22"><span id="Parameters_4"></span><h5 class="editable">Parameters</h5>
<dl> <dt>storeNames</dt> <dd>The names of object stores and indexes that are in the scope of the new transaction. Specify only the object stores that you need to access.</dd> <dt>mode</dt> <dd><em>Optional</em>. The types of access that can be performed in the transaction. Transactions are opened in one of three modes: <code>READ_ONLY</code>, <code>READ_WRITE</code>, and <code>VERSION_CHANGE</code>. If you don't provide the parameter, the default access mode is <code>READ_ONLY</code>. To avoid slowing things down, don't open a <code>READ_WRITE</code> transaction, unless you actually need to write into the database.</dd>
</dl>
</div><div id="section_23"><span id="Sample_code"></span><h5 class="editable">Sample code</h5>
<p>To start a transaction with the following scope, you can use the code snippets in the table. As noted earlier:</p>
<ul> <li>Add prefixes to the methods in WebKit browsers, (that is, instead of <code>IDBTransaction.READ_ONLY</code>, use <code>webkitIDBTransaction.READ_ONLY</code>).</li> <li>The default mode is <code>READ_ONLY</code>, so you don't really have to specify it. Of course, if you need to write into the object store, you can open the transaction in the <code>READ_WRITE</code> mode.</li>
</ul>
<table class="standard-table"> <thead> <tr> <th scope="col" width="185">Scope</th> <th scope="col" width="1018">Code</th> </tr> <tr> <td>Single object store</td> <td> <p><code>var transaction = db.transaction(['my-store-name'], IDBTransaction.READ_ONLY); </code></p> <p>Alternatively:</p> <p><code>var transaction = db.transaction('my-store-name', IDBTransaction.READ_ONLY);</code></p> </td> </tr> <tr> <td>Multiple object stores</td> <td><code>var transaction = db.transaction(['my-store-name', 'my-store-name2'], IDBTransaction.READ_ONLY);</code></td> </tr> <tr> <td>All object stores</td> <td> <p><code>var transaction = db.transaction(db.objectStoreNames, IDBTransaction.READ_ONLY);</code></p> <p>You cannot pass an empty array into the storeNames parameter, such as in the following: <code>var transaction = db.transaction([], IDBTransaction.READ_ONLY);.</code></p> <div class="warning"><strong>Warning:</strong>&nbsp; Accessing all obejct stores under the <code>READ_WRITE</code> mode means that you can run only that transaction. You cannot have writing transactions with overlapping scopes.</div> </td> </tr> </thead> <tbody> </tbody>
</table>
</div><div id="section_24"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBVersionChangeRequest">IDBTransaction</a></code></dt> <dd>The transaction object.</dd>
</dl>
</div><div id="section_25"><span id="Exceptions_3"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The error is thrown for one of two reasons: <ul> <li>The <code>close()</code> method has been called on this IDBDatabase instance.</li> <li>The object store has been deleted or removed.</li> </ul> </td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code></td> <td>One of the object stores doesn't exist in the connected database.</td> </tr> </tbody>
</table>
</div>
    */
  IDBTransaction transaction(String storeName);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction">IDBTransaction</a> object, and starts a transaction in a separate thread. &nbsp;The method returns a transaction object (<a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction"><code>IDBTransaction</code></a>) containing the <a title="en/IndexedDB/IDBTransaction#objectStore()" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#objectStore()">objectStore()</a> method, which you can use to access your object store.&nbsp;</p>

<div id="section_22"><span id="Parameters_4"></span><h5 class="editable">Parameters</h5>
<dl> <dt>storeNames</dt> <dd>The names of object stores and indexes that are in the scope of the new transaction. Specify only the object stores that you need to access.</dd> <dt>mode</dt> <dd><em>Optional</em>. The types of access that can be performed in the transaction. Transactions are opened in one of three modes: <code>READ_ONLY</code>, <code>READ_WRITE</code>, and <code>VERSION_CHANGE</code>. If you don't provide the parameter, the default access mode is <code>READ_ONLY</code>. To avoid slowing things down, don't open a <code>READ_WRITE</code> transaction, unless you actually need to write into the database.</dd>
</dl>
</div><div id="section_23"><span id="Sample_code"></span><h5 class="editable">Sample code</h5>
<p>To start a transaction with the following scope, you can use the code snippets in the table. As noted earlier:</p>
<ul> <li>Add prefixes to the methods in WebKit browsers, (that is, instead of <code>IDBTransaction.READ_ONLY</code>, use <code>webkitIDBTransaction.READ_ONLY</code>).</li> <li>The default mode is <code>READ_ONLY</code>, so you don't really have to specify it. Of course, if you need to write into the object store, you can open the transaction in the <code>READ_WRITE</code> mode.</li>
</ul>
<table class="standard-table"> <thead> <tr> <th scope="col" width="185">Scope</th> <th scope="col" width="1018">Code</th> </tr> <tr> <td>Single object store</td> <td> <p><code>var transaction = db.transaction(['my-store-name'], IDBTransaction.READ_ONLY); </code></p> <p>Alternatively:</p> <p><code>var transaction = db.transaction('my-store-name', IDBTransaction.READ_ONLY);</code></p> </td> </tr> <tr> <td>Multiple object stores</td> <td><code>var transaction = db.transaction(['my-store-name', 'my-store-name2'], IDBTransaction.READ_ONLY);</code></td> </tr> <tr> <td>All object stores</td> <td> <p><code>var transaction = db.transaction(db.objectStoreNames, IDBTransaction.READ_ONLY);</code></p> <p>You cannot pass an empty array into the storeNames parameter, such as in the following: <code>var transaction = db.transaction([], IDBTransaction.READ_ONLY);.</code></p> <div class="warning"><strong>Warning:</strong>&nbsp; Accessing all obejct stores under the <code>READ_WRITE</code> mode means that you can run only that transaction. You cannot have writing transactions with overlapping scopes.</div> </td> </tr> </thead> <tbody> </tbody>
</table>
</div><div id="section_24"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBVersionChangeRequest">IDBTransaction</a></code></dt> <dd>The transaction object.</dd>
</dl>
</div><div id="section_25"><span id="Exceptions_3"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The error is thrown for one of two reasons: <ul> <li>The <code>close()</code> method has been called on this IDBDatabase instance.</li> <li>The object store has been deleted or removed.</li> </ul> </td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code></td> <td>One of the object stores doesn't exist in the connected database.</td> </tr> </tbody>
</table>
</div>
    */
  IDBTransaction transaction(String storeName, String mode);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction">IDBTransaction</a> object, and starts a transaction in a separate thread. &nbsp;The method returns a transaction object (<a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction"><code>IDBTransaction</code></a>) containing the <a title="en/IndexedDB/IDBTransaction#objectStore()" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#objectStore()">objectStore()</a> method, which you can use to access your object store.&nbsp;</p>

<div id="section_22"><span id="Parameters_4"></span><h5 class="editable">Parameters</h5>
<dl> <dt>storeNames</dt> <dd>The names of object stores and indexes that are in the scope of the new transaction. Specify only the object stores that you need to access.</dd> <dt>mode</dt> <dd><em>Optional</em>. The types of access that can be performed in the transaction. Transactions are opened in one of three modes: <code>READ_ONLY</code>, <code>READ_WRITE</code>, and <code>VERSION_CHANGE</code>. If you don't provide the parameter, the default access mode is <code>READ_ONLY</code>. To avoid slowing things down, don't open a <code>READ_WRITE</code> transaction, unless you actually need to write into the database.</dd>
</dl>
</div><div id="section_23"><span id="Sample_code"></span><h5 class="editable">Sample code</h5>
<p>To start a transaction with the following scope, you can use the code snippets in the table. As noted earlier:</p>
<ul> <li>Add prefixes to the methods in WebKit browsers, (that is, instead of <code>IDBTransaction.READ_ONLY</code>, use <code>webkitIDBTransaction.READ_ONLY</code>).</li> <li>The default mode is <code>READ_ONLY</code>, so you don't really have to specify it. Of course, if you need to write into the object store, you can open the transaction in the <code>READ_WRITE</code> mode.</li>
</ul>
<table class="standard-table"> <thead> <tr> <th scope="col" width="185">Scope</th> <th scope="col" width="1018">Code</th> </tr> <tr> <td>Single object store</td> <td> <p><code>var transaction = db.transaction(['my-store-name'], IDBTransaction.READ_ONLY); </code></p> <p>Alternatively:</p> <p><code>var transaction = db.transaction('my-store-name', IDBTransaction.READ_ONLY);</code></p> </td> </tr> <tr> <td>Multiple object stores</td> <td><code>var transaction = db.transaction(['my-store-name', 'my-store-name2'], IDBTransaction.READ_ONLY);</code></td> </tr> <tr> <td>All object stores</td> <td> <p><code>var transaction = db.transaction(db.objectStoreNames, IDBTransaction.READ_ONLY);</code></p> <p>You cannot pass an empty array into the storeNames parameter, such as in the following: <code>var transaction = db.transaction([], IDBTransaction.READ_ONLY);.</code></p> <div class="warning"><strong>Warning:</strong>&nbsp; Accessing all obejct stores under the <code>READ_WRITE</code> mode means that you can run only that transaction. You cannot have writing transactions with overlapping scopes.</div> </td> </tr> </thead> <tbody> </tbody>
</table>
</div><div id="section_24"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBVersionChangeRequest">IDBTransaction</a></code></dt> <dd>The transaction object.</dd>
</dl>
</div><div id="section_25"><span id="Exceptions_3"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The error is thrown for one of two reasons: <ul> <li>The <code>close()</code> method has been called on this IDBDatabase instance.</li> <li>The object store has been deleted or removed.</li> </ul> </td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code></td> <td>One of the object stores doesn't exist in the connected database.</td> </tr> </tbody>
</table>
</div>
    */
  IDBTransaction transaction(Indexable storeNames, int mode);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction">IDBTransaction</a> object, and starts a transaction in a separate thread. &nbsp;The method returns a transaction object (<a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction"><code>IDBTransaction</code></a>) containing the <a title="en/IndexedDB/IDBTransaction#objectStore()" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#objectStore()">objectStore()</a> method, which you can use to access your object store.&nbsp;</p>

<div id="section_22"><span id="Parameters_4"></span><h5 class="editable">Parameters</h5>
<dl> <dt>storeNames</dt> <dd>The names of object stores and indexes that are in the scope of the new transaction. Specify only the object stores that you need to access.</dd> <dt>mode</dt> <dd><em>Optional</em>. The types of access that can be performed in the transaction. Transactions are opened in one of three modes: <code>READ_ONLY</code>, <code>READ_WRITE</code>, and <code>VERSION_CHANGE</code>. If you don't provide the parameter, the default access mode is <code>READ_ONLY</code>. To avoid slowing things down, don't open a <code>READ_WRITE</code> transaction, unless you actually need to write into the database.</dd>
</dl>
</div><div id="section_23"><span id="Sample_code"></span><h5 class="editable">Sample code</h5>
<p>To start a transaction with the following scope, you can use the code snippets in the table. As noted earlier:</p>
<ul> <li>Add prefixes to the methods in WebKit browsers, (that is, instead of <code>IDBTransaction.READ_ONLY</code>, use <code>webkitIDBTransaction.READ_ONLY</code>).</li> <li>The default mode is <code>READ_ONLY</code>, so you don't really have to specify it. Of course, if you need to write into the object store, you can open the transaction in the <code>READ_WRITE</code> mode.</li>
</ul>
<table class="standard-table"> <thead> <tr> <th scope="col" width="185">Scope</th> <th scope="col" width="1018">Code</th> </tr> <tr> <td>Single object store</td> <td> <p><code>var transaction = db.transaction(['my-store-name'], IDBTransaction.READ_ONLY); </code></p> <p>Alternatively:</p> <p><code>var transaction = db.transaction('my-store-name', IDBTransaction.READ_ONLY);</code></p> </td> </tr> <tr> <td>Multiple object stores</td> <td><code>var transaction = db.transaction(['my-store-name', 'my-store-name2'], IDBTransaction.READ_ONLY);</code></td> </tr> <tr> <td>All object stores</td> <td> <p><code>var transaction = db.transaction(db.objectStoreNames, IDBTransaction.READ_ONLY);</code></p> <p>You cannot pass an empty array into the storeNames parameter, such as in the following: <code>var transaction = db.transaction([], IDBTransaction.READ_ONLY);.</code></p> <div class="warning"><strong>Warning:</strong>&nbsp; Accessing all obejct stores under the <code>READ_WRITE</code> mode means that you can run only that transaction. You cannot have writing transactions with overlapping scopes.</div> </td> </tr> </thead> <tbody> </tbody>
</table>
</div><div id="section_24"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBTransaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBVersionChangeRequest">IDBTransaction</a></code></dt> <dd>The transaction object.</dd>
</dl>
</div><div id="section_25"><span id="Exceptions_3"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The error is thrown for one of two reasons: <ul> <li>The <code>close()</code> method has been called on this IDBDatabase instance.</li> <li>The object store has been deleted or removed.</li> </ul> </td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code></td> <td>One of the object stores doesn't exist in the connected database.</td> </tr> </tbody>
</table>
</div>
    */
  IDBTransaction transaction(String storeName, int mode);
}
