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
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.client.CompositeEditor;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.web.bindery.autobean.gwt.rebind.model.JBeanMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Analyzes an Editor driver declaration.
 */
public class EditorModel {
  private static final EditorData[] EMPTY_EDITOR_DATA = new EditorData[0];

  /**
   * Given type assignable to <code>ComposedEditor&lt;Foo,Bar></code>, return
   * <code>{Foo, Bar}</code>. It is an error to call this method with a type not
   * assignable to {@link ComposedEditor}.
   */
  static JClassType[] calculateCompositeTypes(JClassType editorType) {
    JClassType compositeEditorIntf = editorType.getOracle().findType(
        CompositeEditor.class.getName());
    assert compositeEditorIntf.isAssignableFrom(editorType) : editorType.getQualifiedSourceName()
        + " is not a ComposedEditor";

    for (JClassType supertype : editorType.getFlattenedSupertypeHierarchy()) {
      JParameterizedType parameterized = supertype.isParameterized();
      if (parameterized != null) {
        // Found the Editor<Foo> supertype
        if (compositeEditorIntf.equals(parameterized.getBaseType())) {
          JClassType[] typeArgs = parameterized.getTypeArgs();
          assert typeArgs.length == 3;
          return new JClassType[] {typeArgs[1], typeArgs[2]};
        }
      }
    }
    assert false : "Did not find ComposedEditor parameterization for "
        + editorType.getQualifiedSourceName();
    throw new RuntimeException();
  }

  /**
   * Given type assignable to <code>Editor&lt;Foo></code>, return
   * <code>Foo</code>. It is an error to call this method with a type not
   * assignable to {@link Editor}.
   */
  static JClassType calculateEditedType(TreeLogger logger, JClassType editorType)
      throws UnableToCompleteException {
    JClassType editorIntf = editorType.getOracle().findType(
        Editor.class.getName());
    JClassType parameterization[] = ModelUtils.findParameterizationOf(
        editorIntf, editorType);
    if (parameterization != null) {
      return parameterization[0];
    }
    logger.log(TreeLogger.ERROR,
        noEditorParameterizationMessage(editorIntf, editorType));
    throw new UnableToCompleteException();
  }

  /**
   * Given type assignable to <code>IsEditor&lt;Foo, FooEditor></code>, return
   * <code>FooEditor</code>. It is an error to call this method with a type not
   * assignable to {@link IsEditor}.
   */
  static JClassType calculateIsEditedType(TreeLogger logger,
      JClassType editorType) throws UnableToCompleteException {
    JClassType editorIntf = editorType.getOracle().findType(
        IsEditor.class.getName());
    JClassType[] parameterization = ModelUtils.findParameterizationOf(
        editorIntf, editorType);
    if (parameterization != null) {
      return parameterization[0];
    }
    logger.log(TreeLogger.ERROR,
        noEditorParameterizationMessage(editorIntf, editorType));
    throw new UnableToCompleteException();
  }

  static String cycleErrorMessage(JType editorType, String originalPath,
      String errorPath) {
    return String.format(
        "Cycle detected in editor graph. Editor type %s at path %s can"
            + " be reached again at path %s",
        editorType.getQualifiedSourceName(), originalPath, errorPath);
  }

  static String foundPrimitiveMessage(JType type, String getterExpression,
      String path) {
    return String.format("Found unexpected type %s while evauating path"
        + " \"%s\" using getter expression \"%s\"",
        type.getQualifiedSourceName(), path, getterExpression);
  }

  static String mustExtendMessage(JType driverType) {
    return String.format(
        "You must declare an interface that extends the %s type",
        driverType.getSimpleSourceName());
  }

  static String noEditorParameterizationMessage(JClassType editorIntf,
      JClassType type) {
    return String.format("The type %s is assignable to the raw %s type, but"
        + " a type parameterization is required.",
        type.getParameterizedQualifiedSourceName(), editorIntf.getName());
  }

  static String noGetterMessage(String propertyName, JType proxyType) {
    return String.format(
        "Could not find a getter for path %s in proxy type %s", propertyName,
        proxyType.getQualifiedSourceName());
  }

  static String poisonedMessage() {
    return "Unable to create Editor model due to previous errors";
  }

  static String tooManyInterfacesMessage(JType intf) {
    return String.format("The type %s extends more than one interface",
        intf.getQualifiedSourceName());
  }

  static String unexpectedInputTypeMessage(JType driverType, JType intf) {
    return String.format("Unexpected input type: %s is not assignable from %s",
        driverType.getQualifiedSourceName(), intf.getQualifiedSourceName());
  }

