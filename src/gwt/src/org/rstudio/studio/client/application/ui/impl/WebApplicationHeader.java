/*
 * WebApplicationHeader.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.application.ui.impl;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.*;
import org.rstudio.core.client.command.impl.WebMenuCallback;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.MessageDialogLabel;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.studio.client.RStudioGinjector;
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

public class WebApplicationHeader extends Composite implements ApplicationHeader
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
      eventBus_ = eventBus;
      globalDisplay_ = globalDisplay; 
      
      // Use the outer panel to just aggregate the menu bar/account area,
      // with the logo. The logo can't be inside the HorizontalPanel because
      // it needs to overflow out of the top of the panel, and it was much
      // easier to do this with absolute positioning.
      outerPanel_ = new FlowPanel();
      outerPanel_.getElement().getStyle().setPosition(Position.RELATIVE);
      
      // large logo
      logoLarge_ = new Image(ThemeResources.INSTANCE.rstudio());
      ((ImageElement)logoLarge_.getElement().cast()).setAlt("RStudio");
      Style style = logoLarge_.getElement().getStyle();
      style.setPosition(Position.ABSOLUTE);
      style.setTop(5, Unit.PX);
      style.setLeft(18, Unit.PX);
      
      // small logo
      logoSmall_ = new Image(ThemeResources.INSTANCE.rstudio_small());
      ((ImageElement)logoSmall_.getElement().cast()).setAlt("RStudio");
      style = logoSmall_.getElement().getStyle();
      style.setPosition(Position.ABSOLUTE);
      style.setTop(5, Unit.PX);
      style.setLeft(18, Unit.PX);

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
                         KeyboardShortcut.SHIFT;
             
         AppCommand closeSourceDoc = commands.closeSourceDoc();
         closeSourceDoc.setShortcut(new KeyboardShortcut(modifiers, 'Z'));
         ShortcutManager.INSTANCE.register(modifiers, 'Z', closeSourceDoc);
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

      HTML spacer = new HTML();
      headerBarPanel_.add(spacer);
      headerBarPanel_.setCellWidth(spacer, "100%");

      // commands panel (no widgets added until after session init)
      headerBarCommandsPanel_ = new HorizontalPanel();
      headerBarPanel_.add(headerBarCommandsPanel_);
      headerBarPanel_.setCellHorizontalAlignment(headerBarCommandsPanel_,
                                                HorizontalPanel.ALIGN_RIGHT);

      eventBus.addHandler(SessionInitEvent.TYPE, new SessionInitHandler()
      {
         public void onSessionInit(SessionInitEvent sie)
         {
            SessionInfo sessionInfo = session.getSessionInfo();
            
            // only show the user identity if we are in server mode
           if (sessionInfo.getMode().equals(SessionInfo.SERVER_MODE))
               initCommandsPanel(sessionInfo);
            
            // complete toolbar initialization
            toolbar_.completeInitialization(sessionInfo);
            
            // add project tools to main menu
            projectMenuButton_ = 
               new ProjectPopupMenu(sessionInfo, commands).getToolbarButton();
            projectMenuButton_.addStyleName(
                       ThemeStyles.INSTANCE.webHeaderBarCommandsProjectMenu());
            headerBarPanel_.add(projectMenuButton_);
            showProjectMenu(!toolbar_.isVisible());     
         }
      });
      
      // create toolbar
      toolbar_ = new GlobalToolbar(commands, 
                                   eventBus,
                                   pCodeSearch);
      toolbar_.addStyleName(themeResources.themeStyles().webGlobalToolbar());
     
      // initialize widget
      initWidget(outerPanel_);
   }
    
   public void showToolbar(boolean showToolbar)
   {
      outerPanel_.clear();
      
      if (showToolbar)
      {
         HeaderPanel headerPanel = new HeaderPanel(headerBarPanel_, toolbar_);
         outerPanel_.add(headerPanel);
         outerPanel_.add(logoLarge_);
         mainMenu_.getElement().getStyle().setMarginLeft(18, Unit.PX);
         preferredHeight_ = 65;
         showProjectMenu(false);
      }
      else
      {
         MenubarPanel menubarPanel = new MenubarPanel(headerBarPanel_);
         outerPanel_.add(menubarPanel);
         outerPanel_.add(logoSmall_);
         mainMenu_.getElement().getStyle().setMarginLeft(0, Unit.PX);
         preferredHeight_ = 45;
         showProjectMenu(true);
      }
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
      projectMenuButton_.setVisible(show);
   }
   
   

   private native final void suppressBrowserForwardBack() /*-{
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
            return "<tr><td>" + cmd.getMenuLabel(true) + "</td>" +
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
      Label usernameLabel = new Label();
      usernameLabel.setText(sessionInfo.getUserIdentity());
      headerBarCommandsPanel_.add(usernameLabel);
      headerBarCommandsPanel_.add(createCommandSeparator());
          
      // signout link 
      Widget signoutLink = createCommandLink("Sign Out", new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            eventBus_.fireEvent(new LogoutRequestedEvent());
         }
      });
      headerBarCommandsPanel_.add(signoutLink);
   }

   private Widget createCommandSeparator()
   {
      return new HTML("&nbsp;|&nbsp;");
   }
   
   private Widget createCommandLink(String caption, ClickHandler clickHandler)
   {
      HyperlinkLabel link = new HyperlinkLabel(caption, clickHandler);
      return link;
   }

   public Widget asWidget()
   {
      return this;
   }
  
   private int preferredHeight_;
   private FlowPanel outerPanel_;
   private Image logoLarge_;
   private Image logoSmall_;
   private HorizontalPanel headerBarPanel_;
   private HorizontalPanel headerBarCommandsPanel_;
   private ToolbarButton projectMenuButton_;
   private AppMenuBar mainMenu_;
   private GlobalToolbar toolbar_;
   private EventBus eventBus_;
   private GlobalDisplay globalDisplay_;
   
   
}
