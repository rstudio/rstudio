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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.BadPropertyValueException;
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

/**
 * Used to test the {@link SerializableTypeOracleBuilder}.
 */
public class SerializableTypeOracleBuilderTest extends TestCase {
  private static class MockPropertyOracle implements PropertyOracle {
    public String getPropertyValue(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      // Could mock "gwt.enforceRPCTypeVersioning" etc here
      return "";
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

  private static String[] getSortedSerializableTypeNames(
      SerializableTypeOracle sto) {
    JType[] actualTypes = sto.getSerializableTypes();
    String[] names = new String[actualTypes.length];
    for (int i = 0; i < actualTypes.length; ++i) {
      names[i] = actualTypes[i].getParameterizedQualifiedSourceName();
    }
    Arrays.sort(names);
    return names;
  }

  private static String makeSourceName(String binaryName) {
    return binaryName.replace('$', '.');
  }

  private static String toString(String[] strings) {
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    for (int i = 0; i < strings.length; ++i) {
      if (i != 0) {
        sb.append(",");
      }
      sb.append(strings[i]);
    }

    sb.append("]");
    return sb.toString();
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

    String[] actualTypes = getSortedSerializableTypeNames(sto);
    String[] expectedTypes = new String[] {
        IncompatibleRemoteServiceException.class.getName(),
        makeSourceName(CovariantArrays.AA.class.getName()) + "[]",
        makeSourceName(CovariantArrays.BB.class.getName()) + "[]",
        makeSourceName(CovariantArrays.CC.class.getName()) + "[]",
        makeSourceName(CovariantArrays.DD.class.getName()) + "[]",
        makeSourceName(CovariantArrays.A.class.getName()) + "[]",
        makeSourceName(CovariantArrays.B.class.getName()) + "[]",
        makeSourceName(CovariantArrays.B.class.getName()),
        makeSourceName(CovariantArrays.C.class.getName()) + "[]",
        makeSourceName(CovariantArrays.D.class.getName()) + "[]",
        makeSourceName(CovariantArrays.D.class.getName()),
        String.class.getName()};
    Arrays.sort(expectedTypes);
    assertTrue("Expected: " + toString(expectedTypes) + ", Actual: "
        + toString(actualTypes), Arrays.equals(expectedTypes, actualTypes));
  }

  /**
   * Tests that a manually serialized type with a field that is not serializable
   * does not cause the generator to fail.
   */
  public void testManualSerialization() throws NotFoundException, UnableToCompleteException {
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
  public void testNoSerializableTypes() throws NotFoundException {
    try {
      JClassType testServiceClass = typeOracle.getType(NoSerializableTypes.class.getName());
      SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
          logger, typeOracle);
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

    String[] actualTypes = getSortedSerializableTypeNames(sto);
    String[] expectedTypes = new String[] {
        IncompatibleRemoteServiceException.class.getName(),
        makeSourceName(NotAllSubtypesAreSerializable.B.class.getName()),
        makeSourceName(NotAllSubtypesAreSerializable.D.class.getName()),
        String.class.getName()};
    Arrays.sort(expectedTypes);
    assertTrue("Expected: " + toString(expectedTypes) + ", Actual: "
        + toString(actualTypes), Arrays.equals(expectedTypes, actualTypes));
  }

  /**
   * Tests that a method signature which only has Object in its signature fails.
   */
  public void testObjectArrayInMethodSignature() throws NotFoundException {
    JClassType testServiceClass = typeOracle.getType(ObjectArrayInMethodSignature.class.getName());
    SerializableTypeOracleBuilder stob;
    try {
      stob = new SerializableTypeOracleBuilder(logger, typeOracle);
      stob.build(propertyOracle, testServiceClass);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // Should get here
    }
  }

  /**
   * Tests that a method signature which only has Object in its signature fails.
   */
  public void testObjectInMethodSignature() throws NotFoundException {
    JClassType testServiceClass = typeOracle.getType(ObjectInMethodSignature.class.getName());
    SerializableTypeOracleBuilder stob;
    try {
      stob = new SerializableTypeOracleBuilder(logger, typeOracle);
      stob.build(propertyOracle, testServiceClass);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // Should get here
    }
  }

  /**
   * Tests that a method signature which only has abstract serializable types
   * does not fail.
   */
  public void testOnlyAbstractSerializableTypes()
      throws UnableToCompleteException, NotFoundException {

    JClassType testServiceClass = typeOracle.getType(AbstractSerializableTypes.class.getName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    stob.build(propertyOracle, testServiceClass);
  }
}