  private final JGenericType compositeEditorIntf;

  /**
   * The structural model.
   */
  private final EditorData[] editorData;
  /**
   * A reference to {@link Editor}.
   */
  private final JGenericType editorIntf;

  private final JClassType editorType;

  private final EditorData editorSoFar;

  /**
   * A reference to {@link IsEditor}.
   */
  private final JGenericType isEditorIntf;

  private final JGenericType leafValueEditorIntf;

  private final TreeLogger logger;

  private final TypeOracle oracle;

  private final EditorModel parentModel;

  private boolean poisoned;

  private final JClassType proxyType;

  /**
   * Type-specific data.
   */
  private final Map<JClassType, List<EditorData>> typeData;

  /**
   * Constructor to use when starting with an EditorDriver interface.
   */
  public EditorModel(TreeLogger logger, JClassType intf, JClassType driverType)
      throws UnableToCompleteException {
    assert logger != null : "logger was null";
    assert intf != null : "intf was null";
    assert driverType != null : "driver was null";

    editorSoFar = null;
    this.logger = logger.branch(TreeLogger.DEBUG, "Creating Editor model for "
        + intf.getQualifiedSourceName());
    parentModel = null;
    typeData = new HashMap<JClassType, List<EditorData>>();

    if (!driverType.isAssignableFrom(intf)) {
      die(unexpectedInputTypeMessage(driverType, intf));
    } else if (intf.equals(driverType)) {
      die(mustExtendMessage(driverType));
    }

    oracle = intf.getOracle();
    editorIntf = oracle.findType(Editor.class.getName()).isGenericType();
    assert editorIntf != null : "No Editor type";
    isEditorIntf = oracle.findType(IsEditor.class.getName()).isGenericType();
    assert isEditorIntf != null : "No IsEditor type";
    compositeEditorIntf = oracle.findType(CompositeEditor.class.getName()).isGenericType();
    assert compositeEditorIntf != null : "No CompositeEditor type";
    leafValueEditorIntf = oracle.findType(LeafValueEditor.class.getName()).isGenericType();
    assert leafValueEditorIntf != null;

    JClassType[] interfaces = intf.getImplementedInterfaces();
    if (interfaces.length != 1) {
      die(tooManyInterfacesMessage(intf));
    }

    JClassType[] parameters = ModelUtils.findParameterizationOf(driverType,
        intf);
    assert parameters.length == 2 : "Unexpected number of type parameters";
    proxyType = parameters[0];
    editorType = parameters[1];
    editorData = calculateEditorData();

    if (poisoned) {
      die(poisonedMessage());
    }
  }

  private EditorModel(EditorModel parent, JClassType editorType,
      EditorData subEditor, JClassType proxyType)
      throws UnableToCompleteException {
    logger = parent.logger.branch(TreeLogger.DEBUG, "Descending into "
        + subEditor.getPath());
    this.compositeEditorIntf = parent.compositeEditorIntf;
    this.editorIntf = parent.editorIntf;
    this.editorType = editorType;
    this.editorSoFar = subEditor;
    this.isEditorIntf = parent.isEditorIntf;
    this.leafValueEditorIntf = parent.leafValueEditorIntf;
    this.oracle = parent.oracle;
    this.parentModel = parent;
    this.proxyType = proxyType;
    this.typeData = parent.typeData;

    editorData = calculateEditorData();
  }

  public EditorData[] getEditorData() {
    return editorData;
  }

  /**
   * Guaranteed to never return null.
   */
  public EditorData[] getEditorData(JClassType editor) {
    List<EditorData> toReturn = typeData.get(editor);
    if (toReturn == null) {
      return EMPTY_EDITOR_DATA;
    }
    return toReturn.toArray(new EditorData[toReturn.size()]);
  }

  public JClassType getEditorType() {
    return editorType;
  }

  public JClassType getProxyType() {
    return proxyType;
  }

  public EditorData getRootData() throws UnableToCompleteException {
    TreeLogger rootLogger = logger.branch(TreeLogger.DEBUG,
        "Calculating root data for "
            + getEditorType().getParameterizedQualifiedSourceName());
    return new EditorData.Builder(rootLogger).access(
        EditorAccess.root(getEditorType())).build();
  }

  private void accumulateEditorData(List<EditorData> data,
      List<EditorData> flatData, List<EditorData> allData)
      throws UnableToCompleteException {
    flatData.addAll(data);
    allData.addAll(data);
    for (EditorData d : data) {
      descendIntoSubEditor(allData, d);
    }
  }

