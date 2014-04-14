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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.Attribute;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.FieldVisitor;
import com.google.gwt.dev.asm.MethodVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.util.Util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates string hashes for various purposes from walking bytecode.
 */
public class BytecodeSignatureMaker {

  /**
   * This visitor looks at methods and members to compute a signature. This is
   * intended for determining if a type needs to be recompiled if byte code it
   * depends on changes.
   *
   * At first, you'd think only public and protected members should be
   * considered, but the JSNI violator pattern means that even a change in a
   * private member might invalidate an access from another class.
   */
  private static class CompileDependencyVisitor extends ClassVisitor {
    /**
     * Mask to strip access bits we don't care about for computing the
     * signature.
     */
    private static final int ACCESS_FILTER_MASK =
        ~(Opcodes.ACC_DEPRECATED | Opcodes.ACC_NATIVE | Opcodes.ACC_STRICT
            | Opcodes.ACC_SYNCHRONIZED | Opcodes.ACC_SUPER | Opcodes.ACC_TRANSIENT | Opcodes.ACC_VOLATILE);

    private String header;
    private Map<String, String> fields = new HashMap<String, String>();
    private Map<String, String> methods = new HashMap<String, String>();

    public CompileDependencyVisitor() {
      super(Opcodes.ASM4);
    }

    public String getSignature() {
      return Util.computeStrongName(Util.getBytes(getRawString()));
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
      StringBuilder headerBuilder = new StringBuilder();
      // ignoring version
      headerBuilder.append(access & ACCESS_FILTER_MASK);
      headerBuilder.append(":");
      headerBuilder.append(name);
      if (signature != null) {
        headerBuilder.append(":");
        headerBuilder.append(signature);
      }
      if (superName != null) {
        headerBuilder.append(":");
        headerBuilder.append(superName);
      }
      if (interfaces != null) {
        Arrays.sort(interfaces);
        for (String iface : interfaces) {
          headerBuilder.append(":");
          headerBuilder.append(iface);
        }
      }
      header = headerBuilder.toString();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      // ignore
      return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {
      // ignore
    }

    @Override
    public void visitEnd() {
      // unused
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature,
        Object value) {
      StringBuilder fieldBuilder = new StringBuilder();
      // We don't care about synthetic fields
      if ((access & (Opcodes.ACC_SYNTHETIC)) == 0) {
        fieldBuilder.append(access & ACCESS_FILTER_MASK);
        fieldBuilder.append(":");
        fieldBuilder.append(name);
        fieldBuilder.append(":");
        fieldBuilder.append(desc);
        if (signature != null) {
          fieldBuilder.append(":");
          fieldBuilder.append(signature);
        }
        if (value != null) {
          fieldBuilder.append(":");
          fieldBuilder.append(value.toString());
        }
        fields.put(name, fieldBuilder.toString());
      }

      // ignoring annotations/attributes on the field.
      return null;
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
      // ignored
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
      // We don't care about synthetic methods
      if ((access & (Opcodes.ACC_SYNTHETIC)) == 0) {
        StringBuilder methodBuilder = new StringBuilder();
        methodBuilder.append(access & ACCESS_FILTER_MASK);
        methodBuilder.append(":");
        methodBuilder.append(name);
        methodBuilder.append(":");
        methodBuilder.append(desc);
        if (signature != null) {
          methodBuilder.append(":");
          methodBuilder.append(signature);
        }
        if (exceptions != null) {
          String[] sortedExceptions = exceptions;
          Arrays.sort(sortedExceptions);
          for (String exception : sortedExceptions) {
            methodBuilder.append(":");
            methodBuilder.append(exception);
          }
        }
        methods.put(name, methodBuilder.toString());
      }
      return null;
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
      // ignored
    }

    @Override
    public void visitSource(String source, String debug) {
      // ignore
    }

    private String getRawString() {
      StringBuilder signatureBuilder = new StringBuilder();
      signatureBuilder.append(header);
      signatureBuilder.append("|");

      // sort all fields and methods for a deterministic signature.
      String[] sortedFields = fields.values().toArray(new String[0]);
      Arrays.sort(sortedFields);
      for (String field : sortedFields) {
        signatureBuilder.append(field);
        signatureBuilder.append("|");
      }

      String[] sortedMethods = methods.values().toArray(new String[0]);
      Arrays.sort(sortedMethods);
      for (String method : sortedMethods) {
        signatureBuilder.append(method);
        signatureBuilder.append("|");
      }
      return signatureBuilder.toString();
    }
  }

  /**
   * Returns a hash computed from the non-private/non-synthetic members and
   * methods in a class.
   *
   * @param byteCode byte code for class to analyze.
   * @return a hex string representing an MD5 digest.
   */
  public static String getCompileDependencySignature(byte[] byteCode) {
    CompileDependencyVisitor v = visitCompileDependenciesInBytecode(byteCode);
    return v.getSignature();
  }

  /**
   * Returns a raw string used to compute the hash from the
   * non-synthetic members and methods in a class.
   *
   * @param byteCode byte code for class to analyze.
   * @return a human readable string of all public API fields
   */
  static String getCompileDependencyRawSignature(byte[] byteCode) {
    CompileDependencyVisitor v = visitCompileDependenciesInBytecode(byteCode);
    return v.getRawString();
  }

  private static CompileDependencyVisitor visitCompileDependenciesInBytecode(byte[] byteCode) {
    ClassReader reader = new ClassReader(byteCode);
    CompileDependencyVisitor v = new CompileDependencyVisitor();
    reader.accept(v, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return v;
  }

  private BytecodeSignatureMaker() {
  }
}
