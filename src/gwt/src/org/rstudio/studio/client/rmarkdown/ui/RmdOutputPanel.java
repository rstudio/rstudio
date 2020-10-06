/*
 * RmdOutputPanel.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.ScrollUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.AnchorableFrame;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FindTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.shiny.ShinyFrameHelper;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.presentation.SlideNavigationMenu;
import org.rstudio.studio.client.common.presentation.SlideNavigationToolbarMenu;
import org.rstudio.studio.client.common.presentation.events.SlideIndexChangedEvent;
import org.rstudio.studio.client.common.presentation.events.SlideNavigationChangedEvent;
import org.rstudio.studio.client.common.presentation.events.SlideNavigationChangedEvent.Handler;
import org.rstudio.studio.client.common.presentation.model.SlideNavigation;
import org.rstudio.studio.client.common.satellite.Satellite;

public class RmdOutputPanel extends SatelliteFramePanel<AnchorableFrame>
                            implements RmdOutputPresenter.Display
{
   @Inject
   public RmdOutputPanel(Commands commands, 
                         FileTypeRegistry fileTypeRegistry,
                         RMarkdownServerOperations server,
                         RSConnect rsconnect,
                         Satellite satellite)
   {
      super(commands);
      fileTypeRegistry_ = fileTypeRegistry;
      server_ = server;
      shinyFrame_ = new ShinyFrameHelper();
      
      // if this window is a satellite, ensure that the rsconnect instance
      // is initialized
      rsconnect.ensureSessionInit();
   }
   
   @Override
   public void showOutput(RmdPreviewParams params, boolean enablePublish, 
                          boolean refresh)
   {
      // remember output parameters
      outputParms_ = params;

      // remember target file (for invoking editor)
      targetFile_ = FileSystemItem.createFile(params.getTargetFile());
      
      // slide navigation (may be null)
      slideNavigation_ = params.getResult().getSlideNavigation();
      handlerManager_.fireEvent(new SlideNavigationChangedEvent(slideNavigation_));
      slideChangeMonitor_.cancel();
      
      // file label
      if (params.isShinyDocument())
      {
         fileLabel_.setVisible(false);
         fileLabelSeparator_.setVisible(false);
         shinyUrl_ = DomUtils.makeAbsoluteUrl(params.getOutputUrl());
         isShiny_ = true;
      }
      else
      {
         fileLabel_.setVisible(true);
         fileLabelSeparator_.setVisible(true);
         fileLabel_.setText(FileSystemItem.createFile(
                                          params.getOutputFile()).getName());
         isShiny_ = false;
      }
      

      publishButton_.setRmdPreview(params);
      
      // find text box
      boolean showFind = params.getResult().isHtml() && 
                         !params.getResult().isHtmlPresentation();
      findTextBox_.setVisible(showFind);
      findSeparator_.setVisible(showFind);
      
      // when refreshing, reapply the current scroll position and anchor
      scrollPosition_ = refresh ? 
            getScrollPosition() : params.getScrollPosition();
     
      // get the URL to load
      String url = getDocumentUrl();

      // restore anchor if necessary
      if (params.getResult().getRestoreAnchor())
      {
         String anchor = "";
         if (params.getResult().getPreviewSlide() > 0)
            anchor = String.valueOf(params.getResult().getPreviewSlide());
               
         // check for an explicit anchor if there wasn't one implied
         // by the preview_slide
         if (anchor.length() == 0)
            anchor = params.getAnchor();
         
         // add the anchor if necessary
         if (anchor.length() > 0)
            url += "#" + anchor;
      }
      
      showUrl(url);
   }
   
   @Override
   protected void initToolbar (Toolbar toolbar, Commands commands)
   {
      slideNavigationMenu_ = new SlideNavigationToolbarMenu(toolbar, 
                                                            400, 
                                                            100,
                                                            true);
      
      fileLabel_ = new ToolbarLabel();
      fileLabel_.addStyleName(ThemeStyles.INSTANCE.subtitle());
      fileLabel_.getElement().getStyle().setMarginRight(7, Unit.PX);
      toolbar.addLeftWidget(fileLabel_);
      fileLabelSeparator_ = toolbar.addLeftSeparator();
      ToolbarButton popoutButton = 
            commands.viewerPopout().createToolbarButton();
      popoutButton.setText("Open in Browser");
      toolbar.addLeftWidget(popoutButton);

      findTextBox_ = new FindTextBox("Find");
      findTextBox_.setIconVisible(true);
      findTextBox_.setOverrideWidth(120);
      findTextBox_.getElement().getStyle().setMarginRight(6, Unit.PX);
      findSeparator_ = toolbar.addLeftSeparator();
      toolbar.addLeftWidget(findTextBox_);
      
      findTextBox_.addKeyDownHandler(new KeyDownHandler() {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            // enter key triggers a find
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
            {
               event.preventDefault();
               event.stopPropagation();
               findInTopic(findTextBox_.getValue().trim(), findTextBox_);
               findTextBox_.focus();
            }
            else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
            {
               findTextBox_.setValue("");
            }       
         }
         
         private void findInTopic(String term, CanFocus findInputSource)
         {
            // get content window
            WindowEx contentWindow = getFrame().getWindow();
            if (contentWindow == null)
               return;
                
            if (!contentWindow.find(term, false, false, true, false))
            {
               RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
                     MessageDialog.INFO,
                     "Find in Page", 
                     "No occurrences found",
                     findInputSource);
            }     
         }
         
      });
      
      toolbar.addLeftSeparator();
      
      
      publishButton_ = new RSConnectPublishButton(
            RSConnectPublishButton.HOST_RMD_OUTPUT,
            RSConnect.CONTENT_TYPE_DOCUMENT, true, null);
      toolbar.addRightWidget(publishButton_);
      
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands.viewerRefresh().createToolbarButton());
   }
   
   @Override
   protected AnchorableFrame createFrame(String url)
   {
      AnchorableFrame frame = new AnchorableFrame("Rmd Output Panel");
      
      // allow full screen
      Element el = frame.getElement();
      el.setAttribute("webkitallowfullscreen", "");
      el.setAttribute("mozallowfullscreen", "");
      el.setAttribute("allowfullscreen", "");
      
      frame.navigate(url);
      
      final Operation initSlides = new Operation()
      {
         @Override
         public void execute()
         {
            if (getNavigationMenu().isVisible())
            {  
               fireSlideIndexChanged();
               slideChangeMonitor_.scheduleRepeating(100);
            }
         }
      };

      if (isShiny_)
      {
         shinyFrame_.initialize(url, new Operation() 
         {
            @Override
            public void execute()
            {
               shinyFrame_.setScrollPosition(scrollPosition_);
               initSlides.execute();
            }
         });
      }
      else
      {
         // poll for document availability then perform initialization
         // tasks once it's available (addLoadHandler wasn't always 
         // getting called at least under Cocoa WebKit)
         Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

            @Override
            public boolean execute()
            {
               // see if the document is ready
               AnchorableFrame frame = getFrame();
               if (frame == null)
                  return true;
               
               IFrameElementEx iframe = frame.getIFrame();
               if (iframe == null)
                  return true;
               
               Document doc = iframe.getContentDocument();
               if (doc == null)
                  return true;

               initSlides.execute();
               
               // Even though the document exists, it may not have rendered all
               // its content yet
               ScrollUtil.setScrollPositionOnLoad(frame, scrollPosition_);

               return false;
            }
            
         }, 50);
      }
       
      return frame;
   }
   
   private Timer slideChangeMonitor_ = new Timer() {

      @Override
      public void run()
      {
         String url = getCurrentUrl();
         if (url != lastUrl_)
         {
            lastUrl_ = url;
            fireSlideIndexChanged();
         }
      }
      
      private String lastUrl_ = null;  
   };
   
   @Override
   public void refresh()
   {
      // cache the scroll position, so we can re-apply it when the page loads
      scrollPosition_ = getScrollPosition();
      
      // recompute the URL (we can't pick up the URL from the document since 
      // it may have encoding problems--see case 3994)
      String url = getDocumentUrl();
      
      // restore anchor if necessary
      if (outputParms_ != null && outputParms_.getResult().getRestoreAnchor())
      {
         String anchor = getAnchor();
         if (anchor.length() > 0)
            url += "#" + anchor;
      }
      
      // re-initialize the shiny frame with the URL (so it waits for the new 
      // window object to become available after refresh)
      if (isShiny_)
         shinyFrame_.initialize(url, null);

      showUrl(url);
   }

   
   @Override
   public void focusFind()
   {
      findTextBox_.focus();
   }

   @Override
   public int getScrollPosition()
   {
      // if we're asking for this at document creation/destruction time
      // it can sometimes have a null ref somewhere in the call chain,
      // in this case return 0
      try
      {
         if (isShiny_)
            return shinyFrame_.getScrollPosition();
         else
            return getFrame().getIFrame().getContentDocument().getScrollTop();
      }
      catch(Exception ex)
      {
         return 0;
      }
   }
   
   @Override
   public String getTitle()
   {
      return title_;
   }
   
   @Override
   public String getName()
   {
      return shinyFrame_.getWindow().getName();
   }
   
   @Override
   public String getAnchor()
   {
      String url = getCurrentUrl();
      int anchorPos = url.lastIndexOf("#");
      return anchorPos > 0 ? url.substring(anchorPos + 1) : "";
   }
   
   @Override
   public void navigate(int index)
   {
      getFrame().getIFrame().focus();
      String hash = getSlideAnchor(index + 1);

      if (isShiny_)
         shinyFrame_.setHash(hash);
      else
         showUrl(getDocumentUrl() + "#" + hash);
   }

   @Override
   public void editCurrentSlide()
   {
      if (targetFile_ != null && slideNavigation_ != null)
      {
         // determine what slide we are on
         int index = getCurrentSlideIndex();
         
         // get the line of code associated with it
         int line = slideNavigation_.getItems().get(index).getLine();
         
         // invoke the editor
         fileTypeRegistry_.editFile(targetFile_, FilePosition.create(line, 1));
      }
   }
   
   @Override
   public SlideNavigationMenu getNavigationMenu()
   {
      return slideNavigationMenu_;
   }

   @Override
   public HandlerRegistration addSlideNavigationChangedHandler(Handler handler)
   {
      return handlerManager_.addHandler(SlideNavigationChangedEvent.TYPE, 
                                        handler);
   }
   
   @Override
   public HandlerRegistration addSlideIndexChangedHandler(
                                 SlideIndexChangedEvent.Handler handler)
   {
      return handlerManager_.addHandler(SlideIndexChangedEvent.TYPE, handler);
   }
   
   // the current URL is the one currently showing in the frame, which may 
   // reflect navigation occurring after initial load (e.g. anchor changes)
   private String getCurrentUrl()
   {
      String url = "";
      // guard against exceptions (can occur if the content document isn't 
      // fully created yet or we don't have access to it)
      try 
      {
         if (isShiny_)
            url = shinyFrame_.getUrl();
         else
            url = getFrame().getIFrame().getContentDocument().getURL();
      }
      catch (Exception ex)
      {
      }
      return url;
   }
  
   private void fireSlideIndexChanged()
   {
      handlerManager_.fireEvent(new SlideIndexChangedEvent(
                                                   getCurrentSlideIndex())); 
   }
   
   private int getCurrentSlideIndex()
   {
      try
      {
         String anchor = getAnchor();
         if (anchor.length() == 0)
            anchor = "1";
         
         // remove parens if necessary
         anchor = anchor.replaceAll("[()]","");
         
         // return index
         return Integer.parseInt(anchor) - 1;
      }
      catch(NumberFormatException e)
      {
         return 0;
      }
   }
   
   private String getDocumentUrl() 
   {
      return isShiny_ ?
            shinyUrl_ :
            server_.getApplicationURL(outputParms_.getOutputUrl());
   }
   
   private String getSlideAnchor(int index)
   {
      if (slideNavigation_.getUseAnchorParens())
         return "(" + index + ")"; 
      else
         return "" + index;
   }
   
   private SlideNavigationToolbarMenu slideNavigationMenu_;

   private Label fileLabel_;
   private Widget fileLabelSeparator_;
   private RSConnectPublishButton publishButton_;
   private String title_;
   
   private final FileTypeRegistry fileTypeRegistry_;
   private final RMarkdownServerOperations server_;

   private int scrollPosition_ = 0;
   
   private FileSystemItem targetFile_ = null;
   private SlideNavigation slideNavigation_ = null;
   private RmdPreviewParams outputParms_ = null;
   
   private FindTextBox findTextBox_;
   private Widget findSeparator_;

   private boolean isShiny_;
   private String shinyUrl_;
   private ShinyFrameHelper shinyFrame_;
   
   private HandlerManager handlerManager_ = new HandlerManager(this);
}
