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

import com.google.gwt.dev.javac.asmbridge.EmptyVisitor;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.StringInterner;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * A visitor (that collects class data from bytecode) and a model object to hold the collected data.
 */
public class CollectClassData extends EmptyVisitor {

  /**
   * Holds the descriptor and value for an Enum-valued annotation.
   */
  public static class AnnotationEnum {
    private final String desc;
    private final String value;

    /**
     * Construct the value of an Enum-valued annotation.
     *
     * @param desc type descriptor of this enum
     * @param value actual value in this enum
     */
    public AnnotationEnum(String desc, String value) {
      this.desc = StringInterner.get().intern(desc);
      this.value = StringInterner.get().intern(value);
    }

    /**
     * @return the type descriptor of the enum type.
     */
    public String getDesc() {
      return desc;
    }

    /**
     * @return the annotation value.
     */
    public String getValue() {
      return value;
    }
  }

  /**
   * Type of this class.
   */
  public enum ClassType {
    /**
     * An anonymous inner class.
     */
    Anonymous {
      @Override
      public boolean hasNoExternalName() {
        return true;
      }
    },

    /**
     * A non-static named class nested inside another class.
     */
    Inner {
      @Override
      public boolean hasHiddenConstructorArg() {
        return true;
      }
    },

    /**
     * A named class defined inside a method.
     */
    Local {
      /*
       * Note that we do not return true for hasHiddenConstructorArg since Local
       * classes inside a static method will not have one and AFAICT there is no
       * way to distinguish these cases without looking up the declaring method.
       * However, since we are dropping any classes for which
       * hasNoExternalName() returns true in TypeOracleUpdater.addNewUnits, it
       * doesn't matter if we leave the synthetic argument in the list.
       */

      @Override
      public boolean hasNoExternalName() {
        return true;
      }
    },

    /**
     * A static nested class inside another class.
     */
    Nested,

    /**
     * A top level class named the same as its source file.
     */
    TopLevel;

    /**
     * @return true if this class type has a hidden constructor argument for the
     *         containing instance (ie, this$0).
     */
    public boolean hasHiddenConstructorArg() {
      return false;
    }

    /**
     * @return true if this class type is not visible outside a method.
     */
    public boolean hasNoExternalName() {
      return false;
    }
  }

  private int access;

  private final List<CollectAnnotationData> annotations = new ArrayList<CollectAnnotationData>();

  private CollectClassData.ClassType classType = ClassType.TopLevel;

  private String enclosingInternalName;

  private String enclosingMethodDesc;

  private String enclosingMethodName;
  private final List<CollectFieldData> fields = new ArrayList<CollectFieldData>();
  // internal names of interfaces
  private String[] interfaceInternalNames;
  // internal name
  private String internalName;
  // nested source name
  private String nestedSourceName;
  private final List<CollectMethodData> methods = new ArrayList<CollectMethodData>();
  private String signature;
  private String source = null;
  // internal name of superclass
  private String superInternalName;

  /**
   * Construct a visitor that will collect data about a class.
   */
  public CollectClassData() {
  }

  /**
   * @return the access flags for this class (ie, bitwise or of Opcodes.ACC_*).
   */
  public int getAccess() {
    return access;
  }

  public List<CollectAnnotationData> getAnnotations() {
    return annotations;
  }

  public CollectClassData.ClassType getClassType() {
    return classType;
  }

  public String getEnclosingInternalName() {
    return enclosingInternalName;
  }

  public String getEnclosingMethodDesc() {
    return enclosingMethodDesc;
  }

  public String getEnclosingMethodName() {
    return enclosingMethodName;
  }

  public List<CollectFieldData> getFields() {
    return fields;
  }

  /**
   * @return an array of internal names of interfaces implemented by this class.
   */
  public String[] getInterfaceInternalNames() {
    return interfaceInternalNames;
  }

  public String getInternalName() {
    return internalName;
  }

  public List<CollectMethodData> getMethods() {
    return methods;
  }

  public String getNestedSourceName() {
    return nestedSourceName;
  }

  public String getSignature() {
    return signature;
  }

  public String getSource() {
    return source;
  }

  public String getSuperInternalName() {
    return superInternalName;
  }

  /**
   * @return true if this class has no external name (ie, is defined inside a
   *         method).
   */
  public boolean hasNoExternalName() {
    return classType.hasNoExternalName();
  }

  /**
   * @return true if this class has no source name at all.
   */
  public boolean isAnonymous() {
    return classType == ClassType.Anonymous;
  }

  @Override
  public String toString() {
    return "class " + internalName;
  }

