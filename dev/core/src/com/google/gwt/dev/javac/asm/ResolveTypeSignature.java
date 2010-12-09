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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.asm.signature.SignatureVisitor;
import com.google.gwt.dev.javac.Resolver;
import com.google.gwt.dev.javac.TypeParameterLookup;
import com.google.gwt.dev.javac.typemodel.JClassType;
import com.google.gwt.dev.javac.typemodel.JGenericType;
import com.google.gwt.dev.javac.typemodel.JParameterizedType;
import com.google.gwt.dev.javac.typemodel.JRealClassType;
import com.google.gwt.dev.javac.typemodel.JTypeParameter;
import com.google.gwt.dev.javac.typemodel.JWildcardType;
import com.google.gwt.dev.util.Name;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolve a single parameterized type.
 */
public class ResolveTypeSignature extends EmptySignatureVisitor {

  private final Resolver resolver;
  private final Map<String, JRealClassType> binaryMapper;
  private final TreeLogger logger;
  private final JType[] returnTypeRef;
  private final TypeParameterLookup lookup;
  private final char wildcardMatch;
  private final JClassType enclosingClass;

  private JClassType outerClass;
  private final List<JType[]> args = new ArrayList<JType[]>();
  private int arrayDepth = 0;

  /**
   * Resolve a parameterized type.
   * 
   * @param resolver
   * @param binaryMapper
   * @param logger
   * @param returnTypeRef "pointer" to return location, ie. 1-element array
   * @param lookup
   * @param enclosingClass
   */
  public ResolveTypeSignature(Resolver resolver,
      Map<String, JRealClassType> binaryMapper, TreeLogger logger,
      JType[] returnTypeRef, TypeParameterLookup lookup,
      JClassType enclosingClass) {
    this(resolver, binaryMapper, logger, returnTypeRef, lookup, enclosingClass,
        '=');
  }

  public ResolveTypeSignature(Resolver resovler,
      Map<String, JRealClassType> binaryMapper, TreeLogger logger,
      JType[] returnTypeRef, TypeParameterLookup lookup,
      JClassType enclosingClass, char wildcardMatch) {
    this.resolver = resovler;
    this.binaryMapper = binaryMapper;
    this.logger = logger;
    this.returnTypeRef = returnTypeRef;
    this.lookup = lookup;
    this.enclosingClass = enclosingClass;
    this.wildcardMatch = wildcardMatch;
  }

  @Override
  public SignatureVisitor visitArrayType() {
    ++arrayDepth;
    return this;
  }

  @Override
  public void visitBaseType(char descriptor) {
    switch (descriptor) {
      case 'V':
        returnTypeRef[0] = JPrimitiveType.VOID;
        break;
      case 'B':
        returnTypeRef[0] = JPrimitiveType.BYTE;
        break;
      case 'J':
        returnTypeRef[0] = JPrimitiveType.LONG;
        break;
      case 'Z':
        returnTypeRef[0] = JPrimitiveType.BOOLEAN;
        break;
      case 'I':
        returnTypeRef[0] = JPrimitiveType.INT;
        break;
      case 'S':
        returnTypeRef[0] = JPrimitiveType.SHORT;
        break;
      case 'C':
        returnTypeRef[0] = JPrimitiveType.CHAR;
        break;
      case 'F':
        returnTypeRef[0] = JPrimitiveType.FLOAT;
        break;
      case 'D':
        returnTypeRef[0] = JPrimitiveType.DOUBLE;
        break;
      default:
        throw new IllegalStateException("Unrecognized base type " + descriptor);
    }
    // this is the last visitor called on this visitor
    visitEnd();
  }

  @Override
  public void visitClassType(String internalName) {
    assert Name.isInternalName(internalName);
    outerClass = enclosingClass;
    JRealClassType classType = binaryMapper.get(internalName);
    // TODO(jat): failures here are likely binary-only annotations or local
    // classes that have been elided from TypeOracle -- what should we do in
    // those cases? Currently we log an error and replace them with Object,
    // but we may can do something better.
    boolean resolveSuccess = classType == null ? false : resolver.resolveClass(
        logger, classType);
    returnTypeRef[0] = classType;
    if (!resolveSuccess || returnTypeRef[0] == null) {
      logger.log(TreeLogger.ERROR, "Unable to resolve class " + internalName);
      // Replace bound with Object if we can't resolve the class.
      returnTypeRef[0] = resolver.getTypeOracle().getJavaLangObject();
    }
  }

