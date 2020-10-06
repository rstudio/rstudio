/*
 * HelpPane.java
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

package org.rstudio.studio.client.workbench.views.help;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
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
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.URL;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FindTextBox;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.RStudioThemedFrame;
import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.help.Help.LinkMenu;
import org.rstudio.studio.client.workbench.views.help.events.HelpNavigateEvent;
import org.rstudio.studio.client.workbench.views.help.model.VirtualHistory;
import org.rstudio.studio.client.workbench.views.help.search.HelpSearch;

public class HelpPane extends WorkbenchPane
                      implements Help.Display
{
   @Inject
   public HelpPane(Provider<HelpSearch> searchProvider,
                   GlobalDisplay globalDisplay,
                   Commands commands,
                   EventBus events,
                   UserPrefs prefs)
   {
      super("Help", events);

      searchProvider_ = searchProvider;
      globalDisplay_ = globalDisplay;
      commands_ = commands;

      prefs_ = prefs;

      MenuItem clear = commands.clearHelpHistory().createMenuItem(false);
      history_ = new ToolbarLinkMenu(12, true, null, new MenuItem[] { clear });

      Window.addResizeHandler(new ResizeHandler()
      {
         public void onResize(ResizeEvent event)
         {
            history_.getMenu().hide();
         }
      });

      frame_ = new RStudioThemedFrame(
         "Help Pane",
         null,
         RES.editorStyles().getText(),
         null,
         false,
         RStudioThemes.isFlat());
      frame_.setSize("100%", "100%");
      frame_.setStylePrimaryName("rstudio-HelpFrame");
      frame_.addStyleName("ace_editor_theme");
      ElementIds.assignElementId(frame_.getElement(), ElementIds.HELP_FRAME);

      navStack_ = new VirtualHistory(frame_);

      // NOTE: we do some pretty strange gymnastics to save the scroll
      // position for the iframe. when the Help Pane is deactivated
      // (e.g. another tab in the tabset is selected), a synthetic scroll
      // event is sent to the iframe's window, forcing it to scroll back
      // to the top of the window. in order to suppress this behavior, we
      // track whether the scroll event occurred when the tab was deactivated;
      // if it was, then we restore the last-recorded scroll position instead.
      scrollTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            WindowEx contentWindow = getContentWindow();
            if (contentWindow != null)
            {
               if (selected_)
               {
                  scrollPos_ = contentWindow.getScrollPosition();
               }
               else if (scrollPos_ != null)
               {
                  contentWindow.setScrollPosition(scrollPos_);
               }
            }
         }
      };

      prefs_.helpFontSizePoints().bind(new CommandWithArg<Double>()
      {
         public void execute(Double value)
         {
            refresh();
         }
      });

      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      return new AutoGlassPanel(frame_);
   }

   @Override
   public void onBeforeUnselected()
   {
      super.onBeforeUnselected();
      selected_ = false;
   }

   @Override
   public void onSelected()
   {
      super.onSelected();
      selected_ = true;

      if (scrollPos_ == null)
         return;

      IFrameElementEx iframeEl = getIFrameEx();
      if (iframeEl == null)
         return;

      WindowEx windowEl = iframeEl.getContentWindow();
      if (windowEl == null)
         return;

      windowEl.setScrollPosition(scrollPos_);
      setFocus();
   }

   @Override
   public void setFocus()
   {
      focusSearchHelp();
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
      super.onLoad();

      if (!initialized_)
      {
         initialized_ = true;

         initHelpCallbacks();

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

      var thiz = this;
      $wnd.helpNavigated = function(document, win) {
         thiz.@org.rstudio.studio.client.workbench.views.help.HelpPane::helpNavigated(Lcom/google/gwt/dom/client/Document;)(document);
         addEventHandler(win, "unload", function () {
            thiz.@org.rstudio.studio.client.workbench.views.help.HelpPane::unload()();
         });
      };
      $wnd.helpNavigate = function(url) {
         if (url.length)
            thiz.@org.rstudio.studio.client.workbench.views.help.HelpPane::showHelp(Ljava/lang/String;)(url);
      };

      $wnd.helpKeydown = function(e) {
         thiz.@org.rstudio.studio.client.workbench.views.help.HelpPane::handleKeyDown(Lcom/google/gwt/dom/client/NativeEvent;)(e);
      };
   }-*/;



   // delegate shortcuts which occur while Help has focus

   private void handleKeyDown(NativeEvent e)
   {
      // determine whether this key-combination means we should focus find
      int mod = KeyboardShortcut.getModifierValue(e);
      if (mod == (BrowseCap.hasMetaKey() ? KeyboardShortcut.META
                                         : KeyboardShortcut.CTRL))
      {
         if (e.getKeyCode() == 'F')
         {
            e.preventDefault();
            e.stopPropagation();
            WindowEx.get().focus();
            findTextBox_.focus();
            findTextBox_.selectAll();
            return;
         }
         else if (e.getKeyCode() == KeyCodes.KEY_ENTER)
         {
            // extract the selected code, if any
            String code = frame_.getWindow().getSelectedText();
            if (code.isEmpty())
               return;

            // send it to the console
            events_.fireEvent(new SendToConsoleEvent(
                  code,
                  true, // execute
                  false // focus
                  ));
            return;
         }
      }


      // don't let backspace perform browser back
      DomUtils.preventBackspaceCausingBrowserBack(e);

      // delegate to the shortcut manager
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

   private void helpNavigated(Document doc)
   {
      NodeList<Element> elements = doc.getElementsByTagName("a");
      for (int i = 0; i < elements.getLength(); i++)
      {
         ElementEx a = (ElementEx) elements.getItem(i);
         String href = a.getAttribute("href", 2);
         if (href == null)
            continue;

         if (href.contains(":") || href.endsWith(".pdf"))
         {
            // external links
            AnchorElement aElement = a.cast();
            aElement.setTarget("_blank");
         }
         else
         {
            // Internal links need to be handled in JavaScript so that
            // they can participate in virtual session history. This
            // won't have any effect for right-click > Show in New Window
            // but that's a good thing.
            a.setAttribute(
                  "onclick",
                  "window.parent.helpNavigate(this.href); return false");
         }
      }

      String effectiveTitle = getDocTitle(doc);
      title_.setText(effectiveTitle);
      this.fireEvent(new HelpNavigateEvent(doc.getURL(), effectiveTitle));
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
      title_.setText("");
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar("Help Tab");

      toolbar.addLeftWidget(commands_.helpBack().createToolbarButton());
      toolbar.addLeftWidget(commands_.helpForward().createToolbarButton());
      toolbar.addLeftWidget(commands_.helpHome().createToolbarButton());
      toolbar.addLeftSeparator();
      if (!Desktop.isDesktop())
      {
         // QtWebEngine doesn't currently support window.print(); see:
         // https://bugreports.qt.io/browse/QTBUG-53745
         toolbar.addLeftWidget(commands_.printHelp().createToolbarButton());
         toolbar.addLeftSeparator();
      }
      toolbar.addLeftWidget(commands_.helpPopout().createToolbarButton());

      searchWidget_ = searchProvider_.get().getSearchWidget();
      toolbar.addRightWidget((Widget)searchWidget_);

      toolbar.addRightSeparator();

      ToolbarButton refreshButton = commands_.refreshHelp().createToolbarButton();
      refreshButton.addStyleName(ThemeStyles.INSTANCE.refreshToolbarButton());
      toolbar.addRightWidget(refreshButton);

      return toolbar;
   }

   @Override
   protected SecondaryToolbar createSecondaryToolbar()
   {
      SecondaryToolbar toolbar = new SecondaryToolbar("Help Tab Second");

      title_ = new Label();
      title_.addStyleName(RES.styles().topicTitle());
      toolbar.addLeftPopupMenu(title_, history_.getMenu());

      ThemeStyles styles = ThemeStyles.INSTANCE;
      toolbar.getWrapper().addStyleName(styles.tallerToolbarWrapper());

      if (isFindSupported())
      {
         final SmallButton btnNext = new SmallButton("&gt;", true);
         btnNext.setTitle("Find next (Enter)");
         btnNext.addStyleName(RES.styles().topicNavigationButton());
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
         btnPrev.addStyleName(RES.styles().topicNavigationButton());
         btnPrev.setVisible(false);
         btnPrev.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               findPrev();
            }
         });


         findTextBox_ = new FindTextBox("Find in Topic");
         findTextBox_.addStyleName(RES.styles().findTopicTextbox());
         findTextBox_.setOverrideWidth(90);
         ElementIds.assignElementId(findTextBox_, ElementIds.SW_HELP_FIND_IN_TOPIC);
         toolbar.addLeftWidget(findTextBox_);
         findTextBox_.addKeyUpHandler(new KeyUpHandler() {

            @Override
            public void onKeyUp(KeyUpEvent event)
            {
               // ignore modifier key release
               if (event.getNativeKeyCode() == KeyCodes.KEY_CTRL ||
                   event.getNativeKeyCode() == KeyCodes.KEY_ALT ||
                   event.getNativeKeyCode() == KeyCodes.KEY_SHIFT)
               {
                  return;
               }

               WindowEx contentWindow = getContentWindow();
               if (contentWindow != null)
               {
                  // escape means exit find mode and put focus
                  // into the main content window
                  if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
                  {
                     event.preventDefault();
                     event.stopPropagation();
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

                     int modifier = KeyboardShortcut.getModifierValue(event.getNativeEvent());
                     boolean isShift = modifier == KeyboardShortcut.SHIFT;

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

                           performFind(term, !isShift, incremental);
                        }
                        else
                        {
                           if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
                              performFind(term, !isShift, false);
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

      return toolbar;
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

   @Override
   public String getUrl()
   {
      String url = null;
      try
      {
         if (getIFrameEx() != null && getIFrameEx().getContentWindow() != null)
            url = getIFrameEx().getContentWindow().getLocationHref();
      }
      catch (Exception e)
      {
         // attempting to get the URL can throw with a DOM security exception if
         // the current URL is on another domain--in this case we'll just want
         // to return null, so eat the exception.
      }
      return url;
   }

   @Override
   public String getDocTitle()
   {
      return getIFrameEx().getContentDocument().getTitle();
   }

   @Override
   public void focusSearchHelp()
   {
      if (searchWidget_ != null)
         FocusHelper.setFocusDeferred(searchWidget_);
   }

   @Override
   public void showHelp(String url)
   {
      ensureWidget();
      bringToFront();
      navStack_.navigate(url);
      setLocation(url, Point.create(0, 0));
      navigated_ = true;
   }

   private void setLocation(final String url,
                            final Point scrollPos)
   {
      // allow subsequent calls to setLocation to override any previous
      // call (necessary so two consecutive calls like we get during
      // some startup scenarios don't result in the first url displaying
      // rather than the second)
      targetUrl_ = url;

      RepeatingCommand navigateCommand = new RepeatingCommand() {

         @SuppressWarnings("unused")
         private HandlerRegistration handler_ = frame_.addLoadHandler(new LoadHandler()
         {
            @Override
            public void onLoad(LoadEvent event)
            {
               WindowEx contentWindow = getIFrameEx().getContentWindow();
               contentWindow.setScrollPosition(scrollPos);
               setWindowScrollHandler(contentWindow);

               handler_.removeHandler();
               handler_ = null;
            }
         });

         @Override
         public boolean execute()
         {
            if (getIFrameEx() == null)
               return true;

            if (getIFrameEx().getContentWindow() == null)
               return true;

            if (targetUrl_ == getUrl())
            {
               getIFrameEx().getContentWindow().reload();
            }
            else
            {
               frame_.setUrl(targetUrl_);
               replaceFrameUrl(frame_.getIFrame().cast(), targetUrl_);
            }

            return false;
         }
      };

      if (navigateCommand.execute())
      {
         Scheduler.get().scheduleFixedDelay(navigateCommand, 100);
      }
   }

   @Override
   public void refresh()
   {
      String url = getUrl();
      if (url != null)
         setLocation(url, Point.create(0, 0));
   }

   private WindowEx getContentWindow()
   {
      return getIFrameEx() != null ? getIFrameEx().getContentWindow() : null;
   }

   @Override
   public void back()
   {
      VirtualHistory.Data back = navStack_.back();
      if (back != null)
         setLocation(back.getUrl(), back.getScrollPosition());
   }

   @Override
   public void forward()
   {
      VirtualHistory.Data fwd = navStack_.forward();
      if (fwd != null)
         setLocation(fwd.getUrl(), fwd.getScrollPosition());
   }

   @Override
   public void print()
   {
      getContentWindow().focus();
      getContentWindow().print();
   }

   @Override
   public void popout()
   {
      String href = getContentWindow().getLocationHref();
      NewWindowOptions options = new NewWindowOptions();
      options.setName("helppanepopout_" + popoutCount_++);
      globalDisplay_.openWebMinimalWindow(href, false, 0, 0, options);
   }

   @Override
   public void focus()
   {
      WindowEx contentWindow = getContentWindow();
      if (contentWindow != null)
         contentWindow.focus();
   }

   @Override
   public HandlerRegistration addHelpNavigateHandler(HelpNavigateEvent.Handler handler)
   {
      return addHandler(handler, HelpNavigateEvent.TYPE);
   }


   @Override
   public LinkMenu getHistory()
   {
      return history_;
   }

   @Override
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
               "No occurrences found",
               findInputSource);
      }
   }

   private final native void replaceFrameUrl(JavaScriptObject frame, String url) /*-{
      frame.contentWindow.setTimeout(function() {
         this.location.replace(url);
      }, 0);
   }-*/;

   private final native void setWindowScrollHandler(WindowEx window)
   /*-{
      var self = this;
      window.onscroll = $entry(function() {
         self.@org.rstudio.studio.client.workbench.views.help.HelpPane::onScroll()();
      });
   }-*/;

   private void onScroll()
   {
      scrollTimer_.schedule(50);
   }

   public interface Styles extends CssResource
   {
      String findTopicTextbox();
      String topicNavigationButton();
      String topicTitle();
   }

   public interface EditorStyles extends CssResource
   {
   }

   public interface Resources extends ClientBundle
   {
      @Source("HelpPane.css")
      Styles styles();

      @Source("HelpPaneEditorStyles.css")
      EditorStyles editorStyles();
   }

   private static final Resources RES = GWT.create(Resources.class);
   static { RES.styles().ensureInjected(); }

   private UserPrefs prefs_;

   private final VirtualHistory navStack_;
   private final ToolbarLinkMenu history_;
   private Label title_;
   private RStudioThemedFrame frame_;
   private FindTextBox findTextBox_;
   private final Provider<HelpSearch> searchProvider_;
   private GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private boolean navigated_;
   private boolean initialized_;
   private String targetUrl_;
   private Point scrollPos_;
   private Timer scrollTimer_;
   private boolean selected_;
   private static int popoutCount_ = 0;
   private SearchDisplay searchWidget_;
}
