# Copyright (c) 2008, 2009, 2010, 2011 jerome DOT laurens AT u-bourgogne DOT fr
#
# This file is part of the SyncTeX package.
#
# License:
# --------
# Permission is hereby granted, free of charge, to any person
# obtaining a copy of this software and associated documentation
# files (the "Software"), to deal in the Software without
# restriction, including without limitation the rights to use,
# copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following
# conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
# OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
# HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
# OTHER DEALINGS IN THE SOFTWARE
#
# Except as contained in this notice, the name of the copyright holder  
# shall not be used in advertising or otherwise to promote the sale,  
# use or other dealings in this Software without prior written  
# authorization from the copyright holder.
#
# Acknowledgments:
# ----------------
# The author received useful remarks from the pdfTeX developers, especially Hahn The Thanh,
# and significant help from XeTeX developer Jonathan Kew
# 
# Nota Bene:
# ----------
# If you include or use a significant part of the synctex package into a software,
# I would appreciate to be listed as contributor and see "SyncTeX" highlighted.
# 
# Version 1
# Thu Jun 19 09:39:21 UTC 2008


This file gives the .synctex file specifications.

SyncTeX output file specifications, Version 2, Lun 31 mar 2008 13:50:31 UTC
===========================================================================
This is an EBNF file specification extended by ICU regex patterns
(enclosed between 2 '/'s)

The whole synctex file is made of various records gathered into four different sections:

<synctex file> ::= <preamble> <contents> <postamble> <post scriptum>

Each record is a sequence of text characters following an end of record mark or starting the file,
and ending with an end of record mark.
The first characters of a record will determine the type of the record.

<end of record mark> ::= /\n/
<end of record> ::= /[^\n]*/<end of record mark>

The preamble:
-------------

<preamble> ::= <control record> <input record>* <informations> <forthcoming records>

<control record> ::= /SyncTeX Version:/<version><end of record mark>
<SyncTeX> ::= 
<version> ::= /1/

<input record> ::= /Input:/<tag information><end of record mark>
<tag information> ::= <tag>/:/<file name><end of record mark>
<tag> ::= <integer>
<file name> ::= /[^<end of record mark>]*/
This is used to give a shortcut to filenames.

<informations> ::= <output record>? <magnification record>? <unit record>? <x offset record>? <y offset record>?

<output record> ::= /Output:/<output><end of record mark>
<output> ::= /dvi|pdf|xdv|[0-9a-zA-Z]*/

<magnification record> ::= /Magnification:/<magnification><end of record>
<magnification> ::= <integer>
This is the TeX magnification.

<unit record> ::= /Unit:<unit><end of record>/
<unit> ::= <integer>
The SyncTeX unit is <unit> scaled point, 1 in general, 8192 when not given.

<x offset record> ::= /X Offset:/<x offset><end of record>
<x offset> ::= <integer>
<y offset record> ::= /Y Offset:/<y offset><end of record>
<y offset> ::= <integer>
The offset or the origin of the system of coordinates from the top left point of the page.
This defaults to 1in for both the vertical and horizontal offsets.
Both offsets are given in this section in scaled point unit.

<forthcoming records>: The preamble, like any other section may contain in the future any other kind of record,
except the one starting the next section. In order to ensure some forwards compatibility,
parsers should anticipate and parse unknown records: an unexpected record should be silently ignored by the parser.
This means that this format is somehow open an more types of records can be added without breaking existing software.

The preamble ends when a record is found that fits the following section.

The contents:
-------------
<contents> ::= <contents record><sheet(1)><i line>*<sheet(2)><i line>*...<sheet(N)><i line>*

<contents record>::=/Contents:/<byte offset><end of record>/
<offset> ::= <byte offset>
<sheet(n)> ::= <sheet(n) start><v content>*<sheet end>
<sheet(n) start> ::= /{/<byte offset>/:/<the integer n><end of record>/
<sheet end> ::= /}/<byte offset><end of record>

<box content> ::= <vbox section>|<hbox section>|<void vbox record>|<void hbox record>
<vbox section> ::= (<vbox start record><vbox content>*<vbox stop record>)|<box content>*
<hbox content> ::= <glue record>|<kern record>|<math record>|<box content>
<hbox section> ::= <hbox start record><box content>*<hbox stop record> 

<void vbox record> ::= /v/<link>/:/<point>/:/<size><end of record>
<void hbox record> ::= /h/<link>/:/<point>/:/<size><end of record>
<size> ::= <W>/,/<H>/,/<D>
<W> ::= <integer>
<H> ::= <integer>
<D> ::= <integer>
<link> ::= <tag>/,/<line>(/,/<column>)?
<line> ::= <integer>
<column> ::= <integer>

<current record> ::= /x/<link>/:/<point><end of record>
<kern record> ::= /k/<link>/:/<point>/:/<W><end of record>
<glue record> ::= /g/<link>/:/<point><end of record>
<math record> ::= /$/<link>/:/<point><end of record>

The byte offset is an implicit anchor to navigate the synctex file from sheet to sheet.

The postamble:
--------------
The postamble closes the file
If there is no postamble, it means that the typesetting process did not end correctly.
<postamble> ::= <postamble record><number of objects record>

<postamble record> ::= /Postamble:/<byte offset><end of record>
<number of objects record> ::= /Count:/<integer><end of record>

The post scriptum:
------------------
The post scriptum contains material possibly added by 3rd parties.
It allows to append some transformation (shift and magnify).
Typically, one applies a dvi to pdf filter with offset options and magnification,
then he appends the same options to the synctex file, for example

	dvipdfmx -m 0.486 -x 9472573sp -y 13.3dd source.dvi
	echo "X Offset:9472573" >> source.synctex
	echo "Y Offset:13.3dd" >> source.synctex
	echo "Magnification:0.486" >> source.synctex


<post scriptum> ::= (<magnification line>|<x offset line>|<y offset line>)*
<magnification line> ::= /Magnification:/<post magnification><end of line mark>
<post magnification> ::= <unsigned decimal float>
<x offset line> ::= /X Offset:/<post x offset><end of line mark>
<post x offset> ::= <sign><unsigned decimal float><offset unit>
<y offset> ::= <x offset>
<sign> ::= /(+|-)?/
<offset unit> ::= /(in|cm|mm|pt|bp|pc|sp|dd|cc|nd|nc)?/
<end of line mark> ::= /[\n\r]*/
<y offset line> ::= /Y Offset:/<post y offset><end of line mark>
<post y offset> ::= <sign><unsigned decimal float><offset unit>

This second information will override the offset and magnification previously available in the preamble section.
All the numbers are encoded using the decimal representation with "C" locale.

