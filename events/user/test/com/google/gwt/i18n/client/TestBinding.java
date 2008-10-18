/*
 * Copyright 2006 Google Inc.
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

package com.google.gwt.i18n.client;

/**
 * TODO: document me.
 */
public interface TestBinding extends Localizable {
  String a();

  String b();
}

class Wrapper {

  static class TestBinding_a extends TestBinding_ {
    public String a() {
      return "a";
    }
  }

  static class TestBinding_b_C_d extends TestBinding_ {
    public String a() {
      return "b_c_d";
    }
  }

  static class TestBinding_b implements TestBinding {

    public String a() {
      return "b";
    }

    public String b() {
      return "b";
    }
  }

  static class TestBinding_ implements TestBinding {

    public String a() {
      return "default";
    }

    public String b() {
      return "default";
    }
  }
}

class Wrapper2 {
  public abstract static class TestBindingImpl implements Localizable {
    abstract String a();

    abstract String b();
  }

  static class TestBindingImpl_a extends TestBindingImpl_b_C_d {
    public String a() {
      return "a";
    }
  }

  static class TestBindingImpl_b_C_d extends TestBindingImpl {
    public String a() {
      return "b_c_d";
    }

    public String b() {
      return "b_c_d";
    }
  }

  static class TestBinding extends TestBindingImpl {

    public String a() {
      return "never should be here";
    }

    public String b() {
      return "never should be here";
    }
  }

  static class TestBindingImpl_ extends TestBindingImpl {

    public String a() {
      return "default";
    }

    public String b() {
      return "default";
    }
  }
}
