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
package com.google.gwt.editor.rebind.model;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.Editor.Path;

/**
 * Encapsulates the various ways an Editor might be accessed.
 */
class EditorAccess {
  private static final String EDITOR_SUFFIX = "Editor";

  public static EditorAccess via(JField field) {
    String path = field.getName();
    Path pathAnnotation = field.getAnnotation(Editor.Path.class);
    if (pathAnnotation != null) {
      path = pathAnnotation.value();
    } else if (path.endsWith(EDITOR_SUFFIX)) {
      path = path.substring(0, path.length() - EDITOR_SUFFIX.length());
    }

    return new EditorAccess(path, field.getType().isClassOrInterface(),
        field.getName());
  }

  public static EditorAccess via(final JMethod method) {
    String path = method.getName();
    Path pathAnnotation = method.getAnnotation(Editor.Path.class);
    if (pathAnnotation != null) {
      path = pathAnnotation.value();
    } else if (path.endsWith(EDITOR_SUFFIX)) {
      path = path.substring(0, path.length() - EDITOR_SUFFIX.length());
    }

    return new EditorAccess(path, method.getReturnType().isClassOrInterface(),
        method.getName() + "()");
  }

  private final String expression;
  private final String path;
  private final JClassType type;

  private EditorAccess(String path, JClassType type, String expression) {
    this.expression = expression;
    this.path = path;
    this.type = type;
  }

  public JClassType getEditorType() {
    return type;
  }

  public String getExpresson() {
    return expression;
  }

  public String getPath() {
    return path;
  }
}