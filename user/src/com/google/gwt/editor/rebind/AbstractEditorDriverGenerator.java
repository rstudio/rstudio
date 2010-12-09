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
package com.google.gwt.editor.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.impl.DelegateMap;
import com.google.gwt.editor.rebind.model.EditorData;
import com.google.gwt.editor.rebind.model.EditorModel;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A base class for generating Editor drivers.
 */
public abstract class AbstractEditorDriverGenerator extends Generator {

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

    model = new EditorModel(logger, toGenerate,
        oracle.findType(getDriverInterfaceType().getName()));

    ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
        packageName, simpleSourceName);
    factory.setSuperclass(Name.getSourceNameForClass(getDriverSuperclassType())
        + "<" + model.getProxyType().getParameterizedQualifiedSourceName()
        + ", " + model.getEditorType().getParameterizedQualifiedSourceName()
        + ">");
    factory.addImplementedInterface(typeName);
    SourceWriter sw = factory.createSourceWriter(context, pw);
    writeCreateDelegate(sw);
    writeAdditionalContent(logger, context, model, sw);
    sw.commit(logger);

    return factory.getCreatedClassName();
  }

  protected abstract Class<?> getDriverInterfaceType();

  protected abstract Class<?> getDriverSuperclassType();

  protected String getEditorDelegate(EditorData delegateData) {
    JClassType edited = delegateData.getEditedType();
    JClassType editor = delegateData.getEditorType();
    Map<EditorData, String> delegateFields = new IdentityHashMap<EditorData, String>();
    NameFactory nameFactory = new NameFactory();

    /*
     * The paramaterization of the editor type is included to ensure that a
     * correct specialization of a CompositeEditor will be generated. For
     * example, a ListEditor<Person, APersonEditor> would need a different
     * delegate from a ListEditor<Person, AnotherPersonEditor>.
     */
    StringBuilder maybeParameterizedName = new StringBuilder(
        BinaryName.getClassName(editor.getQualifiedBinaryName()));
    if (editor.isParameterized() != null) {
      for (JClassType type : editor.isParameterized().getTypeArgs()) {
        maybeParameterizedName.append("$").append(type.getQualifiedBinaryName());
      }
    }
    String delegateSimpleName = String.format(
        "%s_%s",
        escapedBinaryName(maybeParameterizedName.toString()),
        BinaryName.getShortClassName(Name.getBinaryNameForClass(getEditorDelegateType())));

    String packageName = editor.getPackage().getName();
    PrintWriter pw = context.tryCreate(logger, packageName, delegateSimpleName);
    if (pw != null) {
      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          packageName, delegateSimpleName);
      factory.setSuperclass(String.format("%s",
          Name.getSourceNameForClass(getEditorDelegateType())));
      SourceWriter sw = factory.createSourceWriter(context, pw);

      EditorData[] data = model.getEditorData(editor);

      /*
       * Declare fields in the generated subclass for the editor and the object
       * being edited. This decreases casting over having a generic field in the
       * supertype.
       */
      sw.println("private %s editor;", editor.getQualifiedSourceName());
      sw.println("protected %s getEditor() {return editor;}",
          editor.getQualifiedSourceName());
      sw.println(
          "protected void setEditor(%s editor) {this.editor=(%s)editor;}",
          Editor.class.getCanonicalName(), editor.getQualifiedSourceName());
      sw.println("private %s object;", edited.getQualifiedSourceName());
      sw.println("public %s getObject() {return object;}",
          edited.getQualifiedSourceName());
      sw.println(
          "protected void setObject(Object object) {this.object=(%s)object;}",
          edited.getQualifiedSourceName());

      if (delegateData.isCompositeEditor()) {
        sw.println("protected %s createComposedDelegate() {",
            Name.getSourceNameForClass(this.getEditorDelegateType()));
        sw.indentln("return new %s();",
            getEditorDelegate(delegateData.getComposedData()));
        sw.println("}");
      }

      // Fields for the sub-delegates that must be managed
      for (EditorData d : data) {
        if (d.isDelegateRequired()) {
          String fieldName = nameFactory.createName(d.getPropertyName()
              + "Delegate");
          delegateFields.put(d, fieldName);
          sw.println("%s %s;",
              Name.getSourceNameForClass(getEditorDelegateType()), fieldName);
        }
      }

      // For each entity property, create a sub-delegate and initialize
      sw.println("protected void attachSubEditors(%s delegateMap) {",
          DelegateMap.class.getCanonicalName());
      sw.indent();
      for (EditorData d : data) {
        String subDelegateType = getEditorDelegate(d);
        sw.println("if (editor.%s != null) {", d.getSimpleExpression());
        sw.indent();
        if (d.isDelegateRequired()) {
          sw.println("%s = new %s();", delegateFields.get(d), subDelegateType);
          writeDelegateInitialization(sw, d, delegateFields);
          sw.println("delegateMap.put(%1$s.getObject(), %1$s);",
              delegateFields.get(d));
        } else if (d.isLeafValueEditor()) {
          // if (can().access().without().npe()) { editor.subEditor.setValue() }
          sw.println("if (%4$s) editor.%1$s.setValue(getObject()%2$s%3$s);",
              d.getSimpleExpression(), d.getBeanOwnerExpression(),
              d.getGetterExpression(), d.getBeanOwnerGuard("getObject()"));
          // simpleEditor.put("some.path", editor.simpleEditor());
          sw.println("simpleEditors.put(\"%s\", editor.%s);",
              d.getDeclaredPath(), d.getSimpleExpression());
        }
        sw.outdent();
        sw.println("}");
      }
      sw.outdent();
      sw.println("}");

      // Flush each sub-delegate
      sw.println("protected void flushSubEditors(%s errorAccumulator) {",
          List.class.getCanonicalName());
      sw.indent();
      for (EditorData d : data) {
        String mutableObjectExpression;
        if (d.getBeanOwnerExpression().length() > 0) {
          mutableObjectExpression = mutableObjectExpression(d,
              String.format("(getObject()%s)", d.getBeanOwnerExpression()));
        } else {
          mutableObjectExpression = "getObject()";
        }

        if (d.getSetterName() != null && d.isLeafValueEditor()) {
          // if (editor.subEditor != null && can().access()) {
          sw.println("if (editor.%s != null && %s) {", d.getSimpleExpression(),
              d.getBeanOwnerGuard("getObject()"));
          sw.indent();
          if (d.isDelegateRequired()) {
            sw.println("%s.flush(errorAccumulator);", delegateFields.get(d));
            // mutableObject.setFoo((cast)fooDelegate.getValue());
            sw.println("%s.%s((%s)%s.getObject());", mutableObjectExpression,
                d.getSetterName(), d.getEditedType().getQualifiedSourceName(),
                delegateFields.get(d));
          } else {
            // mutableObject.setFoo(editor.subEditor.getValue());
            sw.println("%s.%s(editor.%s.getValue());", mutableObjectExpression,
                d.getSetterName(), d.getSimpleExpression());
          }
          sw.outdent();
          sw.println("}");
        } else if (d.isDelegateRequired()) {
          // if (fooDelegate != null && can().reach().without().npe()) {
          sw.println("if (%s != null && %s) {", delegateFields.get(d),
              d.getBeanOwnerGuard("getObject()"));
          sw.indent();
          // fooDelegate.flush(errorAccumulator);
          sw.println("%s.flush(errorAccumulator);", delegateFields.get(d));

          sw.outdent();
          sw.println("}");
        }
      }
      sw.outdent();
      sw.println("}");

      // Flush each sub-delegate
      sw.println("protected void flushSubEditorErrors(%s errorAccumulator) {",
          List.class.getCanonicalName());
      sw.indent();
      for (EditorData d : data) {
        if (d.isDelegateRequired()) {
          // if (fooDelegate != null) fooDelegate.flushErrors(accumulator);
          sw.println("if (%1$s != null) %1$s.flushErrors(errorAccumulator);",
              delegateFields.get(d));
        }
      }
      sw.outdent();
      sw.println("}");

      // Reset the data being displayed
      sw.println("protected void refreshEditors() {",
          DelegateMap.class.getCanonicalName());
      sw.indent();
      for (EditorData d : data) {
        if (d.isDelegateRequired()) {
          // if (subEditorDelegate != null) {
          sw.println("if (%s != null) {", delegateFields.get(d));
          sw.indent();
          // if (can().access().without().npe()) {
          sw.println("if (%s) {", d.getBeanOwnerGuard("getObject()"));
          // subDelegate.refresh(getObject().getFoo().getBar());
          sw.indentln("%s.refresh(getObject()%s%s);", delegateFields.get(d),
              d.getBeanOwnerExpression(), d.getGetterExpression());
          // } else { subDelegate.refresh(null); }
          sw.println("} else { %s.refresh(null); }", delegateFields.get(d));
          sw.outdent();
          sw.println("}");
        } else if (d.isLeafValueEditor()) {
          // if (editor.subEditor != null) {
          sw.println("if (editor.%s != null) {", d.getSimpleExpression());
          sw.indent();
          // if (can().access().without().npe()) { editor.subEditor.setValue() }
          sw.println("if (%4$s) editor.%1$s.setValue(getObject()%2$s%3$s);",
              d.getSimpleExpression(), d.getBeanOwnerExpression(),
              d.getGetterExpression(), d.getBeanOwnerGuard("getObject()"));
          // else { editor.subEditor.setValue(null); }
          sw.println("else { editor.%s.setValue(null); }",
              d.getSimpleExpression());
          sw.outdent();
          sw.println("}");
        }
      }
      sw.outdent();
      sw.println("}");

      // Write instance delegate to static implementation
      sw.println("protected void traverse(%s paths) {",
          List.class.getCanonicalName());
      sw.indentln("traverseEditor(getEditor(), \"\", paths);");
      sw.println("}");

      sw.println("public static void traverseEditor(%s editor,"
          + " String prefix, %s<String> paths) {",
          editor.getQualifiedSourceName(), List.class.getName());
      sw.indent();
      for (EditorData d : data) {
        if (d.isDelegateRequired() || d.isDeclaredPathNested()
            || d.isCompositeEditor()) {
          // if (editor.subEditor != null) {
          sw.println("if (editor.%s != null) {", d.getSimpleExpression());
          sw.indent();
          // String localPath = appendPath(prefix, "somePath");
          sw.println("String localPath = appendPath(prefix, \"%s\");",
              d.getDeclaredPath());
          sw.println("paths.add(localPath);");

          if (d.isDelegateRequired()) {
            // fooDelegate.traverseEditor(editor.subEditor, localPath, paths);
            sw.println("%s.traverseEditor(editor.%s, localPath, paths);",
                getEditorDelegate(d), d.getSimpleExpression());
          }
          if (d.isCompositeEditor()) {
            /*
             * composedDelegate.traverseEditor(editor.subEditor.
             * createEditorForTraversal(), localPath, paths);
             */
            sw.println(
                "%s.traverseEditor(editor.%s.createEditorForTraversal(), localPath, paths);",
                getEditorDelegate(d.getComposedData()), d.getSimpleExpression());
          }
          sw.outdent();
          sw.println("}");
        }
      }
      sw.outdent();
      sw.println("}");

      sw.commit(logger);
    }
    return packageName + "." + delegateSimpleName;
  }

  protected abstract Class<?> getEditorDelegateType();

  protected abstract String mutableObjectExpression(EditorData data,
      String sourceObjectExpression);

  /**
   * @param logger
   * @param context
   * @param model
   * @param sw
   * 
   * @throws UnableToCompleteException
   */
  protected void writeAdditionalContent(TreeLogger logger,
      GeneratorContext context, EditorModel model, SourceWriter sw)
      throws UnableToCompleteException {
  }

  protected abstract void writeDelegateInitialization(SourceWriter sw,
      EditorData d, Map<EditorData, String> delegateFields);

  private String escapedBinaryName(String binaryName) {
    return binaryName.replace("_", "_1").replace('$', '_').replace('.', '_');
  }

  private void writeCreateDelegate(SourceWriter sw)
      throws UnableToCompleteException {
    String editorDelegateName = getEditorDelegate(model.getRootData());

    sw.println("protected %s createDelegate() {",
        Name.getSourceNameForClass(getEditorDelegateType()),
        model.getProxyType().getQualifiedSourceName(),
        model.getEditorType().getQualifiedSourceName());
    sw.indent();
    sw.println("return new %1$s();", editorDelegateName);
    sw.outdent();
    sw.println("}");
  }
}
