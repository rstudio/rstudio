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

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.requestfactory.client.impl.AbstractRequestFactoryEditorDriver;
import com.google.gwt.requestfactory.client.impl.RequestFactoryEditorDelegate;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.List;

/**
 * Generates implementations of RFEDs.
 */
public class RequestFactoryEditorDriverGenerator extends Generator {

  private GeneratorContext context;
  private TreeLogger logger;
  private EditorModel model;

  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    this.context = context;
    this.logger = logger;

    TypeOracle oracle = context.getTypeOracle();
    JClassType toGenerate = oracle.findType(typeName).isInterface();
    if (toGenerate == null) {
      logger.log(TreeLogger.ERROR, typeName + " is not an interface type");
      throw new UnableToCompleteException();
    }

    String packageName = toGenerate.getPackage().getName();
    String simpleSourceName = toGenerate.getName().replace('.', '_') + "Impl";
    PrintWriter pw = context.tryCreate(logger, packageName, simpleSourceName);
    if (pw == null) {
      return packageName + "." + simpleSourceName;
    }

    model = new EditorModel(logger, toGenerate);

    ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
        packageName, simpleSourceName);
    factory.setSuperclass(AbstractRequestFactoryEditorDriver.class.getName()
        + "<" + model.getProxyType().getQualifiedSourceName() + ", "
        + model.getEditorType().getQualifiedSourceName() + ">");
    factory.addImplementedInterface(typeName);
    SourceWriter sw = factory.createSourceWriter(context, pw);
    writeCreateDelegate(sw);
    writeTraverseEditors(sw);
    sw.commit(logger);

    return factory.getCreatedClassName();
  }

  /**
   * TODO: Move this code-gen into a separate generator if model data can be
   * shared.
   */
  private String getEditorDelegate(JClassType proxy, JClassType editor) {
    String delegateSimpleName = BinaryName.getClassName(
        editor.getQualifiedBinaryName()).replace("_", "_1").replace('$', '_')
        + "_EditorDelegate";

    String packageName = editor.getPackage().getName();
    PrintWriter pw = context.tryCreate(logger, packageName, delegateSimpleName);
    if (pw != null) {
      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          packageName, delegateSimpleName);
      factory.setSuperclass(String.format("%s<%s, %s>",
          RequestFactoryEditorDelegate.class.getName(),
          proxy.getQualifiedSourceName(), editor.getQualifiedSourceName()));
      SourceWriter sw = factory.createSourceWriter(context, pw);

      EditorData[] data = model.getEditorData(editor);

      /*
       * Declare fields in the generated subclass for the editor and the object
       * being edited. This decreases casting over having a generic field in the
       * supertype.
       */
      sw.println("private %s editor;", editor.getQualifiedSourceName());
      sw.println("protected void setEditor(%s editor) {this.editor=editor;}",
          editor.getQualifiedSourceName());
      sw.println("private %s object;", proxy.getQualifiedSourceName());
      sw.println("protected %s getObject() {return object;}",
          proxy.getQualifiedSourceName());
      sw.println("protected void setObject(%s object) {this.object=object;}",
          proxy.getQualifiedSourceName());

      // Fields for the sub-delegates that must be managed
      for (EditorData d : data) {
        if (d.isProxyEditor()) {
          sw.println("%s<%s, %s> %sDelegate;",
              RequestFactoryEditorDelegate.class.getName(),
              d.getEditedType().getQualifiedBinaryName(),
              d.getEditorType().getQualifiedSourceName(), d.getPropertyName());
        }
      }

      // For each entity property, create a sub-delegate and initialize
      sw.println("protected void attachSubEditors() {");
      sw.indent();
      for (EditorData d : data) {
        if (d.isProxyEditor()) {
          String subEditorType = getEditorDelegate(d.getEditedType(),
              d.getEditorType());
          sw.println("if (editor.%s != null) {", d.getSimpleExpression());
          sw.indent();
          sw.println("%sDelegate = new %s();", d.getPropertyName(),
              subEditorType);
          sw.println("%1$sDelegate.initialize(eventBus, factory, "
              + "appendPath(\"%1$s\"), getObject()%2$s.%3$s(),"
              + " editor.%4$s, request);", d.getPropertyName(),
              d.getBeanOwnerExpression(), d.getGetterName(),
              d.getSimpleExpression());
          sw.outdent();
          sw.println("}");
        }
      }
      sw.outdent();
      sw.println("}");

      // Flush each sub-delegate
      sw.println("protected void flushSubEditors() {");
      sw.indent();
      for (EditorData d : data) {
        if (d.isProxyEditor()) {
          sw.println("if (%1$sDelegate != null) %1$sDelegate.flush();",
              d.getPropertyName());
        }
      }
      sw.outdent();
      sw.println("}");

      // Copy value properties back into the object
      sw.println("protected void flushValues() {");
      sw.indent();
      for (EditorData d : data) {
        if (d.isLeafValueEditor() && d.getSetterName() != null) {
          sw.println("if (editor.%1$s != null)"
              + " request.edit(getObject()%2$s).%3$s(editor.%1$s.getValue());",
              d.getSimpleExpression(), d.getBeanOwnerExpression(),
              d.getSetterName());
        }
      }
      sw.outdent();
      sw.println("}");

      sw.println("public static void traverseEditor(%s editor,"
          + " String prefix, %s<String> paths) {",
          editor.getQualifiedSourceName(), List.class.getName());
      sw.indent();
      for (EditorData d : data) {
        if (d.isProxyEditor() || d.isDeclaredPathNested()) {
          sw.println("if (editor.%s != null) {", d.getSimpleExpression());
          sw.indent();
          sw.println("String localPath = appendPath(prefix, \"%s\");",
              d.getDeclaredPath());
          sw.println("paths.add(localPath);");
          if (d.isProxyEditor()) {
            sw.println("%s.traverseEditor(editor.%s, localPath, paths);",
                getEditorDelegate(d.getEditedType(), d.getEditorType()),
                d.getSimpleExpression());
          }
          sw.outdent();
          sw.println("}");
        }
      }
      sw.outdent();
      sw.println("}");

      // Copy values from the object into editors
      sw.println("protected void pushValues() {");
      sw.indent();
      for (EditorData d : data) {
        if (d.isLeafValueEditor()) {
          sw.println("if (editor.%1$s != null)"
              + " editor.%1$s.setValue(getObject()%2$s.%3$s());",
              d.getSimpleExpression(), d.getBeanOwnerExpression(),
              d.getGetterName());
        }
      }
      sw.outdent();
      sw.println("}");

      sw.commit(logger);
    }
    return packageName + "." + delegateSimpleName;
  }

  private void writeCreateDelegate(SourceWriter sw) {
    String editorDelegateName = getEditorDelegate(model.getProxyType(),
        model.getEditorType());

    sw.println("protected %s<%s,%s> createDelegate() {",
        RequestFactoryEditorDelegate.class.getName(),
        model.getProxyType().getQualifiedSourceName(),
        model.getEditorType().getQualifiedSourceName());
    sw.indent();
    sw.println("return new %1$s();", editorDelegateName);
    sw.outdent();
    sw.println("}");
  }

  private void writeTraverseEditors(SourceWriter sw) {
    sw.println("protected void traverseEditors(%s<String> paths) {",
        List.class.getName());
    sw.println("  %s.traverseEditor(getEditor(), \"\", paths);",
        getEditorDelegate(model.getProxyType(), model.getEditorType()));
    sw.println("}");
  }
}
