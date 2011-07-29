package org.rstudio.studio.client.workbench.codesearch;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;


public interface CodeSearchResources extends ClientBundle
{
   public static interface Styles extends CssResource
   {
      String functionName();
      String functionContext();
   }

  
   @Source("CodeSearch.css")
   Styles styles();
   
  
   
   public static CodeSearchResources INSTANCE = 
      (CodeSearchResources)GWT.create(CodeSearchResources.class) ;
}
