/*
 * HelpPane.java
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

package org.rstudio.studio.client.workbench.views.help;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FindTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.help.Help.LinkMenu;
import org.rstudio.studio.client.workbench.views.help.events.HelpNavigateEvent;
import org.rstudio.studio.client.workbench.views.help.events.HelpNavigateHandler;
import org.rstudio.studio.client.workbench.views.help.model.VirtualHistory;
import org.rstudio.studio.client.workbench.views.help.search.HelpSearch;

public class HelpPane extends WorkbenchPane 
                      implements Help.Display
{
   @Inject
   public HelpPane(Provider<HelpSearch> searchProvider,
                   GlobalDisplay globalDisplay,
                   Commands commands)
   {
      super("Help") ;
      
      searchProvider_ = searchProvider ;
      globalDisplay_ = globalDisplay;
      commands_ = commands;
    
      MenuItem clear = commands.clearHelpHistory().createMenuItem(false);
      history_ = new ToolbarLinkMenu(12, true, null, new MenuItem[] { clear }) ;

      Window.addResizeHandler(new ResizeHandler()
      {
         public void onResize(ResizeEvent event)
         {
            history_.getMenu().hide();
         }
      });

      ensureWidget();
   }

   @Override 
   protected Widget createMainWidget()
   {
      frame_ = new Frame() ;
      frame_.setSize("100%", "100%");
      frame_.setStylePrimaryName("rstudio-HelpFrame") ;

      return new AutoGlassPanel(frame_);
   }

   @Override
   public void onResize()
   {
      manageTitleLabelMaxSize();

      super.onResize();
   }

   private void manageTitleLabelMaxSize()
   {
      if (title_ != null)
      {
         int offsetWidth = getOffsetWidth();
         if (offsetWidth > 0)
         {
            int newWidth = offsetWidth - 25;
            if (newWidth > 0)
               title_.getElement().getStyle().setPropertyPx("maxWidth", newWidth);
         }
      }
   }

   @Override
   protected void onLoad()
   {
      super.onLoad() ;

      if (!initialized_)
      {
         initialized_ = true;

         initHelpCallbacks() ;

         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            public void execute()
            {
               manageTitleLabelMaxSize();
            }
         });
      }
   }
   
   public final native void initHelpCallbacks() /*-{
      function addEventHandler(subject, eventName, handler) {
         if (subject.addEventListener) {
            subject.addEventListener(eventName, handler, false);
         }
         else {
            subject.attachEvent(eventName, handler);
         }
      }

      var thiz = this ;
      $wnd.helpNavigated = function(document, win) {
         thiz.@org.rstudio.studio.client.workbench.views.help.HelpPane::helpNavigated(Lcom/google/gwt/dom/client/Document;)(document);
         addEventHandler(win, "unload", function () {
            thiz.@org.rstudio.studio.client.workbench.views.help.HelpPane::unload()();
         });
      } ;
      $wnd.helpNavigate = function(url) {
         thiz.@org.rstudio.studio.client.workbench.views.help.HelpPane::showHelp(Ljava/lang/String;)(url);
      } ;
      
      $wnd.helpKeydown = function(e) {
         thiz.@org.rstudio.studio.client.workbench.views.help.HelpPane::handleKeyDown(Lcom/google/gwt/dom/client/NativeEvent;)(e);
      } ;
   }-*/;
   
   
   
   // delegate shortcuts which occur while Help has focus
    
   private void handleKeyDown(NativeEvent e)
   { 
      // determine whether this key-combination means we should focus find
      int mod = KeyboardShortcut.getModifierValue(e);
      if ((mod == (BrowseCap.hasMetaKey() ? KeyboardShortcut.META
                                          : KeyboardShortcut.CTRL)) &&
           e.getKeyCode() == 'F')
      {
         e.preventDefault();
         e.stopPropagation();
         WindowEx.get().focus();
         findTextBox_.focus();
         findTextBox_.selectAll();
      }
      
      // delegate to the shortcut manager
      else
      {
         NativeKeyDownEvent evt = new NativeKeyDownEvent(e);
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
   
   private void helpNavigated(Document doc)
   {
      NodeList<Element> elements = doc.getElementsByTagName("a") ;
      for (int i = 0; i < elements.getLength(); i++)
      {
         ElementEx a = (ElementEx) elements.getItem(i) ;
         String href = a.getAttribute("href", 2) ;
         if (href == null)
            continue ;

         if (href.contains(":") || href.endsWith(".pdf"))
         {
            // external links
            ((AnchorElement)a.cast()).setTarget("_blank") ;
         }
         else
         {
            // Internal links need to be handled in JavaScript so that
            // they can participate in virtual session history. This
            // won't have any effect for right-click > Show in New Window
            // but that's a good thing.
            a.setAttribute("onclick",
                           "window.parent.helpNavigate(this.href);return false") ;
         }
      }
      
      
      String effectiveTitle = getDocTitle(doc);
      title_.setText(effectiveTitle) ;
      this.fireEvent(new HelpNavigateEvent(doc.getURL(), effectiveTitle)) ;
   }
   
   private String getDocTitle(Document doc)
   {
      String docUrl = StringUtil.notNull(doc.getURL());
      String docTitle = doc.getTitle();
      
      String previewPrefix = new String("/help/preview?file=");
      int previewLoc = docUrl.indexOf(previewPrefix);
      if (previewLoc != -1)
      {
         String file = docUrl.substring(previewLoc + previewPrefix.length());
         file = URL.decodeQueryString(file);
         FileSystemItem fsi = FileSystemItem.createFile(file);
         docTitle = fsi.getName();
      }
      else if (StringUtil.isNullOrEmpty(docTitle))
      {
         String url = new String(docUrl); 
         url = url.split("\\?")[0];
         url = url.split("#")[0];
         String[] chunks = url.split("/");
         docTitle = chunks[chunks.length - 1];
      }
      
      return docTitle;
   }

   private void unload()
   {
      title_.setText("") ;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();

      toolbar.addLeftWidget(commands_.helpBack().createToolbarButton());
      toolbar.addLeftWidget(commands_.helpForward().createToolbarButton());
      toolbar.addLeftWidget(commands_.helpHome().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.printHelp().createToolbarButton());
      toolbar.addLeftWidget(commands_.helpPopout().createToolbarButton());
      
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.refreshHelp().createToolbarButton());
      
      toolbar.addRightWidget(searchProvider_.get().getSearchWidget());

      return toolbar;
   }
   
   @Override
   protected SecondaryToolbar createSecondaryToolbar()
   {
      SecondaryToolbar toolbar = new SecondaryToolbar() ;
      toolbar.addLeftPopupMenu(title_ = new Label(), history_.getMenu());
      
      if (isFindSupported())
      {
         final SmallButton btnNext = new SmallButton("&gt;", true);
         btnNext.setTitle("Find next (Enter)");
         btnNext.setVisible(false);
         btnNext.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               findNext();
            }  
         });
         
         final SmallButton btnPrev = new SmallButton("&lt;", true);
         btnPrev.setTitle("Find previous");
         btnPrev.setVisible(false);
         btnPrev.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               findPrev();
            }  
         });
         
         
         findTextBox_ = new FindTextBox("Find in Topic");
         findTextBox_.setOverrideWidth(90);
         toolbar.addLeftWidget(findTextBox_);
         findTextBox_.addKeyUpHandler(new KeyUpHandler() { 
            
            @Override
            public void onKeyUp(KeyUpEvent event)
            {     
               WindowEx contentWindow = getContentWindow();
               if (contentWindow != null)
               {
                  // escape or tab means exit find mode and put focus 
                  // into the main content window
                  if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE ||
                      event.getNativeKeyCode() == KeyCodes.KEY_TAB)
                  {
                     event.preventDefault();
                     event.stopPropagation();
                     if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
                        clearTerm();
                     contentWindow.focus();
                  }
                  else
                  {
                     // prevent two enter keys in rapid succession from 
                     // minimizing or maximizing the help pane
                     if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
                     {
                        event.preventDefault();
                        event.stopPropagation();
                     }
                      
                     // check for term
                     String term = findTextBox_.getValue().trim();
                     
                     // if there is a term then search for it
                     if (term.length() > 0)
                     { 
                        // make buttons visible
                        setButtonVisibility(true);
                        
                        // perform the find (check for incremental)
                        if (isIncrementalFindSupported())
                        {
                           boolean incremental = 
                            !event.isAnyModifierKeyDown() &&
                            (event.getNativeKeyCode() != KeyCodes.KEY_ENTER);   
                           
                           performFind(term, true, incremental);
                        }
                        else
                        {
                           if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
                              performFind(term, true, false);
                        }
                     }
                     
                     // no term means clear term and remove selection
                     else
                     {
                        if (isIncrementalFindSupported())
                        {
                           clearTerm();
                           contentWindow.removeSelection();
                        }
                     }
                  }
               }
            }
            
            private void clearTerm()
            {
               findTextBox_.setValue("");
               setButtonVisibility(false);
            }
            
            private void setButtonVisibility(final boolean visible)
            {
               Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                  @Override
                  public void execute()
                  {
                     btnNext.setVisible(visible);
                     btnPrev.setVisible(visible);
                  }
               });
            }
         });
       
         findTextBox_.addKeyDownHandler(new KeyDownHandler() {

            @Override
            public void onKeyDown(KeyDownEvent event)
            { 
               // we handle these directly so prevent the browser
               // from handling them
               if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE ||
                   event.getNativeKeyCode() == KeyCodes.KEY_TAB ||
                   event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
               {
                  event.preventDefault();
                  event.stopPropagation();
               }
            }
            
         });
         
         if (isIncrementalFindSupported())
         {
            btnPrev.getElement().getStyle().setMarginRight(3, Unit.PX);
            toolbar.addLeftWidget(btnPrev);
            toolbar.addLeftWidget(btnNext);
         }
       
      }

      return toolbar ;
   }
   
   private String getTerm()
   {
      return findTextBox_.getValue().trim();
   }
   
   private void findNext()
   {
      String term = getTerm();
      if (term.length() > 0)
         performFind(term, true, false);
   }
   
   private void findPrev()
   {
      String term = getTerm();
      if (term.length() > 0)
         performFind(term, false, false);
   }
   
   private void performFind(String term,
                            boolean forwards, 
                            boolean incremental)
   {
      WindowEx contentWindow = getContentWindow();
      if (contentWindow == null)
         return;
      
      // if this is an incremental search then reset the selection first
      if (incremental)
         contentWindow.removeSelection();
      
      contentWindow.find(term, false, !forwards, true, false);
   }
   
   private boolean isFindSupported()
   {
      return BrowseCap.INSTANCE.hasWindowFind();
   }
   
   // Firefox changes focus during our typeahead search (it must take
   // focus when you set the selection into the iframe) which breaks 
   // typeahead entirely. rather than code around this we simply
   // disable it for Firefox
   private boolean isIncrementalFindSupported()
   {
      return isFindSupported() && !BrowseCap.isFirefox();
   }

   public String getUrl()
   {
      if (getIFrameEx() != null)
         return getIFrameEx().getContentWindow().getLocationHref() ;
      else
         return null;
   }
   
   public String getDocTitle()
   {
      return getIFrameEx().getContentDocument().getTitle() ;
   }

   public void showHelp(String url)
   {
      ensureWidget();
      bringToFront();
      navStack_.navigate(url) ;
      setLocation(url);
      navigated_ = true;
   }
     
   private void setLocation(final String url)
   {
      // allow subsequent calls to setLocation to override any previous 
      // call (necessary so two consecutive calls like we get during
      // some startup scenarios don't result in the first url displaying
      // rather than the second)
      targetUrl_ = url;
      
      RepeatingCommand navigateCommand = new RepeatingCommand() {
         @Override
         public boolean execute()
         {
            if (getIFrameEx() != null && 
                  getIFrameEx().getContentWindow() != null)
            {
               if (targetUrl_.equals(getUrl()))
               {
                  getIFrameEx().getContentWindow().reload();
               }
               else
               {
                  getIFrameEx().getContentWindow().replaceLocationHref(targetUrl_);
                  frame_.setUrl(targetUrl_);
               }
               return false;
            }
            else
            {
               return true;
            }
         }
      };

      if (navigateCommand.execute())
         Scheduler.get().scheduleFixedDelay(navigateCommand, 100);      
   }
   
   public void refresh()
   {
      String url = getUrl();
      if (url != null)
         setLocation(url);
   }

   private WindowEx getContentWindow()
   {
      return getIFrameEx() != null ? getIFrameEx().getContentWindow() : null ;
   }

   public void back()
   {
      String backUrl = navStack_.back() ;
      if (backUrl != null)
         setLocation(backUrl) ;
   }

   public void forward()
   {
      String fwdUrl = navStack_.forward() ;
      if (fwdUrl != null)
         setLocation(fwdUrl) ;
   }

   public void print()
   {
      getContentWindow().focus() ;
      getContentWindow().print() ;
   }
   
   public void popout()
   {
      String href = getContentWindow().getLocationHref() ;
      NewWindowOptions options = new NewWindowOptions();
      options.setAlwaysUseBrowser(true);
      globalDisplay_.openWindow(href, options);
   }
   
   @Override
   public void focus()
   {
      WindowEx contentWindow = getContentWindow();
      if (contentWindow != null)
         contentWindow.focus();
   }
   
   public HandlerRegistration addHelpNavigateHandler(HelpNavigateHandler handler)
   {
      return addHandler(handler, HelpNavigateEvent.TYPE) ;
   }
   
 
   public LinkMenu getHistory()
   {
      return history_ ;
   }

   public boolean navigated()
   {
      return navigated_;
   }

   private IFrameElementEx getIFrameEx()
   {
      return frame_.getElement().cast();
   }
   
   private void findInTopic(String term, CanFocus findInputSource)
   {
      // get content window
      WindowEx contentWindow = getContentWindow();
      if (contentWindow == null)
         return;
          
      if (!contentWindow.find(term, false, false, true, false))
      {
         globalDisplay_.showMessage(MessageDialog.INFO,
               "Find in Topic", 
               "No occurences found",
               findInputSource);
      }     
   }


   private final VirtualHistory navStack_ = new VirtualHistory() ;
   private final ToolbarLinkMenu history_ ;
 
   private Label title_ ;
   private Frame frame_ ;
   private FindTextBox findTextBox_;
   private final Provider<HelpSearch> searchProvider_ ;
   private GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private boolean navigated_;
   private boolean initialized_;
   private String targetUrl_;
}
