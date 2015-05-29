package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class DocumentOutlineWidget extends Composite
{
   private class DocumentOutlineTreeEntry extends Composite
   {
      public DocumentOutlineTreeEntry(final Scope node)
      {
         node_ = node;
         
         DockLayoutPanel panel = new DockLayoutPanel(Unit.PX);
         
         Image icon = createNodeIcon(node);
         Label label = createLabel(node);
         
         panel.addWest(icon, icon.getWidth() + 4);
         panel.add(label);
         panel.setHeight((icon.getHeight() + 8) + "px");
         
         panel.addDomHandler(new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               target_.setCursorPosition(node.getPreamble());
               target_.getDocDisplay().alignCursor(node.getPreamble(), 0.1);
               target_.focus();
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
         return label;
      }
      
      private ImageResource getNodeIcon(Scope node)
      {
         if (node.isChunk())
            return StatusBarWidget.RES.chunk();
         else if (node.isFunction())
            return StandardIcons.INSTANCE.functionLetter();
         else
            return StatusBarWidget.RES.section();
      }
      
      private Image createNodeIcon(Scope node)
      {
         Image icon = new Image(getNodeIcon(node));
         icon.addStyleName(RES.styles().nodeIcon());
         return icon;
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
      
      public DocumentOutlineTreeEntry getEntry()
      {
         return entry_;
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
      target_ = target;
      
      toolbar_ = new Toolbar();
      container_.addNorth(toolbar_, 22);
      
      separator_ = new FlowPanel();
      separator_.addStyleName(RES.styles().leftSeparator());
      container_.addWest(separator_, 1);
      
      tree_ = new Tree();
      tree_.addStyleName(RES.styles().tree());
      
      container_.add(tree_);
      initHandlers();
      
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
      Debug.logToConsole("Render finished!");
      ensureScopeTreePopulated();
      resetTreeStyles();
   }
   
   private void onValueChanged()
   {
      Debug.logToConsole("Value changed!");
      buildScopeTree();
      resetTreeStyles();
   }
   
   private void buildScopeTree()
   {
      scopeTree_ = target_.getDocDisplay().getScopeTree();
      tree_.clear();
      JsArray<Scope> scopeTree = target_.getDocDisplay().getScopeTree();
      for (int i = 0; i < scopeTree.length(); i++)
      {
         DocumentOutlineTreeItem item = createEntry(scopeTree.get(i));
         tree_.addItem(item);
      }
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
   
   private DocumentOutlineTreeItem createEntry(Scope node)
   {
      DocumentOutlineTreeEntry entry = new DocumentOutlineTreeEntry(node);
      DocumentOutlineTreeItem item = new DocumentOutlineTreeItem(entry);
      setTreeItemStyles(item);
      return item;
   }
   
   private void setTreeItemStyles(DocumentOutlineTreeItem item)
   {
      Scope node = item.getScopeNode();
      item.addStyleName(RES.styles().node());
      DomUtils.toggleClass(item.getElement(), RES.styles().activeNode(), isActiveNode(node));
      DomUtils.toggleClass(item.getElement(), RES.styles().visibleNode(), isVisibleNode(node));
   }
   
   private boolean isActiveNode(Scope node)
   {
      return node.equals(target_.getDocDisplay().getCurrentScope());
   }
   
   private boolean isVisibleNode(Scope node)
   {
      Position nodePos = node.getPreamble();
      return target_.getDocDisplay().isPositionVisible(nodePos);
   }
   
   private final DockLayoutPanel container_;
   private final Toolbar toolbar_;
   private final FlowPanel separator_;
   private final Tree tree_;
   private final TextEditingTarget target_;
   
   private final Timer renderTimer_;
   private JsArray<Scope> scopeTree_;
   
   // Styles, Resources etc. ----
   public interface Styles extends CssResource
   {
      String leftSeparator();
      
      String tree();
      
      String node();
      
      String activeNode();
      String visibleNode();
      
      String nodeIcon();
      String nodeLabel();
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
