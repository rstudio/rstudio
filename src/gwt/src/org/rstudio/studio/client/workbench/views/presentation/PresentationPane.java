/*
 * PresentationPane.java
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
package org.rstudio.studio.client.workbench.views.presentation;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.FullscreenPopupPanel;
import org.rstudio.core.client.widget.AnchorableFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.presentation.SlideNavigationMenu;
import org.rstudio.studio.client.common.presentation.SlideNavigationToolbarMenu;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationRPubsSource;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationServerOperations;

public class PresentationPane extends WorkbenchPane implements Presentation.Display
{
   @Inject
   public PresentationPane(Commands commands, Session session,
         PresentationServerOperations server, GlobalDisplay display)
   {
      super("Presentation");
      commands_ = commands;
      session_ = session;
      server_ = server;
      display_ = display;
      ensureWidget();
      
      initPresentationCallbacks();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      boolean isTutorial =
                 session_.getSessionInfo().getPresentationState().isTutorial();
      
      Toolbar toolbar = new Toolbar();
      
      slideNavigationMenu_ = new SlideNavigationToolbarMenu(toolbar); 
      slideNavigationMenu_.setEditButtonVisible(!isTutorial);
      
      toolbar.addLeftSeparator();
      
      toolbar.addLeftWidget(commands_.presentationFullscreen().createToolbarButton());
     
      // More
      if (!isTutorial)
      { 
         ToolbarPopupMenu moreMenu = new ToolbarPopupMenu();
         moreMenu.addItem(commands_.clearPresentationCache().createMenuItem(false));
         moreMenu.addSeparator();
         moreMenu.addItem(commands_.presentationViewInBrowser().createMenuItem(false));
         moreMenu.addItem(commands_.presentationSaveAsStandalone().createMenuItem(false));
         
         ToolbarButton moreButton = new ToolbarButton("More",
                                                      new ImageResource2x(
                                                         StandardIcons.INSTANCE.more_actions2x()),
                                                      moreMenu);

         toolbar.addRightWidget(moreButton);

         // Create the publish button and wire it to our HTML generator
         publishButton_ = new RSConnectPublishButton(
               RSConnect.CONTENT_TYPE_PRES, false, null);
         publishButton_.setPublishHtmlSource(new PublishHtmlSource()
         {
            @Override
            public void generatePublishHtml(
                  final CommandWithArg<String> onCompleted)
            {
               server_.createPresentationRPubsSource(
                  new SimpleRequestCallback<PresentationRPubsSource>() {
                     
                     @Override
                     public void onResponseReceived(
                           PresentationRPubsSource source)
                     {
                        onCompleted.execute(source.getSourceFilePath());
                     }
                     
                     @Override
                     public void onError(ServerError error)
                     {
                        display_.showErrorMessage("Error Saving Presentation",
                          Presentation.getErrorMessage(error));
                     }
               });
            }

            @Override
            public String getTitle()
            {
               return "Presentation:\n" + getPresentationTitle();
            }
         });
         toolbar.addRightSeparator();
         toolbar.addRightWidget(publishButton_);
      }
      else
      {
         toolbar.addRightWidget(commands_.tutorialFeedback().createToolbarButton());
      }
      
      toolbar.addRightSeparator();
      toolbar.addRightWidget(refreshButton_ = 
                  commands_.refreshPresentation().createToolbarButton());
      progressButton_ = new ToolbarButton(
                              CoreResources.INSTANCE.progress_gray(),
                              new ClickHandler() {
                                 @Override
                                 public void onClick(ClickEvent e)
                                 {
                                 }
                              });
      toolbar.addRightWidget(progressButton_);
      progressButton_.setVisible(false);
        
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {  
      frame_ = new PresentationFrame(false) ;
      frame_.setUrl("about:blank");
      frame_.setSize("100%", "100%");
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   public void load(String url, String sourceFile)
   {   
      frame_.navigate(url);
      if (publishButton_ != null)
      {
         publishButton_.setContentPath(sourceFile, null);
      }
   }
   
   @Override
   public void zoom(String title, String url, final Command onClosed)
   {
      // create the titlebar (no title for now)
      HorizontalPanel titlePanel = new HorizontalPanel();
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      Label titleLabel = new Label(title);
      titleLabel.addStyleName(styles.fullscreenCaptionLabel());
      titlePanel.add(titleLabel);
      
      // create the frame
      AnchorableFrame frame = new PresentationFrame(true);
      frame.setSize("100%", "100%");
      
      // create the popup panel & add close handler 
      activeZoomPanel_ = new FullscreenPopupPanel(titlePanel, frame, false);
      activeZoomPanel_.addCloseHandler(new CloseHandler<PopupPanel>() {
         @Override
         public void onClose(CloseEvent<PopupPanel> event)
         {
            activeZoomPanel_ = null;
            onClosed.execute();
         }
      });
      
      // load the frame and show the zoom panel
      frame.navigate(url);
      activeZoomPanel_.center();  
   }
   
   @Override
   public void clear()
   {
      frame_.clear();
   }
   
   @Override
   public boolean hasSlides()
   {
      String href = frame_.getWindow().getLocationHref();
      return !"about:blank".equals(href) &&
             !"javascript:void(0)".equals(href);
   }
   
   @Override
   public void pauseMedia()
   {
      pausePlayers(frame_.getWindow());
   }
   
   private static final native void pausePlayers(WindowEx window) /*-{
      window.pauseAllPlayers();
   }-*/;
   
   @Override
   public void home()
   {
      frame_.home();
   }
   
   @Override
   public void navigate(int index)
   {
      frame_.slide(index);
   }
   
   @Override
   public void next()
   {
      frame_.next();
   }
   
   @Override
   public void prev()
   {
      frame_.prev();
   }
   
   @Override
   public SlideNavigationMenu getNavigationMenu()
   {
      return slideNavigationMenu_;
   }
   
   @Override
   public String getPresentationTitle()
   {
      return frame_.getFrameTitle();
   }
   
   @Override
   public void showBusy()
   {
      busyPending_ = true;
      
      new Timer() {
         @Override
         public void run()
         {
            if (busyPending_)
            {
               refreshButton_.setVisible(false);
               progressButton_.setVisible(true);
               busyPending_ = false;
            }
         }
      }.schedule(750);
   }
   
   @Override
   public void hideBusy()
   {
      busyPending_ = false;
      refreshButton_.setVisible(true);
      progressButton_.setVisible(false);
   }
   
   
   private final native void initPresentationCallbacks() /*-{
      var thiz = this;
      $wnd.presentationKeydown = $entry(function(e) {
         thiz.@org.rstudio.studio.client.workbench.views.presentation.PresentationPane::handleKeyDown(Lcom/google/gwt/dom/client/NativeEvent;)(e);
      });
   }-*/;

   private void handleKeyDown(NativeEvent e)
   {  
      // get the event
      NativeKeyDownEvent evt = new NativeKeyDownEvent(e);
      
      // if there is a zoom panel then ignore other shortcuts
      // (only handle Esc)
      if (activeZoomPanel_ != null)
      {
         if (e.getKeyCode() == KeyCodes.KEY_ESCAPE)
         {
            e.preventDefault();
            e.stopPropagation();
            activeZoomPanel_.close();
         }
      }
      else
      {
         ShortcutManager.INSTANCE.onKeyDown(evt);
         if (evt.isCanceled())
         {
            e.preventDefault();
            e.stopPropagation();
            
            // since this is a shortcut handled by the main window
            // we set focus to it
            WindowEx.get().focus();
         } 
      }
   }
   
   private SlideNavigationToolbarMenu slideNavigationMenu_;
   private ToolbarButton refreshButton_;
   private ToolbarButton progressButton_;
   private RSConnectPublishButton publishButton_;
   private boolean busyPending_ = false;
   private PresentationFrame frame_ ;
   private final Commands commands_;
   private final Session session_;
   private final PresentationServerOperations server_;
   private final GlobalDisplay display_;
   
   private FullscreenPopupPanel activeZoomPanel_ = null;
}
