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
 * <p>Class that contains constants for all ARIA properties as defined by the W3C specification
 * <a href="http://www.w3.org/TR/wai-aria/">W3C ARIA specification</a>.</p>
 *
 * <p>An ARIA property is a characteristic feature of a widget/control that can change over time but
 * more rare than the ARIA state and is often not changed as a result of user action.<p>
 *
 * <p>The following groups of properties exist:
 * <ol>
 * <li>Widget properties -- specific to common user interface elements found on GUI systems or
 * in rich Internet applications which receive user input and process user actions</li>
 * <li>Live Region properties -- specific to live regions in rich Internet applications; may be
 * applied to any element; indicate that content changes may occur without the element having
 * focus, and provides assistive technologies with information on how to process those content
 * updates. </li>
 * <li>Drag-and-drop states -- indicates information about draggable elements and their drop
 * targets</li>
 * <li>Relationship properties -- indicates relationships or associations between elements which
 * cannot be readily determined from the document structure</li>
 * </ol>
 * </p>
 */
public final class Property {
  public static final Attribute<IdReference> ACTIVEDESCENDANT =
      new AriaValueAttribute<IdReference>("aria-activedescendant", "");

  public static final Attribute<Boolean> ATOMIC =
      new PrimitiveValueAttribute<Boolean>("aria-atomic", "false");

  public static final Attribute<AutocompleteValue> AUTOCOMPLETE =
      new AriaValueAttribute<AutocompleteValue>("aria-autocomplete", "none");

  public static final Attribute<IdReference> CONTROLS =
      new AriaValueAttribute<IdReference>("aria-controls", "");

  public static final Attribute<IdReference> DESCRIBEDBY =
      new AriaValueAttribute<IdReference>("aria-describedby", "");

  public static final Attribute<DropeffectValue> DROPEFFECT =
      new AriaValueAttribute<DropeffectValue>("aria-dropeffect", "none");

  public static final Attribute<IdReference> FLOWTO =
      new AriaValueAttribute<IdReference>("aria-flowto", "");

  public static final Attribute<Boolean> HASPOPUP =
      new PrimitiveValueAttribute<Boolean>("aria-haspopup", "false");

  public static final Attribute<String> LABEL =
      new PrimitiveValueAttribute<String>("aria-label", "");

  public static final Attribute<IdReference> LABELLEDBY =
      new AriaValueAttribute<IdReference>("aria-labelledby", "");

  public static final Attribute<Integer> LEVEL =
      new PrimitiveValueAttribute<Integer>("aria-level", "");

  public static final Attribute<LiveValue> LIVE =
      new AriaValueAttribute<LiveValue>("aria-live", "off");

  public static final Attribute<Boolean> MULTILINE =
      new PrimitiveValueAttribute<Boolean>("aria-multiline", "false");

  public static final Attribute<Boolean> MULTISELECTABLE =
      new PrimitiveValueAttribute<Boolean>("aria-multiselectable", "false");

  public static final Attribute<OrientationValue> ORIENTATION =
      new AriaValueAttribute<OrientationValue>("aria-orientation", "vertical");

  public static final Attribute<IdReference> OWNS =
      new AriaValueAttribute<IdReference>("aria-owns", "");

  public static final Attribute<Integer> POSINSET =
      new PrimitiveValueAttribute<Integer>("aria-posinset", "");

  public static final Attribute<Boolean> READONLY =
      new PrimitiveValueAttribute<Boolean>("aria-readonly", "false");

  public static final Attribute<RelevantValue> RELEVANT =
      new AriaValueAttribute<RelevantValue>("aria-relevant", "additions text");

  public static final Attribute<Boolean> REQUIRED =
      new PrimitiveValueAttribute<Boolean>("aria-required", "false");

  public static final Attribute<Integer> SETSIZE =
      new PrimitiveValueAttribute<Integer>("aria-setsize", "");

  public static final Attribute<SortValue> SORT =
      new AriaValueAttribute<SortValue>("aria-sort", "none");

  public static final Attribute<Number> VALUEMAX =
      new PrimitiveValueAttribute<Number>("aria-valuemax", "");

  public static final Attribute<Number> VALUEMIN =
      new PrimitiveValueAttribute<Number>("aria-valuemin", "");

  public static final Attribute<Number> VALUENOW =
      new PrimitiveValueAttribute<Number>("aria-valuenow", "");

  public static final Attribute<String> VALUETEXT =
      new PrimitiveValueAttribute<String>("aria-valuetext", "");

  private Property() {
  }
}