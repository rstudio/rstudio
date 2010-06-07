/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.http.client;

/**
 * Utility class for the encoding and decoding URLs in their entirety or by
 * their individual components.
 * 
 * <h3>Required Module</h3>
 * Modules that use this class should inherit
 * <code>com.google.gwt.http.HTTP</code>.
 * 
 * {@gwt.include com/google/gwt/examples/http/InheritsExample.gwt.xml}
 */
public final class URL {

  /**
   * Returns a string where all URL escape sequences have been converted back to
   * their original character representations.
   * 
   * @param encodedURL string containing encoded URL encoded sequences
   * @return string with no encoded URL encoded sequences
   * 
   * @throws NullPointerException if encodedURL is <code>null</code>
   */
  public static String decode(String encodedURL) {
    StringValidator.throwIfNull("encodedURL", encodedURL);
    return decodeImpl(encodedURL);
  }

  /**
   * Returns a string where all URL component escape sequences have been
   * converted back to their original character representations.
   * <p>
   * Note: this method will convert the space character escape short form, '+',
   * into a space. It should therefore only be used for query-string parts.
   * 
   * @param encodedURLComponent string containing encoded URL component
   *        sequences
   * @return string with no encoded URL component encoded sequences
   * 
   * @throws NullPointerException if encodedURLComponent is <code>null</code>
   * 
   * @deprecated Use {@link #decodeQueryString(String)}
   */
  @Deprecated
  public static String decodeComponent(String encodedURLComponent) {
    return decodeQueryString(encodedURLComponent);
  }

  /**
   * Returns a string where all URL component escape sequences have been
   * converted back to their original character representations.
   * 
   * @param encodedURLComponent string containing encoded URL component
   *        sequences
   * @param fromQueryString if <code>true</code>, +'s will be turned into
   *        spaces, otherwise they'll be kept as-is.
   * @return string with no encoded URL component encoded sequences
   * 
   * @throws NullPointerException if encodedURLComponent is <code>null</code>
   * 
   * @deprecated Use {@link #decodeQueryString(String)},
   *             {@link #decodePathSegment(String)}
   */
  @Deprecated
  public static String decodeComponent(String encodedURLComponent,
      boolean fromQueryString) {
    StringValidator.throwIfNull("encodedURLComponent", encodedURLComponent);
    return fromQueryString ? decodeQueryStringImpl(encodedURLComponent)
        : decodePathSegmentImpl(encodedURLComponent);
  }

  /**
   * Returns a string where all URL component escape sequences have been
   * converted back to their original character representations.
   * 
   * @param encodedURLComponent string containing encoded URL component
   *          sequences
   * @return string with no encoded URL component encoded sequences
   * 
   * @throws NullPointerException if encodedURLComponent is <code>null</code>
   */
  public static String decodePathSegment(String encodedURLComponent) {
    StringValidator.throwIfNull("encodedURLComponent", encodedURLComponent);
    return decodePathSegmentImpl(encodedURLComponent);
  }

  /**
   * Returns a string where all URL component escape sequences have been
   * converted back to their original character representations.
   * <p>
   * Note: this method will convert the space character escape short form, '+',
   * into a space. It should therefore only be used for query-string parts.
   * 
   * @param encodedURLComponent string containing encoded URL component
   *          sequences
   * @return string with no encoded URL component encoded sequences
   * 
   * @throws NullPointerException if encodedURLComponent is <code>null</code>
   */
  public static String decodeQueryString(String encodedURLComponent) {
    StringValidator.throwIfNull("encodedURLComponent", encodedURLComponent);
    return decodeQueryStringImpl(encodedURLComponent);
  }

  /**
   * Returns a string where all characters that are not valid for a complete URL
   * have been escaped. The escaping of a character is done by converting it
   * into its UTF-8 encoding and then encoding each of the resulting bytes as a
   * %xx hexadecimal escape sequence.
   * 
   * <p>
   * The following character sets are <em>not</em> escaped by this method:
   * <ul>
   * <li>ASCII digits or letters</li>
   * <li>ASCII punctuation characters:
   * 
   * <pre>
   * - _ . ! ~ * ' ( )
   * </pre>
   * 
   * </li>
   * <li>URL component delimiter characters:
   * 
   * <pre>
   * ; / ? : &amp; = + $ , #
   * </pre>
   * 
   * </li>
   * </ul>
   * </p>
   * 
   * @param decodedURL a string containing URL characters that may require
   *        encoding
   * @return a string with all invalid URL characters escaped
   * 
   * @throws NullPointerException if decodedURL is <code>null</code>
   */
  public static String encode(String decodedURL) {
    StringValidator.throwIfNull("decodedURL", decodedURL);
    return encodeImpl(decodedURL);
  }

  /**
   * Returns a string where all characters that are not valid for a URL
   * component have been escaped. The escaping of a character is done by
   * converting it into its UTF-8 encoding and then encoding each of the
   * resulting bytes as a %xx hexadecimal escape sequence.
   * <p>
   * Note: this method will convert any the space character into its escape
   * short form, '+' rather than %20. It should therefore only be used for
   * query-string parts.
   * 
   * <p>
   * The following character sets are <em>not</em> escaped by this method:
   * <ul>
   * <li>ASCII digits or letters</li>
   * <li>ASCII punctuation characters:
   * 
   * <pre>- _ . ! ~ * ' ( )</pre>
   * </li>
   * </ul>
   * </p>
   * 
   * <p>
   * Notice that this method <em>does</em> encode the URL component delimiter
   * characters:<blockquote>
   * 
   * <pre>
   * ; / ? : &amp; = + $ , #
   * </pre>
   * 
   * </blockquote>
   * </p>
   * 
   * @param decodedURLComponent a string containing invalid URL characters
   * @return a string with all invalid URL characters escaped
   * 
   * @throws NullPointerException if decodedURLComponent is <code>null</code>
   * 
   * @deprecated Use {@link #encodeQueryString(String)}
   */
  @Deprecated
  public static String encodeComponent(String decodedURLComponent) {
    return encodeQueryString(decodedURLComponent);
  }

