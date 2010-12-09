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
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.asm.signature.SignatureVisitor;
import com.google.gwt.dev.javac.Resolver;
import com.google.gwt.dev.javac.TypeParameterLookup;
import com.google.gwt.dev.javac.typemodel.JClassType;
import com.google.gwt.dev.javac.typemodel.JRealClassType;
import com.google.gwt.dev.javac.typemodel.JTypeParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Signature visitor that resolves all the type variables and their bounds for a
 * given class.
 */
public class ResolveClassSignature extends EmptySignatureVisitor {

  private final Map<String, JRealClassType> binaryMapper;
  private ArrayList<JType[]> bounds = null;
  private JTypeParameter currentParam = null;
  private final List<JType[]> interfaces = new ArrayList<JType[]>();
  private final TreeLogger logger;

  private final TypeParameterLookup lookup;
  private final Resolver resolver;
  private final JType[] superClass = new JType[1];
  private final JRealClassType type;

  public ResolveClassSignature(Resolver resolver,
      Map<String, JRealClassType> binaryMapper, TreeLogger logger,
      JRealClassType type, TypeParameterLookup lookup) {
    this.resolver = resolver;
    this.binaryMapper = binaryMapper;
    this.logger = logger;
    this.type = type;
    this.lookup = lookup;
  }

  public void finish() {
    if (currentParam != null) {
      int n = bounds.size();
      JClassType[] boundTypes = new JClassType[n];
      for (int i = 0; i < n; ++i) {
        boundTypes[i] = (JClassType) bounds.get(i)[0];
      }
      currentParam.setBounds(boundTypes);
      currentParam = null;
      // TODO(jat): remove after debugging phase
      bounds = null;
    }
    if (superClass[0] != null) {
      if (type.isInterface() != null) {
        // The generic signature contains a superclass for interfaces,
        // but TypeOracle doesn't like that -- verify that we were
        // told Object is the superclass and ignore it.
        assert superClass[0].equals(resolver.getTypeOracle().getJavaLangObject());
      } else {
        resolver.setSuperClass(type, (JClassType) superClass[0]);
      }
      superClass[0] = null;
    }
    for (JType[] intfRef : interfaces) {
      if (intfRef[0] != null) {
        resolver.addImplementedInterface(type, (JClassType) intfRef[0]);
      }
    }
    interfaces.clear();
  }

  @Override
  public SignatureVisitor visitArrayType() {
    throw new IllegalStateException(
        "visitArrayType called on ResolveClassTypeVariables");
  }

  @Override
  public SignatureVisitor visitClassBound() {
    JType[] bound = new JType[1];
    bounds.add(bound);
    return new ResolveTypeSignature(resolver, binaryMapper, logger, bound,
        lookup, null);
  }

  @Override
  public void visitFormalTypeParameter(String name) {
    finish();
    currentParam = lookup.lookup(name);
    bounds = new ArrayList<JType[]>();
  }

  @Override
  public SignatureVisitor visitInterface() {
    finish();
    JType[] intf = new JType[1];
    interfaces.add(intf);
    return new ResolveTypeSignature(resolver, binaryMapper, logger, intf,
        lookup, null);
  }

  @Override
  public SignatureVisitor visitInterfaceBound() {
    JType[] bound = new JType[1];
    bounds.add(bound);
    return new ResolveTypeSignature(resolver, binaryMapper, logger, bound,
        lookup, null);
  }

  @Override
  public SignatureVisitor visitSuperclass() {
    finish();
    return new ResolveTypeSignature(resolver, binaryMapper, logger, superClass,
        lookup, null);
  }
}
