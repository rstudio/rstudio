/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.Correlation.Literal;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JEnumType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JField.Disposition;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRuntimeTypeReference;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Create fields to represent the mechanical implementation of class literals.
 * Must be done after all class literals are created, but before optimizations
 * begin. {@link ControlFlowAnalyzer} depends on this.
 * <p>
 * Class literals are implemented as static field references. The static fields
 * are all put into the special com.google.gwt.lang.ClassLiteralHolder class.
 * Ordinarily, accessing one of these fields would trigger a clinit to run, but
 * we've special-cased class literal fields to evaluate as top-level code before
 * the application starts running to avoid the clinit.
 *
 * Class literal factory methods are responsible for installing references
 * to themselves on the Object.clazz field of their JS runtime prototype
 * since getClass() is no longer an overridden method.  Prototypes can be
 * looked up via 'typeId' from the global prototypesByTypeId object, and so each
 * class literal factory method is passed the typeId of its type.
 * <p>
 */
public class ImplementClassLiteralsAsFields {

  private class NormalizeVisitor extends JModVisitor {
    @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      JField field = resolveClassLiteralField(x.getRefType());
      x.setField(field);
    }
  }

  public static void exec(JProgram program) {
    Event normalizerEvent = SpeedTracerLogger.start(CompilerEventType.NORMALIZER);
    new ImplementClassLiteralsAsFields(program).execImpl();
    normalizerEvent.end();
  }

  private static String createIdent(JMethod method) {
    StringBuilder sb = new StringBuilder();
    sb.append(method.getEnclosingType().getName());
    sb.append("::");
    sb.append(method.getName());
    sb.append('(');
    for (JType type : method.getOriginalParamTypes()) {
      sb.append(type.getJsniSignatureName());
    }
    sb.append(')');
    return sb.toString();
  }

  private static String getClassName(String fullName) {
    int pos = fullName.lastIndexOf(".");
    return fullName.substring(pos + 1);
  }

  private static String getPackageName(String fullName) {
    int pos = fullName.lastIndexOf(".");
    return fullName.substring(0, pos + 1);
  }

  private final Map<JType, JField> classLiteralFields = new IdentityHashMap<JType, JField>();
  private final JMethodBody classLiteralHolderClinitBody;
  private final JProgram program;
  private final JClassType typeClassLiteralHolder;

  private ImplementClassLiteralsAsFields(JProgram program) {
    this.program = program;
    this.typeClassLiteralHolder = program.getTypeClassLiteralHolder();
    this.classLiteralHolderClinitBody =
        (JMethodBody) typeClassLiteralHolder.getClinitMethod().getBody();
    assert program.getDeclaredTypes().contains(typeClassLiteralHolder);
  }

  /**
   * Create an expression that will evaluate, at run time, to the class literal.
   * Causes recursive literal create (super type, array element type). Examples:
   *
   * Class:
   *
   * <pre>
   * Class.createForClass("java.lang.", "Object", /JRuntimeTypeReference/"java.lang.Object", null)
   * Class.createForClass("java.lang.", "Exception", /JRuntimeTypeReference/"java.lang.Exception", Throwable.class)
   * </pre>
   *
   * Interface:
   *
   * <pre>
   * Class.createForInterface(&quot;java.lang.&quot;, &quot;Comparable&quot;)
   * </pre>
   *
   * Primitive:
   *
   * <pre>
   * Class.createForPrimitive(&quot;&quot;, &quot;int&quot;, &quot; I&quot;)
   * </pre>
   *
   * Array:
   *
   * <pre>
   * Class.createForArray("", "[I", /JRuntimeTypeReference/"com.google.gwt.lang.Array", int.class)
   * Class.createForArray("[Lcom.example.", "Foo;", /JRuntimeTypeReference/"com.google.gwt.lang.Array", Foo.class)
   * </pre>
   *
   * Enum:
   *
   * <pre>
   * Class.createForEnum("com.example.", "MyEnum", /JRuntimeTypeReference/"com.example.MyEnum", Enum.class,
   *     public static MyEnum[] values(), public static MyEnum valueOf(String name))
   * </pre>
   *
   * Enum subclass:
   *
   * <pre>
   * Class.createForEnum("com.example.", "MyEnum$1", /JRuntimeTypeReference/"com.example.MyEnum$1", MyEnum.class,
   *     null, null))
   * </pre>
   */
  private JMethodCall computeClassObjectAllocation(SourceInfo info, JType type) {
    String typeName = getTypeName(type);

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
    JStringLiteral packageName = program.getStringLiteral(info, getPackageName(typeName));
    JStringLiteral className = program.getStringLiteral(info, getClassName(typeName));
    call.addArgs(packageName, className);

    if (type instanceof JArrayType || type instanceof JClassType) {
      // Add a runtime type reference.
      call.addArg(new JRuntimeTypeReference(info, program.getTypeJavaLangObject(),
          (JReferenceType) type));
    } else if (type instanceof JPrimitiveType) {
      // And give primitive types an illegal, though meaningful, value
      call.addArg(program.getStringLiteral(info, " " + type.getJavahSignatureName()));
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
        if (JProgram.isJsInterfacePrototype(classType)) {
          JInterfaceType jsInterface = program.typeOracle.getNearestJsInterface(classType, true);
          assert jsInterface != null;
          superclassLiteral = createDependentClassLiteral(info, jsInterface);
        } else {
          superclassLiteral = createDependentClassLiteral(info, classType.getSuperClass());
        }
      } else {
        superclassLiteral = JNullLiteral.INSTANCE;
      }

      call.addArg(superclassLiteral);

      if (classType instanceof JEnumType) {
        JEnumType enumType = (JEnumType) classType;
        JMethod valuesMethod = null;
        JMethod valueOfMethod = null;
        for (JMethod methodIt : enumType.getMethods()) {
          if (methodIt.isStatic()) {
            if (methodIt.getSignature().startsWith("values()")) {
              valuesMethod = methodIt;
            } else if (methodIt.getSignature().startsWith("valueOf(Ljava/lang/String;)")) {
              valueOfMethod = methodIt;
            }
          }
        }
        if (valuesMethod == null) {
          throw new InternalCompilerException("Could not find enum values() method");
        }
        if (valueOfMethod == null) {
          throw new InternalCompilerException("Could not find enum valueOf() method");
        }
        call.addArg(new JsniMethodRef(info, createIdent(valuesMethod), valuesMethod, program
            .getJavaScriptObject()));
        call.addArg(new JsniMethodRef(info, createIdent(valueOfMethod), valueOfMethod, program
            .getJavaScriptObject()));
      } else if (isEnumOrSubclass) {
        // A subclass of an enum class
        call.addArg(JNullLiteral.INSTANCE);
        call.addArg(JNullLiteral.INSTANCE);
      }
    } else if (type instanceof JArrayType) {
      JArrayType arrayType = (JArrayType) type;
      JClassLiteral componentLiteral =
          createDependentClassLiteral(info, arrayType.getElementType());
      call.addArg(componentLiteral);
    } else {
      assert (type instanceof JInterfaceType || type instanceof JPrimitiveType);
    }
    assert call.getArgs().size() == method.getParams().size() : "Argument / param mismatch "
        + call.toString() + " versus " + method.toString();
    return call;
  }

  private JClassLiteral createDependentClassLiteral(SourceInfo info, JType type) {
    JClassLiteral classLiteral = new JClassLiteral(info.makeChild(), type);
    JField field = resolveClassLiteralField(classLiteral.getRefType());
    classLiteral.setField(field);
    return classLiteral;
  }

  private void execImpl() {
    NormalizeVisitor visitor = new NormalizeVisitor();
    visitor.accept(program);
    // TODO(rluble): This is kind of hacky. It would be more general purpose to create a class
    // literal implementation for every class and let unused ones be pruned by optimization.
    // Instead we're prematurely optimizing.
    if (!program.typeOracle.hasWholeWorldKnowledge()) {
      for (JType type : program.getDeclaredTypes()) {
        resolveClassLiteralField(type);
      }
    }
    program.recordClassLiteralFields(classLiteralFields);
  }

  private String getTypeName(JType type) {
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

  private JType normalizeJsoType(JType type) {
    if (program.isJavaScriptObject(type)) {
      return program.getJavaScriptObject();
    }

    if (type instanceof JArrayType) {
      JArrayType aType = (JArrayType) type;
      if (program.isJavaScriptObject(aType.getLeafType())) {
        return program.getTypeArray(program.getJavaScriptObject(), aType.getDims());
      }
    }

    return type;
  }

  /**
   * Resolve a class literal field. Takes the form:
   *
   * <pre>
   * class ClassLiteralHolder {
   *   Class Ljava_lang_Object_2_classLit =
   *       Class.createForClass("java.lang.", "Object", /JNameOf/"java.lang.Object", null)
   * }
   * </pre>
   */
  private JField resolveClassLiteralField(JType type) {
    type = normalizeJsoType(type);
    JField field = classLiteralFields.get(type);
    if (field == null) {
      // Create the allocation expression FIRST since this may be recursive on
      // super type (this forces the super type classLit to be created first).
      SourceInfo info = type.getSourceInfo().makeChild();
      JMethodCall alloc = computeClassObjectAllocation(info, type);
      // Create a field in the class literal holder to hold the object.
      field =
          new JField(info, program.getClassLiteralName(type), typeClassLiteralHolder, program
              .getTypeJavaLangClass(), true, Disposition.FINAL);
      typeClassLiteralHolder.addField(field);
      info.addCorrelation(info.getCorrelator().by(Literal.CLASS));

      // Initialize the field.
      JFieldRef fieldRef = new JFieldRef(info, null, field, typeClassLiteralHolder);
      JDeclarationStatement decl = new JDeclarationStatement(info, fieldRef, alloc);
      classLiteralHolderClinitBody.getBlock().addStmt(decl);
      classLiteralFields.put(type, field);
    }
    return field;
  }
}
