/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.safehtml.rebind;

import com.google.gwt.dev.util.Preconditions;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A representation of a parsed HTML template.
 *
 * <p>A parsed template is represented as a sequence of template chunks.
 *
 * <p>A chunk may correspond to a literal string or to a formal template
 * parameter.
 *
 * <p>Parameter chunks have an attribute that refers to the index of the
 * corresponding template method parameter, as well as an attribute that
 * represents the HTML context the template parameter appeared in.
 */
final class ParsedHtmlTemplate {

  /**
   * A representation of the context of a point within a HTML document. A
   * context consists of a type (such as "plain text", "attribute"), as well as
   * a HTML tag and attribute name where applicable.
   */
  static final class HtmlContext {
    /**
     * The possible types of HTML context.
     */
    static enum Type {
      /**
       * Regular inner text HTML context.
       */
      TEXT,
      /**
       * Value of a HTML attribute.
       */
      ATTRIBUTE_VALUE,
      /**
       * At the very start of a URL-valued attribute.
       */
      URL_ATTRIBUTE_START,
      /**
       * A template parameter that comprises an entire URL-valued attribute.
       */
      URL_ATTRIBUTE_ENTIRE,
      /**
       * CSS (style) context.
       */
      CSS,
      /**
       * CSS (style) attribute context.
       */
      CSS_ATTRIBUTE,
      /**
       * At the very start of a CSS (style) attribute context.
       */
      CSS_ATTRIBUTE_START
    }

    private final Type type;
    private final String tag;
    private final String attribute;

    /**
     * Creates a HTML context.
     *
     * @param type the {@link Type} of this context
     */
    public HtmlContext(Type type) {
      this(type, null, null);
    }

    /**
     * Creates a HTML context.
     *
     * @param type the {@link Type} of this context
     * @param tag the HTML tag this context corresponds to, if applicable; null
     *        otherwise
     * @param attribute the HTML attribute this context corresponds to, if
     *        applicable; null otherwise
     */
    public HtmlContext(Type type, String tag, String attribute) {
      Preconditions.checkArgument((type != Type.TEXT)
          || ((tag == null) && (attribute == null)),
          "tag and attribute must be null for context \"TEXT\"");
      this.type = type;
      this.tag = tag;
      this.attribute = attribute;
    }

    /**
     * Returns the attribute of this HTML context.
     */
    public String getAttribute() {
      return attribute;
    }

    /**
     * Returns the tag of this HTML context.
     */
    public String getTag() {
      return tag;
    }

    /**
     * Returns the type of this HTML context.
     */
    public Type getType() {
      return type;
    }

    @Override
    public String toString() {
      return "(" + getType() + "," + getTag() + "," + getAttribute() + ")";
    }
  }

  /**
   * Represents a template chunk corresponding to a literal string.
   */
  static final class LiteralChunk implements TemplateChunk {
    private final StringBuilder chunk;

    /**
     * Creates a literal chunk corresponding to an empty string.
     */
    public LiteralChunk() {
      chunk = new StringBuilder();
    }

    /**
     * Creates a literal chunk corresponding to the provided string.
     */
    public LiteralChunk(String literal) {
      chunk = new StringBuilder(literal);
    }

    public Kind getKind() {
      return Kind.LITERAL;
    }

    public String getLiteral() {
      return chunk.toString();
    }

    @Override
    public String toString() {
      return String.format("L(%s)", getLiteral());
    }

    void append(String s) {
      chunk.append(s);
    }
  }

  /**
   * Represents a template chunk corresponding to a template parameter.
   */
  static final class ParameterChunk implements TemplateChunk {

    private final HtmlContext context;
    private final int parameterIndex;

    /**
     * Creates a parameter chunk.
     *
     * @param context the HTML context this parameter appears in
     * @param parameterIndex the index of the template parameter that this chunk
     *        refers to
     */
    public ParameterChunk(HtmlContext context, int parameterIndex) {
      this.context = context;
      this.parameterIndex = parameterIndex;
    }

    /**
     * Returns the HTML context this parameter appears in.
     */
    public HtmlContext getContext() {
      return context;
    }

    public Kind getKind() {
      return Kind.PARAMETER;
    }

    /**
     * Returns the index of the template parameter that this chunk refers to.
     */
    public int getParameterIndex() {
      return parameterIndex;
    }

    @Override
    public String toString() {
      return String.format("P(%s,%d)", getContext(), getParameterIndex());
    }
  }

  /**
   * Represents a parsed chunk of a template.
   *
   * <p>There are two kinds of chunks: Those representing literal strings and
   * those representing template parameters.
   */
  interface TemplateChunk {
    /**
     * Distinguishes different kinds of {@link TemplateChunk}.
     */
    public static enum Kind {
      LITERAL, PARAMETER,
    }

    /**
     * Returns the {@link Kind} of this template chunk.
     */
    Kind getKind();
  }

  /*
   * The chunks of this parsed template.
   */
  private final LinkedList<TemplateChunk> chunks;

  /**
   * Initializes an empty parsed HTML template.
   */
  public ParsedHtmlTemplate() {
    chunks = new LinkedList<TemplateChunk>();
  }

  /**
   * Adds a literal string to the template.
   *
   * If the currently last chunk of the parsed template is a literal chunk, the
   * provided string literal will be appended to that chunk. I.e., consecutive
   * literal chunks are automatically coalesced.

   * @param literal the string to be added as a literal chunk
   */
  public void addLiteral(String literal) {
    if (chunks.isEmpty()
        || (chunks.getLast().getKind() != TemplateChunk.Kind.LITERAL)) {
      chunks.add(new LiteralChunk(literal));
    } else {
      ((LiteralChunk) chunks.getLast()).append(literal);
    }
  }

  /**
   * Adds a parameter chunk to the template.
   *
   * @param chunk the chunk to be added
   */
  public void addParameter(ParameterChunk chunk) {
    chunks.add(chunk);
  }

  /**
   * Returns the chunks of this parsed template.
   *
   * <p>The returned list is unmodifiable.
   */
  public List<TemplateChunk> getChunks() {
    return Collections.unmodifiableList(chunks);
  }

  @Override
  public String toString() {
    return chunks.toString();
  }
}
