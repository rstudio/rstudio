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
package com.google.gwt.editor.client;

import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.HasText;

/**
 * Adapts various interfaces that provide String values to the Editor
 * architecture.
 */
public abstract class StringEditor implements LeafValueEditor<String> {
  /**
   * Returns an editor with a <code>null</code> value.
   */
  public static StringEditor of() {
    return of((String) null);
  }

  /**
   * Returns an editor with a default value.
   */
  public static StringEditor of(final String value) {
    return new StringEditor() {
      private String v = value;

      public String getValue() {
        return v;
      }

      public void setValue(String value) {
        this.v = value;
      }
    };
  }

  /**
   * Returns an editor backed by a {@link TakesValue}, which is implemented by
   * many Widgets.
   */
  public static StringEditor of(final TakesValue<String> hasValue) {
    return new StringEditor() {
      public String getValue() {
        return hasValue.getValue();
      }

      public void setValue(String value) {
        hasValue.setValue(value);
      }
    };
  }

  /**
   * Construct a StringEditor that wraps a {@link HasText}. Several Widgets
   * implement both HasText and TakesValue&lt;String>, making an overloaded
   * method ambiguous.
   */
  public static StringEditor ofHasText(final HasText widget) {
    return new StringEditor() {
      public String getValue() {
        return widget.getText();
      }

      public void setValue(String value) {
        widget.setText(value);
      }
    };
  }

  /**
   * Prevent subclassing.
   */
  StringEditor() {
  }
}
