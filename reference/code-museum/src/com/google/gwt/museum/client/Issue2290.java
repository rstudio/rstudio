package com.google.gwt.museum.client;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

public class Issue2290 {

  public Issue2290(Panel p) {
    Tree tree = new Tree();
    
    final TreeItem treeItem1 = new TreeItem("Item1");
    tree.addItem(treeItem1);

    final TreeItem treeItem2 = new TreeItem("Item2");
    tree.addItem(treeItem2);
   
    p.add(tree);
    
    final Button button = new Button("Select", new ClickListener() {
      public void onClick(Widget sender) {
        treeItem1.setSelected(!treeItem1.isSelected());
        treeItem2.setSelected(!treeItem2.isSelected());
        
        ((Button)sender).setText(treeItem1.isSelected() ? "Unselect" : "Select");
      }
    });
    p.add(button);
  }
  
}
