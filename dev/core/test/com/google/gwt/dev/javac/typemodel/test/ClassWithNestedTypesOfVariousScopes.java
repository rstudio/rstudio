/*
 * Copyright 2014 Google Inc.
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

package com.google.gwt.dev.javac.typemodel.test;

/**
 * This class defines nested types of various scopes for testing access modifiers.
 */
public class ClassWithNestedTypesOfVariousScopes {

  /**
   * Public nested interface for testing purposes.
   */
  public interface PublicNestedInterface {
  }

  /**
   * Protected nested interface for testing purposes.
   */
  protected interface ProtectedNestedInterface {
  }

  /**
   * Package protected nested interface for testing purposes.
   */
  interface PackageProtectedNestedInterface {
  }

  /**
   * Private nested interface for testing purposes.
   */
  @SuppressWarnings("unused")
  private interface PrivateNestedInterface {
  }

  /**
   * Public nested class for testing purposes.
   */
  public class PublicNestedClass {
  }

  /**
   * Protected nested class for testing purposes.
   */
  protected class ProtectedNestedClass {
  }

  /**
   * Package protected nested class for testing purposes.
   */
  class PackageProtectedNestedClass {
  }

  /**
   * Private nested class for testing purposes.
   */
  @SuppressWarnings("unused")
  private class PrivateNestedClass {
  }

}
