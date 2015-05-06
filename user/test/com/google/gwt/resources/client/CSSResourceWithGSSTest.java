/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.resources.client;


/**
 * Contains various full-stack tests of the CssResource system with gss enabled.
 */
public class CSSResourceWithGSSTest extends CSSResourceTest {

  @Override
  public String getModuleName() {
    return "com.google.gwt.resources.ResourcesGssTest";
  }

  @Override
  public void testCss() {
    FullTestCss css = Resources.INSTANCE.css();
    String text = css.getText();

    // Check the sprite
    assertTrue(text.contains("height:16px"));
    assertTrue(text.contains("width:16px"));

    // Check the value() expansion
    assertTrue(text.contains("offset-left:\"guard\" 16px!important"));
    assertTrue(text.contains("offset:16px 16px"));

    // Make sure renaming works
    assertFalse("replacement".equals(css.replacement()));
    assertTrue(text.contains("." + css.replacement()));
    assertTrue(text.contains("." + css.replacement() + ":after"));
    assertTrue(text.contains("." + css.replacementNotJavaIdent()));

    // Make sure renaming for multi-class selectors (.foo.bar) works
    assertFalse("multiClassA".equals(css.multiClassA()));
    assertFalse("multiClassB".equals(css.multiClassB()));
    assertTrue(text.contains("." + css.multiClassA() + "." + css.multiClassB()));

    // Check static if evaluation
    assertTrue(text.contains("static:passed"));
    assertFalse(text.contains("FAIL"));

    // Check runtime if evaluation
    assertTrue(text.contains("runtime:passed"));

    // Check interestingly-named idents
    assertTrue(text.contains("-some-wacky-extension"));
    assertTrue(text.contains("another-extension:-bar"));
    assertTrue(text.contains("-unescaped-hyphen:-is-better"));
    assertTrue(text.contains("with_underscore:_is_better"));

    assertTrue(text.contains("ns:tag"));
    assertTrue(text.contains("ns:tag:pseudo"));
    assertTrue(text.contains("ns:tag::double-pseudo"));
    assertTrue(text.contains("ns:tag::-webkit-scrollbar"));

    // Check escaped string values
    // GSS outputs an extra whitespace which is okay, see:
    // http://www.w3.org/TR/css3-syntax/#consume-an-escaped-code-point 
    assertTrue(text.contains("Hello\\\\\\000022  world"));

    // Check values
    assertFalse(text.contains("0.0;"));
    assertFalse(text.contains("0.0px;"));
    assertFalse(text.contains("0px;"));
    assertTrue(text.contains("background-color:#fff"));
    assertTrue(text.contains("content:\"bar\""));

    // Check data URL expansion
    assertTrue(text.contains("backgroundTopLevel:url("
            + Resources.INSTANCE.dataMethod().getSafeUri().asString() + ")"));
    assertTrue(text.contains("backgroundNested:url("
            + Resources.INSTANCE.nested().dataMethod().getSafeUri().asString() + ")"));
    assertTrue(text.contains("backgroundCustom:url("
            + Resources.INSTANCE.customDataMethod().getSafeUri().asString() + ")"));
    assertTrue(text.contains("backgroundImage:url("
            + Resources.INSTANCE.spriteMethod().getSafeUri().asString() + ")"));
    assertTrue(text.contains("backgroundImageNested:url("
            + Resources.INSTANCE.nested().spriteMethod().getSafeUri().asString() + ")"));
    assertTrue(text.contains("backgroundImageCustom:url("
            + Resources.INSTANCE.customSpriteMethod().getSafeUri().asString() + ")"));

    // Check @eval expansion
    assertTrue(text.contains(red()));

    // Check @def substitution
    assertTrue(text.contains("50px"));
    // Check @def substitution into function arguments
    // Note that GWT transforms rgb(R, G, B) into #rrggbb form.
    assertTrue(text.contains("-moz-linear-gradient(left,#007f00,#00007f 50%)"));
    assertTrue(text.contains("-webkit-linear-gradient(left,#007f00,#00007f 50%)"));
    assertTrue(text.contains("linear-gradient(to right,#007f00,#00007f 50%)"));

    // Check merging semantics
    assertTrue(text.indexOf("merge:merge") != -1);
    assertTrue(text.indexOf("merge:merge") < text.indexOf("."
        + css.mayNotCombine()));
    assertTrue(text.indexOf("may-not-combine") < text.indexOf("prevent:true"));
    assertTrue(text.indexOf("prevent:true") < text.indexOf("prevent-merge:true"));
    assertTrue(text.indexOf("prevent:true") < text.indexOf("."
        + css.mayNotCombine2()));

    // Check commonly-used CSS3 constructs
    assertTrue(text.contains("background-color:rgba(0,0,0,0.5)"));

    // Check external references
    assertEquals("externalA", css.externalA());
    assertTrue(text.contains(".externalA ." + css.replacement()));
    assertTrue(text.contains(".externalB"));
    assertTrue(text.contains(".externalC"));

    // Test font-face contents
    assertTrue(text.contains("url(Foo.otf) format(\"opentype\")"));
  }

  @Override
  public void testFileChoice() {
    // resource without @Source annotation
    ResourceFileChooser css = Resources.INSTANCE.resourceFileChooser();
    // should use the gss file.
    String expectedCss = "." + css.myClass() + "{width:10px}";
    assertEquals(expectedCss, css.getText());

    // resource with @Source annotation targeting one .css file
    css = Resources.INSTANCE.resourceFileChooserWithSourceTargetingOneCssFile();
    // should use the gss file instead of the css file.
    assertEquals(expectedCss, css.getText());

    // resource with @Source annotation targeting one .gss file
    css = Resources.INSTANCE.resourceFileChooserWithSourceTargetingOneGssFile();
    // should use the gss file
    assertEquals(expectedCss, css.getText());

    // resource with @Source annotation targeting several .css files
    css = Resources.INSTANCE.resourceFileChooserWithSourceTargetingCssFiles();
    // should use the gss files instead of the css files
    expectedCss = "." + css.myClass() + "{width:10px;margin:10px}";
    assertEquals(expectedCss, css.getText());

    // resource with @Source annotation targeting several .gss files
    css = Resources.INSTANCE.resourceFileChooserWithSourceTargetingGssFiles();
    // should use the gss files
    assertEquals(expectedCss, css.getText());

    // resource with @Source annotation targeting several .css files but one css file doesn't have
    // a corresponding gss file.
    css = Resources.INSTANCE.resourceFileChooserWithSourceTargetingCssFilesWithoutGssFile();
    // should use the css file (will be auto-converted)
    expectedCss = "." + css.myClass() + "{width:5px;padding:5px;height:5px}";
    assertEquals(expectedCss, css.getText());
  }
}
