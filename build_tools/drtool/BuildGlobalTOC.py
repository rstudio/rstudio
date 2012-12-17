#!/usr/bin/env python
#
# Copyright 2007 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
Routines for constructing a TOCNode from a file on disk.
"""

from TOCNode import TOCNode
import re

def BuildTOC(toc_filename, displayname):
  """This will return a list of toplevel TOCNodes, each of which may have
  nested children."""

  infile = open(toc_filename)
  bytes = infile.read()
  lines = bytes.split("\n")

  out = TOCNode(displayname)
  parentstack = [(out, -1)]
  previndent = 0
  prevnode = None

  for line in lines:
    if line.startswith("#"): continue

    indentlevel = line.find("*")
    if -1 == indentlevel: continue

    postindent = line[indentlevel:]
    poststar = postindent[2:]

    if poststar.startswith('['):
      splitted  = re.split("[\[\] ]", poststar)
      wikiWord = splitted[1]
      caption = " ".join(splitted[2:])
      caption = caption.replace("&", "and")
      caption = caption.strip()

      tocnode = TOCNode(caption, wikiWord)
    else:
      caption = poststar
      caption = caption.replace("&", "and")
      caption = caption.strip()

      tocnode = TOCNode(caption)

    if indentlevel > previndent:
      if prevnode:
        parentstack.append((prevnode, previndent))
    elif indentlevel < previndent:
      while (parentstack and parentstack[-1][1] >= indentlevel ):
        parentstack.pop()

    parentstack[-1][0].addChild(tocnode)

    prevnode = tocnode
    previndent = indentlevel
  return out.AsTree()