  @Override
  public void visitEnd() {
    if (returnTypeRef[0] == null) {
      return;
    }
    resolveGenerics();
  }

  @Override
  public void visitInnerClassType(String innerName) {
    // Called after visitClass has already been called, and we will
    // successively refine the class by going into its inner classes.
    assert returnTypeRef[0] != null;
    resolveGenerics();
    outerClass = (JClassType) returnTypeRef[0];
    JClassType searchClass = outerClass;
    try {
      JParameterizedType pt = searchClass.isParameterized();
      if (pt != null) {
        searchClass = pt.getBaseType();
      }
      returnTypeRef[0] = searchClass.getNestedType(innerName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to resolve inner class " + innerName
          + " in " + searchClass, e);
    }
  }

  @Override
  public void visitTypeArgument() {
    JType[] arg = new JType[1]; // This could be int[] for example
    arg[0] = resolver.getTypeOracle().getWildcardType(
        JWildcardType.BoundType.UNBOUND,
        resolver.getTypeOracle().getJavaLangObject());
    args.add(arg);
  }

  @Override
  public SignatureVisitor visitTypeArgument(char wildcard) {
    JType[] arg = new JType[1];
    args.add(arg);
    // TODO(jat): should we pass enclosingClass here instead of null?
    // not sure what the enclosing class of a type argument means, but
    // I haven't found a case where it is actually used while processing
    // the type argument.
    return new ResolveTypeSignature(resolver, binaryMapper, logger, arg,
        lookup, null, wildcard);
  }

  @Override
  public void visitTypeVariable(String name) {
    returnTypeRef[0] = lookup.lookup(name);
    // this is the last visitor called on this visitor
    visitEnd();
  }

  /**
   * Merge the bounds from the declared type parameters into the type arguments
   * for this type if necessary.
   * 
   * <pre>
   * Example:
   * class Foo<T extends Bar> ...
   * 
   * Foo<?> foo
   * 
   * foo needs to have bounds ? extends Bar.
   * </pre>
   * 
   * <p>
   * Currently we only deal with unbound wildcards as above, which matches
   * existing TypeOracleMediator behavior. However, this may need to be
   * extended.
   * 
   * @param typeParams
   * @param typeArgs
   */
  private void mergeTypeParamBounds(JTypeParameter[] typeParams,
      JClassType[] typeArgs) {
    int n = typeArgs.length;
    for (int i = 0; i < n; ++i) {
      JWildcardType wildcard = typeArgs[i].isWildcard();
      // right now we only replace Foo<?> with the constraints defined on the
      // definition (which appears to match the existing TypeOracleMediator)
      // but other cases may need to be handled.
      if (wildcard != null
          && wildcard.getBoundType() == BoundType.UNBOUND
          && wildcard.getBaseType() == resolver.getTypeOracle().getJavaLangObject()
          && typeParams[i].getBaseType() != null) {
        typeArgs[i] = resolver.getTypeOracle().getWildcardType(
            BoundType.UNBOUND, typeParams[i].getBaseType());
      }
    }
  }

