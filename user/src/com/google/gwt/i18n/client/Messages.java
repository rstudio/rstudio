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

package com.google.gwt.i18n.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A tag interface that facilitates locale-sensitive, compile-time binding of
 * messages supplied from various sources.Using
 * <code>GWT.create(<i>class</i>)</code> to "instantiate" an interface that
 * extends <code>Messages</code> returns an instance of an automatically
 * generated subclass that is implemented using message templates selected based
 * on locale. Message templates are based on a subset of the format used by <a
 * href="http://download.oracle.com/javase/1.5.0/docs/api/java/text/MessageFormat.html">
 * <code>MessageFormat</code></a>.  Note in particular that single quotes are
 * used to quote other characters, and should be doubled for a literal single
 * quote.
 * 
 * <p>
 * Locale is specified at run time using a meta tag or query string as described
 * for {@link com.google.gwt.i18n.client.Localizable}.
 * </p>
 * 
 * <h3>Extending <code>Messages</code></h3>
 * To use <code>Messages</code>, begin by defining an interface that extends
 * it. Each interface method is referred to as a <i>message accessor</i>, and
 * its corresponding message template is loaded based on the key for that
 * method. The default key is simply the unqualified name of the method, but can
 * be specified directly with an {@code @Key} annotation or a different
 * generation method using {@code @GenerateKeys}. Additionally, if
 * plural forms are used on a given method the plural form is added as a suffix
 * to the key, such as <code>widgets[one]</code> for the singular version of
 * the <code>widgets</code> message. The resulting key is used to find
 * translated versions of the message from any supported input file, such as
 * Java properties files. For example,
 * 
 * {@example com.google.gwt.examples.i18n.GameStatusMessages}
 * 
 * expects to find properties named <code>turnsLeft</code> and
 * <code>currentScore</code> in an associated properties file, formatted as
 * message templates taking two arguments and one argument, respectively. For
 * example, the following properties would correctly bind to the
 * <code>GameStatusMessages</code> interface:
 * 
 * {@gwt.include com/google/gwt/examples/i18n/GameStatusMessages.properties}
 * 
 * <p>
 * The following example demonstrates how to use constant accessors defined in
 * the interface above:
 * 
 * {@example com.google.gwt.examples.i18n.GameStatusMessagesExample#beginNewGameRound(String)}
 * </p>
 * 
 * <p>The following example shows how to use annotations to store the default strings
 * in the source file itself, rather than needing a properties file (you still need
 * properties files for the translated strings):
 * 
 * {@example com.google.gwt.examples.i18n.GameStatusMessagesAnnot}
 * </p>
 * 
 * <p>In this example, calling <code>msg.turnsLeft("John", 13)</code> would
 * return the string <code>"Turns left for player 'John': 13"</code>.
 * </p>
 * 
 * <h3>Defining Message Accessors</h3>
 * Message accessors must be of the form
 * 
 * <pre>String methodName(<i>optional-params</i>)</pre>
 * 
 * and parameters may be of any type. Arguments are converted into strings at
 * runtime using Java string concatenation syntax (the '+' operator), which
 * uniformly handles primitives, <code>null</code>, and invoking
 * <code>toString()</code> to format objects.
 * 
 * <p>
 * Compile-time checks are performed to ensure that the number of placeholders
 * in a message template (e.g. <code>{0}</code>) matches the number of
 * parameters supplied.
 * </p>
 * 
 * <p>
 * Integral arguments may be used to select the proper plural form to use for
 * different locales. To do this, mark the particular argument with
 * {@code @PluralCount} (a plural rule may be specified with
 * {@code @PluralCount} if necessary, but you will almost never need to
 * do this). The actual plural forms for the default locale can be supplied in a
 * {@code @PluralText} annotation on the method, such as
 * <code>@PluralText({"one", "You have one widget"})</code>, or they can be
 * supplied in the properties file as {@code methodkey[one]=You have one widget}. Note
 * that non-default plural forms are not inherited between locales, because the
 * different locales may have different plural rules (especially {@code default} and
 * anything else and those which use different scripts such as {@code sr_Cyrl} and
 * {@code sr_Latn} [one of which would likely be the default], but also subtle cases
 * like {@code pt} and {@code pt_BR}).
 * </p>
 * 
 * <p>
 * Additionally, individual arguments can be marked as optional (ie, GWT will
 * not give an error if a particular translation does not reference the
 * argument) with the {@code @Optional} annotation, and an example may be supplied to
 * the translator with the {@code @Example(String)} annotation.
 * </p>
 * 
 * <h3>Complete Annotations Example</h3>
 * In addition to the default properties file, default text and additional
 * metadata may be stored in the source file itself using annotations. A
 * complete example of using annotations in this way is:
 * 
 * <code><pre>
 * &#64;Generate(format = "com.google.gwt.i18n.rebind.format.PropertiesFormat")
 * &#64;DefaultLocale("en_US")
 * public interface MyMessages extends Messages {
 *   &#64;Key("1234")
 *   &#64;DefaultText("This is a plain string.")
 *   String oneTwoThreeFour();
 *   
 *   &#64;DefaultText("You have {0} widgets")
 *   &#64;PluralText({"one", "You have one widget")
 *   String widgetCount(&#64;PluralCount int count);
 *   
 *   &#64;DefaultText("No reference to the argument")
 *   String optionalArg(&#64;Optional String ignored);
 *   
 *   &#64;DefaultText("Your cart total is {0,number,currency}")
 *   &#64;Description("The total value of the items in the shopping cart in local currency")
 *   String totalAmount(&#64;Example("$5.00") double amount);
 *   
 *   &#64;Meaning("the color")
 *   &#64;DefaultMessage("orange")
 *   String orangeColor();
 *   
 *   &#64;Meaning("the fruit")
 *   &#64;DefaultMessage("orange")
 *   String orangeFruit();
 * }
 * </pre></code>
 * 
 * <h3>Binding to Properties Files</h3>
 * Interfaces extending <code>Messages</code> are bound to resource files
 * using the same algorithm as interfaces extending <code>Constants</code>.
 * See the documentation for {@link Constants} for a description of the
 * algorithm.
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
public interface Messages extends LocalizableResource {

  /**
   * Provides alternate forms of a message, such as are needed when plural
   * forms are used or a placeholder has known gender. The selection of which
   * form to use is based on the value of the arguments marked
   * PluralCount and/or Select.
   * 
   * <p>Example:
   * <code><pre>
   *   &#64;DefaultMessage("You have {0} widgets.")
   *   &#64;AlternateMessage({"one", "You have one widget.")
   *   String example(&#64;PluralCount int count);
   * </pre></code>
   * </p>
   * 
   * <p>If multiple {@link PluralCount} or {@link Select} parameters are
   * supplied, the forms for each, in the order they appear in the parameter
   * list, are supplied separated by a vertical bar ("|").  Example:
   * <code><pre>
   *   &#64;DefaultMessage("You have {0} messages and {1} notifications.")
   *   &#64;AlternateMessage({
   *       "=0|=0", "You have no messages or notifications."
   *       "=0|one", "You have a notification."
   *       "one|=0", "You have a message."
   *       "one|one", "You have one message and one notification."
   *       "other|one", "You have {0} messages and one notification."
   *       "one|other", "You have one message and {1} notifications."
   *   })
   *   String messages(&#64;PluralCount int msgCount,
   *       &#64;PluralCount int notifyCount);
   * </pre></code>
   * 
   * Note that the number of permutations can grow quickly, and that the default
   * message is used when every {@link PluralCount} or {@link Select} would use
   * the "other" value.
   * </p>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface AlternateMessage {

    /**
     * An array of pairs of strings containing the strings for different forms.
     * 
     * Each pair is the name of a form followed by the string in the source
     * locale for that form.  Each form name is the name of a plural form if
     * {@link PluralCount} is used, or the matching value if {@link Select} is
     * used.  An example for a locale that has "none", "one", and "other" plural
     * forms:
     * 
     * <code><pre>
     * &#64;DefaultMessage("{0} widgets")
     * &#64;AlternateMessage({"none", "No widgets", "one", "One widget"})
     * </pre>
     * 
     * Note that the plural form "other" gets the translation specified in
     * {@code &#64;DefaultMessage}, as does any {@code &#64;Select} value not
     * listed. 
     * 
     * If more than one way of selecting a translation exists, they will be
     * combined, separated with {@code |}, in the order they are supplied as
     * arguments in the method.  For example:
     * <code><pre>
     *   &#64;DefaultMessage("{0} gave away their {2} widgets")
     *   &#64;AlternateMesssage({
     *     "MALE|other", "{0} gave away his {2} widgets",
     *     "FEMALE|other", "{0} gave away her {2} widgets",
     *     "MALE|one", "{0} gave away his widget",
     *     "FEMALE|one", "{0} gave away her widget",
     *     "other|one", "{0} gave away their widget",
     *   })
     *   String giveAway(String name, &#64;Select Gender gender,
     *       &#64;PluralCount int count);
     * </pre></code>
     */
    String[] value();
  }

  /**
   * Default text to be used if no translation is found (and also used as the
   * source for translation). Format should be that expected by
   * {@link java.text.MessageFormat}.
   * 
   * <p>Example:
   * <code><pre>
   *   &#64;DefaultMessage("Don''t panic - you have {0} widgets left")
   *   String example(int count)
   * </pre></code>
   * </p>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface DefaultMessage {
    String value();
  }

  /**
   * An example of the annotated parameter to assist translators.
   * 
   * <p>Example:
   * <code><pre>
   *   String example(&#64;Example("/etc/passwd") String fileName)
   * </pre></code>
   * </p>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface Example {
    String value();
  }

  /**
   * Ignored except on parameters also tagged with {@link PluralCount}, and
   * provides an offset to be subtracted from the value before a plural rule
   * is chosen or the value is formatted.  Note that "=n" forms are evaluated
   * before this offset is applied.
   * 
   * <p>Example:
   * <code><pre>
   *   &#64;PluralText({"=0", "No one has recommended this movie",
   *     "=1", "{0} has recommended this movie",
   *     "=2", "{0} and {1} have recommended this movie",
   *     "one", "{0}, {1} and one other have recommended this movie"})
   *   &#64;DefaultMessage("{0}, {1} and {2,number} others have recommended this movie")
   *   String recommenders(&#64;Optional String rec1, &#64;Optional String rec2,
   *     &#64;PluralCount &#64;Offset(2) int count);
   * </pre></code>
   * would result in
   * <code><pre>
   * recommenders("John", null, 1) => "John has..."
   * recommenders("John", "Jane", 3) => "John, Jane, and one other..."
   * recommenders("John", "Jane", 1402) => "John, Jane, and 1,400 others..."
   * </pre></code>
   * </p>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface Offset {
    int value();
  }

  /**
   * Indicates the specified parameter is optional and need not appear in a
   * particular translation of this message.
   * 
   * <p>Example:
   * <code><pre>
   *   String example(&#64;Optional int count)
   * </pre></code>
   * </p>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface Optional {
  }

  /**
   * Provides multiple plural forms based on a count. The selection of which
   * plural form is performed by a PluralRule implementation.
   * 
   * This annotation is applied to a single parameter of a Messages subinterface
   * and indicates that parameter is to be used to choose the proper plural form
   * of the message. The parameter chosen must be of type short or int.
   * 
   * Optionally, a class literal referring to a PluralRule implementation can be
   * supplied as the argument if the standard implementation is insufficient.
   * 
   * <p>Example:
   * <code><pre>
   *   &#64;DefaultMessage("You have {0} widgets.")
   *   &#64;AlternateMessage({"one", "You have one widget."})
   *   String example(&#64;PluralCount int count)
   * </pre></code>
   * </p>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface PluralCount {

    /**
     * The PluralRule implementation to use for this message. If not specified,
     * the GWT-supplied one is used instead, which should cover most use cases.
     * 
     * <p>{@code PluralRule.class} is used as a default value here, which will
     * be replaced during code generation with the default implementation.
     * </p>
     */
    // http://bugs.sun.com/view_bug.do?bug_id=6512707
    Class<? extends PluralRule> value() default com.google.gwt.i18n.client.PluralRule.class;
  }

  /**
   * Provides multiple plural forms based on a count. The selection of which
   * plural form to use is based on the value of the argument marked
   * PluralCount, which may also specify an alternate plural rule to use.
   * 
   * <p>Example:
   * <code><pre>
   *   &#64;DefaultMessage("You have {0} widgets.")
   *   &#64;PluralText({"one", "You have one widget.")
   *   String example(&#64;PluralCount int count)
   * </pre></code>
   * </p>
   * 
   * @deprecated use {@link AlternateMessage} instead
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  @Deprecated
  public @interface PluralText {

    /**
     * An array of pairs of strings containing the strings for different plural
     * forms.
     * 
     * Each pair is the name of the plural form (as returned by
     * PluralForm.toString) followed by the string for that plural form. An
     * example for a locale that has "none", "one", and "other" plural forms:
     * 
     * <code><pre>
     * &#64;DefaultMessage("{0} widgets")
     * &#64;PluralText({"none", "No widgets", "one", "One widget"})
     * </pre>
     * 
     * "other" must not be included in this array as it will map to the
     * DefaultMessage value.
     */
    String[] value();
  }

  /**
   * Provides multiple forms based on a dynamic parameter.
   * 
   * This annotation is applied to a single parameter of a Messages subinterface
   * and indicates that parameter is to be used to choose the proper form of the
   * message. The parameter chosen must be of type Enum, String, boolean, or a
   * primitive integral type.  This is frequently used to get proper gender for
   * translations to languages where surrounding words depend on the gender of
   * a person or noun.  This also marks the parameter as {@link Optional}.
   * 
   * <p>Example:
   * <code><pre>
   *   &#64;DefaultMessage("{0} likes their widgets.")
   *   &#64;AlternateMessage({
   *       "FEMALE", "{0} likes her widgets.",
   *       "MALE", "{0} likes his widgets.",
   *   })
   *   String example(String name, &#64;Select Gender gender)
   * </pre></code>
   * </p>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface Select {
  }
}
