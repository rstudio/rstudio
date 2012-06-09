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
public interface MediaQueryListListener {

  void queryChanged(MediaQueryList list);
}
