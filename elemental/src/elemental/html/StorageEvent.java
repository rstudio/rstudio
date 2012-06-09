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
import elemental.events.Event;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div><div>

<a rel="custom" href="http://mxr.mozilla.org/mozilla-central/source/dom/interfaces/storage/nsIDOMStorageEvent.idl"><code>dom/interfaces/storage/nsIDOMStorageEvent.idl</code></a><span><a rel="internal" href="https://developer.mozilla.org/en/Interfaces/About_Scriptable_Interfaces" title="en/Interfaces/About_Scriptable_Interfaces">Scriptable</a></span></div><span>Describes an event occurring on HTML5 client-side storage data.</span><div><div>1.0</div><div>11.0</div><div></div><div>Introduced</div><div>Gecko 2.0</div><div title="Introduced in Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
"></div><div title="Last changed in Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
"></div></div>
<div>Inherits from: <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsIDOMEvent">nsIDOMEvent</a></code>
<span>Last changed in Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
</span></div></div>
<p></p>
<p>A <code>StorageEvent</code> is sent to a window when a storage area changes.</p>
<div class="geckoVersionNote">
<p>
</p><div class="geckoVersionHeading">Gecko 2.0 note<div>(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
</div></div>
<p></p>
<p>Although this event existed prior to Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
, it did not match the specification. The old event format is now represented by the <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsIDOMStorageEventObsolete">nsIDOMStorageEventObsolete</a></code>
 interface.</p>
</div>
  */
public interface StorageEvent extends Event {


  /**
    * Represents the key changed. The <code>key</code> attribute is <code>null</code> when the change is caused by the storage <code>clear()</code> method. <strong>Read only.</strong>
    */
  String getKey();


  /**
    * The new value of the <code>key</code>. The <code>newValue</code> is <code>null</code> when the change has been invoked by storage <code>clear()</code> method or the <code>key</code> has been removed from the storage. <strong>Read only.</strong>
    */
  String getNewValue();


  /**
    * The original value of the <code>key</code>. The <code>oldValue</code> is <code>null</code> when the change has been invoked by storage <code>clear()</code> method or the <code>key</code> has been newly added and therefor doesn't have any previous value. <strong>Read only.</strong>
    */
  String getOldValue();


  /**
    * Represents the Storage object that was affected. <strong>Read only.</strong>
    */
  Storage getStorageArea();


  /**
    * The URL of the document whose <code>key</code> changed. <strong>Read only.</strong>
    */
  String getUrl();


  /**
    * <p>Initializes the event in a manner analogous to the similarly-named method in the DOM Events interfaces.</p>

<div id="section_5"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>typeArg</code></dt> <dd>The name of the event.</dd> <dt><code>canBubbleArg</code></dt> <dd>A boolean indicating whether the event bubbles up through the DOM or not.</dd> <dt><code>cancelableArg</code></dt> <dd>A boolean indicating whether the event is cancelable.</dd> <dt><code>keyArg</code></dt> <dd>The key whose value is changing as a result of this event.</dd> <dt><code>oldValueArg</code></dt> <dd>The key's old value.</dd> <dt><code>newValueArg</code></dt> <dd>The key's new value.</dd> <dt><code>urlArg</code></dt> <dd>Missing Description</dd> <dt><code>storageAreaArg</code></dt> <dd>The DOM&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Storage">Storage</a></code>
 object representing the storage area on which this event occurred.</dd>
</dl></div>
    */
  void initStorageEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg, String keyArg, String oldValueArg, String newValueArg, String urlArg, Storage storageAreaArg);
}
