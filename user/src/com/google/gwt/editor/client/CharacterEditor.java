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

/**
 * Adapts various interfaces that provide Character values to the Editor
 * architecture.
 */
public abstract class CharacterEditor implements LeafValueEditor<Character> {
  /**
   * Returns an editor with a <code>null</code> value.
   */
  public static CharacterEditor of() {
    return of((Character) null);
  }

  /**
   * Returns an editor with a default value.
   */
  public static CharacterEditor of(final Character value) {
    return new CharacterEditor() {
      private Character v = value;

      public Character getValue() {
        return v;
      }

      public void setValue(Character value) {
        this.v = value;
      }
    };
  }

  /**
   * Returns an editor backed by a {@link TakesValue}, which is implemented by
   * many Widgets.
   */
  public static CharacterEditor of(final TakesValue<Character> hasValue) {
    return new CharacterEditor() {
      public Character getValue() {
        return hasValue.getValue();
      }

      public void setValue(Character value) {
        hasValue.setValue(value);
      }
    };
  }

  /**
   * Prevent subclassing.
   */
  CharacterEditor() {
  }
}
