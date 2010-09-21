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
package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.HasInitializeHandlers;
import com.google.gwt.event.logical.shared.InitializeEvent;
import com.google.gwt.event.logical.shared.InitializeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.client.HasSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.impl.RichTextAreaImpl;

/**
 * A rich text editor that allows complex styling and formatting.
 * 
 * Because some browsers do not support rich text editing, and others support
 * only a limited subset of functionality, there are two formatter interfaces,
 * accessed via {@link #getBasicFormatter()} and {@link #getExtendedFormatter()}
 * . A browser that does not support rich text editing at all will return
 * <code>null</code> for both of these, while one that supports only the basic
 * functionality will return <code>null</code> for the latter.
 * 
 * <p>
 * <img class='gallery' src='doc-files/RichTextArea.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3> 
 * <dl>
 *   <dt>.gwt-RichTextArea</dt>
 *   <dd>Applied to the rich text element.</dd>
 * </dl>
 */
public class RichTextArea extends FocusWidget implements HasHTML,
    HasInitializeHandlers, HasSafeHtml {

  /**
   * <p>
   * This interface is used to access basic formatting options, when available.
   * If the implementation supports basic formatting, then
   * {@link RichTextArea#getBasicFormatter()} will return an instance of this
   * class.
   * </p>
   * <p>
   * The formatter will format the user selected text in the
   * {@link RichTextArea}. As a result, it will only work reliably if the
   * {@link RichTextArea} is attached, visible to on the page, and has been
   * focused at least once. If you just want to initialize the content of the
   * {@link RichTextArea}, use {@link RichTextArea#setHTML(String)} instead.
   * </p>
   * 
   * @deprecated use {@link RichTextArea.Formatter} instead
   */
  @Deprecated
  public interface BasicFormatter {

    /**
     * Gets the background color.
     * 
     * @return the background color
     */
    String getBackColor();

    /**
     * Gets the foreground color.
     * 
     * @return the foreground color
     */
    String getForeColor();

    /**
     * Is the current region bold?
     * 
     * @return true if the current region is bold
     */
    boolean isBold();

    /**
     * Is the current region italic?
     * 
     * @return true if the current region is italic
     */
    boolean isItalic();

    /**
     * Is the current region subscript?
     * 
     * @return true if the current region is subscript
     */
    boolean isSubscript();

    /**
     * Is the current region superscript?
     * 
     * @return true if the current region is superscript
     */
    boolean isSuperscript();

    /**
     * Is the current region underlined?
     * 
     * @return true if the current region is underlined
     */
    boolean isUnderlined();

    /**
     * Selects all the text.
     */
    void selectAll();

    /**
     * Sets the background color.
     * 
     * @param color the new background color
     */
    void setBackColor(String color);

    /**
     * Sets the font name.
     * 
     * @param name the new font name
     */
    void setFontName(String name);

    /**
     * Sets the font size.
     * 
     * @param fontSize the new font size
     */
    void setFontSize(FontSize fontSize);

    /**
     * Sets the foreground color.
     * 
     * @param color the new foreground color
     */
    void setForeColor(String color);

    /**
     * Sets the justification.
     * 
     * @param justification the new justification
     */
    void setJustification(Justification justification);

    /**
     * Toggles bold.
     */
    void toggleBold();

    /**
     * Toggles italic.
     */
    void toggleItalic();

    /**
     * Toggles subscript.
     */
    void toggleSubscript();

    /**
     * Toggles superscript.
     */
    void toggleSuperscript();

    /**
     * Toggles underline.
     */
    void toggleUnderline();
  }

  /**
   * <p>
   * This interface is used to access full formatting options, when available.
   * If the implementation supports full formatting, then
   * {@link RichTextArea#getExtendedFormatter()} will return an instance of this
   * class.
   * </p>
   * <p>
   * The formatter will format the user selected text in the
   * {@link RichTextArea}. As a result, it will only work reliably if the
   * {@link RichTextArea} is attached, visible to on the page, and has been
   * focused at least once. If you just want to initialize the content of the
   * {@link RichTextArea}, use {@link RichTextArea#setHTML(String)} instead.
   * </p>
   * 
   * @deprecated use {@link RichTextArea.Formatter} instead
   */
  @Deprecated
  public interface ExtendedFormatter extends BasicFormatter {

    /**
     * Creates a link to the supplied URL.
     * 
     * @param url the URL to be linked to
     */
    void createLink(String url);

    /**
     * Inserts a horizontal rule.
     */
    void insertHorizontalRule();

    /**
     * Inserts an image element.
     * 
     * @param url the url of the image to be inserted
     */
    void insertImage(String url);

    /**
     * Starts an numbered list. Indentation will create nested items.
     */
    void insertOrderedList();

    /**
     * Starts an bulleted list. Indentation will create nested items.
     */
    void insertUnorderedList();

    /**
     * Is the current region strikethrough?
     * 
     * @return true if the current region is strikethrough
     */
    boolean isStrikethrough();

    /**
     * Left indent.
     */
    void leftIndent();

    /**
     * Removes all formatting on the selected text.
     */
    void removeFormat();

    /**
     * Removes any link from the selected text.
     */
    void removeLink();

    /**
     * Right indent.
     */
    void rightIndent();

    /**
     * Toggles strikethrough.
     */
    void toggleStrikethrough();
  }

  /**
   * Font size enumeration. Represents the seven basic HTML font sizes, as
   * defined in CSS.
   */
  public static class FontSize {

    /**
     * Represents a Large font.
     */
    public static final FontSize LARGE = new FontSize(5);

    /**
     * Represents a Medium font.
     */
    public static final FontSize MEDIUM = new FontSize(4);

    /**
     * Represents a Small font.
     */
    public static final FontSize SMALL = new FontSize(3);

    /**
     * Represents an X-Large font.
     */
    public static final FontSize X_LARGE = new FontSize(6);

    /**
     * Represents an X-Small font.
     */
    public static final FontSize X_SMALL = new FontSize(2);

    /**
     * Represents an XX-Large font.
     */
    public static final FontSize XX_LARGE = new FontSize(7);

    /**
     * Represents an XX-Small font.
     */
    public static final FontSize XX_SMALL = new FontSize(1);

    private int number;

    private FontSize(int number) {
      this.number = number;
    }

    /**
     * Gets the HTML font number associated with this font size.
     * 
     * @return an integer from 1 to 7 inclusive
     */
    public int getNumber() {
      return number;
    }

    @Override
    public String toString() {
      return Integer.toString(number);
    }
  }

  /**
   * <p>
   * This interface is used to access full formatting options, when available.
   * If the implementation supports full formatting, then
   * {@link RichTextArea#getFormatter()} will return an instance of this class.
   * </p>
   * <p>
   * The formatter will format the user selected text in the
   * {@link RichTextArea}. As a result, it will only work reliably if the
   * {@link RichTextArea} is attached, visible to on the page, and has been
   * focused at least once. If you just want to initialize the content of the
   * {@link RichTextArea}, use {@link RichTextArea#setHTML(String)} instead.
   * </p>
   */
  public interface Formatter extends ExtendedFormatter {
    /**
     * Creates a link to the supplied URL.
     * 
     * @param url the URL to be linked to
     */
    void createLink(String url);

    /**
     * Gets the background color.
     * 
     * @return the background color
     */
    String getBackColor();

    /**
     * Gets the foreground color.
     * 
     * @return the foreground color
     */
    String getForeColor();

    /**
     * Inserts a horizontal rule.
     */
    void insertHorizontalRule();

    /**
     * Inserts generic html.
     * 
     * @param html the HTML to insert
     */
    void insertHTML(String html);

    /**
     * Inserts an image element.
     * 
     * @param url the url of the image to be inserted
     */
    void insertImage(String url);

    /**
     * Starts an numbered list. Indentation will create nested items.
     */
    void insertOrderedList();

    /**
     * Starts an bulleted list. Indentation will create nested items.
     */
    void insertUnorderedList();

    /**
     * Is the current region bold?
     * 
     * @return true if the current region is bold
     */
    boolean isBold();

    /**
     * Is the current region italic?
     * 
     * @return true if the current region is italic
     */
    boolean isItalic();

    /**
     * Is the current region strikethrough?
     * 
     * @return true if the current region is strikethrough
     */
    boolean isStrikethrough();

    /**
     * Is the current region subscript?
     * 
     * @return true if the current region is subscript
     */
    boolean isSubscript();

    /**
     * Is the current region superscript?
     * 
     * @return true if the current region is superscript
     */
    boolean isSuperscript();

    /**
     * Is the current region underlined?
     * 
     * @return true if the current region is underlined
     */
    boolean isUnderlined();

    /**
     * Left indent.
     */
    void leftIndent();

    /**
     * Redo an action that was just undone.
     */
    void redo();

    /**
     * Removes all formatting on the selected text.
     */
    void removeFormat();

    /**
     * Removes any link from the selected text.
     */
    void removeLink();

    /**
     * Right indent.
     */
    void rightIndent();

    /**
     * Selects all the text.
     */
    void selectAll();

    /**
     * Sets the background color.
     * 
     * @param color the new background color
     */
    void setBackColor(String color);

    /**
     * Sets the font name.
     * 
     * @param name the new font name
     */
    void setFontName(String name);

    /**
     * Sets the font size.
     * 
     * @param fontSize the new font size
     */
    void setFontSize(FontSize fontSize);

    /**
     * Sets the foreground color.
     * 
     * @param color the new foreground color
     */
    void setForeColor(String color);

    /**
     * Sets the justification.
     * 
     * @param justification the new justification
     */
    void setJustification(Justification justification);

    /**
     * Toggles bold.
     */
    void toggleBold();

    /**
     * Toggles italic.
     */
    void toggleItalic();

    /**
     * Toggles strikethrough.
     */
    void toggleStrikethrough();

    /**
     * Toggles subscript.
     */
    void toggleSubscript();

    /**
     * Toggles superscript.
     */
    void toggleSuperscript();

    /**
     * Toggles underline.
     */
    void toggleUnderline();

    /**
     * Undo the last action.
     */
    void undo();
  }

  /**
   * Justification enumeration. The three values are <code>left</code>,
   * <code>right</code>, <code>center</code>.
   */
  public static class Justification {

    /**
     * Center justification.
     */
    public static final Justification CENTER = new Justification("Center");

    /**
     * Full justification.
     */
    public static final Justification FULL = new Justification("Full");

    /**
     * Left justification.
     */
    public static final Justification LEFT = new Justification("Left");

    /**
     * Right justification.
     */
    public static final Justification RIGHT = new Justification("Right");

    private String tag;

    private Justification(String tag) {
      this.tag = tag;
    }

    @Override
    public String toString() {
      return "Justify " + tag;
    }
  }

  private RichTextAreaImpl impl = GWT.create(RichTextAreaImpl.class);

  /**
   * Creates a new, blank {@link RichTextArea} object with no stylesheet.
   */
  public RichTextArea() {
    setElement(impl.getElement());
    setStyleName("gwt-RichTextArea");
    impl.setOwner(this);
  }

  public HandlerRegistration addInitializeHandler(InitializeHandler handler) {
    return addHandler(handler, InitializeEvent.getType());
  }

  /**
   * Gets the basic rich text formatting interface. Note that formatting can
   * only be done when the {@link RichTextArea} is attached, visible on the
   * page, and has been focused by the user.
   * 
   * @return <code>null</code> if basic formatting is not supported
   * @deprecated use {@link #getFormatter()} instead
   */
  @Deprecated
  public BasicFormatter getBasicFormatter() {
    return getFormatter();
  }

  /**
   * Gets the full rich text formatting interface. Note that formatting can only
   * be done when the {@link RichTextArea} is attached, visible on the page, and
   * has been focused by the user.
   * 
   * @return <code>null</code> if full formatting is not supported
   * @deprecated use {@link #getFormatter()} instead
   */
  @Deprecated
  public ExtendedFormatter getExtendedFormatter() {
    return getFormatter();
  }

  /**
   * Gets the rich text formatting interface. Note that formatting can only be
   * done when the {@link RichTextArea} is attached, visible on the page, and
   * has been focused by the user.
   * 
   * @return <code>null</code> if full formatting is not supported
   */
  public Formatter getFormatter() {
    if (impl instanceof Formatter) {
      return (Formatter) impl;
    }
    return null;
  }

  public String getHTML() {
    return impl.getHTML();
  }

  public String getText() {
    return impl.getText();
  }

  @Override
  public boolean isEnabled() {
    return impl.isEnabled();
  }

  @Override
  public void setEnabled(boolean enabled) {
    impl.setEnabled(enabled);
  }

  @Override
  public void setFocus(boolean focused) {
    // There are different problems on each browser when you try to focus an
    // unattached rich text iframe, so just cut it off early.
    if (isAttached()) {
      impl.setFocus(focused);
    }
  }

  public void setHTML(String html) {
    impl.setHTML(html);
  }

  public void setHTML(SafeHtml html) {
    setHTML(html.asString());
  }

  public void setText(String text) {
    impl.setText(text);
  }

  @Override
  protected void onAttach() {
    super.onAttach();
    impl.initElement();
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    impl.uninitElement();
  }
}
