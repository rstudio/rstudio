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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.rebind.rpc.testcases.client.AbstractSerializableTypes;
import com.google.gwt.user.rebind.rpc.testcases.client.CovariantArrays;
import com.google.gwt.user.rebind.rpc.testcases.client.ManualSerialization;
import com.google.gwt.user.rebind.rpc.testcases.client.MissingGwtTypeArgs;
import com.google.gwt.user.rebind.rpc.testcases.client.NoSerializableTypes;
import com.google.gwt.user.rebind.rpc.testcases.client.NotAllSubtypesAreSerializable;
import com.google.gwt.user.rebind.rpc.testcases.client.ObjectArrayInMethodSignature;
import com.google.gwt.user.rebind.rpc.testcases.client.ObjectInMethodSignature;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Used to test the {@link SerializableTypeOracleBuilder}.
 */
public class SerializableTypeOracleBuilderTest extends TestCase {
  /**
   * Used to test the results produced by the {@link SerializableTypeOracle}.
   */
  static class TypeInfo {
    boolean maybeInstantiated;
    final String sourceName;

    TypeInfo(String binaryName, boolean maybeInstantiated) {
      this.sourceName = makeSourceName(binaryName);
      this.maybeInstantiated = maybeInstantiated;
    }

    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof TypeInfo)) {
        return false;
      }

      TypeInfo other = (TypeInfo) obj;
      return sourceName.equals(other.sourceName)
          && maybeInstantiated == other.maybeInstantiated;
    }

    public String toString() {
      return "{ " + sourceName + ", " + Boolean.toString(maybeInstantiated)
          + " }";
    }
  }

  private static class MockPropertyOracle implements PropertyOracle {
    public String getPropertyValue(TreeLogger logger, String propertyName) {
      // Could mock "gwt.enforceRPCTypeVersioning" etc here
      return "";
    }

    public String[] getPropertyValueSet(TreeLogger logger, String propertyName) {
      return new String[] {};
    }
  }

  /**
   * No logger output will be written to the console.
   */
  private static final boolean SUPPRESS_LOGGER_OUTPUT = true;

  static {
    ModuleDefLoader.setEnableCachingModules(true);
    // Not required for an isolated module that doesn't ref GWTTestCase.
    ModuleDefLoader.forceInherit("com.google.gwt.junit.JUnit");
  }

  private static TypeInfo[] getActualTypeInfo(SerializableTypeOracle sto) {
    JType[] types = sto.getSerializableTypes();
    TypeInfo[] actual = new TypeInfo[types.length];
    for (int i = 0; i < types.length; ++i) {
      JType type = types[i];
      actual[i] = new TypeInfo(type.getParameterizedQualifiedSourceName(),
          sto.maybeInstantiated(type));
    }
    sort(actual);
    return actual;
  }

  private static String makeSourceName(String binaryName) {
    return binaryName.replace('$', '.');
  }

  private static void sort(TypeInfo[] typeInfos) {
    Arrays.sort(typeInfos, new Comparator<TypeInfo>() {
      public int compare(TypeInfo ti1, TypeInfo ti2) {
        if (ti1 == ti2) {
          return 0;
        }

        return ti1.sourceName.compareTo(ti2.sourceName);
      }
    });
  }

  private static String toString(TypeInfo[] typeInfos) {
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    for (int i = 0; i < typeInfos.length; ++i) {
      if (i != 0) {
        sb.append(",");
      }
      sb.append(typeInfos[i].toString());
      sb.append("\n");
    }

    sb.append("]");
    return sb.toString();
  }

  private static void validateSTO(SerializableTypeOracle sto,
      TypeInfo[] expected) {
    sort(expected);
    TypeInfo[] actual = getActualTypeInfo(sto);

    assertTrue("Expected: \n" + toString(expected) + ",\n Actual: \n"
        + toString(actual), Arrays.equals(expected, actual));
  }

  /**
   * This could be your own tree logger, perhaps validating the error output.
   */
  private final TreeLogger logger = SUPPRESS_LOGGER_OUTPUT ? TreeLogger.NULL
      : new PrintWriterTreeLogger();

  private final ModuleDef moduleDef;

  private final PropertyOracle propertyOracle = new MockPropertyOracle();

  private final TypeOracle typeOracle;

  public SerializableTypeOracleBuilderTest() throws UnableToCompleteException {
    if (logger instanceof AbstractTreeLogger) {
      ((AbstractTreeLogger) logger).setMaxDetail(TreeLogger.INFO);
    }

    moduleDef = ModuleDefLoader.loadFromClassPath(logger,
        "com.google.gwt.user.rebind.rpc.testcases.RebindRPCTestCases");
    typeOracle = moduleDef.getTypeOracle(logger);
  }

  /**
   * Tests that a method signature which returns an Array type also includes the
   * possible covariant array types which could contain a serializable type.
   */
  public void testCovariantArrays() throws UnableToCompleteException,
      NotFoundException {

    JClassType testServiceClass = typeOracle.getType(CovariantArrays.class.getName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    SerializableTypeOracle sto = stob.build(propertyOracle, testServiceClass);

    TypeInfo[] expected = new TypeInfo[] {
        new TypeInfo(IncompatibleRemoteServiceException.class.getName(), true),
        new TypeInfo(CovariantArrays.AA.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.BB.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.CC.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.DD.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.A.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.B.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.B.class.getName(), true),
        new TypeInfo(CovariantArrays.C.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.D.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.D.class.getName(), true),
        new TypeInfo(String.class.getName(), true)};
    validateSTO(sto, expected);
  }

  /**
   * Tests that a manually serialized type with a field that is not serializable
   * does not cause the generator to fail.
   */
  public void testManualSerialization() throws NotFoundException,
      UnableToCompleteException {
    JClassType testServiceClass = typeOracle.getType(ManualSerialization.class.getName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    stob.build(propertyOracle, testServiceClass);
  }

  /**
   * Tests that a missing gwt.typeArgs will not result in a failure. The set of
   * types is not currently being checked.
   */
  public void testMissingGwtTypeArgs() throws NotFoundException,
      UnableToCompleteException {
    JClassType testServiceClass = typeOracle.getType(MissingGwtTypeArgs.class.getName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    stob.build(propertyOracle, testServiceClass);
  }

  /**
   * Tests that a method signature which has no serializable types will result
   * in a failure.
   */
  public void testNoSerializableTypes() throws NotFoundException,
      UnableToCompleteException {
    JClassType testServiceClass = typeOracle.getType(NoSerializableTypes.class.getName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    try {
      stob.build(propertyOracle, testServiceClass);
      fail("Should have thrown an UnableToCompleteException");
    } catch (UnableToCompleteException ex) {
      // expected to get here
    }
  }

  /**
   * Tests that a method signature which only has type whose inheritance
   * hiearchy has a mix of serializable and unserializable types will not cause
   * the generator fail. It also checks for the set of serializable types.
   */
  public void testNotAllSubtypesAreSerializable()
      throws UnableToCompleteException, NotFoundException {

    JClassType testServiceClass = typeOracle.getType(NotAllSubtypesAreSerializable.class.getName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    SerializableTypeOracle sto = stob.build(propertyOracle, testServiceClass);

    TypeInfo[] expected = new TypeInfo[] {
        new TypeInfo(IncompatibleRemoteServiceException.class.getName(), true),
        new TypeInfo(
            makeSourceName(NotAllSubtypesAreSerializable.B.class.getName()),
            true),
        new TypeInfo(
            makeSourceName(NotAllSubtypesAreSerializable.D.class.getName()),
            true), new TypeInfo(String.class.getName(), true)};
    validateSTO(sto, expected);
  }

  /**
   * Tests that a method signature which only has Object in its signature fails.
   */
  public void testObjectArrayInMethodSignature() throws NotFoundException,
      UnableToCompleteException {
    JClassType testServiceClass = typeOracle.getType(ObjectArrayInMethodSignature.class.getName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    try {
      stob.build(propertyOracle, testServiceClass);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // Should get here
    }
  }

  /**
   * Tests that a method signature which only has Object in its signature fails.
   */
  public void testObjectInMethodSignature() throws NotFoundException,
      UnableToCompleteException {
    JClassType testServiceClass = typeOracle.getType(ObjectInMethodSignature.class.getName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    try {
      stob.build(propertyOracle, testServiceClass);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // Should get here
    }
  }

  /**
   * Tests that a method signature which only has abstract serializable types
   * fails.
   */
  public void testOnlyAbstractSerializableTypes()
      throws UnableToCompleteException, NotFoundException {

    JClassType testServiceClass = typeOracle.getType(AbstractSerializableTypes.class.getName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    try {
      stob.build(propertyOracle, testServiceClass);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // Should get here
    }
  }
}
