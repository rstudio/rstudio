package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.widget.ThemedButton;

public class TextBoxWithButton extends Composite
{
   public TextBoxWithButton(String label, String action, ClickHandler handler)
   {
      PreferencesDialogResources res = GWT.create(PreferencesDialogResources.class);

      textBox_ = new TextBox();
      textBox_.setWidth("100%");
      textBox_.setReadOnly(true);

      themedButton_ = new ThemedButton(action, handler);

      HorizontalPanel inner = new HorizontalPanel();
      inner.add(textBox_);
      inner.add(themedButton_);
      inner.setCellWidth(textBox_, "100%");
      inner.setWidth("100%");

      FlowPanel outer = new FlowPanel();
      if (label != null)
      {
         outer.add(new Label(label, true));
      }
      outer.add(inner);
      initWidget(outer);

      this.addStyleName(res.styles().textBoxWithButton());
   }

   public void setText(String text)
   {
      textBox_.setText(text);
   }

   public void click()
   {
      themedButton_.click();
   }

   public boolean isEnabled()
   {
      return themedButton_.isEnabled();
   }

   public void setEnabled(boolean enabled)
   {
      textBox_.setEnabled(enabled);
      themedButton_.setEnabled(enabled);
   }

   private TextBox textBox_;
   private ThemedButton themedButton_;
}
