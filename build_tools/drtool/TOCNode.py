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

"""Routines and a class for describing and manipulating data from a
TableOfContents wiki page.
"""

nodes = {}
def lookup(wikiWord):
  if nodes.has_key(wikiWord):
    return nodes[wikiWord]
  else:
    return None

class TOCNode:
  def __init__(self, caption, wikiWord=None):
    self.caption = caption
    self.wikiWord = wikiWord
    self.children = []

    if(wikiWord):
      nodes[wikiWord] = self

  def addChild(self, child):
    self.children.append(child)

  def AsTree(self):
    out = {"caption":self.caption, "wikiword":self.wikiWord}

    if(not self.children):
      out["children"] = None
    else:
      out["children"] = map(TOCNode.AsTree, self.children)

    return out

  def __repr__(self):
    return str(self)

  def __str__(self):
    return "(((%s)))" % self.caption
