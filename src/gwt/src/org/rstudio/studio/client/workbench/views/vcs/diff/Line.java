package org.rstudio.studio.client.workbench.views.vcs.diff;

public class Line
{
   public enum Type
   {
      Same,
      Insertion,
      Deletion
   }

   public static Line createIns(Integer newLine, String text)
   {
      return new Line(Type.Insertion, null, newLine, text);
   }

   public static Line createDel(Integer oldLine, String text)
   {
      return new Line(Type.Deletion, oldLine, null, text);
   }

   public static Line createSame(Integer oldLine, Integer newLine, String text)
   {
      return new Line(Type.Same, oldLine, newLine, text);
   }

   private Line(Type type, Integer oldLine, Integer newLine, String text)
   {
      type_ = type;
      oldLine_ = oldLine;
      newLine_ = newLine;
      text_ = text;
   }

   public Type getType()
   {
      return type_;
   }

   public Integer getOldLine()
   {
      return oldLine_;
   }

   public Integer getNewLine()
   {
      return newLine_;
   }

   public String getText()
   {
      return text_;
   }

   private Type type_;
   private Integer oldLine_;
   private Integer newLine_;
   private String text_;
}