  /**
   * Returns a string where all characters that are not valid for a URL
   * component have been escaped. The escaping of a character is done by
   * converting it into its UTF-8 encoding and then encoding each of the
   * resulting bytes as a %xx hexadecimal escape sequence.
   * 
   * <p>
   * The following character sets are <em>not</em> escaped by this method:
   * <ul>
   * <li>ASCII digits or letters</li>
+   * <li>ASCII punctuation characters:
   * 
   * <pre>- _ . ! ~ * ' ( )</pre>
   * </li>
   * </ul>
   * </p>
   * 
   * <p>
   * Notice that this method <em>does</em> encode the URL component delimiter
   * characters:<blockquote>
   * 
   * <pre>
   * ; / ? : &amp; = + $ , #
   * </pre>
   * 
   * </blockquote>
   * </p>
   * 
   * @param decodedURLComponent a string containing invalid URL characters
   * @param queryStringSpaces if <code>true</code>, spaces will be encoded as
   *          +'s.
   * @return a string with all invalid URL characters escaped
   * 
   * @throws NullPointerException if decodedURLComponent is <code>null</code>
   * 
   * @deprecated Use {@link #encodeQueryString(String)},
   *             {@link #encodePathSegment(String)}
   */
  @Deprecated
  public static String encodeComponent(String decodedURLComponent,
      boolean queryStringSpaces) {
    StringValidator.throwIfNull("decodedURLComponent", decodedURLComponent);
    return queryStringSpaces ? encodeQueryStringImpl(decodedURLComponent)
        : encodePathSegmentImpl(decodedURLComponent);
  }

  /**
   * Returns a string where all characters that are not valid for a URL
   * component have been escaped. The escaping of a character is done by
   * converting it into its UTF-8 encoding and then encoding each of the
   * resulting bytes as a %xx hexadecimal escape sequence.
   * 
   * <p>
   * The following character sets are <em>not</em> escaped by this method:
   * <ul>
   * <li>ASCII digits or letters</li>
   * <li>ASCII punctuation characters:
   * 
   * <pre>- _ . ! ~ * ' ( )</pre>
   * </li>
   * </ul>
   * </p>
   * 
   * <p>
   * Notice that this method <em>does</em> encode the URL component delimiter
   * characters:<blockquote>
   * 
   * <pre>
   * ; / ? : &amp; = + $ , #
   * </pre>
   * 
   * </blockquote>
   * </p>
   * 
   * @param decodedURLComponent a string containing invalid URL characters
   * @return a string with all invalid URL characters escaped
   * 
   * @throws NullPointerException if decodedURLComponent is <code>null</code>
   */
  public static String encodePathSegment(String decodedURLComponent) {
    StringValidator.throwIfNull("decodedURLComponent", decodedURLComponent);
    return encodePathSegmentImpl(decodedURLComponent);
  }

  /**
   * Returns a string where all characters that are not valid for a URL
   * component have been escaped. The escaping of a character is done by
   * converting it into its UTF-8 encoding and then encoding each of the
   * resulting bytes as a %xx hexadecimal escape sequence.
   * <p>
   * Note: this method will convert any the space character into its escape
   * short form, '+' rather than %20. It should therefore only be used for
   * query-string parts.
   * 
   * <p>
   * The following character sets are <em>not</em> escaped by this method:
   * <ul>
   * <li>ASCII digits or letters</li>
   * <li>ASCII punctuation characters:
   * 
   * <pre>- _ . ! ~ * ' ( )</pre>
   * </li>
   * </ul>
   * </p>
   * 
   * <p>
   * Notice that this method <em>does</em> encode the URL component delimiter
   * characters:<blockquote>
   * 
   * <pre>
   * ; / ? : &amp; = + $ , #
   * </pre>
   * 
   * </blockquote>
   * </p>
   * 
   * @param decodedURLComponent a string containing invalid URL characters
   * @return a string with all invalid URL characters escaped
   * 
   * @throws NullPointerException if decodedURLComponent is <code>null</code>
   */
  public static String encodeQueryString(String decodedURLComponent) {
    StringValidator.throwIfNull("decodedURLComponent", decodedURLComponent);
    return encodeQueryStringImpl(decodedURLComponent);
  }

  private static native String decodeImpl(String encodedURL) /*-{
    return decodeURI(encodedURL);
  }-*/;

  private static native String decodePathSegmentImpl(String encodedURLComponent) /*-{
    return decodeURIComponent(encodedURLComponent);
  }-*/;

  private static native String decodeQueryStringImpl(String encodedURLComponent) /*-{
    var regexp = /\+/g;
    return decodeURIComponent(encodedURLComponent.replace(regexp, "%20"));
  }-*/;

  private static native String encodeImpl(String decodedURL) /*-{
    return encodeURI(decodedURL);
  }-*/;

  private static native String encodePathSegmentImpl(String decodedURLComponent) /*-{
    return encodeURIComponent(decodedURLComponent);
  }-*/;

  private static native String encodeQueryStringImpl(String decodedURLComponent) /*-{
    var regexp = /%20/g;
    return encodeURIComponent(decodedURLComponent).replace(regexp, "+");
  }-*/;

  private URL() {
  }
}
