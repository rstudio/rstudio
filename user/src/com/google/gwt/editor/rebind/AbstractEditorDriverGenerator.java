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
import com.google.gwt.editor.client.EditorVisitor;
import com.google.gwt.editor.client.impl.AbstractEditorContext;
import com.google.gwt.editor.client.impl.RootEditorContext;
import com.google.gwt.editor.rebind.model.EditorData;
import com.google.gwt.editor.rebind.model.EditorModel;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.IdentityHashMap;
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

    String delegateSimpleName = String.format(
        "%s_%s",
        escapedMaybeParameterizedBinaryName(editor),
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
      sw.println("@Override protected %s getEditor() {return editor;}",
          editor.getQualifiedSourceName());
      sw.println(
          "protected void setEditor(%s editor) {this.editor=(%s)editor;}",
          Editor.class.getCanonicalName(), editor.getQualifiedSourceName());
      sw.println("private %s object;", edited.getQualifiedSourceName());
      sw.println("@Override public %s getObject() {return object;}",
          edited.getQualifiedSourceName());
      sw.println(
          "@Override protected void setObject(Object object) {this.object=(%s)object;}",
          edited.getQualifiedSourceName());

      if (delegateData.isCompositeEditor()) {
        sw.println("@Override protected %s createComposedDelegate() {",
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
      sw.println("@Override protected void initializeSubDelegates() {");
      sw.indent();
      if (delegateData.isCompositeEditor()) {
        sw.println(
            "createChain(%s.class);",
            delegateData.getComposedData().getEditedType().getQualifiedSourceName());
      }
      for (EditorData d : data) {
        String subDelegateType = getEditorDelegate(d);
        if (d.isDelegateRequired()) {
          sw.println("if (editor.%s != null) {", d.getSimpleExpression());
          sw.indent();
          sw.println("%s = new %s();", delegateFields.get(d), subDelegateType);
          sw.println("addSubDelegate(%s, appendPath(\"%s\"), editor.%s);",
              delegateFields.get(d), d.getDeclaredPath(),
              d.getSimpleExpression());
          sw.outdent();
          sw.println("}");
        }
      }
      sw.outdent();
      sw.println("}");

      sw.println("@Override public void accept(%s visitor) {",
          EditorVisitor.class.getCanonicalName());
      sw.indent();
      if (delegateData.isCompositeEditor()) {
        sw.println("getEditorChain().accept(visitor);");
      }
      for (EditorData d : data) {
        if (d.isDelegateRequired()) {
          sw.println("if (%s != null) ", delegateFields.get(d));
        }
        sw.println("{");
        sw.indent();
        String editorContextName = getEditorContext(delegateData, d);
        sw.println(
            "%s ctx = new %s(getObject(), editor.%s, appendPath(\"%s\"));",
            editorContextName, editorContextName, d.getSimpleExpression(),
            d.getDeclaredPath());
        if (d.isDelegateRequired()) {
          sw.println("ctx.setEditorDelegate(%s);", delegateFields.get(d));
        }
        sw.println("ctx.traverse(visitor, %s);", d.isDelegateRequired()
            ? delegateFields.get(d) : "null");
        sw.outdent();
        sw.println("}");
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

  protected void writeAdditionalContent(TreeLogger logger,
      GeneratorContext context, EditorModel model, SourceWriter sw)
      throws UnableToCompleteException {
  }

  private String escapedBinaryName(String binaryName) {
    return binaryName.replace("_", "_1").replace('$', '_').replace('.', '_');
  }

  private String escapedMaybeParameterizedBinaryName(JClassType editor) {
    /*
     * The parameterization of the editor type is included to ensure that a
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
    return escapedBinaryName(maybeParameterizedName.toString());
  }

  /**
   * Create an EditorContext implementation that will provide access to
   * {@link data} owned by {@link parent}. In other words, given the EditorData
   * for a {@code PersonEditor} and the EditorData for a {@code AddressEditor}
   * nested in the {@code PersonEditor}, create an EditorContext that will
   * describe the relationship.
   * 
   * @return the qualified name of the EditorContext implementation
   */
  private String getEditorContext(EditorData parent, EditorData data) {
    String pkg = parent.getEditorType().getPackage().getName();
    // PersonEditor_manager_name_Context
    String simpleName =
        escapedMaybeParameterizedBinaryName(parent.getEditorType())
        + "_" + data.getDeclaredPath().replace("_", "_1").replace(".", "_")
        + "_Context";

    PrintWriter pw = context.tryCreate(logger, pkg, simpleName);
    if (pw != null) {
      ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
          pkg, simpleName);
      String editedSourceName = data.getEditedType().getParameterizedQualifiedSourceName();
      f.setSuperclass(AbstractEditorContext.class.getCanonicalName() + "<"
          + editedSourceName + ">");
      SourceWriter sw = f.createSourceWriter(context, pw);

      String parentSourceName = parent.getEditedType().getQualifiedSourceName();
      sw.println("private final %s parent;", parentSourceName);

      sw.println("public %s(%s parent, %s<%s> editor, String path) {",
          simpleName, parentSourceName, Editor.class.getCanonicalName(),
          editedSourceName);
      sw.indentln("super(editor,path);");
      sw.indentln("this.parent = parent;");
      sw.println("}");

      sw.println("@Override public boolean canSetInModel() {");
      sw.indentln("return parent != null && %s && %s;",
          data.getSetterName() == null ? "false" : "true",
          data.getBeanOwnerGuard("parent"));
      sw.println("}");

      sw.println("@Override public %s checkAssignment(Object value) {",
          editedSourceName);
      sw.indentln("return (%s) value;", editedSourceName);
      sw.println("}");

      sw.println(
          "@Override public Class getEditedType() { return %s.class; }",
          data.getEditedType().getQualifiedSourceName());

      sw.println("@Override public %s getFromModel() {", editedSourceName);
      sw.indentln("return (parent != null && %s) ? parent%s%s : null;",
          data.getBeanOwnerGuard("parent"), data.getBeanOwnerExpression(),
          data.getGetterExpression());
      sw.println("}");

      sw.println("@Override public void setInModel(%s data) {",
          editedSourceName);
      if (data.getSetterName() == null) {
        sw.indentln("throw new UnsupportedOperationException();");
      } else {
        sw.indentln("parent%s.%s(data);", data.getBeanOwnerExpression(),
            data.getSetterName());
      }
      sw.println("}");

      sw.commit(logger);
    }
    return pkg + "." + simpleName;
  }

  private void writeCreateDelegate(SourceWriter sw)
      throws UnableToCompleteException {
    String editorDelegateName = getEditorDelegate(model.getRootData());

    sw.println("@Override public void accept(%s visitor) {",
        EditorVisitor.class.getCanonicalName());
    sw.indent();
    sw.println("%1$s ctx = new %1$s(getDelegate(), %2$s.class, getObject());",
        RootEditorContext.class.getCanonicalName(),
        model.getProxyType().getQualifiedSourceName());
    sw.println("ctx.traverse(visitor, getDelegate());");
    sw.outdent();
    sw.println("}");

    sw.println("@Override protected %s createDelegate() {",
        Name.getSourceNameForClass(getEditorDelegateType()),
        model.getProxyType().getQualifiedSourceName(),
        model.getEditorType().getQualifiedSourceName());
    sw.indent();
    sw.println("return new %1$s();", editorDelegateName);
    sw.outdent();
    sw.println("}");
  }
}
