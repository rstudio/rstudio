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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Arrays;
import java.util.Iterator;

/**
 * This test is intended to exercise as many code paths and node types as
 * possible in the Java to JavaScript compiler. This test is not at all intended
 * to execute correctly.
 */
@SuppressWarnings("hiding")
public class CoverageTest extends CoverageBase {

  /**
   * TODO: document me.
   */
  public class Inner extends Super {

    public int x = 3;
    public final int y = 4;

    public Inner() {
      // ExplicitConstructorCall
      this(4);
    }

    public Inner(int i) {
      // ExplicitConstructorCall
      super(i);
    }

    public void foo() {
      final int z = this.y;

      new Inner() {
        {
          x = z;
          this.x = z;
          Inner.this.x = z;
          next = CoverageTest.this.next;
          next.foo();
          CoverageTest.this.next.foo();
          CoverageTest.this.x = z;
          CoverageTest.super.x = z;
        }

        public void foo() {
          x = z;
          this.x = z;
          Inner.this.x = z;
          next = CoverageTest.this.next;
          next.foo();
          CoverageTest.this.next.foo();
          CoverageTest.this.x = z;
          CoverageTest.super.x = z;
        }
      };

      class NamedLocal extends Inner {
        @SuppressWarnings("unused")
        public void foo() {
          CoverageTest.this.getNext();
          Inner.this.bar();
          super.bar();
          int x = z;
        }

        // JDT bug? This works in 5.0 but not in 1.4
        // TODO: will javac compile it?
        class NamedLocalSub extends NamedLocal {
          @SuppressWarnings("unused")
          public void foo() {
            Inner.this.bar();
            NamedLocal.this.foo();
            super.foo();
            int x = z;
          }
        }
      }
      testEmptyStatement();

      new InnerSub().new InnerSubSub().fda();
      new SecondMain().new FunkyInner();
      /*
       * The statement below causes a javac bug in openJdk and sun's java 6. It
       * produces incorrect bytecode that fails with a java.lang.VerifyError --
       * see Google's internal issue 1628473. This is likely to be an hindrance
       * if and when GWT attempts to read bytecode directly.
       */
      // new NamedLocal().new NamedLocalSub().foo();
    }

    public void bar() {
    }

    private void testAllocationExpression() {
      // AllocationExpression
      o = new Super();
      assertEquals("3", o.toString());
      o = new Super(42);
      assertEquals("42", o.toString());
    }

    private void testAndAndExpression() {
      // AND_AND_Expression
      z = i == 1 && betterNotEval();
      assertFalse(z);
    }

    private void testArrayAllocationExpression() {
      // ArrayAllocationExpression
      ia = new int[4];
      assertEquals(4, ia.length);
      iaa = new int[4][3];
      assertEquals(4, iaa.length);
      assertEquals(3, iaa[2].length);
      iaaa = new int[4][3][];
      assertEquals(4, iaaa.length);
      assertEquals(3, iaaa[2].length);
      assertNull(iaaa[2][2]);
    }

    private void testArrayInitializer() {
      // ArrayInitializer
      ia = new int[] {i, j};
      assertEquals(2, ia.length);
      assertEquals(j, ia[1]);
      iaa = new int[][] {{i, j}};
      assertEquals(1, iaa.length);
      assertEquals(2, iaa[0].length);
      assertEquals(j, iaa[0][1]);
      iaa = new int[][] { {i, j}, ia};
      assertEquals(2, iaa.length);
      assertEquals(2, iaa[0].length);
      assertEquals(j, iaa[0][1]);
      assertEquals(ia, iaa[1]);
    }

    private void testArrayReference() {
      ia = new int[] {i, j};
      // ArrayReference
      i = ia[0];
      assertEquals(ia[0], i);
      ia[0] = i;
    }

