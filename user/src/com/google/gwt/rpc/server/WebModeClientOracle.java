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
package com.google.gwt.rpc.server;

import com.google.gwt.rpc.client.ast.CommandSink;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Encapsulates data about the structure of the client code.
 */
public final class WebModeClientOracle extends ClientOracle implements
    Serializable {
  /*
   * TODO: Don't use Java serialization.
   */

  /**
   * A Builder object to create ClientOracles.
   */
  public static class Builder {
    private WebModeClientOracle oracle = new WebModeClientOracle();

    public void add(String jsIdent, String jsniIdent, String className,
        String memberName, String runtimeTypeId, CastableTypeData castableTypeData) {

      oracle.idents.add(jsIdent);
      ClassData data = oracle.getClassData(className);

      /*
       * Don't overwrite castableTypeData and runtimeTypeId if already set.
       * There are many versions of symbols for a given className,
       * corresponding to the type of member fields, etc.,
       * which don't have the runtimeTypeId or castableTypeData initialized.  Only
       * the symbol data for the class itself has this info.
       */
      if (data.castableTypeData == null) {
        data.runtimeTypeId = runtimeTypeId;
        data.castableTypeData = castableTypeData;
      }

      if (jsniIdent == null || jsniIdent.length() == 0) {
        data.typeName = className;
        data.jsSymbolName = jsIdent;
        oracle.seedNamesToClassData.put(jsIdent, data);
        // Class.getName() with metadata disabled is "Class$S<seedId>"
        oracle.seedIdsToClassData.put("S" + runtimeTypeId, data);
        data.runtimeTypeId = runtimeTypeId;
      } else {
        if (jsniIdent.contains("(")) {
          jsniIdent = jsniIdent.substring(jsniIdent.indexOf("::") + 2,
              jsniIdent.indexOf(')') + 1);
          data.methodJsniNamesToIdents.put(jsniIdent, jsIdent);
        } else {
          data.fieldIdentsToNames.put(jsIdent, memberName);
          data.fieldNamesToIdents.put(memberName, jsIdent);
        }
      }
    }

    public WebModeClientOracle getOracle() {
      WebModeClientOracle toReturn = oracle;
      oracle = null;
      return toReturn;
    }

    public void setSerializableFields(String className, List<String> fieldNames) {
      ClassData data = oracle.getClassData(className);
      assert data.serializableFields == null
          || fieldNames.containsAll(data.serializableFields);
      if (fieldNames.size() == 1) {
        data.serializableFields = Collections.singletonList(fieldNames.get(0));
      } else {
        data.serializableFields = new ArrayList<String>(fieldNames);
        Collections.sort(data.serializableFields);
      }
    }
  }

  /**
   * A pair with extra data.
   */
  public static class Triple<A, B, C> extends Pair<A, B> {
    private final C[] c;

    public Triple(A a, B b, C... c) {
      super(a, b);
      this.c = c;
    }

    public C[] getC() {
      return c;
    }
  }

  private static class ClassData implements Serializable {
    private static final long serialVersionUID = 6L;

    public CastableTypeData castableTypeData;
    public final Map<String, String> fieldIdentsToNames = new HashMap<String, String>();
    public final Map<String, String> fieldNamesToIdents = new HashMap<String, String>();
    public final Map<String, String> methodJsniNamesToIdents = new HashMap<String, String>();
    public String runtimeTypeId;
    public String jsSymbolName;
    public List<String> serializableFields = Collections.emptyList();
    public String typeName;
  }

  /**
   * Defined to prevent simple changes from invalidating stored data.
   *
   * TODO: Use something other than Java serialization to store this type's
   * data.
   */
  private static final long serialVersionUID = 3L;

  /**
   * Recreate a WebModeClientOracle based on the contents previously emitted by
   * {@link #store}. The underlying format should be considered opaque.
   */
  public static WebModeClientOracle load(InputStream stream) throws IOException {
    try {
      stream = new GZIPInputStream(stream);
      return readStreamAsObject(stream, WebModeClientOracle.class);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Should never reach this", e);
    }
  }

  static String jsniName(Class<?> clazz) {
    if (clazz.isPrimitive()) {
      if (clazz.equals(boolean.class)) {
        return "Z";
      } else if (clazz.equals(byte.class)) {
        return "B";
      } else if (clazz.equals(char.class)) {
        return "C";
      } else if (clazz.equals(short.class)) {
        return "S";
      } else if (clazz.equals(int.class)) {
        return "I";
      } else if (clazz.equals(long.class)) {
        return "J";
      } else if (clazz.equals(float.class)) {
        return "F";
      } else if (clazz.equals(double.class)) {
        return "D";
      }
      throw new RuntimeException("Unhandled primitive type " + clazz.getName());
    } else if (clazz.isArray()) {
      return "[" + jsniName(clazz.getComponentType());
    } else {
      return "L" + clazz.getName().replace('.', '/') + ";";
    }
  }

  /**
   * Copied from dev.Utility class which is not part of servlet.jar.
   */
  private static <T> T readStreamAsObject(InputStream inputStream, Class<T> type)
      throws ClassNotFoundException {
    ObjectInputStream objectInputStream = null;
    try {
      objectInputStream = new ObjectInputStream(inputStream);
      return type.cast(objectInputStream.readObject());
    } catch (IOException e) {
      return null;
    } finally {
      try {
        objectInputStream.close();
      } catch (IOException e) {
        // Ignore
      }
    }
  }

  /**
   * Serializes an object and writes it to a stream. Copied from Util to avoid
   * dependecy on gwt-dev.
   */
  private static void writeObjectToStream(OutputStream stream,
      Object... objects) throws IOException {
    ObjectOutputStream objectStream = new ObjectOutputStream(stream);
    for (Object object : objects) {
      objectStream.writeObject(object);
    }
    objectStream.flush();
  }

  /**
   * A map of class names to ClassData elements.
   */
  private final Map<String, ClassData> classData = new HashMap<String, ClassData>();

  private final Set<String> idents = new HashSet<String>();

  private final Map<String, ClassData> seedNamesToClassData = new HashMap<String, ClassData>();
  private final Map<String, ClassData> seedIdsToClassData = new HashMap<String, ClassData>();

  private transient Map<Class<?>, Field[]> operableFieldMap = new IdentityHashMap<Class<?>, Field[]>();

  /**
   * Instances of WebModeClientOracle are created either through the
   * {@link Builder} class or via the {@link #load} method.
   */
  protected WebModeClientOracle() {
  }

  @Override
  public CommandSink createCommandSink(OutputStream out) throws IOException {
    return new WebModePayloadSink(this, out);
  }

  @Override
  public String createUnusedIdent(String ident) {
    while (idents.contains(ident)) {
      ident += "$";
    }
    return ident;
  }

  @Override
  public CastableTypeData getCastableTypeData(Class<?> clazz) {
    while (clazz != null) {
      CastableTypeData toReturn = getCastableTypeData(canonicalName(clazz));
      if (toReturn != null) {
        return toReturn;
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  @Override
  public String getFieldId(Class<?> clazz, String fieldName) {
    while (clazz != null) {
      String className = clazz.getName();
      ClassData data = getClassData(className);
      if (data.fieldNamesToIdents.containsKey(fieldName)) {
        return data.fieldNamesToIdents.get(fieldName);
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  @Override
  public String getFieldId(Enum<?> value) {
    return getFieldId(value.getDeclaringClass(), value.name());
  }

  @Override
  public String getFieldId(String className, String fieldName) {
    ClassData data = getClassData(className);
    return data.fieldNamesToIdents.get(fieldName);
  }

  @Override
  public Pair<Class<?>, String> getFieldName(Class<?> clazz, String fieldId) {
    while (clazz != null) {
      ClassData data = getClassData(clazz.getName());
      String fieldName = data.fieldIdentsToNames.get(fieldId);
      if (fieldName == null) {
        clazz = clazz.getSuperclass();
      } else {
        return new Pair<Class<?>, String>(clazz, fieldName);
      }
    }
    return null;
  }

  /**
   * This will search superclasses.
   */
  @Override
  public String getMethodId(Class<?> clazz, String methodName, Class<?>... args) {
    while (clazz != null) {
      String toReturn = getMethodId(clazz.getName(), methodName, args);
      if (toReturn != null) {
        return toReturn;
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  @Override
  public String getMethodId(String className, String methodName,
      String... jsniArgTypes) {
    StringBuilder sb = new StringBuilder();
    sb.append(methodName);
    sb.append("(");
    for (String jsniArg : jsniArgTypes) {
      sb.append(jsniArg);
    }
    sb.append(")");

    ClassData data = getClassData(className);
    String jsIdent = data.methodJsniNamesToIdents.get(sb.toString());
    return jsIdent;
  }

  @Override
  public Field[] getOperableFields(Class<?> clazz) {
    Field[] toReturn;
    synchronized (operableFieldMap) {
      toReturn = operableFieldMap.get(clazz);
    }
    if (toReturn != null) {
      return toReturn;
    }

    ClassData data = getClassData(clazz.getName());
    toReturn = new Field[data.serializableFields.size()];
    for (int i = 0; i < toReturn.length; i++) {
      String fieldName = data.serializableFields.get(i);
      try {
        toReturn[i] = clazz.getDeclaredField(fieldName);
      } catch (SecurityException e) {
        throw new IncompatibleRemoteServiceException("Cannot access field "
            + fieldName, e);
      } catch (NoSuchFieldException e) {
        throw new IncompatibleRemoteServiceException("No field " + fieldName, e);
      }
    }

    synchronized (operableFieldMap) {
      operableFieldMap.put(clazz, toReturn);
    }
    return toReturn;
  }

  @Override
  public String getRuntimeTypeId(Class<?> clazz) {
    while (clazz != null) {
      String toReturn = getRuntimeTypeId(canonicalName(clazz));
      if (toReturn != null) {
        return toReturn;
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  @Override
  public String getJsSymbolName(Class<?> clazz) {
    ClassData data = getClassData(clazz.getName());
    return data.jsSymbolName;
  }

  @Override
  public String getTypeName(String seedName) {
    // TODO: Decide how to handle the no-metadata case
    ClassData data = null;
    if (seedName.startsWith("Class$")) {
      seedName = seedName.substring(6);
      data = seedIdsToClassData.get(seedName);
    }

    if (data == null) {
      data = seedNamesToClassData.get(seedName);
    }
    return data == null ? null : data.typeName;
  }

  @Override
  public boolean isScript() {
    return true;
  }

  /**
   * Write the state of the WebModeClientOracle into an OutputStream. The
   * underlying format should be considered opaque.
   */
  public void store(OutputStream stream) throws IOException {
    stream = new GZIPOutputStream(stream);
    writeObjectToStream(stream, this);
    stream.close();
  }

  private String canonicalName(Class<?> clazz) {
    if (clazz.isArray()) {
      Class<?> leafType = clazz;
      do {
        leafType = leafType.getComponentType();
      } while (leafType.isArray());

      Class<?> enclosing = leafType.getEnclosingClass();
      if (enclosing != null) {
        // com.foo.Enclosing$Name[]
        return canonicalName(enclosing) + "$" + clazz.getSimpleName();
      } else if (leafType.getPackage() == null) {
        // Name0[
        return clazz.getSimpleName();
      } else {
        // com.foo.Name[]
        return leafType.getPackage().getName() + "." + clazz.getSimpleName();
      }
    } else {
      return clazz.getName();
    }
  }

  private CastableTypeData getCastableTypeData(String className) {
    ClassData data = getClassData(className);
    return data.castableTypeData;
  }

  private ClassData getClassData(String className) {
    ClassData toReturn = classData.get(className);
    if (toReturn == null) {
      toReturn = new ClassData();
      classData.put(className, toReturn);
    }
    return toReturn;
  }

  /**
   * This will not search superclasses and is used to access magic GWT types
   * like Array.
   */
  private String getMethodId(String className, String methodName,
      Class<?>... args) {
    String[] jsniArgTypes = new String[args.length];
    for (int i = 0, j = args.length; i < j; i++) {
      jsniArgTypes[i] = jsniName(args[i]);
    }
    return getMethodId(className, methodName, jsniArgTypes);
  }

  private String getRuntimeTypeId(String className) {
    ClassData data = getClassData(className);
    return data.runtimeTypeId;
  }

  /**
   * Reinitialize the <code>operableFieldMap</code> field when the
   * WebModeClientOracle is reloaded.
   */
  private Object readResolve() {
    operableFieldMap = new HashMap<Class<?>, Field[]>();
    return this;
  }
}
