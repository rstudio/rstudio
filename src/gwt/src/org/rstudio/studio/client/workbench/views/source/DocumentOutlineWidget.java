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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Counter;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeFunction;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeStyleChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DocumentOutlineWidget extends Composite
                  implements EditorThemeStyleChangedEvent.Handler
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
      public DocumentOutlineTreeEntry(Scope node, int depth)
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
               target_.setCursorPosition(node_.getPreamble());
               target_.getDocDisplay().alignCursor(node_.getPreamble(), 0.1);
               
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
         String text = "";
         if (node.isChunk())
         {
            text = node.getChunkLabel();
            if (StringUtil.isNullOrEmpty(text))
               text = "(" + node.getLabel().toLowerCase() + ")";
         }
         else if (node.isFunction())
         {
            ScopeFunction asFunctionNode = (ScopeFunction) node;
            text = asFunctionNode.getFunctionName();
         }
         else if (node.isYaml())
         {
            text = "Title";
         }
         else
         {
            text = node.getLabel();
         }

         if (label_ == null)
            label_ = new Label(text);
         else
            label_.setText(text);
         
         label_.addStyleName(RES.styles().nodeLabel());
         label_.addStyleName(ThemeStyles.INSTANCE.handCursor());
         
         label_.removeStyleName(RES.styles().nodeLabelChunk());
         label_.removeStyleName(RES.styles().nodeLabelSection());
         label_.removeStyleName(RES.styles().nodeLabelFunction());
         
         if (node.isChunk())
            label_.addStyleName(RES.styles().nodeLabelChunk());
         else if (node.isSection() && !node.isMarkdownHeader() && !node.isYaml())
            label_.addStyleName(RES.styles().nodeLabelSection());
         else if (node.isFunction())
            label_.addStyleName(RES.styles().nodeLabelFunction());
      }
      
      private void setIndent(int depth)
      {
         depth = Math.max(0, depth);
         String text = StringUtil.repeat("&nbsp;", depth * 2);
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
   
   @Inject
   private void initialize(UIPrefs uiPrefs)
   {
      uiPrefs_ = uiPrefs;
   }
   
   public DocumentOutlineWidget(TextEditingTarget target)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      emptyPlaceholder_ = new FlowPanel();
      emptyPlaceholder_.add(new Label("No outline available"));
      emptyPlaceholder_.addStyleName(RES.styles().emptyPlaceholder());
      
      container_ = new DockLayoutPanel(Unit.PX);
      container_.addStyleName(RES.styles().container());
      target_ = target;
      
      separator_ = new VerticalSeparator();
      container_.addWest(separator_, 4);
      
      // This is a somewhat hacky way of allowing the separator to 'fit'
      // to a size of 4px, but overflow an extra 4px (to provide extra
      // space for a mouse cursor to drag or resize)
      Element parent = separator_.getElement().getParentElement();
      parent.getStyle().setPaddingRight(4, Unit.PX);
      
      tree_ = new Tree();
      tree_.addStyleName(RES.styles().tree());
      
      panel_ = new FlowPanel();
      panel_.addStyleName(RES.styles().panel());
      panel_.add(tree_);
      
      container_.add(panel_);
      handlers_ = new HandlerRegistrations();
      initHandlers();
          
      initWidget(container_);
   }
   
   public Widget getLeftSeparator()
   {
      return separator_;
   }
   
   @Override
   public void onEditorThemeStyleChanged(EditorThemeStyleChangedEvent event)
   {
      updateStyles(container_, event.getStyle());
      updateStyles(emptyPlaceholder_, event.getStyle());
   }
   
   private void initHandlers()
   {
      handlers_.add(target_.getDocDisplay().addScopeTreeReadyHandler(new ScopeTreeReadyEvent.Handler()
      {
         @Override
         public void onScopeTreeReady(ScopeTreeReadyEvent event)
         {
            rebuildScopeTree(event.getScopeTree(), event.getCurrentScope());
            resetTreeStyles();
         }
      }));
      
      handlers_.add(target_.getDocDisplay().addCursorChangedHandler(new CursorChangedHandler()
      {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            if (target_.getDocDisplay().isScopeTreeReady(event.getPosition().getRow()))
            {
               currentScope_ = target_.getDocDisplay().getCurrentScope();
               currentVisibleScope_ = getCurrentVisibleScope(currentScope_);
               resetTreeStyles();
            }
         }
      }));
      
      handlers_.add(target_.addEditorThemeStyleChangedHandler(this));
      
      handlers_.add(uiPrefs_.shownSectionsInDocumentOutline().bind(new CommandWithArg<String>()
      {
         @Override
         public void execute(String prefValue)
         {
            rebuildScopeTreeOnPrefChange();
         }
      }));
      
   }
   
   private void updateStyles(Widget widget, Style computed)
   {
      Style outlineStyles = widget.getElement().getStyle();
      outlineStyles.setBackgroundColor(computed.getBackgroundColor());
      outlineStyles.setColor(computed.getColor());
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
   
   private void setActiveWidget(Widget widget)
   {
      panel_.clear();
      panel_.add(widget);
   }
   
   private void rebuildScopeTreeOnPrefChange()
   {
      if (scopeTree_ == null || currentScope_ == null)
         return;
      
      rebuildScopeTree(scopeTree_, currentScope_);
   }
   
   private void rebuildScopeTree(JsArray<Scope> scopeTree, Scope currentScope)
   {
      scopeTree_ = scopeTree;
      currentScope_ = currentScope;
      currentVisibleScope_ = getCurrentVisibleScope(currentScope_);
      
      if (scopeTree_.length() == 0)
      {
         setActiveWidget(emptyPlaceholder_);
         return;
      }
      
      setActiveWidget(tree_);
      
      int h1Count = 0;
      for (int i = 0; i < scopeTree_.length(); i++)
      {
         Scope node = scopeTree_.get(i);
         if (node.isMarkdownHeader())
         {
            if (node.getDepth() == 1)
               h1Count++;
         }
      }
      
      int initialDepth = h1Count == 1 ? -1 : 0;
      
      Counter counter = new Counter(-1);
      for (int i = 0; i < scopeTree_.length(); i++)
         buildScopeTreeImpl(scopeTree_.get(i), initialDepth, counter);
      
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
      {
         int newDepth = depth + 1;
         
         // Don't add extra indentation for items within namespaces
         if (node.isNamespace())
            newDepth--;
         
         buildScopeTreeImpl(children.get(i), newDepth, counter);
      }
   }
   
   private boolean isUnnamedNode(Scope node)
   {
      if (node.isChunk())
         return StringUtil.isNullOrEmpty(node.getChunkLabel());
      return StringUtil.isNullOrEmpty(node.getLabel());
   }
   
   private boolean shouldDisplayNode(Scope node)
   {
      String shownSectionsPref = uiPrefs_.shownSectionsInDocumentOutline().getGlobalValue();
      if (node.isChunk() && shownSectionsPref.equals(UIPrefsAccessor.DOC_OUTLINE_SHOW_SECTIONS_ONLY))
         return false;
      
      if (isUnnamedNode(node) && !shownSectionsPref.equals(UIPrefsAccessor.DOC_OUTLINE_SHOW_ALL))
         return false;
      
      // NOTE: the 'is*' items are not mutually exclusive
      if (node.isAnon() || node.isLambda() || node.isTopLevel())
         return false;
      
      // Don't show namespaces in the scope tree
      if (node.isNamespace())
         return false;
      
      // don't show R functions or R sections in .Rmd unless requested
      TextFileType fileType = target_.getDocDisplay().getFileType();
      if (!shownSectionsPref.equals(UIPrefsAccessor.DOC_OUTLINE_SHOW_ALL) && fileType.isRmd())
      {
         if (node.isFunction())
            return false;
         
         if (node.isSection() && !node.isMarkdownHeader())
            return false;
      }
      
      // filter out anonymous functions
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
   
   private Scope getCurrentVisibleScope(Scope node)
   {
      for (; node != null && !node.isTopLevel(); node = node.getParentScope())
         if (shouldDisplayNode(node))
            return node;
      return null;
   }
   
   private boolean isActiveNode(Scope node)
   {
      return node != null && node.equals(currentVisibleScope_);
   }
   
   private final DockLayoutPanel container_;
   private final FlowPanel panel_;
   private final VerticalSeparator separator_;
   private final Tree tree_;
   private final FlowPanel emptyPlaceholder_;
   
   private final TextEditingTarget target_;
   private final HandlerRegistrations handlers_;
   
   private JsArray<Scope> scopeTree_;
   private Scope currentScope_;
   private Scope currentVisibleScope_;
   
   private UIPrefs uiPrefs_;
   
   // Styles, Resources etc. ----
   public interface Styles extends CssResource
   {
      String panel();
      String container();
      
      String leftSeparator();
      String emptyPlaceholder();
      
      String tree();
      
      String node();
      
      String activeNode();
      String activeParentNode();
      
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
