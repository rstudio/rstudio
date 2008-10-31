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

package com.google.gwt.i18n.client.resolutiontest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.Localizable;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.TestConstants;
import com.google.gwt.i18n.client.resolutiontest.Inners.InnerClass.ProtectedInnerInnerClass.ExtendsAnotherInner;
import com.google.gwt.i18n.client.resolutiontest.Inners.InnerClass.ProtectedInnerInnerClass.ExtendsAnotherInner.ExtendProtectedInner;
import com.google.gwt.i18n.client.resolutiontest.Inners.OuterLoc.InnerLoc;

import java.util.Map;

/**
 * Test class.
 */
public class Inners {

  public static String testInnerLoc() {
    // Check being able to create inner
    InnerLoc loc = (InnerLoc) GWT.create(InnerLoc.class);
    return loc.string();
  }

  /**
   * Check binding to static inner.
   */
  public static class OuterLoc implements Localizable {
    public String string() {
      return "default";
    }

    /**
     * Test Inner Localizable.
     */
    protected abstract static class InnerLoc implements Localizable {
      public abstract String string();
    }

    /**
     * Inner Localizable Impl.
     */
    protected static class InnerLoc_ extends InnerLoc {
      public String string() {
        return "InnerLoc";
      }
    }
  }

  /**
   * Test Extension of Inner Inner.
   */
  public interface ExtendsInnerInner extends InnerClass.InnerInner {
    String extendsInnerInner();
  }

  /** Test inner localizable. */
  public interface LocalizableSimpleInner extends Localizable {
    String getLocalizableInner();
  }

  /**
   * Tests Localizable, Must be static.
   */
  public static class LocalizableSimpleInner_ implements LocalizableSimpleInner {

    public String getLocalizableInner() {
      return "getLocalizableInner";
    }
  }

  /**
   * Test embedded constant.
   */
  public interface HasInner extends SimpleInner {

    /**
     * Test inner or constant.
     */
    public interface IsInner extends Constants {
      int isInner();
    }

    String hasInner();
  }

  /**
   * Inner class.
   */
  public static class InnerClass {

    /** Test inner inner,abstract,and static localizable. */
    public abstract static class LocalizableInnerInner implements Localizable {
      public abstract String string();
    }

    /** Test inner inner and static localizable. */
    public static class LocalizableInnerInner_ extends LocalizableInnerInner {
      public String string() {
        return "localizableInnerInner";
      }
    }

    /**
     * Messages use the same resolution mechanisms as constants, but including a
     * couple of message cases to sanity check.
     */
    public static interface InnerInnerMessages extends Messages {
      String innerClassMessages(String a);
    }

    /** Test inner interface of inner interface. */
    public interface InnerInner extends Outer {
      float innerInner();
    }

    /**
     * OuterLoc piglatin binding.
     */
    public static class OuterLoc_piglatin extends OuterLoc {
      public String string() {
        return "piglatin";
      }
    }

    /** Tests Protected Inner Class. */
    public Map<String, String> testExtendsAnotherInner() {
      ExtendsAnotherInner clazz = (ExtendsAnotherInner) GWT.create(ExtendsAnotherInner.class);
      Map<String, String> answer = clazz.extendsAnotherInner();
      return answer;
    }

    /** Test for ExtendProtectedInner. */
    public String testExtendsProtectedInner() {
      ExtendProtectedInner inner = (ExtendsAnotherInner.ExtendProtectedInner) GWT.create(ExtendProtectedInner.class);
      return inner.extendProtectedInner();
    }

    /** Another inner class. */
    protected static class ProtectedInnerInnerClass {

      /** Test extension. */
      public interface ExtendsAnotherInner extends InnerInner {

        /** Test protected extension. */
        interface ExtendProtectedInner extends ProtectedInner {
          String extendProtectedInner();
        }

        /** Test maps in extension. */
        Map<String, String> extendsAnotherInner();
      }

      /**
       * Checks inner inner and across packages.
       */
      public interface InnerInnerInner extends TestConstants {
        String[] innerInnerInner();
      }

      /**
       * Messages use the same resolution mechanisms as constants, but including
       * a couple of message cases to sanity check.
       */
      public static interface InnerInnerInnerMessages extends
          InnerInnerMessages {
        String innerClassMessages(String a);
      }
    }
  }

  /** Tests Simple Constant inheritance. */
  public interface SimpleInner extends TestConstants {
    String simpleInner();
  }

  /**
   * Boolean return from protected inner.
   */
  protected interface ProtectedInner extends TestConstants {
    boolean protectedInner();
  }

  /**
   * Testing protected inner.
   */
  public boolean testProtectedInner() {
    ProtectedInner inner = (ProtectedInner) GWT.create(ProtectedInner.class);
    return inner.protectedInner();
  }
}

/** Used to test extension from package protected file. */
interface Outer extends Constants {
  String outer();
}
