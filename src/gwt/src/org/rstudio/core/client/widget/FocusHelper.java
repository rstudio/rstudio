/*
 * FocusHelper.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.Focusable;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.DomUtils;

import java.util.ArrayList;

/**
 * Helpers for marking first and last focusable element in a dialog or other control.
 */
public class FocusHelper
{
   public static final String firstFocusClass = "__rstudio_modal_first_focus";
   public static final String lastFocusClass = "__rstudio_modal_last_focus";

   /**
    * @param parent top-level element of dialog or control whose focus loop to manage
    */
   public FocusHelper(Element parent)
   {
      parent_ = parent;
   }

   /**
    * @param element first keyboard focusable element
    */
   public void setFirst(Element element)
   {
      removeExisting(firstFocusClass);
      element.addClassName(firstFocusClass);
   }

   /**
    * @param element last keyboard focusable element
    */
   public void setLast(Element element)
   {
      removeExisting(lastFocusClass);
      element.addClassName(lastFocusClass);
   }

   /**
    * @param element
    * @return true if element is marked as the first focusable element
    */
   public boolean isFirst(Element element)
   {
      return element.hasClassName(firstFocusClass);
   }

   /**
    * @param element
    * @return true if element is marked as last focusable element
    */
   public boolean isLast(Element element)
   {
      return element.hasClassName(lastFocusClass);
   }

   /**
    * @return true if an element in this container is marked as first
    */
   public boolean containsFirst()
   {
      return getByClass(firstFocusClass) != null;
   }

   /**
    * @return true if an element in this container is marked as last
    */
   public boolean containsLast()
   {
      return getByClass(lastFocusClass) != null;
   }

   /**
    * Set focus on first keyboard focusable element in dialog, as set by
    * refreshFocusableElements or setFirstFocusableElement.
    *
    * Invoked when Tabbing off the last control in the modal dialog to set focus back to
    * the first control, and by default to set initial focus when the dialog is shown.
    *
    * To set focus on a different control when the dialog is displayed, override
    * focusInitialControl, instead.
    */
   protected void focusFirstControl()
   {
      Element first = getByClass(firstFocusClass);
      if (first != null)
         first.focus();
   }

   /**
    * Set focus on last keyboard focusable element in dialog, as set by
    * <code>refreshFocusableElements</code> or <code>setLastFocusableElement</code>.
    */
   protected void focusLastControl()
   {
      Element last = getByClass(lastFocusClass);
      if (last != null)
         last.focus();
   }

   /**
    * Gets an ordered list of keyboard-focusable elements
    */
   @SuppressWarnings("unused")
   private ArrayList<Element> getFocusableElements()
   {
      return DomUtils.getFocusableElements(parent_);
   }

   private void removeExisting(String classname)
   {
      Element current = getByClass(classname);
      if (current != null)
         current.removeClassName(classname);
   }

   private Element getByClass(String classname)
   {
      NodeList<Element> current = DomUtils.querySelectorAll(parent_, "." + classname);
      if (current.getLength() > 1)
      {
         Debug.logWarning("Multiple controls found with class: " + classname);
         return null;
      }
      if (current.getLength() == 1)
      {
         return current.getItem(0);
      }
      return null;
   }

   public static void setFocusDeferred(final CanFocus canFocus)
   {
      Scheduler.get().scheduleDeferred(() -> canFocus.focus());
   }

   public static void setFocusDeferred(final Focusable focusable)
   {
      Scheduler.get().scheduleDeferred(() -> focusable.setFocus(true));
   }

   private final Element parent_;
}