    private void testAssertStatement() {
      // AssertStatement

      if (!CoverageTest.class.desiredAssertionStatus()) {
        return;
      }

      i = 1;
      try {
        assert i == 2;
        fail();
      } catch (AssertionError e) {
      }

      try {
        assert i == 3 : true;
        fail();
      } catch (AssertionError e) {
        assertEquals("true", e.getMessage());
      }

      try {
        assert i == 3 : 'c';
        fail();
      } catch (AssertionError e) {
        assertEquals("c", e.getMessage());
      }

      try {
        assert i == 3 : 1.1;
        fail();
      } catch (AssertionError e) {
        assertEquals("1.1", e.getMessage());
      }

      try {
        assert i == 3 : 1.5f;
        fail();
      } catch (AssertionError e) {
        assertEquals("1.5", e.getMessage());
      }

      try {
        assert i == 3 : 5;
        fail();
      } catch (AssertionError e) {
        assertEquals("5", e.getMessage());
      }

      try {
        assert i == 3 : 6L;
        fail();
      } catch (AssertionError e) {
        assertEquals("6", e.getMessage());
      }

      try {
        assert i == 3 : "foo";
        fail();
      } catch (AssertionError e) {
        assertEquals("foo", e.getMessage());
      }

      try {
        assert i == 3 : new Object() {
          @Override
          public String toString() {
            return "bar";
          }
        };
        fail();
      } catch (AssertionError e) {
        assertEquals("bar", e.getMessage());
      }
    }

    private void testAssignment() {
      // Assignment
      i = j;
      assertEquals(j, i);
    }

    private void testBinaryExpression() {
      // BinaryExpression
      i = 4;
      i = i + j;
      assertEquals(6, i);
      i = i - j;
      assertEquals(4, i);
      i = i * j;
      assertEquals(8, i);
      i = i / j;
      assertEquals(4, i);
      i = i % j;
      assertEquals(0, i);
      i = 7;
      i = i & j;
      assertEquals(2, i);
      i = 0;
      i = i | j;
      assertEquals(2, i);
      i = 7;
      i = i ^ j;
      assertEquals(5, i);
      i = i << j;
      assertEquals(20, i);
      i = i >> j;
      assertEquals(5, i);
      i = i >>> j;
      assertEquals(1, i);
    }

    private void testBreakContinueLabelStatement() {
      // BreakStatement, ContinueStatement
      z = true;
      i = 0;
      x = 0;
      outer : while (z) {
        ++x;
        inner : while (z) {
          ++i;
          if (i == 1) {
            continue;
          }
          if (i == 2) {
            continue inner;
          }
          if (i == 3) {
            continue outer;
          }
          if (i == 4) {
            break;
          }
          if (i == 5) {
            break inner;
          }
          if (i == 6) {
            break outer;
          }
        }
      }
      assertEquals(6, i);
      assertEquals(4, x);
    }

    private void testCaseSwitchStatement() {
      // CaseStatement, SwitchStatement
      i = 6;
      switch (j) {
        case 1:
          ++i;
          // fallthrough
        case 2:
          i += 2;
          // fallthrough
        case 3:
          i += 3;
          // fallthrough
        case 4:
          i += 4;
          // fallthrough
        default:
          i += 0;
      }
      assertEquals(15, i);
    }

    @SuppressWarnings("cast")
    private void testCastExpression() {
      // CastExpression
      o = (Super) o;
    }

    private void testCharLiteral() {
      // CharLiteral
      i = 'c';
      assertEquals("c", String.valueOf((char) i));
    }

    private void testClassLiteralAccess() {
      // ClassLiteralAccess
      o = Super.class;
      String str = o.toString();

      // Class metadata could be disabled
      if (!str.startsWith("class Class$")) {
        assertEquals("class com.google.gwt.dev.jjs.test.CoverageTest$Super",
            str);
      }
    }

    private void testCompoundAssignment() {
      // CompoundAssignment
      i = 4;
      i += j;
      assertEquals(6, i);
      i -= j;
      assertEquals(4, i);
      i *= j;
      assertEquals(8, i);
      i /= j;
      assertEquals(4, i);
      i %= j;
      assertEquals(0, i);
      i = 7;
      i &= j;
      assertEquals(2, i);
      i = 0;
      i |= j;
      assertEquals(2, i);
      i = 7;
      i ^= j;
      assertEquals(5, i);
      i <<= j;
      assertEquals(20, i);
      i >>= j;
      assertEquals(5, i);
      i >>>= j;
      assertEquals(1, i);
    }

    private void testConditionalExpression() {
      // ConditionalExpression
      z = false;
      i = z ? 7 : j;
      assertEquals(j, i);
    }

    private void testDoStatement() {
      // DoStatement
      i = 3;
      z = false;
      do {
        i += j;
      } while (z);
      assertEquals(5, i);
    }

    private void testEmptyStatement() {
      // EmptyStatement
      ;
    }

    private void testEqualExpression() {
      // EqualExpression
      i = 3;
      assertFalse(i == j);
      assertTrue(i != j);
      assertFalse(i < j);
      assertFalse(i <= j);
      assertTrue(i > j);
      assertTrue(i >= j);
    }

