/*
 * PanmirrorCommands.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio; then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY; INCLUDING THOSE OF NON-INFRINGEMENT;
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.panmirror.command;



public class PanmirrorCommands
{
   // text editing
   public static final String Undo = "201CA961-829E-4708-8FBC-8896FDE85A10";
   public static final String Redo = "B6272475-04E0-48C0-86E3-DAFA763BDF7B";
   public static final String SelectAll = "E42BF0DA-8A02-4FCE-A202-7EA8A4833FC5";
   public static final String ClearFormatting = "C22D8CC4-0A9F-41D5-B540-7DAAAB80F344";

   // formatting
   public static final String Strong = "83B04020-1195-4A65-8A8E-7C173C87F439";
   public static final String Em = "9E1B73E4-8140-43C3-92E4-A5E2583F40E6";
   public static final String Code = "32621150-F829-4B8F-B5BD-627FABBBCF53";
   public static final String Strikeout = "D5F0225B-EC73-4600-A1F3-01F418EE8CB4";
   public static final String Superscript = "0200D2FC-B5AF-423B-8B7A-4A7FC3DAA6AF";
   public static final String Subscript = "3150428F-E468-4E6E-BF53-A2713E59B4A0";
   public static final String Smallcaps = "41D8030F-5E8B-48F2-B1EE-6BC40FD502E4";
   public static final String Paragraph = "20EC2695-75CE-4DCD-A644-266E9F5F5913";
   public static final String Heading1 = "5B77642B-923D-4440-B85D-1A27C9CF9D77";
   public static final String Heading2 = "42985A4B-6BF2-4EEF-AA30-3E84A8B9111C";
   public static final String Heading3 = "3F84D9DF-5EF6-484C-8615-BAAE2AC9ECE2";
   public static final String Heading4 = "DA76731D-1D84-4DBA-9BEF-A6F73536F0B9";
   public static final String Heading5 = "59E24247-A140-466A-BC96-3C8ADABB57A5";
   public static final String Heading6 = "DB495DF5-8501-43C7-AE07-59CE9D9C373D";
   public static final String CodeBlock = "3BA12A49-3E29-4ABC-9A49-436A3B49B880";
   public static final String CodeBlockFormat = "07A6F2AA-01DC-41D7-9F01-AA91EAD856EE";
   public static final String Blockquote = "AF0717E7-E4BA-4909-9F10-17EB757CDD0F";
   public static final String LineBlock = "F401687C-B995-49AF-B2B0-59C158174FD5";
   public static final String AttrEdit = "0F8A254D-9272-46BF-904D-3A9D68B91032";
   public static final String Span = "852CF3E3-8A2B-420D-BD95-F79C54118E7E";
   public static final String Div = "15EDB8F1-6015-4DA9-AE50-5987B24C1D96";
   public static final String InsertDiv = "ACA1521B-8875-4113-9D43-B47F0038B19F";
   public static final String InsertSymbol = "1419765F-6E4A-4A4C-8670-D9E8578EA996";
   public static final String InsertEmoji = "F73896A2-02CC-4E5D-A596-78444A1D2A37";

   // lists
   public static final String BulletList = "D897FD2B-D6A4-44A7-A404-57B5251FBF64";
   public static final String OrderedList = "3B8B82D5-7B6C-4480-B7DD-CF79C6817980";
   public static final String TightList = "A32B668F-74F3-43D7-8759-6576DDE1D603";
   public static final String ListItemSink = "7B503FA6-6576-4397-89EF-37887A1B2EED";
   public static final String ListItemLift = "53F89F57-22E2-4FCC-AF71-3E382EC10FC8";
   public static final String ListItemSplit = "19BBD87F-96D6-4276-B7B8-470652CF4106";
   public static final String ListItemCheck = "2F6DA9D8-EE57-418C-9459-50B6FD84137F";
   public static final String ListItemCheckToggle = "34D30F3D-8441-44AD-B75A-415DA8AC740B";
   public static final String EditListProperties = "E006A68C-EA39-4954-91B9-DDB07D1CBDA2";

   // tables
   public static final String TableInsertTable = "FBE39613-2DAA-445D-9E92-E1EABFB33E2C";
   public static final String TableToggleHeader = "A5EDA226-A3CA-4C1B-8D4D-C2675EF51AFF";
   public static final String TableToggleCaption = "C598D85C-E15C-4E10-9850-95882AEC7E60";
   public static final String TableNextCell = "14299819-3E19-4A27-8D0B-8035315CF0B4";
   public static final String TablePreviousCell = "0F041FB5-0203-4FF1-9D13-B16606A80F3E";
   public static final String TableAddColumnBefore = "2447B81F-E07A-4C7D-8026-F2B148D5FF4A";
   public static final String TableAddColumnAfter = "ED86CFAF-D0B3-4B1F-9BB8-89987A939C8C";
   public static final String TableDeleteColumn = "B3D077BC-DD51-4E3A-8AD4-DE5DE686F7C4";
   public static final String TableAddRowBefore = "E97FB318-4052-41E5-A2F5-55B64E9826A5";
   public static final String TableAddRowAfter = "3F28FA24-4BDD-4C13-84FF-9C5E1D4B04D6";
   public static final String TableDeleteRow = "5F3B4DCD-5006-43A5-A069-405A946CAC68";
   public static final String TableDeleteTable = "116D1E68-9315-4FEB-B6A0-AD25B3B9C881";
   public static final String TableAlignColumnLeft = "0CD6A2A4-06F9-435D-B8C9-070B22B19D8";
   public static final String TableAlignColumnRight = "86D90C12-BB12-4A9D-802F-D00EB7CEF2C5";
   public static final String TableAlignColumnCenter = "63333996-2F65-4586-8494-EA9CAB5A7751";
   public static final String TableAlignColumnDefault = "7860A9C1-60AF-40AD-9EB8-A10F6ADF25C5";

   // insert
   public static final String OmniInsert = "12F96C13-38C1-4266-A0A1-E871D8C709FB";
   public static final String Link = "842FCB9A-CA61-4C5F-A0A0-43507B4B3FA9";
   public static final String RemoveLink = "072D2084-218D-4A34-AF1F-7E196AF684B2";
   public static final String Image = "808220A3-2B83-4CB6-BCC1-46565D54FA47";
   public static final String Footnote = "1D1A73C0-F0E1-4A0F-BEBC-08398DE14A4D";
   public static final String ParagraphInsert = "4E68830A-3E68-450A-B3F3-2591F4EB6B9A";
   public static final String HorizontalRule = "EAA7115B-181C-49EC-BDB1-F0FF10369278";
   public static final String YamlMetadata = "431B5A45-1B25-4A55-9BAF-C0FE95D9B2B6";
   public static final String Shortcode = "0FDDA7E8-419D-4A5D-A1F5-74061466655D";
   public static final String Citation = "EFFCFC81-F2E7-441E-B7FA-C693146B4185";
   public static final String CrossReference = "48CEED4F-1D18-4AF9-8686-9FEB5DF6BCC8";
   public static final String DefinitionList = "CFAB8F4D-3350-4398-9754-8DE0FB95167B";
   public static final String DefinitionTerm = "204D1A8F-8EE6-424A-8E69-99768C85B39E";
   public static final String DefinitionDescription = "F0738D83-8E11-4CB5-B958-390190A2D7DD";
   public static final String EmDash = "5B0DD33B-6209-4713-B8BB-60B5CA0BC3B3";
   public static final String EnDash = "C32AFE32-0E57-4A16-9C39-88EB1D82B8B4";
   public static final String NonBreakingSpace = "CF6428AB-F36E-446C-8661-2781B2CD1169";
   public static final String HardLineBreak = "3606FF87-866C-4729-8F3F-D065388FC339";
   
   // raw
   public static final String TexInline = "CFE8E9E5-93BA-4FFA-9A77-BA7EFC373864";
   public static final String TexBlock = "BD11A6A7-E528-40A2-8139-3F8F5F556ED2";
   public static final String InlineMath = "A35C562A-0BD6-4B14-93D5-6FF3BE1A0C8A";
   public static final String DisplayMath = "3E36BA99-2AE9-47C3-8C85-7CC5314A88DF";
   public static final String HTMLComment = "F973CBA4-2882-4AC5-A642-47F4733EBDD4";
   public static final String HTMLInline = "C682C6B5-E58D-498C-A38F-FB07BEC3A82D";
   public static final String HTMLBlock = "6F9F64AF-711F-4F91-8642-B51C41717F31";
   public static final String RawInline = "984167C8-8582-469C-97D8-42CB12773657";
   public static final String RawBlock = "F5757992-4D33-45E6-86DC-F7D7B174B1EC";
   
   // chunk
   public static final String RCodeChunk = "EBFD21FF-4A6E-4D88-A2E0-B38470B00BB9";
   public static final String BashCodeChunk = "5FBB7283-E8AB-450C-9359-A4658CBCD136";
   public static final String D3CodeChunk = "C73CA46C-B56F-40B6-AEFA-DDBB30CA8C08";
   public static final String PythonCodeChunk = "42A7A138-421A-4DCF-8A88-FE2F8EC5B8F6";
   public static final String RcppCodeChunk = "6BD2810B-6B20-4358-8AA4-74BBFFC92AC3";
   public static final String SQLCodeChunk = "41D61FD2-B56B-48A7-99BC-2F60BC0D9F78";
   public static final String StanCodeChunk = "65D33344-CBE9-438C-B337-A538F8D7FCE5";
   
   // outline
   public static final String GoToNextSection = "AE827BDA-96F8-4E84-8030-298D98386765";
   public static final String GoToPreviousSection = "E6AA728C-2B75-4939-9123-0F082837ACDF";
}
