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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * The <code>IDBObjectStore</code> interface of the <a title="en/IndexedDB" rel="internal" href="https://developer.mozilla.org/en/IndexedDB">IndexedDB API</a> represents an <a title="en/IndexedDB#gloss object store" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_object_store">object store</a> in a database.&nbsp;Records within an object store are sorted according to their keys. This sorting enable fast insertion, look-up, and &nbsp;ordered retrieval.&nbsp;
  */
public interface IDBObjectStore {

  boolean isAutoIncrement();


  /**
    * A list of the names of <a title="en/IndexedDB#gloss index" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_index">indexes</a> on objects in this object store.
    */
  Indexable getIndexNames();


  /**
    * The <a title="en/IndexedDB#gloss key path" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_path">key path</a> of this object store. If this attribute is null, the application must provide a key for each modification operation.
    */
  Object getKeyPath();


  /**
    * The name of this object store.
    */
  String getName();

  IDBTransaction getTransaction();


  /**
    * <p>Returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and, in a separate thread, creates a <a class="external" title="http://www.whatwg.org/specs/web-apps/current-work/multipage/urls.html#structured-clone" rel="external" href="http://www.whatwg.org/specs/web-apps/current-work/multipage/urls.html#structured-clone" target="_blank">structured clone</a> of the <code>value</code>, and stores the cloned value in the object store. If the record is successfully stored, then a success event is fired on the returned request object, using the <a title="en/IndexedDB/IDBTransactionEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent">IDBTransactionEvent</a> interface, with the <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to the key for the stored record, and <code><a title="en/IndexedDB/IDBTransactionEvent#attr transaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent#attr_transaction">transaction</a></code> set to the transaction in which this object store is opened. If a record already exists in the object store with the <code>key</code> parameter as its key, then an error event is fired on the returned request object, with <a title="en/IndexedDB/IDBErrorEvent#attr code" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_code">code</a> set to <code><a title="en/IndexedDB/DatabaseException#CONSTRAINT ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#CONSTRAINT_ERR">CONSTRAINT_ERR</a></code>.</p>

<div id="section_5"><span id="Parameters"></span><h5 class="editable">Parameters</h5>
<dl> <dt>value</dt> <dd>The value to be stored.</dd> <dt>key</dt> <dd>The key to use to identify the record. If unspecified, it results to null.</dd>
</dl>
</div><div id="section_6"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_7"><span id="Exceptions"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#DATA ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#DATA_ERR">DATA_ERR</a></code></dt> <dd>If the object store uses in-line keys or has a key generator, and a key parameter was provided.<br> If the object store uses out-of-line keys and has no key generator, and no key parameter was provided.<br> If the object store uses in-line keys but no key generator, and the object store's key path does not yield a valid key.<br> If the key parameter was provided but does not contain a valid key.<br> If there are indexed on this object store, and using their key path on the value parameter yields a value that is not a valid key.</dd> <dt><code><a title="en/IndexedDB/IDBDatabaseException#READ ONLY ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#READ_ONLY_ERR">READ_ONLY_ERR</a></code></dt> <dd>If the mode of the associated transaction is <code><a title="en/IndexedDB/IDBTransaction#READ ONLY" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#READ_ONLY">READ_ONLY</a></code>.</dd> <dt><code><a title="en/IndexedDB/IDBDatabaseException#TRANSACTION INACTIVE ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#TRANSACTION_INACTIVE_ERR">TRANSACTION_INACTIVE_ERR</a></code></dt> <dd>If the associated transaction is not active.</dd>
</dl>
<p>This method can raise a <a title="en/DOM/DOMException" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMException">DOMException</a> with the following code:</p>
<dl> <dt><code>DATA_CLONE_ERR</code></dt> <dd>If the data being stored could not be cloned by the internal structured cloning algorithm.</dd>
</dl>
<dl>
</dl>
</div>
    */
  IDBRequest add(Object value);


