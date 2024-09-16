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
         cursor.contentEquals('\0');
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
         cursor.contentEquals('\0');
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

   public void testFwdToMatchingCharacterEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertEquals(0, cursor.getIndex());
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
   }

   public void testFwdToMatchingCharacterNonSpecialAdjacent()
   {
      TextCursor cursor = new TextCursor("XX");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(1, cursor.getIndex());
   }

   public void testFwdToMatchingCharacterNonSpecialNoMatch()
   {
      TextCursor cursor = new TextCursor("X");
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor(" T ", 1);
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(1, cursor.getIndex());
   }

   public void testFwdToMatchingCharacterNonSpecial()
   {
      TextCursor cursor = new TextCursor("XxX");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
   }

   public void testFwdToMatchingCharacterNonSpecialRepeated()
   {
      TextCursor cursor = new TextCursor("zZzZz");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(4, cursor.getIndex());
   }

   public void testFwdToMatchingCharacterBeforeStart()
   {
      TextCursor cursor = new TextCursor("zZzZz", -1);
      try
      {
         cursor.fwdToMatchingCharacter();
         assertEquals("Expected exception to be thrown", "Nope");
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   public void testFwdToMatchingCharacterAfterEnd()
   {
      TextCursor cursor = new TextCursor("01", 3);
      try
      {
         cursor.fwdToMatchingCharacter();
         assertEquals("Expected exception to be thrown", "Nope");
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   public void testFwdToMatchingCharacterBrackets()
   {
      TextCursor cursor = new TextCursor("()");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(1, cursor.getIndex());
      cursor = new TextCursor("(  )  ");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
      cursor = new TextCursor("[]");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(1, cursor.getIndex());
      cursor = new TextCursor("[  ]  ");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
      cursor = new TextCursor("{}");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(1, cursor.getIndex());
      cursor = new TextCursor("{  }  ");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
    }

   public void testFwdToMatchingCharacterBracketsNoMatch()
   {
      TextCursor cursor = new TextCursor("(");
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("01(23[{}4", 2);
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      cursor = new TextCursor("[");
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("01[23({}4", 2);
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      cursor = new TextCursor("{");
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("01{23([]4", 2);
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
   }

   public void testFwdToMatchingCharacterNestedBrackets()
   {
      TextCursor cursor = new TextCursor("((()))");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(5, cursor.getIndex());
      cursor = new TextCursor("[[[]]]");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(5, cursor.getIndex());
      cursor = new TextCursor("{{{}}}");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(5, cursor.getIndex());
   }

   public void testFwdToMatchingCharacterNestedBracketsAssorted()
   {
      TextCursor cursor = new TextCursor("({[hello world]})");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(16, cursor.getIndex());
    }

   public void testFwdToMatchingCharacterQuotes()
   {
      TextCursor cursor = new TextCursor("''");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(1, cursor.getIndex());
      cursor = new TextCursor("'  '  ");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
      cursor = new TextCursor(" \"\"", 1);
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      cursor = new TextCursor("\"  \"  ");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
      cursor = new TextCursor("   ``   ", 3);
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(4, cursor.getIndex());
      cursor = new TextCursor("`  `  ");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
    }

   public void testFwdToMatchingCharacterUnbalancedQuotes()
   {
      TextCursor cursor = new TextCursor("'");
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("' \" `  ", 2);
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      cursor = new TextCursor("012\"", 3);
      assertFalse(cursor.fwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
    }

   public void testFwdToMatchingCharacterEscapedQuotes()
   {
      TextCursor cursor = new TextCursor("'\\''");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
      cursor = new TextCursor("'\\'\" '  ");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(5, cursor.getIndex());
      assertTrue(cursor.contentEquals('\''));
      cursor = new TextCursor("\"\\\"\"");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
      assertTrue(cursor.contentEquals('"'));
      cursor = new TextCursor("\"  \\\"  \"");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(7, cursor.getIndex());
      cursor = new TextCursor("   \"\\\"\"   ", 3);
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(6, cursor.getIndex());
      assertTrue(cursor.contentEquals('\"'));
      cursor = new TextCursor("\" \\\" \"  ");
      assertTrue(cursor.fwdToMatchingCharacter());
      assertEquals(5, cursor.getIndex());
    }

   // #endregion
   // #region BwdToMatchingCharacter

   public void testBwdToMatchingCharacterEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertEquals(0, cursor.getIndex());
      assertFalse(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
   }

   public void testBwdToMatchingCharacterNonSpecialAdjacent()
   {
      TextCursor cursor = new TextCursor("XX", 1);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
   }

   public void testBwdToMatchingCharacterNonSpecialNoMatch()
   {
      TextCursor cursor = new TextCursor("X");
      assertFalse(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor(" T ", 1);
      assertFalse(cursor.bwdToMatchingCharacter());
      assertEquals(1, cursor.getIndex());
   }

   public void testBwdToMatchingCharacterNonSpecial()
   {
      TextCursor cursor = new TextCursor("XxX", 2);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
   }

   public void testBwdToMatchingCharacterNonSpecialRepeated()
   {
      TextCursor cursor = new TextCursor("zZzZz", 4);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
   }

   public void testBwdToMatchingCharacterBeforeStart()
   {
      TextCursor cursor = new TextCursor("zZzZz", -1);
      try
      {
         cursor.bwdToMatchingCharacter();
         assertEquals("Expected exception to be thrown", "Nope");
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   public void testBwdToMatchingCharacterAfterEnd()
   {
      TextCursor cursor = new TextCursor("01", 3);
      try
      {
         cursor.bwdToMatchingCharacter();
         assertEquals("Expected exception to be thrown", "Nope");
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   public void testBwdToMatchingCharacterBrackets()
   {
      TextCursor cursor = new TextCursor("()", 1);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("(  )  ", 3);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("[]", 1);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("[  ]  ", 3);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("{}", 1);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("{  }  ", 3);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
    }

   public void testBwdToMatchingCharacterBracketsNoMatch()
   {
      TextCursor cursor = new TextCursor("(");
      assertFalse(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("01(23[{}4", 2);
      assertFalse(cursor.bwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      cursor = new TextCursor("[");
      assertFalse(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("01[23({}4", 2);
      assertFalse(cursor.bwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      cursor = new TextCursor("{");
      assertFalse(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("01{23([]4", 2);
      assertFalse(cursor.bwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
   }

   public void testBwdToMatchingCharacterNestedBrackets()
   {
      TextCursor cursor = new TextCursor("((()))", 5);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("[[[]]]", 4);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(1, cursor.getIndex());
      cursor = new TextCursor("{{{}}}", 5);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
   }

   public void testBwdToMatchingCharacterNestedBracketsAssorted()
   {
      TextCursor cursor = new TextCursor("({[hello world]})", 16);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
    }

   public void testBwdToMatchingCharacterQuotes()
   {
      TextCursor cursor = new TextCursor("''", 1);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("'  '  ", 3);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor(" \"\"", 2);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(1, cursor.getIndex());
      cursor = new TextCursor("\"  \"  ", 3);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("   ``   ", 4);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
      cursor = new TextCursor("`  `  ", 3);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(0, cursor.getIndex());
    }

   // Note: unlike the forward version, this method doesn't pay attention
   // to escaped quotes in the string.
   public void testBwdToMatchingCharacterEscapedQuotes()
   {
      TextCursor cursor = new TextCursor("'\\''", 3);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      cursor = new TextCursor("'\\'\" '  ", 5);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      assertTrue(cursor.contentEquals('\''));
      cursor = new TextCursor("\"\\\"\"", 3);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(2, cursor.getIndex());
      assertTrue(cursor.contentEquals('"'));
      cursor = new TextCursor("\"  \\\"  \"", 7);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(4, cursor.getIndex());
      cursor = new TextCursor("   \"\\\"\"   ", 6);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(5, cursor.getIndex());
      assertTrue(cursor.contentEquals('\"'));
      cursor = new TextCursor("\" \\\" \"  ", 5);
      assertTrue(cursor.bwdToMatchingCharacter());
      assertEquals(3, cursor.getIndex());
    }


   // #endregion
   // #region FwdToCharacterNoSkip

   public void testFwdToCharacterEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertFalse(cursor.fwdToCharacter('a', false));
   }

   public void testFwdToCharacterSingleNonMatchingChar()
   {
      TextCursor cursor = new TextCursor("a");
      assertFalse(cursor.fwdToCharacter('b', false));
   }

   public void testFwdToCharacterMultipleNonMatchingChar()
   {
      TextCursor cursor = new TextCursor("abcdefg012345#$@%#^$#%");
      assertFalse(cursor.fwdToCharacter('9', false));
      assertEquals(0, cursor.getIndex());
   }

   public void testFwdToCharacterSingleMatchingChar()
   {
      TextCursor cursor = new TextCursor("a");
      assertTrue(cursor.fwdToCharacter('a', false));
   }

   public void testFwdToCharacterMultipleMatching()
   {
      TextCursor cursor = new TextCursor("abaa", 1);
      assertTrue(cursor.fwdToCharacter('a', false));
      assertEquals(2, cursor.getIndex());
   }

   public void testFwdToCharacterAtEnd()
   {
      TextCursor cursor = new TextCursor("0123", 4);
      assertFalse(cursor.fwdToCharacter('0', false));
      assertEquals(4, cursor.getIndex());
   }

   public void testFwdToCharacterPastEnd()
   {
      TextCursor cursor = new TextCursor("0123", 5);
      assertFalse(cursor.fwdToCharacter('0', false));
      assertEquals(5, cursor.getIndex());
   }

   public void testFwdToCharacterBeforeStart()
   {
      TextCursor cursor = new TextCursor("0123", -2);
      assertTrue(cursor.fwdToCharacter('2', false));
      assertEquals(2, cursor.getIndex());
   }

   public void testFwdToCharacterInQuotes()
   {
      TextCursor cursor = new TextCursor("```{python roger, fig.cap='hello=world', message=FALSE, echo=TRUE}");
      assertTrue(cursor.fwdToCharacter('=', false));
      assertEquals(25, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToCharacter('=', false));
      assertEquals(32, cursor.getIndex()); // the 'hello=world' instance
      cursor.advance(1);
      assertTrue(cursor.fwdToCharacter('=', false));
      assertEquals(48, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToCharacter('=', false));
      assertEquals(60, cursor.getIndex());
   }

   public void testFwdToCharacterNotIgnoreBraces()
   {
      TextCursor cursor = new TextCursor(" a [a]{a}(a) a");
      assertTrue(cursor.fwdToCharacter('a', false));
      assertEquals(1, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToCharacter('a', false));
      assertEquals(4, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToCharacter('a', false));
      assertEquals(7, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToCharacter('a', false));
      assertEquals(10, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToCharacter('a', false));
      assertEquals(13, cursor.getIndex());
      cursor = new TextCursor("abasjekd{axjdk}x");
      assertTrue(cursor.fwdToCharacter('a', false));
      assertEquals(0, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToCharacter('a', false));
      assertEquals(2, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToCharacter('a', false));
      assertEquals(9, cursor.getIndex());
   }

   // #endregion
   // #region FwdToCharacterSkipBraces
   
   public void testFwdToCharacterSkipEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertFalse(cursor.fwdToCharacter('a', true));
   }

   public void testFwdToCharacterSkipSingleNonMatchingChar()
   {
      TextCursor cursor = new TextCursor("a");
      assertFalse(cursor.fwdToCharacter('b', true));
   }

   public void testFwdToCharacterSkipMultipleNonMatchingChar()
   {
      TextCursor cursor = new TextCursor("abcdefg012345#$@%#^$#%");
      assertFalse(cursor.fwdToCharacter('9', true));
      assertEquals(0, cursor.getIndex());
   }

   public void testFwdToCharacterSkipSingleMatchingChar()
   {
      TextCursor cursor = new TextCursor("a");
      assertTrue(cursor.fwdToCharacter('a', true));
   }

   public void testFwdToCharacterSkipMultipleMatching()
   {
      TextCursor cursor = new TextCursor("abaa", 1);
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(2, cursor.getIndex());
   }

   public void testFwdToCharacterSkipAtEnd()
   {
      TextCursor cursor = new TextCursor("0123", 4);
      assertFalse(cursor.fwdToCharacter('0', true));
      assertEquals(4, cursor.getIndex());
   }

   public void testFwdToCharacterSkipPastEnd()
   {
      TextCursor cursor = new TextCursor("0123", 5);
      try
      {
         cursor.fwdToCharacter('0', true);
         assertEquals("Expected exception to be thrown", "Nope");
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   public void testFwdToCharacterSkipBeforeStart()
   {
      TextCursor cursor = new TextCursor("0123", -2);
      try
      {
         cursor.fwdToCharacter('2', true);
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   public void testFwdToCharacterSkipSingleNonMatchingBrackets()
   {
      TextCursor cursor = new TextCursor("[]");
      assertFalse(cursor.fwdToCharacter('b', true));
      cursor.setIndex(1);
      assertFalse(cursor.fwdToCharacter('[', true));
   }

   public void testFwdToCharacterSkipMatchSingleEnclosed()
   {
      TextCursor cursor = new TextCursor(" [a]");
      assertFalse(cursor.fwdToCharacter('a', true));
      cursor = new TextCursor(" { a}");
      assertFalse(cursor.fwdToCharacter('a', true));
      cursor = new TextCursor(" ( a )");
      assertFalse(cursor.fwdToCharacter('a', true));
      assertEquals(0, cursor.getIndex());
   }

   public void testFwdToCharacterSkipMatchSingleTrailing()
   {
      TextCursor cursor = new TextCursor(" [a] a");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(5, cursor.getIndex());
      cursor = new TextCursor(" {a} a");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(5, cursor.getIndex());
      cursor = new TextCursor(" (a) a");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(5, cursor.getIndex());
    }

   public void testFwdToCharacterSkipMatchSingleUnclosed()
   {
      TextCursor cursor = new TextCursor(" [a a");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(2, cursor.getIndex());
      cursor = new TextCursor(" { a a");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(3, cursor.getIndex());
      cursor = new TextCursor(" (  a a (");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(4, cursor.getIndex());
    }

   public void testFwdToCharacterSkipMatchLeading()
   {
      TextCursor cursor = new TextCursor(" a [a]{a}(a) a");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(1, cursor.getIndex());
      cursor = new TextCursor("abasjekd{axjdk}x");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(0, cursor.getIndex());
      cursor = new TextCursor("a(a)a");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(0, cursor.getIndex());
    }

   public void testFwdToCharacterSkipNoMatchMultipleBraces()
   {
      TextCursor cursor = new TextCursor("[[a]]");
      assertFalse(cursor.fwdToCharacter('a', true));
      cursor = new TextCursor("{[(a)a]}");
      assertFalse(cursor.fwdToCharacter('a', true));
      cursor = new TextCursor("{a[a(a)a]a}");
      assertFalse(cursor.fwdToCharacter('a', true));
      cursor = new TextCursor("{bac[cab(daf)gad]3a$}");
      assertFalse(cursor.fwdToCharacter('a', true));
   }

   public void testFwdToCharacterSkipAfterMultipleBraces()
   {
      TextCursor cursor = new TextCursor("[[a]] hello a world");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(12, cursor.getIndex());
      cursor = new TextCursor("[a{a[xaza]a}aaa]a hello a world");
      assertTrue(cursor.fwdToCharacter('a', true));
      assertEquals(16, cursor.getIndex());
   }

   // #endregion
   // #region FwdToNonQuotedCharacter

   public void testFwdToNonQuotedCharacterEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertFalse(cursor.fwdToNonQuotedCharacter('a'));
   }

   public void testFwdToNonQuotedCharacterSingleNonMatchingChar()
   {
      TextCursor cursor = new TextCursor("a");
      assertFalse(cursor.fwdToNonQuotedCharacter('b'));
   }

   public void testFwdToNonQuotedCharacterMultipleNonMatchingChar()
   {
      TextCursor cursor = new TextCursor("abcdefg012345#$@%#^$#%");
      assertFalse(cursor.fwdToNonQuotedCharacter('9'));
      assertEquals(0, cursor.getIndex());
   }

   public void testFwdToNonQuotedCharacterSingleMatchingChar()
   {
      TextCursor cursor = new TextCursor("a");
      assertTrue(cursor.fwdToNonQuotedCharacter('a'));
   }

   public void testFwdToNonQuotedCharacterMultipleMatching()
   {
      TextCursor cursor = new TextCursor("abaa", 1);
      assertTrue(cursor.fwdToNonQuotedCharacter('a'));
      assertEquals(2, cursor.getIndex());
   }

   public void testFwdToNonQuotedCharacterAtEnd()
   {
      TextCursor cursor = new TextCursor("0123", 4);
      assertFalse(cursor.fwdToNonQuotedCharacter('0'));
      assertEquals(4, cursor.getIndex());
   }

   public void testFwdToNonQuotedCharacterPastEnd()
   {
      TextCursor cursor = new TextCursor("0123", 5);
      assertFalse(cursor.fwdToNonQuotedCharacter('0'));
      assertEquals(5, cursor.getIndex());
   }

   public void testFwdToNonQuotedCharacterBeforeStart()
   {
      TextCursor cursor = new TextCursor("0123", -2);
      assertTrue(cursor.fwdToNonQuotedCharacter('2'));
      assertEquals(2, cursor.getIndex());
   }

   public void testFwdToNonQuotedCharacterInQuotes()
   {
      TextCursor cursor = new TextCursor("```{python roger, fig.cap='hello=world', message=FALSE, echo=TRUE, foo='1=2'}");
      assertTrue(cursor.fwdToNonQuotedCharacter('='));
      assertEquals(25, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToNonQuotedCharacter('='));
      assertEquals(48, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToNonQuotedCharacter('='));
      assertEquals(60, cursor.getIndex());
      cursor.advance(1);
      assertTrue(cursor.fwdToNonQuotedCharacter('='));
      assertEquals(70, cursor.getIndex());
      cursor.advance(1);
      assertFalse(cursor.fwdToNonQuotedCharacter('='));
      assertEquals(71, cursor.getIndex());
   }

   // #endregion
   // #region FindNext

   public void testFindNextEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertEquals(-1, cursor.findNext('\0'));
      assertEquals(-1, cursor.findNext('a'));
      assertEquals(-1, cursor.findNext('('));
      assertEquals(-1, cursor.findNext('4'));
      assertEquals(0, cursor.getIndex());
   }

   public void testFindNextNoNext()
   {
      TextCursor cursor = new TextCursor("a");
      assertEquals(-1, cursor.findNext('a'));
   }

   public void testFindNext()
   {
      TextCursor cursor = new TextCursor("012345012345");
      assertEquals(6, cursor.findNext('0'));
      assertEquals(0, cursor.getIndex());
      assertEquals(1, cursor.findNext('1'));
      cursor.setIndex(3);
      assertEquals(8, cursor.findNext('2'));
   }

   public void testFindNextWhitespace()
   {
      TextCursor cursor = new TextCursor("012 4567\t12345");
      assertEquals(3, cursor.findNext(' '));
      assertEquals(8, cursor.findNext('\t'));
   }

   public void testFindNextNoMatches()
   {
      TextCursor cursor = new TextCursor("012345012345");
      assertEquals(-1, cursor.findNext('a'));
      assertEquals(-1, cursor.findNext('$'));
   }

   public void testFindNextAtEnd()
   {
      TextCursor cursor = new TextCursor("012345", 6);
      assertEquals(-1, cursor.findNext('\0'));
   }

   public void testFindNextBeyondEnd()
   {
      TextCursor cursor = new TextCursor("012345", 7);
      assertEquals(-1, cursor.findNext('\0'));
   }

   // #endregion
   // #region ConsumeChar

   public void testConsumeEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertFalse(cursor.consume('a'));
      assertFalse(cursor.consume('7'));
      assertFalse(cursor.consume('('));
      assertEquals(0, cursor.getIndex());
   }

   public void testConsumeFirstCharNotMatched()
   {
      TextCursor cursor = new TextCursor("zazz");
      assertFalse(cursor.consume('a'));
      assertEquals(0, cursor.getIndex());
   }

   public void testConsumeFirstCharMatched()
   {
      TextCursor cursor = new TextCursor("zzzz");
      assertTrue(cursor.consume('z'));
      assertEquals(1, cursor.getIndex());
      assertTrue(cursor.consume('z'));
      assertEquals(2, cursor.getIndex());
   }

   public void testConsumeMiddleCharMatched()
   {
      TextCursor cursor = new TextCursor("zabz", 1);
      assertTrue(cursor.consume('a'));
      assertEquals(2, cursor.getIndex());
      assertTrue(cursor.consume('b'));
      assertEquals(3, cursor.getIndex());
   }

   public void testConsumeLastCharMatched()
   {
      TextCursor cursor = new TextCursor("zabz", 3);
      assertTrue(cursor.consume('z'));
      assertEquals(4, cursor.getIndex());
   }

   public void testConsumeAtEnd()
   {
      TextCursor cursor = new TextCursor("zabz", 4);
      assertFalse(cursor.consume('\0'));
   }

   public void testConsumeBeyondEnd()
   {
      TextCursor cursor = new TextCursor("zabz", 5);
      try
      {
         cursor.consume('\0');
         assertEquals("Expected exception to be thrown", "Nope");
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   public void testConsumeBeforeStart()
   {
      TextCursor cursor = new TextCursor("zabz", -1);
      try
      {
         cursor.consume('\0');
         assertEquals("Expected exception to be thrown", "Nope");
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   // #endregion
   // #region ConsumeString

   public void testConsumeStringEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertTrue(cursor.consume(""));
      assertFalse(cursor.consume(" "));
      assertFalse(cursor.consume("hello"));
      assertEquals(0, cursor.getIndex());
   }

   public void testConsumeStringOneCharStart()
   {
      TextCursor cursor = new TextCursor("012345");
      assertTrue(cursor.consume("0"));
      assertEquals(1, cursor.getIndex());
   }

   public void testConsumeStringOneCharMiddle()
   {
      TextCursor cursor = new TextCursor("012345", 3);
      assertTrue(cursor.consume("3"));
      assertEquals(4, cursor.getIndex());
   }

   public void testConsumeStringOneCharEnd()
   {
      TextCursor cursor = new TextCursor("012345", 5);
      assertTrue(cursor.consume("5"));
      assertEquals(6, cursor.getIndex());
   }

   public void testConsumeStringStart()
   {
      TextCursor cursor = new TextCursor("012345");
      assertTrue(cursor.consume("0123"));
      assertEquals(4, cursor.getIndex());
   }

   public void testConsumeStringMiddle()
   {
      TextCursor cursor = new TextCursor("012345", 1);
      assertTrue(cursor.consume("1234"));
      assertEquals(5, cursor.getIndex());
   }

   public void testConsumeStringAtEnd()
   {
      TextCursor cursor = new TextCursor("012345", 3);
      assertTrue(cursor.consume("345"));
      assertEquals(6, cursor.getIndex());
   }

   public void testConsumeStringPartialStart()
   {
      TextCursor cursor = new TextCursor("012345");
      assertFalse(cursor.consume("01ab"));
      assertEquals(2, cursor.getIndex());
   }

   public void testConsumeStringPartialMiddle()
   {
      TextCursor cursor = new TextCursor("012345", 3);
      assertFalse(cursor.consume("34b"));
      assertEquals(5, cursor.getIndex());
   }

   public void testConsumeStringPartialAtEnd()
   {
      TextCursor cursor = new TextCursor("012345", 4);
      assertFalse(cursor.consume("45a"));
      assertEquals(6, cursor.getIndex());
   }

   public void testConsumeStringCursorAtEnd()
   {
      TextCursor cursor = new TextCursor("012345", 6);
      assertFalse(cursor.consume("123"));
   }

   public void testConsumeStringBeyondEnd()
   {
      TextCursor cursor = new TextCursor("012345", 6);
      assertFalse(cursor.consume("\0"));
   }

   public void testConsumeStringBeforeStart()
   {
      TextCursor cursor = new TextCursor("012345", -1);
      try
      {
         cursor.consume("\0");
         assertEquals("Expected exception to be thrown", "Nope");
      }
      catch (StringIndexOutOfBoundsException e)
      {
         assertEquals("Exception thrown", "Exception thrown");
      }
   }

   // #endregion
   // #region consumeUntilChar

   public void testConsumeUntilCharEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertFalse(cursor.consumeUntil('a'));
      assertFalse(cursor.consumeUntil('\0'));
   }

   public void testConsumeUntilCharSingleMatchingChar()
   {
      TextCursor cursor = new TextCursor("a");
      assertTrue(cursor.consumeUntil('a'));
      assertEquals(0, cursor.getIndex());
   }

   public void testConsumeUntilCharMatch()
   {
      TextCursor cursor = new TextCursor("0123456");
      assertTrue(cursor.consumeUntil('3'));
      assertEquals(3, cursor.getIndex());
   }

   public void testConsumeUntilCharLastCharMatch()
   {
      TextCursor cursor = new TextCursor("0123456");
      assertTrue(cursor.consumeUntil('6'));
      assertEquals(6, cursor.getIndex());
   }

   public void testConsumeUntilCharAtEnd()
   {
      TextCursor cursor = new TextCursor("012345", 6);
      assertFalse(cursor.consumeUntil('a'));
      assertFalse(cursor.consumeUntil('\0'));
   }

   public void testConsumeUntilCharPastEnd()
   {
      TextCursor cursor = new TextCursor("012345", 9);
      assertFalse(cursor.consumeUntil('a'));
      assertFalse(cursor.consumeUntil('\0'));
   }

   public void testConsumeUntilCharBeforeStart()
   {
      TextCursor cursor = new TextCursor("012345", -2);
      assertFalse(cursor.consumeUntil('a'));
      assertFalse(cursor.consumeUntil('\0'));
   }

   // #endregion
   // #region consumeUntilString

   public void testConsumeUntilStringEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertFalse(cursor.consumeUntil("abc"));
      assertFalse(cursor.consumeUntil("as\0asd"));
   }

   public void testConsumeUntilStringSingleMatchingChar()
   {
      TextCursor cursor = new TextCursor("a");
      assertTrue(cursor.consumeUntil("a"));
      assertEquals(0, cursor.getIndex());
   }

   public void testConsumeUntilStringMatch()
   {
      TextCursor cursor = new TextCursor("0123456");
      assertTrue(cursor.consumeUntil("345"));
      assertEquals(3, cursor.getIndex());
   }

   public void testConsumeUntilStringMatchAtEnd()
   {
      TextCursor cursor = new TextCursor("012345");
      assertTrue(cursor.consumeUntil("345"));
      assertEquals(3, cursor.getIndex());
   }

   public void testConsumeUntilStringAtEnd()
   {
      TextCursor cursor = new TextCursor("012345", 6);
      assertFalse(cursor.consumeUntil("\0"));
   }

   public void testConsumeUntilStringPastEnd()
   {
      TextCursor cursor = new TextCursor("012345", 9);
      assertFalse(cursor.consumeUntil("a"));
      assertFalse(cursor.consumeUntil("\0"));
   }

   public void testConsumeUntilStringBeforeStart()
   {
      TextCursor cursor = new TextCursor("012345", -2);
      assertFalse(cursor.consumeUntil("a"));
      assertFalse(cursor.consumeUntil("\0"));
   }

   // #endregion
   // #region consumeUntilRegex

   public void testConsumeUntilRegExEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertFalse(cursor.consumeUntilRegex("ABC"));
   }

   public void testConsumeUntilRegExNoMatch()
   {
      TextCursor cursor = new TextCursor("helloABCDEFGworld");
      assertFalse(cursor.consumeUntilRegex("abcdefg"));
   }

   public void testConsumeUntilRegExMatch()
   {
      TextCursor cursor = new TextCursor("helloABCDEFGworld");
      assertTrue(cursor.consumeUntilRegex("ABCDEFG"));
      assertEquals(5, cursor.getIndex());
   }

   // #endregion
   // #region consumeUntilNotQuoted

   public void testConsumeUntilNotQuotedEmpty()
   {
      TextCursor cursor = new TextCursor("");
      assertFalse(cursor.consumeUntilNotQuoted());
      assertEquals(0, cursor.getIndex());
   }

   public void testConsumeUntilNotQuotedBeginningNotQuoted()
   {
      TextCursor cursor = new TextCursor("a\"bcd\",'ef', `ijkl` the \"end\"");
      assertFalse(cursor.consumeUntilNotQuoted());
      assertEquals(0, cursor.getIndex());
   }

   public void testConsumeUntilNotQuotedCurrentEndNotQuoted()
   {
      TextCursor cursor = new TextCursor("012\"45\"7");
      cursor.setIndex(cursor.getData().length() - 1);
      assertEquals('7', cursor.peek());
      assertFalse(cursor.consumeUntilNotQuoted());
      assertEquals(7, cursor.getIndex());
   }

   public void testConsumeUntilNotQuotedCurrentMiddleNotQuoted()
   {
      TextCursor cursor = new TextCursor("012\"45\"7");
      cursor.setIndex(2);
      assertEquals('2', cursor.peek());
      assertFalse(cursor.consumeUntilNotQuoted());
      assertEquals(2, cursor.getIndex());
   }

   public void testConsumeUntilNotQuotedEntireStringQuoted()
   {
      TextCursor cursor = new TextCursor("\"1234\"");
      assertTrue(cursor.consumeUntilNotQuoted());
      assertEquals(cursor.getData().length(), cursor.getIndex());
   }

   public void testConsumeUntilNotQuotedStartingInQuotes()
   {
      TextCursor cursor = new TextCursor("01'345'78", 3);
      assertTrue(cursor.consumeUntilNotQuoted());
      assertEquals(7, cursor.getIndex());
      cursor = new TextCursor("01\"345\"78", 4);
      assertTrue(cursor.consumeUntilNotQuoted());
      assertEquals(7, cursor.getIndex());
      cursor = new TextCursor("01`345`78", 5);
      assertTrue(cursor.consumeUntilNotQuoted());
      assertEquals(7, cursor.getIndex());
   }

   public void testConsumeUntilNotQuotedStartingInQuotesMultiple()
   {
      TextCursor cursor = new TextCursor("01'345'78'ab'de", 4);
      assertTrue(cursor.consumeUntilNotQuoted());
      assertEquals(7, cursor.getIndex());
      cursor.setIndex(8);
      assertFalse(cursor.consumeUntilNotQuoted());
      assertEquals(8, cursor.getIndex());
      cursor.setIndex(10); // "a"
      assertTrue(cursor.consumeUntilNotQuoted());
      assertEquals(13, cursor.getIndex());
   }

   // #endregion
   // #region IsQuote

   public void testIsQuoteEmptyString()
   {
      TextCursor cursor = new TextCursor("");
      assertFalse(cursor.isQuote());
   }

   public void testIsQuoteSingleChar()
   {
      TextCursor cursor = new TextCursor("\"'`");
      assertTrue(cursor.isQuote());
      cursor.advance(1);
      assertTrue(cursor.isQuote());
      cursor.advance(1);
      assertTrue(cursor.isQuote());
   }

   public void testIsQuoteMiddle()
   {
      TextCursor cursor = new TextCursor("Hello \"World\", 'zoom', ` the end");
      assertFalse(cursor.isQuote());
      cursor.advance(6);
      assertTrue(cursor.isQuote());
      cursor.advance(6);
      assertTrue(cursor.isQuote());
      cursor.advance(3);
      assertTrue(cursor.isQuote());
      cursor.advance(5);
      assertTrue(cursor.isQuote());
      cursor.advance(3);
      assertTrue(cursor.isQuote());
      cursor.advance(1);
      assertFalse(cursor.isQuote());
   }
   // #endregion

}
