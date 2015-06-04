package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
         
         Label indent = createIndent(depth);
         Label label = createLabel(node);
         
         panel.add(indent);
         panel.add(label);
         
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
      
      private Label createLabel(Scope node)
      {
         String text = node.isChunk() ?
            node.getChunkLabel() :
            node.getLabel();
         
         if (text.equals(""))
            text = node.isChunk() ? "(Unnamed Chunk)" : "(Unnamed Section)";
         
         Label label = new Label(text);
         label.addStyleName(RES.styles().nodeLabel());
         if (node.isChunk())
            label.addStyleName(RES.styles().nodeLabelChunk());
         else if (node.isSection())
            label.addStyleName(RES.styles().nodeLabelSection());
         else if (node.isFunction())
            label.addStyleName(RES.styles().nodeLabelFunction());
         return label;
      }
      
      private HTML createIndent(int depth)
      {
         HTML indent = new HTML(StringUtil.repeat("&nbsp;", depth * 4));
         indent.addStyleName(RES.styles().nodeLabel());
         indent.getElement().getStyle().setFloat(Style.Float.LEFT);
         return indent;
      }
      
      public Scope getScopeNode()
      {
         return node_;
      }
      
      private final Scope node_;
   }
   
   private class DocumentOutlineTreeItem extends TreeItem
   {
      public DocumentOutlineTreeItem(DocumentOutlineTreeEntry entry)
      {
         super(entry);
         entry_ = entry;
      }
      
      public Scope getScopeNode()
      {
         return entry_.getScopeNode();
      }
      
      private final DocumentOutlineTreeEntry entry_;
   }
   
   public DocumentOutlineWidget(TextEditingTarget target)
   {
      container_ = new DockLayoutPanel(Unit.PX);
      container_.addStyleName(RES.styles().container());
      target_ = target;
      
      separator_ = new VerticalSeparator();
      container_.addWest(separator_, 6);
      
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
      
      target_.getDocDisplay().addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Void> event)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  DocumentOutlineWidget.this.onValueChanged();
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
   
   private void onValueChanged()
   {
      buildScopeTree();
      resetTreeStyles();
   }
   
   private void buildScopeTree()
   {
      scopeTree_ = target_.getDocDisplay().getScopeTree();
      tree_.clear();
      JsArray<Scope> scopeTree = target_.getDocDisplay().getScopeTree();
      for (int i = 0; i < scopeTree.length(); i++)
         buildScopeTreeImpl(scopeTree.get(i), -1);
   }
   
   private void buildScopeTreeImpl(Scope node, int depth)
   {
      if (shouldDisplayNode(node))
         tree_.addItem(createEntry(node, depth));
      
      JsArray<Scope> children = node.getChildren();
      for (int i = 0; i < children.length(); i++)
         buildScopeTreeImpl(children.get(i), depth + 1);
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
         buildScopeTree();
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
      Scope node = item.getScopeNode();
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
