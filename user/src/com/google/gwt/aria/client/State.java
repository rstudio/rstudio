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
 */
public final class State {
  public static final Attribute<Boolean> BUSY =
      new PrimitiveValueAttribute<Boolean>("aria-busy", "false");

  public static final Attribute<CheckedValue> CHECKED =
      new AriaValueAttribute<CheckedValue>("aria-checked", "undefined");

  public static final Attribute<Boolean> DISABLED =
      new PrimitiveValueAttribute<Boolean>("aria-disabled", "false");

  public static final Attribute<ExpandedValue> EXPANDED =
      new AriaValueAttribute<ExpandedValue>("aria-expanded", "undefined");

  public static final Attribute<GrabbedValue> GRABBED =
      new AriaValueAttribute<GrabbedValue>("aria-grabbed", "undefined");

  public static final Attribute<Boolean> HIDDEN =
      new PrimitiveValueAttribute<Boolean>("aria-hidden", "false");

  public static final Attribute<InvalidValue> INVALID =
      new AriaValueAttribute<InvalidValue>("aria-invalid", "false");

  public static final Attribute<PressedValue> PRESSED =
      new AriaValueAttribute<PressedValue>("aria-pressed", "undefined");

  public static final Attribute<SelectedValue> SELECTED =
      new AriaValueAttribute<SelectedValue>("aria-selected", "undefined");

  private State() {
  }
}
