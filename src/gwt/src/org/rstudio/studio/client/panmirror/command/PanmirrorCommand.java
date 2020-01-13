/*
 * PanmirrorEditorCommand.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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

package org.rstudio.studio.client.panmirror.command;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class PanmirrorCommand
{
   public String id;
   public String[] keymap;
   public native boolean isEnabled();
   public native boolean isActive();
   public native void execute();
   
   // text editing
  @JsOverlay public static final String Undo = "201CA961-829E-4708-8FBC-8896FDE85A10";
  @JsOverlay public static final String Redo = "B6272475-04E0-48C0-86E3-DAFA763BDF7B";
  @JsOverlay public static final String SelectAll = "E42BF0DA-8A02-4FCE-A202-7EA8A4833FC5";

   // formatting
  @JsOverlay public static final String Strong = "83B04020-1195-4A65-8A8E-7C173C87F439";
  @JsOverlay public static final String Em = "9E1B73E4-8140-43C3-92E4-A5E2583F40E6";
  @JsOverlay public static final String Code = "32621150-F829-4B8F-B5BD-627FABBBCF53";
  @JsOverlay public static final String Strikeout = "D5F0225B-EC73-4600-A1F3-01F418EE8CB4";
  @JsOverlay public static final String Superscript = "0200D2FC-B5AF-423B-8B7A-4A7FC3DAA6AF";
  @JsOverlay public static final String Subscript = "3150428F-E468-4E6E-BF53-A2713E59B4A0";
  @JsOverlay public static final String Smallcaps = "41D8030F-5E8B-48F2-B1EE-6BC40FD502E4";
  @JsOverlay public static final String Paragraph = "20EC2695-75CE-4DCD-A644-266E9F5F5913";
  @JsOverlay public static final String Heading1 = "5B77642B-923D-4440-B85D-1A27C9CF9D77";
  @JsOverlay public static final String Heading2 = "42985A4B-6BF2-4EEF-AA30-3E84A8B9111C";
  @JsOverlay public static final String Heading3 = "3F84D9DF-5EF6-484C-8615-BAAE2AC9ECE2";
  @JsOverlay public static final String Heading4 = "DA76731D-1D84-4DBA-9BEF-A6F73536F0B9";
  @JsOverlay public static final String Heading5 = "59E24247-A140-466A-BC96-3C8ADABB57A5";
  @JsOverlay public static final String Heading6 = "DB495DF5-8501-43C7-AE07-59CE9D9C373D";
  @JsOverlay public static final String CodeBlock = "3BA12A49-3E29-4ABC-9A49-436A3B49B880";
  @JsOverlay public static final String Blockquote = "AF0717E7-E4BA-4909-9F10-17EB757CDD0F";
  @JsOverlay public static final String LineBlock = "F401687C-B995-49AF-B2B0-59C158174FD5";
  @JsOverlay public static final String AttrEdit = "0F8A254D-9272-46BF-904D-3A9D68B91032";
  @JsOverlay public static final String Span = "852CF3E3-8A2B-420D-BD95-F79C54118E7E";
  @JsOverlay public static final String Div = "15EDB8F1-6015-4DA9-AE50-5987B24C1D96";

   // lists
  @JsOverlay public static final String BulletList = "D897FD2B-D6A4-44A7-A404-57B5251FBF64";
  @JsOverlay public static final String OrderedList = "3B8B82D5-7B6C-4480-B7DD-CF79C6817980";
  @JsOverlay public static final String TightList = "A32B668F-74F3-43D7-8759-6576DDE1D603";
  @JsOverlay public static final String ListItemSink = "7B503FA6-6576-4397-89EF-37887A1B2EED";
  @JsOverlay public static final String ListItemLift = "53F89F57-22E2-4FCC-AF71-3E382EC10FC8";
  @JsOverlay public static final String ListItemSplit = "19BBD87F-96D6-4276-B7B8-470652CF4106";
  @JsOverlay public static final String ListItemCheck = "2F6DA9D8-EE57-418C-9459-50B6FD84137F";
  @JsOverlay public static final String ListItemCheckToggle = "34D30F3D-8441-44AD-B75A-415DA8AC740B";
  @JsOverlay public static final String OrderedListEdit = "E006A68C-EA39-4954-91B9-DDB07D1CBDA2";

   // tables
  @JsOverlay public static final String TableInsertTable = "FBE39613-2DAA-445D-9E92-E1EABFB33E2C";
  @JsOverlay public static final String TableToggleHeader = "A5EDA226-A3CA-4C1B-8D4D-C2675EF51AFF";
  @JsOverlay public static final String TableToggleCaption = "C598D85C-E15C-4E10-9850-95882AEC7E60";
  @JsOverlay public static final String TableNextCell = "14299819-3E19-4A27-8D0B-8035315CF0B4";
  @JsOverlay public static final String TablePreviousCell = "0F041FB5-0203-4FF1-9D13-B16606A80F3E";
  @JsOverlay public static final String TableAddColumnBefore = "2447B81F-E07A-4C7D-8026-F2B148D5FF4A";
  @JsOverlay public static final String TableAddColumnAfter = "ED86CFAF-D0B3-4B1F-9BB8-89987A939C8C";
  @JsOverlay public static final String TableDeleteColumn = "B3D077BC-DD51-4E3A-8AD4-DE5DE686F7C4";
  @JsOverlay public static final String TableAddRowBefore = "E97FB318-4052-41E5-A2F5-55B64E9826A5";
  @JsOverlay public static final String TableAddRowAfter = "3F28FA24-4BDD-4C13-84FF-9C5E1D4B04D6";
  @JsOverlay public static final String TableDeleteRow = "5F3B4DCD-5006-43A5-A069-405A946CAC68";
  @JsOverlay public static final String TableDeleteTable = "116D1E68-9315-4FEB-B6A0-AD25B3B9C881";
  @JsOverlay public static final String TableAlignColumnLeft = "0CD6A2A4-06F9-435D-B8C9-070B22B19D8";
  @JsOverlay public static final String TableAlignColumnRight = "86D90C12-BB12-4A9D-802F-D00EB7CEF2C5";
  @JsOverlay public static final String TableAlignColumnCenter = "63333996-2F65-4586-8494-EA9CAB5A7751";
  @JsOverlay public static final String TableAlignColumnDefault = "7860A9C1-60AF-40AD-9EB8-A10F6ADF25C5";

   // insert
  @JsOverlay public static final String Link = "842FCB9A-CA61-4C5F-A0A0-43507B4B3FA9";
  @JsOverlay public static final String Image = "808220A3-2B83-4CB6-BCC1-46565D54FA47";
  @JsOverlay public static final String Footnote = "1D1A73C0-F0E1-4A0F-BEBC-08398DE14A4D";
  @JsOverlay public static final String ParagraphInsert = "4E68830A-3E68-450A-B3F3-2591F4EB6B9A";
  @JsOverlay public static final String HorizontalRule = "EAA7115B-181C-49EC-BDB1-F0FF10369278";
  @JsOverlay public static final String InlineMath = "A35C562A-0BD6-4B14-93D5-6FF3BE1A0C8A";
  @JsOverlay public static final String DisplayMath = "3E36BA99-2AE9-47C3-8C85-7CC5314A88DF";
  @JsOverlay public static final String RawInline = "984167C8-8582-469C-97D8-42CB12773657";
  @JsOverlay public static final String RawBlock = "F5757992-4D33-45E6-86DC-F7D7B174B1EC";
  @JsOverlay public static final String YamlMetadata = "431B5A45-1B25-4A55-9BAF-C0FE95D9B2B6";
  @JsOverlay public static final String RmdChunk = "EBFD21FF-4A6E-4D88-A2E0-B38470B00BB9";
  @JsOverlay public static final String InlineLatex = "CFE8E9E5-93BA-4FFA-9A77-BA7EFC373864";
  @JsOverlay public static final String Citation = "EFFCFC81-F2E7-441E-B7FA-C693146B4185";
  @JsOverlay public static final String DefinitionList = "CFAB8F4D-3350-4398-9754-8DE0FB95167B";
  @JsOverlay public static final String DefinitionTerm = "204D1A8F-8EE6-424A-8E69-99768C85B39E";
}
