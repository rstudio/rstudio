/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.safehtml.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A tag interface that facilitates compile-time binding of HTML templates to
 * generate SafeHtml strings.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 *   public interface MyTemplate extends SafeHtmlTemplates {
 *     &#064;Template("&lt;span class=\"{3}\"&gt;{0}: &lt;a href=\"{1}\"&gt;{2}&lt;/a&gt;&lt;/span&gt;")
 *     SafeHtml messageWithLink(SafeHtml message, String url, String linkText,
 *       String style);
 *   }
 *
 *   private static final MyTemplate TEMPLATE = GWT.create(MyTemplate.class);
 *
 *   public void useTemplate(...) {
 *     SafeHtml message;
 *     String url;
 *     String linkText;
 *     String style;
 *     // ...
 *     SafeHtml messageWithLink =
 *       TEMPLATE.messageWithLink(message, url, linkText, style);
 *   }
 * </pre>
 *
 * <p>
 * Instantiating a {@code SafeHtmlTemplates} interface with {@code GWT.create()}
 * returns an instance of an implementation that is generated at compile time.
 * The code generator parses the value of each template method's
 * {@code @Template} annotation as an HTML template, with template
 * variables denoted by curly-brace placeholders that refer by index to the
 * corresponding template method parameter.
 *
 * <p>
 * The code generator's template parser is lenient, and will accept HTML that is
 * not well-formed; the accepted set of HTML is similar to what is typically
 * accepted by browsers. However, the following constraints on the HTML template
 * are enforced:
 *
 * <ol>
 * <li>Template variables may not appear in a JavaScript context (inside a
 * {@code <script>} tag, or in an {@code onClick} etc handler).</li>
 * <li>Template variables may not appear inside HTML comments.</li>
 * <li>If a template variable appears inside the value of an attribute, the
 * value must be enclosed in quotes.</li>
 * <li>Template variables may not appear in the context of an attribute name,
 * nor elsewhere inside a tag except within a quoted attribute value.</li>
 * <li>The template must end in "inner HTML" context, and not inside a tag or
 * attribute.</li>
 * </ol>
 *
 * <p>
 * <b>Limitation:</b> For templates with template variables in a CSS (style)
 * context, the current implementation of the code generator does not guarantee
 * that the generated template method produces values that adhere to the
 * {@code SafeHtml} type contract. When the code generator encounters a template
 * with a variable in a style attribute or tag, such as,
 * {@code <div style=\"{0}\">}, a warning will be emitted. In this
 * case, developers are advised to carefully review uses of this template to
 * ensure that parameters passed to the template are from a trusted source or
 * suitably sanitized.
 *
 * <p> Future implementations of the code generator may place additional
 * constraints on template parameters in style contexts.
 */
public interface SafeHtmlTemplates {

  /**
   * The HTML template.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface Template {
    String value();
  }
}