  /**
   * Called at the beginning of visiting the class.
   *
   * @param version classfile version (ie, Opcodes.V1_5 etc)
   * @param access access flags (ie, bitwise or of Opcodes.ACC_*)
   * @param signature generic signature or null
   * @param interfaces array of internal names of implemented interfaces
   * @param internalName internal name of this class (ie, com/google/Foo)
   * @param superInternalName internal name of superclass (ie, java/lang/Object)
   */
  @Override
  public void visit(int version, int access, String internalName, String signature,
      String superInternalName, String[] interfaces) {
    this.access = access;
    assert Name.isInternalName(internalName);
    this.internalName = internalName;
    this.signature = signature;
    this.superInternalName = superInternalName;
    this.interfaceInternalNames = interfaces;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    CollectAnnotationData av = new CollectAnnotationData(desc, visible);
    annotations.add(av);
    return av;
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    if (classType == ClassType.TopLevel) {
      // top level source name calculation is trivial
      nestedSourceName = internalName.substring(internalName.lastIndexOf('/') + 1);
    } else if (classType == ClassType.Anonymous) {
      nestedSourceName = null;
    }
  }

  /**
   * Called for each field.
   *
   * @param access access flags for field
   * @param name field name
   * @param desc type descriptor (ie, Ljava/lang/String;)
   * @param signature generic signature (null if not generic)
   * @param value initialized value if constant
   */
  @Override
  public FieldVisitor visitField(int access, String name, String desc,
      String signature, Object value) {
    if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
      // if ("this$1".equals(name) && classType == ClassType.Anonymous) {
      // // TODO(jat): !!! really nasty hack
      // classType = ClassType.Inner;
      // }
      // skip synthetic fields
      return null;
    }
    CollectFieldData fv = new CollectFieldData(access, name, desc, signature,
        value);
    fields.add(fv);
    return fv;
  }

  /**
   * Called once for every inner class of this class.
   *
   * @param internalName internal name of inner class (ie, com/google/Foo$1)
   * @param enclosingInternalName internal name of enclosing class (null if not a member
   *          class or anonymous)
   * @param innerName simple name of the inner class (null if anonymous)
   * @param access access flags (bitwise or of Opcodes.ACC_*) as declared in the
   *          enclosing class
   */
  @Override
  public void visitInnerClass(String internalName, String enclosingInternalName, String innerName,
      int access) {

    buildNestedSourceName(internalName, enclosingInternalName, innerName);

    // If this inner class is ourselves, take the access flags defined in the InnerClass attribute.
    if (this.internalName.equals(internalName)) {
      if (enclosingInternalName != null) {
        this.enclosingInternalName = enclosingInternalName;
      }
      this.access = access;
      boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
      switch (classType) {
        case TopLevel:
          classType = isStatic ? ClassType.Nested : ClassType.Inner;
          break;
        case Anonymous:
          if (innerName != null) {
            classType = ClassType.Local;
          }
          break;
        case Inner:
          // Already marked as inner class by the synthetic this$1 field
          break;
        default:
          throw new IllegalStateException("Unexpected INNERCLASS with type of "
              + classType);
      }
    }
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
      // skip synthetic methods
      return null;
    }
    CollectMethodData mv = new CollectMethodData(classType, access, name, desc,
        signature, exceptions);
    methods.add(mv);
    return mv;
  }

  @Override
  public void visitOuterClass(
      String enclosingInternalName, String enclosingMethodName, String enclosingMethodDesc) {
    this.enclosingInternalName = enclosingInternalName;
    this.enclosingMethodName = enclosingMethodName;
    this.enclosingMethodDesc = enclosingMethodDesc;
    classType = ClassType.Anonymous; // Could be Local, catch that later
  }

  /**
   * If compiled with debug, visit the source information.
   *
   * @param source unqualified filename containing source (ie, Foo.java)
   * @param debug additional debug information (may be null)
   */
  @Override
  public void visitSource(String source, String debug) {
    this.source = source;
  }

  private void buildNestedSourceName(String internalName, String enclosingInternalName,
      String innerName) {
    if (classType == ClassType.Anonymous || enclosingInternalName == null) {
      return;
    }

    // ignores classes outside of this class' containment chain
    if (!this.internalName.startsWith(internalName + "$")
        && !this.internalName.equals(internalName)) {
      return;
    }

    if (nestedSourceName == null) {
      // for 'com.Foo$Bar' in 'com.Foo', starts nestedSourceName as 'Foo'
      nestedSourceName =
          enclosingInternalName.substring(enclosingInternalName.lastIndexOf('/') + 1);
    }
    // tacks on the simple name, which might contain a '$'
    nestedSourceName += "." + innerName;
  }
}
