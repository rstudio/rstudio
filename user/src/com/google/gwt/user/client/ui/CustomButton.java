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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * Custom Button is a base button class with built in support for a set number
 * of button faces.
 * 
 * Each face has its own style modifier. For example, <code>downHovering</code>
 * is assigned the CSS modifier <i>downHovering</i>. So, if the button's
 * overall style name is <i>gwt-PushButton</i> then when showing the
 * <code>downHovering</code> face, the button's style is <i>
 * gwt-PushButton-downHovering</i>.
 * 
 * <p>
 * Each button face can be assigned is own image, text, or html contents. If no
 * content is defined for a face, then the face will use the contents of another
 * face. For example, if <code>downHovering</code> does not have defined
 * contents, it will use the contents defined by the <code>down</code> face.
 * </p>
 * 
 * 
 * 
 * <p>
 * The supported faces are defined below:
 * </p>
 * <p>
 * <table border="4">
 * <tr>
 * 
 * <td><b>CSS style name</b></td>
 * <td><b>Getter method</b></td>
 * <td><b>description of face</b></td>
 * <td><b>defaults to contents of face</b></td>
 * </tr>
 * 
 * <tr>
 * <td>up</td>
 * <td> {@link #getUpFace()} </td>
 * <td>face shown when button is up</td>
 * <td> none</td>
 * </tr>
 * 
 * <tr>
 * <td>down</td>
 * <td> {@link #getDownFace()} </td>
 * <td>face shown when button is down</td>
 * <td> up </td>
 * </tr>
 * 
 * <tr>
 * <td>upHovering</td>
 * <td> {@link #getUpHoveringFace()} </td>
 * <td>face shown when button is up and hovering</td>
 * <td> up </td>
 * </tr>
 * 
 * 
 * <tr>
 * <td>upDisabled</td>
 * <td> {@link #getUpDisabledFace()} </td>
 * <td>face shown when button is up and disabled</td>
 * <td> up</td>
 * </tr>
 * 
 * <tr>
 * <td>downHovering</td>
 * <td> {@link #getDownHoveringFace()} </td>
 * <td>face shown when button is down and hovering</td>
 * <td> down</td>
 * </tr>
 * 
 * <tr>
 * <td>downDisabled</td>
 * <td> {@link #getDownDisabledFace()} </td>
 * <td>face shown when button is down and disabled</td>
 * <td>down</td>
 * </tr>
 * </table>
 * </p>
 * 
 */
public abstract class CustomButton extends ButtonBase implements
    SourcesKeyboardEvents {
  /**
   * Represents a button's face. Each face is associated with its own style
   * modifier and, optionally, its own contents html, text, or image.
   */
  public abstract class Face implements HasHTML, HasText {
    private static final String STYLENAME_HTML_FACE = "html-face";
    private final Face delegateTo;
    private Element face;

    /**
     * Constructor for <code>Face</code>. Creates a new face that delegates
     * to the supplied face.
     * 
     * @param delegateTo default content provider
     */
    private Face(Face delegateTo) {
      this.delegateTo = delegateTo;
    }

    /**
     * Gets the face's contents as html.
     * 
     * @return face's contents as html
     * 
     */
    public String getHTML() {
      return DOM.getInnerHTML(getFace());
    }

    /**
     * Gets the face's contents as text.
     * 
     * @return face's contents as text
     * 
     */
    public String getText() {
      return DOM.getInnerText(getFace());
    }

    /**
     * Set the face's contents as html.
     * 
     * @param html html to set as face's contents html
     * 
     */
    public void setHTML(String html) {
      face = DOM.createDiv();
      UIObject.setStyleName(face, STYLENAME_HTML_FACE, true);
      DOM.setInnerHTML(face, html);
      updateButtonFace();
    }

    /**
     * Set the face's contents as an image.
     * 
     * @param image image to set as face contents
     */
    public final void setImage(Image image) {
      face = image.getElement();
      updateButtonFace();
    }

    /**
     * Sets the face's contents as text.
     * 
     * @param text text to set as face's contents
     */
    public final void setText(String text) {
      face = DOM.createDiv();
      UIObject.setStyleName(face, STYLENAME_HTML_FACE, true);
      DOM.setInnerText(face, text);
      updateButtonFace();
    }

    public final String toString() {
      return this.getName();
    }

    /**
     * Gets the ID associated with this face. This will be a bitwise and of all
     * of the attributes that comprise this face.
     */
    abstract int getFaceID();

    /**
     * Get the name of the face. This property is also used as a modifier on the
     * <code>CustomButton</code> style. <p/> For instance, if the
     * <code>CustomButton</code> style is "gwt-PushButton" and the face name
     * is "up", then the CSS class name will be "gwt-PushButton-up".
     * 
     * @return the face's name
     */
    abstract String getName();

    /**
     * Gets the contents associated with this face.
     */
    private Element getFace() {
      if (face == null) {
        if (delegateTo == null) {
          // provide a default face as none was supplied.
          face = DOM.createDiv();
          return face;
        } else {
          return delegateTo.getFace();
        }
      } else {
        return face;
      }
    }

    private void updateButtonFace() {
      if (curFace != null && curFace.getFace() == this.getFace()) {
        setCurrentFaceElement(face);
      }
    }
  }

  private static final String STYLENAME_DEFAULT = "gwt-CustomButton";

  /**
   * Pressed Attribute bit.
   */
  private static final int DOWN_ATTRIBUTE = 1;

  /**
   * Hovering Attribute bit.
   */
  private static final int HOVERING_ATTRIBUTE = 2;

  /**
   * Disabled Attribute bit.
   */
  private static final int DISABLED_ATTRIBUTE = 4;

  /**
   * ID for up face.
   */
  private static final int UP = 0;

  /**
   * ID for down face.
   */
  private static final int DOWN = DOWN_ATTRIBUTE;

  /**
   * ID for upHovering face.
   */
  private static final int UP_HOVERING = HOVERING_ATTRIBUTE;

  /**
   * ID for downHovering face.
   */
  private static final int DOWN_HOVERING = DOWN_ATTRIBUTE | HOVERING_ATTRIBUTE;

  /**
   * ID for upDisabled face.
   */
  private static final int UP_DISABLED = DISABLED_ATTRIBUTE;

  /**
   * ID for downDisabled face.
   */
  private static final int DOWN_DISABLED = DOWN | DISABLED_ATTRIBUTE;

  /**
   * Base style name. By default gwt-CustomButton.
   */
  private String baseStyleName;

  /**
   * The button's current face element.
   */
  private Element curFaceElement;

  /**
   * The button's current face.
   */
  private Face curFace;

  /**
   * Face for up.
   */
  private Face up;

  /**
   * Face for down.
   */
  private Face down;

  /**
   * Face for downHover.
   */
  private Face downHovering;

  /**
   * Face for upHover.
   */
  private Face upHovering;

  /**
   * Face for upDisabled.
   */
  private Face upDisabled;

  /**
   * Face for downDisabled.
   */
  private Face downDisabled;

  /**
   * 
   * Constructor for <code>CustomButton</code>.
   * 
   * @param upImage image for the default (up) face of the button
   */
  public CustomButton(Image upImage) {
    this();
    getUpFace().setImage(upImage);
  }

  /**
   * 
   * Constructor for <code>CustomButton</code>.
   * 
   * @param upImage image for the default (up) face of the button
   * @param downImage image for the down face of the button
   */
  public CustomButton(Image upImage, Image downImage) {
    this(upImage);
    getDownFace().setImage(downImage);
  }

  /**
   * Constructor for <code>CustomButton</code>.
   * 
   * @param upImage image for the default (up) face of the button
   * @param downImage image for the down face of the button
   * @param listener clickListener
   */
  public CustomButton(Image upImage, Image downImage, ClickListener listener) {
    this(upImage, listener);
    getDownFace().setImage(downImage);
  }

  /**
   * 
   * Constructor for <code>CustomButton</code>.
   * 
   * @param upImage image for the default (up) face of the button
   * @param listener the click listener
   */
  public CustomButton(Image upImage, ClickListener listener) {
    this(upImage);
    addClickListener(listener);
  }

  /**
   * 
   * Constructor for <code>CustomButton</code>.
   * 
   * @param upText the text for the default (up) face of the button.
   */
  public CustomButton(String upText) {
    this();
    getUpFace().setText(upText);
  }

  /**
   * Constructor for <code>CustomButton</code>.
   * 
   * @param upText the text for the default (up) face of the button
   * @param listener the click listener
   */
  public CustomButton(String upText, ClickListener listener) {
    this(upText);
    addClickListener(listener);
  }

  /**
   * 
   * Constructor for <code>CustomButton</code>.
   * 
   * @param upText the text for the default (up) face of the button
   * @param downText the text for the down face of the button
   */
  public CustomButton(String upText, String downText) {
    this(upText);
  }

  /**
   * Constructor for <code>CustomButton</code>.
   */
  protected CustomButton() {
    // Use FocusPanel.impl rather than FocusWidget because only FocusPanel.impl
    // works across browsers to create a focusable element.
    super(FocusPanel.impl.createFocusable());
    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.FOCUSEVENTS);
    setUpFace(createFace(null, "up", UP));
    setStyleName(STYLENAME_DEFAULT);
  }

  /**
   * Gets the downDisabled face of the button.
   * 
   * @return the downDisabled face
   */
  public final Face getDownDisabledFace() {
    if (downDisabled == null) {
      setDownDisabledFace(createFace(getDownFace(), "down-disabled",
          DOWN_DISABLED));
    }
    return downDisabled;
  }

  /**
   * Gets the down face of the button.
   * 
   * @return the down face
   */
  public final Face getDownFace() {
    if (down == null) {
      setDownFace(createFace(getUpFace(), "down", DOWN));
    }
    return down;
  }

  /**
   * Gets the downHovering face of the button.
   * 
   * @return the downHovering face
   */
  public final Face getDownHoveringFace() {
    if (downHovering == null) {
      setDownHoveringFace(createFace(getDownFace(), "down-hovering",
          DOWN_HOVERING));
    }
    return downHovering;
  }

  /**
   * Gets the current face's html.
   * 
   * @return current face's html
   */
  public String getHTML() {
    return getCurrentFace().getHTML();
  }

  /**
   * Gets the style name associated with the object. The actual CSS style name
   * for the button will be this name + "-" + current face name.
   * 
   * @return the object's style name
   * @see #setStyleName(String)
   */
  public final String getStyleName() {
    return baseStyleName;
  }

  public int getTabIndex() {
    return FocusPanel.impl.getTabIndex(getElement());
  }

  /**
   * Gets the current face's text.
   * 
   * @return current face's text
   */
  public String getText() {
    return getCurrentFace().getText();
  }

  /**
   * Gets the upDisabled face of the button.
   * 
   * @return the upDisabled face
   */
  public final Face getUpDisabledFace() {
    if (upDisabled == null) {
      setUpDisabledFace(createFace(getUpFace(), "up-disabled", UP_DISABLED));
    }
    return upDisabled;
  }

  /**
   * Gets the up face of the button.
   * 
   * @return the up face
   */
  public final Face getUpFace() {
    return up;
  }

  /**
   * Gets the upHovering face of the button.
   * 
   * @return the upHovering face
   */
  public final Face getUpHoveringFace() {
    if (upHovering == null) {
      setUpHoveringFace(createFace(getUpFace(), "up-hovering", UP_HOVERING));
    }
    return upHovering;
  }

  public void onBrowserEvent(Event event) {
    // Should not act on button if disabled.
    if (isEnabled() == false) {
      // This can happen when events are bubbled up from non-disabled children
      return;
    }

    int type = DOM.eventGetType(event);
    switch (type) {
      case Event.ONMOUSEOUT:
        setHovering(false);
        break;
      case Event.ONMOUSEOVER:
        setHovering(true);
        break;
    }
    super.onBrowserEvent(event);
  }

  public void setAccessKey(char key) {
    FocusPanel.impl.setAccessKey(getElement(), key);
  }

  /**
   * Sets whether this button is enabled.
   * 
   * @param enabled <code>true</code> to enable the button, <code>false</code>
   *          to disable it
   */
  public final void setEnabled(boolean enabled) {
    if (isEnabled() != enabled) {
      toggleDisabled();
      super.setEnabled(enabled);
    }
  }

  public void setFocus(boolean focused) {
    if (focused) {
      FocusPanel.impl.focus(getElement());
    } else {
      FocusPanel.impl.blur(getElement());
    }
  }

  /**
   * Sets the current face's html.
   * 
   * @param html html to set
   */
  public void setHTML(String html) {
    getCurrentFace().setHTML(html);
  }

  /**
   * Sets the style name associated with the object. The actual CSS style name
   * for the button will be this name + "-" + current face name.
   * 
   * @param styleName the object's style name
   * @see #setStyleName(String)
   */
  public final void setStyleName(String styleName) {
    if (styleName == null) {
      throw new IllegalStateException("Cannot set the base style name to null");
    }
    if (curFace != null) {
      super.removeStyleName(getCSSStyleName());
      baseStyleName = styleName;
      super.addStyleName(getCSSStyleName());
    } else {
      baseStyleName = styleName;
    }
  }

  public void setTabIndex(int index) {
    FocusPanel.impl.setTabIndex(getElement(), index);
  }

  /**
   * Sets the current face's text.
   * 
   * @param text text to set
   */
  public void setText(String text) {
    getCurrentFace().setText(text);
  }

  /**
   * Is this button down?
   * 
   * @return <code>true</code> if the button is down
   */
  protected boolean isDown() {
    return (DOWN_ATTRIBUTE & getCurrentFace().getFaceID()) > 0;
  }

  /**
   * Overridden on attach to ensure that a button face has been chosen before
   * the button is displayed.
   */
  protected void onAttach() {
    finishSetup();
    super.onAttach();
  }

  /**
   * Sets whether this button is down.
   * 
   * @param down <code>true</code> to press the button, <code>false</code>
   *          otherwise
   */
  protected void setDown(boolean down) {
    if (down != isDown()) {
      toggleDown();
    }
  }

  /**
   * Common setup between constructors.
   */
  void finishSetup() {
    if (curFace == null) {
      setCurrentFace(getUpFace());
    }
  }

  /**
   * Gets the current face of the button.
   * 
   * @return the current face
   */

  Face getCurrentFace() {
    /*
     * Implementation note: Package protected so we can use it when testing the
     * button.
     */

    finishSetup();

    return curFace;
  }

  /**
   * Is the mouse hovering over this button?
   * 
   * @return <code>true</code> if the mouse is hovering
   */
  final boolean isHovering() {
    return (HOVERING_ATTRIBUTE & getCurrentFace().getFaceID()) > 0;
  }

  void setCurrentFace(Face newFace) {
    /*
     * Implementation note: default access for testing.
     */
    if (curFace != newFace) {
      if (curFace != null) {
        super.removeStyleName(getCSSStyleName());
      }
      curFace = newFace;
      setCurrentFaceElement(newFace.getFace());
      super.addStyleName(getCSSStyleName());
    }
  }

  /**
   * Sets whether this button is hovering.
   * 
   * @param hovering is this button hovering?
   */
  final void setHovering(boolean hovering) {
    if (hovering != isHovering()) {
      toggleHover();
    }
  }

  /**
   * Toggle the up/down attribute.
   */
  void toggleDown() {
    int newFaceID = getCurrentFace().getFaceID() ^ DOWN_ATTRIBUTE;
    setCurrentFace(newFaceID);
  }

  private Face createFace(Face delegateTo, final String name, final int faceID) {
    return new Face(delegateTo) {

      public String getName() {
        return name;
      }

      int getFaceID() {
        return faceID;
      }
    };
  }

  /**
   * Gets the modified style name.
   * 
   * @return the modified style name
   */
  private String getCSSStyleName() {
    return baseStyleName + "-" + curFace.getName();
  }

  private Face getFaceFromID(int id) {
    switch (id) {
      case DOWN:
        return getDownFace();
      case UP:
        return getUpFace();
      case DOWN_HOVERING:
        return getDownHoveringFace();
      case UP_HOVERING:
        return getUpHoveringFace();
      case UP_DISABLED:
        return getUpDisabledFace();
      case DOWN_DISABLED:
        return getDownDisabledFace();
      default:
        throw new IllegalStateException(id + " is not a known face id.");
    }
  }

  /**
   * Sets the current face based on the faceID.
   * 
   * @param faceID sets the new face of the button
   */
  private void setCurrentFace(int faceID) {
    Face newFace = getFaceFromID(faceID);
    setCurrentFace(newFace);
  }

  private void setCurrentFaceElement(Element newFaceElement) {
    if (curFaceElement != newFaceElement) {
      if (curFaceElement != null) {
        DOM.removeChild(getElement(), curFaceElement);
      }
      curFaceElement = newFaceElement;
      DOM.appendChild(getElement(), curFaceElement);
    }
  }

  /**
   * Sets the downDisabled face of the button.
   * 
   * @param downDisabled downDisabled face
   */
  private void setDownDisabledFace(Face downDisabled) {
    this.downDisabled = downDisabled;
  }

  /**
   * Sets the down face of the button.
   * 
   * @param down the down face
   */
  private void setDownFace(Face down) {
    this.down = down;
  }

  /**
   * Sets the downHovering face of the button.
   * 
   * @param downHovering hoverDown face
   */
  private void setDownHoveringFace(Face downHovering) {
    this.downHovering = downHovering;
  }

  /**
   * Sets the upDisabled face of the button.
   * 
   * @param upDisabled upDisabled face
   */
  private void setUpDisabledFace(Face upDisabled) {
    this.upDisabled = upDisabled;
  }

  /**
   * Sets the up face of the button.
   * 
   * @param up up face
   */
  private void setUpFace(Face up) {
    this.up = up;
  }

  /**
   * Sets the upHovering face of the button.
   * 
   * @param upHovering upHovering face
   */
  private void setUpHoveringFace(Face upHovering) {
    this.upHovering = upHovering;
  }

  /**
   * Toggle the disabled attribute.
   */
  private void toggleDisabled() {
    // Toggle disabled.
    int newFaceID = getCurrentFace().getFaceID() ^ DISABLED_ATTRIBUTE;

    // Remove hovering.
    newFaceID &= ~HOVERING_ATTRIBUTE;

    // Sets the current face.
    setCurrentFace(newFaceID);
  }

  /**
   * Toggle the hovering attribute.
   */
  private void toggleHover() {
    // Toggle hovering.
    int newFaceID = getCurrentFace().getFaceID() ^ HOVERING_ATTRIBUTE;

    // Remove disabled.
    newFaceID &= ~DISABLED_ATTRIBUTE;
    setCurrentFace(newFaceID);
  }
}
