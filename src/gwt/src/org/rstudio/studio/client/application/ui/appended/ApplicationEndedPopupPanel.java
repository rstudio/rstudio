/*
 * ApplicationEndedPopupPanel.java
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
package org.rstudio.studio.client.application.ui.appended;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.CenterPanel;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.studio.client.application.Desktop;

public class ApplicationEndedPopupPanel extends PopupPanel
{
   public static void showQuit()
   {
      asyncShow(QUIT, "", null);
   }
   
   public static void showMultiSessionQuit()
   {
      asyncShow(QUIT_MULTI, "", null);
   }
   
   public static void showSuicide(String reason)
   {
      String description = "<p>R encountered a fatal error.";
      if (reason.length() > 0)
         description += ": " + reason;
      description += "</p>The session was terminated.";
      asyncShow(SUICIDE, description, null);
   }
   
   public static void showDisconnected()
   {
      String description =
         "This browser was disconnected from the R session because another " +
         "browser connected (only one browser at a time may be connected " +
         "to an RStudio session). You may reconnect using the button below.";

      asyncShow(DISCONNECTED, description, null);
   }
   
   public static void showOffline()
   {
      String description = 
         "RStudio is temporarily offline due to system maintenance. We " +
         "apologize for the inconvenience, please try again in a few minutes.";
      
      asyncShow(OFFLINE, description, null);
   }

   public static void prefetch(Command continuation)
   {
      asyncShow(PREFETCH, null, continuation);
   }

   private static void asyncShow(final int mode,
                                 final String description,
                                 final Command continuation)
   {
      GWT.runAsync(new RunAsyncCallback()
      {
         public void onFailure(Throwable reason)
         {
            Window.alert(description);

            if (continuation != null)
               continuation.execute();
         }

         public void onSuccess()
         {
            if (mode == PREFETCH)
               return;
            new ApplicationEndedPopupPanel(mode, description).center();

            if (continuation != null)
               continuation.execute();
         }
      });
   }

   private static final int PREFETCH = 0 ;
   private static final int QUIT = 1 ;
   private static final int SUICIDE = 2;
   private static final int DISCONNECTED = 3;
   private static final int OFFLINE = 4;
   private static final int QUIT_MULTI = 5;
   
   private ApplicationEndedPopupPanel(int mode, String description)
   {
      super(false, false);
      setStylePrimaryName(RESOURCES.styles().applicationEndedPopupPanel());
      setGlassEnabled(true);
      setGlassStyleName(RESOURCES.styles().glass());
      
      // main panel
      HorizontalPanel horizontalPanel = new HorizontalPanel(); 
      horizontalPanel.setSpacing(10);
      
      // create widgets and make mode dependent customizations
      Image image;
      Label captionLabel = new Label();
      captionLabel.setStylePrimaryName(RESOURCES.styles().captionLabel());
      final FancyButton button = new FancyButton();
      button.addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            reloadApplication();
         } 
      });
      FocusHelper.setFocusDeferred(button);
      
      switch(mode)
      {
      case QUIT:
         image = new Image(new ImageResource2x(RESOURCES.applicationQuit2x()));
         captionLabel.setText("R Session Ended");
         button.setText("Start New Session");
         break;
         
      case SUICIDE:
         image = new Image(new ImageResource2x(RESOURCES.applicationSuicide2x()));
         captionLabel.setText("R Session Aborted");
         button.setText("Start New Session");
         break;
       
      case DISCONNECTED:
         image = new Image(new ImageResource2x(RESOURCES.applicationDisconnected2x()));
         captionLabel.setText("R Session Disconnected");
         button.setText("Reconnect");
         break;
         
      case OFFLINE:
         image = new Image(new ImageResource2x(RESOURCES.applicationOffline2x()));
         captionLabel.setText("RStudio Temporarily Offline");
         button.setText("Reconnect");
         break;
         
      case QUIT_MULTI:
         image = new Image(new ImageResource2x(RESOURCES.applicationQuit2x()));
         captionLabel.setText("R Session Ended");
         button.setText("Reconnect");
         break;
      
      default:
         throw new IllegalArgumentException("Unknown mode " + mode);
      }
      
      // add image
      horizontalPanel.add(image);
 
      // captions and button
      VerticalPanel contentPanel = new VerticalPanel();
      contentPanel.setStylePrimaryName(RESOURCES.styles().contentPanel());
      contentPanel.add(captionLabel);
      HTML descriptionLabel = new HTML(description);
      descriptionLabel.setStylePrimaryName(RESOURCES.styles().descriptionLabel());
      contentPanel.add(descriptionLabel);
      contentPanel.add(button);
      horizontalPanel.add(contentPanel);
      
      // center the horizontal panel within the popup 
      CenterPanel mainPanel = new CenterPanel(horizontalPanel);
      mainPanel.setStylePrimaryName(RESOURCES.styles().mainPanel());

      setWidget(((MyUiBinder)GWT.create(MyUiBinder.class)).createAndBindUi(this));
      content_.setWidget(mainPanel);
   }
   
   @Override
   public void onPreviewNativeEvent(Event.NativePreviewEvent event)
   {
      if (event.getTypeInt() == Event.ONKEYDOWN)
      {
         NativeEvent nativeEvent = event.getNativeEvent();
         switch (nativeEvent.getKeyCode())
         {
            case KeyCodes.KEY_ENTER:
               
               nativeEvent.preventDefault();
               nativeEvent.stopPropagation();
               reloadApplication();    
               break;
         } 
      }
   }
   
   private void reloadApplication()
   {
      if (Desktop.isDesktop())
      {
         Desktop.getFrame().launchSession(true);
      }
      else
      {
         Window.Location.reload();
      }
   }
      
   static interface Styles extends CssResource
   {
      String applicationEndedPopupPanel();
      String glass();
      String mainPanel();
      String contentPanel();
      String captionLabel();
      String descriptionLabel();

      String NW();
      String N();
      String NE();
      String W();
      String C();
      String E();
      String SW();
      String S();
      String SE();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("ApplicationEndedPopupPanel.css")
      Styles styles();
      
      @Source("applicationQuit_2x.png")
      ImageResource applicationQuit2x();

      @Source("applicationSuicide_2x.png")
      ImageResource applicationSuicide2x();

      @Source("applicationDisconnected_2x.png")
      ImageResource applicationDisconnected2x();

      @Source("applicationOffline_2x.png")
      ImageResource applicationOffline2x();

      ImageResource panelTopLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource panelTop();
      ImageResource panelTopRight();

      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource panelLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource panelRight();

      ImageResource panelBottomLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource panelBottom();
      ImageResource panelBottomRight();
   }

   interface MyUiBinder extends UiBinder<Widget, ApplicationEndedPopupPanel> {}
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }

   @UiField
   SimplePanel content_;
}
