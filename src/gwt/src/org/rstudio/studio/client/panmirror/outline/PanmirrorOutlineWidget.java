/*
 * PanmirrorOutlineWidget.java
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


package org.rstudio.studio.client.panmirror.outline;


import java.util.ArrayList;
import java.util.Iterator;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.panmirror.PanmirrorSelection;
import org.rstudio.studio.client.panmirror.events.PanmirrorOutlineNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.DocumentOutlineWidget;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

import jsinterop.base.Js;

public class PanmirrorOutlineWidget extends Composite
   implements PanmirrorOutlineNavigationEvent.HasPanmirrorOutlineNavigationHandlers
{
   public PanmirrorOutlineWidget()
   {
    
      emptyPlaceholder_ = new DocumentOutlineWidget.EmptyPlaceholder();
      
      container_ = new DockLayoutPanel(Unit.PX);
      container_.addStyleName(outlineStyles_.container());
      
      resizer_ = new DocumentOutlineWidget.VerticalSeparator();
      container_.addWest(resizer_, 4);
      resizer_.pad();
       
      tree_ = new Tree();
      tree_.addStyleName(outlineStyles_.tree());
      Roles.getTreeRole().setAriaLabelProperty(tree_.getElement(), "Document Outline");
      
      panel_ = new FlowPanel();
      panel_.addStyleName(outlineStyles_.panel());
      panel_.add(tree_);
      
      container_.add(panel_);
      
      RStudioGinjector.INSTANCE.injectMembers(this);
         
      initWidget(container_);
      
      addStyleName("ace_editor_theme");
   }
  
   @Override
   public HandlerRegistration addPanmirrorOutlineNavigationHandler(PanmirrorOutlineNavigationEvent.Handler handler)
   {
      return handlers_.addHandler(PanmirrorOutlineNavigationEvent.getType(), handler);
   }
   
   public void updateOutline(PanmirrorOutlineItem[] outline)
   {
      outline_ = flattenOutline(outline);
      rebuildOutline();
      updateActiveItem();
   }
   
   public void updateSelection(PanmirrorSelection selection)
   {
      selection_ = selection;
      updateActiveItem();
   }
   
   public Widget getResizer()
   {
      return resizer_;
   }
   
   public void setAriaVisible(boolean visible)
   {
      if (visible)
         A11y.setARIAVisible(getElement());
      else
         A11y.setARIAHidden(getElement());
   }   
   
   private void rebuildOutline()
   {
      // set empty placeholder if the outline is empty
      if (outline_.size() == 0)
      {
         setActiveWidget(emptyPlaceholder_);
         return;
      }
      
      // otherwise, set the tree as active 
      setActiveWidget(tree_);
      
      // determine the minimum level
      outlineMinLevel_ = Integer.MAX_VALUE;
      for (int i=0; i<outline_.size(); i++)
      {
         PanmirrorOutlineItem item = Js.uncheckedCast(outline_.get(i));
         outlineMinLevel_ = Math.min(outlineMinLevel_, item.level);
      }
     
      if (outlineMinLevel_ == Integer.MAX_VALUE)
         outlineMinLevel_ = 1;
     
         
      // if the outline and existing tree have diffent sizes then rebuild
      if (outline_.size() != tree_.getItemCount())
      {
         tree_.clear();
         for (int i=0; i<outline_.size(); i++)
         {
            PanmirrorOutlineItem item = Js.uncheckedCast(outline_.get(i));
            addToTree(item);
         }
      }
      else // otherwise update items
      {
         Iterator<TreeItem> treeIter = tree_.treeItemIterator();
         for (int i=0; i<outline_.size(); i++)
         {
            PanmirrorOutlineItem item = Js.uncheckedCast(outline_.get(i));
            OutlineTreeItem outlineTreeItem = (OutlineTreeItem)treeIter.next();
            outlineTreeItem.getEntry().update(item);
         }
      }
   }
   
   private void addToTree(PanmirrorOutlineItem item)
   {
      OutlineTreeEntry entry = new OutlineTreeEntry(item);
      OutlineTreeItem treeItem = new OutlineTreeItem(entry);
      setTreeItemStyles(treeItem);
      tree_.addItem(treeItem);
   }
   
   private ArrayList<PanmirrorOutlineItem>  flattenOutline(PanmirrorOutlineItem[] items)
   {
      ArrayList<PanmirrorOutlineItem> flattenedItems = new ArrayList<PanmirrorOutlineItem>();
      doFlattenOutline(items, flattenedItems);
      return flattenedItems;
   }
   
   private void doFlattenOutline(PanmirrorOutlineItem[] items,  ArrayList<PanmirrorOutlineItem> flattenedItems)
   {
      for (int i=0; i<items.length; i++)
      {
         PanmirrorOutlineItem item = items[i];
         if (item.type == PanmirrorOutlineItemType.Heading && 
               !StringUtil.isNullOrEmpty(item.title))
         {
            flattenedItems.add(item);
            doFlattenOutline(item.children, flattenedItems);
         }
      }
   }
   
   private void setActiveWidget(Widget widget)
   {
      panel_.clear();
      panel_.add(widget);
   }
   
   private void resetTreeStyles()
   {
      for (int i = 0; i < tree_.getItemCount(); i++)
        setTreeItemStyles((OutlineTreeItem)tree_.getItem(i));
   }
   
   private void setTreeItemStyles(OutlineTreeItem treeItem)
   {
      PanmirrorOutlineItem item = treeItem.getEntry().getItem();
      treeItem.addStyleName(outlineStyles_.node());
      DomUtils.toggleClass(treeItem.getElement(), outlineStyles_.activeNode(), isActiveItem(item));
   }
   
   private boolean isActiveItem(PanmirrorOutlineItem item)
   {
      return item == activeItem_;
   }
   
   private void updateActiveItem()
   { 
      activeItem_ = null; 
      
      if (outline_ == null || outline_.size() == 0)
         return;
      
      if (selection_ == null)
         return;
      
      for (int i = 0; i < tree_.getItemCount(); i++) 
      {
         OutlineTreeItem treeItem = (OutlineTreeItem)tree_.getItem(i);
         PanmirrorOutlineItem item = treeItem.getEntry().getItem();
         if (item.navigation_id.equals(selection_.navigation_id)) 
         {
            activeItem_ = item;
            break;
         } 
      }
        
      resetTreeStyles();
   }
   
   private class OutlineTreeItem extends TreeItem
   {
      public OutlineTreeItem(OutlineTreeEntry entry)
      {
         super(entry);
         entry_ = entry;
      }
      
      public OutlineTreeEntry getEntry()
      {
         return entry_;
      }
      
      private final OutlineTreeEntry entry_;
   }
   
   private class OutlineTreeEntry extends Composite
   {
      public OutlineTreeEntry(PanmirrorOutlineItem item)
      {
         item_ = item;
         FlowPanel panel = new FlowPanel();
         
         setIndent(item.level);
         setLabel(item);
         
         panel.add(indent_);
         panel.add(label_);
         
         panel.addDomHandler(new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               event.stopPropagation();
               event.preventDefault();
               
               // Defer focus so it occurs after click has been fully handled
               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     PanmirrorOutlineNavigationEvent.fire(PanmirrorOutlineWidget.this, item_.navigation_id);
                  }
               });
            }
         }, ClickEvent.getType());
         
         initWidget(panel);
      }
      
      private void setLabel(PanmirrorOutlineItem item)
      {
         String text = item.title;
       
         if (label_ == null)
            label_ = new Label(text);
         else
            label_.setText(text);
         
         label_.addStyleName(outlineStyles_.nodeLabel());
         label_.addStyleName(ThemeStyles.INSTANCE.handCursor());
      }
      
      private void setIndent(int depth)
      {
         depth = depth - PanmirrorOutlineWidget.this.outlineMinLevel_;
         String text = StringUtil.repeat("&nbsp;", depth * 2);
         if (indent_ == null)
            indent_ = new HTML(text);
         else
            indent_.setHTML(text);

         indent_.addStyleName(outlineStyles_.nodeLabel());
         indent_.getElement().getStyle().setFloat(Style.Float.LEFT);
      }
      
      public void update(PanmirrorOutlineItem item)
      {
         item_ = item;
         setLabel(item);
         setIndent(item.level);
      }
      
      public PanmirrorOutlineItem getItem()
      {
         return item_;
      }
      
      PanmirrorOutlineItem item_;
      private HTML indent_;
      private Label label_;
   }
   
   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }
  
  
   private final static DocumentOutlineWidget.Styles outlineStyles_ = DocumentOutlineWidget.RES.styles();
   
   private ArrayList<PanmirrorOutlineItem> outline_ = null;
   private int outlineMinLevel_ = 1;
   private PanmirrorSelection selection_ = null;
   private PanmirrorOutlineItem activeItem_ = null;
   
   private final Tree tree_;
   private final DockLayoutPanel container_;
   private final FlowPanel panel_;
   private final DocumentOutlineWidget.VerticalSeparator resizer_;
   private final DocumentOutlineWidget.EmptyPlaceholder emptyPlaceholder_;
    
   private final HandlerManager handlers_ = new HandlerManager(this);
  
}