    private void testForeachStatement() {
      ia = new int[] {i, j};
      // Array of primitive.
      for (int q : ia) {
        i = q;
      }
      // Array of primitive with unboxing.
      for (Integer q : ia) {
        i = q;
      }
      // Array of object.
      for (String str : sa) {
        s = str;
      }
      // Iterable.
      for (Object obj : Arrays.asList(new Object(), new Object())) {
        o = obj;
      }
      // Iterable with unboxing.
      for (int q : Arrays.asList(1, 2, 3)) {
        i = q;
      }
      // Iterable with generic cast.
      for (String str : Arrays.asList(sa)) {
        s = str;
      }
      // Iterable with array element.
      for (String[] stra : Arrays.asList(sa, sa, sa)) {
        s = sa[0];
      }
      // Iterable Iterator subclass.
      class SubIterator<T> implements Iterator<T> {
        private final Iterator<T> it;

        public SubIterator(Iterator<T> it) {
          this.it = it;
        }

        @Override
        public boolean hasNext() {
          return it.hasNext();
        }

        @Override
        public T next() {
          return it.next();
        }

        @Override
        public void remove() {
          it.remove();
        }
      }
      class SubIterableString implements Iterable<String> {
        @Override
        public SubIterator<String> iterator() {
          return new SubIterator<String>(Arrays.asList(sa).iterator());
        }
      }
      for (String str : new SubIterableString()) {
        s = str;
      }
    }

    private void testForStatement() {
      // ForStatement
      i = 0;
      for (int q = 0, v = 4; q < v; ++q) {
        i += q;
      }
      assertEquals(6, i);
      for (i = 0; i < 4; ++i) {
      }
      assertEquals(4, i);
    }

    private void testIfStatement() {
      // IfStatement
      z = false;
      if (z) {
        fail();
      }
      if (z) {
        fail();
      } else {
        assertFalse(z);
      }
      if (!z) {
        assertFalse(z);
      } else {
        fail();
      }
    }

    private void testInstanceOfExpression() {
      // InstanceOfExpression
      Object o = CoverageTest.this;
      assertTrue(o instanceof CoverageBase);
    }

    private void testLiterals() {
      // DoubleLiteral
      d = 3.141592653589793;
      assertEquals(3, (int) d);

      // FalseLiteral
      assertFalse(false);

      // FloatLiteral
      f = 3.1415927f;
      assertEquals(3, (int) f);

      // IntLiteral
      i = 4;

      // IntLiteralMinValue
      i = -2147483648;

      // LongLiteral
      l = 4L;

      // LongLiteralMinValue
      l = -9223372036854775808L;

      // NullLiteral
      o = null;

      // StringLiteral
      s = "f'oo\b\t\n\f\r\"\\";
      assertEquals(s, "f" + '\'' + 'o' + 'o' + '\b' + '\t' + '\n' + '\f' + '\r'
          + '"' + '\\');

      // TrueLiteral
      assertTrue(true);
    }

    private void testOrOrExpression() {
      // OR_OR_Expression
      i = 1;
      assertTrue(i == 1 || betterNotEval());
    }

    private void testPostfixExpression() {
      // PostfixExpression
      i = 1;
      assertEquals(1, i++);
      assertEquals(2, i--);
    }

    private void testPrefixExpression() {
      // PrefixExpression
      i = 1;
      assertEquals(2, ++i);
      assertEquals(1, --i);
    }

    private void testQualifiedAllocationExpression() {
      // QualifiedAllocationExpression
      o = new Inner();
      o = CoverageTest.this.new Inner();
      o = new CoverageTest().new Inner();
    }

    private void testQualifiedNameReference() {
      // QualifiedNameReference
      CoverageTest m = new CoverageTest();
      ia = new int[2];
      assertEquals("1", 2, ia.length);
      assertEquals("2", 2, m.j);
      assertEquals("3", 4, m.y);
      assertEquals("4", 2, new CoverageTest().j);
      assertEquals("5", 4, new CoverageTest().y);
      assertEquals("6", 2, m.next.j);
      assertEquals("7", 4, m.next.y);
      assertEquals("8", 2, new CoverageTest().next.j);
      assertEquals("9", 4, new CoverageTest().next.y);
      assertEquals("A", 2, m.getNext().j);
      assertEquals("B", 4, m.getNext().y);
      assertEquals("C", 2, new CoverageTest().getNext().j);
      assertEquals("D", 4, new CoverageTest().getNext().y);
    }

