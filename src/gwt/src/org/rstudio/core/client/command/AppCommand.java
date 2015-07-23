/*
 * AppCommand.java
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
package org.rstudio.core.client.command;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuItem;

import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;

public class AppCommand implements Command, ClickHandler, ImageResourceProvider
{
   private class CommandToolbarButton extends ToolbarButton implements
         EnabledChangedHandler, VisibleChangedHandler
   { 
      public CommandToolbarButton(String buttonLabel,
            ImageResourceProvider imageResourceProvider, AppCommand command,
            boolean synced)
      {
         super(buttonLabel, imageResourceProvider, command);
         command_ = command;
         synced_ = synced;
      }

      @Override
      protected void onAttach()
      {
         if (synced_)
         {
            setEnabled(command_.isEnabled());
            setVisible(command_.isVisible());
            handlerReg_ = command_.addEnabledChangedHandler(this);
            handlerReg2_ = command_.addVisibleChangedHandler(this);
         }
         
         parentToolbar_ = getParentToolbar();

         super.onAttach();
      }

      @Override
      protected void onDetach()
      {
         super.onDetach();

         if (synced_)
         {
            handlerReg_.removeHandler();
            handlerReg2_.removeHandler();
         }
      }

      public void onEnabledChanged(AppCommand command)
      {
         setEnabled(command_.isEnabled());
      }

      public void onVisibleChanged(AppCommand command)
      {
         setVisible(command_.isVisible());
         if (command_.isVisible())
            setEnabled(command_.isEnabled());

         parentToolbar_.invalidateSeparators();
      }

      private final AppCommand command_;
      private boolean synced_ = true;
      private HandlerRegistration handlerReg_;
      private HandlerRegistration handlerReg2_;
      private Toolbar parentToolbar_;
   }
   
   public enum Category
   {
      WORKBENCH,
      EDITOR,
      R,
      HELP,
      VCS,
      PACKRAT,
      PRESENTATION
   }

   public AppCommand()
   {
   }

   void executeFromShortcut()
   {
      executedFromShortcut_ = true;
      doExecute();
   }
   
   public void execute()
   {
      executedFromShortcut_ = false;
      doExecute();
   }
   
   private void doExecute()
   {
      assert enabled_ : "AppCommand executed when it was not enabled";
      if (!enabled_)
         return;
      assert visible_ : "AppCommand executed when it was not visible";
      if (!visible_)
         return;
      
      // if this window is a satellite but the command only wants to be handled
      // in the main window, activate the main window and execute the command
      // there instead
      Satellite satellite = RStudioGinjector.INSTANCE.getSatellite();
      if (satellite.isCurrentWindowSatellite() 
          && (getWindowMode().equals(WINDOW_MODE_MAIN) ||
              getWindowMode().equals(WINDOW_MODE_BACKGROUND)))
      {
         if (getWindowMode().equals(WINDOW_MODE_MAIN))
            satellite.focusMainWindow();
         SatelliteManager mgr = RStudioGinjector.INSTANCE.getSatelliteManager();
         mgr.dispatchCommand(this, null);
         return;
      }

      if (enableNoHandlerAssertions_)
      {
         assert handlers_.getHandlerCount(CommandEvent.TYPE) > 0 
                  : "AppCommand executed but nobody was listening";
      }
      
      handlers_.fireEvent(new CommandEvent(this));
   }

   public boolean isEnabled()
   {
      return enabled_ && isVisible(); // jcheng 06/30/2010: Hmmmm, smells weird.
   }

   public void setEnabled(boolean enabled)
   {
      if (enabled != enabled_)
      {
         enabled_ = enabled;
         handlers_.fireEvent(new EnabledChangedEvent(this));
      }
   }

   public boolean isVisible()
   {
      return visible_;
   }

   public void setVisible(boolean visible)
   {
      if (!removed_ && visible != visible_)
      {
         visible_ = visible;
         handlers_.fireEvent(new VisibleChangedEvent(this));
      }
   }

   public boolean isCheckable()
   {
      return checkable_;
   }

   public void setCheckable(boolean isCheckable)
   {
      checkable_ = isCheckable;
   }

   public boolean isChecked()
   {
      return checkable_ && checked_;
   }

   public void setChecked(boolean checked)
   {
      if (!isCheckable())
         return;
      checked_ = checked;
   }

   public boolean preventShortcutWhenDisabled()
   {
      return preventShortcutWhenDisabled_;
   }

   public void setPreventShortcutWhenDisabled(boolean preventShortcut)
   {
      preventShortcutWhenDisabled_ = preventShortcut;
   }
   
   public String getWindowMode()
   {
      return windowMode_;
   }
   
   public void setWindowMode(String mode)
   {
      windowMode_ = mode;
   }
   
   public boolean isRebindable()
   {
      return rebindable_;
   }
   
   public void setRebindable(boolean rebindable)
   {
      rebindable_ = rebindable;
   }
   
   public Category getCategory()
   {
      return category_;
   }
   
   public void setCategory(String category)
   {
      String lower = category.toLowerCase();
      if (lower.equals("workbench"))
         category_ = Category.WORKBENCH;
      else if (lower.equals("editor"))
         category_ = Category.EDITOR;
      else if (lower.equals("vcs"))
         category_ = Category.VCS;
      else if (lower.equals("r"))
         category_ = Category.R;
      else if (lower.equals("help"))
         category_ = Category.HELP;
      else if (lower.equals("packrat"))
         category_ = Category.PACKRAT;
      else if (lower.equals("presentation"))
         category_ = Category.PRESENTATION;
      else
         throw new Error("Invalid AppCommand category '" + category + "'");
   }

   /**
    * Hides the command and makes sure it never comes back.
    */
   public void remove()
   {
      setVisible(false);
      removed_ = true;
   }

   public String getDesc()
   {
      return desc_;
   }

   public String getTooltip()
   {
      String desc = StringUtil.notNull(getDesc());
      String shortcut = getShortcutPrettyHtml();
      shortcut = StringUtil.isNullOrEmpty(shortcut) 
                 ? "" 
                 : "(" + DomUtils.htmlToText(shortcut) + ")";

      String result = (desc + " " + shortcut).trim();
      return result.length() == 0 ? null : result;
   }
   
   public String getId()
   {
      return id_;
   }

   // Called by CommandBundleGenerator
   public void setId(String id)
   {
      id_ = id;
   }

   public void setDesc(String desc)
   {
      desc_ = desc;
   }

   public String getLabel()
   {
      return label_;
   }

   public void setLabel(String label)
   {
      label_ = label;
   }

   public String getButtonLabel()
   {
      if (buttonLabel_ != null)
         return buttonLabel_;
      return getLabel();
   }

   public void setButtonLabel(String buttonLabel)
   {
      buttonLabel_ = buttonLabel;
   }

   public String getMenuLabel(boolean useMnemonics)
   {
      if (menuLabel_ != null)
      {
         return AppMenuItem.replaceMnemonics(menuLabel_, useMnemonics ? "&" : "");
      }
      return getLabel();
   }
   
   public void setMenuLabel(String menuLabel)
   {
      menuLabel_ = menuLabel;
   }

   @Override
   public ImageResource getImageResource()
   {
      if (isCheckable())
      {
         return isChecked() ? 
            ThemeResources.INSTANCE.menuCheck() :
            null;
      } 
      else
      {
         return imageResource_;
      }
   }
   
   @Override
   public void addRenderedImage(Image image)
   {
   }

   public void setImageResource(ImageResource imageResource)
   {
      imageResource_ = imageResource;
   }
   
   public void setRightImage(ImageResource image)
   {
      setRightImage(image, null);
   }
   
   public void setRightImage(ImageResource image, String desc)
   {
      rightImage_ = image;
      rightImageDesc_ = desc;
   }

   public HandlerRegistration addHandler(CommandHandler handler)
   {
      return handlers_.addHandler(CommandEvent.TYPE, handler);
   }

   public HandlerRegistration addEnabledChangedHandler(
         EnabledChangedHandler handler)
   {
      return handlers_.addHandler(EnabledChangedEvent.TYPE, handler);
   }

   public HandlerRegistration addVisibleChangedHandler(
         VisibleChangedHandler handler)
   {
      return handlers_.addHandler(VisibleChangedEvent.TYPE, handler);
   }

   public void onClick(ClickEvent event)
   {
      execute();
   }

   public ToolbarButton createToolbarButton()
   {
      return createToolbarButton(true);
   }
   
   public ToolbarButton createToolbarButton(boolean synced)
   {
      CommandToolbarButton button = new CommandToolbarButton(getButtonLabel(),
                                                             this, 
                                                             this, 
                                                             synced);
      if (getTooltip() != null)
         button.setTitle(getTooltip());
      return button;
   }

   public MenuItem createMenuItem(boolean mainMenu)
   {
      return new AppMenuItem(this, mainMenu);
   }

   public String getMenuHTML(boolean mainMenu)
   {
      String label = getMenuLabel(false);
      String shortcut = shortcut_ != null ? shortcut_.toString(true) : "";

      return formatMenuLabel(
            getImageResource(), label, shortcut, rightImage_, rightImageDesc_);
   }
   
   public static String formatMenuLabel(ImageResource icon, 
         String label,
         String shortcut)
   {
      return formatMenuLabel(icon, label, false, shortcut);
   }
   
   public static String formatMenuLabel(ImageResource icon, 
         String label,
         boolean html,
         String shortcut)
   {
      return formatMenuLabel(icon, label, html, shortcut, null, null);
   }
   
   public static String formatMenuLabel(ImageResource icon, 
         String label,
         String shortcut,
         ImageResource rightImage,
         String rightImageDesc)
   {
      return formatMenuLabel(icon, label, false, shortcut, rightImage, rightImageDesc);
   }

   public static String formatMenuLabel(ImageResource icon, 
                                         String label,
                                         boolean html,
                                         String shortcut,
                                         ImageResource rightImage,
                                         String rightImageDesc)
   {
      return formatMenuLabel(icon, 
                             label, 
                             html,
                             shortcut, 
                             null, 
                             rightImage,
                             rightImageDesc);
   }
   
   public static String formatMenuLabel(ImageResource icon, 
         String label,
         String shortcut, 
         Integer iconOffsetY)
   {
      return formatMenuLabel(icon, label, false, shortcut, iconOffsetY);
   }
   
   public static String formatMenuLabel(ImageResource icon, 
                                        String label,
                                        boolean html,
                                        String shortcut, 
                                        Integer iconOffsetY)
   {
      return formatMenuLabel(icon, label, html, shortcut, iconOffsetY, null, null);
   }
   
   public static String formatMenuLabel(ImageResource icon, 
                                         String label,
                                         boolean html,
                                         String shortcut, 
                                         Integer iconOffsetY,
                                         ImageResource rightImage,
                                         String rightImageDesc)
   {
      StringBuilder text = new StringBuilder();
      int topOffset = -10;
      if (iconOffsetY != null)
         topOffset += iconOffsetY;
      text.append("<table border=0 cellpadding=0 cellspacing=0 width='100%'><tr>");

      text.append("<td width=\"25\"><div style=\"width: 25px; margin-top: " +
                  topOffset + "px; margin-bottom: -10px\">");
      if (icon != null)
      {
         text.append(AbstractImagePrototype.create(icon).getHTML());
      }
      else
      {
         text.append("<br/>");
      }
      text.append("</div></td>");

      label = StringUtil.notNull(label);
      if (!html)
         label = DomUtils.textToHtml(label);
      text.append("<td>" + label + "</td>");
      if (rightImage != null)
      {
         SafeHtml imageHtml = createRightImageHtml(rightImage, rightImageDesc);
         text.append("<td align=right width=\"25\"><div style=\"width: 25px; float: right; margin-top: -7px; margin-bottom: -10px;\">" + imageHtml.asString() + "</div></td>");
      }
      else if (shortcut != null)
      {
         text.append("<td align=right nowrap>&nbsp;&nbsp;&nbsp;&nbsp;" + shortcut + "</td>");
      }
      text.append("</tr></table>");

      return text.toString();
   }
   
   public KeyboardShortcut getShortcut()
   {
      return shortcut_;
   }
   
   public KeySequence getKeySequence()
   {
      if (shortcut_ == null)
         return null;
      
      return shortcut_.getKeySequence();
   }

   public void setShortcut(KeyboardShortcut shortcut)
   {
      shortcut_ = shortcut;
   }
   
   public String getShortcutRaw()
   {
      return shortcut_ != null ? shortcut_.toString(false) : null;
   }

   public String getShortcutPrettyHtml()
   {
      return shortcut_ != null ? shortcut_.toString(true) : null;
   }

   public boolean getExecutedFromShortcut()
   {
      return executedFromShortcut_;
   }
   
   public static void disableNoHandlerAssertions()
   {
      enableNoHandlerAssertions_ = false;
   }
   
   private static SafeHtml createRightImageHtml(ImageResource image, 
                                                String desc)
   {
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      sb.append(SafeHtmlUtil.createOpenTag("img",
        "class", ThemeStyles.INSTANCE.menuRightImage(),
        "title", StringUtil.notNull(desc),
        "src", image.getSafeUri().asString()));
      sb.appendHtmlConstant("</img>");   
      return sb.toSafeHtml();
   }
   
   private boolean enabled_ = true;
   private boolean visible_ = true;
   private boolean removed_ = false;
   private boolean preventShortcutWhenDisabled_ = true;
   private boolean checkable_ = false;
   private boolean checked_ = false;
   private String windowMode_ = "any";
   private final HandlerManager handlers_ = new HandlerManager(this);
   private boolean rebindable_ = true;
   private Category category_ = Category.WORKBENCH;

   private String label_ = null;
   private String buttonLabel_ = null;
   private String menuLabel_ = null;
   private String desc_;
   private ImageResource imageResource_;
   private KeyboardShortcut shortcut_;
   private String id_;
   private ImageResource rightImage_ = null;
   private String rightImageDesc_ = null;
   
   private boolean executedFromShortcut_ = false;
 
   private static boolean enableNoHandlerAssertions_ = true;
   private static final String WINDOW_MODE_MAIN = "main";
   private static final String WINDOW_MODE_BACKGROUND = "background";
}