  /**
    * <p>Returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and, in a separate thread, creates a <a class="external" title="http://www.whatwg.org/specs/web-apps/current-work/multipage/urls.html#structured-clone" rel="external" href="http://www.whatwg.org/specs/web-apps/current-work/multipage/urls.html#structured-clone" target="_blank">structured clone</a> of the <code>value</code>, and stores the cloned value in the object store. If the record is successfully stored, then a success event is fired on the returned request object, using the <a title="en/IndexedDB/IDBTransactionEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent">IDBTransactionEvent</a> interface, with the <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to the key for the stored record, and <code><a title="en/IndexedDB/IDBTransactionEvent#attr transaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent#attr_transaction">transaction</a></code> set to the transaction in which this object store is opened. If a record already exists in the object store with the <code>key</code> parameter as its key, then an error event is fired on the returned request object, with <a title="en/IndexedDB/IDBErrorEvent#attr code" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_code">code</a> set to <code><a title="en/IndexedDB/DatabaseException#CONSTRAINT ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#CONSTRAINT_ERR">CONSTRAINT_ERR</a></code>.</p>

<div id="section_5"><span id="Parameters"></span><h5 class="editable">Parameters</h5>
<dl> <dt>value</dt> <dd>The value to be stored.</dd> <dt>key</dt> <dd>The key to use to identify the record. If unspecified, it results to null.</dd>
</dl>
</div><div id="section_6"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_7"><span id="Exceptions"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#DATA ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#DATA_ERR">DATA_ERR</a></code></dt> <dd>If the object store uses in-line keys or has a key generator, and a key parameter was provided.<br> If the object store uses out-of-line keys and has no key generator, and no key parameter was provided.<br> If the object store uses in-line keys but no key generator, and the object store's key path does not yield a valid key.<br> If the key parameter was provided but does not contain a valid key.<br> If there are indexed on this object store, and using their key path on the value parameter yields a value that is not a valid key.</dd> <dt><code><a title="en/IndexedDB/IDBDatabaseException#READ ONLY ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#READ_ONLY_ERR">READ_ONLY_ERR</a></code></dt> <dd>If the mode of the associated transaction is <code><a title="en/IndexedDB/IDBTransaction#READ ONLY" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#READ_ONLY">READ_ONLY</a></code>.</dd> <dt><code><a title="en/IndexedDB/IDBDatabaseException#TRANSACTION INACTIVE ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#TRANSACTION_INACTIVE_ERR">TRANSACTION_INACTIVE_ERR</a></code></dt> <dd>If the associated transaction is not active.</dd>
</dl>
<p>This method can raise a <a title="en/DOM/DOMException" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMException">DOMException</a> with the following code:</p>
<dl> <dt><code>DATA_CLONE_ERR</code></dt> <dd>If the data being stored could not be cloned by the internal structured cloning algorithm.</dd>
</dl>
<dl>
</dl>
</div>
    */
  IDBRequest add(Object value, Object key);


  /**
    * <p>If the mode of the transaction that this object store belongs to is <code><a title="en/IndexedDB/IDBTransaction#READ ONLY" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#READ_ONLY">READ_ONLY</a></code>, this method raises an&nbsp;<a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with its code set to <code><a title="en/IndexedDB/IDBDatabaseException#READ ONLY ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#READ_ONLY_ERR">READ_ONLY_ERR</a></code>. Otherwise, this method creates and immediately returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and clears this object store in a separate thread. Clearing an object store consists of removing all records from the object store and removing all records in indexes that reference the object store.</p>

<div id="section_9"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<dl> <dt><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_10"><span id="Exceptions_2"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <dt><a title="en/IndexedDB/IDBDatabaseException#READ ONLY ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#READ_ONLY_ERR"><code>READ_ONLY_ERR</code></a></dt> <dd>If the mode of the transaction that this object store belongs to is READ_ONLY.</dd> <dt><a title="en/IndexedDB/IDBDatabaseException#TRANSACTION INACTIVE ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#TRANSACTION_INACTIVE_ERR"><code>TRANSACTION_INACTIVE_ERR</code></a></dt> <dd>If the transaction that this object store belongs to is not active.</dd>
</dl></div>
    */
  IDBRequest clear();


