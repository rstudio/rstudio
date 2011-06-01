/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.sample.mobilewebapp.client.ui;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.editor.client.IsEditor;

import java.util.List;

/**
 * A simple editor that connects an {@link Editor} with an element to display
 * editor errors.
 * 
 * @param <T> the type of data being edited
 */
public class EditorDecorator<T> implements HasEditorErrors<T>, IsEditor<Editor<T>> {

  /**
   * Create a new {@link EditorDecorator} using the specified editor and error
   * label.
   * 
   * @param editor the editor to decorate
   * @param errorLabel the label that displays errors
   */
  public static <T> EditorDecorator<T> create(Editor<T> editor, Element errorLabel) {
    return new EditorDecorator<T>(editor, errorLabel);
  }

  private final Editor<T> editor;
  private final Element errorLabel;

  /**
   * Construct a new {@link EditorDecorator}.
   * 
   * @param editor the editor to decorate
   * @param errorLabel the label that displays errors
   */
  public EditorDecorator(Editor<T> editor, Element errorLabel) {
    this.editor = editor;
    this.errorLabel = errorLabel;
  }

  public Editor<T> asEditor() {
    return editor;
  }

  public void showErrors(List<EditorError> errors) {
    StringBuilder sb = new StringBuilder();
    for (EditorError error : errors) {
      Editor<?> errorEditor = error.getEditor();
      if (this.equals(errorEditor) || editor.equals(errorEditor)) {
        sb.append("\n").append(error.getMessage());
      }
    }

    if (sb.length() == 0) {
      errorLabel.setInnerText("");
      errorLabel.getStyle().setDisplay(Display.NONE);
      return;
    }

    errorLabel.setInnerText(sb.substring(1));
    errorLabel.getStyle().setDisplay(Display.INLINE_BLOCK);
  }
}
