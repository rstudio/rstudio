package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.studio.client.RStudioGinjector;

public class NumericValueWidget extends Composite
      implements HasValue<String>,
                 HasEnsureVisibleHandlers
{
   public NumericValueWidget(String label)
   {
      FlowPanel flowPanel = new FlowPanel();
      flowPanel.add(new SpanLabel(label, true));

      textBox_ = new TextBox();
      textBox_.setWidth("30px");
      textBox_.getElement().getStyle().setMarginLeft(0.6, Unit.EM);
      flowPanel.add(textBox_);

      initWidget(flowPanel);
      PreferencesDialogResources res = GWT.create(PreferencesDialogResources.class);
      addStyleName(res.styles().numericValueWidget());
   }

   public String getValue()
   {
      return textBox_.getValue();
   }

   public void setValue(String value)
   {
      textBox_.setValue(value);
   }

   public void setValue(String value, boolean fireEvents)
   {
      textBox_.setValue(value, fireEvents);
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler)
   {
      return textBox_.addValueChangeHandler(handler);
   }

   public boolean validate(String fieldName)
   {
      String value = textBox_.getValue().trim();
      if (!value.matches("^\\d+$"))
      {
         fireEvent(new EnsureVisibleEvent());
         textBox_.getElement().focus();
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               "Error",
               fieldName + " must be a valid number.");
         return false;
      }
      return true;
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   private TextBox textBox_;
}
