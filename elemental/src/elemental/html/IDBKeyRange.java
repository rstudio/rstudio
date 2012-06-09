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
  * The <code>IDBKeyRange</code> interface of the <a title="en/IndexedDB" rel="internal" href="https://developer.mozilla.org/en/IndexedDB">IndexedDB API</a> represents a continuous interval over some data type that is used for keys. Records can be retrieved from object stores and indexes using keys or a range of keys. You can limit the range using lower and upper bounds. For example, you can iterate over all values of a key between x and y.
  */
public interface IDBKeyRange {

  Object getLower();

  boolean isLowerOpen();

  Object getUpper();


  /**
    * Returns false if the upper-bound value is included in the key range.
    */
  boolean isUpperOpen();
}
