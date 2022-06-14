/*
 * Presentation2Pane.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.presentation2;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.URIConstants;
import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.quarto.model.QuartoNavigate;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationHashChangeEvent;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationInitEvent;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationInitEvent.Handler;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationSlideChangeEvent;
import org.rstudio.studio.client.workbench.views.presentation2.model.RevealMessage;
import org.rstudio.studio.client.workbench.views.presentation2.model.RevealSlide;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Presentation2Pane extends WorkbenchPane implements Presentation2.Display
{
   @Inject
   public Presentation2Pane(Commands commands, ApplicationServerOperations server)
   {
      // This should always be title "Presentation" (rather than the name of the underlying
      // tab "Presentations". The proper name is "Presentation", we just used
      // "Presentations" so the configurations wouldn't conflict.
      super(constants_.presentationTitle());
      commands_ = commands;
      server_ = server;
      ensureWidget();
      initializeEvents();
      setSecondaryToolbarVisible(false);
   }
   

   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar(constants_.presentationToolbarLabel());
      
      
      toolbar_.addLeftWidget(commands_.presentation2Present().createToolbarButton());
      
      ToolbarPopupMenu presentMenu = new ToolbarPopupMenu();
      presentMenu.addItem(commands_.presentation2PresentFromBeginning().createMenuItem(false));
      ToolbarMenuButton presentButton = new ToolbarMenuButton(ToolbarButton.NoText, constants_.presentTitle(), presentMenu, true);
      presentButton.setEnabled(commands_.presentation2PresentFromBeginning().isEnabled());
      commands_.presentation2PresentFromBeginning().addEnabledChangedHandler(event -> {
         presentButton.setEnabled(commands_.presentation2PresentFromBeginning().isEnabled());
      });
      toolbar_.addLeftWidget(presentButton);
      
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.presentation2Print().createToolbarButton());
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.presentation2Edit().createToolbarButton());
      toolbar_.addLeftSeparator();
      
      ToolbarButton openExternalButton = commands_.presentation2Present().createToolbarButton();
      openExternalButton.setLeftImage(commands_.viewerPopout().getImageResource());
      openExternalButton.setText("");
      toolbar_.addLeftWidget(openExternalButton);
      
      // publish
      publishButton_ = new RSConnectPublishButton(
            RSConnectPublishButton.HOST_PRESENTATION2,
            RSConnect.CONTENT_TYPE_NONE, true, null);
      toolbar_.addRightWidget(publishButton_);

      // refresh
      toolbar_.addRightSeparator();
      toolbar_.addRightWidget(commands_.refreshPresentation2().createToolbarButton());
      
      return toolbar_;
   }
   
   @Override
   protected SecondaryToolbar createSecondaryToolbar()
   {
      SecondaryToolbar toolbar = new SecondaryToolbar(constants_.presentationSlidesToolbarLabel());
      toolbar.addLeftWidget(commands_.presentation2Prev().createToolbarButton());
      toolbar.addLeftWidget(commands_.presentation2Next().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.presentation2Home().createToolbarButton());
      toolbar.addLeftSeparator();
      slidesMenuLabel_ = new Label();
      slidesMenuLabel_.addStyleName(ThemeResources.INSTANCE.themeStyles().presentationNavigatorLabel());
      slidesMenuLabel_.addStyleName(ThemeResources.INSTANCE.themeStyles().toolbarButtonLabel());   
      slidesMenu_ = new SlidesPopupMenu();
      slidesMenuWidget_ = toolbar.addLeftPopupMenu(slidesMenuLabel_, slidesMenu_);
      slidesMenuWidget_.getElement().getStyle().setMarginTop(-3, Unit.PX);
      
      return toolbar;
      
   }

   @Override
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame(constants_.presentationPreviewTitle());
      frame_.addStyleName("ace_editor_theme");
      frame_.setSize("100%", "100%");
      frame_.setUrl(URIConstants.ABOUT_BLANK);
      frame_.getElement().setAttribute("webkitallowfullscreen", "");
      frame_.getElement().setAttribute("mozallowfullscreen", "");
      frame_.getElement().setAttribute("allowfullscreen", "");
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   public void onResize()
   {
      super.onResize();
      
      // sometimes width is 0 (not sure why)
      int width = getOffsetWidth();
      if (width == 0)
         return;
      
      slidesMenuLabel_.getElement().getStyle().setProperty("maxWidth", (width - 130) + "px");
      slidesMenuWidget_.getElement().getStyle().setProperty("maxWidth", (width - 100) + "px");
   
   }
   
   @Override
   public void activate()
   {
      // bring tab to front
      bringToFront();
   }
   
   @Override
   public void setFocus()
   {
      if (frame_ != null && frame_.getIFrame() != null)
         frame_.getIFrame().setFocus();
      else
         super.setFocus();
   }
   
   @Override
   public void navigate(String url, QuartoNavigate nav)
   {
      // in desktop mode we need to be careful about loading URLs which are
      // non-local; before changing the URL, set the iframe to be sandboxed
      // based on whether we're working with a local URL (note that prior to
      // RStudio 1.2 local URLs were forbidden entirely)
      if (Desktop.hasDesktopFrame())
      {
         if (URIUtils.isLocalUrl(url))
            frame_.getElement().removeAttribute("sandbox");
         else
            frame_.getElement().setAttribute("sandbox", "allow-scripts");
      }
      
      // update active presentation target
      activeUrl_ = url; // used for post-message
      
      // captured from init message and played back to reveal frame
      source_ = null;
      origin_ = null;
      
      // compute min height based on normal reveal presentation ratio and current width
      int frameWidth = frame_.getOffsetWidth();
      if (frameWidth > 0)
      {
         double ratio = (double)700 / 1050;
         ensureHeight((int)(frameWidth * ratio) + getToolbarsHeight());
      }
      
      // load content
      frame_.setUrl(activeUrl_);
      
      // set publish button
      if (nav.isWebsite())
         publishButton_.setQuartoSitePreview();
      else
         publishButton_.setQuartoDocPreview(nav.getSourceFile(), nav.getOutputFile());
      
      // redraw toolbar
      toolbar_.invalidateSeparators();      
   }
   
   @Override
   public boolean connected()
   {
      return source_ != null;
   }
   
   @Override
   public void init(JsArray<RevealSlide> slides, int slideIndex)
   {
      // save slides (for creating slide caption)
      activeSlides_ = slides;
      
      // navigate to initial slide
      RevealSlide intialSlide = activeSlides_.get(slideIndex);
      if (intialSlide != null)
         postRevealMessage("slide", intialSlide);   
      
      // populate the menu
      slidesMenu_.clearItems();
      for (int i=0; i<slides.length(); i++)
      {
         // get slide
         final RevealSlide slide = slides.get(i);
          
         // build html
         SafeHtmlBuilder menuHtml = new SafeHtmlBuilder();
         if (slide.getVIndex() > 0)
            menuHtml.appendHtmlConstant("&nbsp;&nbsp;");
         menuHtml.appendEscaped(slideTitle(slide.getTitle())); 
      
         slidesMenu_.addItem(new MenuItem(menuHtml.toSafeHtml(),
                                        new Command() {
            @Override
            public void execute()
            {
               postRevealMessage("slide", slide);
            }
         })); 
      }
      
      setSecondaryToolbarVisible(true);

      // signal reveal that we are ready for more events
      postRevealMessage("ready");
   }

   
   @Override
   public void clear()
   {
      activeUrl_ = null;
      activeSlides_ = null;
      source_ = null;
      origin_ = null;
      publishButton_.setContentType(RSConnect.CONTENT_TYPE_NONE);
      setSecondaryToolbarVisible(false);
      frame_.setUrl("about:blank");
   }
      
   @Override
   public void home()
   {
      postRevealMessage("home");
   }

   @Override
   public void next()
   {
      postRevealMessage("next");
   }


   @Override
   public void prev()
   {
      postRevealMessage("prev");
   }

   @Override
   public void refresh()
   {
      postRevealMessage("refresh");
   }

   
   @Override
   public HandlerRegistration addPresentationInitHandler(Handler handler)
   {
      return handlerManager_.addHandler(PresentationInitEvent.TYPE, handler);
   }

   @Override
   public HandlerRegistration addPresentationSlideChangeHandler(org.rstudio.studio.client.workbench.views.presentation2.events.PresentationSlideChangeEvent.Handler handler)
   {
      return handlerManager_.addHandler(PresentationSlideChangeEvent.TYPE, handler);
   }
   
   @Override
   public HandlerRegistration addPresentationHashChangeHandler(org.rstudio.studio.client.workbench.views.presentation2.events.PresentationHashChangeEvent.Handler handler)
   {
      return handlerManager_.addHandler(PresentationHashChangeEvent.TYPE, handler);
   }
   
   private String slideTitle(String title)
   {
      if (StringUtil.isNullOrEmpty(title))
         title = "(Untitled)";
      return title;
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
         return 350;
      }
   }
   
   private int slideChanged(RevealSlide slide)
   { 
      // determine slide index
      int slideIndex = 0;
      int totalSlides = activeSlides_.length();
      for (int i=0; i<totalSlides; i++)
      {
         if (activeSlides_.get(i).hasSameIndices(slide))
         {
            slideIndex = i;
            break;
         }
      }
      
      // form title (use index if we have one)
      String title = slideTitle(slide.getTitle());
      title = title + " (" + (slideIndex+1) + "/" + totalSlides + ")";
      
      // set title
      slidesMenuLabel_.setText(title);
      
      // return slide index
      return slideIndex;
   }
   
   
   private native void initializeEvents() /*-{  
      var self = this;
      var callback = $entry(function(event) {
         self.@org.rstudio.studio.client.workbench.views.presentation2.Presentation2Pane::onMessage(*)(
            event.source,
            event.origin,
            event.data
         );
      });
      $wnd.addEventListener("message", callback, true);
      
   }-*/;

   private void onMessage(JavaScriptObject source, String origin, RevealMessage message) 
   {    
      // check to see if the message originated from the same origin
      if (activeUrl_ == null || !activeUrl_.startsWith(origin) || source == null || origin== null)
         return;
        
      final String kRevealMessagePrefix = "reveal-";
      if (message.getMessage() != null &&
          message.getMessage().startsWith(kRevealMessagePrefix))
      {
         // record source and origin for sending messages back
         source_ = source;
         origin_ = origin;
         
         String type = message.getMessage().replaceFirst(kRevealMessagePrefix, "");
         if (type == "init")
         {
            handlerManager_.fireEvent(new PresentationInitEvent(message.getData().cast()));
         }
         else if (type == "slidechange")
         {
            PresentationSlideChangeEvent.Data data = message.getData().cast();
            int slideIndex = slideChanged(data.getSlide());
            handlerManager_.fireEvent(new PresentationSlideChangeEvent(
              PresentationSlideChangeEvent.Data.withSlideIndex(data,  slideIndex)
            ));
         }  
         else if (type == "hashchange")
         {
            handlerManager_.fireEvent(new PresentationHashChangeEvent(message.getData().cast()));
         }
      }
    
   }
   
   private void postRevealMessage(String type)
   {
      postRevealMessage(type, null);
   }
   
   private void postRevealMessage(String type, JavaScriptObject data)
   {
      postRevealMessage(source_, origin_, type, data);
   }
   
   private native static void postRevealMessage(JavaScriptObject source, String origin, 
                                                String message, JavaScriptObject data) /*-{
      source.postMessage({
         message: "reveal-" + message,
         data: data
      }, origin);
   }-*/;

   private String activeUrl_ = null;
   private JsArray<RevealSlide> activeSlides_ = null;
   private JavaScriptObject source_ = null;
   private String origin_ = null;
     
   private final Commands commands_;
   private ApplicationServerOperations server_;
   
   private Toolbar toolbar_;
   private Label slidesMenuLabel_;
   private SlidesPopupMenu slidesMenu_;
   private Widget slidesMenuWidget_;
   private RSConnectPublishButton publishButton_;
  
   private RStudioFrame frame_;
  
   private HandlerManager handlerManager_ = new HandlerManager(this);
   private static final Presentation2Constants constants_ = com.google.gwt.core.client.GWT.create(Presentation2Constants.class);
}
