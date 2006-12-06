// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.jdtcoverage;

/**
 * This test is intended to exercise as many code paths and node types as
 * possible in the Java to JavaScript compiler. This test is not at all intended
 * to execute correctly.
 */
public class Main extends MainSuper {

  public static int i = 1 + 2 + 3;
  public static final int j = 2;
  public static long l;
  public static int[] ia;
  public static int[][] iaa;
  public static int[][][] iaaa;
  public static Object o;
  public static boolean z;
  public static double d;
  public static float f;
  public static String s = "foo";
  public static Main singleton;

  public int x = 3;
  public final int y = 4;
  public Main next;

  public void onModuleLoad() {
    new Inner().go();
  }
  
  public Main getNext() {
    return next;
  }

  public void foo() {
  }

  protected static void sfoo() {
  }

  public static class Super {
    // Initializer
    static {
      Super.i = 1;
    }

    public static int i = 2;
    public static final int j = 2;

    // Initializer
    static {
      Super.i = 3;
    }

    // Initializer
    {
      x = 1;
    }

    public int x = 2;
    public final int y = 4;

    // Initializer
    {
      x = 3;
    }


    public Super() {
    }

    public Super(int i) {
    }

    public void foo() {
    }

    protected static void sfoo() {
    }

  }

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

    public void go() {

      // AllocationExpression
      o = new Super();
      o = new Super(42);

      // AND_AND_Expression
      z = i == 1 && j == 2;

      // ArrayAllocationExpression
      ia = new int[4];
      iaa = new int[4][3];
      iaaa = new int[4][3][];

      // ArrayInitializer
      ia = new int[]{i, j};
      iaa = new int[][]{{i, j}};
      iaa = new int[][]{{i, j}, ia};

      // ArrayQualifiedTypeReference
      // TODO: ???

      // ArrayReference
      i = ia[0];
      ia[0] = i;

      // Assignment
      i = j;

      // BinaryExpression
      i = i + j;
      i = i - j;
      i = i * j;
      i = i / j;
      i = i % j;
      i = i & j;
      i = i | j;
      i = i ^ j;
      i = i << j;
      i = i >> j;
      i = i >>> j;

      // CastExpression
      o = (Super) o;

      // CharLiteral
      i = 'c';

      // ClassLiteralAccess
      o = Super.class;

      // ConditionalExpression
      i = z ? i : j;

      // CompoundAssignment
      i += j;
      i -= j;
      i *= j;
      i /= j;
      i %= j;
      i &= j;
      i |= j;
      i ^= j;
      i <<= j;
      i >>= j;
      i >>>= j;

      // DoubleLiteral
      d = 3.141592653589793;

      // EqualExpression
      z = i == j;
      z = i != j;
      z = i < j;
      z = i <= j;
      z = i > j;
      z = i >= j;

      // ExtendedStringLiteral
      // TODO: ????

      // FalseLiteral
      z = false;

      // FieldReference, QualifiedSuperReference, QualifiedThisReference,
      // SuperReference, ThisReference
      Inner other = new Inner();
      i = i + j + x + y;
      i = this.i + this.j + this.x + this.y;
      i = Main.i + Main.j;
      i = Inner.i + Inner.j;
      i = Super.i + Super.j;
      i = other.i + other.j + other.x + other.y;
      i = Inner.this.i + Inner.this.j + Inner.this.x + Inner.this.y;
      i = Main.this.i + Main.this.j + Main.this.x + Main.this.y;
      i = super.i + super.j + super.x + super.y;
      i = Inner.super.i + Inner.super.j + Inner.super.x + Inner.super.y;
      i = Main.super.i + Main.super.j + Main.super.x + Main.super.y;

      // FloatLiteral
      f = 3.1415927f;

      // InstanceOfExpression
      z = o instanceof Super;

      // IntLiteral
      i = 4;

      // IntLiteralMinValue
      i = -2147483648;

      // LongLiteral
      l = 4L;

      // LongLiteralMinValue
      l = -9223372036854775808L;

      // MessageSend, QualifiedSuperReference, QualifiedThisReference,
      // SuperReference, ThisReference
      foo();
      this.foo();
      other.foo();
      Main.this.foo();
      super.foo();
      Inner.super.foo();
      Main.super.foo();

      sfoo();
      this.sfoo();
      Main.sfoo();
      Inner.sfoo();
      Super.sfoo();
      other.sfoo();
      Main.this.sfoo();
      super.sfoo();
      Inner.super.sfoo();
      Main.super.sfoo();

      // NullLiteral
      o = null;

      // OR_OR_Expression
      z = i == 1 || j == 2;

      // PostfixExpression
      i++;
      i--;

      // PrefixExpression
      ++i;
      --i;

      // QualifiedAllocationExpression
      o = new Inner();
      o = Main.this.new Inner();
      o = new Main().new Inner();

      // QualifiedNameReference
      // TODO: fields????
      Main m = new Main();
      i = ia.length;
      i = m.j;
      i = m.y;
      i = new Main().j;
      i = new Main().y;
      i = m.next.j;
      i = m.next.y;
      i = new Main().next.j;
      i = new Main().next.y;
      i = m.getNext().j;
      i = m.getNext().y;
      i = new Main().getNext().j;
      i = new Main().getNext().y;

      // QualifiedTypeReference
      // TODO: ????

      // SingleNameReference
      int asdf;
      asdf = i;
      i = asdf;

      // StringLiteral
      s = "f'oo\b\t\n\f\r\"\\";

      // TrueLiteral
      z = true;

      // TypeReference
      // TODO: ????

      // UnaryExpression
      i = -i;
      i = ~i;
      z = !z;

      // AssertStatement
      assert i == 2;
      assert i == 3 : null;
      
      // BreakStatement, ContinueStatement
      outer : while (z) {
        inner : while (z) {
          if (i == 1)
            break;
          if (i == 2)
            break outer;
          if (i == 3)
            continue;
          if (i == 4)
            continue outer;
        }
      }

      // CaseStatement, SwitchStatement
      switch (j) {
        case 1: ++i;
        case 2: ++i;
        case 3: ++i; 
        case 4: ++i;
        default: ++i;
      }
      
      // DoStatement
      do
        i = j;
      while(z);

      do {
        i = j;
        x = y;
      } while(z);
      
      // EmptyStatement
      ;
      
// // ForeachStatement; 5.0 only
// for (int q : ia) {
// i = q;
// }
      
      // ForStatement
      for (int q = 0, v = j; q < v; ++q)
        i = q;
      
      // IfStatement
      if (z)
        i = j;
      else
        i = x;
      
      // LabeledStatement
      label: i = j;
      
      // LocalDeclaration
      int aaa;
      int bbb = j;
      int ccc, ddd;
      int eee = 4, fff = 5;
      
      // ReturnStatement
      if (z)
        return;
      
      // SynchronizedStatement
      synchronized (other) {
        other.x = i;
      }
      
      // ThrowStatement, TryStatement
      try {
        throw new Exception();
      } catch (Exception e) {
        i = j;
      } catch (Throwable t) {
        x = y;
      }
      
      try {
        i = j;
      } finally {
        x = y;
      }

      try {
        try {
          i = j;
        } catch (Throwable t) {
          throw t;
        }
      } catch (Throwable t) {
        i = j;
      } finally {
        x = y;
      }

      // WhileStatement
      while (z)
        i = j;

      while (z) {
        i = j;
        x = y;
      }

    }

