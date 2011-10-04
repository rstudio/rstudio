package org.rstudio.studio.client.workbench.views.source.editors.codebrowser;

import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class CodeBrowserContextLabel extends Composite
{
   public CodeBrowserContextLabel(CodeBrowserEditingTargetWidget.Styles styles)
   {
      HorizontalPanel panel = new HorizontalPanel();
      
      nameLabel_ = new Label();
      nameLabel_.addStyleName(styles.functionName());
      panel.add(nameLabel_);
      
      namespaceLabel_ = new Label();
      namespaceLabel_.addStyleName(styles.functionNamespace());
      panel.add(namespaceLabel_);
      
      initWidget(panel);
   }
   
   public void setCurrentFunction(SearchPathFunctionDefinition functionDef)
   {
      nameLabel_.setText(functionDef.getName());
      namespaceLabel_.setText("{" + functionDef.getNamespace() + "}");
   }
   
   private Label nameLabel_;
   private Label namespaceLabel_;
}
