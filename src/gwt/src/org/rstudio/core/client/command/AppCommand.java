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
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuItem;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;

public class AppCommand implements Command, ClickHandler, ImageResourceProvider
{
   private class CommandToolbarButton extends ToolbarButton implements
         EnabledChangedHandler, VisibleChangedHandler
   {
      public CommandToolbarButton(String buttonLabel,
            ImageResourceProvider imageResourceProvider, AppCommand command)
      {
         super(buttonLabel, imageResourceProvider, command);
         command_ = command;
      }

      @Override
      protected void onAttach()
      {
         setEnabled(command_.isEnabled());
         setVisible(command_.isVisible());
         handlerReg_ = command_.addEnabledChangedHandler(this);
         handlerReg2_ = command_.addVisibleChangedHandler(this);

         parentToolbar_ = getParentToolbar();

         super.onAttach();
      }

      @Override
      protected void onDetach()
      {
         super.onDetach();

         handlerReg_.removeHandler();
         handlerReg2_.removeHandler();
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
      private HandlerRegistration handlerReg_;
      private HandlerRegistration handlerReg2_;
      private Toolbar parentToolbar_;
   }

   public AppCommand()
   {
   }

   public void execute()
   {
      assert enabled_ : "AppCommand executed when it was not enabled";
      if (!enabled_)
         return;
      assert visible_ : "AppCommand executed when it was not visible";
      if (!visible_)
         return;

      if (isCheckable())
         setChecked(!isChecked());

      if (enableNoHandlerAssertions_)
      {
         assert handlers_.getHandlerCount(CommandEvent.TYPE) > 0 : "AppCommand executed but nobody was listening";
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
      shortcut = StringUtil.isNullOrEmpty(shortcut) ? "" : "("
            + DomUtils.htmlToText(shortcut) + ")";

      String result = (desc + " " + shortcut).trim();
      return result.length() == 0 ? null : result;
   }

   // Called by CommandBundleGenerator
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
         return AppMenuItem.replaceMnemonics(menuLabel_, useMnemonics ? "&"
               : "");
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
         if (isChecked())
         {
            return ThemeResources.INSTANCE.switchOn();
         }
         else
         {
            return ThemeResources.INSTANCE.switchOff();
         }
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
      CommandToolbarButton button = new CommandToolbarButton(getButtonLabel(),
            this, this);
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
            getImageResource(), label, shortcut);
   }
   
   public static String formatMenuLabel(ImageResource icon, String label,
         String shortcut)
   {
      StringBuilder text = new StringBuilder();

      text.append("<table border=0 cellpadding=0 cellspacing=0 width='100%'><tr>");

      text.append("<td width=\"25\"><div style=\"width: 25px; margin-top: -10px; margin-bottom: -10px\">");
      if (icon != null)
      {
         text.append(AbstractImagePrototype.create(icon).getHTML());
      } 
      else
      {
         text.append("<br/>");
      }
      text.append("</div></td>");

      text.append("<td>" + DomUtils.textToHtml(StringUtil.notNull(label))
            + "</td>");
      if (shortcut != null)
         text.append("<td align=right nowrap>&nbsp;&nbsp;&nbsp;&nbsp;"
               + shortcut + "</td>");
      text.append("</tr></table>");

      return text.toString();
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

   public static void disableNoHandlerAssertions()
   {
      enableNoHandlerAssertions_ = false;
   }
   
   private boolean enabled_ = true;
   private boolean visible_ = true;
   private boolean removed_ = false;
   private boolean preventShortcutWhenDisabled_ = true;
   private boolean checkable_ = false;
   private boolean checked_ = false;
   private final HandlerManager handlers_ = new HandlerManager(this);

   private String label_;
   private String buttonLabel_;
   private String menuLabel_;
   private String desc_;
   private ImageResource imageResource_;
   private KeyboardShortcut shortcut_;
 
   private static boolean enableNoHandlerAssertions_ = true;
}
