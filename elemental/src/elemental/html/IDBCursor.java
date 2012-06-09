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
  * The <code>IDBCursor</code> interface of the <a title="en/IndexedDB" rel="internal" href="https://developer.mozilla.org/en/IndexedDB">IndexedDB API</a> represents a <a title="en/IndexedDB#gloss_cursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/Basic_Concepts_Behind_IndexedDB#gloss_cursor">cursor</a> for traversing or iterating over multiple records in a database.
  */
public interface IDBCursor {

  /**
    * The cursor shows all records, including duplicates. It starts at the lower bound of the key range and moves upwards (monotonically increasing in the order of keys).
    */

    static final int NEXT = 0;

  /**
    * The cursor shows all records, excluding duplicates. If multiple records exist with the same key, only the first one iterated is retrieved. It starts at the lower bound of the key range and moves upwards.
    */

    static final int NEXT_NO_DUPLICATE = 1;

  /**
    * The cursor shows all records, including duplicates. It starts at the upper bound of the key range and moves downwards (monotonically decreasing in the order of keys).
    */

    static final int PREV = 2;

  /**
    * The cursor shows all records, excluding duplicates. If multiple records exist with the same key, only the first one iterated is retrieved. It starts at the upper bound of the key range and moves downwards.
    */

    static final int PREV_NO_DUPLICATE = 3;


  /**
    * On getting, returns the <a title="en/IndexedDB/Basic_Concepts_Behind_IndexedDB#gloss direction" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/Basic_Concepts_Behind_IndexedDB#gloss_direction">direction</a> of traversal of the cursor. See Constants for possible values.
    */
  String getDirection();


  /**
    * Returns the key for the record at the cursor's position. If the cursor is outside its range, this is <code>undefined</code>.
    */
  Object getKey();


  /**
    * Returns the cursor's current effective key. If the cursor is currently being iterated or has iterated outside its range, this is <code>undefined</code>.
    */
  Object getPrimaryKey();


  /**
    * On getting, returns the <code>IDBObjectStore</code> or <code>IDBIndex</code> that the cursor is iterating. This function never returns null or throws an exception, even if the cursor is currently being iterated, has iterated past its end, or its transaction is not active.
    */
  Object getSource();


  /**
    * <p>Sets the number times a cursor should move its position forward.</p>
<pre>IDBRequest advance (
  in long <em>count</em>
) raises (IDBDatabaseException);</pre>
<div id="section_13"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>count</dt> <dd>The number of advances forward the cursor should make.</dd>
</dl>
</div><div id="section_14"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div><div id="section_15"><span id="Exceptions_2"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><a title="en/IndexedDB/IDBDatabaseException#NON_TRANSIET_ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NON_TRANSIET_ERR"><code>NON_TRANSIENT_ERR</code></a></td> <td> <p>The value passed into the <code>count</code> parameter was zero or a negative number.</p> </td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The cursor was created using <a title="en/IndexedDB/IDBIndex#openKeyCursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBIndex#openKeyCursor">openKeyCursor()</a>, or if it is currently being iterated (you cannot call this method again until the new cursor data has been loaded), or if it has iterated past the end of its range.</td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#TRANSACTION INACTIVE ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#TRANSACTION_INACTIVE_ERR">TRANSACTION_INACTIVE_ERR</a></code></td> <td>The transaction that this cursor belongs to is inactive.</td> </tr> </tbody>
</table>
</div>
    */
  void advance(int count);

  void continueFunction();

  void continueFunction(Object key);


  /**
    * <p>Returns an <code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code> object, and, in a separate thread, deletes the record at the cursor's position, without changing the cursor's position. Once the record is deleted, the cursor's <code>value</code> is set to <code>null</code>.</p>
<pre>IDBRequest delete (
) raises (IDBDatabaseException);</pre>
<div id="section_21"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></dt> <dd>A request object on which subsequent events related to this operation are fired. The <code>result</code> attribute is set to <code>undefined</code>.</dd>
</dl>
</div><div id="section_22"><span id="Exceptions_4"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following code:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The cursor was created using <a title="en/IndexedDB/IDBIndex#openKeyCursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBIndex#openKeyCursor">openKeyCursor()</a>, or if it is currently being iterated (you cannot call this method again until the new cursor data has been loaded), or if it has iterated past the end of its range.</td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#READ ONLY ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#READ_ONLY_ERR">READ_ONLY_ERR</a></code></td> <td>The cursor is in a transaction whose mode is <code><a title="en/IndexedDB/IDBTransaction#READ ONLY" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#READ_ONLY">READ_ONLY</a></code>.</td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#TRANSACTION INACTIVE ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#TRANSACTION_INACTIVE_ERR">TRANSACTION_INACTIVE_ERR</a></code></td> <td>The transaction that this cursor belongs to is inactive.</td> </tr> </tbody>
</table>
</div>
    */
  IDBRequest _delete();


  /**
    * <p>Returns an IDBRequest object, and, in a separate thread, updates the value at the current position of the cursor in the object store. If the cursor points to a record that has just been deleted, a new record is created.</p>
<pre>IDBRequest update (
  in any <em>value</em>
) raises (IDBDatabaseException, DOMException);
</pre>
<div id="section_9"><span id="Parameter"></span><h5 class="editable">Parameter</h5>
<dl> <dt>value</dt> <dd>The value to be stored.</dd>
</dl>
</div><div id="section_10"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></dt> <dd>A request object on which subsequent events related to this operation are fired.</dd>
</dl>
</div><div id="section_11"><span id="Exceptions"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following codes:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Exception</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#DATA ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#DATA_ERR">DATA_ERR</a></code></td> <td> <p>The underlying object store uses <a title="en/IndexedDB/Basic_Concepts_Behind_IndexedDB#gloss in-line keys" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/Basic_Concepts_Behind_IndexedDB#gloss_in-line_keys">in-line keys</a>, and the key for the cursor's position does not match the <code>value</code> property at the object store's&nbsp;<a class="external" title="object store key path" rel="external" href="http://dvcs.w3.org/hg/IndexedDB/raw-file/tip/Overview.html#dfn-object-store-key-path" target="_blank">key path</a>.</p> </td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NOT ALLOWED ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NOT_ALLOWED_ERR">NOT_ALLOWED_ERR</a></code></td> <td>The cursor was created using <a title="en/IndexedDB/IDBIndex#openKeyCursor" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBIndex#openKeyCursor">openKeyCursor()</a>, or if it is currently being iterated (you cannot call this method again until the new cursor data has been loaded), or if it has iterated past the end of its range.</td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#READ ONLY ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#READ_ONLY_ERR">READ_ONLY_ERR</a></code></td> <td>The cursor is in a transaction whose mode is <code><a title="en/IndexedDB/IDBTransaction#READ ONLY" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#READ_ONLY">READ_ONLY</a></code>.</td> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#TRANSACTION INACTIVE ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#TRANSACTION_INACTIVE_ERR">TRANSACTION_INACTIVE_ERR</a></code></td> <td>The transaction that this cursor belongs to is inactive.</td> </tr> </tbody>
</table>
<p>It can also raise a <a title="En/DOM/DOMException" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMException">DOMException</a> with the following code:</p>
<table class="standard-table"> <thead> <tr> <th scope="col" width="131">Attribute</th> <th scope="col" width="698">Description</th> </tr> </thead> <tbody> <tr> <td><code>DATA_CLONE_ERR</code></td> <td>If the value could not be cloned.</td> </tr> </tbody>
</table>
</div>
    */
  IDBRequest update(Object value);
}