  /**
   * Create the EditorData objects for the {@link #editorData} type.
   * Essentially, the point of this method is to calculate the paths of all
   * Editor types referenced by {@link #editorType}.
   */
  private EditorData[] calculateEditorData() throws UnableToCompleteException {
    List<EditorData> flatData = new ArrayList<EditorData>();
    List<EditorData> toReturn = new ArrayList<EditorData>();

    // Only look for sub-editor accessors if the editor isn't a leaf
    if (!leafValueEditorIntf.isAssignableFrom(editorType)) {
      for (JClassType type : editorType.getFlattenedSupertypeHierarchy()) {
        for (JField field : type.getFields()) {
          if (field.isPrivate() || field.isStatic()
              || field.getAnnotation(Editor.Ignore.class) != null) {
            continue;
          }
          JType fieldClassType = field.getType();
          if (shouldExamine(fieldClassType)) {
            List<EditorData> data = createEditorData(EditorAccess.via(field));
            accumulateEditorData(data, flatData, toReturn);
          }
        }
        for (JMethod method : type.getMethods()) {
          if (method.isPrivate() || method.isStatic()
              || method.getAnnotation(Editor.Ignore.class) != null) {
            continue;
          }
          JType methodReturnType = method.getReturnType();
          if (shouldExamine(methodReturnType)
              && method.getParameters().length == 0) {
            EditorAccess access = EditorAccess.via(method);
            if (access.getPath().equals("as")
                && isEditorIntf.isAssignableFrom(editorType)) {
              // Ignore IsEditor.asEditor()
              continue;
            } else if (access.getPath().equals("createEditorForTraversal")
                && compositeEditorIntf.isAssignableFrom(editorType)) {
              // Ignore CompositeEditor.createEditorForTraversal();
              continue;
            }
            List<EditorData> data = createEditorData(access);
            accumulateEditorData(data, flatData, toReturn);
          }
        }
        type = type.getSuperclass();
      }
    }

    if (compositeEditorIntf.isAssignableFrom(editorType)) {
      JClassType subEditorType = calculateCompositeTypes(editorType)[1];
      EditorAccess access = EditorAccess.root(subEditorType);
      EditorData subEditor = new EditorData.Builder(logger).access(access).parent(
          editorSoFar).build();
      List<EditorData> accumulator = new ArrayList<EditorData>();
      descendIntoSubEditor(accumulator, subEditor);

      /*
       * It's necessary to generate a sub-Model here so that any Editor types
       * reachable only through the composite type will be added to the types
       * map. The path data isn't actually useful, since we rely on
       * CompositeEditor.getPathElement() at runtime.
       */
      EditorModel subModel = new EditorModel(this, subEditor.getEditorType(),
          subEditor, subEditor.getEditedType());
      poisoned |= subModel.poisoned;
    }

    if (!typeData.containsKey(editorType)) {
      typeData.put(editorType, flatData);
    }

    return toReturn.toArray(new EditorData[toReturn.size()]);
  }

  private List<EditorData> createEditorData(EditorAccess access)
      throws UnableToCompleteException {
    TreeLogger subLogger = logger.branch(TreeLogger.DEBUG, "Examining "
        + access.toString());

    List<EditorData> toReturn = new ArrayList<EditorData>();

    // Are we looking at a view that implements IsEditor?
    if (access.isEditor()) {
      EditorAccess subAccess = EditorAccess.via(access,
          calculateIsEditedType(subLogger, access.getEditorType()));
      toReturn = createEditorData(subAccess);

      // If an object only implements IsEditor, return now
      if (!editorIntf.isAssignableFrom(access.getEditorType())) {
        return toReturn;
      }
    }

    // Determine the Foo in Editor<Foo>
    JClassType expectedToEdit = calculateEditedType(subLogger,
        access.getEditorType());

    EditorData.Builder builder = new EditorData.Builder(subLogger);
    builder.access(access);
    builder.parent(editorSoFar);

    // Find the bean methods on the proxy interface
    findBeanPropertyMethods(access.getPath(), expectedToEdit, builder);

    toReturn.add(builder.build());
    return toReturn;
  }

  /**
   * @param accumulator
   * @param data
   * @throws UnableToCompleteException
   */
  private void descendIntoSubEditor(List<EditorData> accumulator,
      EditorData data) throws UnableToCompleteException {
    EditorModel superModel = parentModel;
    while (superModel != null) {
      if (superModel.editorType.isAssignableFrom(data.getEditorType())
          || data.getEditorType().isAssignableFrom(superModel.editorType)) {
        poison(cycleErrorMessage(data.getEditorType(), superModel.getPath(),
            data.getPath()));
        return;
      }
      superModel = superModel.parentModel;
    }

    if (data.isDelegateRequired()) {
      EditorModel subModel = new EditorModel(this, data.getEditorType(), data,
          calculateEditedType(logger, data.getEditorType()));
      accumulator.addAll(accumulator.indexOf(data) + 1,
          Arrays.asList(subModel.getEditorData()));
      poisoned |= subModel.poisoned;
    }
  }