  /**
    * <p>Immediately returns an <a title="IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object and asynchronously count the amount of objects in the object store that match the parameter, a key or a key range. If the parameter is not valid returns an exception.</p>

<div id="section_12"><span id="Parameters_2"></span><h5 class="editable">Parameters</h5>
<dl> <dt>key</dt> <dd>The key or key range that identifies the records to be counted.</dd>
</dl>
</div><div id="section_13"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_14"><span id="Exceptions_3"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <dt><code><a href="IDBDatabaseException#DATA_ERR" rel="internal" title="en/IndexedDB/DatabaseException#DATA ERR">DATA_ERR</a></code></dt> <dd>If the object store uses in-line keys or has a key generator, and a key parameter was provided.<br> If the object store uses out-of-line keys and has no key generator, and no key parameter was provided.<br> If the object store uses in-line keys but no key generator, and the object store's key path does not yield a valid key.<br> If the key parameter was provided but does not contain a valid key.<br> If there are indexed on this object store, and using their key path on the value parameter yields a value that is not a valid key.</dd> <dt><code><a href="IDBDatabaseException#NOT_ALLOWED_ERR" rel="internal" title="en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>The request was made on a source object that has been deleted or removed.</dd>
</dl></div>
    */
  IDBRequest count();


  /**
    * <p>Immediately returns an <a title="IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object and asynchronously count the amount of objects in the object store that match the parameter, a key or a key range. If the parameter is not valid returns an exception.</p>

<div id="section_12"><span id="Parameters_2"></span><h5 class="editable">Parameters</h5>
<dl> <dt>key</dt> <dd>The key or key range that identifies the records to be counted.</dd>
</dl>
</div><div id="section_13"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_14"><span id="Exceptions_3"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <dt><code><a href="IDBDatabaseException#DATA_ERR" rel="internal" title="en/IndexedDB/DatabaseException#DATA ERR">DATA_ERR</a></code></dt> <dd>If the object store uses in-line keys or has a key generator, and a key parameter was provided.<br> If the object store uses out-of-line keys and has no key generator, and no key parameter was provided.<br> If the object store uses in-line keys but no key generator, and the object store's key path does not yield a valid key.<br> If the key parameter was provided but does not contain a valid key.<br> If there are indexed on this object store, and using their key path on the value parameter yields a value that is not a valid key.</dd> <dt><code><a href="IDBDatabaseException#NOT_ALLOWED_ERR" rel="internal" title="en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>The request was made on a source object that has been deleted or removed.</dd>
</dl></div>
    */
  IDBRequest count(IDBKeyRange range);


  /**
    * <p>Immediately returns an <a title="IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object and asynchronously count the amount of objects in the object store that match the parameter, a key or a key range. If the parameter is not valid returns an exception.</p>

<div id="section_12"><span id="Parameters_2"></span><h5 class="editable">Parameters</h5>
<dl> <dt>key</dt> <dd>The key or key range that identifies the records to be counted.</dd>
</dl>
</div><div id="section_13"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_14"><span id="Exceptions_3"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <dt><code><a href="IDBDatabaseException#DATA_ERR" rel="internal" title="en/IndexedDB/DatabaseException#DATA ERR">DATA_ERR</a></code></dt> <dd>If the object store uses in-line keys or has a key generator, and a key parameter was provided.<br> If the object store uses out-of-line keys and has no key generator, and no key parameter was provided.<br> If the object store uses in-line keys but no key generator, and the object store's key path does not yield a valid key.<br> If the key parameter was provided but does not contain a valid key.<br> If there are indexed on this object store, and using their key path on the value parameter yields a value that is not a valid key.</dd> <dt><code><a href="IDBDatabaseException#NOT_ALLOWED_ERR" rel="internal" title="en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>The request was made on a source object that has been deleted or removed.</dd>
</dl></div>
    */
  IDBRequest count(Object key);


  /**
    * <p>Creates and returns a new index in the connected database. Note that this method must be called only from a <a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE"><code>VERSION_CHANGE</code></a> transaction callback.</p>
<pre>IDBIndex createIndex (
&nbsp; in DOMString name, 
&nbsp; in DOMString keyPath, 
&nbsp; in Object optionalParameters
) raises (IDBDatabaseException);

</pre>
<div id="section_16"><span id="Parameters_3"></span><h5 class="editable">Parameters</h5>
<dl> <dt>name</dt> <dd>The name of the index to create.</dd> <dt>keyPath</dt> <dd>The key path for the index to use.</dd> <dt>optionalParameters</dt> <dd> <div class="warning"><strong>Warning:</strong> The latest draft of the specification changed this to <code>IDBIndexParameters</code>, which is not yet recognized by any browser</div> <p>Options object whose attributes are optional parameters to the method. It includes the following properties:</p> <table class="standard-table"> <thead> <tr> <th scope="col" width="131">Attribute</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code>unique</code></td> <td>If true, the index will not allow duplicate values for a single key.</td> </tr> <tr> <td><code>multientry</code></td> <td>If true, the index will add an entry in the index for each array element when the <em>keypath</em> resolves to an Array. If false, it will add one single entry containing the Array.</td> </tr> </tbody> </table> <p>Unknown parameters are ignored.</p> </dd> <dd></dd>
</dl>
</div><div id="section_17"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><a title="en/IndexedDB/IDBIndex" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBIndex">IDBIndex</a></dt> <dd>The newly created index.</dd>
</dl>
</div><div id="section_18"><span id="Exceptions_4"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#CONSTRAINT ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#CONSTRAINT_ERR">CONSTRAINT_ERR</a></code></dt> <dd>If an index with the same name (based on case-sensitive comparison) already exists in the connected database.</dd> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If this method was not called from a <a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE"><code>VERSION_CHANGE</code></a> transaction callback.</dd>
</dl></div>
    */
  IDBIndex createIndex(String name, String keyPath);


