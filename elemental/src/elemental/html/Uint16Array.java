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
  * <p>The <code>Uint16Array</code> type represents an array of unsigned 16-bit integers..</p>
<p>Once established, you can reference elements in the array using the object's methods, or using standard array index syntax (that is, using bracket notation).</p>
  */
public interface Uint16Array extends ArrayBufferView, IndexableInt {

  /**
    * The size, in bytes, of each array element.
    */

    static final int BYTES_PER_ELEMENT = 2;


  /**
    * The number of entries in the array. <strong>Read only.</strong>
    */
  int getLength();

  void setElements(Object array);

  void setElements(Object array, int offset);


  /**
    * <p>Returns a new <code>Uint16Array</code> view on the <a title="en/JavaScript typed arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer"><code>ArrayBuffer</code></a> store for this <code>Uint16Array</code> object.</p>

<div id="section_15"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>begin</code></dt> <dd>The offset to the first element in the array to be referenced by the new <code>Uint16Array</code> object.</dd> <dt><code>end</code> 
<span title="">Optional</span>
</dt> <dd>The offset to the last element in the array to be referenced by the new <code>Uint16Array</code> object; if not specified, all elements from the one specified by <code>begin</code> to the end of the array are included in the new view.</dd>
</dl>
</div><div id="section_16"><span id="Notes_2"></span><h6 class="editable">Notes</h6>
<p>The range specified by <code>begin</code> and <code>end</code> is clamped to the valid index range for the current array; if the computed length of the new array would be negative, it's clamped to zero. If either <code>begin</code> or <code>end</code> is negative, it refers to an index from the end of the array instead of from the beginning.</p>
<div class="note"><strong>Note:</strong> Keep in mind that this is creating a new view on the existing buffer; changes to the new object's contents will impact the original object and vice versa.</div>
</div>
    */
  Uint16Array subarray(int start);


  /**
    * <p>Returns a new <code>Uint16Array</code> view on the <a title="en/JavaScript typed arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer"><code>ArrayBuffer</code></a> store for this <code>Uint16Array</code> object.</p>

<div id="section_15"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>begin</code></dt> <dd>The offset to the first element in the array to be referenced by the new <code>Uint16Array</code> object.</dd> <dt><code>end</code> 
<span title="">Optional</span>
</dt> <dd>The offset to the last element in the array to be referenced by the new <code>Uint16Array</code> object; if not specified, all elements from the one specified by <code>begin</code> to the end of the array are included in the new view.</dd>
</dl>
</div><div id="section_16"><span id="Notes_2"></span><h6 class="editable">Notes</h6>
<p>The range specified by <code>begin</code> and <code>end</code> is clamped to the valid index range for the current array; if the computed length of the new array would be negative, it's clamped to zero. If either <code>begin</code> or <code>end</code> is negative, it refers to an index from the end of the array instead of from the beginning.</p>
<div class="note"><strong>Note:</strong> Keep in mind that this is creating a new view on the existing buffer; changes to the new object's contents will impact the original object and vice versa.</div>
</div>
    */
  Uint16Array subarray(int start, int end);
}
