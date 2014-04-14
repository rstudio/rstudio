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
package com.google.gwt.dev.javac.asm;

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.Label;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects data from a single method.
 */
public class CollectMethodData extends MethodVisitor {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private final List<CollectAnnotationData> annotations = new ArrayList<CollectAnnotationData>();
  private final String name;
  private final String desc;
  private final String signature;
  private final String[] exceptions;
  private Type[] argTypes;
  private final String[] argNames;
  private final List<CollectAnnotationData>[] paramAnnots;
  private boolean actualArgNames = false;
  private final int access;
  private int syntheticArgs;
  private int longDoubleCounter;

  /**
   * Prepare to collect data for a method from bytecode.
   *
   * @param classType
   * @param access
   * @param name
   * @param desc
   * @param signature
   * @param exceptions
   */
  @SuppressWarnings("unchecked")
  // for new List[]
  public CollectMethodData(CollectClassData.ClassType classType, int access,
      String name, String desc, String signature, String[] exceptions) {
    super(Opcodes.ASM4);
    this.access = access;
    this.name = name;
    this.desc = desc;
    this.signature = signature;
    this.exceptions = exceptions;
    syntheticArgs = 0;
    argTypes = Type.getArgumentTypes(desc);
    // Non-static instance methods and constructors of non-static inner
    // classes have an extra synthetic parameter that isn't in the source,
    // so we remove it. Note that for local classes, they may or may not
    // have this synthetic parameter depending on whether the containing
    // method is static, but we can't get that info here. However, since
    // local classes are dropped from TypeOracle, we don't care.
    if (classType.hasHiddenConstructorArg() && "<init>".equals(name)) {
      // remove "this$1" as a parameter
      if (argTypes.length < 1) {
        throw new IllegalStateException(
            "Missing synthetic argument in constructor");
      }
      syntheticArgs = 1;
      int n = argTypes.length - syntheticArgs;
      Type[] newArgTypes = new Type[n];
      System.arraycopy(argTypes, syntheticArgs, newArgTypes, 0, n);
      argTypes = newArgTypes;
    }
    argNames = new String[argTypes.length];
    paramAnnots = new List[argTypes.length];
    for (int i = 0; i < argNames.length; ++i) {
      argNames[i] = "arg" + i;
      paramAnnots[i] = new ArrayList<CollectAnnotationData>();
    }
    if (argNames.length == 0) {
      // save some work later if there aren't any parameters
      actualArgNames = true;
    }
  }

  /**
   * @return the access
   */
  public int getAccess() {
    return access;
  }

  /**
   * @return the annotations
   */
  public List<CollectAnnotationData> getAnnotations() {
    return annotations;
  }

  public List<CollectAnnotationData>[] getArgAnnotations() {
    return paramAnnots;
  }

  /**
   * @return the argNames
   */
  public String[] getArgNames() {
    return argNames;
  }

  /**
   * @return the argTypes
   */
  public Type[] getArgTypes() {
    return argTypes;
  }

  /**
   * @return the desc
   */
  public String getDesc() {
    return desc;
  }

  /**
   * @return the exceptions
   */
  public String[] getExceptions() {
    return exceptions == null ? EMPTY_STRING_ARRAY : exceptions;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the signature
   */
  public String getSignature() {
    return signature;
  }

  /**
   * @return the actualArgNames
   */
  public boolean hasActualArgNames() {
    return actualArgNames;
  }

  @Override
  public String toString() {
    return "method " + name;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    CollectAnnotationData av = new CollectAnnotationData(desc, visible);
    annotations.add(av);
    return av;
  }

  @Override
  public void visitLocalVariable(String name, String desc, String signature,
      Label start, Label end, int index) {
    // The incoming index is based on counting long and double
    // arguments as taking up two slots.  For example, a method 'm(int
    // a, long b, String c, double d, Object e)' will call this method
    // with indices 0, 1, 3, 4, 6 (ignoring 'this' for the sake of the
    // example).  To compensate, we set a counter to 0 at the first
    // argument of each method and increment it for each long or
    // double argument we encounter.  Then each incoming index is
    // adjusted down by the value of the counter to obtain a simple
    // index.
    if (index == 0) {
      longDoubleCounter = 0;
    } else {
      index -= longDoubleCounter;
    }

    if ((access & Opcodes.ACC_STATIC) == 0) {
      // adjust for "this"
      // TODO(jat): do we need to account for this$0 in inner classes?
      --index;
    }
    // TODO(jat): is it safe to assume parameter slots don't get reused?
    // Do we need to check if the name has already been assigned?
    if (index >= 0 && index < argNames.length) {
      actualArgNames = true;
      argNames[index] = name;
    }

    // Adjust the counter
    if ("J".equals(desc) || "D".equals(desc)) {
      longDoubleCounter++;
    }
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(int parameter, String desc,
      boolean visible) {
    CollectAnnotationData av = new CollectAnnotationData(desc, visible);
    if (parameter >= syntheticArgs) {
      // javac adds @Synthetic annotation on its synthetic constructor
      // arg, so we ignore it since it isn't in the source.
      paramAnnots[parameter - syntheticArgs].add(av);
    }
    return av;
  }
}
