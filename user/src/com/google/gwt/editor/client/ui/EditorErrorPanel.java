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
package com.google.gwt.editor.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * A simple decorator to display an Editor's EditorErrors in conjunction with a
 * Widget.
 * 
 * @param <T> the type of data being editod
 * @param <E> the type of Editor
 */
public class EditorErrorPanel<T, E extends Editor<T>> extends Composite
    implements HasEditorErrors<T>, IsEditor<E> {
  interface Binder extends UiBinder<Widget, EditorErrorPanel<?, ?>> {
    Binder BINDER = GWT.create(Binder.class);
  }

  @UiField
  SimplePanel contents;

  @UiField
  DivElement errorLabel;

  private E editor;

  @UiConstructor
  public EditorErrorPanel() {
    initWidget(Binder.BINDER.createAndBindUi(this));
  }

  public EditorErrorPanel(Widget widget, E editor) {
    this();
    setWidget(widget);
    setEditor(editor);
  }

  public E asEditor() {
    return editor;
  }

  public void setEditor(E editor) {
    this.editor = editor;
  }

  /**
   * Set the widget that the EditorPanel will display. If the widget is also an
   * {@link Editor} or implements {@link IsEditor}, this method will
   * automatically call {@link #setEditor}.
   */
  @UiChild(limit = 1, tagname = "contents")
  public void setWidget(Widget widget) {
    contents.add(widget);

    if (widget instanceof Editor<?>) {
      @SuppressWarnings("unchecked")
      E isEditor = (E) widget;
      setEditor(isEditor);
    } else if (widget instanceof IsEditor<?>) {
      @SuppressWarnings("unchecked")
      E isEditor = ((IsEditor<E>) widget).asEditor();
      setEditor(isEditor);
    }
  }

  /**
   * The default implementation will display, but not consume, received errors
   * whose {@link EditorError#getEditor() getEditor()} method returns the Editor
   * passed into {@link #setEditor()}.
   */
  public void showErrors(List<EditorError> errors) {
    if (errors.isEmpty()) {
      errorLabel.setInnerText("");
      errorLabel.getStyle().setDisplay(Display.NONE);
      return;
    }

    StringBuilder sb = new StringBuilder();
    for (EditorError error : errors) {
      if (error.getEditor().equals(editor)) {
        sb.append("\n").append(error.getMessage());
      }
    }
    errorLabel.setInnerText(sb.substring(1));
    errorLabel.getStyle().setDisplay(Display.INLINE_BLOCK);
  }
}
