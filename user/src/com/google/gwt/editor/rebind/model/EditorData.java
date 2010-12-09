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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.client.CompositeEditor;
import com.google.gwt.editor.client.HasEditorDelegate;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.ValueAwareEditor;

/**
 * Describes how an Editor is related to bean properties. This type contains
 * answers to questions asked by the generator code.
 */
public class EditorData {
  /**
   * Used to construct EditorData objects.
   */
  public static class Builder {
    private EditorAccess access;
    private EditorData data = new EditorData();
    private final TreeLogger logger;
    private EditorData parent;

    public Builder(TreeLogger logger) {
      this.logger = logger;
    }

    public Builder access(EditorAccess access) throws UnableToCompleteException {
      this.access = access;
      data.declaredPath = access.getPath();
      data.editorType = access.getEditorType();
      data.editedType = EditorModel.calculateEditedType(logger, data.editorType);
      data.simpleExpression = access.getExpresson();

      TypeOracle oracle = data.editorType.getOracle();
      JClassType leafType = oracle.findType(LeafValueEditor.class.getName());
      data.isLeaf = leafType.isAssignableFrom(data.editorType);

      JClassType composedType = oracle.findType(CompositeEditor.class.getName());
      data.isCompositeEditor = composedType.isAssignableFrom(data.editorType);

      JClassType hasDelegateType = oracle.findType(HasEditorDelegate.class.getName());
      JClassType hasEditorErrorsType = oracle.findType(HasEditorErrors.class.getName());
      data.isDelegateRequired = hasDelegateType.isAssignableFrom(data.editorType)
          || hasEditorErrorsType.isAssignableFrom(data.editorType)
          || !ModelUtils.isValueType(oracle, data.editedType);

      JClassType valueAwareType = oracle.findType(ValueAwareEditor.class.getName());
      data.isValueAware = valueAwareType.isAssignableFrom(data.editorType);

      return this;
    }

    public Builder beanOwnerExpression(String value) {
      data.beanOwnerExpression = value;
      return this;
    }

    public Builder beanOwnerGuard(String value) {
      data.beanOwnerGuard = value;
      return this;
    }

    public EditorData build() throws UnableToCompleteException {
      if (data == null) {
        throw new IllegalStateException();
      }
      try {
        data.editorExpression = (parent == null ? ""
            : (parent.getExpression() + ".")) + access.getExpresson();
        data.path = (parent == null ? "" : (parent.getPath() + "."))
            + access.getPath();

        if (data.isCompositeEditor) {
          TreeLogger compositeLogger = logger.branch(TreeLogger.DEBUG,
              "Examining composite editor at " + data.path);
          JClassType subEditorType = EditorModel.calculateCompositeTypes(data.editorType)[1];
          data.composedData = new Builder(compositeLogger).access(
              EditorAccess.root(subEditorType)).parent(data).build();
        }
        return data;
      } finally {
        data = null;
      }
    }

    public Builder getterExpression(String value) {
      data.getterExpression = value;
      return this;
    }

    public Builder parent(EditorData value) {
      parent = value;
      return this;
    }

    public Builder propertyOwnerType(JClassType ownerType) {
      data.propertyOwnerType = ownerType;
      return this;
    }

    public Builder setterName(String value) {
      data.setterName = value;
      return this;
    }
  }

  private String beanOwnerExpression = "";
  private String beanOwnerGuard = "true";
  private EditorData composedData;
  private String declaredPath;
  private JClassType editedType;
  private JClassType editorType;
  private String editorExpression;
  private String getterExpression;
  private boolean isLeaf;
  private boolean isCompositeEditor;
  private boolean isDelegateRequired;
  private boolean isValueAware;
  private String path;
  private JClassType propertyOwnerType;
  private String setterName;
  private String simpleExpression;

  private EditorData() {
  }

  public String getBeanOwnerExpression() {
    return beanOwnerExpression;
  }

  public String getBeanOwnerGuard(String ownerExpression) {
    return String.format(beanOwnerGuard, ownerExpression);
  }

  public EditorData getComposedData() {
    return composedData;
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

  /**
   * Returns an expression, relative to an instance of the parent object being
   * edited, to retrieve the value to pass into the editor.
   */
  public String getGetterExpression() {
    return getterExpression;
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

  /**
   * Mainly useful for nested properties where there may not be an editor for
   * the enclosing instance (e.g. <code>person.manager.name</code>).
   */
  public JClassType getPropertyOwnerType() {
    return propertyOwnerType;
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
   * Indicates if the Editor is a {@link ComposedEditor .
   */
  public boolean isCompositeEditor() {
    return isCompositeEditor;
  }

  /**
   * Returns <code>true</code> if the editor "reaches through" an interstitial
   * property.
   */
  public boolean isDeclaredPathNested() {
    return declaredPath.contains(".");
  }

  /**
   * Returns <code>true<code> if the editor requires an EditorDelegate.
   */
  public boolean isDelegateRequired() {
    return isDelegateRequired;
  }

  /**
   * Indicates if the Editor extends {@link LeafValueEditor}.
   */
  public boolean isLeafValueEditor() {
    return isLeaf;
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