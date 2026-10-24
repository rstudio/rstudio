/*
 * ModalDialogTracker.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.command.impl.DesktopMenuCallback;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;

public class ModalDialogTracker
{
   public static void onShow(PopupPanel panel)
   {
      ensureVisible(panel);
      markModal(panel.getElement());
      dialogStack_.add(panel);

      if (Desktop.hasDesktopFrame()) {
         if (BrowseCap.isElectron())
            Desktop.getFrame().setNumGwtModalsShowing(numModalsShowing());
         DesktopMenuCallback.setMainMenuEnabled(false);
      }

      updateInert(true);
   }

   public static boolean isTopMost(PopupPanel panel)
   {
      return !dialogStack_.isEmpty() &&
             dialogStack_.get(dialogStack_.size() - 1) == panel;
   }

   public static void onHide(PopupPanel panel)
   {
      dialogStack_.removeIf(panel::equals);
      
      if (Desktop.hasDesktopFrame()) {
         int numModals = numModalsShowing();
         if (BrowseCap.isElectron())
            Desktop.getFrame().setNumGwtModalsShowing(numModals);
         if (numModals == 0)
            DesktopMenuCallback.setMainMenuEnabled(true);
      }
      updateInert(false);
   }

   public static int numModalsShowing()
   {
      return dialogStack_.size();
   }

   /**
    * Hide every modal dialog currently in the stack. Used during application
    * shutdown so an open modal cannot prevent the quit path from completing.
    */
   public static void dismissAllModalDialogs()
   {
      // Iterate over a copy: hide() triggers onUnload() which calls onHide()
      // and mutates dialogStack_.
      List<PopupPanel> dialogs = new ArrayList<>(dialogStack_);
      for (PopupPanel dialog : dialogs)
      {
         try
         {
            dialog.hide();
         }
         catch (Exception e)
         {
            Debug.logException(e);
         }
      }
   }

   /**
    * Attempt to dispatch an aria-live message to the topmost popup that supports it, if any.
    * 
    * @param message message to announce
    * @param delayMs delay before announcing
    * @param severity polite or assertive announcement
    * @return true if message was dispatched, false if no popups support status messages
    */
   public static boolean dispatchAriaLiveStatus(String message, int delayMs, Severity severity)
   {
      for (int i = dialogStack_.size() - 1; i >= 0; i--)
      {
         if (dialogStack_.get(i) instanceof AriaLiveStatusReporter)
         {
            ((AriaLiveStatusReporter) dialogStack_.get(i)).reportStatus(message, delayMs, severity);
            return true;
         }
      }
      return false;
   }
   
   public static List<PopupPanel> getModalDialogs()
   {
      return dialogStack_;
   }

   private static void updateInert(boolean addedPopup)
   {
      assert(numModalsShowing() >= 0);

      if (numModalsShowing() == 0)
      {
         // closed last popup, reenable main window
         restoreBodyChildren();
         setInertMainWindow(false);
      }
      else if (numModalsShowing() == 1 && addedPopup)
      {
         // added first popup, disable main window
         setInertMainWindow(true);
         setInertBodyChildren(getTopMostElement());
      }
      else if (addedPopup)
         A11y.setInert(getSecondTopMostElement(), true); // disable previous topmost
      else
         A11y.setInert(getTopMostElement(), false); // enable newly exposed topmost
   }

   private static Element getTopMostElement()
   {
      if (dialogStack_.isEmpty())
         return null;
      return dialogStack_.get(dialogStack_.size() - 1).getElement();
   }

   private static Element getSecondTopMostElement()
   {
      if (dialogStack_.size() < 2)
         return null;
      return dialogStack_.get(dialogStack_.size() - 2).getElement();
   }

   /**
    * The rest of the page is made inert while a modal shows; aria-modal tells assistive
    * technology so from the dialog itself. It's only valid on the dialog roles, so panels
    * with any other role are left alone.
    */
   private static void markModal(Element el)
   {
      String role = el.getAttribute("role");
      if (role.equals("dialog") || role.equals("alertdialog"))
         el.setAttribute("aria-modal", "true");
   }

   private static void setInertMainWindow(boolean inert)
   {
      Element ideWrapper = DOM.getElementById("rstudio_container");
      Element ideFrame = DOM.getElementById("rstudio");
      Element satWrapper = DOM.getElementById(ElementIds.getElementId(ElementIds.SATELLITE_PANEL));

      if (ideWrapper != null)
         A11y.setInert(ideWrapper, inert);
      if (ideFrame != null)
         A11y.setInert(ideFrame, inert);
      if (satWrapper != null)
         A11y.setInert(satWrapper, inert);
   }

   /**
    * Non-modal popups are attached to the body, outside the main window's containers,
    * so make any already showing inert too. Only `inert` is set because it also hides
    * them from assistive technology, and restoring it can't clobber an existing
    * aria-hidden. Popups opened later, e.g. from the dialog itself, stay usable.
    */
   private static void setInertBodyChildren(Element dialog)
   {
      Element child = Document.get().getBody().getFirstChildElement();
      while (child != null)
      {
         if (child != dialog && !child.hasAttribute("inert"))
         {
            child.setAttribute("inert", "");
            inertBodyChildren_.add(child);
         }
         child = child.getNextSiblingElement();
      }
   }

   private static void restoreBodyChildren()
   {
      for (Element child : inertBodyChildren_)
         child.removeAttribute("inert");
      inertBodyChildren_.clear();
   }

   private static void ensureVisible(Panel panel)
   {
      try
      {
         ensureVisibleImpl(panel);
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
   }

   private static void ensureVisibleImpl(Panel panel)
   {
      Style styles = DomUtils.getComputedStyles(panel.getElement());
      Integer zIndex = StringUtil.parseInt(styles.getZIndex(), 1000);
      panel.getElement().getStyle().setZIndex(zIndex + dialogStack_.size());
   }

   private static final List<PopupPanel> dialogStack_ = new ArrayList<>();
   private static final List<Element> inertBodyChildren_ = new ArrayList<>();
}
