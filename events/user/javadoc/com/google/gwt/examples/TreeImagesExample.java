package com.google.gwt.examples;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeImages;

public class TreeImagesExample implements EntryPoint {
  
  /**
   * Allows us to override Tree default images. If we don't override one of the
   * methods, the default will be used.
   */
  interface MyTreeImages extends TreeImages {
    
    @Resource("downArrow.png")
    AbstractImagePrototype treeOpen();
    
    @Resource("rightArrow.png")
    AbstractImagePrototype treeClosed();
  }
  
  public void onModuleLoad() {
    TreeImages images = (TreeImages)GWT.create(MyTreeImages.class);
    Tree tree = new Tree(images);
    RootPanel.get().add(tree);
  }
}
