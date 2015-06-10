/*
 * DocumentOutlineWidget.java
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
package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.core.client.Counter;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

public class DocumentOutlineWidget extends Composite
{
   public class VerticalSeparator extends Composite
   {
      public VerticalSeparator()
      {
         panel_ = new FlowPanel();
         panel_.addStyleName(RES.styles().leftSeparator());
         initWidget(panel_);
      }
      
      private final FlowPanel panel_;
   }
   
   private class DocumentOutlineTreeEntry extends Composite
   {
      public DocumentOutlineTreeEntry(final Scope node, final int depth)
      {
         node_ = node;
         FlowPanel panel = new FlowPanel();
         
         setIndent(depth);
         setLabel(node);
         
         panel.add(indent_);
         panel.add(label_);
         
         panel.addDomHandler(new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               target_.setCursorPosition(node.getPreamble());
               target_.getDocDisplay().alignCursor(node.getPreamble(), 0.1);
               
               // Defer focus so it occurs after click has been fully handled
               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     target_.focus();
                  }
               });
            }
         }, ClickEvent.getType());
         
         initWidget(panel);
      }
      
      private void setLabel(Scope node)
      {
         String text = node.isChunk() ?
            node.getChunkLabel() :
            node.getLabel();

         if (text.equals(""))
            text = node.isChunk() ? "(Unnamed Chunk)" : "(Unnamed Section)";

         if (label_ == null)
            label_ = new Label(text);
         else
            label_.setText(text);
         
         label_.addStyleName(RES.styles().nodeLabel());
         if (node.isChunk())
            label_.addStyleName(RES.styles().nodeLabelChunk());
         else if (node.isSection())
            label_.addStyleName(RES.styles().nodeLabelSection());
         else if (node.isFunction())
            label_.addStyleName(RES.styles().nodeLabelFunction());
      }
      
      private void setIndent(int depth)
      {
         String text = StringUtil.repeat("&nbsp;", depth * 4);
         if (indent_ == null)
            indent_ = new HTML(text);
         else
            indent_.setHTML(text);

         indent_.addStyleName(RES.styles().nodeLabel());
         indent_.getElement().getStyle().setFloat(Style.Float.LEFT);
      }
      
      public void update(Scope node, int depth)
      {
         node_ = node;
         setLabel(node);
         setIndent(depth);
      }
      
      public Scope getScopeNode()
      {
         return node_;
      }
      
      private Scope node_;
      private HTML indent_;
      private Label label_;
   }
   
   private class DocumentOutlineTreeItem extends TreeItem
   {
      public DocumentOutlineTreeItem(DocumentOutlineTreeEntry entry)
      {
         super(entry);
         entry_ = entry;
      }
      
      public DocumentOutlineTreeEntry getEntry()
      {
         return entry_;
      }
      
      private final DocumentOutlineTreeEntry entry_;
   }
   
   public DocumentOutlineWidget(TextEditingTarget target)
   {
      container_ = new DockLayoutPanel(Unit.PX);
      container_.addStyleName(RES.styles().container());
      target_ = target;
      
      separator_ = new VerticalSeparator();
      container_.addWest(separator_, 8);
      
      tree_ = new Tree();
      tree_.addStyleName(RES.styles().tree());
      
      container_.add(tree_);
      initHandlers();
      
      // Since render events can be run in quick succession, we use a timer
      // to ensure multiple render events are 'bundled' into a single run
      renderTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            onRenderFinished();
         }
      };
      
      initWidget(container_);
   }
   
   public Widget getLeftSeparator()
   {
      return separator_;
   }
   
   private void initHandlers()
   {
      target_.getDocDisplay().addRenderFinishedHandler(new RenderFinishedEvent.Handler()
      {
         @Override
         public void onRenderFinished(RenderFinishedEvent event)
         {
            renderTimer_.schedule(10);
         }
      });
      
      target_.getDocDisplay().addDocumentChangedHandler(
            new DocumentChangedEvent.Handler()
            {
               @Override
               public void onDocumentChanged(final DocumentChangedEvent event)
               {
                  Scheduler.get().scheduleDeferred(new ScheduledCommand()
                  {
                     @Override
                     public void execute()
                     {
                        DocumentOutlineWidget.this.onDocumentChanged(event);
                     }
                  });
               }
            });
      
   }
   
   private void onRenderFinished()
   {
      ensureScopeTreePopulated();
      resetTreeStyles();
   }
   
   private void onDocumentChanged(final DocumentChangedEvent event)
   {
      // Debounce value changed events to avoid over-aggressively rebuilding
      // the scope tree.
      if (docUpdateTimer_ != null)
         docUpdateTimer_.cancel();
      
      docUpdateTimer_ = new Timer()
      {
         
         @Override
         public void run()
         {
            updateScopeTree(event);
            resetTreeStyles();
         }
      };
      
      docUpdateTimer_.schedule(1000);
   }
   
   private void addOrSetItem(Scope node, int depth, int index)
   {
      int treeSize = tree_.getItemCount();
      if (index < treeSize)
      {
         DocumentOutlineTreeItem item =
            (DocumentOutlineTreeItem) tree_.getItem(index);
         item.getEntry().update(node, depth);
      }
      else
      {
         tree_.addItem(createEntry(node, depth));
      }
   }
   
   private void updateScopeTree(DocumentChangedEvent event)
   {
      rebuildScopeTree();
   }
   
   private void rebuildScopeTree()
   {
      scopeTree_ = target_.getDocDisplay().getScopeTree();
      Counter counter = new Counter(-1);
      JsArray<Scope> scopeTree = target_.getDocDisplay().getScopeTree();
      for (int i = 0; i < scopeTree.length(); i++)
         buildScopeTreeImpl(scopeTree.get(i), -1, counter);
      
      // Clean up leftovers in the tree. 
      int oldTreeSize = tree_.getItemCount();
      int newTreeSize = counter.increment();
      
      for (int i = oldTreeSize - 1; i >= newTreeSize; i--)
      {
         TreeItem item = tree_.getItem(i);
         if (item != null)
            item.remove();
      }
   }
   
   private void buildScopeTreeImpl(Scope node, int depth, Counter counter)
   {
      if (shouldDisplayNode(node))
         addOrSetItem(node, depth, counter.increment());
      
      JsArray<Scope> children = node.getChildren();
      for (int i = 0; i < children.length(); i++)
         buildScopeTreeImpl(children.get(i), depth + 1, counter);
   }
   
   private boolean shouldDisplayNode(Scope node)
   {
      // NOTE: the 'is*' items are not mutually exclusive
      if (node.isAnon() || node.isLambda())
         return false;
      
      // TODO: Annotate scope tree in such a way that this isn't necessary
      if (node.getLabel() != null && node.getLabel().startsWith("<function>"))
         return false;
      
      return node.isChunk() ||
             node.isClass() ||
             node.isFunction() ||
             node.isNamespace() ||
             node.isSection();
   }
   
   private void resetTreeStyles()
   {
      for (int i = 0; i < tree_.getItemCount(); i++)
         setTreeItemStyles((DocumentOutlineTreeItem) tree_.getItem(i));
   }
   
   private void ensureScopeTreePopulated()
   {
      if (scopeTree_ == null)
         rebuildScopeTree();
   }
   
   private DocumentOutlineTreeItem createEntry(Scope node, int depth)
   {
      DocumentOutlineTreeEntry entry = new DocumentOutlineTreeEntry(node, depth);
      DocumentOutlineTreeItem item = new DocumentOutlineTreeItem(entry);
      setTreeItemStyles(item);
      return item;
   }
   
   private void setTreeItemStyles(DocumentOutlineTreeItem item)
   {
      Scope node = item.getEntry().getScopeNode();
      item.addStyleName(RES.styles().node());
      DomUtils.toggleClass(item.getElement(), RES.styles().activeNode(), isActiveNode(node));
   }
   
   private boolean isActiveNode(Scope node)
   {
      return node.equals(target_.getDocDisplay().getCurrentScope());
   }
   
   private final DockLayoutPanel container_;
   private final VerticalSeparator separator_;
   private final Tree tree_;
   private final TextEditingTarget target_;
   
   private final Timer renderTimer_;
   private Timer docUpdateTimer_;
   private JsArray<Scope> scopeTree_;
   
   // Styles, Resources etc. ----
   public interface Styles extends CssResource
   {
      String container();
      
      String leftSeparator();
      
      String tree();
      
      String node();
      
      String activeNode();
      
      String nodeLabel();
      String nodeLabelChunk();
      String nodeLabelSection();
      String nodeLabelFunction();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("DocumentOutlineWidget.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
}
