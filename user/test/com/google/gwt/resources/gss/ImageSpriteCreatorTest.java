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

package com.google.gwt.resources.gss;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.gss.ImageSpriteCreator.MethodByPathHelper;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssError;

/**
 * Test class for {@link ImageSpriteCreator}.
 */
public class ImageSpriteCreatorTest extends BaseGssTest {
  private JClassType imageResourceType;
  private ResourceContext resourceContext;
  private ErrorManager errorManager;
  private MethodByPathHelper methodByPathHelper;
  private JMethod imageMethod;

  @Override
  protected void setUp() {
    errorManager = mock(ErrorManager.class);
    imageResourceType = mock(JClassType.class);
    resourceContext = mockResourceContext();
    methodByPathHelper = mock(MethodByPathHelper.class);
    imageMethod = mock(JMethod.class);
  }

  public void testInvalidSpriteWithSeveralValue() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        ".someClassWithSprite { ",
        "  gwt-sprite: imageResource otherImageResource;",
        "}"));

    ImageSpriteCreator visitor = new ImageSpriteCreator(cssTree.getMutatingVisitController(),
        resourceContext, errorManager, methodByPathHelper);

    // when
    visitor.runPass();

    // then
    verify(errorManager).report(any(GssError.class));
  }

  public void testInvalidSpriteImageMethodNotFound() throws NotFoundException {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        ".someClassWithSprite { ",
        "  gwt-sprite: imageResource;",
        "}"));
    when(resourceContext.getClientBundleType()).thenReturn(mock(JClassType.class));
    when(methodByPathHelper.getMethodByPath(any(ResourceContext.class), anyList(),
        any(JClassType.class))).thenThrow(new NotFoundException(""));

    ImageSpriteCreator visitor = new ImageSpriteCreator(cssTree.getMutatingVisitController(),
        resourceContext, errorManager, methodByPathHelper);

    // when
    visitor.runPass();

    // then
    verify(errorManager).report(any(GssError.class));
  }

  public void testSimpleSpriteCreation() throws NotFoundException {
    testSpriteCreation("no-repeat", null);
  }

  public void testSpriteCreationWithHorizontalRepeat() throws NotFoundException {
    testSpriteCreation("repeat-x", RepeatStyle.Horizontal);
  }

  public void testSpriteCreationWithVerticalRepeat() throws NotFoundException {
    testSpriteCreation("repeat-y", RepeatStyle.Vertical);
  }

  public void testSpriteCreationWithRepeat() throws NotFoundException {
    testSpriteCreation("repeat", RepeatStyle.Both);
  }

  public void testSpriteCreationWithNoneRepeat() throws NotFoundException {
    testSpriteCreation("no-repeat", RepeatStyle.None);
  }

  private void testSpriteCreation(String repeat, RepeatStyle repeatStyle) throws NotFoundException {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        ".someClassWithSprite { ",
        "  color: white;",
        "  gwt-sprite: 'imageResource';",
        "  background-color: black;",
        "}"));

    when(methodByPathHelper.getMethodByPath(any(ResourceContext.class), anyList(),
        any(JClassType.class))).thenReturn(imageMethod);

    if (repeatStyle != null) {
      // simulate a @ImageOptions(repeatStyle)
      ImageOptions imageOptions = mock(ImageOptions.class);
      when(imageOptions.repeatStyle()).thenReturn(repeatStyle);
      when(imageMethod.getAnnotation(ImageOptions.class)).thenReturn(imageOptions);
    }

    ImageSpriteCreator visitor = new ImageSpriteCreator(cssTree.getMutatingVisitController(),
        resourceContext, errorManager, methodByPathHelper);

    // when
    visitor.runPass();

    // then
    verify(errorManager, never()).report(any(GssError.class));

    String widthRule = "[/* @alternate */]width:[ImageSpriteCreatorTest.this.imageResource()" +
        ".getWidth() + \"px\"], ";
    String heightRule = "[/* @alternate */]height:[ImageSpriteCreatorTest.this.imageResource()" +
        ".getHeight() + \"px\"], ";

    if (repeatStyle == RepeatStyle.Horizontal || repeatStyle == RepeatStyle.Both) {
      widthRule = "";
    }

    if (repeatStyle == RepeatStyle.Vertical || repeatStyle == RepeatStyle.Both) {
      heightRule = "";
    }

    String cssTreeStringExpected = "[" +
        "[.someClassWithSprite]{" +
        "[color:[white], " +
        heightRule +
        widthRule +
        "[/* @alternate */]overflow:[hidden], " +
        "[/* @alternate */]background:" +
        "[url(ImageSpriteCreatorTest.this.imageResource().getSafeUri().asString()), " +
        "\"-\" + ImageSpriteCreatorTest.this.imageResource().getLeft() + \"px\", " +
        "\"-\" + ImageSpriteCreatorTest.this.imageResource().getTop() + \"px\",  " +
        "" + repeat + "], " +
        "background-color:[black]]" +
        "}]";
    assertEquals(cssTreeStringExpected, cssTree.getRoot().getBody().toString());
  }

  private ResourceContext mockResourceContext() {
    ResourceContext context = mock(ResourceContext.class);
    when(context.getImplementationSimpleSourceName()).thenReturn("ImageSpriteCreatorTest");
    GeneratorContext generatorContext = mock(GeneratorContext.class);
    TypeOracle oracle = mock(TypeOracle.class);

    when(oracle.findType(ImageResource.class.getCanonicalName())).thenReturn(imageResourceType);

    when(generatorContext.getTypeOracle()).thenReturn(oracle);
    when(context.getGeneratorContext()).thenReturn(generatorContext);
    return context;
  }

  @Override
  protected void runPassesOnNewTree(CssTree cssTree, ErrorManager errorManager) {
  }
}
