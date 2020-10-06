/*
 * ShortcutViewer.java
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


import com.google.inject.Provider;
import org.rstudio.core.client.widget.ShortcutInfoPanel;
import org.rstudio.core.client.widget.VimKeyInfoPanel;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

@Singleton
public class ShortcutViewer implements NativePreviewHandler
{
   public interface Binder extends CommandBinder<Commands, ShortcutViewer> {}

   @Inject
   public ShortcutViewer(
         Binder binder,
         Commands commands,
         Provider<UserPrefs> pPrefs,
         Provider<AriaLiveService> pAriaLive,
         GlobalDisplay globalDisplay)
   {
      binder.bind(commands, this);
      pPrefs_ = pPrefs;
      pAriaLive_ = pAriaLive;
      globalDisplay_ = globalDisplay;
   }

   @Handler
   public void onHelpKeyboardShortcuts()
   {
      // prevent reentry
      if (shortcutInfo_ != null)
      {
         return;
      }

      Command showAllShortcutsPage = ()->
      {
         if (Desktop.hasDesktopFrame())
            Desktop.getFrame().showKeyboardShortcutHelp();
         else
            openApplicationURL("docs/keyboard.htm");
      };

      if (pPrefs_.get().enableScreenReader().getValue())
         showAllShortcutsPage.execute();
      else
         showShortcutInfoPanel(new ShortcutInfoPanel(showAllShortcutsPage));
   }

   public void showVimKeyboardShortcuts()
   {
      // prevent reentry
      if (shortcutInfo_ != null)
      {
         return;
      }
      if (pPrefs_.get().enableScreenReader().getValue())
      {
         pAriaLive_.get().announce(AriaLiveService.INACCESSIBLE_FEATURE,
               "Vim keyboard shortcut help not screen reader accessible. Press any key to close.",
               Timing.IMMEDIATE,
               Severity.ALERT);
      }
      showShortcutInfoPanel(new VimKeyInfoPanel());
   }

   private void showShortcutInfoPanel(ShortcutInfoPanel panel)
   {
      shortcutInfo_ = panel;
      RootLayoutPanel.get().add(shortcutInfo_);
      preview_ = Event.addNativePreviewHandler(this);
   }

   @Override
   public void onPreviewNativeEvent(NativePreviewEvent event)
   {
      if (event.isCanceled())
         return;

      if (event.getTypeInt() == Event.ONKEYDOWN ||
          event.getTypeInt() == Event.ONMOUSEDOWN)
      {
         if (event.getTypeInt() == Event.ONMOUSEDOWN &&
             event.getNativeEvent().getButton() == NativeEvent.BUTTON_RIGHT)
            return;

         // Don't dismiss the dialog if the click is targeted for a child
         // of the shortcut info panel's root element
         EventTarget et = event.getNativeEvent().getEventTarget();
         if (Element.is(et) && event.getTypeInt() == Event.ONMOUSEDOWN)
         {
            Element e = Element.as(et);
            while (e != null)
            {
               if (e == shortcutInfo_.getRootElement())
                  return;
               e = e.getParentElement();
            }
         }

         // This is a keystroke or a click outside the panel; dismiss the panel
         if (shortcutInfo_ != null)
            RootLayoutPanel.get().remove(shortcutInfo_);
         shortcutInfo_ = null;
         if (preview_ != null)
            preview_.removeHandler();
         preview_ = null;
         event.cancel();
      }
   }

   private void openApplicationURL(String relativeURL)
   {
      String url = GWT.getHostPageBaseURL() + relativeURL;
      globalDisplay_.openWindow(url);
   }

   private ShortcutInfoPanel shortcutInfo_ = null;
   private HandlerRegistration preview_;

   // injected
   private final Provider<UserPrefs> pPrefs_;
   private final Provider<AriaLiveService> pAriaLive_;
   private final GlobalDisplay globalDisplay_;
}
