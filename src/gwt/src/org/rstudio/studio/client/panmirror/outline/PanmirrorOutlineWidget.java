/*
 * PanmirrorOutlineWidget.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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


import java.util.Arrays;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.panmirror.PanmirrorSelection;
import org.rstudio.studio.client.workbench.views.source.DocumentOutlineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeStyleChangedEvent;

import com.google.gwt.aria.client.OrientationValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

// do we need to expose the outline/selection outside of the widget?
// prefs/listener callbacks
// spacing (too much)
// incremental updating
// scroll position on rebuild
// other opportunities to share code


public class PanmirrorOutlineWidget extends Composite
{
   public interface Navigator
   {
      void navigate(String navigationId);
   }
   
   public class VerticalSeparator extends Composite
   {
      public VerticalSeparator()
      {
         panel_ = new FlowPanel();
         panel_.addStyleName(outlineStyles_.leftSeparator());
         Roles.getSeparatorRole().set(panel_.getElement());
         Roles.getSeparatorRole().setAriaOrientationProperty(panel_.getElement(),
               OrientationValue.VERTICAL);
         initWidget(panel_);
      }
      
      private final FlowPanel panel_;
   }
   
   public PanmirrorOutlineWidget()
   {
    
      emptyPlaceholder_ = new FlowPanel();
      emptyPlaceholder_.add(new Label("No outline available"));
      emptyPlaceholder_.addStyleName(outlineStyles_.emptyPlaceholder());
      
      container_ = new DockLayoutPanel(Unit.PX);
      container_.addStyleName(outlineStyles_.container());
      
      separator_ = new VerticalSeparator();
      container_.addWest(separator_, 4);
      
      // This is a somewhat hacky way of allowing the separator to 'fit'
      // to a size of 4px, but overflow an extra 4px (to provide extra
      // space for a mouse cursor to drag or resize)
      Element parent = separator_.getElement().getParentElement();
      parent.getStyle().setPaddingRight(4, Unit.PX);
      
      tree_ = new Tree();
      tree_.addStyleName(outlineStyles_.tree());
      Roles.getTreeRole().setAriaLabelProperty(tree_.getElement(), "Document Outline");
      
      panel_ = new FlowPanel();
      panel_.addStyleName(outlineStyles_.panel());
      panel_.add(tree_);
      
      container_.add(panel_);
      handlers_ = new HandlerRegistrations();
      
      RStudioGinjector.INSTANCE.injectMembers(this);
     
         
      initWidget(container_);
      addStyleName(RES.styles().outline());
      
      
   }
   
   @Inject
   private void initialize(EventBus eventBus)
   {
      handlers_.add(eventBus.addHandler(EditorThemeStyleChangedEvent.TYPE, event -> {
         updateStyles(container_, event.getStyle());
         updateStyles(emptyPlaceholder_, event.getStyle());
         rebuildOutline();
      }));
   
   }
   
   @Override
   protected void onUnload()
   {
      handlers_.removeHandler();
   }
   
   public void setNavigator(Navigator navigator)
   {
      navigator_ = navigator;
   }
   
   public void updateOutline(PanmirrorOutlineItem[] outline)
   {
      outline_ = outline;
      rebuildOutline();
      updateActiveItem();
   }
   
   public void updateSelection(PanmirrorSelection selection)
   {
      selection_ = selection;
      updateActiveItem();
   }
   
   public Widget getLeftSeparator()
   {
      return separator_;
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
      if (outline_.length == 0)
      {
         setActiveWidget(emptyPlaceholder_);
         return;
      }
      
      setActiveWidget(tree_);
      tree_.clear();
      Arrays.stream(outline_).forEach(item -> { addToTree(item); });
   }
   
   private void addToTree(PanmirrorOutlineItem item)
   {
      OutlineTreeItem treeItem = createTreeItem(item);
      tree_.addItem(treeItem);
      Arrays.stream(item.children).forEach(child -> addToTree(child));
   }
   
   private void setActiveWidget(Widget widget)
   {
      panel_.clear();
      panel_.add(widget);
   }
   
   
   private void updateStyles(Widget widget, Style computed)
   {
      Style outlineStyles = widget.getElement().getStyle();
      outlineStyles.setBackgroundColor(computed.getBackgroundColor());
      outlineStyles.setColor(computed.getColor());
   }
   
   private OutlineTreeItem createTreeItem(PanmirrorOutlineItem item)
   {
      OutlineTreeEntry entry = new OutlineTreeEntry(item);
      OutlineTreeItem treeItem = new OutlineTreeItem(entry);
      setTreeItemStyles(treeItem);
      return treeItem;
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
      
      if (outline_ == null || outline_.length == 0)
         return;
      
      if (selection_ == null)
         return;
      
      activeItem_ = outline_[0];
      
      for (int i = 0; i < tree_.getItemCount(); i++) 
      {
         OutlineTreeItem treeItem = (OutlineTreeItem)tree_.getItem(i);
         PanmirrorOutlineItem item = treeItem.getEntry().getItem();
         
         if (item.pos >= selection_.from)
            break;
         
         activeItem_ = item;
         
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
                     navigator_.navigate(item.navigation_id);
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
         depth = Math.max(0, depth - 1);
         String text = StringUtil.repeat("&nbsp;", depth * 2);
         if (indent_ == null)
            indent_ = new HTML(text);
         else
            indent_.setHTML(text);

         indent_.addStyleName(outlineStyles_.nodeLabel());
         indent_.getElement().getStyle().setFloat(Style.Float.LEFT);
      }
      
      @SuppressWarnings("unused")
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
   
   private final static PanmirrorOutlineResources RES = PanmirrorOutlineResources.INSTANCE;
   private final static DocumentOutlineWidget.Styles outlineStyles_ = DocumentOutlineWidget.RES.styles();
   
   private PanmirrorOutlineItem[] outline_ = null;
   private PanmirrorSelection selection_ = null;
   private PanmirrorOutlineItem activeItem_ = null;
   private Navigator navigator_ = null;
   
   private final Tree tree_;
   private final DockLayoutPanel container_;
   private final FlowPanel panel_;
   private final VerticalSeparator separator_;
   private final FlowPanel emptyPlaceholder_;
  
   
   private final HandlerRegistrations handlers_;

  
  
  
   
}
