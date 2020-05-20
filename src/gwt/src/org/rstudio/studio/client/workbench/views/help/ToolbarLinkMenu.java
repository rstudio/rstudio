/*
 * ToolbarLinkMenu.java
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

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.workbench.views.help.Help.LinkMenu;
import org.rstudio.studio.client.workbench.views.help.model.Link;

import java.util.ArrayList;

public class ToolbarLinkMenu implements LinkMenu
{
   public ToolbarLinkMenu(int maxLinks,
                          boolean addFromTop,
                          MenuItem[] pre,
                          MenuItem[] post)
   {
      maxLinks_ = maxLinks;
      top_ = addFromTop;
      pre_ = pre != null ? pre : new MenuItem[0];
      post_ = post != null ? post : new MenuItem[0];
      menu_ = new ToolbarPopupMenu();
      clearLinks();
   }
   
   public ToolbarPopupMenu getMenu()
   {
      return menu_;
   }

   public void addLink(Link link)
   {
      LinkMenuItem menuItem = new LinkMenuItem(link, this);
      int beforeIndex;
      if (top_)
      {
         beforeIndex = pre_.length == 0 ? 0 : pre_.length + 1;
      }
      else
      {
         beforeIndex = menu_.getItemCount();
         if (pre_.length > 0)
            beforeIndex++; // initial separator isn't counted in getItemCount()
         if (post_.length > 0)
            beforeIndex -= post_.length + 1;
         
         // some weird race condition causes beforeIndex to go negative
         beforeIndex = Math.max(0, beforeIndex);
      }

      try
      {
         menu_.insertItem(menuItem, beforeIndex);
      }
      catch (RuntimeException e)
      {
         Debug.log("beforeIndex: " + beforeIndex + ", length: " + menu_.getItemCount());
         throw e;
      }
      
      links_.add(top_ ? 0 : links_.size(), link);

      while (links_.size() > maxLinks_)
         removeLink(links_.get(top_ ? links_.size() - 1 : 0));
   }

   public void removeLink(Link link)
   {
      menu_.removeItem(new LinkMenuItem(link, this));
      links_.remove(link);
   }
   
   public boolean containsLink(Link link)
   {
      return menu_.containsItem(new LinkMenuItem(link, this));
   }
   
   public void clearLinks()
   {
      menu_.clearItems();
      for (MenuItem mi : pre_)
         menu_.addItem(mi);
      if (pre_.length > 0)
         menu_.addSeparator();
      if (post_.length > 0)
         menu_.addSeparator();
      for (MenuItem mi : post_)
         menu_.addItem(mi);
      
      links_.clear();
   }
   
   public ArrayList<Link> getLinks()
   {
      return new ArrayList<Link>(links_);
   }

   public HandlerRegistration addSelectionHandler(
                                            SelectionHandler<String> handler)
   {
      return handlers_.addHandler(SelectionEvent.getType(), handler);
   }
   
   private class LinkMenuItem extends MenuItem
   {
      public LinkMenuItem(final Link link, 
                          final ToolbarLinkMenu thiz)
      {
         super(link.getTitle(), new Command() {
            public void execute()
            {
               SelectionEvent.fire(thiz, link.getUrl());
            }
         });
         
         link_ = link;
      }
      
      @Override
      public int hashCode()
      {
         return link_.hashCode();
      }

      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         LinkMenuItem other = (LinkMenuItem) obj;
         if (link_ == null)
         {
            if (other.link_ != null)
               return false;
         } else if (!link_.equals(other.link_))
            return false;
         return true;
      }

      private final Link link_;
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   private final HandlerManager handlers_ = new HandlerManager(null);
   private final ToolbarPopupMenu menu_;
   private final MenuItem[] pre_;
   private final MenuItem[] post_;
   private final ArrayList<Link> links_ = new ArrayList<Link>();
   private final int maxLinks_;
   private boolean top_;
}
