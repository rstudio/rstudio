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

import com.google.gwt.aria.client.CommonAttributeTypes.AriaAttributeType;

/**
 * Token type enums for Property ARIA attributes
 */
public final class PropertyTokenTypes {
  /**
   * AutocompleteToken type for the 'aria-autocomplete' property.
   */
  public static enum AutocompleteToken implements AriaAttributeType {
    INLINE, LIST, BOTH, NONE;

    @Override
    public String getAriaValue() {
      switch (this) {
        case INLINE:
          return "inline";
        case LIST:
          return "list";
        case BOTH:
          return "both";
        case NONE:
          return "none";
      }
      return null; // not reachable
    }
  }

  /**
   * DropeffectToken type for the 'aria-dropeffect' property.
   */
  public static enum DropeffectToken implements AriaAttributeType {
    COPY, MOVE, LINK, EXECUTE, POPUP, NONE;

    @Override
    public String getAriaValue() {
      switch (this) {
        case COPY:
          return "copy";
        case MOVE:
          return "move";
        case LINK:
          return "link";
        case EXECUTE:
          return "execute";
        case POPUP:
          return "popup";
        case NONE:
          return "none";
      }
      return null; // not reachable
    }
  }

  /**
   * DropeffectTokenList tokens class type.
   */
  public static final class DropeffectTokenList implements AriaAttributeType {

    private final DropeffectToken[] tokens;

    public DropeffectTokenList(DropeffectToken... tokens) {
      this.tokens = tokens;
    }

    @Override
    public String getAriaValue() {
      StringBuffer buf = new StringBuffer();
       for (DropeffectToken token : tokens) {
         buf.append(token.getAriaValue()).append(" ");
      }
      return buf.toString().trim();
    }
  }

  /**
   * LiveToken type for the 'aria-live' property.
   */
  public static enum LiveToken implements AriaAttributeType {
    OFF, POLITE, ASSERTIVE;

    @Override
    public String getAriaValue() {
      switch (this) {
        case OFF:
          return "off";
        case POLITE:
          return "polite";
        case ASSERTIVE:
          return "assertive";
      }
      return null; // not reachable
    }
  }

  /**
   * OrientationToken type for the 'aria-orientation' property.
   */
  public static enum OrientationToken implements AriaAttributeType {
    HORIZONTAL, VERTICAL;

    @Override
    public String getAriaValue() {
      switch (this) {
        case HORIZONTAL:
          return "horizontal";
        case VERTICAL:
          return "vertical";
      }
      return null; // not reachable
    }
  }

  /**
   * RelevantToken type for the 'aria-relevant' property.
   */
  public static enum RelevantToken implements AriaAttributeType {
    ADDITIONS, REMOVALS, TEXT, ALL;

    @Override
    public String getAriaValue() {
      switch (this) {
        case ADDITIONS:
          return "additions";
        case REMOVALS:
          return "removals";
        case TEXT:
          return "text";
        case ALL:
          return "all";
      }
      return null; // not reachable
    }
  }

  /**
   * RelevantTokenList tokens class type.
   */
  public static final class RelevantTokenList implements AriaAttributeType {

    private final RelevantToken[] tokens;

    public RelevantTokenList(RelevantToken... tokens) {
      this.tokens = tokens;
    }

    @Override
    public String getAriaValue() {
      StringBuffer buf = new StringBuffer();
       for (RelevantToken token : tokens) {
         buf.append(token.getAriaValue()).append(" ");
      }
      return buf.toString().trim();
    }
  }

  /**
   * SortToken type for the 'aria-sort' property.
   */
  public static enum SortToken implements AriaAttributeType {
    ASCENDING, DESCENDING, NONE, OTHER;

    @Override
    public String getAriaValue() {
      switch (this) {
        case ASCENDING:
          return "ascending";
        case DESCENDING:
          return "descending";
        case NONE:
          return "none";
        case OTHER:
          return "other";
      }
      return null; // not reachable
    }
  }
}
