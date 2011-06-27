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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.javac.typemodel.JAbstractMethod;
import com.google.gwt.dev.javac.typemodel.JClassType;
import com.google.gwt.dev.javac.typemodel.JParameter;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.core.util.CodeSnippetParsingUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Methods to do direct parsing of Java source -- currently the only uses are
 * for finding actual method parameter names on request.
 */
public class JavaSourceParser {

  public static JClassType getTopmostType(JClassType type) {
    while (type.getEnclosingType() != null) {
      type = type.getEnclosingType();
    }
    return type;
  }

  /**
   * Spits a binary name into a series of char arrays, corresponding to
   * enclosing classes.
   * 
   * <p>
   * For example, {@code test.Foo$Bar} gets expanded to [[Foo],[Bar]]. Note that
   * the package is not included.
   * 
   * @param binaryName class name in binary form (ie, test.Foo$Bar)
   * @return list of char arrays of class names, from outer to inner
   */
  // @VisibleForTesting
  static List<char[]> getClassChain(String binaryName) {
    ArrayList<char[]> result = new ArrayList<char[]>();
    String className = BinaryName.getClassName(binaryName);
    int idx;
    while ((idx = className.indexOf('$')) >= 0) {
      result.add(className.substring(0, idx).toCharArray());
      className = className.substring(idx + 1);
    }
    result.add(className.toCharArray());
    return result;
  }

  /**
   * Find a matching method in a type.
   * 
   * @param type JDT method
   * @param jMethod TypeOracle method object to find
   * @return method declaration or null if not found
   */
  private static AbstractMethodDeclaration findMethod(TypeDeclaration type,
      JAbstractMethod jMethod) {
    List<AbstractMethodDeclaration> candidates = findNamedMethods(type,
        jMethod.getName());
    if (candidates.size() == 0) {
      return null;
    }
    if (candidates.size() == 1) {
      return candidates.get(0);
    }
    nextCandidate : for (AbstractMethodDeclaration candidate : candidates) {
      int n = candidate.arguments == null ? 0 : candidate.arguments.length;
      JParameter[] params = jMethod.getParameters();
      if (n != params.length) {
        continue;
      }
      for (int i = 0; i < n; ++i) {
        if (!typeMatches(candidate.arguments[i].type, params[i].getType())) {
          continue nextCandidate;
        }
      }
      return candidate;
    }
    return null;
  }

  /**
   * Find all methods which have the requested name.
   * 
   * <p>
   * {@code <clinit>} is not supported.
   * 
   * @param type JDT type declaration
   * @param name name of methods to find
   * @return list of matching methods
   */
  private static List<AbstractMethodDeclaration> findNamedMethods(
      TypeDeclaration type, String name) {
    List<AbstractMethodDeclaration> matching = new ArrayList<AbstractMethodDeclaration>();
    boolean isCtor = "<init>".equals(name);
    char[] nameArray = name.toCharArray();
    for (AbstractMethodDeclaration method : type.methods) {
      if ((isCtor && method.isConstructor())
          || (!isCtor && !method.isConstructor() && !method.isClinit() && Arrays.equals(
              method.selector, nameArray))) {
        matching.add(method);
      }
    }
    return matching;
  }

  /**
   * Find a particular type in a compilation unit.
   * 
   * @param unit JDT cud
   * @param binaryName binary name of the type to find (ie, test.Foo$Bar)
   * @return type declaration or null if not found
   */
  private static TypeDeclaration findType(CompilationUnitDeclaration unit,
      String binaryName) {
    List<char[]> classChain = getClassChain(binaryName);
    TypeDeclaration curType = findType(unit.types, classChain.get(0));
    for (int i = 1; i < classChain.size(); ++i) {
      if (curType == null) {
        return null;
      }
      curType = findType(curType.memberTypes, classChain.get(i));
    }
    return curType;
  }

  /**
   * Find one type by name in a array of types.
   * 
   * @param types array of types
   * @param name name of type to find
   * @return matching type or null if not found
   */
  private static TypeDeclaration findType(TypeDeclaration[] types, char[] name) {
    for (TypeDeclaration type : types) {
      if (Arrays.equals(name, type.name)) {
        return type;
      }
    }
    return null;
  }

