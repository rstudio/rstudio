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
  private static final String NO_DEFAULT_CTOR_ERROR = "%1$s has no default (zero args) constructor. To fix this, you can define"
      + " a @UiFactory method on the UiBinder's owner, or annotate a constructor of %2$s with"
      + " @UiConstructor.";

  private final String name;

  private final Set<FieldWriter> needs = new LinkedHashSet<FieldWriter>();
  private String initializer;
  private boolean written;
  private MortalLogger logger;

  public AbstractFieldWriter(String name, MortalLogger logger) {
    if (name == null) {
      throw new RuntimeException("name cannot be null");
    }
    this.name = name;
    this.logger = logger;
  }

  public String getInitializer() {
    return initializer;
  }

  public JType getReturnType(String[] path, MonitoredLogger logger) {
    if (!name.equals(path[0])) {
      throw new RuntimeException(this
          + " asked to evaluate another field's path: " + path[0]);
    }

    List<String> pathList = Arrays.asList(path).subList(1, path.length);
    return getReturnType(getAssignableType(), pathList, logger);
  }

  public void needs(FieldWriter f) {
    needs.add(f);
  }

  public void setInitializer(String initializer) {
    if (this.initializer != null) {
      throw new IllegalStateException(String.format(
          "Second attempt to set initializer for field \"%s\", "
              + "from \"%s\" to \"%s\"", name, this.initializer, initializer));
    }
    setInitializerMaybe(initializer);
  }

  @Deprecated
  public void setInitializerMaybe(String initializer) {
    if (this.initializer != null && !this.initializer.equals(initializer)) {
      throw new IllegalStateException(String.format(
          "Attempt to change initializer for field \"%s\", "
              + "from \"%s\" to \"%s\"", name, this.initializer, initializer));
    }
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
      // TODO(rdamazio, rjrjr) This is simplistic, and will fail when
      // we support more interesting contexts (e.g. the same need being used
      // inside two different
      // LazyPanels)
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