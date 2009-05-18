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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.javac.BinaryTypeReferenceRestrictionsChecker.BinaryTypeReferenceSite;

import junit.framework.TestCase;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryField;
import org.eclipse.jdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.jdt.internal.compiler.env.IBinaryNestedType;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;

import java.io.File;
import java.util.List;

/**
 * 
 */
public class BinaryTypeReferenceRestrictionsCheckerTest extends TestCase {
  /**
   * Mocks a binary type
   */
  static class MockBinaryType implements IBinaryType {
    private final String qualifiedTypeName;

    MockBinaryType(String typeName) {
      this.qualifiedTypeName = typeName;
    }

    public IBinaryAnnotation[] getAnnotations() {
      return null;
    }

    public char[] getEnclosingTypeName() {
      return null;
    }

    public IBinaryField[] getFields() {
      return null;
    }

    public char[] getFileName() {
      return (qualifiedTypeName.replace('.', File.separatorChar) + ".java").toCharArray();
    }

    public char[] getGenericSignature() {
      return qualifiedTypeName.toCharArray();
    }

    public char[][] getInterfaceNames() {
      return null;
    }

    public IBinaryNestedType[] getMemberTypes() {
      return null;
    }

    public IBinaryMethod[] getMethods() {
      return null;
    }

    public char[][][] getMissingTypeNames() {
      return null;
    }

    public int getModifiers() {
      return 0;
    }

    public char[] getName() {
      return qualifiedTypeName.toCharArray();
    }

    public char[] getSourceName() {
      return null;
    }

    public char[] getSuperclassName() {
      return null;
    }

    public long getTagBits() {
      return 0;
    }

    public boolean isAnonymous() {
      return false;
    }

    public boolean isBinaryType() {
      return true;
    }

    public boolean isLocal() {
      return false;
    }

    public boolean isMember() {
      return false;
    }

    public char[] sourceFileName() {
      return null;
    }
  }

  private static final String BINARY_TYPE_NAME = "BinaryType";

  private static TypeReference createMockBinaryTypeReference(
      BinaryTypeBinding binaryTypeBinding) {
    SingleTypeReference singleTypeReference = new SingleTypeReference(null, 0);
    singleTypeReference.resolvedType = binaryTypeBinding;
    return singleTypeReference;
  }

  private static LookupEnvironment createMockLookupEnvironment() {
    LookupEnvironment lookupEnvironment = new LookupEnvironment(null, null,
        null, null);
    lookupEnvironment.globalOptions = new CompilerOptions();
    return lookupEnvironment;
  }

  public void testCheck() {
    // fail("Not yet implemented");
  }

  /**
   * Creates a mock {@link CompilationUnitDeclaration} that has binary type
   * references in a superclass reference, in a method return type, in an
   * annotation and in a local variable declaration. It then checks that the we
   * find all of these locations except for the one used in an annotation.
   */
  public void testFindAllBinaryTypeReferenceSites() {
    CompilationResult compilationResult = new CompilationResult(
        "TestCompilationUnit.java".toCharArray(), 0, 0, 0);
    CompilationUnitDeclaration cud = new CompilationUnitDeclaration(null,
        compilationResult, 1);
    LookupEnvironment lookupEnvironment = createMockLookupEnvironment();
    cud.scope = new CompilationUnitScope(cud, lookupEnvironment);

    TypeDeclaration typeDeclaration = new TypeDeclaration(compilationResult);
    typeDeclaration.scope = new ClassScope(cud.scope, null);
    typeDeclaration.staticInitializerScope = new MethodScope(
        typeDeclaration.scope, null, false);
    cud.types = new TypeDeclaration[] {typeDeclaration};

    BinaryTypeBinding binaryTypeBinding = new BinaryTypeBinding(null,
        new MockBinaryType(BINARY_TYPE_NAME), lookupEnvironment);
    typeDeclaration.superclass = createMockBinaryTypeReference(binaryTypeBinding);

    MethodDeclaration methodDeclaration = new MethodDeclaration(
        compilationResult);
    methodDeclaration.scope = new MethodScope(typeDeclaration.scope, null,
        false);
    methodDeclaration.returnType = createMockBinaryTypeReference(binaryTypeBinding);

    LocalDeclaration localDeclaration = new LocalDeclaration(null, 0, 0);
    localDeclaration.type = createMockBinaryTypeReference(binaryTypeBinding);
    methodDeclaration.statements = new Statement[] {localDeclaration};

    SingleMemberAnnotation annotation = new SingleMemberAnnotation(
        createMockBinaryTypeReference(binaryTypeBinding), 0);
    annotation.memberValue = annotation.type;
    typeDeclaration.annotations = new Annotation[] {annotation};

    typeDeclaration.methods = new AbstractMethodDeclaration[] {methodDeclaration};

    /*
     * Check that we find binary type references in the following expected
     * locations.
     */
    Expression[] expectedExpressions = new Expression[] {
        typeDeclaration.superclass, methodDeclaration.returnType,
        localDeclaration.type};

    List<BinaryTypeReferenceSite> binaryTypeReferenceSites = BinaryTypeReferenceRestrictionsChecker.findAllBinaryTypeReferenceSites(cud);
    assertEquals(expectedExpressions.length, binaryTypeReferenceSites.size());
    for (int i = 0; i < binaryTypeReferenceSites.size(); ++i) {
      BinaryTypeReferenceSite binaryTypeReferenceSite = binaryTypeReferenceSites.get(i);
      assertSame(binaryTypeBinding,
          binaryTypeReferenceSite.getBinaryTypeBinding());
      assertSame(expectedExpressions[i],
          binaryTypeReferenceSite.getExpression());
    }
  }

  public void testFormatBinaryTypeRefErrorMessage() {
    String expectedMessage = "No source code is available for type MyClass; did you forget to inherit a required module?";
    String actualMessage = BinaryTypeReferenceRestrictionsChecker.formatBinaryTypeRefErrorMessage("MyClass");
    assertEquals(expectedMessage, actualMessage);
  }
}
