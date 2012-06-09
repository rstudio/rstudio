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
<div class="note"><strong>Note:</strong> <code>DataView</code> is not yet implemented in Gecko. It is implemented in Chrome 9.</div>
<p>An <code>ArrayBuffer</code> is a useful object for representing an arbitrary chunk of data. In many cases, such data will be read from disk or from the network, and will not follow the alignment restrictions that are imposed on the <a title="en/JavaScript_typed_arrays/ArrayBufferView" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBufferView">Typed Array Views</a> described earlier. In addition, the data will often be heterogeneous in nature and have a defined byte order.</p>
<p>The <code>DataView</code> view provides a low-level interface for reading such data from and writing it to an <code><a title="en/JavaScript_typed_arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer">ArrayBuffer</a></code>.</p>
  */
public interface DataView extends ArrayBufferView {

  float getFloat32(int byteOffset);

  float getFloat32(int byteOffset, boolean littleEndian);

  double getFloat64(int byteOffset);

  double getFloat64(int byteOffset, boolean littleEndian);

  short getInt16(int byteOffset);

  short getInt16(int byteOffset, boolean littleEndian);

  int getInt32(int byteOffset);

  int getInt32(int byteOffset, boolean littleEndian);


  /**
    * <p>Gets a signed 8-bit integer at the specified byte offset from the start of the view.</p>

<div id="section_11"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>offset</code></dt> <dd>The offset, in byte, from the start of the view where to read the data.</dd>
</dl>
</div><div id="section_12"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>The <code>byteOffset</code> is set such as it would read beyond the end of the view</dd>
</dl>
</div>
    */
  Object getInt8();

  int getUint16(int byteOffset);

  int getUint16(int byteOffset, boolean littleEndian);

  int getUint32(int byteOffset);

  int getUint32(int byteOffset, boolean littleEndian);


  /**
    * <p>Gets an unsigned 8-bit integer at the specified byte offset from the start of the view.</p>

<div id="section_14"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>offset</code></dt> <dd>The offset, in byte, from the start of the view where to read the data.</dd>
</dl>
<dl> <dt><code>INDEX_SIZE_ERR</code></dt> <dd>The <code>byteOffset</code> is set such as it would read beyond the end of the view</dd>
</dl>
</div>
    */
  Object getUint8();

  void setFloat32(int byteOffset, float value);

  void setFloat32(int byteOffset, float value, boolean littleEndian);

  void setFloat64(int byteOffset, double value);

  void setFloat64(int byteOffset, double value, boolean littleEndian);

  void setInt16(int byteOffset, short value);

  void setInt16(int byteOffset, short value, boolean littleEndian);

  void setInt32(int byteOffset, int value);

  void setInt32(int byteOffset, int value, boolean littleEndian);

  void setInt8();

  void setUint16(int byteOffset, int value);

  void setUint16(int byteOffset, int value, boolean littleEndian);

  void setUint32(int byteOffset, int value);

  void setUint32(int byteOffset, int value, boolean littleEndian);

  void setUint8();
}
