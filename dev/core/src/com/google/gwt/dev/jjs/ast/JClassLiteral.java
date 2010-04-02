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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;

/**
 * Java class literal expression.
 * 
 * NOTE: This class is modeled as if it were a JFieldRef to a field declared in
 * ClassLiteralHolder. That field contains the class object allocation
 * initializer.
 */
public class JClassLiteral extends JLiteral implements JAnnotationArgument {
  /**
   * Create an expression that will evaluate, at run time, to the class literal.
   * Cannot be called after optimizations begin.
   */
  static JMethodCall computeClassObjectAllocation(JProgram program,
      SourceInfo info, JType type) {
    String typeName = getTypeName(program, type);

    JMethod method = program.getIndexedMethod(type.getClassLiteralFactoryMethod());

    /*
     * Use the classForEnum() constructor even for enum subtypes to aid in
     * pruning supertype data.
     */
    boolean isEnumOrSubclass = false;
    if (type instanceof JClassType) {
      JEnumType maybeEnum = ((JClassType) type).isEnumOrSubclass();
      if (maybeEnum != null) {
        isEnumOrSubclass = true;
        method = program.getIndexedMethod(maybeEnum.getClassLiteralFactoryMethod());
      }
    }

    assert method != null;

    JMethodCall call = new JMethodCall(info, null, method);
    JStringLiteral packageName = program.getLiteralString(info,
        getPackageName(typeName));
    JStringLiteral className = program.getLiteralString(info,
        getClassName(typeName));
    call.addArgs(packageName, className);

    if (type instanceof JArrayType) {
      // There's only one seed function for all arrays
      JDeclaredType arrayType = program.getIndexedType("Array");
      call.addArg(new JNameOf(info, className.getType(), arrayType));

    } else if (type instanceof JClassType) {
      // Add the name of the seed function for concrete types
      call.addArg(new JNameOf(info, className.getType(), type));

    } else if (type instanceof JPrimitiveType) {
      // And give primitive types an illegal, though meaningful, value
      call.addArg(program.getLiteralString(info, " "
          + type.getJavahSignatureName()));
    }

    if (type instanceof JClassType) {
      /*
       * For non-array classes and enums, determine the class literal of the
       * supertype, if there is one. Arrays are excluded because they always
       * have Object as their superclass.
       */
      JClassType classType = (JClassType) type;

      JLiteral superclassLiteral;
      if (classType.getSuperClass() != null) {
        superclassLiteral = program.getLiteralClass(classType.getSuperClass());
      } else {
        superclassLiteral = program.getLiteralNull();
      }

      call.addArg(superclassLiteral);

      if (classType instanceof JEnumType) {
        JEnumType enumType = (JEnumType) classType;
        JMethod valuesMethod = null;
        for (JMethod methodIt : enumType.getMethods()) {
          if ("values".equals(methodIt.getName())) {
            if (methodIt.getParams().size() != 0) {
              continue;
            }
            valuesMethod = methodIt;
            break;
          }
        }
        if (valuesMethod == null) {
          throw new InternalCompilerException(
              "Could not find enum values() method");
        }
        JsniMethodRef jsniMethodRef = new JsniMethodRef(info, null,
            valuesMethod, program.getJavaScriptObject());
        call.addArg(jsniMethodRef);
      } else if (isEnumOrSubclass) {
        // A subclass of an enum class
        call.addArg(program.getLiteralNull());
      }
    } else if (type instanceof JArrayType) {
      JArrayType arrayType = (JArrayType) type;
      JClassLiteral componentLiteral = program.getLiteralClass(arrayType.getElementType());
      call.addArg(componentLiteral);
    } else {
      assert (type instanceof JExternalType || type instanceof JInterfaceType || type instanceof JPrimitiveType);
    }
    assert call.getArgs().size() == method.getParams().size() : "Argument / param mismatch "
        + call.toString() + " versus " + method.toString();
    return call;
  }

  private static String getClassName(String fullName) {
    int pos = fullName.lastIndexOf(".");
    return fullName.substring(pos + 1);
  }

  private static String getPackageName(String fullName) {
    int pos = fullName.lastIndexOf(".");
    return fullName.substring(0, pos + 1);
  }

  private static String getTypeName(JProgram program, JType type) {
    String typeName;
    if (type instanceof JArrayType) {
      typeName = type.getJsniSignatureName().replace('/', '.');
      // Mangle the class name to match hosted mode.
      if (program.isJavaScriptObject(((JArrayType) type).getLeafType())) {
        typeName = typeName.replace(";", "$;");
      }
    } else {
      typeName = type.getName();
      // Mangle the class name to match hosted mode.
      if (program.isJavaScriptObject(type)) {
        typeName += '$';
      }
    }
    return typeName;
  }

  private final JField field;
  private final JType refType;

  /**
   * This constructor is only used by {@link JProgram}.
   */
  JClassLiteral(SourceInfo sourceInfo, JType type, JField field) {
    super(sourceInfo);
    refType = type;
    this.field = field;
  }

  public JNode annotationNode() {
    return this;
  }

  /**
   * Returns the field holding my allocated object.
   */
  public JField getField() {
    return field;
  }

  public JType getRefType() {
    return refType;
  }

  public JType getType() {
    return field.getType();
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
}
