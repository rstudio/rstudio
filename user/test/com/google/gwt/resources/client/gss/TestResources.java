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
import com.google.gwt.resources.client.CssResource.Import;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.gss.ScopeResource.SharedParent;

/**
 * Contains various CssResource used to test the CssResource system with GSS.
 */
public interface TestResources extends ClientBundle {
  /**
   * Inner ClientBundle with a reference to an ImageResource.
   */
  public interface ImageResources extends ClientBundle {
        @Source("someImageResource.png")
        ImageResource someResource();
  }

  /**
   * Simple CssResource.
   */
  interface SomeGssResource extends CssResource {
    String someClass();
  }

  /**
   * Used to test sprite definition.
   */
  interface SpriteGssResource extends CssResource {
    String someClassWithSprite();

    String embeddedSprite();

    // define a style class having the same name than another resource in the ClientBundle
    // test possible conflict
    String someImageResource();
  }

  /**
   * Used to test {@code @external} at-rule.
   */
  interface ExternalClasses extends CssResource {
    String obfuscatedClass();

    String externalClass();

    String externalClass2();

    String unobfuscated();

    String unobfuscated2();
  }

  /**
   * Used to test that GSS file can contains empty style class definition.
   */
  interface EmptyClass extends CssResource {
    String empty();
  }

  /**
   * Used to test constant definition.
   */
  interface WithConstant extends CssResource {
    String constantOne();

    String classOne();
  }

  /**
   * Used to test {@link ClassName} annotation.
   */
  interface ClassNameAnnotation extends CssResource {
    @ClassName("renamed-class")
    String renamedClass();

    String nonRenamedClass();
  }

  /**
   * Used to test {@link Import} annotation.
   */
  interface TestImportCss extends CssResource {
    String other();
  }

  /**
   * Used to test shared annotation between clientBundle.
   */
  interface SharedChild3 extends SharedParent {
    String nonSharedClassName();
  }

  /**
   * Used to test conflict between style class name and constant name.
   */
  interface CssWithConstant extends CssResource {
    String constantOne();

    int constantTwo();

    String CONSTANT_THREE();

    String className1();

    String conflictConstantClass();

    int overrideConstantInt();

    @ClassName("overrideConstantInt")
    String overrideConstantIntClass();
  }

  /**
   * Used to test conditional nodes containing conditions evaluated at runtime.
   */
  interface RuntimeConditional extends CssResource {
    boolean CONSTANT_DEFINED_ON_INTERFACE = true;

    String foo();
  }

  /**
   * Used to test that the generator accepts non standard at-rules if they are defined in the
   * module file.
   */
  interface NonStandardAtRules extends CssResource {
    String foo();
  }

  /**
   * Used to test that the generator accepts non standard css function if they are defined in the
   * module file.
   */
  interface NonStandardFunctions extends CssResource {
    String foo();
  }

  /**
   * Used to test charset at-rule.
   */
  interface Charset extends CssResource {
  }

  /**
   * Used to test constants that use other constants.
   */
  interface Constants extends CssResource {
    String color1();

    int margin();

    String mycolor();

    String mycolor1();

    String padding2();

    int width();

    int bar();
  }

  /**
   * Used to test empty file.
   */
  interface Empty extends CssResource {
  }

  /**
   * Used to test For loop feature.
   */
  interface Forloop extends CssResource {
    @ClassName("foo-0")
    String foo0();

    @ClassName("foo-2")
    String foo2();

    @ClassName("foo-4")
    String foo4();
  }

  /**
   * Used to test vendor keyframes generation.
   */
  interface GenKeyFrames extends CssResource {
  }

  @Source("constants.gss")
  Constants constants();

  ClassNameAnnotation classNameAnnotation();

  SomeGssResource mixin();

  SomeGssResource add();

  SomeGssResource eval();

  SomeGssResource resourceUrl();

  SpriteGssResource sprite();

  ExternalClasses externalClasses();

  EmptyClass emptyClass();

  WithConstant withConstant();

  ImageResource someImageResource();

  @Source("bananaguitar.ani")
  DataResource someDataResource();

  @Import({ImportResource.ImportWithPrefixCss.class, ImportResource.ImportCss.class})
  TestImportCss testImportCss();

  SharedChild3 sharedChild3();

  CssWithConstant cssWithConstant();

  @NotStrict
  SomeGssResource notstrict();

  RuntimeConditional runtimeConditional();

  ImageResources embeddedImageResources();

  NonStandardAtRules nonStandardAtRules();

  NonStandardFunctions nonStandardFunctions();

  Charset charset();

  Empty empty();

  Forloop forloop();

  GenKeyFrames genKeyFrames();
}
