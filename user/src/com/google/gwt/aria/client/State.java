/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.aria.client;
/////////////////////////////////////////////////////////
// This is auto-generated code.  Do not manually edit! //
/////////////////////////////////////////////////////////

import com.google.gwt.aria.client.StateTokenTypes.CheckedToken;
import com.google.gwt.aria.client.StateTokenTypes.ExpandedToken;
import com.google.gwt.aria.client.StateTokenTypes.GrabbedToken;
import com.google.gwt.aria.client.StateTokenTypes.InvalidToken;
import com.google.gwt.aria.client.StateTokenTypes.PressedToken;
import com.google.gwt.aria.client.StateTokenTypes.SelectedToken;

/**
 * <p>Class that contains constants for ARIA states. States combined with ARIA roles supply
 * information about the changes in the web page that can be used for alerts, notification,
 * navigation assistance. The state is changed as a result of an user interaction and developers
 * should consider changing the widget state when handling user actions.</p>
 *
 * <p>The following groups of states exist:
 * <ol>
 * <li>Widget states -- specific to common user interface elements found on GUI systems or
 * in rich Internet applications which receive user input and process user actions</li>
 * <li>Live Region states -- specific to live regions in rich Internet applications; may be applied
 * to any element; indicate that content changes may occur without the element having focus, and
 * provides assistive technologies with information on how to process those content updates. </li>
 * <li>Drag-and-drop states -- indicates information about draggable elements and their drop
 * targets</li>
 * </ol>
 * </p>
 *
 * @param <T> The state value type
 */
public final class State<T> extends Attribute<T> {
  public static final State<Boolean> BUSY =
      new State<Boolean>("aria-busy", "false");

  public static final State<CheckedToken> CHECKED =
      new State<CheckedToken>("aria-checked", "undefined");

  public static final State<Boolean> DISABLED =
      new State<Boolean>("aria-disabled", "false");

  public static final State<ExpandedToken> EXPANDED =
      new State<ExpandedToken>("aria-expanded", "undefined");

  public static final State<GrabbedToken> GRABBED =
      new State<GrabbedToken>("aria-grabbed", "undefined");

  public static final State<Boolean> HIDDEN =
      new State<Boolean>("aria-hidden", "false");

  public static final State<InvalidToken> INVALID =
      new State<InvalidToken>("aria-invalid", "false");

  public static final State<PressedToken> PRESSED =
      new State<PressedToken>("aria-pressed", "undefined");

  public static final State<SelectedToken> SELECTED =
      new State<SelectedToken>("aria-selected", "undefined");


  public State(String name) {
    super(name);
  }

  public State(String name, String defaultValue) {
    super(name, defaultValue);
  }
}