  private JType resolveGeneric(JType type, JClassType outer,
      JClassType[] typeArgs) {
    JGenericType genericType = (JGenericType) type.isGenericType();
    if (genericType != null) {
      int actual = typeArgs.length;
      JTypeParameter[] typeParams = genericType.getTypeParameters();
      int expected = typeParams.length;
      if (actual == 0 && expected > 0) {
        // If no type parameters were supplied, this is a raw type usage.
        type = genericType.getRawType();
      } else {
        if (actual != expected) {
          throw new IllegalStateException("Incorrect # of type parameters to "
              + genericType.getQualifiedBinaryName() + ": expected " + expected
              + ", actual=" + actual);
        }
        JClassType genericEnc = genericType.getEnclosingType();
        if (outer == null && genericEnc != null) {
          // Sometimes the signature is like Foo$Bar<H> even if Foo is a
          // generic class. The cases I have seen are where Foo's type
          // parameter is also named H and has the same bounds. That
          // manifests itself as getting visitClassType("Foo$Bar") and
          // then VisitTypeArgument/etc, rather than the usual
          // visitClassType("Foo"), visitTypeArgument/etc,
          // visitInnerClass("Bar"), visitTypeArgument/etc.
          //
          // So, in this case we have to build our own chain of enclosing
          // classes here, properly parameterizing any generics along the
          // way.
          // TODO(jat): more testing to validate this assumption
          JClassType[] outerArgs = null;
          JGenericType genericEncGeneric = genericEnc.isGenericType();
          if (genericEncGeneric != null) {
            JTypeParameter[] encTypeParams = genericEncGeneric.getTypeParameters();
            int n = encTypeParams.length;
            outerArgs = new JClassType[n];
            for (int i = 0; i < n; ++i) {
              outerArgs[i] = lookup.lookup(encTypeParams[i].getName());
              if (outerArgs[i] == null) {
                // check to see if our current type has a parameter of the same
                // name, and use it if so.
                for (int j = 0; j < expected; ++j) {
                  if (typeParams[j].getName().equals(encTypeParams[j].getName())) {
                    outerArgs[i] = typeArgs[j];
                    break;
                  }
                }
              }
              assert outerArgs[i] != null : "Unable to resolve type parameter "
                  + encTypeParams[i].getName() + " in enclosing type "
                  + genericEnc + " of type " + genericType;
            }
          }
          outer = (JClassType) resolveGeneric(genericEnc, null, outerArgs);
        }
        try {
          mergeTypeParamBounds(typeParams, typeArgs);
          type = resolver.getTypeOracle().getParameterizedType(genericType,
              outer, typeArgs);
        } catch (IllegalArgumentException e) {
          // Can't use toString on typeArgs as they aren't completely built
          // yet, so we have to roll our own.
          StringBuilder buf = new StringBuilder();
          buf.append("Unable to build parameterized type ");
          buf.append(genericType);
          String prefix = " with args <";
          for (JClassType typeArg : typeArgs) {
            buf.append(prefix).append(typeArg.getName());
            prefix = ", ";
          }
          if (", ".equals(prefix)) {
            buf.append('>');
          }
          logger.log(TreeLogger.ERROR, buf.toString(), e);
          type = genericType.getRawType();
        }
      }
    }
    return type;
  }

  private void resolveGenerics() {
    JGenericType genericType = (JGenericType) returnTypeRef[0].isGenericType();
    if (genericType != null) {
      int actual = args.size();
      JClassType[] typeArgs = new JClassType[actual];
      for (int i = 0; i < actual; ++i) {
        JType type = args.get(i)[0];
        if (!(type instanceof JClassType)) {
          logger.log(TreeLogger.ERROR, "Parameterized type argument is " + type
              + ", expected reference type");
        } else {
          typeArgs[i] = (JClassType) type;
        }
      }
      returnTypeRef[0] = resolveGeneric(genericType, outerClass, typeArgs);
      args.clear();
    }
    for (int i = 0; i < arrayDepth; ++i) {
      returnTypeRef[0] = resolver.getTypeOracle().getArrayType(returnTypeRef[0]);
    }
    switch (wildcardMatch) {
      case '=':
        // nothing to do for an exact match
        break;
      case '*':
        returnTypeRef[0] = resolver.getTypeOracle().getWildcardType(
            JWildcardType.BoundType.UNBOUND, (JClassType) returnTypeRef[0]);
        break;
      case '+':
        // ? extends T
        returnTypeRef[0] = resolver.getTypeOracle().getWildcardType(
            JWildcardType.BoundType.EXTENDS, (JClassType) returnTypeRef[0]);
        break;
      case '-':
        // ? super T
        returnTypeRef[0] = resolver.getTypeOracle().getWildcardType(
            JWildcardType.BoundType.SUPER, (JClassType) returnTypeRef[0]);
        break;
    }
    if (returnTypeRef[0] instanceof JClassType) {
      // Only JClassTypes can be an outer class
      outerClass = (JClassType) returnTypeRef[0];
    }
  }
}