    public void foo() {
      final int z = this.y;
      
      new Inner() {
        {
          x = z;
          this.x = z;
          Inner.this.x = z;
          next = Main.this.next;
          next.foo();
          Main.this.next.foo();
          Main.this.x = z;
          Main.super.x = z; // inexpressible in Java without name mangling
        }
        public void foo() {
          x = z;
          this.x = z;
          Inner.this.x = z;
          next = Main.this.next;
          next.foo();
          Main.this.next.foo();
          Main.this.x = z;
          Main.super.x = z; // inexpressible in Java without name mangling
        }
      };
      
      class NamedLocal extends Inner {
        public void foo() {
          Main.this.getNext();
          Inner.this.foo();
          super.foo();
          int x = z;
        }
        // JDT bug?  This works in 5.0 but not in 1.4
        // TODO: will javac compile it?
//        class NamedLocalSub extends NamedLocal {
//          public void foo() {
//            Main.this.getNext();
//            Inner.this.foo();
//            NamedLocal.this.foo();
//            super.foo();
//            int x = z;
//          }
//        }
      };
      
//      new NamedLocal().new NamedLocalSub().foo();
      new InnerSub().new InnerSubSub().fda();
      new SecondMain().new FunkyInner();
    }

  }
  
  private static class InnerSub extends Inner {
    InnerSub() {
      new Main().super();
    }
    
    private int asdfasdfasdf = 3;
    
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
  }
  
  private static class SecondMain {
    private class FunkyInner extends Inner {
      FunkyInner() {
        new Main().super();
      }
    }
  }

}