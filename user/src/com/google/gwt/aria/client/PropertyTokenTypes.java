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
   * AutocompleteToken enum type
   */
  public static enum AutocompleteToken implements AriaAttributeType {
    INLINE("inline"), LIST("list"), BOTH("both"), NONE("none");

    private final String value;

    private AutocompleteToken(String value) {
      this.value = value;
    }

    @Override
    public String getAriaValue() {
      return value;
    }
  }

  /**
   * DropeffectToken enum type
   */
  public static enum DropeffectToken implements AriaAttributeType {
    COPY("copy"), MOVE("move"), LINK("link"), EXECUTE("execute"), POPUP("popup"), NONE("none");

    private final String value;

    private DropeffectToken(String value) {
      this.value = value;
    }

    @Override
    public String getAriaValue() {
      return value;
    }
  }

  /**
   * DropeffectTokenList tokens class type
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
   * LiveToken enum type
   */
  public static enum LiveToken implements AriaAttributeType {
    OFF("off"), POLITE("polite"), ASSERTIVE("assertive");

    private final String value;

    private LiveToken(String value) {
      this.value = value;
    }

    @Override
    public String getAriaValue() {
      return value;
    }
  }

  /**
   * OrientationToken enum type
   */
  public static enum OrientationToken implements AriaAttributeType {
    HORIZONTAL("horizontal"), VERTICAL("vertical");

    private final String value;

    private OrientationToken(String value) {
      this.value = value;
    }

    @Override
    public String getAriaValue() {
      return value;
    }
  }

  /**
   * RelevantToken enum type
   */
  public static enum RelevantToken implements AriaAttributeType {
    ADDITIONS("additions"), REMOVALS("removals"), TEXT("text"), ALL("all");

    private final String value;

    private RelevantToken(String value) {
      this.value = value;
    }

    @Override
    public String getAriaValue() {
      return value;
    }
  }

  /**
   * RelevantTokenList tokens class type
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
   * SortToken enum type
   */
  public static enum SortToken implements AriaAttributeType {
    ASCENDING("ascending"), DESCENDING("descending"), NONE("none"), OTHER("other");

    private final String value;

    private SortToken(String value) {
      this.value = value;
    }

    @Override
    public String getAriaValue() {
      return value;
    }
  }
}
