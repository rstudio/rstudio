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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Old Mozilla-specific implementation of rich-text editing.
 */
public class RichTextAreaImplOldMozilla extends RichTextAreaImplMozilla {
  /**
   * The content window cannot be focused immediately after the content window
   * has been loaded, so we need to wait for an additional deferred command.
   */
  @Override
  protected void onElementInitialized() {
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        RichTextAreaImplOldMozilla.super.onElementInitialized();
      }
    });
  }

  @Override
  protected void setFirstFocusImpl() {
    setFocusImpl(true);
  }

  @Override
  protected void setFocusImpl(boolean focused) {
    // Old Mozilla does not support blur on the content window of an iframe.
    if (focused) {
      super.setFocusImpl(focused);
    }
  }
}
