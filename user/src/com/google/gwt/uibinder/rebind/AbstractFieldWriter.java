/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.rebind.model.OwnerField;
import com.google.gwt.user.client.ui.RenderablePanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Most of the implementation of {@link FieldWriter}. Subclasses are responsible
 * for {@link FieldWriter#getQualifiedSourceName()} and
 * {@link FieldWriter#getInstantiableType()}.
 */
abstract class AbstractFieldWriter implements FieldWriter {
  private static final String NO_DEFAULT_CTOR_ERROR =
      "%1$s has no default (zero args) constructor. To fix this, you can define"
      + " a @UiFactory method on the UiBinder's owner, or annotate a constructor of %2$s with"
      + " @UiConstructor.";

  private static int nextAttachVar;

  public static String getNextAttachVar() {
    return "attachRecord" + nextAttachVar++;
  }

  private final FieldManager manager;
  private final Set<FieldWriter> needs = new LinkedHashSet<FieldWriter>();
  private final List<String> statements = new ArrayList<String>();
  private final List<String> attachStatements = new ArrayList<String>();
  private final List<String> detachStatements = new ArrayList<String>();

  private final String name;
  private String initializer;
  private boolean written;
  private int buildPrecedence;
  private final MortalLogger logger;
  private final FieldWriterType fieldType;
  private String html;

  public AbstractFieldWriter(FieldManager manager, FieldWriterType fieldType,
      String name, MortalLogger logger) {
    if (name == null) {
      throw new RuntimeException("name cannot be null");
    }
    this.manager = manager;
    this.name = name;
    this.logger = logger;
    this.buildPrecedence = 1;
    this.fieldType = fieldType;
  }

  @Override
  public void addAttachStatement(String format, Object... args) {
    attachStatements.add(String.format(format, args));
  }

  @Override
  public void addDetachStatement(String format, Object... args) {
    detachStatements.add(String.format(format, args));
  }

  @Override
  public void addStatement(String format, Object... args) {
    statements.add(String.format(format, args));
  }

  @Override
  public int getBuildPrecedence() {
    return buildPrecedence;
  }

  @Override
  public FieldWriterType getFieldType() {
    return fieldType;
  }

  public String getHtml() {
    return html + ".asString()";
  }

  public String getInitializer() {
    return initializer;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getNextReference() {
    return manager.convertFieldToGetter(name);
  }

  public JType getReturnType(String[] path, MonitoredLogger logger) {
    if (!name.equals(path[0])) {
      throw new RuntimeException(this
          + " asked to evaluate another field's path: " + path[0]);
    }

    List<String> pathList = Arrays.asList(path).subList(1, path.length);
    return getReturnType(getAssignableType(), pathList, logger);
  }

  public String getSafeHtml() {
    return html;
  }

  public void needs(FieldWriter f) {
    needs.add(f);
  }

  @Override
  public void setBuildPrecedence(int precedence) {
    this.buildPrecedence = precedence;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public void setInitializer(String initializer) {
    this.initializer = initializer;
  }

  @Override
  public String toString() {
    return String.format("[%s %s = %s]", this.getClass().getName(), name,
        initializer);
  }

  public void write(IndentedWriter w) throws UnableToCompleteException {
    if (written) {
      return;
    }

    for (FieldWriter f : needs) {
      f.write(w);
    }

    if (initializer == null) {
      JClassType type = getInstantiableType();
      if (type != null) {
        if ((type.isInterface() == null)
            && (type.findConstructor(new JType[0]) == null)) {
          logger.die(NO_DEFAULT_CTOR_ERROR, type.getQualifiedSourceName(),
              type.getName());
        }
      }
    }

    if (null == initializer) {
      initializer = String.format("(%1$s) GWT.create(%1$s.class)",
          getQualifiedSourceName());
    }

    w.write("%s %s = %s;", getQualifiedSourceName(), name, initializer);

    this.written = true;
  }

  @Override
  public void writeFieldBuilder(IndentedWriter w, int getterCount,
    OwnerField ownerField) {
    if (getterCount > 1) {
      w.write("%s;  // more than one getter call detected. Type: %s, precedence: %s",
            FieldManager.getFieldBuilder(name), getFieldType(), getBuildPrecedence());
      return;
    }

    if (getterCount == 0 && ownerField != null) {
      w.write("%s;  // no getter call detected but must bind to ui:field. "
          + "Type: %s, precedence: %s", FieldManager.getFieldBuilder(name),
          getFieldType(), getBuildPrecedence());
    }
  }

  @Override
  public void writeFieldDefinition(IndentedWriter w, TypeOracle typeOracle,
      OwnerField ownerField, DesignTimeUtils designTime, int getterCount,
      boolean useLazyWidgetBuilders)
      throws UnableToCompleteException {

    JClassType renderablePanelType = typeOracle.findType(
        RenderablePanel.class.getName());
    boolean outputAttachDetachCallbacks = useLazyWidgetBuilders
        && getAssignableType() != null
        && getAssignableType().isAssignableTo(renderablePanelType);

    // Check initializer.
    if (initializer == null) {
      if (ownerField != null && ownerField.isProvided()) {
        initializer = String.format("owner.%s", name);
      } else {
        JClassType type = getInstantiableType();
        if (type != null) {
          if ((type.isInterface() == null)
              && (type.findConstructor(new JType[0]) == null)) {
            logger.die(NO_DEFAULT_CTOR_ERROR, type.getQualifiedSourceName(),
                type.getName());
          }
        }
        initializer = String.format("(%1$s) GWT.create(%1$s.class)",
            getQualifiedSourceName());
      }
    }

    w.newline();
    w.write("/**");
    w.write(" * Getter for %s called %s times. Type: %s. Build precedence: %s.",
        name, getterCount, getFieldType(), getBuildPrecedence());
    w.write(" */");
    if (getterCount > 1) {
      w.write("private %1$s %2$s;", getQualifiedSourceName(), name);
    }

    w.write("private %s %s {", getQualifiedSourceName(), FieldManager.getFieldGetter(name));
    w.indent();
    w.write("return %s;", (getterCount > 1) ? name : FieldManager.getFieldBuilder(name));
    w.outdent();
    w.write("}");

    w.write("private %s %s {", getQualifiedSourceName(), FieldManager.getFieldBuilder(name));
    w.indent();

    w.write("// Creation section.");
    if (getterCount > 1) {
      w.write("%s = %s;", name, initializer);
    } else {
      w.write("final %s %s = %s;", getQualifiedSourceName(), name, initializer);
    }
    if (ownerField != null && ownerField.isProvided() && !designTime.isDesignTime()) {
      w.write("assert %1$s != null : \"UiField %1$s with 'provided = true' was null\";", name);
    }

    w.write("// Setup section.");
    for (String s : statements) {
      w.write(s);
    }

    String attachedVar = null;

    if (attachStatements.size() > 0) {
      w.newline();
      w.write("// Attach section.");
      if (outputAttachDetachCallbacks) {
        // TODO(rdcastro): This is too coupled with RenderablePanel.
        // Make this nicer.
        w.write("%s.wrapInitializationCallback = ", getName());
        w.indent();
        w.indent();
        w.write(
            "new com.google.gwt.user.client.Command() {");
        w.outdent();
        w.write("@Override public void execute() {");
        w.indent();
      } else {
        attachedVar = getNextAttachVar();

        JClassType elementType = typeOracle.findType(Element.class.getName());

        String elementToAttach = getInstantiableType().isAssignableTo(elementType)
            ? name : name + ".getElement()";

        w.write("UiBinderUtil.TempAttachment %s = UiBinderUtil.attachToDom(%s);",
                attachedVar, elementToAttach);
      }

      for (String s : attachStatements) {
        w.write(s);
      }

      if (outputAttachDetachCallbacks) {
        w.outdent();
        w.write("}");
        w.outdent();
        w.write("};");
      }
    }

    w.newline();
    // If we forced an attach, we should always detach, regardless of whether
    // there are any detach statements.
    if (attachedVar != null) {
      w.write("// Detach section.");
      w.write("%s.detach();", attachedVar);
    }

    if (detachStatements.size() > 0) {
      if (outputAttachDetachCallbacks) {
        w.write("%s.detachedInitializationCallback = ", getName());
        w.indent();
        w.indent();
        w.write("new com.google.gwt.user.client.Command() {");
        w.outdent();
        w.write("@Override public void execute() {");
        w.indent();
      }

      for (String s : detachStatements) {
        w.write(s);
      }

      if (outputAttachDetachCallbacks) {
        w.outdent();
        w.write("}");
        w.outdent();
        w.write("};");
      }
    }

    if ((ownerField != null) && !ownerField.isProvided()) {
      w.newline();
      w.write("owner.%1$s = %1$s;", name);
    }

    w.newline();
    w.write("return %s;", name);
    w.outdent();
    w.write("}");
  }

  private JMethod findMethod(JClassType type, String methodName) {
    // TODO Move this and getClassHierarchyBreadthFirst to JClassType
    for (JClassType nextType : UiBinderWriter.getClassHierarchyBreadthFirst(type)) {
      try {
        return nextType.getMethod(methodName, new JType[0]);
      } catch (NotFoundException e) {
        /* try parent */
      }
    }
    return null;
  }

  private JType getReturnType(JType type, List<String> path,
      MonitoredLogger logger) {
    // TODO(rjrjr,bobv) This is derived from CssResourceGenerator.validateValue
    // We should find a way share code

    Iterator<String> i = path.iterator();
    while (i.hasNext()) {
      String pathElement = i.next();

      JClassType referenceType = type.isClassOrInterface();
      if (referenceType == null) {
        logger.error("Cannot resolve member " + pathElement
            + " on non-reference type " + type.getQualifiedSourceName());
        return null;
      }

      JMethod m = findMethod(referenceType, pathElement);
      if (m == null) {
        logger.error("Could not find no-arg method named " + pathElement
            + " in type " + type.getQualifiedSourceName());
        return null;
      }

      type = m.getReturnType();
    }
    return type;
  }
}