  /**
    * <p>Creates and returns a new index in the connected database. Note that this method must be called only from a <a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE"><code>VERSION_CHANGE</code></a> transaction callback.</p>
<pre>IDBIndex createIndex (
&nbsp; in DOMString name, 
&nbsp; in DOMString keyPath, 
&nbsp; in Object optionalParameters
) raises (IDBDatabaseException);

</pre>
<div id="section_16"><span id="Parameters_3"></span><h5 class="editable">Parameters</h5>
<dl> <dt>name</dt> <dd>The name of the index to create.</dd> <dt>keyPath</dt> <dd>The key path for the index to use.</dd> <dt>optionalParameters</dt> <dd> <div class="warning"><strong>Warning:</strong> The latest draft of the specification changed this to <code>IDBIndexParameters</code>, which is not yet recognized by any browser</div> <p>Options object whose attributes are optional parameters to the method. It includes the following properties:</p> <table class="standard-table"> <thead> <tr> <th scope="col" width="131">Attribute</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code>unique</code></td> <td>If true, the index will not allow duplicate values for a single key.</td> </tr> <tr> <td><code>multientry</code></td> <td>If true, the index will add an entry in the index for each array element when the <em>keypath</em> resolves to an Array. If false, it will add one single entry containing the Array.</td> </tr> </tbody> </table> <p>Unknown parameters are ignored.</p> </dd> <dd></dd>
</dl>
</div><div id="section_17"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><a title="en/IndexedDB/IDBIndex" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBIndex">IDBIndex</a></dt> <dd>The newly created index.</dd>
</dl>
</div><div id="section_18"><span id="Exceptions_4"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#CONSTRAINT ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#CONSTRAINT_ERR">CONSTRAINT_ERR</a></code></dt> <dd>If an index with the same name (based on case-sensitive comparison) already exists in the connected database.</dd> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If this method was not called from a <a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE"><code>VERSION_CHANGE</code></a> transaction callback.</dd>
</dl></div>
    */
  IDBIndex createIndex(String name, String keyPath, Mappable options);


  /**
    * <p>Immediately returns an <code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code> object, and removes the record specified by the given key from this object store, and any indexes that reference it, in a separate thread. If no record exists in this object store corresponding to the key, an error event is fired on the returned request object, with its <code><a title="en/IndexedDB/IDBErrorEvent#attr code" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_code">code</a></code> set to <code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code> and an appropriate <code><a title="en/IndexedDB/IDBErrorEvent#attr message" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_message">message</a></code>. If the record is successfully removed, then a success event is fired on the returned request object, using the <code><a title="en/IndexedDB/IDBTransactionEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent">IDBTransactionEvent</a></code> interface, with the <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to <code>undefined</code>, and <a title="en/IndexedDB/IDBTransactionEvent#attr transaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent#attr_transaction">transaction</a> set to the transaction in which this object store is opened.</p>
<pre>IDBRequest delete (
  in any key
) raises (IDBDatabaseException); 
</pre>
<div id="section_20"><span id="Parameters_4"></span><h5 class="editable">Parameters</h5>
<dl> <dt>key</dt> <dd>The key to use to identify the record.</dd>
</dl>
</div><div id="section_21"><span id="Returns_5"></span><h5 class="editable">Returns</h5>
<dl> <dt><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_22"><span id="Exceptions_5"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p></div>
    */
  IDBRequest _delete(IDBKeyRange keyRange);


