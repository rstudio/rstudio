/*
 * Copyright 2007 Google Inc.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A tag interface that facilitates locale-sensitive, compile-time binding of
 * constant values supplied from properties files. Using
 * <code>GWT.create(<i>class</i>)</code> to "instantiate" an interface that
 * extends <code>Constants</code> returns an instance of an automatically
 * generated subclass that is implemented using values from a property file
 * selected based on locale.
 * 
 * <p>
 * Locale is specified at run time using a meta tag or query string as described
 * for {@link com.google.gwt.i18n.client.Localizable}.
 * </p>
 * 
 * <h3>Extending <code>Constants</code></h3>
 * To use <code>Constants</code>, begin by defining an interface that extends
 * it. Each interface method is referred to as a <i>constant accessor</i>, and
 * its corresponding localized value is loaded based on the key for that method.
 * The default key is simply the unqualified name of the method, but can be specified
 * directly with an {@code @Key} annotation or a different generation method using
 * {@code @GenerateKeys}.  Also, the default value can be specified in an annotation
 * rather than a default properties file (and some key generators may require the value
 * to be given in the source file via annotations). For example,
 * 
 * {@example com.google.gwt.examples.i18n.NumberFormatConstants}
 * 
 * expects to find properties named <code>decimalSeparator</code> and
 * <code>thousandsSeparator</code> in an associated properties file. For
 * example, the following properties would be used for a German locale:
 * 
 * {@gwt.include com/google/gwt/examples/i18n/NumberFormatConstants_de_DE.properties}
 * 
 * <p>
 * The following example demonstrates how to use constant accessors defined in
 * the interface above:
 * 
 * {@example com.google.gwt.examples.i18n.NumberFormatConstantsExample#useNumberFormatConstants()}
 * </p>
 * 
 * <p>
 * Here is the same example using annotations to store the default values:
 * 
 * {@example com.google.gwt.examples.i18n.NumberFormatConstantsAnnot}
 * </p>
 * 
 * <p>
 * It is also possible to change the property name bound to a constant accessor
 * using the {@code @Key} annotation. For example,
 * {@example com.google.gwt.examples.i18n.NumberFormatConstantsWithAltKey}
 * 
 * would match the names of the following properties:
 * 
 * {@gwt.include com/google/gwt/examples/i18n/NumberFormatConstantsWithAltKey_en.properties}
 * </p>
 * 
 * <h3>Defining Constant Accessors</h3>
 * Constant accessors must be of the form
 * 
 * <pre>T methodName()</pre>
 * 
 * where <code>T</code> is one of the return types in the following table:
 * 
 * <table>
 * <tr>
 * <th><nobr>If the return type is...&#160;&#160;&#160;</nobr></th>
 * <th>The property value is interpreted as...</th>
 * <th>Annotation to use for default value</th>
 * </tr>
 * 
 * <tr>
 * <td><code>String</code></td>
 * <td>A plain string value</td>
 * <td>{@code @DefaultStringValue}</td>
 * </tr>
 * 
 * <tr>
 * <td><code>String[]</code></td>
 * <td>A comma-separated array of strings; use '<code>\\,</code>' to escape
 * commas</td>
 * <td>{@code @DefaultStringArrayValue}</td>
 * </tr>
 * 
 * <tr>
 * <td><code>int</code></td>
 * <td>An <code>int</code> value, checked during compilation</td>
 * <td>{@code @DefaultIntValue}</td>
 * </tr>
 * 
 * <tr>
 * <td><code>float</code></td>
 * <td>A <code>float</code> value, checked during compilation</td>
 * <td>{@code @DefaultFloatValue}</td>
 * </tr>
 * 
 * <tr>
 * <td><code>double</code></td>
 * <td>A <code>double</code> value, checked during compilation</td>
 * <td>{@code @DefaultDoubleValue}</td>
 * </tr>
 * 
 * <tr>
 * <td><code>boolean</code></td>
 * <td>A <code>boolean</code> value ("true" or "false"), checked during
 * compilation</td>
 * <td>{@code @DefaultBooleanValue}</td>
 * </tr>
 * 
 * <tr>
 * <td><code>Map</code></td>
 * <td>A comma-separated list of property names, each of which is a key into a
 * generated map; the value mapped to given key is the value of the property
 * having named by that key</td>
 * <td>{@code @DefaultStringMapValue}</td>
 * </tr>
 * 
 * </table>
 * 
 * <p>
 * As an example of a <code>Map</code>, for the following property file:
 * </p>
 * 
 * <pre>
 * a = X
 * b = Y
 * c = Z
 * someMap = a, b, c
 * </pre>
 * 
 * <p>
 * the constant accessor <code>someMap()</code> would return a
 * <code>Map</code> that maps <code>"a"</code> onto <code>"X"</code>,
 * <code>"b"</code> onto <code>"Y"</code>, and <code>"c"</code> onto
 * <code>"Z"</code>. Iterating through this <code>Map</code> will return
 * the keys or entries in declaration order.
 * </p>
 * 
 * <p>The benefit of using annotations, aside from not having to switch to
 * a different file to enter the default values, is that you can make use
 * of compile-time constants and not worrying about quoting commas.  For example:
 * 
 * {@example com.google.gwt.examples.i18n.AnnotConstants}
 * </p>
 * 
 * <h3>Binding to Properties Files</h3>
 * If an interface <code>org.example.foo.Intf</code> extends
 * <code>Constants</code> and the following code is used to create an object
 * from <code>Intf</code> as follows:
 * 
 * <pre class="code">Intf constants = (Intf)GWT.create(Intf.class);</pre>
 * 
 * then <code>constants</code> will be assigned an instance of a generated
 * class whose constant accessors are implemented by extracting values from a
 * set of matching properties files. Property values are sought using a
 * best-match algorithm, and candidate properties files are those which (1)
 * reside in the same package as the interface (<code>org/example/foo/</code>),
 * (2) have a base filename matching the interface name (<code>Intf</code>),
 * and (3) have a suffix that most closely matches the locale. Suffixes are
 * matched as follows:
 * 
 * <table>
 * 
 * <tr>
 * <th align='left'><nobr>If <code>locale</code> is...&#160;&#160;</nobr></th>
 * <th align='left'>The properties file that binds to
 * <code>org.example.foo.Intf</code> is...</th>
 * </tr>
 * 
 * <tr>
 * <td><i>unspecified</i></td>
 * <td><code>org/example/foo/Intf.properties</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>x</code></td>
 * <td><code>org/example/foo/Intf_x.properties</code> if it exists and
 * defines the property being sought, otherwise treated as if
 * <code>locale</code> were <i>unspecified</i></td>
 * </tr>
 * 
 * <tr>
 * <td><code>x_Y</code></td>
 * <td><code>org/example/foo/Intf_x_Y.properties</code> if it exists and
 * defines the property being sought, otherwise treated as if
 * <code>locale</code> were <code>x</code></td>
 * </tr>
 * 
 * </table> where <code>x</code> and <code>Y</code> are language and locale
 * codes, as described in the documentation for
 * {@link com.google.gwt.i18n.client.Localizable}.  Note that default values
 * supplied in the source file in annotations take precedence over those in
 * the default properties file, if it is also present.
 * 
 * <p>
 * Note that the matching algorithm is applied independently for each constant
 * accessor. It is therefore possible to create a hierarchy of related
 * properties files such that an unlocalized properties file acts as a baseline,
 * and locale-specific properties files may redefine a subset of those
 * properties, relying on the matching algorithm to prefer localized properties
 * while still finding unlocalized properties.
 * </p>
 * 
 * <h3>Required Module</h3>
 * Modules that use this interface should inherit
 * <code>com.google.gwt.i18n.I18N</code>.
 * 
 * {@gwt.include com/google/gwt/examples/i18n/InheritsExample.gwt.xml}
 * 
 * <h3>Note</h3>
 * You should not directly implement this interface or interfaces derived from
 * it since an implementation is generated automatically when message interfaces
 * are created using {@link com.google.gwt.core.client.GWT#create(Class)}.
 */
