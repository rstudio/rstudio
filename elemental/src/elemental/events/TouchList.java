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
package elemental.events;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * A <code>TouchList</code> represents a list of all of the points of contact with a touch surface; for example, if the user has three fingers on the screen (or trackpad), the corresponding <code>TouchList</code> would have one <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Touch">Touch</a></code>
 object for each finger, for a total of three entries.
  */
public interface TouchList extends Indexable {


  /**
    * The number of <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Touch">Touch</a></code>
&nbsp;objects in the <code>TouchList</code>. <strong>Read only.</strong>
    */
  int getLength();


  /**
    * Returns the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Touch">Touch</a></code>
 object at the specified index in the list. You can also simply reference the <code>TouchList</code> using array syntax (<code>touchList[x]</code>).
    */
  Touch item(int index);
}
