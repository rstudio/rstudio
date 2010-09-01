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
package com.google.gwt.requestfactory.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.requestfactory.shared.Record;

/**
 * Describes how an Editor is related to bean properties. This type contains
 * answers to questions asked by the generator code.
 */
public class EditorData {
  private final String beanOwnerExpression;
  private final String declaredPath;
  private final JClassType editedType;
  private final JClassType editorType;
  private final String editorExpression;
  private final String getterName;
  private final boolean isLeaf;
  private final boolean isProxyEditor;
  private final boolean isValueAware;
  private final String path;
  private final String setterName;
  private final String simpleExpression;

  EditorData(EditorData parent, EditorAccess access,
      String beanOwnerExpression, String getterName, String setterName) {
    this.beanOwnerExpression = beanOwnerExpression;
    this.getterName = getterName;
    this.setterName = setterName;

    editorType = access.getEditorType();
    editedType = EditorModel.calculateEditedType(editorType);

    editorExpression = (parent == null ? "" : (parent.getExpression() + "."))
        + access.getExpresson();
    simpleExpression = access.getExpresson();

    declaredPath = access.getPath();
    path = (parent == null ? "" : (parent.getPath() + ".")) + access.getPath();

    TypeOracle oracle = editorType.getOracle();
    JClassType leafType = oracle.findType(LeafValueEditor.class.getName());
    isLeaf = leafType.isAssignableFrom(editorType);

    JClassType proxyType = oracle.findType(Record.class.getName());
    isProxyEditor = proxyType.isAssignableFrom(editedType);

    JClassType valueAwareType = oracle.findType(ValueAwareEditor.class.getName());
    isValueAware = valueAwareType.isAssignableFrom(editorType);
  }

  public String getBeanOwnerExpression() {
    return beanOwnerExpression;
  }

  /**
   * Gets the path specified by the {@code @Path} annotation or inferred via
   * convention.
   */
  public String getDeclaredPath() {
    return declaredPath;
  }

  public JClassType getEditedType() {
    return editedType;
  }

  public JClassType getEditorType() {
    return editorType;
  }

  /**
   * Returns a complete expression to retrieve the editor.
   */
  public String getExpression() {
    return editorExpression;
  }

  public String getGetterName() {
    return getterName;
  }

  /**
   * Returns the complete path of the editor, relative to the root object.
   */
  public String getPath() {
    return path;
  }

  public String getPropertyName() {
    return getPath().substring(getPath().lastIndexOf('.') + 1);
  }

  public String getSetterName() {
    return setterName;
  }

  /**
   * Returns an expression relative to the enclosing Editor to retrieve the
   * editor.
   */
  public String getSimpleExpression() {
    return simpleExpression;
  }

  /**
   * Returns <code>true</code> if the editor "reaches through" an interstitial
   * property.
   */
  public boolean isDeclaredPathNested() {
    return declaredPath.contains(".");
  }

  /**
   * Indicates if the Editor extends {@link LeafValueEditor}.
   */
  public boolean isLeafValueEditor() {
    return isLeaf;
  }

  /**
   * Indicates if the Editor accepts a RequestFactory Proxy type.
   */
  public boolean isProxyEditor() {
    return isProxyEditor;
  }

  /**
   * Indicates if the Editor extends {@link ValueAwareEditor}.
   */
  public boolean isValueAwareEditor() {
    return isValueAware;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return getPath() + " = " + getEditorType();
  }
}