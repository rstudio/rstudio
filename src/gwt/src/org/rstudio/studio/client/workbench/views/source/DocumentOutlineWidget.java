package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
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
         DockLayoutPanel panel = new DockLayoutPanel(Unit.PX);
         
         Image icon = new Image(target_.getIcon()); // placeholder
         String text = node.isChunk() ?
               node.getChunkLabel() :
                  node.getLabel();
         Label label = new Label(text);
         
         panel.addWest(icon, icon.getWidth() + 4);
         panel.add(label);
         panel.setHeight((icon.getHeight() + 4) + "px");
         
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
   }
   
   public DocumentOutlineWidget(TextEditingTarget target)
   {
      container_ = new FlowPanel();
      target_ = target;
      
      tree_ = new Tree();
      container_.add(tree_);
      container_.getElement().getStyle().setMarginLeft(-20, Unit.PX);
      initHandlers();
      
      initWidget(container_);
   }
   
   private void initHandlers()
   {
      target_.getDocDisplay().addRenderFinishedHandler(new RenderFinishedEvent.Handler()
      {
         @Override
         public void onRenderFinished(RenderFinishedEvent event)
         {
            synchronize();
         }
      });
   }
   
   private void synchronize()
   {
      tree_.clear();
      JsArray<Scope> scopeTree = target_.getDocDisplay().getScopeTree();
      for (int i = 0; i < scopeTree.length(); i++)
      {
         TreeItem item = createEntry(scopeTree.get(i));
         tree_.addItem(item);
      }
   }
   
   private TreeItem createEntry(Scope node)
   {
      DocumentOutlineTreeEntry entry = new DocumentOutlineTreeEntry(node);
      
      TreeItem item = new TreeItem(entry);
      item.addStyleName(RES.styles().node());
      
      if (isActiveNode(node))
         item.addStyleName(RES.styles().activeNode());
      else if (isVisibleNode(node))
         item.addStyleName(RES.styles().visibleNode());
      
      return item;
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
   
   private final FlowPanel container_;
   private final Tree tree_;
   private final TextEditingTarget target_;
   
   // Styles, Resources etc. ----
   public interface Styles extends CssResource
   {
      String node();
      
      String activeNode();
      String visibleNode();
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
