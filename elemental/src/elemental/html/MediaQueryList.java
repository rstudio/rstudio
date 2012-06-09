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
<p>A <code>MediaQueryList</code> object maintains a list of <a title="En/CSS/Media queries" rel="internal" href="https://developer.mozilla.org/En/CSS/Media_queries">media queries</a> on a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/document">document</a></code>
, and handles sending notifications to listeners when the media queries on the document change.</p>
<p>This makes it possible to observe a document to detect when its media queries change, instead of polling the values periodically, if you need to programmatically detect changes to the values of media queries on a document.</p>
  */
public interface MediaQueryList {


  /**
    * <code>true</code> if the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/document">document</a></code>
 currently matches the media query list; otherwise <code>false</code>. <strong>Read only.</strong>
    */
  boolean isMatches();


  /**
    * The serialized media query list.
    */
  String getMedia();


  /**
    * <p>Adds a new listener to the media query list. If the specified listener is already in the list, this method has no effect.</p>

<div id="section_5"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>listener</code></dt> <dd>The <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/MediaQueryListListener">MediaQueryListListener</a></code>
 to invoke when the media query's evaluated result changes.</dd> <div id="section_6"><span id="removeListener()"></span></div></dl></div>
    */
  void addListener(MediaQueryListListener listener);


  /**
    * <div id="section_5"><dl><div id="section_6"><p>Removes a listener from the media query list. Does nothing if the specified listener isn't already in the list.</p> 
</div></dl>
</div><div id="section_7"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>listener</code></dt> <dd>The <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/MediaQueryListListener">MediaQueryListListener</a></code>
 to stop calling on changes to the media query's evaluated result.</dd>
</dl>
</div>
    */
  void removeListener(MediaQueryListListener listener);
}
