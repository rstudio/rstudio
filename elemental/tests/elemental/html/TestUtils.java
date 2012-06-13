/*
 * Copyright 2010 Google Inc.
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

import static elemental.client.Browser.getDocument;

import elemental.events.EventTarget;
import elemental.events.MouseEvent;

/**
 * Utilities for simplifying DOM tests.
 */
public class TestUtils {

  /**
   * Fires a left-click event on the given target (typically a DOM node).
   */
  public static void click(EventTarget target) {
    MouseEvent evt = (MouseEvent) getDocument().createEvent(
        Document.Event.MOUSE);
    evt.initMouseEvent("click", true, true, null, 0, 0, 0, 0, 0, false, false,
        false, false, MouseEvent.Button.PRIMARY, null);
    target.dispatchEvent(evt);
  }
}