public interface Constants extends LocalizableResource {
  /**
   * Default boolean value to be used if no translation is found (and also used as the
   * source for translation).  No quoting (other than normal Java string quoting)
   * is done.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface DefaultBooleanValue {
    boolean value();
  }

  /**
   * Default double value to be used if no translation is found (and also used as the
   * source for translation).  No quoting (other than normal Java string quoting)
   * is done.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface DefaultDoubleValue {
    double value();
  }

  /**
   * Default float value to be used if no translation is found (and also used as the
   * source for translation).  No quoting (other than normal Java string quoting)
   * is done.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface DefaultFloatValue {
    float value();
  }

  /**
   * Default integer value to be used if no translation is found (and also used as the
   * source for translation).  No quoting (other than normal Java string quoting)
   * is done.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface DefaultIntValue {
    int value();
  }

  /**
   * Default string array value to be used if no translation is found (and also
   * used as the source for translation). No quoting (other than normal Java
   * string quoting) is done.
   * 
   * Note that in the corresponding properties/etc file, commas are used to separate
   * elements of the array unless they are preceded with a backslash.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface DefaultStringArrayValue {
    String[] value();
  }

  /**
   * Default string map value to be used if no translation is found (and also
   * used as the source for translation). No quoting (other than normal Java
   * string quoting) is done.  The strings for the map are supplied in key/value
   * pairs.
   * 
   * Note that in the corresponding properties/etc file, new keys can be supplied
   * with the name of the method (or its corresponding key) listing the set of keys
   * for the map separated by commas (commas can be part of the keys by preceding
   * them with a backslash).  In either case, further entries have keys matching
   * the key in this map.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface DefaultStringMapValue {
    /**
     * Must be key-value pairs.
     */
    String[] value();
  }

  /**
   * Default string value to be used if no translation is found (and also used as the
   * source for translation).  No quoting (other than normal Java string quoting)
   * is done.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface DefaultStringValue {
    String value();
  }
}
