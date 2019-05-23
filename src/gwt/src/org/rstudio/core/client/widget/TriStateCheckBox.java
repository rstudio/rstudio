/*
 * TriStateCheckBox.java
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

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;

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

// A three state checkbox that toggles between
// off -> indeterminate -> on
public class TriStateCheckBox extends Composite
   implements HasValueChangeHandlers<TriStateCheckBox.State>
{
   public static class State
   {
      private State () {}
   }
   
   public TriStateCheckBox(String label)
   {
      panel_ = new HorizontalPanel();
      panel_.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
      panel_.addDomHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            toggleState();
         }
      }, ClickEvent.getType());
      
      alignHelper_ = new InlineHTML();
      alignHelper_.addStyleName(RES.styles().alignHelper());
      
      checkboxInner_ = new Image();
      checkboxOuter_ = new FlowPanel();
      checkboxOuter_.add(alignHelper_);
      checkboxOuter_.add(checkboxInner_);
      panel_.add(checkboxOuter_);
      
      label_ = new Label(label);
      label_.addStyleName(RES.styles().checkboxLabel());
      panel_.add(label_);
      
      setState(STATE_INDETERMINATE);
      
      initWidget(panel_);
   }
   
   public void setState(State state)
   {
      if (state == STATE_INDETERMINATE)
         checkboxInner_.setResource(new ImageResource2x(
            ThemeResources.INSTANCE.checkboxTri2x()));
      else if (state == STATE_OFF)
         checkboxInner_.setResource(ThemeResources.INSTANCE.checkboxOff());
      else if (state == STATE_ON)
         checkboxInner_.setResource(ThemeResources.INSTANCE.checkboxOn());
      
      checkboxOuter_.getElement().getStyle().setHeight(
            checkboxInner_.getHeight(), Unit.PX);
      state_ = state;
   }
   
   public void setValue(boolean value)
   {
      if (value)
      {
         checkboxInner_.setResource(ThemeResources.INSTANCE.checkboxOn());
         state_ = STATE_ON;
      }
      else
      {
         checkboxInner_.setResource(ThemeResources.INSTANCE.checkboxOff());
         state_ = STATE_OFF;
      }
   }
   
   private void toggleState()
   {
      if (state_ == STATE_OFF)
         setState(STATE_ON);
      else if (state_ == STATE_INDETERMINATE)
         setState(STATE_OFF);
      else if (state_ == STATE_ON)
         setState(STATE_INDETERMINATE);
      
      ValueChangeEvent.fire(this, state_);
   }
   
   public State getState()
   {
      return state_;
   }
   
   public boolean isChecked()
   {
      return state_ == STATE_ON;
   }
   
   public boolean isUnchecked()
   {
      return state_ == STATE_OFF;
   }
   
   public boolean isIndeterminate()
   {
      return state_ == STATE_INDETERMINATE;
   }
   
   private final HorizontalPanel panel_;
   private final Label label_;
   
   private final InlineHTML alignHelper_;
   private final Image checkboxInner_;
   private final FlowPanel checkboxOuter_;
   private State state_;
   
   public static final State STATE_INDETERMINATE = new State();
   public static final State STATE_OFF = new State();
   public static final State STATE_ON = new State();
   
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
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<State> handler)
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
