/*
 * Toggle.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import com.google.gwt.aria.client.CheckedValue;
import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.RStudioGinjector;

/**
 * A custom-drawn checkbox supporting both on/off, and on/off/indeterminate modes
 */
public class Toggle
      extends Composite
      implements HasValueChangeHandlers<Toggle.State>
{
   public enum State
   {
      OFF,
      INDETERMINATE,
      ON
   }
   
   public Toggle(String label, boolean indeterminateStateEnabled)
   {
      this();
      setText(label);
      setIndeterminateStateEnabled(indeterminateStateEnabled);
      setState(indeterminateStateEnabled ? State.INDETERMINATE : State.OFF);
   }
   
   public Toggle(String label)
   {
      this();
      setText(label);
      setState(State.INDETERMINATE);
   }
   
   private Toggle()
   {
      BINDER.createAndBindUi(this);

      container_.addDomHandler((ClickEvent clickEvent) ->
      {
         track_.getElement().focus();
         toggleState();
      }, ClickEvent.getType());

      container_.addDomHandler((KeyDownEvent keyDownEvent) ->
      {
         if (keyDownEvent.getNativeKeyCode() == KeyCodes.KEY_SPACE)
         {
            toggleState();
         }
      }, KeyDownEvent.getType());

      DomUtils.ensureHasId(label_.getElement());
      Roles.getCheckboxRole().set(track_.getElement());
      Roles.getCheckboxRole().setAriaLabelledbyProperty(track_.getElement(), Id.of(label_.getElement()));
      track_.getElement().setTabIndex(0);
      initWidget(container_);
   }
   
   public void setState(State state, boolean animate)
   {
      if (!indeterminateStateEnabled_ && state == State.INDETERMINATE)
      {
         assert false : "Attempted to set indeterminate state on binary toggle";
      }
      
      if (animate && !RStudioGinjector.INSTANCE.getUserPrefs().reducedMotion().getValue())
         knob_.removeStyleName(styles_.transitionDisabled());
      else
         knob_.addStyleName(styles_.transitionDisabled());
      
      clearToggleStyles();
      
      switch (state)
      {
      case OFF:
         knob_.addStyleName(styles_.knobLeft());
         Roles.getCheckboxRole().setAriaCheckedState(track_.getElement(), CheckedValue.FALSE);
         break;
         
      case INDETERMINATE:
         knob_.addStyleName(styles_.knobMiddle());
         Roles.getCheckboxRole().setAriaCheckedState(track_.getElement(), CheckedValue.MIXED);
         break;
         
      case ON:
         knob_.addStyleName(styles_.knobRight());
         Roles.getCheckboxRole().setAriaCheckedState(track_.getElement(), CheckedValue.TRUE);
         break;
      }
      
      if (state_ != state)
      {
         state_ = state;
         ValueChangeEvent.fire(this, state);
      }
   }
   
   public void setState(State state)
   {
      setState(state, false);
   }
   
   public void setValue(boolean value)
   {
      setState(value ? State.ON : State.OFF);
   }
   
   public void setText(String text)
   {
      label_.setText(text);
   }
   
   public void setIndeterminateStateEnabled(boolean enabled)
   {
      indeterminateStateEnabled_ = enabled;
   }
   
   public State getState()
   {
      return state_;
   }
   
   private void toggleState()
   {
      clearToggleStyles();
      
      switch (state_)
      {
      case OFF:
         setState(indeterminateStateEnabled_ ? State.INDETERMINATE : State.ON, true);
         break;
         
      case INDETERMINATE:
         setState(State.ON, true);
         break;
         
      case ON:
         setState(State.OFF, true);
         break;
      }
   }
   
   private void clearToggleStyles()
   {
      knob_.removeStyleName(styles_.knobLeft());
      knob_.removeStyleName(styles_.knobMiddle());
      knob_.removeStyleName(styles_.knobRight());
   }
   
   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<State> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }
   
   private State state_ = State.OFF;
   private boolean indeterminateStateEnabled_ = true;
   
   @UiField HorizontalPanel container_;
   @UiField FlowPanel track_;
   @UiField FlowPanel knob_;
   @UiField Label label_;
   @UiField Styles styles_;
   
   // UI Binder ----
   static interface Binder extends UiBinder<Widget, Toggle>
   {
   }
   
   public interface Styles extends CssResource
   {
      String track();
      String knob();
      String knobLeft();
      String knobMiddle();
      String knobRight();
      String transitionDisabled();
   }

   private static final Binder BINDER = GWT.create(Binder.class);

}
