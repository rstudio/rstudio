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
  * The <code>ArrayBuffer</code> is a data type that is used to represent a generic, fixed-length binary data buffer. You can't directly manipulate the contents of an <code>ArrayBuffer</code>; instead, you create an <a title="en/JavaScript typed arrays/ArrayBufferView" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBufferView"><code>ArrayBufferView</code></a> object which represents the buffer in a specific format, and use that to read and write the contents of the buffer.
  */
public interface ArrayBuffer {


  /**
    * The size, in bytes, of the array. This is established when the array is constructed and cannot be changed. <strong>Read only.</strong>
    */
  int getByteLength();

  ArrayBuffer slice(int begin);

  ArrayBuffer slice(int begin, int end);
}
