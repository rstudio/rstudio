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
package com.google.gwt.dev;

/**
 * Legacy ApplicationCreator that will let users know where the new class can be
 * found.
 */
public final class ApplicationCreator {

  public static void main(String[] args) {
    System.err.println("This application has moved!");
    System.err.println("Please run com.google.gwt.user.tools.ApplicationCreator in gwt-user.jar");
    System.exit(1);
  }

}
