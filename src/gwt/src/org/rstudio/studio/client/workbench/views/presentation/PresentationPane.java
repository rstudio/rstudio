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
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;


import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.FullscreenPopupPanel;
import org.rstudio.core.client.widget.AnchorableFrame;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.core.client.widget.Toolbar;

import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.presentation.Presentation.SlideMenu;

public class PresentationPane extends WorkbenchPane implements Presentation.Display
{
   @Inject
   public PresentationPane(Commands commands, Session session)
   {
      super("Presentation");
      commands_ = commands;
      ensureWidget();
      
      initPresentationCallbacks();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      toolbar.addLeftWidget(commands_.presentationHome().createToolbarButton());
      toolbar.addLeftSeparator();
      menuWidget_ = toolbar.addLeftPopupMenu(titleLabel_, slidesMenu_); 
      getSlideMenu().setDropDownVisible(false);
      
      toolbar.addRightWidget(commands_.presentationFullscreen().createToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.refreshPresentation().createToolbarButton());
        
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
   public void load(String url)
   {   
      frame_.navigate(url);
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
   public void slide(int index)
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
   public SlideMenu getSlideMenu()
   {
      return slideMenu_;
   }

   private SlideMenu slideMenu_ = new SlideMenu() {

      @Override
      public void setCaption(String caption)
      {
         titleLabel_.setText(caption);
      }

      @Override
      public void addItem(MenuItem menu)
      {
         slidesMenu_.addItem(menu);  
      }

      @Override
      public void clear()
      {
         slidesMenu_.clearItems();
      }

      @Override
      public void setDropDownVisible(boolean visible)
      {
         menuWidget_.setVisible(visible);
      }
   };
   
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
   
   private class SlidesPopupMenu extends ScrollableToolbarPopupMenu
   {
      public SlidesPopupMenu()
      {
         addStyleName(ThemeStyles.INSTANCE.statusBarMenu());
      }
      
      @Override
      protected int getMaxHeight()
      {
         return Window.getClientHeight() - titleLabel_.getAbsoluteTop() -
                titleLabel_.getOffsetHeight() - 300;
      }
   }
   
   private Label titleLabel_ = new Label();
   private SlidesPopupMenu slidesMenu_ = new SlidesPopupMenu();
   private Widget menuWidget_;
   private PresentationFrame frame_ ;
   private final Commands commands_;
   
   private FullscreenPopupPanel activeZoomPanel_ = null;
}
