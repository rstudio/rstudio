/*
 * WebApplicationHeader.java
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

package org.rstudio.studio.client.application.ui.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.TextDecoration;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.*;
import org.rstudio.core.client.command.impl.WebMenuCallback;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.MessageDialogLabel;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.core.client.widget.ToolbarSeparator;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.LogoutRequestedEvent;
import org.rstudio.studio.client.application.ui.ApplicationHeader;
import org.rstudio.studio.client.application.ui.GlobalToolbar;
import org.rstudio.studio.client.application.ui.ProjectPopupMenu;
import org.rstudio.studio.client.application.ui.impl.header.HeaderPanel;
import org.rstudio.studio.client.application.ui.impl.header.MenubarPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dialog.WebDialogBuilderFactory;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public class WebApplicationHeader extends Composite 
                                  implements ApplicationHeader,
                                  WebApplicationHeaderOverlay.Context
{  
   public WebApplicationHeader()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   public void initialize(
                  final Commands commands,
                  EventBus eventBus,
                  GlobalDisplay globalDisplay,
                  ThemeResources themeResources,
                  final Session session,
                  Provider<CodeSearch> pCodeSearch)
   {
      commands_ = commands;
      eventBus_ = eventBus;
      globalDisplay_ = globalDisplay; 
      overlay_ = new WebApplicationHeaderOverlay();
      
      // Use the outer panel to just aggregate the menu bar/account area,
      // with the logo. The logo can't be inside the HorizontalPanel because
      // it needs to overflow out of the top of the panel, and it was much
      // easier to do this with absolute positioning.
      outerPanel_ = new FlowPanel();
      outerPanel_.getElement().getStyle().setPosition(Position.RELATIVE);
      
      // large logo
      logoLarge_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.rstudio2x()));
      ((ImageElement)logoLarge_.getElement().cast()).setAlt("RStudio");
      logoLarge_.getElement().getStyle().setBorderWidth(0, Unit.PX);
      
      // small logo
      logoSmall_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.rstudio_small2x()));
      ((ImageElement)logoSmall_.getElement().cast()).setAlt("RStudio");
      logoSmall_.getElement().getStyle().setBorderWidth(0, Unit.PX);

      // link target for logo
      logoAnchor_ = new Anchor();
      Style style = logoAnchor_.getElement().getStyle();
      style.setPosition(Position.ABSOLUTE);
      style.setTop(5, Unit.PX);
      style.setLeft(18, Unit.PX);
      style.setTextDecoration(TextDecoration.NONE);
      style.setOutlineWidth(0, Unit.PX);

      logoAnchor_.setStylePrimaryName(themeResources.themeStyles().logoAnchor());

      // header container
      headerBarPanel_ = new HorizontalPanel() ;
      headerBarPanel_.setStylePrimaryName(themeResources.themeStyles().header());
      headerBarPanel_.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      headerBarPanel_.setWidth("100%");

      if (BrowseCap.INSTANCE.suppressBrowserForwardBack())
         suppressBrowserForwardBack();

      // override Cmd+W keybaord shortcut for Chrome
      if (BrowseCap.isChrome())
      {
         int modifiers = (BrowseCap.hasMetaKey() ? KeyboardShortcut.META : 
                                                   KeyboardShortcut.CTRL) |
                         KeyboardShortcut.ALT;
             
         AppCommand closeSourceDoc = commands.closeSourceDoc();
         closeSourceDoc.setShortcut(new KeyboardShortcut(modifiers, 'W'));
         ShortcutManager.INSTANCE.register(
               modifiers, 'W', closeSourceDoc, "", "", "");
      }
      
      // main menu
      advertiseEditingShortcuts(globalDisplay, commands);
      WebMenuCallback menuCallback = new WebMenuCallback();
      commands.mainMenu(menuCallback);
      mainMenu_ = menuCallback.getMenu();
      mainMenu_.setAutoHideRedundantSeparators(false);
      fixup(mainMenu_);
      mainMenu_.addStyleName(themeResources.themeStyles().mainMenu());
      AppMenuBar.addSubMenuVisibleChangedHandler(new SubMenuVisibleChangedHandler()
      {
         public void onSubMenuVisibleChanged(SubMenuVisibleChangedEvent event)
         {
            // When submenus of the main menu appear, glass over any iframes
            // so that mouse clicks can make the menus disappear
            if (event.isVisible())
               eventBus_.fireEvent(new GlassVisibilityEvent(true));
            else
               eventBus_.fireEvent(new GlassVisibilityEvent(false));
         }
      });
      headerBarPanel_.add(mainMenu_);

      // commands panel (no widgets added until after session init)
      headerBarCommandsPanel_ = new HorizontalPanel();
      headerBarCommandsPanel_.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      headerBarCommandsPanel_.setWidth("100%");
      headerBarPanel_.add(headerBarCommandsPanel_);
      headerBarPanel_.setCellWidth(headerBarCommandsPanel_, "100%");
      headerBarPanel_.setCellHorizontalAlignment(headerBarCommandsPanel_,
                                                HorizontalPanel.ALIGN_RIGHT);

      eventBus.addHandler(SessionInitEvent.TYPE, new SessionInitHandler()
      {
         public void onSessionInit(SessionInitEvent sie)
         {
            SessionInfo sessionInfo = session.getSessionInfo();
            
            // complete toolbar initialization
            toolbar_.completeInitialization(sessionInfo);
             
            // add project tools to main menu
            projectMenuSeparator_ = createCommandSeparator();
            headerBarPanel_.add(projectMenuSeparator_);
            projectMenuButton_ = 
               new ProjectPopupMenu(sessionInfo, commands).getToolbarButton();
            projectMenuButton_.addStyleName(
                       ThemeStyles.INSTANCE.webHeaderBarCommandsProjectMenu());
            headerBarPanel_.add(projectMenuButton_);
            showProjectMenu(!toolbar_.isVisible());
                
            // record logo target url (if any)
            logoTargetUrl_ = sessionInfo.getUserHomePageUrl();
            if (logoTargetUrl_ != null)
            {
               logoAnchor_.setHref(logoTargetUrl_);
               logoAnchor_.setTitle("RStudio Server Home");

               logoLarge_.setResource(new ImageResource2x(ThemeResources.INSTANCE.rstudio_home2x()));
               logoSmall_.setResource(new ImageResource2x(ThemeResources.INSTANCE.rstudio_home_small2x()));
            }
            else
            {
               // no link, so ensure this doesn't get styled as clickable
               logoAnchor_.getElement().getStyle().setCursor(Cursor.DEFAULT);
            }
            
            // init commands panel in server mode
            if (!Desktop.isDesktop())
               initCommandsPanel(sessionInfo);
                      
            // notify overlay of global toolbar state
            overlay_.setGlobalToolbarVisible(WebApplicationHeader.this, 
                                             toolbar_.isVisible());
         }
      });
      
      // create toolbar
      toolbar_ = new GlobalToolbar(commands, 
                                   eventBus,
                                   pCodeSearch);
      toolbar_.addStyleName(themeResources.themeStyles().webGlobalToolbar());
      
      // create host for project commands
      projectBarCommandsPanel_ = new HorizontalPanel();
      toolbar_.addRightWidget(projectBarCommandsPanel_);
      
      // initialize widget
      initWidget(outerPanel_);
   }
    
   public void showToolbar(boolean showToolbar)
   {
      outerPanel_.clear();
      
      if (showToolbar)
      {
         logoAnchor_.getElement().removeAllChildren();
         logoAnchor_.getElement().appendChild(logoLarge_.getElement());
         outerPanel_.add(logoAnchor_);
         HeaderPanel headerPanel = new HeaderPanel(headerBarPanel_, toolbar_);
         outerPanel_.add(headerPanel);
         preferredHeight_ = 65;
         showProjectMenu(false);
      }
      else
      {
         logoAnchor_.getElement().removeAllChildren();
         logoAnchor_.getElement().appendChild(logoSmall_.getElement());
         outerPanel_.add(logoAnchor_);
         MenubarPanel menubarPanel = new MenubarPanel(headerBarPanel_);
         outerPanel_.add(menubarPanel);
         mainMenu_.getElement().getStyle().setMarginLeft(0, Unit.PX);
         preferredHeight_ = 45;
         showProjectMenu(true);
      }
      
      overlay_.setGlobalToolbarVisible(this, showToolbar);
   }
   
   public boolean isToolbarVisible()
   {
      return !projectMenuButton_.isVisible();
   }
   
   public void focusGoToFunction()
   {
      toolbar_.focusGoToFunction();
   }
   
   private void showProjectMenu(boolean show)
   {
      projectMenuSeparator_.setVisible(show);
      projectMenuButton_.setVisible(show);
   }
   
   private native final void suppressBrowserForwardBack() /*-{
      try {
      var outerWindow = $wnd.parent;
      if (outerWindow.addEventListener) {
         var handler = function(evt) {
            if ((evt.keyCode == 37 || evt.keyCode == 39) && (evt.metaKey && !evt.ctrlKey && !evt.shiftKey && !evt.altKey)) {
               evt.preventDefault();
               evt.stopPropagation();
            }
         };
         outerWindow.addEventListener('keydown', handler, false);
         $wnd.addEventListener('keydown', handler, false);
      }
      } catch(err) {}
   }-*/;

   private void advertiseEditingShortcuts(final GlobalDisplay display,
                                          final Commands commands)
   {
      int mod = BrowseCap.hasMetaKey() ? KeyboardShortcut.META : KeyboardShortcut.CTRL;

      commands.undoDummy().setShortcut(new KeyboardShortcut(mod, 'Z'));
      commands.redoDummy().setShortcut(new KeyboardShortcut(mod| KeyboardShortcut.SHIFT, 'Z'));

      commands.cutDummy().setShortcut(new KeyboardShortcut(mod, 'X'));
      commands.copyDummy().setShortcut(new KeyboardShortcut(mod, 'C'));
      commands.pasteDummy().setShortcut(new KeyboardShortcut(mod, 'V'));

      CommandHandler useKeyboardNotification = new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            MessageDialogLabel label = new MessageDialogLabel();
            label.setHtml("Your browser does not allow access to your<br/>" +
                          "computer's clipboard. As a result you must<br/>" +
                          "use keyboard shortcuts for:" +
                          "<br/><br/><table cellpadding=0 cellspacing=0 border=0>" +
                          makeRow(commands.undoDummy()) +
                          makeRow(commands.redoDummy()) +
                          makeRow(commands.cutDummy()) +
                          makeRow(commands.copyDummy()) +
                          makeRow(commands.pasteDummy()) +
                          "</table>"
                          );
            new WebDialogBuilderFactory().create(
                  GlobalDisplay.MSG_WARNING,
                  "Use Keyboard Shortcut",
                  label).showModal();
         }

         private String makeRow(AppCommand cmd)
         {
            String textAlign = BrowseCap.hasMetaKey()
                               ? "text-align: right"
                               : "";
            return "<tr><td>" + cmd.getMenuLabel(false) + "</td>" +
                   "<td style='padding-left: 12px; " + textAlign + "'>"
                   + cmd.getShortcutPrettyHtml() + "</td></tr>";
         }
      };

      commands.undoDummy().addHandler(useKeyboardNotification);
      commands.redoDummy().addHandler(useKeyboardNotification);
      commands.cutDummy().addHandler(useKeyboardNotification);
      commands.copyDummy().addHandler(useKeyboardNotification);
      commands.pasteDummy().addHandler(useKeyboardNotification);
   }

   public int getPreferredHeight()
   {
      return preferredHeight_;
   }

   /**
    * Without this fixup, the main menu doesn't properly deselect its items
    * when the mouse takes focus away.
    */
   private void fixup(final AppMenuBar mainMenu)
   {
      mainMenu.addCloseHandler(new CloseHandler<PopupPanel>()
      {
         public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent)
         {
            // Only dismiss the selection if the panel that just closed belongs
            // to the currently selected item. Otherwise, the selected item
            // has already changed and we don't want to mess with it. (This is
            // NOT an edge case, it is very common.)
            MenuItem menuItem = mainMenu.getSelectedItem();
            if (menuItem != null)
            {
               MenuBar subMenu = menuItem.getSubMenu();
               if (subMenu != null &&
                   popupPanelCloseEvent.getTarget() != null &&
                   subMenu.equals(popupPanelCloseEvent.getTarget().getWidget()))
               {
                  Scheduler.get().scheduleDeferred(new ScheduledCommand()
                  {
                     public void execute()
                     {
                        mainMenu.selectItem(null);
                     }
                  });
               }
            }
         }
      });
   }

   private void initCommandsPanel(final SessionInfo sessionInfo)
   {  
      // add username 
      if (sessionInfo.getShowIdentity())
      {
         ToolbarLabel usernameLabel = new ToolbarLabel();
         usernameLabel.getElement().getStyle().setMarginRight(2, Unit.PX);
         if (!BrowseCap.isFirefox())
            usernameLabel.getElement().getStyle().setMarginTop(2, Unit.PX);
         String userIdentity = sessionInfo.getUserIdentity();
         usernameLabel.setTitle(userIdentity);
         userIdentity = userIdentity.split("@")[0];
         usernameLabel.setText(userIdentity);
         headerBarCommandsPanel_.add(usernameLabel);
        
         ToolbarButton signOutButton = new ToolbarButton(new ImageResource2x(RESOURCES.signOut2x()),
              new ClickHandler() {
            public void onClick(ClickEvent event)
            {
               eventBus_.fireEvent(new LogoutRequestedEvent());
            }
         });
         signOutButton.setTitle("Sign out");
         headerBarCommandsPanel_.add(signOutButton);
         headerBarCommandsPanel_.add(
                  signOutSeparator_ = createCommandSeparator());
      }
      
      overlay_.addCommands(this);
      
      headerBarCommandsPanel_.add(commands_.quitSession().createToolbarButton());
   }

   private Widget createCommandSeparator()
   {
      ToolbarSeparator sep = new ToolbarSeparator();
      Style style = sep.getElement().getStyle();
      style.setMarginTop(2, Unit.PX);
      style.setMarginLeft(3, Unit.PX);
      return sep;
   }
   
   private Widget createCommandLink(String caption, ClickHandler clickHandler)
   {
      HyperlinkLabel link = new HyperlinkLabel(caption, clickHandler);
      return link;
   }
   
   @Override
   public void addCommand(Widget widget)
   {
      headerBarCommandsPanel_.add(widget);
   }

   @Override
   public Widget addCommandSeparator()
   {
      Widget separator = createCommandSeparator();
      headerBarCommandsPanel_.add(separator);
      return separator;
   }
   
   @Override
   public void addLeftCommand(Widget widget)
   {
      addLeftCommand(widget, null);
   }

   @Override
   public void addLeftCommand(Widget widget, String width)
   {
      headerBarCommandsPanel_.insert(widget, 0);
      if (width != null)
      {
         headerBarCommandsPanel_.setCellWidth(widget, width);
      }
   }

   @Override
   public void addRightCommand(Widget widget)
   {
      headerBarPanel_.add(widget);
   }
   
   @Override
   public Widget addRightCommandSeparator()
   {
      Widget separator = createCommandSeparator();
      headerBarPanel_.add(separator);
      return separator;
   }
   
   @Override
   public void addProjectCommand(Widget widget)
   {
      projectBarCommandsPanel_.add(widget);
   }

   @Override
   public Widget addProjectCommandSeparator()
   {
      Widget separator = createCommandSeparator();
      projectBarCommandsPanel_.add(separator);
      return separator;
   }
   
   @Override
   public void addProjectRightCommand(Widget widget)
   {
      toolbar_.addRightWidget(widget);   
   }

   @Override
   public Widget addProjectRightCommandSeparator()
   {
      return toolbar_.addRightSeparator();
   }
   
   @Override
   public AppMenuBar getMainMenu()
   {
      return mainMenu_;
   }
   

   public Widget asWidget()
   {
      return this;
   }
   
   interface Resources extends ClientBundle
   {
      @Source("signOut_2x.png")
      ImageResource signOut2x();
   }
   
   private static final Resources RESOURCES =  
                              (Resources) GWT.create(Resources.class);
  
   private int preferredHeight_;
   private FlowPanel outerPanel_;
   private Anchor logoAnchor_;
   private Image logoLarge_;
   private Image logoSmall_;
   private String logoTargetUrl_ = null;
   private HorizontalPanel headerBarPanel_;
   private HorizontalPanel headerBarCommandsPanel_;
   private HorizontalPanel projectBarCommandsPanel_;
   private Widget signOutSeparator_;
   private Widget projectMenuSeparator_;
   private ToolbarButton projectMenuButton_;
   private AppMenuBar mainMenu_;
   private GlobalToolbar toolbar_;
   private EventBus eventBus_;
   private GlobalDisplay globalDisplay_;
   private Commands commands_; 
   private WebApplicationHeaderOverlay overlay_;
}
