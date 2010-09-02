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
package com.google.gwt.editor.client.adapters;

import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.user.client.TakesValue;

/**
 * Adapts various interfaces that provide Byte values to the Editor
 * architecture.
 */
public abstract class ByteEditor implements LeafValueEditor<Byte> {
  /**
   * Returns an editor with a <code>null</code> value.
   */
  public static ByteEditor of() {
    return of((Byte) null);
  }

  /**
   * Returns an editor with a default value.
   */
  public static ByteEditor of(final Byte value) {
    return new ByteEditor() {
      private Byte v = value;

      public Byte getValue() {
        return v;
      }

      public void setValue(Byte value) {
        this.v = value;
      }
    };
  }

  /**
   * Returns an editor backed by a {@link TakesValue}, which is implemented by
   * many Widgets.
   */
  public static ByteEditor of(final TakesValue<Byte> hasValue) {
    return new ByteEditor() {
      public Byte getValue() {
        return hasValue.getValue();
      }

      public void setValue(Byte value) {
        hasValue.setValue(value);
      }
    };
  }

  /**
   * Prevent subclassing.
   */
  ByteEditor() {
  }
}