  /**
    * <p>Immediately returns an <code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code> object, and removes the record specified by the given key from this object store, and any indexes that reference it, in a separate thread. If no record exists in this object store corresponding to the key, an error event is fired on the returned request object, with its <code><a title="en/IndexedDB/IDBErrorEvent#attr code" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_code">code</a></code> set to <code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code> and an appropriate <code><a title="en/IndexedDB/IDBErrorEvent#attr message" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_message">message</a></code>. If the record is successfully removed, then a success event is fired on the returned request object, using the <code><a title="en/IndexedDB/IDBTransactionEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent">IDBTransactionEvent</a></code> interface, with the <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to <code>undefined</code>, and <a title="en/IndexedDB/IDBTransactionEvent#attr transaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent#attr_transaction">transaction</a> set to the transaction in which this object store is opened.</p>
<pre>IDBRequest delete (
  in any key
) raises (IDBDatabaseException); 
</pre>
<div id="section_20"><span id="Parameters_4"></span><h5 class="editable">Parameters</h5>
<dl> <dt>key</dt> <dd>The key to use to identify the record.</dd>
</dl>
</div><div id="section_21"><span id="Returns_5"></span><h5 class="editable">Returns</h5>
<dl> <dt><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_22"><span id="Exceptions_5"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p></div>
    */
  IDBRequest _delete(Object key);


  /**
    * <p>Destroys the index with the specified name in the connected database. Note that this method must be called only from a <code><a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE">VERSION_CHANGE</a></code> transaction callback.</p>
<pre>void removeIndex(
  in DOMString indexName
) raises (IDBDatabaseException); 
</pre>
<div id="section_24"><span id="Parameters_5"></span><h5 class="editable">Parameters</h5>
<p>&nbsp;</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If the object store is not in the <a title="en/IndexedDB#gloss scope" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_scope">scope</a> of any existing <a title="en/IndexedDB#gloss transaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_transaction">transaction</a>, or if the associated transaction's mode is <a title="en/IndexedDB/IDBTransaction#const read only" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#const_read_only"><code>READ_ONLY</code></a>&nbsp;or <a title="en/IndexedDB/IDBTransaction#const snapshot read" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#const_snapshot_read"><code>SNAPSHOT_READ</code></a>.</dd> <dt><code><a title="en/IndexedDB/IDBDatabaseException#TRANSACTION INACTIVE ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#TRANSACTION_INACTIVE_ERR">TRANSACTION_INACTIVE_ERR</a></code></dt> <dd>If the associated transaction is not active.</dd> <dt>indexName</dt> <dd>The name of the existing index to remove.</dd>
</dl>
</div><div id="section_25"><span id="Exceptions_6"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If this method was not called from a <a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE">VERSION_CHANGE</a> transaction callback.</dd> <dt><code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code></dt> <dd>If no index exists with the specified name (based on case-sensitive comparison) in the connected database.</dd>
</dl>
</div>
    */
  void deleteIndex(String name);

  IDBRequest getObject(IDBKeyRange key);

  IDBRequest getObject(Object key);


