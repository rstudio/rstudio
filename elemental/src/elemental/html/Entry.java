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
  * <div><strong>DRAFT</strong> <div>This page is not complete.</div>
</div>
<p>The <code>Entry</code> interface of the <a title="en/DOM/File_API/File_System_API" rel="internal" href="https://developer.mozilla.org/en/DOM/File_API/File_System_API">FileSystem API</a> represents entries in a file system. The entries can be a file&nbsp;or a <a href="https://developer.mozilla.org/en/DOM/File_API/File_system_API/DirectoryEntry" rel="internal" title="en/DOM/File_API/File_system_API/DirectoryEntry">DirectoryEntry</a>.</p>
  */
public interface Entry {


  /**
    * The file system on which the entry resides.
    */
  DOMFileSystem getFilesystem();

  String getFullPath();


  /**
    * The entry is a directory.
    */
  boolean isDirectory();


  /**
    * The entry is a file.
    */
  boolean isFile();


  /**
    * The name of the entry, excluding the path leading to it.
    */
  String getName();


  /**
    * <p>Copy an entry to a different location on the file system. You cannot copy an entry inside itself if it is a directory nor can you copy it into its parent if a name different from its current one isn't provided. Directory copies are always recursive—that is, they copy all contents of the directory.</p>
<pre>void vopyTo (
  <em>(in DirectoryEntry parent, optional DOMString newName, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_12"><span id="Parameter_3"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCallback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_13"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void copyTo(DirectoryEntry parent);


  /**
    * <p>Copy an entry to a different location on the file system. You cannot copy an entry inside itself if it is a directory nor can you copy it into its parent if a name different from its current one isn't provided. Directory copies are always recursive—that is, they copy all contents of the directory.</p>
<pre>void vopyTo (
  <em>(in DirectoryEntry parent, optional DOMString newName, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_12"><span id="Parameter_3"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCallback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_13"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void copyTo(DirectoryEntry parent, String name);


  /**
    * <p>Copy an entry to a different location on the file system. You cannot copy an entry inside itself if it is a directory nor can you copy it into its parent if a name different from its current one isn't provided. Directory copies are always recursive—that is, they copy all contents of the directory.</p>
<pre>void vopyTo (
  <em>(in DirectoryEntry parent, optional DOMString newName, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_12"><span id="Parameter_3"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCallback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_13"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void copyTo(DirectoryEntry parent, String name, EntryCallback successCallback);


  /**
    * <p>Copy an entry to a different location on the file system. You cannot copy an entry inside itself if it is a directory nor can you copy it into its parent if a name different from its current one isn't provided. Directory copies are always recursive—that is, they copy all contents of the directory.</p>
<pre>void vopyTo (
  <em>(in DirectoryEntry parent, optional DOMString newName, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_12"><span id="Parameter_3"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCallback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_13"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void copyTo(DirectoryEntry parent, String name, EntryCallback successCallback, ErrorCallback errorCallback);


  /**
    * <p>Look up metadata about this entry.</p>
<pre>void getMetada (
  in MetadataCallback ErrorCallback
);</pre>
<div id="section_6"><span id="Parameter"></span><h5 class="editable">Parameter</h5>
<dl> <dt>successCallback</dt> <dd>A callback that is called with the time of the last modification.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_7"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void getMetadata(MetadataCallback successCallback);


  /**
    * <p>Look up metadata about this entry.</p>
<pre>void getMetada (
  in MetadataCallback ErrorCallback
);</pre>
<div id="section_6"><span id="Parameter"></span><h5 class="editable">Parameter</h5>
<dl> <dt>successCallback</dt> <dd>A callback that is called with the time of the last modification.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_7"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void getMetadata(MetadataCallback successCallback, ErrorCallback errorCallback);


  /**
    * <p>Look up the parent <code>DirectoryEntry</code> containing this entry. If this entry is the root of its filesystem, its parent is itself.</p>
<pre>void getParent (
  <em>(in EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_21"><span id="Parameter_6"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCellback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_22"><span id="Returns_6"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void getParent();


  /**
    * <p>Look up the parent <code>DirectoryEntry</code> containing this entry. If this entry is the root of its filesystem, its parent is itself.</p>
<pre>void getParent (
  <em>(in EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_21"><span id="Parameter_6"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCellback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_22"><span id="Returns_6"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void getParent(EntryCallback successCallback);


  /**
    * <p>Look up the parent <code>DirectoryEntry</code> containing this entry. If this entry is the root of its filesystem, its parent is itself.</p>
<pre>void getParent (
  <em>(in EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_21"><span id="Parameter_6"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCellback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_22"><span id="Returns_6"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void getParent(EntryCallback successCallback, ErrorCallback errorCallback);


  /**
    * <p>Move an entry to a different location on the file system. You cannot do the following:</p>
<ul> <li>move a directory inside itself or to any child at any depth;</li> <li>move an entry into its parent if a name different from its current one isn't provided;</li> <li>move a file to a path occupied by a directory;</li> <li>move a directory to a path occupied by a file;</li> <li>move any element to a path occupied by a directory which is not empty.</li>
</ul>
<p>Moving a file over an existing file&nbsp;replaces that existing file. A move of a directory on top of an existing empty directory&nbsp;replaces that directory.</p>
<pre>void moveTo (
  <em>(in DirectoryEntry parent, optional DOMString newName, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_9"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCallback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_10"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void moveTo(DirectoryEntry parent);


  /**
    * <p>Move an entry to a different location on the file system. You cannot do the following:</p>
<ul> <li>move a directory inside itself or to any child at any depth;</li> <li>move an entry into its parent if a name different from its current one isn't provided;</li> <li>move a file to a path occupied by a directory;</li> <li>move a directory to a path occupied by a file;</li> <li>move any element to a path occupied by a directory which is not empty.</li>
</ul>
<p>Moving a file over an existing file&nbsp;replaces that existing file. A move of a directory on top of an existing empty directory&nbsp;replaces that directory.</p>
<pre>void moveTo (
  <em>(in DirectoryEntry parent, optional DOMString newName, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_9"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCallback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_10"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void moveTo(DirectoryEntry parent, String name);


  /**
    * <p>Move an entry to a different location on the file system. You cannot do the following:</p>
<ul> <li>move a directory inside itself or to any child at any depth;</li> <li>move an entry into its parent if a name different from its current one isn't provided;</li> <li>move a file to a path occupied by a directory;</li> <li>move a directory to a path occupied by a file;</li> <li>move any element to a path occupied by a directory which is not empty.</li>
</ul>
<p>Moving a file over an existing file&nbsp;replaces that existing file. A move of a directory on top of an existing empty directory&nbsp;replaces that directory.</p>
<pre>void moveTo (
  <em>(in DirectoryEntry parent, optional DOMString newName, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_9"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCallback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_10"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void moveTo(DirectoryEntry parent, String name, EntryCallback successCallback);


  /**
    * <p>Move an entry to a different location on the file system. You cannot do the following:</p>
<ul> <li>move a directory inside itself or to any child at any depth;</li> <li>move an entry into its parent if a name different from its current one isn't provided;</li> <li>move a file to a path occupied by a directory;</li> <li>move a directory to a path occupied by a file;</li> <li>move any element to a path occupied by a directory which is not empty.</li>
</ul>
<p>Moving a file over an existing file&nbsp;replaces that existing file. A move of a directory on top of an existing empty directory&nbsp;replaces that directory.</p>
<pre>void moveTo (
  <em>(in DirectoryEntry parent, optional DOMString newName, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_9"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>parent</dt> <dd>The directory to which to move the entry.</dd> <dt>newName</dt> <dd>The new name of the entry. Defaults to the entry's current name if unspecified.</dd> <dt>successCallback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_10"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void moveTo(DirectoryEntry parent, String name, EntryCallback successCallback, ErrorCallback errorCallback);


  /**
    * <p>Deletes a file or directory. You cannot delete an empty directory or the root directory of a filesystem.</p>
<pre>void remove (
  <em>(in VoidCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_18"><span id="Parameter_5"></span><h5 class="editable">Parameter</h5>
<dl> <dt>successCallback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_19"><span id="Returns_5"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void remove(VoidCallback successCallback);


  /**
    * <p>Deletes a file or directory. You cannot delete an empty directory or the root directory of a filesystem.</p>
<pre>void remove (
  <em>(in VoidCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_18"><span id="Parameter_5"></span><h5 class="editable">Parameter</h5>
<dl> <dt>successCallback</dt> <dd>A callback that is called with the entry for the new object.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl>
</div><div id="section_19"><span id="Returns_5"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void remove(VoidCallback successCallback, ErrorCallback errorCallback);


  /**
    * <p>Returns a URL that can be used to identify this entry. It has no specific expiration. Bcause it describes a location on disk, it is valid for as long as that location exists. Users can supply&nbsp;<code>mimeType</code>&nbsp;to simulate the optional mime-type header associated with HTTP downloads.</p>
<pre>DOMString toURL (
  <em>(in </em>optional DOMString mimeType<em>);</em>
);</pre>
<div id="section_15"><span id="Parameter_4"></span><h5 class="editable">Parameter</h5>
<dl> <dt>mimeType</dt> <dd>For a FileEntry, the mime type to be used to interpret the file, when loaded through this URL.</dd>
</dl>
</div><div id="section_16"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>DOMString</code></dt>
</dl>
</div>
    */
  String toURL();
}
