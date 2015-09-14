/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * FieldReferences Graph, which records {referencedFields <-> methods} pairs.
 */
public class FieldReferencesGraph {

  /**
   * Visitor used to build fieldReferencesGraph.
   */
  class BuildFieldReferencesGraphVisitor extends JVisitor {
    private JMethod currentMethod;

    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      JField field = x.getField();
      if (currentMethod != null) {
        methodsByReferencedField.put(field, currentMethod);
        referencedFieldsByMethod.put(currentMethod, field);
      }
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      assert (currentMethod == x);
      currentMethod = null;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      assert (currentMethod == null);
      currentMethod = x;
      return true;
    }
  }

  private Multimap<JField, JMethod> methodsByReferencedField = LinkedHashMultimap.create();
  private Multimap<JMethod, JField> referencedFieldsByMethod = LinkedHashMultimap.create();

  /**
   * Build the field references graph of a JProgram.
   */
  public void buildFieldReferencesGraph(JProgram program) {
    reset();
    BuildFieldReferencesGraphVisitor buildFieldUsesVisitor = new BuildFieldReferencesGraphVisitor();
    buildFieldUsesVisitor.accept(program);
  }

  /**
   * Return the referenced fields by {@code methods}.
   */
  public Set<JField> getReferencedFieldsByMethods(Collection<JMethod> methods) {
    assert (methods != null);
    Set<JField> result = new LinkedHashSet<JField>();
    for (JMethod method : methods) {
      result.addAll(referencedFieldsByMethod.get(method));
    }
    return result;
  }

  /**
   * Return the methods that reference {@code fields}.
   */
  public Set<JMethod> getReferencingMethodsForFields(Collection<JField> fields) {
    assert (fields != null);
    Set<JMethod> referencingMethods = new LinkedHashSet<JMethod>();
    for (JField field : fields) {
      referencingMethods.addAll(methodsByReferencedField.get(field));
    }
    return referencingMethods;
  }

  /**
   * For removing a field, remove the {referencedFields <-> methods} pairs that are related to the
   * field.
   */
  public void removeField(JField field) {
    Collection<JMethod> methods = methodsByReferencedField.removeAll(field);
    for (JMethod method : methods) {
      referencedFieldsByMethod.remove(method, field);
    }
  }

  /**
   * For removing a method, remove the {referencedFields <-> methods} pairs that are related to the
   * method.
   */
  public void removeMethod(JMethod method) {
    Collection<JField> referencedFields = referencedFieldsByMethod.removeAll(method);
    for (JField referencedField : referencedFields) {
      methodsByReferencedField.remove(referencedField, method);
    }
  }

  /**
   * Reset the graph.
   */
  public void reset() {
    methodsByReferencedField.clear();
    referencedFieldsByMethod.clear();
  }

  /**
   * Update field references graph of a method.
   */
  public void updateFieldReferencesOfMethod(JMethod method) {
    removeMethod(method);
    BuildFieldReferencesGraphVisitor buildFieldUsesVisitor = new BuildFieldReferencesGraphVisitor();
    buildFieldUsesVisitor.accept(method);
  }
}