  /**
    * <p>Opens the named index in this object store.</p>

<div id="section_31"><span id="Parameters_7"></span><h5 class="editable">Parameters</h5>
<dl> <dt>name</dt> <dd>The name of the index to open.</dd>
</dl>
</div><div id="section_32"><span id="Returns_7"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBIndex" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBIndex">IDBIndex</a></code></dt> <dd>An object for accessing the index.</dd>
</dl>
</div><div id="section_33"><span id="Exceptions_8"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following code:</p>
<dl> <dt><code><a title="en/IndexedDB/IDBDatabaseException#NOT FOUND ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_FOUND_ERR">NOT_FOUND_ERR</a></code></dt> <dd>If no index exists with the specified name (based on case-sensitive comparison) in the connected database.</dd>
</dl>
</div>
    */
  IDBIndex index(String name);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and creates a <a title="en/IndexedDB#gloss cursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_cursor">cursor</a> over the records in this object store, in a separate thread. If there is even a single record that matches the <a title="en/IndexedDB#gloss key range" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_range">key range</a>, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to the <a title="en/IndexedDB/IDBCursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBCursor">IDBCursor</a> object for the new cursor. If no records match the key range, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to null.</p>
<pre>IDBRequest openCursor (
&nbsp; in optional IDBKeyRange range, 
&nbsp; in optional unsigned short direction
) raises (IDBDatabaseException);
</pre>
<div id="section_35"><span id="Parameters_8"></span><h5 class="editable">Parameters</h5>
<dl> <dt>range</dt> <dd>The key range to use as the cursor's range. If this parameter is unspecified or null, then the range includes all the records in the object store.</dd> <dt>direction</dt> <dd>The cursor's <a title="en/IndexedDB#gloss direction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_direction">direction</a>.</dd>
</dl>
</div><div id="section_36"><span id="Returns_8"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_37"><span id="Exceptions_9"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an IDBDatabaseException with the following code:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If this object store is not in the scope of any existing transaction on the connected database.</dd>
</dl>
</div>
    */
  IDBRequest openCursor(IDBKeyRange range);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and creates a <a title="en/IndexedDB#gloss cursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_cursor">cursor</a> over the records in this object store, in a separate thread. If there is even a single record that matches the <a title="en/IndexedDB#gloss key range" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_range">key range</a>, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to the <a title="en/IndexedDB/IDBCursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBCursor">IDBCursor</a> object for the new cursor. If no records match the key range, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to null.</p>
<pre>IDBRequest openCursor (
&nbsp; in optional IDBKeyRange range, 
&nbsp; in optional unsigned short direction
) raises (IDBDatabaseException);
</pre>
<div id="section_35"><span id="Parameters_8"></span><h5 class="editable">Parameters</h5>
<dl> <dt>range</dt> <dd>The key range to use as the cursor's range. If this parameter is unspecified or null, then the range includes all the records in the object store.</dd> <dt>direction</dt> <dd>The cursor's <a title="en/IndexedDB#gloss direction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_direction">direction</a>.</dd>
</dl>
</div><div id="section_36"><span id="Returns_8"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_37"><span id="Exceptions_9"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an IDBDatabaseException with the following code:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If this object store is not in the scope of any existing transaction on the connected database.</dd>
</dl>
</div>
    */
  IDBRequest openCursor(IDBKeyRange range, String direction);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and creates a <a title="en/IndexedDB#gloss cursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_cursor">cursor</a> over the records in this object store, in a separate thread. If there is even a single record that matches the <a title="en/IndexedDB#gloss key range" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_range">key range</a>, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to the <a title="en/IndexedDB/IDBCursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBCursor">IDBCursor</a> object for the new cursor. If no records match the key range, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to null.</p>
<pre>IDBRequest openCursor (
&nbsp; in optional IDBKeyRange range, 
&nbsp; in optional unsigned short direction
) raises (IDBDatabaseException);
</pre>
<div id="section_35"><span id="Parameters_8"></span><h5 class="editable">Parameters</h5>
<dl> <dt>range</dt> <dd>The key range to use as the cursor's range. If this parameter is unspecified or null, then the range includes all the records in the object store.</dd> <dt>direction</dt> <dd>The cursor's <a title="en/IndexedDB#gloss direction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_direction">direction</a>.</dd>
</dl>
</div><div id="section_36"><span id="Returns_8"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_37"><span id="Exceptions_9"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an IDBDatabaseException with the following code:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If this object store is not in the scope of any existing transaction on the connected database.</dd>
</dl>
</div>
    */
  IDBRequest openCursor(Object key);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and creates a <a title="en/IndexedDB#gloss cursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_cursor">cursor</a> over the records in this object store, in a separate thread. If there is even a single record that matches the <a title="en/IndexedDB#gloss key range" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_range">key range</a>, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to the <a title="en/IndexedDB/IDBCursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBCursor">IDBCursor</a> object for the new cursor. If no records match the key range, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to null.</p>
<pre>IDBRequest openCursor (
&nbsp; in optional IDBKeyRange range, 
&nbsp; in optional unsigned short direction
) raises (IDBDatabaseException);
</pre>
<div id="section_35"><span id="Parameters_8"></span><h5 class="editable">Parameters</h5>
<dl> <dt>range</dt> <dd>The key range to use as the cursor's range. If this parameter is unspecified or null, then the range includes all the records in the object store.</dd> <dt>direction</dt> <dd>The cursor's <a title="en/IndexedDB#gloss direction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_direction">direction</a>.</dd>
</dl>
</div><div id="section_36"><span id="Returns_8"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_37"><span id="Exceptions_9"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an IDBDatabaseException with the following code:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If this object store is not in the scope of any existing transaction on the connected database.</dd>
</dl>
</div>
    */
  IDBRequest openCursor(Object key, String direction);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and creates a <a title="en/IndexedDB#gloss cursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_cursor">cursor</a> over the records in this object store, in a separate thread. If there is even a single record that matches the <a title="en/IndexedDB#gloss key range" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_range">key range</a>, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to the <a title="en/IndexedDB/IDBCursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBCursor">IDBCursor</a> object for the new cursor. If no records match the key range, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to null.</p>
<pre>IDBRequest openCursor (
&nbsp; in optional IDBKeyRange range, 
&nbsp; in optional unsigned short direction
) raises (IDBDatabaseException);
</pre>
<div id="section_35"><span id="Parameters_8"></span><h5 class="editable">Parameters</h5>
<dl> <dt>range</dt> <dd>The key range to use as the cursor's range. If this parameter is unspecified or null, then the range includes all the records in the object store.</dd> <dt>direction</dt> <dd>The cursor's <a title="en/IndexedDB#gloss direction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_direction">direction</a>.</dd>
</dl>
</div><div id="section_36"><span id="Returns_8"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_37"><span id="Exceptions_9"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an IDBDatabaseException with the following code:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If this object store is not in the scope of any existing transaction on the connected database.</dd>
</dl>
</div>
    */
  IDBRequest openCursor(IDBKeyRange range, int direction);


