package org.rstudio.studio.client.workbench.views.vcs.console;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.widget.FontSizer;

public class ConsoleBarView extends Composite
   implements ConsoleBarPresenter.Display,
              HasKeyDownHandlers,
              HasSelectionCommitHandlers<String>,
              HasText
{
   interface MyBinder extends UiBinder<Widget, ConsoleBarView>
   {}

   interface Resources extends ClientBundle
   {
      ImageResource chevronUp();
      ImageResource chevronDown();
   }

   public ConsoleBarView()
   {
      initWidget(GWT.<MyBinder>create(MyBinder.class).createAndBindUi(this));

      expand_.setResource(res_.chevronUp());

      FontSizer.applyNormalFontSize(getElement());
      FontSizer.applyNormalFontSize(input_);
   }

   @Override
   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return input_.addKeyDownHandler(handler);
   }

   @Override
   public HandlerRegistration addSelectionCommitHandler(SelectionCommitHandler<String> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }

   @Override
   public String getText()
   {
      return input_.getText();
   }

   @Override
   public void setText(String text)
   {
      input_.setText(text);
   }

   @UiField
   Image expand_;
   @UiField
   TextBox input_;

   private String defaultText_ = "git ";
   private final Resources res_ = GWT.create(Resources.class);
}
