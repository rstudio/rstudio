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
      new AriaValuedAttribute<IdReference>("aria-activedescendant", "");

  public static final Attribute<Boolean> ATOMIC =
      new PrimitiveTypeAttribute<Boolean>("aria-atomic", "false");

  public static final Attribute<AutocompleteValue> AUTOCOMPLETE =
      new AriaValuedAttribute<AutocompleteValue>("aria-autocomplete", "none");

  public static final Attribute<IdReference> CONTROLS =
      new AriaValuedAttribute<IdReference>("aria-controls", "");

  public static final Attribute<IdReference> DESCRIBEDBY =
      new AriaValuedAttribute<IdReference>("aria-describedby", "");

  public static final Attribute<DropeffectValue> DROPEFFECT =
      new AriaValuedAttribute<DropeffectValue>("aria-dropeffect", "none");

  public static final Attribute<IdReference> FLOWTO =
      new AriaValuedAttribute<IdReference>("aria-flowto", "");

  public static final Attribute<Boolean> HASPOPUP =
      new PrimitiveTypeAttribute<Boolean>("aria-haspopup", "false");

  public static final Attribute<String> LABEL =
      new PrimitiveTypeAttribute<String>("aria-label", "");

  public static final Attribute<IdReference> LABELLEDBY =
      new AriaValuedAttribute<IdReference>("aria-labelledby", "");

  public static final Attribute<Integer> LEVEL =
      new PrimitiveTypeAttribute<Integer>("aria-level", "");

  public static final Attribute<LiveValue> LIVE =
      new AriaValuedAttribute<LiveValue>("aria-live", "off");

  public static final Attribute<Boolean> MULTILINE =
      new PrimitiveTypeAttribute<Boolean>("aria-multiline", "false");

  public static final Attribute<Boolean> MULTISELECTABLE =
      new PrimitiveTypeAttribute<Boolean>("aria-multiselectable", "false");

  public static final Attribute<OrientationValue> ORIENTATION =
      new AriaValuedAttribute<OrientationValue>("aria-orientation", "vertical");

  public static final Attribute<IdReference> OWNS =
      new AriaValuedAttribute<IdReference>("aria-owns", "");

  public static final Attribute<Integer> POSINSET =
      new PrimitiveTypeAttribute<Integer>("aria-posinset", "");

  public static final Attribute<Boolean> READONLY =
      new PrimitiveTypeAttribute<Boolean>("aria-readonly", "false");

  public static final Attribute<RelevantValue> RELEVANT =
      new AriaValuedAttribute<RelevantValue>("aria-relevant", "additions text");

  public static final Attribute<Boolean> REQUIRED =
      new PrimitiveTypeAttribute<Boolean>("aria-required", "false");

  public static final Attribute<Integer> SETSIZE =
      new PrimitiveTypeAttribute<Integer>("aria-setsize", "");

  public static final Attribute<SortValue> SORT =
      new AriaValuedAttribute<SortValue>("aria-sort", "none");

  public static final Attribute<Number> VALUEMAX =
      new PrimitiveTypeAttribute<Number>("aria-valuemax", "");

  public static final Attribute<Number> VALUEMIN =
      new PrimitiveTypeAttribute<Number>("aria-valuemin", "");

  public static final Attribute<Number> VALUENOW =
      new PrimitiveTypeAttribute<Number>("aria-valuenow", "");

  public static final Attribute<String> VALUETEXT =
      new PrimitiveTypeAttribute<String>("aria-valuetext", "");

  private Property() {
  }
}