    private void testReferenceCalls() {
      // MessageSend, QualifiedSuperReference, QualifiedThisReference,
      // SuperReference, ThisReference
      Inner other = new Inner();
      foo();
      this.foo();
      other.foo();
      CoverageTest.this.foo();
      super.foo();
      Inner.super.foo();
      CoverageTest.super.foo();

      sfoo();
      this.sfoo();
      CoverageTest.sfoo();
      Inner.sfoo();
      Super.sfoo();
      other.sfoo();
      CoverageTest.this.sfoo();
      super.sfoo();
      Inner.super.sfoo();
      CoverageTest.super.sfoo();
    }

    private Inner testReferences() {
      // FieldReference, QualifiedSuperReference, QualifiedThisReference,
      // SuperReference, ThisReference
      Inner other = new Inner();
      i = 3;
      i = i + j + x + y;
      assertEquals(12, i);
      i = this.i + this.j + this.x + this.y;
      assertEquals(21, i);
      i = CoverageTest.i + CoverageTest.j;
      assertEquals(8, i);
      i = Inner.i + Inner.j;
      assertEquals(10, i);
      i = Super.i + Super.j;
      assertEquals(12, i);
      i = other.i + other.j + other.x + other.y;
      assertEquals(21, i);
      i = Inner.this.i + Inner.this.j + Inner.this.x + Inner.this.y;
      assertEquals(30, i);
      i = CoverageTest.this.i + CoverageTest.this.j + CoverageTest.this.x
          + CoverageTest.this.y;
      assertEquals(15, i);
      i = super.i + super.j + super.x + super.y;
      assertEquals(25, i);
      i = Inner.super.i + Inner.super.j + Inner.super.x + Inner.super.y;
      assertEquals(35, i);
      i = CoverageTest.super.i + CoverageTest.super.j + CoverageTest.super.x
          + CoverageTest.super.y;
      assertEquals(10, i);
      return other;
    }

    private void testReturnStatement() {
      // ReturnStatement
      assertEquals("foo", doReturnFoo());
      if (true) {
        return;
      }
      fail();
    }

    private void testSynchronizedStatement() {
      // SynchronizedStatement
      synchronized (inner) {
        inner.i = i;
      }
    }

    private void testTryCatchFinallyThrowStatement() {
      // ThrowStatement, TryStatement
      try {
        i = 3;
        if (true) {
          throw new Exception();
        }
        fail();
      } catch (Exception e) {
      } finally {
        i = 7;
      }
      assertEquals(7, i);

      try {
        try {
          i = 3;
        } catch (Throwable t) {
          fail();
        }
      } catch (Throwable t) {
        fail();
      } finally {
        i = 7;
      }
      assertEquals(7, i);
    }

    private void testUnaryExpression() {
      // UnaryExpression
      i = 4;
      assertEquals(-4, -i);
      assertEquals(-5, ~i);
      z = true;
      assertFalse(!z);
    }

    private void testWhileStatement() {
      // WhileStatement
      z = false;
      while (z) {
        fail();
      }
    }
  }

  /**
   * TODO: document me.
   */
  public static class Super {
    public static int i = 2;
    public static final int j = 2;

    // Initializer
    static {
      Super.i = 1;
    }

    // Initializer
    static {
      Super.i = 3;
    }

    protected static void sfoo() {
    }

    public int x = 2;
    public final int y = 4;

    // Initializer
    {
      x = 1;
    }

    // Initializer
    {
      x = 3;
    }

    public Super() {
    }

    public Super(int i) {
      x = i;
    }

    public void foo() {
    }

    public String toString() {
      return String.valueOf(x);
    }
  }

  private static class InnerSub extends Inner {
    private class InnerSubSub extends InnerSub {
      {
        asdfasdfasdf = InnerSub.this.asdfasdfasdf;
        InnerSub.this.asdfasdfasdf = asdfasdfasdf;
        asdfasdfasdf = super.asdfasdfasdf;
        super.asdfasdfasdf = asdfasdfasdf;
      }

      void fda() {
        asdfasdfasdf = InnerSub.this.asdfasdfasdf;
        InnerSub.this.asdfasdfasdf = asdfasdfasdf;
        asdfasdfasdf = super.asdfasdfasdf;
        super.asdfasdfasdf = asdfasdfasdf;
      }
    }

