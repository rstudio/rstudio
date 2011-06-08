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
 * This is the common superinterface to Messages and Constants.
 * 
 * Each (and the Constants subinterface ConstantsWithLookup) provide
 * compile-time localization of various forms of data.  Messages is
 * used for <code>MessageFormat</code>-style strings which can have
 * parameters (including support for plural forms), while Constants
 * can be other types, have simplified quoting requirements, and do
 * not take any parameters.
 * 
 * The annotations defined here are common to both -- see the individual
 * subinterfaces for additional annotations which apply only to each
 * one.
 */
public interface LocalizableResource extends Localizable {
  
  /**
   * Specifies the default locale for messages in this file.  If not
   * specified, the default is <code>DEFAULT_LOCALE</code>.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface DefaultLocale {

    String DEFAULT_LOCALE = "en";

    String value() default DEFAULT_LOCALE;
  }

  /**
   * Specifies a description of the string to be translated, such as a note
   * about the context.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Description {
    String value();
  }

  /**
   * Requests that a translation source file be generated from the annotated
   * interface.  The file type is determined by the format argument, and the
   * file name by the optional fileName argument.  Some file formats support
   * aggregating messages from multiple interfaces into one file, while others
   * do not; also, additional parameters may be specified via format-specific
   * annotations -- see the documentation of the MessageCatalogFormat implementation
   * for details.
   * 
   * Examples:
   * <ul>
   * <li>&#64;Generate(format = "com.google.gwt.i18n.server.PropertyCatalogFactory")
   * <br>generates properties files for all locales, and the names will be
   *      of the form MyMessages_locale.properties
   * <li>&#64;Generate(format = {"com.example.ProprietaryFormat1",
   *    "com.example.ProprietaryFormat2"},
   *    fileName = "myapp_translate_source", locales = {"default"})
   * <br>generates default files in two proprietary formats, with filenames like
   *      myapp_translate_source.p1 and myapp_translate_source.p2
   * </pre></code>  
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface Generate {

    /**
     * Placeholder used to detect that no value was supplied for the fileName
     * parameter.
     */
    String DEFAULT = "[default]";
    
    /**
     * Fully-qualified class names of the generator classes. Each class must
     * implement com.google.gwt.i18n.server.MessageCatalogFactory
     * (com.google.gwt.i18n.rebind.format.MessageCatalogFormat still works, but
     * is deprecated).
     * 
     * Strings are used here instead of class literals because the generators
     * will likely contain non-translatable code and thus can't be referenced
     * from translatable code directly.
     * 
     * Each generator may define additional annotations to supply other
     * necessary parameters.
     */
    String[] format();

    /**
     * A platform-specific filename for output. If not present, the file will be
     * named based on the fully-qualified name of the annotated interface. File
     * names without a slash are given a relative name based on the
     * fully-qualified package name of the annotated interface. Relative
     * pathnames are generated in the auxiliary module directory (moduleName-aux
     * in the output directory, which is specified by the "-out" flag to the
     * compiler, or the current directory if not present) -- absolute path names
     * are not allowed. Unless exactly one locale is specified for locales (not
     * just only one locale happened to be compiled for), the locale will be
     * appended to the name (such as _default [for the default locale], _en_US,
     * etc) as well as the proper extension for the specified format.
     * 
     * Note that if multiple generators are used, they will have the same base
     * filename so the extensions must be different.
     */
    String fileName() default DEFAULT;
    
    /**
     * A list of locales for which to generate this output file.  If no locales
     * are specified, all locales for which the application is compiled for will
     * be generated. Note that the default locale is "default".
     */
    String[] locales() default {};
  }

  /**
   * Annotation indicating this is a generated file and the source file it was
   * generated from. 
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface GeneratedFrom {
    String value();
  }

  /**
   * Requests that the keys for messages be generated automatically.  If the
   * annotation is supplied with no value, the default is to use an MD5 hash of
   * the text and meaning.  If this annotation is not supplied, the keys will be
   * the unqualified method names.
   * 
   * <p>The value is either the name of an inner class of {@code KeyGenerator} or the
   * fully-qualified class name of some implementation of {@code KeyGenerator}.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface GenerateKeys {
    String value() default "com.google.gwt.i18n.server.keygen.MD5KeyGenerator";
  }

  /**
   * The key used for lookup of translated strings.  If not present, the
   * key will be generated based on the {@code @GenerateKeys} annotation,
   * or the unqualified method name if it is not present.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Key {
    String value();
  }

  /**
   * Specifies the meaning of the translated string.  For example, to
   * distinguish between multiple meanings of a word or phrase.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface Meaning {
    String value();
  }
}