  /**
    * <p>Immediately returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and creates a <a title="en/IndexedDB#gloss cursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_cursor">cursor</a> over the records in this object store, in a separate thread. If there is even a single record that matches the <a title="en/IndexedDB#gloss key range" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_range">key range</a>, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to the <a title="en/IndexedDB/IDBCursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBCursor">IDBCursor</a> object for the new cursor. If no records match the key range, then a success event is fired on the returned object, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to null.</p>
<pre>IDBRequest openCursor (
&nbsp; in optional IDBKeyRange range, 
&nbsp; in optional unsigned short direction
) raises (IDBDatabaseException);
</pre>
<div id="section_35"><span id="Parameters_8"></span><h5 class="editable">Parameters</h5>
<dl> <dt>range</dt> <dd>The key range to use as the cursor's range. If this parameter is unspecified or null, then the range includes all the records in the object store.</dd> <dt>direction</dt> <dd>The cursor's <a title="en/IndexedDB#gloss direction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_direction">direction</a>.</dd>
</dl>
</div><div id="section_36"><span id="Returns_8"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_37"><span id="Exceptions_9"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an IDBDatabaseException with the following code:</p>
<dl> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If this object store is not in the scope of any existing transaction on the connected database.</dd>
</dl>
</div>
    */
  IDBRequest openCursor(Object key, int direction);


  /**
    * <p>Returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and, in a separate thread, creates a <a class="external" title="http://www.whatwg.org/specs/web-apps/current-work/multipage/urls.html#structured-clone" rel="external" href="http://www.whatwg.org/specs/web-apps/current-work/multipage/urls.html#structured-clone" target="_blank">structured clone</a> of the <code>value</code>, and stores the cloned value in the object store. If the record is successfully stored, then a success event is fired on the returned request object, using the <a title="en/IndexedDB/IDBTransactionEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent">IDBTransactionEvent</a> interface, with the <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to the key for the stored record, and <code><a title="en/IndexedDB/IDBTransactionEvent#attr transaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent#attr_transaction">transaction</a></code> set to the transaction in which this object store is opened.</p>

<div id="section_39"><span id="Parameters_9"></span><h5 class="editable">Parameters</h5>
<dl> <dt>value</dt> <dd>The value to be stored.</dd> <dt>key</dt> <dd>The key to use to identify the record. If unspecified, it results to null.</dd>
</dl>
</div><div id="section_40"><span id="Returns_9"></span><h5 class="editable">Returns</h5>
<dl> <dt>IDBRequest</dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_41"><span id="Exceptions_10"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <li> <ul> <li>If this object store uses <a title="en/IndexedDB#gloss out-of-line key" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_out-of-line_key">out-of-line keys</a> and does not use a <a title="en/IndexedDB#gloss key generator" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_generator">key generator</a>, but the <code>key</code> parameter was not passed</li> <li>If the object store uses <a title="en/IndexedDB#gloss in-line key" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_in-line_key">in-line keys</a>, but the <code>value</code> object does not have a property identified by the object store's key path.</li> </ul> </li> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If the object store is not in the <a title="en/IndexedDB#gloss scope" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_scope">scope</a> of any existing <a title="en/IndexedDB#gloss transaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_transaction">transaction</a>, or if the associated transaction's mode is <a title="en/IndexedDB/IDBTransaction#const read only" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#const_read_only"><code>READ_ONLY</code></a>&nbsp;or <a title="en/IndexedDB/IDBTransaction#const snapshot read" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#const_snapshot_read"><code>SNAPSHOT_READ</code></a>.</dd> <dt><code><a title="en/IndexedDB/IDBDatabaseException#SERIAL ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#SERIAL_ERR">SERIAL_ERR</a></code></dt> <dd>If the data being stored could not be serialized by the internal structured cloning algorithm.</dd>
</dl>
<p>This method can raise a <a title="en/DOM/DOMException" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMException">DOMException</a> with the following code:</p>
<dl> <dt><code>DATA_CLONE_ERR</code></dt> <dd>If the data being stored could not be cloned by the internal structured cloning algorithm.</dd>
</dl>
</div>
    */
  IDBRequest put(Object value);


