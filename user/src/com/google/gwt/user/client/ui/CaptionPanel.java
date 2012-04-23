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
package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.FieldSetElement;
import com.google.gwt.dom.client.LegendElement;
import com.google.gwt.safehtml.shared.SafeHtml;

import java.util.Iterator;

/**
 * A panel that wraps its contents in a border with a caption that appears in
 * the upper left corner of the border. This is an implementation of the
 * fieldset HTML element.
 */
public class CaptionPanel extends Composite implements HasWidgets.ForIsWidget {
  /**
   * Implementation class without browser-specific hacks.
   */
  public static class CaptionPanelImpl {
    public void setCaption(
        FieldSetElement fieldset, Element legend, SafeHtml caption) {
      setCaption(fieldset, legend, caption.asString(), true);
    }

    public void setCaption(FieldSetElement fieldset, Element legend,
        String caption, boolean asHTML) {
      // TODO(bruce): rewrite to be inlinable
      assert (caption != null);

      if (asHTML) {
        legend.setInnerHTML(caption);
      } else {
        legend.setInnerText(caption);
      }

      if (!"".equals(caption)) {
        // This is formulated to become an append (if there's no widget), an
        // insertion at index 0 (if there is a widget but no legend already), or
        // a no-op (if the legend is already in place).
        fieldset.insertBefore(legend, fieldset.getFirstChild());
      } else if (legend.getParentNode() != null) {
        // We remove the legend from the DOM because leaving it in with an empty
        // string renders as an ugly gap in the top border on some browsers.
        fieldset.removeChild(legend);
      }
    }
  }

  /**
   * Implementation class that handles Mozilla rendering issues.
   */
  public static class CaptionPanelImplMozilla extends CaptionPanelImpl {
    @Override
    public void setCaption(
        final FieldSetElement fieldset, Element legend, SafeHtml caption) {
      setCaption(fieldset, legend, caption.asString(), true);
    }

    @Override
    public void setCaption(final FieldSetElement fieldset, Element legend,
        String caption, boolean asHTML) {
      fieldset.getStyle().setProperty("display", "none");
      super.setCaption(fieldset, legend, caption, asHTML);
      fieldset.getStyle().setProperty("display", "");
    }
  }

  /**
   * Implementation class that handles Safari rendering issues.
   */
  public static class CaptionPanelImplSafari extends CaptionPanelImpl {
    @Override
    public void setCaption(
        final FieldSetElement fieldset, Element legend, SafeHtml caption) {
      setCaption(fieldset, legend, caption.asString(), true);
    }

    @Override
    public void setCaption(final FieldSetElement fieldset, Element legend,
        String caption, boolean asHTML) {
      fieldset.getStyle().setProperty("visibility", "hidden");
      super.setCaption(fieldset, legend, caption, asHTML);
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        public void execute() {
          fieldset.getStyle().setProperty("visibility", "");
        }
      });
    }
  }

  /**
   * The implementation instance.
   */
  private static CaptionPanelImpl impl = (CaptionPanelImpl) GWT.create(CaptionPanelImpl.class);

  /**
   * The legend element used as the caption.
   */
  private LegendElement legend;

  /**
   * Constructs a CaptionPanel with an empty caption.
   */
  public CaptionPanel() {
    this("", false);
  }

  /**
   * Constructs a CaptionPanel with specified caption text.
   *
   * @param caption the text of the caption
   */
  public CaptionPanel(SafeHtml caption) {
    this(caption.asString(), true);
  }

  /**
   * Constructs a CaptionPanel with specified caption text.
   *
   * @param captionText the text of the caption, which is automatically escaped
   */
  public CaptionPanel(String captionText) {
    this(captionText, false);
  }

  /**
   * Constructs a CaptionPanel having the specified caption.
   *
   * @param caption the caption to display
   * @param asHTML if <code>true</code>, the <code>caption</code> param is
   *            interpreted as HTML; otherwise, <code>caption</code> is
   *            treated as text and automatically escaped
   */
  public CaptionPanel(String caption, boolean asHTML) {
    FieldSetElement fieldSet = Document.get().createFieldSetElement();
    initWidget(new SimplePanel(fieldSet));
    legend = Document.get().createLegendElement();
    fieldSet.appendChild(legend);
    if (asHTML) {
      setCaptionHTML(caption);
    } else {
      setCaptionText(caption);
    }
  }

  public void add(Widget w) {
    ((SimplePanel) getWidget()).add(w);
  }
  
  /**
   * Overloaded version for IsWidget.
   * 
   * @see #add(Widget)
   */
  public void add(IsWidget w) {
    this.add(asWidgetOrNull(w));
  }

  /**
   * Removes the content widget.
   */
  public void clear() {
    ((SimplePanel) getWidget()).clear();
  }

  /**
   * Returns the caption as HTML; note that if the caption was previously set
   * using {@link #setCaptionText(String)}, the return value is undefined.
   */
  public String getCaptionHTML() {
    String html = legend.getInnerHTML();
    assert (html != null);
    return html;
  }

  /**
   * Returns the caption as text; note that if the caption was previously set
   * using {@link #setCaptionHTML(String)}, the return value is undefined.
   */
  public String getCaptionText() {
    String text = legend.getInnerText();
    assert (text != null);
    return text;
  }

  /**
   * Accesses the content widget, if present.
   *
   * @return the content widget specified previously in
   *         {@link #setContentWidget(Widget)}
   */
  public Widget getContentWidget() {
    return ((SimplePanel) getWidget()).getWidget();
  }

  /**
   * Iterates over the singular content widget, if present.
   */
  public Iterator<Widget> iterator() {
    return ((SimplePanel) getWidget()).iterator();
  }

  /**
   * Removes the specified widget, although in practice the specified widget
   * must be the content widget.
   *
   * @param w the widget to remove; note that anything other than the Widget
   *            returned by {@link #getContentWidget()} will have no effect
   */
  public boolean remove(Widget w) {
    return ((SimplePanel) getWidget()).remove(w);
  }
  
  /**
   * Overloaded version for IsWidget.
   * 
   * @see #remove(Widget)
   */
  public boolean remove(IsWidget w) {
    return this.remove(asWidgetOrNull(w));
  }

  /**
   * Sets the caption for the panel using an HTML fragment. Pass in empty string
   * to remove the caption completely, leaving just the unadorned panel.
   *
   * @param html HTML for the new caption; must not be <code>null</code>
   */
  public void setCaptionHTML(String html) {
    assert (html != null);
    impl.setCaption(FieldSetElement.as(getElement()), legend, html, true);
  }

  /**
   * Sets the caption for the panel using a SafeHtml string.
   *
   * @param html HTML for the new caption; must not be <code>null</code>
   */
  public void setCaptionHTML(SafeHtml html) {
    setCaptionHTML(html.asString());
  }

  /**
   * Sets the caption for the panel using text that will be automatically
   * escaped. Pass in empty string to remove the caption completely, leaving
   * just the unadorned panel.
   *
   * @param text text for the new caption; must not be <code>null</code>
   */
  public void setCaptionText(String text) {
    assert (text != null);
    impl.setCaption(FieldSetElement.as(getElement()), legend, text, false);
  }

  /**
   * Sets or replaces the content widget within the CaptionPanel.
   *
   * @param w the content widget to be set
   */
  public void setContentWidget(Widget w) {
    ((SimplePanel) getWidget()).setWidget(w);
  }
}