  private void die(String message) throws UnableToCompleteException {
    logger.log(TreeLogger.ERROR, message);
    throw new UnableToCompleteException();
  }

  /**
   * Traverses a path to create expressions to access the getter and setter.
   * <p>
   * This method returns a three-element string array containing the
   * interstitial getter expression specified by the path, the name of the
   * getter method, and the name of the setter method. For example, the input
   * <code>foo.bar.baz</code> might return
   * <code>{ ".getFoo().getBar()", "getBaz", "setBaz" }</code>.
   */
  private void findBeanPropertyMethods(String path, JClassType propertyType,
      EditorData.Builder builder) {
    StringBuilder interstitialGetters = new StringBuilder();
    StringBuilder interstitialGuard = new StringBuilder("true");
    String[] parts = path.split(Pattern.quote("."));
    String setterName = null;

    JClassType lookingAt = proxyType;
    part : for (int i = 0, j = parts.length; i < j; i++) {
      if (parts[i].length() == 0) {
        continue;
      }
      boolean lastPart = i == j - 1;
      boolean foundGetterForPart = false;

      for (JMethod maybeSetter : lookingAt.getInheritableMethods()) {
        JBeanMethod which = JBeanMethod.which(maybeSetter);
        if (JBeanMethod.CALL.equals(which)) {
          continue;
        }
        if (!which.inferName(maybeSetter).equals(parts[i])) {
          continue;
        }
        switch (which) {
          case GET: {
            JType returnType = maybeSetter.getReturnType();
            lookingAt = returnType.isClassOrInterface();
            if (!lastPart && lookingAt == null) {
              poison(foundPrimitiveMessage(returnType, interstitialGetters.toString(), path));
              return;
            }
            interstitialGetters.append(".").append(maybeSetter.getName()).append("()");
            interstitialGuard.append(" && %1$s").append(interstitialGetters).append(" != null");
            builder.propertyOwnerType(maybeSetter.getEnclosingType());
            foundGetterForPart = true;
            if (!lastPart) {
              continue part;
            }
            break;
          }
          case SET:
          case SET_BUILDER: {
            if (lastPart && setterName == null) {
              /*
               * If looking at the last element of the path, also look for a
               * setter.
               */

              JType setterParamType = maybeSetter.getParameters()[0].getType();
              // Handle the case of setFoo(int) vs. Editor<Integer>
              if (setterParamType.isPrimitive() != null) {
                // Replace the int with Integer
                setterParamType =
                    oracle.findType(setterParamType.isPrimitive().getQualifiedBoxedSourceName());
              }
              boolean matches = setterParamType.isClassOrInterface().isAssignableFrom(propertyType);
              if (matches) {
                setterName = maybeSetter.getName();
              }
            }
            break;
          }
        }
      }
      if (!foundGetterForPart) {
        poison(noGetterMessage(path, proxyType));
        return;
      }
    }

    int idx = interstitialGetters.lastIndexOf(".");
    builder.beanOwnerExpression(idx <= 0 ? "" : interstitialGetters.substring(0, idx));
    if (parts.length > 1) {
      // Strip after last && since null is a valid value
      interstitialGuard.delete(interstitialGuard.lastIndexOf(" &&"), interstitialGuard.length());
      builder.beanOwnerGuard(interstitialGuard.substring(8));
    }
    if (interstitialGetters.length() > 0) {
      builder.getterExpression("."
          + interstitialGetters.substring(idx + 1, interstitialGetters.length() - 2) + "()");
    } else {
      builder.getterExpression("");
    }
    builder.setterName(setterName);
  }

  private String getPath() {
    if (editorSoFar != null) {
      return editorSoFar.getPath();
    } else {
      return "<Root Object>";
    }
  }

  /**
   * Record an error that is not immediately fatal.
   */
  private void poison(String message) {
    logger.log(TreeLogger.ERROR, message);
    poisoned = true;
  }

  /**
   * Returns <code>true</code> if the given type participates in Editor
   * hierarchies.
   */
  private boolean shouldExamine(JType type) {
    JClassType classType = type.isClassOrInterface();
    if (classType == null) {
      return false;
    }
    return editorIntf.isAssignableFrom(classType)
        || isEditorIntf.isAssignableFrom(classType);
  }
}
