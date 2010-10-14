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
package com.google.gwt.editor.ui.client.adapters;

import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.user.client.ui.HasText;

/**
 * Adapts the HasText interface to the Editor framework.
 */
public class HasTextEditor implements LeafValueEditor<String> {
  /**
   * Returns a new ValueEditor that that modifies the given {@link HasText} peer
   * instance.
   * 
   * @param peer a {@link HasText} instance
   * @return a HasTextEditor instance
   */
  public static HasTextEditor of(HasText peer) {
    return new HasTextEditor(peer);
  }

  private HasText peer;

  /**
   * Constructs a new HasTextEditor that that modifies the given {@link HasText}
   * peer instance.
   * 
   * @param peer a {@link HasText} instance
   */
  protected HasTextEditor(HasText peer) {
    this.peer = peer;
  }

  public String getValue() {
    return peer.getText();
  }

  public void setValue(String value) {
    peer.setText(value);
  }
}
