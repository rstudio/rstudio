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

import com.google.gwt.aria.client.CommonAttributeTypes.IdReference;
import com.google.gwt.aria.client.CommonAttributeTypes.IdReferenceList;
import com.google.gwt.aria.client.PropertyTokenTypes.AutocompleteToken;
import com.google.gwt.aria.client.PropertyTokenTypes.DropeffectTokenList;
import com.google.gwt.aria.client.PropertyTokenTypes.LiveToken;
import com.google.gwt.aria.client.PropertyTokenTypes.OrientationToken;
import com.google.gwt.aria.client.PropertyTokenTypes.RelevantTokenList;
import com.google.gwt.aria.client.PropertyTokenTypes.SortToken;

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
 *
 * @param <T> The property value type
 */
public final class Property<T> extends Attribute<T> {
  public static final Property<IdReference> ACTIVEDESCENDANT =
      new Property<IdReference>("aria-activedescendant", "");

  public static final Property<Boolean> ATOMIC =
      new Property<Boolean>("aria-atomic", "false");

  public static final Property<AutocompleteToken> AUTOCOMPLETE =
      new Property<AutocompleteToken>("aria-autocomplete", "none");

  public static final Property<IdReferenceList> CONTROLS =
      new Property<IdReferenceList>("aria-controls", "");

  public static final Property<IdReferenceList> DESCRIBEDBY =
      new Property<IdReferenceList>("aria-describedby", "");

  public static final Property<DropeffectTokenList> DROPEFFECT =
      new Property<DropeffectTokenList>("aria-dropeffect", "none");

  public static final Property<IdReferenceList> FLOWTO =
      new Property<IdReferenceList>("aria-flowto", "");

  public static final Property<Boolean> HASPOPUP =
      new Property<Boolean>("aria-haspopup", "false");

  public static final Property<String> LABEL =
      new Property<String>("aria-label", "");

  public static final Property<IdReferenceList> LABELLEDBY =
      new Property<IdReferenceList>("aria-labelledby", "");

  public static final Property<Integer> LEVEL =
      new Property<Integer>("aria-level", "");

  public static final Property<LiveToken> LIVE =
      new Property<LiveToken>("aria-live", "off");

  public static final Property<Boolean> MULTILINE =
      new Property<Boolean>("aria-multiline", "false");

  public static final Property<Boolean> MULTISELECTABLE =
      new Property<Boolean>("aria-multiselectable", "false");

  public static final Property<OrientationToken> ORIENTATION =
      new Property<OrientationToken>("aria-orientation", "vertical");

  public static final Property<IdReferenceList> OWNS =
      new Property<IdReferenceList>("aria-owns", "");

  public static final Property<Integer> POSINSET =
      new Property<Integer>("aria-posinset", "");

  public static final Property<Boolean> READONLY =
      new Property<Boolean>("aria-readonly", "false");

  public static final Property<RelevantTokenList> RELEVANT =
      new Property<RelevantTokenList>("aria-relevant", "additions text");

  public static final Property<Boolean> REQUIRED =
      new Property<Boolean>("aria-required", "false");

  public static final Property<Integer> SETSIZE =
      new Property<Integer>("aria-setsize", "");

  public static final Property<SortToken> SORT =
      new Property<SortToken>("aria-sort", "none");

  public static final Property<Number> VALUEMAX =
      new Property<Number>("aria-valuemax", "");

  public static final Property<Number> VALUEMIN =
      new Property<Number>("aria-valuemin", "");

  public static final Property<Number> VALUENOW =
      new Property<Number>("aria-valuenow", "");

  public static final Property<String> VALUETEXT =
      new Property<String>("aria-valuetext", "");


  public Property(String name) {
    super(name);
  }

  public Property(String name, String defaultValue) {
    super(name, defaultValue);
  }
}