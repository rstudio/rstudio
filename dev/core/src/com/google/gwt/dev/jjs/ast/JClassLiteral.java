/*
 * Copyright 2007 Google Inc.
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

/**
 * Java class literal expression.
 */
public class JClassLiteral extends JLiteral {

  // Matches constants in our java.lang.Class
  private static final int PRIMITIVE = 0x00000001;
  private static final int INTERFACE = 0x00000002;
  private static final int ARRAY = 0x00000004;
  private static final int ENUM = 0x00000008;

  private static String getClassName(String fullName) {
    int pos = fullName.lastIndexOf(".");
    return fullName.substring(pos + 1);
  }

  private static String getPackageName(String fullName) {
    int pos = fullName.lastIndexOf(".");
    return fullName.substring(0, pos + 1);
  }

  private static JIntLiteral getTypeBitsLit(JProgram program, JType type) {
    int bits;
    if (type instanceof JArrayType) {
      bits = ARRAY;
    } else if (type instanceof JEnumType) {
      bits = ENUM;
    } else if (type instanceof JClassType) {
      bits = 0;
    } else if (type instanceof JInterfaceType) {
      bits = INTERFACE;
    } else if (type instanceof JPrimitiveType) {
      bits = PRIMITIVE;
    } else {
      throw new InternalCompilerException("Unknown kind of type");
    }
    return program.getLiteralInt(bits);
  }

  private static JExpression getTypeNameLit(JProgram program, JType type) {
    JExpression typeNameLit;
    String typeName;
    if (type instanceof JArrayType) {
      typeName = type.getJsniSignatureName().replace('/', '.');
    } else {
      typeName = type.getName();
    }

    // Split the full class name into package + class so package strings
    // can be merged.
    String className = getClassName(typeName);
    String packageName = getPackageName(typeName);
    if (packageName.length() > 0) {
      // use "com.example.foo." + "Foo"
      typeNameLit = new JBinaryOperation(program, null,
          program.getTypeJavaLangString(), JBinaryOperator.ADD,
          program.getLiteralString(packageName),
          program.getLiteralString(className));
    } else {
      // no package name could be split, just use the full name
      typeNameLit = program.getLiteralString(typeName);
    }
    return typeNameLit;
  }

  private JExpression classObjectAllocation;
  private final JType refType;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JClassLiteral(JProgram program, JType type) {
    super(program);
    refType = type;

    JNewInstance classAlloc = new JNewInstance(program, null,
        program.getTypeJavaLangClass());
    JMethodCall call = new JMethodCall(program, null, classAlloc,
        program.getIndexedMethod("Class.Class"));

    call.getArgs().add(getTypeNameLit(program, type));
    call.getArgs().add(getTypeBitsLit(program, type));

    // TODO: enums

    classObjectAllocation = call;
  }

  public JType getRefType() {
    return refType;
  }

  public JType getType() {
    return classObjectAllocation.getType();
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      classObjectAllocation = visitor.accept(classObjectAllocation);
    }
    visitor.endVisit(this, ctx);
  }
}