  /**
    * <p>Returns an <a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a> object, and, in a separate thread, creates a <a class="external" title="http://www.whatwg.org/specs/web-apps/current-work/multipage/urls.html#structured-clone" rel="external" href="http://www.whatwg.org/specs/web-apps/current-work/multipage/urls.html#structured-clone" target="_blank">structured clone</a> of the <code>value</code>, and stores the cloned value in the object store. If the record is successfully stored, then a success event is fired on the returned request object, using the <a title="en/IndexedDB/IDBTransactionEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent">IDBTransactionEvent</a> interface, with the <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to the key for the stored record, and <code><a title="en/IndexedDB/IDBTransactionEvent#attr transaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransactionEvent#attr_transaction">transaction</a></code> set to the transaction in which this object store is opened.</p>

<div id="section_39"><span id="Parameters_9"></span><h5 class="editable">Parameters</h5>
<dl> <dt>value</dt> <dd>The value to be stored.</dd> <dt>key</dt> <dd>The key to use to identify the record. If unspecified, it results to null.</dd>
</dl>
</div><div id="section_40"><span id="Returns_9"></span><h5 class="editable">Returns</h5>
<dl> <dt>IDBRequest</dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_41"><span id="Exceptions_10"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<dl> <li> <ul> <li>If this object store uses <a title="en/IndexedDB#gloss out-of-line key" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_out-of-line_key">out-of-line keys</a> and does not use a <a title="en/IndexedDB#gloss key generator" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_key_generator">key generator</a>, but the <code>key</code> parameter was not passed</li> <li>If the object store uses <a title="en/IndexedDB#gloss in-line key" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_in-line_key">in-line keys</a>, but the <code>value</code> object does not have a property identified by the object store's key path.</li> </ul> </li> <dt><code><a title="en/IndexedDB/DatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></dt> <dd>If the object store is not in the <a title="en/IndexedDB#gloss scope" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_scope">scope</a> of any existing <a title="en/IndexedDB#gloss transaction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_transaction">transaction</a>, or if the associated transaction's mode is <a title="en/IndexedDB/IDBTransaction#const read only" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#const_read_only"><code>READ_ONLY</code></a>&nbsp;or <a title="en/IndexedDB/IDBTransaction#const snapshot read" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#const_snapshot_read"><code>SNAPSHOT_READ</code></a>.</dd> <dt><code><a title="en/IndexedDB/IDBDatabaseException#SERIAL ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#SERIAL_ERR">SERIAL_ERR</a></code></dt> <dd>If the data being stored could not be serialized by the internal structured cloning algorithm.</dd>
</dl>
<p>This method can raise a <a title="en/DOM/DOMException" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMException">DOMException</a> with the following code:</p>
<dl> <dt><code>DATA_CLONE_ERR</code></dt> <dd>If the data being stored could not be cloned by the internal structured cloning algorithm.</dd>
</dl>
</div>
    */
  IDBRequest put(Object value, Object key);
}
