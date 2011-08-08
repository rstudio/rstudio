package org.rstudio.studio.client.workbench.codesearch.ui;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;


public interface CodeSearchResources extends ClientBundle
{
   public static interface Styles extends CssResource
   {
      String codeSearchWidget();
      String fileImage();
      String itemImage();
      String itemName();
      String itemContext();
      String codeSearchDialogMainWidget();
   }

  
   @Source("CodeSearch.css")
   Styles styles();
   
   ImageResource function();
   ImageResource method();
   ImageResource cls();
   ImageResource gotoFunction();
   
   public static CodeSearchResources INSTANCE = 
      (CodeSearchResources)GWT.create(CodeSearchResources.class) ;
}
