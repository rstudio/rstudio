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
import elemental.dom.Element;
import elemental.dom.NodeList;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * The HTML <em>progress</em> (<code>&lt;progress&gt;</code>) element is used to view the completion progress of a task. While the specifics of how it's displayed is left up to the browser developer, it's typically displayed as a progress bar.
  */
public interface ProgressElement extends Element {

  NodeList getLabels();


  /**
    * This attribute describes how much work the task indicated by the <code>progress</code> element requires.
    */
  double getMax();

  void setMax(double arg);

  double getPosition();


  /**
    * <dl><dd>This attribute specifies how much of the task that has been completed. If there is no <code>value</code> attribute, the progress bar is indeterminate; this indicates that an activity is ongoing with no indication of how long it is expected to take.</dd>
</dl>
<p>You can use the <code><a rel="custom" href="https://developer.mozilla.org/en/CSS/orient">orient</a></code>
 property to specify whether the progress bar should be rendered horizontally (the default) or vertically. The <code><a rel="custom" href="https://developer.mozilla.org/en/CSS/%3Aindeterminate">:indeterminate</a></code>
 pseudo-class can be used to match against indeterminate progress bars.</p>
    */
  double getValue();

  void setValue(double arg);
}
