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

package com.google.gwt.resources.client.gss;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Shared;

/**
 * ClientBundle that contains several CssResource for testing different scopes.
 */
public interface ScopeResource extends ClientBundle {
  /**
   * Not shared CssResource.
   */
  interface ScopeA extends CssResource {
    String foo();
  }

  /**
   * Not shared CssResource.
   */
  interface ScopeB extends ScopeA {
    String foo();
  }

  /**
   * Not shared CssResource.
   */
  interface ScopeC extends ScopeA {
    // Intentionally not defining foo()
  }

  /**
   * Shared CssResource.
   */
  @Shared
  interface SharedParent extends CssResource {
    String sharedClassName1();
    String sharedClassName2();
  }

  /**
   * Shared CssResource.
   */
  interface SharedChild1 extends SharedParent {
    String nonSharedClassName();
  }

  /**
   * Shared CssResource.
   */
  interface SharedChild2 extends SharedParent {
    String nonSharedClassName();
  }

  /**
   * Shared CssResource.
   */
  interface SharedGreatChild extends SharedChild2 {
    // Intentionally empty
  }

  SharedChild1 sharedChild1();

  SharedChild2 sharedChild2();

  SharedGreatChild sharedGreatChild();

  SharedParent sharedParent();

  ScopeA scopeA();

  ScopeA scopeA2();

  ScopeB scopeB();

  ScopeC scopeC();
}
