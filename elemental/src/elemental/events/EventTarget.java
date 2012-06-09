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
  * An <code>EventTarget</code> is a DOM interface implemented by objects that can receive DOM events and have listeners for them. The most common <code>EventTarget</code>s are <a rel="internal" href="https://developer.mozilla.org/en/DOM/element" title="en/DOM/element">DOM elements</a>, although other objects can be <code>EventTarget</code>s too, for example <a rel="internal" href="https://developer.mozilla.org/en/DOM/document" title="en/DOM/document">document</a>, <a rel="internal" href="https://developer.mozilla.org/en/DOM/window" title="en/DOM/window">window</a>, <a rel="internal" href="https://developer.mozilla.org/en/XMLHttpRequest" title="en/XMLHttpRequest">XMLHttpRequest</a>, and others.

  */
public interface EventTarget {


  /**
    * 
Register an event handler of a specific event type on the <code>EventTarget</code>.
    */
  EventRemover addEventListener(String type, EventListener listener);


  /**
    * 
Register an event handler of a specific event type on the <code>EventTarget</code>.
    */
  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);


  /**
    * 
Dispatch an event to this <code>EventTarget</code>.
    */
  boolean dispatchEvent(Event event);


  /**
    * 
Removes an event listener from the <code>EventTarget</code>.
    */
  void removeEventListener(String type, EventListener listener);


  /**
    * 
Removes an event listener from the <code>EventTarget</code>.
    */
  void removeEventListener(String type, EventListener listener, boolean useCapture);
}
