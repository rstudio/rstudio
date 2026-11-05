/*
 * WarningBar.java
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
package org.rstudio.studio.client.application.ui;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Document;
import com.google.inject.Inject;
import org.rstudio.core.client.theme.res.ThemeResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.ImageButton;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.WarningBarClosedEvent;

public class WarningBar extends Composite
      implements HasCloseHandlers<WarningBar>
{
   interface Resources extends ClientBundle
   {
      @Source("WarningBar.css")
      Styles styles();

      ImageResource warningBarLeft();
      ImageResource warningBarRight();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource warningBarTile();

      @Source("warningIconSmall_2x.png")
      ImageResource warningIconSmall2x(); 
   }

   interface Styles extends CssResource
   {
      String warningBar();
      String left();
      String right();
      String center();
      String message();
      String controls();
      String details();
      String warningIcon();
      String label();
      String action();
      String dismiss();

      String warning();
      String error();
   }

   interface Binder extends UiBinder<Widget, WarningBar>{}
   static final Binder binder = GWT.create(Binder.class);

   @Inject
   public WarningBar(EventBus events, GlobalDisplay globalDisplay)
   {
      events_ = events;
      initWidget(binder.createAndBindUi(this));
      dismiss_.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
      dismiss_.addClickHandler(event -> CloseEvent.fire(WarningBar.this, WarningBar.this));
      moreButton_.setVisible(false);
      moreButton_.setText(constants_.manageLicenseText());
      actionLink_.setVisible(false);
      actionLink_.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
      // registered once, as each setClickHandler call adds another handler
      actionLink_.setClickHandler(() ->
      {
         if (action_ != null)
            action_.execute();
      });
      moreButton_.addClickHandler(event -> Desktop.getFrame().showLicenseDialog());
      label_.setId(Document.get().createUniqueId());
      Roles.getButtonRole().setAriaDescribedbyProperty(dismiss_.getElement(), Id.of(label_.getId()));
      details_.setText(constants_.warningDetailsText());
      details_.addClickHandler(event -> globalDisplay.showMessage(
            GlobalDisplay.MSG_WARNING,
            constants_.warningDetailsCaption(),
            getText(),
            detailsReturnFocus_));
   }

   public void setText(String value)
   {
      label_.setInnerHTML(value);
      label_.setTitle(getText());
   }

   public String getText()
   {
      return label_.getInnerText();
   }

   public void setWarningNumber(int number)
   {
      Roles.getGroupRole().set(getElement());
      Roles.getGroupRole().setAriaLabelProperty(getElement(), constants_.warningNumberLabel(number));
      dismiss_.setDescription(constants_.dismissWarningLabel(number));
      Roles.getButtonRole().setAriaLabelProperty(details_.getElement(), constants_.warningDetailsLabel(number));
   }

   public void setDetailsReturnFocus(CanFocus returnFocus)
   {
      detailsReturnFocus_ = returnFocus;
   }

   public void focusDetailsButton()
   {
      details_.setFocus(true);
   }

   public void focusDismissButton()
   {
      dismiss_.setFocus(true);
   }

   /**
    * Show an action link after the message, or hide it when the label is null.
    */
   public void setAction(String label, Command command)
   {
      if (label == null || command == null)
      {
         action_ = null;
         actionLink_.setVisible(false);
         return;
      }

      action_ = command;
      actionLink_.setText(label);
      actionLink_.setVisible(true);
   }

   public void showLicenseButton(boolean show)
   {
      // never show the license button in server mode or remote desktop mode
      // license button should only be visible when error is purely the result of a local license problem
      if (Desktop.isDesktop())
         moreButton_.setVisible(show);
   }

   public void setSeverity(boolean severe)
   {
      if (severe)
      {
         addStyleName(styles_.error());
         removeStyleName(styles_.warning());
      }
      else
      {
         addStyleName(styles_.warning());
         removeStyleName(styles_.error());
      }
   }

   public int getHeight()
   {
      return HEIGHT;
   }

   public void setWarningId(String id)
   {
      id_ = id;
   }

   public static final int HEIGHT = 28;
   private String id_;

   public HandlerRegistration addCloseHandler(CloseHandler<WarningBar> handler)
   {
      return addHandler(handler, CloseEvent.getType());
   }
   
   @Override
   public void onDetach()
   {
      events_.fireEvent(new WarningBarClosedEvent(id_));
      super.onDetach();
   }

   @UiField
   SpanElement label_;
   @UiField
   Button details_;
   @UiField
   Button moreButton_;
   @UiField
   HyperlinkLabel actionLink_;
   @UiField
   ImageButton dismiss_;

   private Command action_;
   private CanFocus detailsReturnFocus_ = this::focusDetailsButton;

   private static final Styles styles_ =
         ((Resources) GWT.create(Resources.class)).styles();
   static
   {
      styles_.ensureInjected();
   }
   
   private final EventBus events_;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
