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

import com.google.gwt.dev.javac.JsInteropUtil;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.impl.GwtAstBuilder;
import com.google.gwt.dev.jjs.impl.JjsUtils;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Base class for any declared type.
 *
 * Declared types have fields and methods. Two of the methods are treated specially: the class
 * initializer method (named <code>$clinit</code>) and the instance initializer method
 * (named <code>$init</code>).
 *
 * The class initializer method is responsible for initializing all class variables as well as
 * those of the superclasses (by calling the superclass class initializer method).
 *
 * The instance initializer is responsible for initializing all instance variables as well as those
 * of the superclasses (by calling the superclass instance initializer method).
 *
 * Optimizations may eliminate class initializers (<code>$clinit</code>) if no static variables need
 * initialization, and use the private variable <code>clinitTarget</code>to keep track which
 * initializer in the superclass chain needs to be called.
 */
public abstract class JDeclaredType extends JReferenceType
    implements CanHaveSuppressedWarnings, HasJsName {

  private boolean isJsFunction;
  private boolean isJsType;
  private boolean isClassWideExport;
  private boolean isJsNative;
  private boolean canBeImplementedExternally;
  private String jsNamespace = null;
  private String jsName = null;
  private Set<String> suppressedWarnings;

  /**
   * The types of nested classes, https://docs.oracle.com/javase/tutorial/java/javaOO/nested.html
   */
  public enum NestedClassDisposition {
    /**
     * Static Nested Class.
     */
    STATIC,
    /**
     * Inner Nested Class.
     */
    INNER,
    /**
     * Local Class.
     */
    LOCAL(true),
    /**
     * Anonymous Inner Class.
     */
    ANONYMOUS(true),
    /**
     * Synthetic Inner Class from Lambda or method reference.
     */
    LAMBDA(true),
    /**
     * Regular top-level class.
     */
    TOP_LEVEL;

    private final boolean localType;

    NestedClassDisposition(boolean local) {
      this.localType = local;
    }

    NestedClassDisposition() {
      this.localType = false;
    }

    public boolean isLocalType() {
      return localType;
    }
  }

  private NestedClassDisposition nestedClassDisposition = NestedClassDisposition.TOP_LEVEL;

  /**
   * This type's fields. Special serialization treatment.
   */
  protected transient List<JField> fields = Lists.create();

  /**
   * This type's methods. Special serialization treatment.
   */
  protected transient List<JMethod> methods = Lists.create();

  /**
   * Tracks the target static initialization for this class. Default to self (if it has a non
   * empty initializer) or point to a superclass or be null.
   */
  private JDeclaredType clinitTarget = this;

  /**
   * The type which originally enclosed this type. Null if this class was a
   * top-level type. Note that all classes are converted to top-level types in
   * {@link com.google.gwt.dev.jjs.impl.GenerateJavaAST}; this information is
   * for tracking purposes.
   */
  private JDeclaredType enclosingType;

  /**
   * True if this class is provided externally to the program by the program's
   * host execution environment. For example, while compiling for the JVM, JRE
   * types are external types. External types definitions are provided by class
   * files which are considered opaque by the GWT compiler.
   */
  private boolean isExternal;

  /**
   * This type's implemented interfaces.
   */
  private List<JInterfaceType> superInterfaces = Lists.create();

  public JDeclaredType(SourceInfo info, String name) {
    super(info, name);
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
  public final void addMethod(int index, JMethod method) {
    assert method.getEnclosingType() == this;
    assert !method.getName().equals(GwtAstBuilder.CLINIT_METHOD_NAME)
        || getMethods().size() == 0
        : "Attempted adding $clinit method with index != 0";
    assert !method.getName().equals(GwtAstBuilder.INIT_NAME_METHOD_NAME)
        || method.getParams().size() != 0
        || getMethods().size() == 1
        : "Attempted adding $init method with index != 1";
    methods = Lists.add(methods, index, method);
  }

  public void addMethod(JMethod newMethod) {
    addMethod(methods.size(), newMethod);
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

  /**
   * Returns the method with the given signature, if there is one.<br />
   *
   * Optionally can search up the super type chain.
   */
  public JMethod findMethod(String methodSignature, boolean recurse) {
    for (JMethod method : getMethods()) {
      if (method.getSignature().equals(methodSignature)) {
        return method;
      }
    }
    if (recurse && getSuperClass() != null) {
      return getSuperClass().findMethod(methodSignature, true);
    }
    return null;
  }

  /**
   * Determines whether a subclass of this type is in the collection <code>types</code>.
   *
   * @param types a collections of types.
   * @return the first subtype found in the collection  if the collection <code>types</code>
   *             contains a subtype of this type; null otherwise.
   */
  public JDeclaredType findSubtype(Iterable<JDeclaredType> types) {
    for (JDeclaredType type : types) {
      JDeclaredType tp = type;
      while (tp != null) {
        if (this == tp) {
          return type;
        }
        tp = tp.getSuperClass();
      }
    }
    return null;
  }

  /**
   * Returns the class initializer method.
   * Can only be called after making sure the class has a class initializer method.
   *
   * @return The class initializer method.
   */
  public final JMethod getClinitMethod() {
    assert getMethods().size() != 0;
    JMethod clinit = this.getMethods().get(GwtAstBuilder.CLINIT_METHOD_INDEX);

    assert clinit != null;
    assert clinit.getName().equals(GwtAstBuilder.CLINIT_METHOD_NAME);
    return clinit;
  }

  /**
   * Returns the class that must be initialized to use this class. May be a
   * superclass, or <code>null</code> if this class has no static initializer.
   */
  public final JDeclaredType getClinitTarget() {
    if (isJsFunction()) {
      return null;
    }
    return clinitTarget;
  }

  @Override
  public String[] getCompoundName() {
    // TODO(rluble): refactor the way names are constructed so that the ground data passed from
    // JDT is stored.
    if (enclosingType == null) {
      return new String[] { getShortName() };
    }

    String className = StringInterner.get().intern(
        getShortName().substring(enclosingType.getShortName().length() + 1));

    String[] enclosingCompoundName = enclosingType.getCompoundName();
    String[] compoundName = new String[enclosingCompoundName.length + 1];
    System.arraycopy(enclosingCompoundName, 0, compoundName, 0, enclosingCompoundName.length);
    compoundName[compoundName.length - 1] = className;
    return compoundName;
  }

  /**
   * Returns the simple source name for the class.
   * <p>e.g. if the class is a.b.Foo.Bar it returns Bar as opposed to the short name Foo$Bar.
   */
  public String getSimpleName() {
    String[] compoundName = getCompoundName();
    return compoundName[compoundName.length - 1];
  }

  /**
   * Returns the constructors for this type.
   */
  public Iterable<JConstructor> getConstructors() {
    return (Iterable) Iterables.filter(methods, Predicates.instanceOf(JConstructor.class));
  }

  /**
   * Returns the type which encloses this type.
   *
   * @return The enclosing type. May be {@code null}.
   */
  public JDeclaredType getEnclosingType() {
    return enclosingType;
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

  /**
   * Returns the instance initializer ($init) method.
   *
   * @return The instance initializer method.
   */
  public abstract JMethod getInitMethod();

  @Override
  public String getJavahSignatureName() {
    return JjsUtils.javahSignatureFromName(name);
  }

  @Override
  public String getJsniSignatureName() {
    return "L" + name.replace('.', '/') + ';';
  }

  /**
   * Returns this type's declared methods; does not include methods defined in a
   * super type unless they are overridden by this type.
   */
  public final List<JMethod> getMethods() {
    return methods;
  }

  public Iterable<JMember> getMembers() {
    return Iterables.<JMember>concat(fields, methods);
  }

  @Override
  public boolean isJsType() {
    return isJsType;
  }

  @Override
  public boolean isJsFunction() {
    return isJsFunction;
  }

  public boolean isJsFunctionImplementation() {
    return false;
  }

  public boolean isClassWideExport() {
    return isClassWideExport;
  }

  public boolean hasJsInteropEntryPoints() {
    for (JMethod method : getMethods()) {
      if (method.isJsInteropEntryPoint()) {
        return true;
      }
    }

    for (JField field : getFields()) {
      if (field.isJsInteropEntryPoint()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean canBeReferencedExternally() {
    if (isJsType()) {
      return true;
    }
    for (JMember member : getMembers()) {
      if (member.canBeReferencedExternally()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isJsNative() {
    return isJsNative;
  }

  @Override
  public boolean canBeImplementedExternally() {
    return canBeImplementedExternally;
  }

  /**
   * Returns this type's super class, or <code>null</code> if this type is
   * {@link Object} or an interface.
   */
  public abstract JClassType getSuperClass();

  /**
   * Returns <code>true</code> when this class's clinit must be run dynamically.
   */
  public boolean hasClinit() {
    return getClinitTarget() != null;
  }

  @Override
  public boolean isExternal() {
    return isExternal;
  }

  /**
   * Returns whether this type can be instantiated.
   */
  public boolean isInstantiable() {
    if (isAbstract()) {
      return false;
    }
    if (!(this instanceof JClassType) && !(this instanceof JEnumType)) {
      return false;
    }
    if (getDefaultConstructor() == null) {
      return false;
    }
    return true;
  }

  /**
   * Removes the field at the specified index.
   */
  public void removeField(int i) {
    assert !isExternal() : "External types can not be modified.";
    fields = Lists.remove(fields, i);
  }

  /**
   * Removes the method at the specified index.
   */
  public void removeMethod(int i) {
    assert !isExternal() : "External types can not be modified.";
    methods = Lists.remove(methods, i);
  }

  /**
   * Resolves external references during AST stitching.
   */
  public void resolve(List<JInterfaceType> resolvedInterfaces, JDeclaredType pkgInfo) {
    assert JType.replaces(resolvedInterfaces, superInterfaces);
    superInterfaces = Lists.normalize(resolvedInterfaces);
    if (jsNamespace == null) {
      jsNamespace = computeJsNamespace(pkgInfo);
    }
  }

  private String computeJsNamespace(JDeclaredType pkgInfo) {
    if (enclosingType != null) {
      return enclosingType.getQualifiedJsName();
    }
    return pkgInfo != null && pkgInfo.jsNamespace != null ? pkgInfo.jsNamespace : getPackageName();
  }

  /**
   * Sets the type which encloses this types.
   *
   * @param enclosingType May be {@code null}.
   */
  public void setEnclosingType(JDeclaredType enclosingType) {
    this.enclosingType = enclosingType;
  }

  public void setExternal(boolean isExternal) {
    this.isExternal = isExternal;
  }

  public void setJsTypeInfo(boolean isJsType, boolean isJsNative, boolean isJsFunction,
      String jsNamespace, String jsName, boolean isClassWideExport,
      boolean canBeImplementedExternally) {
    this.isJsType = isJsType;
    this.isJsNative = isJsNative;
    this.isJsFunction = isJsFunction;
    this.jsNamespace = jsNamespace;
    this.jsName = jsName;
    this.isClassWideExport = isClassWideExport;
    this.canBeImplementedExternally = canBeImplementedExternally;
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
   * Subclasses must replace themselves with a shallow reference when
   * {@link #isExternal()} is <code>true</code>.
   */
  protected abstract Object writeReplace();

  /**
   * See {@link #writeMembers(ObjectOutputStream)}.
   *
   * @see #writeMembers(ObjectOutputStream)
   */
  @SuppressWarnings("unchecked") void readMembers(ObjectInputStream stream)
      throws IOException, ClassNotFoundException {
    fields = (List<JField>) stream.readObject();
    methods = (List<JMethod>) stream.readObject();
  }

  /**
   * See {@link #writeMethodBodies(ObjectOutputStream)}.
   *
   * @see #writeMethodBodies(ObjectOutputStream)
   */
  void readMethodBodies(ObjectInputStream stream) throws IOException, ClassNotFoundException {
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
    if (newClinitTarget != null && getClass().desiredAssertionStatus()) {
      // Make sure this is a pure upgrade to a superclass or null.
      for (JClassType current = (JClassType) clinitTarget; current != newClinitTarget; current =
          current.getSuperClass()) {
        Preconditions.checkNotNull(current.getSuperClass(),
            "Null super class for: %s (currentTarget: %s; newTarget: %s) in %s", current,
            clinitTarget, newClinitTarget, this);
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

  private JMethod getDefaultConstructor() {
    for (JMethod constructor : getConstructors()) {
      if (constructor.getOriginalParamTypes().size() == 0) {
        return constructor;
      }
    }
    return null;
  }

  @Override
  public String getJsName() {
    return Strings.isNullOrEmpty(jsName) ? getSimpleName() : jsName;
  }

  @Override
  public String getJsNamespace() {
    return jsNamespace;
  }

  @Override
  public String getQualifiedJsName() {
    return JsInteropUtil.isGlobal(jsNamespace) ? getJsName() : jsNamespace + "." + getJsName();
  }

  public NestedClassDisposition getClassDisposition() {
    return nestedClassDisposition;
  }

  public void setClassDisposition(NestedClassDisposition nestedClassDisposition) {
    this.nestedClassDisposition = nestedClassDisposition;
  }

  @Override
  public Set<String> getSuppressedWarnings() {
    return suppressedWarnings;
  }

  @Override
  public void setSuppressedWarnings(Set<String> suppressedWarnings) {
    this.suppressedWarnings = suppressedWarnings;
  }
}