    private int asdfasdfasdf = 3;

    InnerSub() {
      new CoverageTest().super();
    }
  }

  private static class SecondMain {
    private class FunkyInner extends Inner {
      FunkyInner() {
        new CoverageTest().super();
      }
    }
  }

  public static double d;

  public static float f;

  public static int i = 1 + 2 + 3;

  public static int[] ia;

  public static int[][] iaa;

  public static int[][][] iaaa;

  public static final int j = 2;

  public static long l;

  public static Object o;

  public static String s = "foo";

  public static String[] sa = new String[]{"foo", "bar", "bar"};

  public static CoverageTest singleton;

  public static boolean z;

  public static boolean betterNotEval() {
    fail();
    return false;
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    x = 3;
    d = 0;
    f = 0;
    i = 1 + 2 + 3;
    ia = null;
    iaa = null;
    iaaa = null;
    l = 0;
    o = null;
    s = "foo";
    sa = new String[]{"foo", "bar", "bar"};
    z = false;
  }
  
  protected static void sfoo() {
  }

  private static String doReturnFoo() {
    if (true) {
      return "foo";
    }
    fail();
    return "bar";
  }

  public final Inner inner = new Inner();

  public CoverageTest next;

  public int x = 3;

  public final int y = 4;

  public CoverageTest() {
    if (singleton == null) {
      singleton = this;
    }
    next = this;
  }

  public void foo() {
  }

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public CoverageTest getNext() {
    return next;
  }

  public void testAllocationExpression() {
    inner.testAllocationExpression();
  }

  public void testAndAndExpression() {
    inner.testAndAndExpression();
  }

  public void testArrayAllocationExpression() {
    inner.testArrayAllocationExpression();
  }

  public void testArrayInitializer() {
    inner.testArrayInitializer();
  }

  public void testArrayReference() {
    inner.testArrayReference();
  }

  public void testAssertStatement() {
    inner.testAssertStatement();
  }

  public void testAssignment() {
    inner.testAssignment();
  }

  public void testBinaryExpression() {
    inner.testBinaryExpression();
  }

  public void testBreakContinueLabelStatement() {
    inner.testBreakContinueLabelStatement();
  }

  public void testCaseSwitchStatement() {
    inner.testCaseSwitchStatement();
  }

  public void testCastExpression() {
    inner.testCastExpression();
  }

  public void testCharLiteral() {
    inner.testCharLiteral();
  }

  public void testClassLiteralAccess() {
    inner.testClassLiteralAccess();
  }

  public void testCompoundAssignment() {
    inner.testCompoundAssignment();
  }

  public void testConditionalExpression() {
    inner.testConditionalExpression();
  }

  public void testDoStatement() {
    inner.testDoStatement();
  }

  public void testEmptyStatement() {
    inner.testEmptyStatement();
  }

  public void testEqualExpression() {
    inner.testEqualExpression();
  }

  public void testForeachStatement() {
    inner.testForeachStatement();
  }

  public void testForStatement() {
    inner.testForStatement();
  }

  public void testIfStatement() {
    inner.testIfStatement();
  }

  public void testInstanceOfExpression() {
    inner.testInstanceOfExpression();
  }

  public void testLiterals() {
    inner.testLiterals();
  }

  public void testOrOrExpression() {
    inner.testOrOrExpression();
  }

  public void testPostfixExpression() {
    inner.testPostfixExpression();
  }

  public void testPrefixExpression() {
    inner.testPrefixExpression();
  }

  public void testQualifiedAllocationExpression() {
    inner.testQualifiedAllocationExpression();
  }

  public void testQualifiedNameReference() {
    inner.testQualifiedNameReference();
  }

  public void testReferenceCalls() {
    inner.testReferenceCalls();
  }

  public void testReferences() {
    inner.testReferences();
  }

  public void testReturnStatement() {
    inner.testReturnStatement();
  }

  public void testSynchronizedStatement() {
    inner.testSynchronizedStatement();
  }

  public void testTryCatchFinallyThrowStatement() {
    inner.testTryCatchFinallyThrowStatement();
  }

  public void testUnaryExpression() {
    inner.testUnaryExpression();
  }

  public void testWhileStatement() {
    inner.testWhileStatement();
  }

}

abstract class CoverageBase extends GWTTestCase {

  public static int i = 1;
  public static final int j = 2;

  protected static void sfoo() {
  }

  public int x = 3;
  public final int y = 4;

  public void foo() {
  }

}
