/*
 * StatusBarElementWidget.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DecorativeImage;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

public class StatusBarElementWidget extends FlowPanel implements StatusBarElement
{
   public StatusBarElementWidget()
   {
      options_ = new ArrayList<>();
      icon_ = new FlowPanel();
      label_ = new Label();
      
      icon_.setVisible(false);
      label_.setVisible(true);
      
      add(icon_);
      add(label_);

      addDomHandler(mouseDownEvent ->
      {
         mouseDownEvent.preventDefault();
         mouseDownEvent.stopPropagation();

         if (options_.size() == 0)
            return;

         StatusBarPopupMenu menu = new StatusBarPopupMenu();
         for (final String option : options_)
         {
            menu.addItem(new MenuItem(option, (Command) () ->
            {
               SelectionEvent.fire(StatusBarElementWidget.this, option);
            }));
         }
         
         menu.showRelativeToUpward(label_,
               StringUtil.equals(popupAlignment_, POPUP_ALIGNMENT_RIGHT));
         
      }, MouseDownEvent.getType());
   }
   
   public void setIcon(ImageResource resource)
   {
      icon_.clear();
      hideSpinner();

      if (resource != null)
      {
         Image icon = new Image(resource);
         icon.addStyleName(RES.styles().icon());
         icon_.add(icon);
      }

      icon_.setVisible(resource != null);
   }

   public void showSpinner()
   {
      icon_.setVisible(false);
      if (spinner_ == null)
      {
         spinner_ = new Image(SPINNER_SVG_URI);
         spinner_.setPixelSize(12, 12);
         spinner_.addStyleName(RES.styles().spinnerIcon());
         StatusBarElementWidget.this.insert(spinner_, 0);
      }
      spinner_.setVisible(true);
   }

   public void hideSpinner()
   {
      if (spinner_ != null)
      {
         spinner_.setVisible(false);
      }
   }

   public void setValue(String value)
   {
      setIcon(null);
      label_.setText(value);
   }

   public String getValue()
   {
      return label_.getText();
   }

   public void addOptionValue(String option)
   {
      options_.add(option);
   }

   public void clearOptions()
   {
      options_.clear();
   }

   public void click()
   {
      NativeEvent evt = Document.get().createMouseDownEvent(0, 0, 0, 0, 0,
                                                            false, false,
                                                            false, false, 0);
      ClickEvent.fireNativeEvent(evt, this);
   }

   public void setPopupAlignment(String alignment)
   {
      popupAlignment_ = alignment;
   }

   public void setShowArrows(boolean showArrows)
   {
      if (showArrows ^ arrows_ != null)
      {
         if (showArrows)
         {
            arrows_ = new DecorativeImage(new ImageResource2x(RES.upDownArrow2x()));
            arrows_.addStyleName("rstudio-themes-inverts");
            add(arrows_);
         }
         else
         {
            arrows_.removeFromParent();
            arrows_ = null;
         }
      }
   }

   public String getText()
   {
      return label_.getText();
   }

   public void setText(String s)
   {
      label_.setText(s);
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<String> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }

   public HandlerRegistration addMouseDownHandler(final MouseDownHandler handler)
   {
      return addDomHandler(mouseDownEvent ->
      {
         if (clicksEnabled_)
            handler.onMouseDown(mouseDownEvent);
      }, MouseDownEvent.getType());
   }

   public void setContentsVisible(boolean visible)
   {
      label_.setVisible(visible);
      if (arrows_ != null)
         arrows_.setVisible(visible);
   }

   public boolean getContentsVisible()
   {
      return label_.isVisible();
   }

   public void setClicksEnabled(boolean enabled)
   {
      clicksEnabled_ = enabled;
   }

   private final ArrayList<String> options_;
   private final FlowPanel icon_;
   private final Label label_;
   private Image spinner_;
   private DecorativeImage arrows_;
   private boolean clicksEnabled_ = true;

   // animated SVG spinner that works as an <img> element (inline display safe)
   private static final String SPINNER_SVG_URI =
      "data:image/svg+xml," +
      "%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' " +
      "width='24' height='24'%3E" +
      "%3Cstyle%3E@keyframes s{to{transform:rotate(360deg)}}%3C/style%3E" +
      "%3Ccircle cx='12' cy='12' r='9' fill='none' stroke='%23888' " +
      "stroke-width='3' stroke-dasharray='42 14' " +
      "style='animation:s .8s linear infinite;transform-origin:center'/%3E" +
      "%3C/svg%3E";
   private String popupAlignment_ = POPUP_ALIGNMENT_LEFT;

   public final static String POPUP_ALIGNMENT_LEFT = "left";
   public final static String POPUP_ALIGNMENT_RIGHT = "right";
   
   interface Styles extends CssResource
   {
      String icon();
      String spinnerIcon();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("StatusBarElementWidget.css")
      Styles styles();
      
      @Source("upDownArrow_2x.png")
      ImageResource upDownArrow2x();
   }

   
   
   private static final Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }
   
}
