/*
 * WarningBar.java
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
package org.rstudio.studio.client.application.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.DivElement;
import com.google.inject.Inject;
import org.rstudio.core.client.a11y.A11y;
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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.ImageButton;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.WarningBarClosedEvent;
import org.rstudio.studio.client.common.Timers;

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
      String warningIcon();
      String label();
      String dismiss();

      String warning();
      String error();
   }

   interface Binder extends UiBinder<Widget, WarningBar>{}
   static final Binder binder = GWT.create(Binder.class);

   @Inject
   public WarningBar(EventBus events, AriaLiveService ariaLive)
   {
      events_ = events;
      initWidget(binder.createAndBindUi(this));
      dismiss_.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
      dismiss_.addClickHandler(event -> CloseEvent.fire(WarningBar.this, WarningBar.this));
      moreButton_.setVisible(false);
      moreButton_.setText("Manage License...");
      moreButton_.addClickHandler(event -> Desktop.getFrame().showLicenseDialog());
      A11y.setARIAHidden(label_);
      if (!ariaLive.isDisabled(AriaLiveService.WARNING_BAR))
         Roles.getAlertRole().set(live_);
   }

   public void setText(String value)
   {
      label_.setInnerText(value);

      // Give screen reader time to process page to improve chance it will notice the live region
      Timers.singleShot(AriaLiveService.UI_ANNOUNCEMENT_DELAY, () -> live_.setInnerText(value));
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
      return 28;
   }

   public HandlerRegistration addCloseHandler(CloseHandler<WarningBar> handler)
   {
      return addHandler(handler, CloseEvent.getType());
   }
   
   @Override
   public void onDetach()
   {
      events_.fireEvent(new WarningBarClosedEvent());
      super.onDetach();
   }

   @UiField
   SpanElement label_;
   @UiField
   DivElement live_;
   @UiField
   Button moreButton_;
   @UiField
   ImageButton dismiss_;

   private static final Styles styles_ =
         ((Resources) GWT.create(Resources.class)).styles();
   static
   {
      styles_.ensureInjected();
   }
   
   private final EventBus events_;
}