  /**
   * Parse Java source.
   * 
   * @param javaSource String containing Java source to parse
   * @return a CompilationUnitDeclaration or null if parsing failed
   */
  private static CompilationUnitDeclaration parseJava(String javaSource) {
    CodeSnippetParsingUtil parsingUtil = new CodeSnippetParsingUtil();
    CompilerOptions options = new CompilerOptions();
    options.complianceLevel = ClassFileConstants.JDK1_5;
    options.sourceLevel = ClassFileConstants.JDK1_5;
    CompilationUnitDeclaration unit = parsingUtil.parseCompilationUnit(
        javaSource.toString().toCharArray(), options.getMap(), true);
    if (unit.compilationResult().hasProblems()) {
      return null;
    }
    return unit;
  }

  /**
   * Compares an unresolved JDT type to a TypeOracle type to see if they match.
   * 
   * @param jdtType
   * @param toType
   * @return true if the two type objects resolve to the same
   */
  private static boolean typeMatches(TypeReference jdtType, JType toType) {
    List<char[]> toNameComponents = getClassChain(toType.getQualifiedBinaryName());
    int toLen = toNameComponents.size();
    char[][] jdtNameComponents = jdtType.getTypeName();
    int jdtLen = jdtNameComponents.length;
    int maxToCompare = Math.min(toLen, jdtLen);

    // compare from the end
    for (int i = 1; i <= maxToCompare; ++i) {
      if (!Arrays.equals(jdtNameComponents[jdtLen - i],
          toNameComponents.get(toLen - i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Map of top-level classes to the source file associated with it.
   */
  private WeakHashMap<JClassType, Resource> classSources = new WeakHashMap<JClassType, Resource>();

  /**
   * Cache of top-level classes to JDT CUDs associated with them.
   * 
   * <p>
   * CUDs may be discarded at any time (with a performance cost if they are
   * needed again), and are held in SoftReferences to allow GC to dump them.
   */
  private WeakHashMap<JClassType, SoftReference<CompilationUnitDeclaration>> cudCache = new WeakHashMap<JClassType, SoftReference<CompilationUnitDeclaration>>();

  /**
   * Add a source file associated with the outermost enclosing class.
   * 
   * @param topType
   * @param source
   * 
   *          TODO: reduce visibility
   */
  public synchronized void addSourceForType(JClassType topType, Resource source) {
    classSources.put(topType, source);
  }

  /**
   * Return the real argument names for a given method from the source.
   * 
   * @param method method to lookup parameter names for
   * @return array of argument names or null if no source is available
   */
  public synchronized String[] getArguments(JAbstractMethod method) {
    JClassType type = method.getEnclosingType();
    JClassType topType = getTopmostType(type);
    CompilationUnitDeclaration cud = getCudForTopLevelType(topType);
    if (cud == null) {
      return null;
    }
    TypeDeclaration jdtType = findType(cud, type.getQualifiedBinaryName());
    if (jdtType == null) {
      // TODO(jat): any thing else to do here?
      return null;
    }
    AbstractMethodDeclaration jdtMethod = findMethod(jdtType, method);
    if (jdtMethod == null) {
      // TODO(jat): any thing else to do here?
      return null;
    }
    int n = jdtMethod.arguments.length;
    String[] argNames = new String[n];
    for (int i = 0; i < n; ++i) {
      argNames[i] = String.valueOf(jdtMethod.arguments[i].name);
    }
    return argNames;
  }

  /**
   * Finds a JDT CUD for a given top-level type, generating it if needed.
   * 
   * @param topType top-level JClassType
   * @return CUD instance or null if no source found
   */
  private synchronized CompilationUnitDeclaration getCudForTopLevelType(
      JClassType topType) {
    CompilationUnitDeclaration cud = null;
    if (cudCache.containsKey(topType)) {
      SoftReference<CompilationUnitDeclaration> cudRef = cudCache.get(topType);
      if (cudRef != null) {
        cud = cudRef.get();
      }
    }
    if (cud == null) {
      Resource classSource = classSources.get(topType);
      String source = null;
      if (classSource != null) {
        try {
          InputStream stream = classSource.openContents();
          source = Util.readStreamAsString(stream);
        } catch (IOException ex) {
          throw new InternalCompilerException("Problem reading resource: "
              + classSource.getLocation(), ex);
        }
      }
      if (source == null) {
        // cache negative result so we don't try again
        cudCache.put(topType, null);
      } else {
        cud = parseJava(source);
        cudCache.put(topType,
            new SoftReference<CompilationUnitDeclaration>(cud));
      }
    }
    return cud;
  }
}
