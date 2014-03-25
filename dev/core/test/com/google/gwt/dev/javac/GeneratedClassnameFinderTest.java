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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.CompilationUnit.GeneratedClassnameFinder;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.util.List;

/**
 * Test-cases to check that we indeed obtain the correct list of nested types
 * with generated classNames by examining bytecodes using ASM.
 * 
 */
public class GeneratedClassnameFinderTest extends TestCase {
  enum EnumClass {
    A, B, C,
  }

  static class MainClass {
    static class NestedClass {
      void foo() {
        TestInterface c = new TestInterface() {
          @Override
          public void foo() {
          }
        };
        EnumClass et = EnumClass.A;
        switch (et) {
          case A:
            break;
        }
        TestInterface d = new TestInterface() {
          @Override
          public void foo() {
          }
        };
      }
    }

    void foo() {
      TestInterface a = new TestInterface() {
        @Override
        public void foo() {
        }
      };
      EnumClass et = EnumClass.A;
      switch (et) {
        case A:
          break;
      }
      TestInterface b = new TestInterface() {
        @Override
        public void foo() {
        }
      };
    }
  }
  interface TestInterface {
    void foo();
  }

  static final TreeLogger logger = new PrintWriterTreeLogger();

  public void test() {
    String mainClassName = this.getClass().getName().replace('.', '/');
    assertEquals(
        4,
        new GeneratedClassnameFinder(logger, mainClassName).getClassNames().size());
    assertEquals(0, new GeneratedClassnameFinder(logger, mainClassName
        + "$EnumClass").getClassNames().size());
    assertEquals(0, new GeneratedClassnameFinder(logger, mainClassName
        + "$TestInterface").getClassNames().size());
    assertEquals(4, new GeneratedClassnameFinder(logger, mainClassName
        + "$MainClass").getClassNames().size());
    assertEquals(2, new GeneratedClassnameFinder(logger, mainClassName
        + "$MainClass$NestedClass").getClassNames().size());
  }

  public void testAbstractNative() {
    assertEquals(2, new AbstractNativeTester().getGeneratedClasses().size());
  }

  public void testAnonymous() {
    assertEquals(1, new AnonymousTester().getGeneratedClasses().size());
  }

  public void testEnum() {
    assertEquals(0, new EnumTester().getGeneratedClasses().size());
  }

  public void testJavacWeirdness() {
    List<String> classNames = new JavacWeirdnessTester().getGeneratedClasses();
    if (classNames.size() == 3) {
      // javac7 - JavacWeirdnessTester$1 doesn't verify, so it's excluded
      assertTrue(classNames.get(0) + " should not contain Foo",
          classNames.get(0).indexOf("Foo") == -1);
      assertTrue(classNames.get(1) + " should contain Foo",
          classNames.get(1).indexOf("Foo") != -1);
      assertTrue(classNames.get(2) + " should contain Foo",
          classNames.get(2).indexOf("Foo") != -1);
    } else if (classNames.size() == 4) {
      // javac8:
      // JavacWeirdnessTester$1
      // JavacWeirdnessTester$2
      // JavacWeirdnessTester$2Foo
      // JavacWeirdnessTester$3Foo
      assertTrue(classNames.get(0) + " should not contain Foo",
        classNames.get(0).indexOf("Foo") == -1);
      assertTrue(classNames.get(1) + " should not contain Foo",
        classNames.get(1).indexOf("Foo") == -1);
      assertTrue(classNames.get(2) + " should contain Foo",
          classNames.get(2).indexOf("Foo") != -1);
      assertTrue(classNames.get(3) + " should contain Foo",
        classNames.get(3).indexOf("Foo") != -1);
    } else {
      fail();
    }
  }

  public void testNamedLocal() {
    assertEquals(2, new NamedLocalTester().getGeneratedClasses().size());
  }

  public void testNested() {
    assertEquals(2, new NestedTester().getGeneratedClasses().size());
  }

  public void testStatic() {
    assertEquals(0, new StaticTester().getGeneratedClasses().size());
  }

  public void testTopLevel() {
    assertEquals(1, new TopLevelTester().getGeneratedClasses().size());
  }


}

/**
 * For testing a class containing anonymous inner classes with abstract and
 * native methods.
 */
class AbstractNativeTester {
  void foo() {
    abstract class Fooer {
      abstract void foo();
    }
    Fooer a = new Fooer() {
      @Override
      native void foo();
    };
    a.foo();
  }

  List<String> getGeneratedClasses() {
    return (new GeneratedClassnameFinder(GeneratedClassnameFinderTest.logger,
        this.getClass().getName().replace('.', '/'))).getClassNames();
  }
}

/**
 * For testing a class containing an anonymous inner class.
 */
class AnonymousTester {
  interface TestInterface {
    void foo();
  }

  void foo() {
    TestInterface a = new TestInterface() {
      @Override
      public void foo() {
      }
    };
    a.foo();
  }

