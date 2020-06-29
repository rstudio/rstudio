package org.rstudio.studio.client.panmirror.dialogs.model;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorInsertCiteWork
{
   public String publisher;
   public String[] title;
   public String DOI;
   public String URL;
   public String type;
   public PanmirrorInsertCiteAuthor[] author; 
   public PanmirrorInsertCiteDate issued; 
   public String issue;
   public String volume;
   public String page;   
}
