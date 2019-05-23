/*
 * ThemedCheckBox.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;

import org.rstudio.core.client.theme.res.ThemeResources;

public class ThemedCheckBox extends Composite implements HasValueChangeHandlers<Boolean>
{
   public ThemedCheckBox(String label, boolean initialValue)
   {
      panel_ = new HorizontalPanel();
      panel_.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
      panel_.addDomHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            toggleValue();
         }
      }, ClickEvent.getType());
      
      alignHelper_ = new InlineHTML();
      alignHelper_.addStyleName(RES.styles().alignHelper());
      
      checkboxInner_ = new Image();
      checkboxOuter_ = new FlowPanel();
      checkboxOuter_.getElement().getStyle().setHeight(12, Unit.PX);
      checkboxOuter_.add(alignHelper_);
      checkboxOuter_.add(checkboxInner_);
      panel_.add(checkboxOuter_);
      
      label_ = new Label(label);
      label_.addStyleName(RES.styles().checkboxLabel());
      panel_.add(label_);
      
      setValue(initialValue, true);
      
      initWidget(panel_);
   }
   
   public ThemedCheckBox(String label)
   {
      this(label, false);
   }
   
   public boolean toggleValue()
   {
      setValue(!getValue());
      return getValue();
   }
   
   public boolean getValue()
   {
      return value_;
   }
   
   private void setValue(boolean value, boolean force)
   {
      if (force || value_ != value)
      {
         value_ = value;
         updateCheckboxImage();
         ValueChangeEvent.fire(this, value);
      }
   }
   
   public void setValue(boolean value)
   {
      setValue(value, false);
   }
   
   private void updateCheckboxImage()
   {
      if (getValue())
         checkboxInner_.setResource(ThemeResources.INSTANCE.checkboxOn());
      else
         checkboxInner_.setResource(ThemeResources.INSTANCE.checkboxOff());
   }
   
   private final HorizontalPanel panel_;
   private final Label label_;
   
   private final InlineHTML alignHelper_;
   private final Image checkboxInner_;
   private final FlowPanel checkboxOuter_;
   
   private boolean value_;
   
   public interface Styles extends CssResource
   {
      String alignHelper();
      String checkboxLabel();
   }
   public interface Resources extends ClientBundle
   {
      @Source("TriStateCheckBox.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
   private final HandlerManager handlerManager_ = new HandlerManager(this);
   
   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler)
   {
      return handlerManager_.addHandler(
            ValueChangeEvent.getType(),
            handler);
   }

   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlerManager_.fireEvent(event);
   }
   

}
