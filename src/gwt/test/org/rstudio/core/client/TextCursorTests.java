/*
 * TextCursorTests.java
 *
 * Copyright (C) 2024 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

import com.google.gwt.junit.client.GWTTestCase;

public class TextCursorTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // #region Clone, Get and Set Index

   public void testClone()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      cursor.setIndex(5);
      assertTrue(cursor.contentEquals('5'));
      assertEquals(16, cursor.getData().length());
      TextCursor newCursor = cursor.clone();
      assertTrue(newCursor.contentEquals('5'));
      assertEquals(16, newCursor.getData().length());
      cursor.setIndex(6);
      assertEquals(6, cursor.getIndex());
      assertEquals(5, newCursor.getIndex());
   }

   public void testSetGetDefaultIndex()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      assertEquals(0, cursor.getIndex());
   }

   public void testSetGetFirstIndex()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      cursor.setIndex(0);
      assertEquals(0, cursor.getIndex());
   }

   public void testSetGetMiddleIndex()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      cursor.setIndex(8);
      assertEquals(8, cursor.getIndex());
   }

   public void testSetGetLastIndex()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      cursor.setIndex(15);
      assertEquals(15, cursor.getIndex());
   }

   public void testSetGetBeyondLastIndex()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      cursor.setIndex(36);
      assertEquals(36, cursor.getIndex());
   }

   public void testSetGetNegativeIndex()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      cursor.setIndex(-5);
      assertEquals(-5, cursor.getIndex());
   }

   public void testNonZeroStartingIndex()
   {
      TextCursor cursor = new TextCursor("0123456789", 0);
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("0123456789", 5);
      assertEquals(5, cursor.getIndex());
      assertEquals('5', cursor.peek());
      cursor = new TextCursor("0123456789", 10);
      assertEquals(10, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      cursor = new TextCursor("0123456789", 9);
      assertEquals(9, cursor.getIndex());
      assertEquals('9', cursor.peek());
   }

   public void testNegativeStartingIndex()
   {
      TextCursor cursor = new TextCursor("0123456789", -5);
      assertEquals(-5, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.advance(1));
      assertEquals(-4, cursor.getIndex());
      assertEquals('\0', cursor.peek());
   }

   // #endregion
   // #region Peek

   public void testEmptyPeek()
   {
      TextCursor cursor = new TextCursor("");
      assertEquals('\0', cursor.peek());
   }

   public void testPeekFirstChar()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      assertEquals('0', cursor.peek());
   }

   public void testPeekMiddleChar()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef", 7);
      assertEquals('7', cursor.peek());
   }

   public void testPeekLastChar()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef", 15);
      assertEquals('f', cursor.peek());
   }

   public void testPeekBeyondEnd()
   {
      TextCursor cursor = new TextCursor("0123", 6);
      assertEquals(6, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(5, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(4, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(3, cursor.getIndex());
      assertEquals('3', cursor.peek());
   }

   public void testPeekBeforeStart()
   {
      TextCursor cursor = new TextCursor("0123", -3);
      assertEquals(-3, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(-4, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals(-3, cursor.getIndex());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals(-2, cursor.getIndex());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals(-1, cursor.getIndex());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals(0, cursor.getIndex());
   }

   public void testEmptyPeekByIndex()
   {
      TextCursor cursor = new TextCursor("");
      assertEquals('\0', cursor.peek(0));
   }

   public void testPeekFirstCharByIndex()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      assertEquals('0', cursor.peek(0));
   }

   public void testPeekMiddleCharByIndex()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      assertEquals('7', cursor.peek(7));
   }

   public void testPeekLastCharByIndex()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      assertEquals('f', cursor.peek(15));
   }

   public void testPeekBeyondEndByIndex()
   {
      TextCursor cursor = new TextCursor("0123");
      assertEquals(0, cursor.getIndex());
      assertEquals('\0', cursor.peek(6));
      assertEquals('\0', cursor.peek(5));
      assertEquals('\0', cursor.peek(4));
      assertEquals('3', cursor.peek(3));
   }

   public void testPeekBeforeStartByIndex()
   {
      TextCursor cursor = new TextCursor("0123");
      assertEquals('\0', cursor.peek(-1));
      assertEquals('\0', cursor.peek(-2));
      assertEquals('\0', cursor.peek(-3));
      assertEquals('\0', cursor.peek(-4));
   }

   // #endregion
   // #region ContentEquals

   public void testContentEqualsEmpty()
   {
      TextCursor cursor = new TextCursor("");
      assertFalse(cursor.contentEquals('\0'));
   }

   public void testContentEqualsFirstChar()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      assertTrue(cursor.contentEquals('0'));
   }

   public void testContentEqualsMiddleChar()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef", 7);
      assertTrue(cursor.contentEquals('7'));
   }

   public void testContentEqualsLastChar()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef", 15);
      assertTrue(cursor.contentEquals('f'));
   }

   public void testContentEqualsNullTerminator()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef", 16);
      assertFalse(cursor.contentEquals('\0'));
   }

   public void testContentEqualsBeyondEnd()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef", 17);
      try
      {
         assertTrue(cursor.contentEquals('\0'));
         assertEquals("Expected exception to be thrown", "Nope");
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   public void testContentEqualsBeforeStart()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef", -1);
      try
      {
         assertTrue(cursor.contentEquals('\0'));
         assertEquals("Expected exception to be thrown", "Nope");
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   // #endregion
   // #region Advance

   public void testEmptyStringAdvance()
   {
      TextCursor cursor = new TextCursor("");
      assertEquals(0, cursor.getIndex());
      assertTrue(cursor.advance(0));
      assertEquals(0, cursor.getIndex());
      assertFalse(cursor.advance(1));
      assertEquals(0, cursor.getIndex());
      assertFalse(cursor.advance(2));
      assertEquals(0, cursor.getIndex());
      assertFalse(cursor.advance(5));
      assertEquals(0, cursor.getIndex());
      assertEquals('\0', cursor.peek());
   }

      public void testAdvanceWithinString()
   {
      TextCursor cursor = new TextCursor("0123456789abcdef");
      assertTrue(cursor.advance(1));
      assertEquals(1, cursor.getIndex());
      assertTrue(cursor.contentEquals('1'));
      assertTrue(cursor.advance(9));
      assertEquals(10, cursor.getIndex());
      assertTrue(cursor.contentEquals('a'));
   }

   public void testAdvanceToEndOfString()
   {
      TextCursor cursor = new TextCursor("01234");
      assertTrue(cursor.advance(5));
      assertEquals(5, cursor.getIndex());
      assertEquals('\0', cursor.peek());
   }

   public void testAdvanceBeyondEndOfString()
   {
      TextCursor cursor = new TextCursor("01234");
      assertFalse(cursor.advance(99));
      assertEquals(5, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertFalse(cursor.advance(1));
      assertEquals(5, cursor.getIndex());
   }

   public void testAdvanceWithNegativeStep()
   {
      TextCursor cursor = new TextCursor("0123456789");
      cursor.advance(6);
      assertEquals('6', cursor.peek());

      // advance with any negative step value is a no-op
      assertTrue(cursor.advance(-2));
      assertEquals(6, cursor.getIndex());
      assertTrue(cursor.advance(-2));
      assertEquals(6, cursor.getIndex());
   }

   // #endregion
   // #region MoveToNextCharacter

   public void testMoveToNextCharacter()
   {
      TextCursor cursor = new TextCursor("0123");
      assertTrue(cursor.moveToNextCharacter());
      assertEquals('1', cursor.peek());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals('2', cursor.peek());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals('3', cursor.peek());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals('\0', cursor.peek());
      assertEquals(4, cursor.getIndex());
      assertFalse(cursor.moveToNextCharacter());
      assertEquals('\0', cursor.peek());
      assertEquals(4, cursor.getIndex());
   }

   public void testMoveToNextCharacterEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertEquals(0, cursor.getIndex());
      assertFalse(cursor.moveToNextCharacter());
   }

   public void testMoveToNextCharacterBeforeStart()
   {
      TextCursor cursor = new TextCursor("0123", -2);
      assertEquals(-2, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals(-1, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals(0, cursor.getIndex());
      assertEquals('0', cursor.peek());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals('1', cursor.peek());
   }

   public void testMoveToNextCharacterAfterEnd()
   {
      TextCursor cursor = new TextCursor("0123", 8);
      assertEquals(8, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.moveToNextCharacter());
      assertEquals(9, cursor.getIndex());
      assertEquals('\0', cursor.peek());
   }

   // #endregion
   // #region MoveToPreviousCharacter

   public void testMoveToPreviousCharacter()
   {
      TextCursor cursor = new TextCursor("0123", 3);
      assertEquals('3', cursor.peek());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals('2', cursor.peek());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals('1', cursor.peek());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals('0', cursor.peek());
      assertFalse(cursor.moveToPreviousCharacter());
      assertEquals('0', cursor.peek());
   }

   public void testMoveToPreviousCharacterEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertEquals('\0', cursor.peek());
      assertFalse(cursor.moveToPreviousCharacter());
      assertEquals(0, cursor.getIndex());
   }

   public void testMoveToPreviousCharacterBeforeStart()
   {
      TextCursor cursor = new TextCursor("0123", -2);
      assertEquals(-2, cursor.getIndex());
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(-3, cursor.getIndex());
      assertEquals('\0', cursor.peek());
   }

   public void testMoveToPreviousCharacterAfterEnd()
   {
      TextCursor cursor = new TextCursor("0123", 6);
      assertEquals('\0', cursor.peek());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(5, cursor.getIndex());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(4, cursor.getIndex());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(3, cursor.getIndex());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(2, cursor.getIndex());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(1, cursor.getIndex());
      assertTrue(cursor.moveToPreviousCharacter());
      assertEquals(0, cursor.getIndex());
      assertEquals('0', cursor.peek());
      assertFalse(cursor.moveToPreviousCharacter());
      assertEquals(0, cursor.getIndex());
    }

   // #endregion
   // #region FwdToMatchingCharacter

   // #endregion
   // #region BwdToMatchingCharacter

   // #endregion

}
