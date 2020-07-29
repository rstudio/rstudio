/*
 * AppCommand.java
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
package org.rstudio.core.client.command;

import com.google.gwt.aria.client.MenuitemRole;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuItem;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.impl.DesktopMenuCallback;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudio;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;

public class AppCommand implements Command, ClickHandler, ImageResourceProvider
{
   private class CommandToolbarButton extends ToolbarButton implements
         EnabledChangedHandler, VisibleChangedHandler
   {
      public CommandToolbarButton(String buttonLabel, String buttonTitle,
            ImageResourceProvider imageResourceProvider, AppCommand command,
            boolean synced)
      {
         super(buttonLabel, buttonTitle, imageResourceProvider, command);
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
         ElementIds.assignElementId(getElement(), "tb_" + ElementIds.idSafeString(getId()));
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

      public void onEnabledChanged(EnabledChangedEvent event)
      {
         setEnabled(command_.isEnabled());
      }

      public void onVisibleChanged(VisibleChangedEvent event)
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

   public AppCommand()
   {
      if (Desktop.hasDesktopFrame())
      {
         // If this command is destined for the main window, do not allow
         // satellite windows to alter the global state of the command.
         //
         // Note that we can't use the more robust isCurrentWindowSatellite()
         // here since AppCommand callbacks run on app init (before the
         // satellite callbacks are wired up).
         // 
         // We also need to execute this check at runtime (not in the constructor)
         // since windowMode is not set when the AppCommand is constructed.
         addEnabledChangedHandler((event) -> 
         {
            if (!StringUtil.equals(getWindowMode(), WINDOW_MODE_MAIN) ||
                StringUtil.isNullOrEmpty(RStudio.getSatelliteView()))
            {
               DesktopMenuCallback.setCommandEnabled(id_, enabled_); 
            }
         });
         addVisibleChangedHandler((event) -> 
         {
            if (!StringUtil.equals(getWindowMode(), WINDOW_MODE_MAIN) ||
                StringUtil.isNullOrEmpty(RStudio.getSatelliteView()))
            {
               DesktopMenuCallback.setCommandVisible(id_, visible_); 
            }
         });
      }
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
      // in the a different window, execute the command there instead
      Satellite satellite = RStudioGinjector.INSTANCE.getSatellite();
      if (getWindowMode() != WINDOW_MODE_ANY &&
          Satellite.isCurrentWindowSatellite() &&
          satellite.getSatelliteName() != getWindowMode())
      {
         if (getWindowMode() == WINDOW_MODE_MAIN)
         {
            // raise the main window if it's not a background command
            satellite.focusMainWindow();
         }
         else if (getWindowMode() == WINDOW_MODE_BACKGROUND &&
                  Desktop.hasDesktopFrame())
         {
            // for background commands, we still want the main window to be
            // as visible as possible, so bring it up behind the current window
            // (this is of course only possible in desktop mode)
            Desktop.getFrame().bringMainFrameBehindActive();
         }

         // satellites don't fire commands peer-to-peer--route it to the main
         // window for processing
         SatelliteManager mgr = RStudioGinjector.INSTANCE.getSatelliteManager();
         mgr.dispatchCommand(this, null);
         return;
      }

      if (enableNoHandlerAssertions_)
      {
         assert handlers_.getHandlerCount(CommandEvent.TYPE) > 0
                  : "AppCommand executed but nobody was listening: " + getId();
      }

      CommandEvent event = new CommandEvent(this);
      RStudioGinjector.INSTANCE.getEventBus().fireEvent(event);
      handlers_.fireEvent(event);
   }

   public boolean isEnabled()
   {
      return enabled_ && isVisible(); // jcheng 06/30/2010: Hmmmm, smells weird.
   }

   /**
    * Determines whether there are any handlers established that will execute
    * when this command runs. This is useful for determining if the command
    * will do anything when executed.
    *
    * @return Whether this command has handlers.
    */
   public boolean hasCommandHandlers()
   {
      return handlers_.getHandlerCount(CommandEvent.TYPE) > 0;
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

   /**
    * Restores a command which was formerly removed. The command must still be made
    * visible and enabled in order to work.
    */
   public void restore()
   {
      removed_ = false;
   }

   public boolean isCheckable()
   {
      return checkable_;
   }

   public void setCheckable(boolean isCheckable)
   {
      if (isRadio())
         checkable_ = true;
      else
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
      if (Desktop.hasDesktopFrame())
         DesktopMenuCallback.setCommandChecked(id_, checked_);
   }

   public void setRadio(boolean isRadio)
   {
      radio_ = isRadio;
      if (radio_)
         setCheckable(true);
   }

   public boolean isRadio()
   {
      return radio_;
   }

   public MenuitemRole getMenuRole()
   {
      if (isRadio())
         return Roles.getMenuitemradioRole();
      else if (isCheckable())
         return Roles.getMenuitemcheckboxRole();
      else
         return Roles.getMenuitemRole();
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

   public enum Context
   {
      Workbench, Editor, R, Cpp, PackageDevelopment, RMarkdown,
      Markdown, Sweave, Help, VCS, Packrat, Renv, RPresentation,
      Addin, Viewer, History, Tutorial, Diagnostics, Import, Files;

      @Override
      public String toString()
      {
         if (this == Cpp)
            return "C / C++";

         return StringUtil.prettyCamel(super.toString());
      }
   }

   public Context getContext()
   {
      return context_;
   }

   public void setContext(String context)
   {
      String lower = context.toLowerCase();
      if (lower.equals("workbench"))
         context_ = Context.Workbench;
      else if (lower.equals("editor"))
         context_ = Context.Editor;
      else if (lower.equals("vcs"))
         context_ = Context.VCS;
      else if (lower.equals("r"))
         context_ = Context.R;
      else if (lower.equals("cpp"))
         context_ = Context.Cpp;
      else if (lower.equals("packagedevelopment"))
         context_ = Context.PackageDevelopment;
      else if (lower.equals("rmarkdown"))
         context_ = Context.RMarkdown;
      else if (lower.equals("sweave"))
         context_ = Context.Markdown;
      else if (lower.equals("markdown"))
         context_ = Context.Sweave;
      else if (lower.equals("help"))
         context_ = Context.Help;
      else if (lower.equals("packrat"))
         context_ = Context.Packrat;
      else if (lower.equals("renv"))
         context_ = Context.Renv;
      else if (lower.equals("presentation"))
         context_ = Context.RPresentation;
      else if (lower.equals("viewer"))
         context_ = Context.Viewer;
      else if (lower.equals("tutorial"))
         context_ = Context.Tutorial;
      else if (lower.equals("diagnostics"))
         context_ = Context.Diagnostics;
      else if (lower.equals("history"))
         context_ = Context.History;
      else if (lower.equals("import"))
         context_ = Context.Import;
      else if (lower.equals("files"))
         context_ = Context.Files;
      else
         throw new Error("Invalid AppCommand context '" + context + "'");
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
      if (Desktop.hasDesktopFrame())
         DesktopMenuCallback.setCommandLabel(id_, menuLabel_);
   }

   @Override
   public ImageResource getImageResource()
   {
      return menuImageResource(isCheckable(), isChecked(), imageResource_);
   }

   public static ImageResource menuImageResource(boolean isCheckable, boolean isChecked, ImageResource defaultImage)
   {
      if (isCheckable)
      {
         if (RStudioThemes.isFlat() && RStudioThemes.isEditorDark()) {
            return isChecked ?
               new ImageResource2x(ThemeResources.INSTANCE.menuCheckInverted2x()) :
               null;
         }
         else {
            return isChecked ?
               new ImageResource2x(ThemeResources.INSTANCE.menuCheck2x()) :
               null;
         }
      }
      else
      {
         return defaultImage;
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
                                                             getDesc(),
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

   public static String formatMenuLabelWithStyle(ImageResource icon,
                                                 String label,
                                                 String shortcut,
                                                 String styleName)
   {
      return formatMenuLabel(icon,
            label,
            false,
            shortcut,
            null,
            null,
            null,
            styleName);
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
                             rightImageDesc,
                             null);
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
      return formatMenuLabel(icon, label, html, shortcut, iconOffsetY, null, null, null);
   }

   public static String formatMenuLabel(ImageResource icon,
                                         String label,
                                         boolean html,
                                         String shortcut,
                                         Integer iconOffsetY,
                                         ImageResource rightImage,
                                         String rightImageDesc,
                                         String styleName)
   {
      StringBuilder text = new StringBuilder();
      int topOffset = -2;
      if (iconOffsetY != null)
         topOffset += iconOffsetY;
      text.append("<table role=\"presentation\"");
      if (label != null)
      {
         text.append("id=\"" + ElementIds.idFromLabel(label) + "_command\" ");
      }

      if (styleName != null)
      {
         text.append("class='" + styleName + "' ");
      }

      text.append("border=0 cellpadding=0 cellspacing=0 width='100%'><tr>");

      text.append("<td width=\"25\" style=\"vertical-align: top\">" +
                  "<div style=\"width: 25px; margin-top: " +
                  topOffset + "px; margin-bottom: -10px\">");
      if (icon != null)
      {
         SafeHtml imageHtml = createMenuImageHtml(icon);
         text.append(imageHtml.asString());
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
         return new KeySequence();

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

   public String summarize()
   {
      if (!StringUtil.isNullOrEmpty(getMenuLabel(false)))
         return getMenuLabel(false);
      else if (!StringUtil.isNullOrEmpty(getButtonLabel()))
         return getButtonLabel();
      else if (!StringUtil.isNullOrEmpty(getDesc()))
         return getDesc();
      else
         return "(no description)";
   }

   private static SafeHtml createRightImageHtml(ImageResource image,
                                                String desc)
   {
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      sb.append(SafeHtmlUtil.createOpenTag("img",
        "class", ThemeStyles.INSTANCE.menuRightImage(),
        "title", StringUtil.notNull(desc),
        "aria-label", StringUtil.notNull(desc),
        "width", Integer.toString(image.getWidth()),
        "height", Integer.toString(image.getHeight()),
        "src", image.getSafeUri().asString(),
        "alt", ""));
      sb.appendHtmlConstant("</img>");
      return sb.toSafeHtml();
   }

   private static SafeHtml createMenuImageHtml(ImageResource image)
   {
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      sb.append(SafeHtmlUtil.createOpenTag("img",
        "width", Integer.toString(image.getWidth()),
        "height", Integer.toString(image.getHeight()),
        "src", image.getSafeUri().asString(),
        "alt", ""));
      sb.appendHtmlConstant("</img>");
      return sb.toSafeHtml();
   }

   private boolean enabled_ = true;
   private boolean visible_ = true;
   private boolean removed_ = false;
   private boolean checkable_ = false;
   private boolean radio_ = false;
   private boolean checked_ = false;
   private String windowMode_ = "any";
   private final HandlerManager handlers_ = new HandlerManager(this);
   private boolean rebindable_ = true;
   private Context context_ = Context.Workbench;

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

   public static final String WINDOW_MODE_BACKGROUND = "background";
   public static final String WINDOW_MODE_MAIN = "main";
   public static final String WINDOW_MODE_ANY = "any";
}
