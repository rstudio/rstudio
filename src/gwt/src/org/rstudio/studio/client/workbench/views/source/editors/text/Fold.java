/*
 * Fold.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayMixed;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceFold;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import java.util.ArrayList;

public class Fold
{
   public static int getCollectiveHashCode(ArrayList<Fold> folds)
   {
      int hashCode = 0;
      for (Fold fold : folds)
      {
         hashCode *= 31;
         hashCode += fold.hashCode();
      }
      return hashCode;
   }

   public static Fold fromAceFold(AceFold fold, Position offset)
   {
      return new Fold(
            Position.create(fold.getStart().getRow() + offset.getRow(), fold.getStart().getColumn()),
            Position.create(fold.getEnd().getRow() + offset.getRow(), fold.getEnd().getColumn()),
            fold.getPlaceholder());
   }

   public static String encode(ArrayList<Fold> folds)
   {
      StringBuilder result = new StringBuilder();
      for (Fold f : folds)
      {
         result.append(f.getStartRow()).append('|')
               .append(f.getStartColumn()).append('|')
               .append(f.getEndRow()).append('|')
               .append(f.getEndColumn()).append('|')
               .append('\n');
      }
      return result.toString();
   }

   public static ArrayList<Fold> decode(String foldData)
   {
      ArrayList<Fold> results = new ArrayList<Fold>();
      String[] chunks = foldData.split("\n");
      for (String chunk : chunks)
      {
         if (chunk.isEmpty())
            continue;

         String[] pieces = chunk.split("\\|");
         results.add(new Fold(Integer.parseInt(pieces[0]),
                              Integer.parseInt(pieces[1]),
                              Integer.parseInt(pieces[2]),
                              Integer.parseInt(pieces[3]),
                              "..."));//pieces[4]));
      }
      return results;
   }

   public static JsArray<JsArrayMixed> toJs(ArrayList<Fold> folds)
   {
      JsArray<JsArrayMixed> results = JavaScriptObject.createArray().cast();
      for (Fold f : folds)
      {
         JsArrayMixed foldData = JavaScriptObject.createArray().cast();
         foldData.set(0, f.getStartRow());
         foldData.set(1, f.getStartColumn());
         foldData.set(2, f.getEndRow());
         foldData.set(3, f.getEndColumn());
         foldData.set(4, f.getPlaceholder());
         results.push(foldData);
      }
      return results;
   }

   public static ArrayList<Fold> fromJs(JsArray<JsArrayMixed> folds)
   {
      ArrayList<Fold> results = new ArrayList<Fold>();
      for (int i = 0; i < folds.length(); i++)
      {
         JsArrayMixed foldData = folds.get(i);
         results.add(new Fold((int)foldData.getNumber(0),
                              (int)foldData.getNumber(1),
                              (int)foldData.getNumber(2),
                              (int)foldData.getNumber(3),
                              foldData.getString(4)));
      }
      return results;
   }


   /**
    * Puts the input ace folds, and their subfolds (recursively), into a flat
    * list of Fold objects.
    */
   public static ArrayList<Fold> flatten(JsArray<AceFold> folds)
   {
      ArrayList<Fold> results = new ArrayList<Fold>();
      for (int i = 0; i < folds.length(); i++)
         collect(folds.get(i), results, Position.create(0, 0));
      return results;
   }

   private static void collect(AceFold fold, ArrayList<Fold> results, Position parentOffset)
   {
      results.add(fromAceFold(fold, parentOffset));
      JsArray<AceFold> subFolds = fold.getSubFolds();
      for (int i = 0; i < subFolds.length(); i++)
      {
         AceFold subFold = subFolds.get(i);
         Position offset = Position.create(
               fold.getStart().getRow() + parentOffset.getRow(),
               fold.getStart().getColumn() + parentOffset.getColumn());
         collect(subFold, results, offset);
      }
   }

   public Fold(Position start, Position end, String placeholder)
   {
      this(start.getRow(),
           start.getColumn(),
           end.getRow(),
           end.getColumn(),
           placeholder);
   }

   public Fold(int startRow,
               int startColumn,
               int endRow,
               int endColumn,
               String placeholder)
   {
      startRow_ = startRow;
      startColumn_ = startColumn;
      endRow_ = endRow;
      endColumn_ = endColumn;
      placeholder_ = placeholder;
   }

   public int getStartRow()
   {
      return startRow_;
   }

   public int getStartColumn()
   {
      return startColumn_;
   }

   public int getEndRow()
   {
      return endRow_;
   }

   public int getEndColumn()
   {
      return endColumn_;
   }

   public String getPlaceholder()
   {
      return placeholder_;
   }

   public Range getRange()
   {
      return Range.fromPoints(
            Position.create(getStartRow(), getStartColumn()),
            Position.create(getEndRow(), getEndColumn()));
   }

   public AceFold toAceFold()
   {
      return AceFold.createFold(getRange(), placeholder_);
   }

   @Override
   public int hashCode()
   {
      int hashCode = startRow_;
      hashCode *= 31;
      hashCode += startColumn_;
      hashCode *= 31;
      hashCode += endRow_;
      hashCode *= 31;
      hashCode += endColumn_;
      hashCode *= 31;
      hashCode += placeholder_.hashCode();
      return hashCode;
   }

   private final int startRow_;
   private final int startColumn_;
   private final int endRow_;
   private final int endColumn_;
   private final String placeholder_;
}
