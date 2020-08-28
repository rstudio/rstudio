/*
 * VirtualScrollerManager.java
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
package org.rstudio.core.client.virtualscroller;

import com.google.gwt.core.client.JavaScriptException;

import java.util.Date;
import java.util.HashMap;
import com.google.gwt.dom.client.Element;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.theme.res.ThemeStyles;

public class VirtualScrollerManager
{
   public static void init()
   {
      if (!initialized_ && !loading_)
      {
         scrollers_ = new HashMap<>();
         loading_ = true;
         virtualScrollerLoader_.addCallback(() -> {
            loading_ = false;
            try
            {
               initialized_ = true;
            }
            catch (JavaScriptException e)
            {
               // If we get an error initializing the virtualScroller, log
               // and no-op the error to continue non-virtualized
               Debug.log(e.getMessage());
            }
         });
      }
   }

   public static void append(Element parent, Element content)
   {
      // wholly degenerate case
      if (parent == null) {
         return;
      }

      // ensure that the element is added to be turned into a bucket later
      if (!initialized_) {
         parent.appendChild(content);
         return;
      }

      parent = getVirtualScrollerAncestor(parent);

      if (parent.getAttribute(scrollerAttribute_) == null || parent.getAttribute(scrollerAttribute_).length() == 0)
         parent.setAttribute(scrollerAttribute_, "vs_" + new Date().getTime());

      if (scrollers_.get(parent.getAttribute(scrollerAttribute_)) == null)
      {
         VirtualScrollerNative vs = new VirtualScrollerNative();
         vs.setup(parent, ThemeStyles.INSTANCE.visuallyHidden());
         scrollers_.put(parent.getAttribute(scrollerAttribute_), vs);
      }

      scrollers_.get(parent.getAttribute(scrollerAttribute_)).append(content);
   }

   public static void clear(Element parent)
   {
      // if we don't have a connection to that parent there's nothing to clear
      if (!initialized_ || parent == null )
         return;

      parent = getVirtualScrollerAncestor(parent);

      if (scrollers_.get(parent.getAttribute(scrollerAttribute_)) == null)
         return;

      scrollers_.get(parent.getAttribute(scrollerAttribute_)).clear();
   }

   public static Element getCurBucket(Element parent)
   {
      if (scrollerForElement(parent) == null) return null;

      return scrollerForElement(parent).getCurBucket();
   }

   public static VirtualScrollerNative scrollerForElement(Element parent)
   {
      if (!initialized_ || parent == null) return null;

      return scrollers_.get(parent.getAttribute(scrollerAttribute_));
   }

   public static Element getVirtualScrollerAncestor(Element parent)
   {
      // Unfortunately we can't trust the calling code to always pass us the top level container
      // element. There are cases where the VirtualConsole will initialize itself multiple times
      // and pass in elements that are not direct descendants of the container.
      Element ancestor = parent;
      while (ancestor != null)
      {
         if (ancestor.getAttribute(scrollerAttribute_).length() > 0)
         {
            parent = ancestor;
            ancestor = null;
         } else
         {
            ancestor = ancestor.getParentElement();
         }
      }

      return parent;
   }

   private static final ExternalJavaScriptLoader virtualScrollerLoader_ =
      new ExternalJavaScriptLoader(VirtualScrollerResources.INSTANCE.virtualscrollerjs().getSafeUri().asString());

   private static HashMap<String, VirtualScrollerNative> scrollers_;
   private static boolean initialized_ = false;
   private static boolean loading_ = false;

   private static final String scrollerAttribute_ = "scroller_id";
}
