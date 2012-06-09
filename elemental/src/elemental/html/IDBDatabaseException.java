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
  * In the <a title="en/IndexedDB" rel="internal" href="https://developer.mozilla.org/en/IndexedDB">IndexedDB API</a>, an <code>IDBDatabaseException</code> object represents exception conditions that can be encountered while performing database operations.
  */
public interface IDBDatabaseException {

  /**
    * A request was aborted, for example, through a call to<a title="en/IndexedDB/IDBTransaction#abort" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#abort"> <code>IDBTransaction.abort</code></a>.
    */

    static final int ABORT_ERR = 20;

  /**
    * A mutation operation in the transaction failed because a constraint was not satisfied. For example, an object, such as an object store or index, already exists and a request attempted to create a new one.
    */

    static final int CONSTRAINT_ERR = 4;

  /**
    * Data provided to an operation does not meet requirements.
    */

    static final int DATA_ERR = 5;

  /**
    * An operation was not allowed on an object. Unless the cause of the error is corrected, retrying the same operation would result in failure.
    */

    static final int NON_TRANSIENT_ERR = 2;

  /**
    * <p>An operation was called on an object where it is not allowed or at a time when it is not allowed. It also occurs if a request is made on a source object that has been deleted or removed.</p> <p>More specific variants of this error includes: <code> TRANSACTION_INACTIVE_ERR</code> and <code>READ_ONLY_ERR</code>.</p>
    */

    static final int NOT_ALLOWED_ERR = 6;

  /**
    * The operation failed, because the requested database object could not be found; for example, an object store did not exist but was being opened.
    */

    static final int NOT_FOUND_ERR = 8;

    static final int NO_ERR = 0;

  /**
    * Either there's not enough remaining storage space or the storage quota was reached and the user declined to give more space to the database.
    */

    static final int QUOTA_ERR = 22;

  /**
    * A mutation operation was attempted in a <code>READ_ONLY</code>&nbsp;transaction.
    */

    static final int READ_ONLY_ERR = 9;

  /**
    * A lock for the transaction could not be obtained in a reasonable time.
    */

    static final int TIMEOUT_ERR = 23;

  /**
    * A request was made against a transaction that is either not currently active or is already finished.
    */

    static final int TRANSACTION_INACTIVE_ERR = 7;

  /**
    * The operation failed for reasons unrelated to the database itself, and it is not covered by any other error code; for example, a failure due to disk IO errors.
    */

    static final int UNKNOWN_ERR = 1;

  /**
    * A request to open a database with a version lower than the one it already has. This can only happen with <a title="en/IndexedDB/IDBOpenDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBOpenDBRequest"><code>IDBOpenDBRequest</code></a>.
    */

    static final int VER_ERR = 12;


  /**
    * The most appropriate error code for the condition.
    */
  int getCode();


  /**
    * Error message describing the exception raised.
    */
  String getMessage();

  String getName();
}
