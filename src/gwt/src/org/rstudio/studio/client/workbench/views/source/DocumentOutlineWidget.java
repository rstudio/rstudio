package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
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
      public DocumentOutlineTreeEntry(String text)
      {
         DockLayoutPanel panel = new DockLayoutPanel(Unit.PX);
         
         Image icon = new Image(target_.getIcon()); // placeholder
         Label label = new Label(text);
         
         panel.addWest(icon, icon.getWidth() + 4);
         panel.add(label);
         panel.setHeight((icon.getHeight() + 4) + "px");
         
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
      return new TreeItem(
            new DocumentOutlineTreeEntry(node.getChunkLabel()));
   }
   
   private final FlowPanel container_;
   private final Tree tree_;
   private final TextEditingTarget target_;
}
