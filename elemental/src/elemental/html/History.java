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
  * Returns a reference to the <code>History</code> object, which provides an interface for manipulating the browser <em>session history</em> (pages visited in the tab or frame that the current page is loaded in).
  */
public interface History {


  /**
    * Read-only. Returns the number of elements in the session history, including the currently loaded page. For example, for a page loaded in a new tab this property returns <code>1</code>.
    */
  int getLength();


  /**
    * Returns the state at the top of the history stack. This is a way to look at the state without having to wait for a <code>popstate</code> event. <strong>Read only.</strong>
    */
  Object getState();


  /**
    * <p>Goes to the previous page in session history, the same action as when the user clicks the browser's Back button. Equivalent to <code>history.go(-1)</code>.</p> <div class="note"><strong>Note:</strong> Calling this method to go back beyond the first page in the session history has no effect and doesn't raise an exception.</div>
    */
  void back();


  /**
    * <p>Goes to the next page in session history, the same action as when the user clicks the browser's Forward button; this is equivalent to <code>history.go(1)</code>.</p> <div class="note"><strong>Note:</strong> Calling this method to go back beyond the last page in the session history has no effect and doesn't raise an exception.</div>
    */
  void forward();


  /**
    * Loads a page from the session history, identified by its relative location to the current page, for example <code>-1</code> for the previous page or <code>1</code> for the next page. When <code><em>integerDelta</em></code> is out of bounds (e.g. -1 when there are no previously visited pages in the session history), the method doesn't do anything and doesn't raise an exception. Calling <code>go()</code> without parameters or with a non-integer argument has no effect (unlike Internet Explorer, <a class="external" title="http://msdn.microsoft.com/en-us/library/ms536443(VS.85).aspx" rel="external" href="http://msdn.microsoft.com/en-us/library/ms536443(VS.85).aspx" target="_blank">which supports string URLs as the argument</a>).
    */
  void go(int distance);


  /**
    * <p>Pushes the given data onto the session history stack with the specified title and, if provided, URL. The data is treated as opaque by the DOM; you may specify any JavaScript object that can be serialized.&nbsp; Note that Firefox currently ignores the title parameter; for more information, see <a title="en/DOM/Manipulating the browser history" rel="internal" href="https://developer.mozilla.org/en/DOM/Manipulating_the_browser_history">manipulating the browser history</a>.</p> <div class="note"><strong>Note:</strong> In Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
 through Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
, the passed object is serialized using JSON. Starting in Gecko 6.0 (Firefox 6.0 / Thunderbird 6.0 / SeaMonkey 2.3)
, the object is serialized using <a title="en/DOM/The structured clone algorithm" rel="internal" href="https://developer.mozilla.org/en/DOM/The_structured_clone_algorithm">the structured clone algorithm</a>. This allows a wider variety of objects to be safely passed.</div>
    */
  void pushState(Object data, String title);


  /**
    * <p>Pushes the given data onto the session history stack with the specified title and, if provided, URL. The data is treated as opaque by the DOM; you may specify any JavaScript object that can be serialized.&nbsp; Note that Firefox currently ignores the title parameter; for more information, see <a title="en/DOM/Manipulating the browser history" rel="internal" href="https://developer.mozilla.org/en/DOM/Manipulating_the_browser_history">manipulating the browser history</a>.</p> <div class="note"><strong>Note:</strong> In Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
 through Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
, the passed object is serialized using JSON. Starting in Gecko 6.0 (Firefox 6.0 / Thunderbird 6.0 / SeaMonkey 2.3)
, the object is serialized using <a title="en/DOM/The structured clone algorithm" rel="internal" href="https://developer.mozilla.org/en/DOM/The_structured_clone_algorithm">the structured clone algorithm</a>. This allows a wider variety of objects to be safely passed.</div>
    */
  void pushState(Object data, String title, String url);


  /**
    * <p>Updates the most recent entry on the history stack to have the specified data, title, and, if provided, URL. The data is treated as opaque by the DOM; you may specify any JavaScript object that can be serialized.&nbsp; Note that Firefox currently ignores the title parameter; for more information, see <a title="en/DOM/Manipulating the browser history" rel="internal" href="https://developer.mozilla.org/en/DOM/Manipulating_the_browser_history">manipulating the browser history</a>.</p> <div class="note"><strong>Note:</strong> In Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
 through Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
, the passed object is serialized using JSON. Starting in Gecko 6.0 (Firefox 6.0 / Thunderbird 6.0 / SeaMonkey 2.3)
, the object is serialized using <a title="en/DOM/The structured clone algorithm" rel="internal" href="https://developer.mozilla.org/en/DOM/The_structured_clone_algorithm">the structured clone algorithm</a>. This allows a wider variety of objects to be safely passed.</div>
    */
  void replaceState(Object data, String title);


  /**
    * <p>Updates the most recent entry on the history stack to have the specified data, title, and, if provided, URL. The data is treated as opaque by the DOM; you may specify any JavaScript object that can be serialized.&nbsp; Note that Firefox currently ignores the title parameter; for more information, see <a title="en/DOM/Manipulating the browser history" rel="internal" href="https://developer.mozilla.org/en/DOM/Manipulating_the_browser_history">manipulating the browser history</a>.</p> <div class="note"><strong>Note:</strong> In Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
 through Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
, the passed object is serialized using JSON. Starting in Gecko 6.0 (Firefox 6.0 / Thunderbird 6.0 / SeaMonkey 2.3)
, the object is serialized using <a title="en/DOM/The structured clone algorithm" rel="internal" href="https://developer.mozilla.org/en/DOM/The_structured_clone_algorithm">the structured clone algorithm</a>. This allows a wider variety of objects to be safely passed.</div>
    */
  void replaceState(Object data, String title, String url);
}
