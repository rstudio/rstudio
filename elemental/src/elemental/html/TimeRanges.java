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
  * <p>The <code>TimeRanges</code> interface is used to represent a set of time ranges, primarily for the purpose of tracking which portions of media have been buffered when loading it for use by the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/audio">&lt;audio&gt;</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
&nbsp;elements.</p>
<p>A <code>TimeRanges</code> object includes one or more ranges of time, each specified by a starting and ending time offset. You reference each time range by using the <code>start()</code> and <code>end()</code> methods, passing the index number of the time range you want to retrieve.</p>
  */
public interface TimeRanges {


  /**
    * The number of time ranges represented by the time range object. <strong>Read only.</strong>
    */
  int getLength();


  /**
    * Returns the time for the end of the specified range.
    */
  float end(int index);


  /**
    * Returns the time for the start of the range with the specified index.
    */
  float start(int index);
}