  List<String> getGeneratedClasses() {
    return (new GeneratedClassnameFinder(GeneratedClassnameFinderTest.logger,
        this.getClass().getName().replace('.', '/'))).getClassNames();
  }
}

/**
 * For testing a class with an Enum (for which javac generates a synthetic
 * class).
 */
class EnumTester {
  enum EnumClass {
    A, B, C,
  }

  void foo() {
    EnumClass et = EnumClass.A;
    switch (et) {
      case A:
        break;
    }
  }

  List<String> getGeneratedClasses() {
    return (new GeneratedClassnameFinder(GeneratedClassnameFinderTest.logger,
        this.getClass().getName().replace('.', '/'))).getClassNames();
  }
}

/**
 * Javac generates weird code for the following class. It passes a synthetic
 * class ...Tester$1 as a first parameter to constructors of Fuji and Granny.
 * Normally, it generates the synthetic class, but in this case, it decides not
 * to generate the class. However, the bytecode still has reference to the
 * synthetic class -- it just passes null for the synthetic class.
 * 
 * This code also tests for an anonymous class extending a named local class.
 */
class JavacWeirdnessTester {
  private abstract static class Apple implements Fruit {
  }

  private static interface Fruit {
  }

  private static class Fuji extends Apple {
  }

  private static class Granny extends Apple {
  }

  private static volatile boolean TRUE = true;

  List<String> getGeneratedClasses() {
    return (new GeneratedClassnameFinder(GeneratedClassnameFinderTest.logger,
        this.getClass().getName().replace('.', '/'))).getClassNames();
  }

  private void assertEquals(Object a, Object b) {
  }

  private void testArrayStore() {
    Apple[] apple = TRUE ? new Granny[3] : new Apple[3];
    Apple g = TRUE ? (Apple) new Granny() : (Apple) new Fuji();
    Apple a = apple[0] = g;
    assertEquals(g, a);
  }

  private void testDeadTypes() {
    if (false) {
      new Object() {
      }.toString();

      class Foo {
        void a() {
        }
      }
      new Foo().a();
    }
  }

  private void testLocalClasses() {
    class Foo {
      public Foo(int j) {
        assertEquals(1, j);
      };
    }
    final int i;
    new Foo(i = 1) {
      {
        assertEquals(1, i);
      }
    };
    assertEquals(1, i);
  }

  private void testReturnStatementInCtor() {
    class Foo {
      int i;

      Foo(int i) {
        this.i = i;
        if (i == 0) {
          return;
        } else if (i == 1) {
          return;
        }
        return;
      }
    }
    assertEquals(new Foo(0).i, 0);
  }
}

/**
 * For testing a class with a generated name like $1Foo.
 */
class NamedLocalTester {
  void foo1() {
    if (false) {
      class Foo {
        void foo() {
        }
      }
      new Foo().foo();
    }
  }

  void foo2() {
    class Foo {
      void foo() {
      }
    }
    new Foo().foo();
  }

  void foo3() {
    class Foo {
      void foo() {
      }
    }
    new Foo().foo();
  }

  List<String> getGeneratedClasses() {
    return (new GeneratedClassnameFinder(GeneratedClassnameFinderTest.logger,
        this.getClass().getName().replace('.', '/'))).getClassNames();
  }
}

/**
 * For testing that nested classes are examined recursively for classes with
 * generated names.
 */
class NestedTester {
  class MainClass {
    class NestedClass {
      void foo() {
        class Foo {
          void bar() {
          }
        }
        new Foo().bar();
      }
    }

    void foo() {
      class Foo {
        void bar() {
        }
      }
      new Foo().bar();
    }
  }

  List<String> getGeneratedClasses() {
    return (new GeneratedClassnameFinder(GeneratedClassnameFinderTest.logger,
        this.getClass().getName().replace('.', '/'))).getClassNames();
  }
}

/**
 * For testing classes with private static members (javac generates a synthetic
 * class here but the jdt does not).
 */
class StaticTester {
  private abstract static class Apple implements Fruit {
  }

  private static interface Fruit {
    void bar();
  }

  private static class Fuji extends Apple {
    @Override
    public void bar() {
    }
  }

  private static class Granny extends Apple {
    @Override
    public void bar() {
    }
  }

  List<String> getGeneratedClasses() {
    return (new GeneratedClassnameFinder(GeneratedClassnameFinderTest.logger,
        this.getClass().getName().replace('.', '/'))).getClassNames();
  }

}

/**
 * For testing that a class with a generated name inside another top-level class
 * is found.
 */
class TopLevelTester {
  public void foo() {
    GeneratedClassnameFinderTest.TestInterface a = new GeneratedClassnameFinderTest.TestInterface() {
      @Override
      public void foo() {
      }
    };
  }

  List<String> getGeneratedClasses() {
    return (new GeneratedClassnameFinder(GeneratedClassnameFinderTest.logger,
        this.getClass().getName().replace('.', '/'))).getClassNames();
  }
}
