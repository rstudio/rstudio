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
package elemental.css;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * CSS counters are an implementation of <a class="external" rel="external" href="http://www.w3.org/TR/CSS21/generate.html#counters" title="http://www.w3.org/TR/CSS21/generate.html#counters" target="_blank">Automatic counters and numbering</a> in CSS 2.1. The value of a counter is manipulated through the use of <code><a rel="custom" href="https://developer.mozilla.org/en/CSS/counter-reset">counter-reset</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/CSS/counter-increment">counter-increment</a></code>
 and is displayed on a page using the <code>counter()</code> or <code>counters()</code> function of the <code><a title="en/CSS/content" rel="internal" href="https://developer.mozilla.org/en/CSS/content">content</a></code> property.
  */
public interface Counter {

  String getIdentifier();

  String getListStyle();

  String getSeparator();
}
