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
package com.google.web.bindery.requestfactory.vm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Resolves payload type tokens to binary class names.
 */
public class TypeTokenResolver {
  /**
   * Constructs {@link TypeTokenResolver} instances.
   */
  public static class Builder {
    private TypeTokenResolver d = new TypeTokenResolver();

    /**
     * To be removed as well.
     */
    public Builder addTypeToken(String token, String binaryName) {
      d.typeTokens.put(token, binaryName);
      return this;
    }

    public TypeTokenResolver build() {
      TypeTokenResolver toReturn = d;
      toReturn.typeTokens = Collections.unmodifiableMap(toReturn.typeTokens);
      toReturn.referencedTypes =
          Collections.unmodifiableSet(new HashSet<String>(toReturn.typeTokens.values()));
      d = null;
      return toReturn;
    }

    /**
     * Adds data from a single InputStream to the state accumulated by the
     * Builder.
     */
    public void load(InputStream in) throws IOException {
      Properties props = new Properties();
      props.load(in);
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        addTypeToken(entry.getKey().toString(), entry.getValue().toString());
      }
      in.close();
    }

    public TypeTokenResolver peek() {
      return d;
    }
  }

  public static final String TOKEN_MANIFEST = "META-INF/requestFactory/typeTokens";

  /**
   * Loads all resources on path {@value #TOKEN_MANIFEST} and creates a
   * TypeTokenResolver.
   */
  public static TypeTokenResolver loadFromClasspath() throws IOException {
    Builder builder;
    boolean mustLoad = true;
    try {
      // Look for a pre-cooked Builder type
      Class<?> maybeBuilderImpl =
          Class.forName(TypeTokenResolver.class.getName() + "BuilderImpl", false, Thread
              .currentThread().getContextClassLoader());
      builder = maybeBuilderImpl.asSubclass(Builder.class).newInstance();
      mustLoad = false;
    } catch (ClassNotFoundException ignored) {
      // Try manifest-based approach
      builder = new Builder();
    } catch (InstantiationException e) {
      throw new RuntimeException("Could not instantiate TypeTokenResolverImpl", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Could not instantiate TypeTokenResolverImpl", e);
    }
    Enumeration<URL> locations =
        Thread.currentThread().getContextClassLoader().getResources(TOKEN_MANIFEST);
    if (mustLoad && !locations.hasMoreElements()) {
      throw new RuntimeException("No token manifest found.  Did the RequestFactory annotation"
          + " processor run? Check classpath for " + TOKEN_MANIFEST + " file and ensure that"
          + " your proxy types are compiled with the requestfactory-apt.jar on javac's classpath.");
    }
    while (locations.hasMoreElements()) {
      builder.load(locations.nextElement().openStream());
    }
    return builder.build();
  }

  /**
   * The values of {@link #typeTokens}.
   */
  private Set<String> referencedTypes;

  /**
   * Map of obfuscated ids to binary class names.
   */
  private Map<String, String> typeTokens = new HashMap<String, String>();

  public SortedMap<String, String> getAllTypeTokens() {
    return new TreeMap<String, String>(typeTokens);
  }

  public String getTypeFromToken(String typeToken) {
    return typeTokens.get(typeToken);
  }

  public boolean isReferencedType(String binaryName) {
    return referencedTypes.contains(binaryName);
  }

  /**
   * Closes the OutputStream.
   */
  public void store(OutputStream out) throws IOException {
    Properties props = new Properties();
    props.putAll(typeTokens);
    props.store(out, "GENERATED FILE -- DO NOT EDIT");
    out.close();
  }
}
