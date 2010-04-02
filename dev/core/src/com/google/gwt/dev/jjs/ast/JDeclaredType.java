/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.util.collect.Lists;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for any reference type.
 */
public abstract class JDeclaredType extends JReferenceType implements
    HasAnnotations {
  /**
   * Annotations applied to the type. Special serialization treatment.
   */
  protected transient List<JAnnotation> annotations = Lists.create();

  /**
   * The other nodes that this node should implicitly rescue. Special
   * serialization treatment.
   */
  protected transient List<JNode> artificialRescues = Lists.create();

  /**
   * This type's fields. Special serialization treatment.
   */
  protected transient List<JField> fields = Lists.create();

  /**
   * This type's methods. Special serialization treatment.
   */
  protected transient List<JMethod> methods = Lists.create();

  /**
   * Tracks the target static initialization for this class. Default to self
   * until removed or set to be a superclass.
   */
  private JDeclaredType clinitTarget = this;

  /**
   * This type's super class.
   */
  private JClassType superClass;

  /**
   * This type's implemented interfaces.
   */
  private List<JInterfaceType> superInterfaces = Lists.create();

  public JDeclaredType(SourceInfo info, String name) {
    super(info, name);
  }

  public void addAnnotation(JAnnotation annotation) {
    annotations = Lists.add(annotations, annotation);
  }

  public void addArtificialRescue(JNode node) {
    artificialRescues = Lists.add(artificialRescues, node);
  }

  /**
   * Adds a field to this type.
   */
  public void addField(JField field) {
    assert field.getEnclosingType() == this;
    fields = Lists.add(fields, field);
  }

  /**
   * Adds an implemented interface to this type.
   */
  public void addImplements(JInterfaceType superInterface) {
    superInterfaces = Lists.add(superInterfaces, superInterface);
  }

  /**
   * Adds a method to this type.
   */
  public void addMethod(JMethod method) {
    assert method.getEnclosingType() == this;
    methods = Lists.add(methods, method);
  }

  /**
   * Returns <code>true</code> if a static field access of
   * <code>targetType</code> from within this type should generate a clinit
   * call. This will be true in cases where <code>targetType</code> has a live
   * clinit method which we cannot statically know has already run. We can
   * statically know the clinit method has already run when:
   * <ol>
   * <li><code>this == targetType</code></li>
   * <li><code>this</code> is a subclass of <code>targetType</code>, because my
   * clinit would have already run this <code>targetType</code>'s clinit; see
   * JLS 12.4</li>
   * </ol>
   */
  public boolean checkClinitTo(JDeclaredType targetType) {
    if (this == targetType) {
      // Call to self (very common case).
      return false;
    }
    if (targetType == null || !targetType.hasClinit()) {
      // Target has no clinit (common case).
      return false;
    }
    /*
     * The clinit for the source of the reference must already have run, so if
     * it's the same as this one, there it must have already run. One example is
     * a reference from a subclass to something in a superclass.
     */
    return this.getClinitTarget() != targetType.getClinitTarget();
  }

  public JAnnotation findAnnotation(String className) {
    return JAnnotation.findAnnotation(this, className);
  }

  public List<JAnnotation> getAnnotations() {
    return Lists.normalizeUnmodifiable(annotations);
  }

  public List<JNode> getArtificialRescues() {
    return artificialRescues;
  }

  /**
   * Returns the class that must be initialized to use this class. May be a
   * superclass, or <code>null</code> if this class has no static initializer.
   */
  public JDeclaredType getClinitTarget() {
    return clinitTarget;
  }

  /**
   * Returns this type's fields;does not include fields defined in a super type
   * unless they are overridden by this type.
   */
  public List<JField> getFields() {
    return fields;
  }

  /**
   * Returns this type's implemented interfaces. Returns an empty list if this
   * type implements no interfaces.
   */
  public List<JInterfaceType> getImplements() {
    return superInterfaces;
  }

  @Override
  public String getJavahSignatureName() {
    return "L" + name.replaceAll("_", "_1").replace('.', '_') + "_2";
  }

  @Override
  public String getJsniSignatureName() {
    return "L" + name.replace('.', '/') + ';';
  }

  /**
   * Returns this type's declared methods; does not include methods defined in a
   * super type unless they are overridden by this type.
   */
  public List<JMethod> getMethods() {
    return methods;
  }

  @Override
  public String getShortName() {
    int dotpos = name.lastIndexOf('.');
    return name.substring(dotpos + 1);
  }

  /**
   * Returns this type's super class, or <code>null</code> if this type is
   * {@link Object} or the {@link JNullType}.
   */
  @Override
  public JClassType getSuperClass() {
    return superClass;
  }

  /**
   * Returns <code>true</code> when this class's clinit must be run dynamically.
   */
  public boolean hasClinit() {
    return clinitTarget != null;
  }

  /**
   * Removes the field at the specified index.
   */
  public void removeField(int i) {
    fields = Lists.remove(fields, i);
  }

  /**
   * Removes the method at the specified index.
   */
  public void removeMethod(int i) {
    methods = Lists.remove(methods, i);
  }

  /**
   * Sets this type's super class.
   */
  @Override
  public void setSuperClass(JClassType superClass) {
    this.superClass = superClass;
  }

  /**
   * Sorts this type's fields according to the specified sort.
   */
  public void sortFields(Comparator<? super JField> sort) {
    fields = Lists.sort(fields, sort);
  }

  /**
   * Sorts this type's methods according to the specified sort.
   */
  public void sortMethods(Comparator<? super JMethod> sort) {
    // Sort the methods manually to avoid sorting clinit out of place!
    JMethod a[] = methods.toArray(new JMethod[methods.size()]);
    Arrays.sort(a, 1, a.length, sort);
    methods = Lists.create(a);
  }

  /**
   * Clears all existing implemented interfaces.
   */
  void clearImplements() {
    superInterfaces = Lists.create();
  }

  /**
   * See {@link #writeMembers(ObjectOutputStream)}.
   * 
   * @see #writeMembers(ObjectOutputStream)
   */
  @SuppressWarnings("unchecked")
  void readMembers(ObjectInputStream stream) throws IOException,
      ClassNotFoundException {
    fields = (List<JField>) stream.readObject();
    methods = (List<JMethod>) stream.readObject();
    artificialRescues = (List<JNode>) stream.readObject();
    annotations = (List<JAnnotation>) stream.readObject();
  }

  /**
   * See {@link #writeMethodBodies(ObjectOutputStream).
   * 
   * @see #writeMethodBodies(ObjectOutputStream)
   */
  void readMethodBodies(ObjectInputStream stream) throws IOException,
      ClassNotFoundException {
    for (JMethod method : methods) {
      method.readBody(stream);
    }
  }

  /**
   * Called to set this class's trivial initializer to point to a superclass.
   */
  void setClinitTarget(JDeclaredType newClinitTarget) {
    if (clinitTarget == newClinitTarget) {
      return;
    }
    if (getClass().desiredAssertionStatus()) {
      // Make sure this is a pure upgrade to a superclass or null.
      for (JDeclaredType current = clinitTarget; current != newClinitTarget; current = current.getSuperClass()) {
        assert current.getSuperClass() != null;
      }
    }
    clinitTarget = newClinitTarget;
  }

  /**
   * After all types are written to the stream without transient members, this
   * method actually writes fields and methods to the stream, which establishes
   * type identity for them.
   * 
   * @see JProgram#writeObject(ObjectOutputStream)
   */
  void writeMembers(ObjectOutputStream stream) throws IOException {
    stream.writeObject(fields);
    stream.writeObject(methods);
    stream.writeObject(artificialRescues);
    stream.writeObject(annotations);
  }

  /**
   * After all types, fields, and methods are written to the stream, this method
   * writes method bodies to the stream.
   * 
   * @see JProgram#writeObject(ObjectOutputStream)
   */
  void writeMethodBodies(ObjectOutputStream stream) throws IOException {
    for (JMethod method : methods) {
      method.writeBody(stream);
    }
  }
}
