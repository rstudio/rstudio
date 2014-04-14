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
package com.google.gwt.dev.jjs.impl.gflow.constants;

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;

import junit.framework.TestCase;

/**
 * Test for ConstantsAssumption.
 *
 * We use toString comparison in this test to make it simpler.
 */
public class ConstantsAssumptionTest extends TestCase {
  private final JIntLiteral zero = newIntLiteral(0);
  private final JIntLiteral one = newIntLiteral(1);
  private final JLocal i = newLocal("i", JPrimitiveType.INT);
  private final JLocal j = newLocal("j", JPrimitiveType.INT);

  public void testEmptyAssumption() {
    ConstantsAssumption a = new ConstantsAssumption();
    assertEquals("T", a.toString());
  }

  public void testSet() {
    ConstantsAssumption a = new ConstantsAssumption();
    a.set(i, zero);
    assertEquals("{i = 0}", a.toString());
    a.set(j, one);
    assertEquals("{i = 0, j = 1}", a.toString());
    a.set(i, null);
    assertEquals("{j = 1}", a.toString());
  }

  public void testJoin_SameValues() {
    ConstantsAssumption a1 = new ConstantsAssumption();
    a1.set(i, zero);
    a1.set(j, one);

    ConstantsAssumption a2 = new ConstantsAssumption();
    a2.set(i, zero);
    a2.set(j, one);

    assertEquals("{i = 0, j = 1}", a1.join(a2).toString());
  }

  public void testJoin_WithEmpty() {
    ConstantsAssumption a1 = new ConstantsAssumption();
    a1.set(i, zero);

    assertEquals("T", a1.join(new ConstantsAssumption()).toString());
    assertEquals("T", new ConstantsAssumption().join(a1).toString());
    assertEquals("T", a1.join(ConstantsAssumption.TOP).toString());
    assertEquals("T", ConstantsAssumption.TOP.join(a1).toString());

    assertEquals(a1, a1.join(null));
  }

  public void testJoin_DifferentValues() {
    ConstantsAssumption a1 = new ConstantsAssumption();
    a1.set(i, zero);
    a1.set(j, one);

    ConstantsAssumption a2 = new ConstantsAssumption();
    a2.set(i, one);
    a2.set(j, zero);

    assertEquals("T", a1.join(a2).toString());
  }

  public void testJoin_DifferentKeys() {
    ConstantsAssumption a1 = new ConstantsAssumption();
    a1.set(i, zero);

    ConstantsAssumption a2 = new ConstantsAssumption();
    a2.set(j, zero);

    assertEquals("T", a1.join(a2).toString());
  }

  public void testEquals_ComparesValues() {
    ConstantsAssumption a1 = new ConstantsAssumption();
    a1.set(i, newIntLiteral(0));

    ConstantsAssumption a2 = new ConstantsAssumption();
    a2.set(i, newIntLiteral(0));

    assertTrue(a1.equals(a2));
  }

  private JIntLiteral newIntLiteral(int value) {
    return new JIntLiteral(SourceOrigin.UNKNOWN, value);
  }

  private JLocal newLocal(String name, JPrimitiveType type) {
    return JProgram.createLocal(SourceOrigin.UNKNOWN, name, type, false,
        new JMethodBody(SourceOrigin.UNKNOWN));
  }
}
