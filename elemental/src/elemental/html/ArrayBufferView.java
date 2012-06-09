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
  * <p>The <code>ArrayBufferView</code> type describes a particular view on the contents of an <a title="en/JavaScript typed arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer"><code>ArrayBuffer</code></a>'s data.</p>
<p>Of note is that you may create multiple views into the same buffer, each looking at the buffer's contents starting at a particular offset. This makes it possible to set up views of different data types to read the contents of a buffer based on the types of data at specific offsets into the buffer.</p>
<div class="note"><strong>Note:</strong> Typically, you'll instantiate one of the <a title="en/JavaScript typed arrays/ArrayBufferView#Typed array subclasses" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBufferView#Typed_array_subclasses">subclasses</a> of this object instead of this base class. Those provide access to the data formatted using specific data types.</div>
  */
public interface ArrayBufferView {


  /**
    * The buffer this view references. <strong>Read only.</strong>
    */
  ArrayBuffer getBuffer();


  /**
    * The length, in bytes, of the view. <strong>Read only.</strong>
    */
  int getByteLength();


  /**
    * The offset, in bytes, to the first byte of the view within the <a title="en/JavaScript typed arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer"><code>ArrayBuffer</code></a>.
    */
  int getByteOffset();
}
