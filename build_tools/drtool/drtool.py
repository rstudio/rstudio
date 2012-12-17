#!/usr/bin/env python
#
# Copyright 2008 Google Inc.
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

"""drtool is for moving your wiki documents between googlecode projects.

drtool actually does three things:
  - copy all of your wiki documentation between googlecode projects. This is
    done via the "copy" command. References to a project will be changed during
    the move.

  - tell you what wiki pages are not referenced by your docreader-enabled
    TableOfContents page. Use "nothave" or "orphans" or "list-orphans" for
    this.

  - find all of the http references to your subversion repository in your wiki
    docs. "references" or "refs".

It's assumed that you have svn checkouts of both projects.
"""

import sys
import shutil
import re
import os.path
import glob
import TOCNode
from BuildGlobalTOC import BuildTOC

NOTHAVE = ["nothave", "orphans", "list-orphans"]
REFERENCES = ["references", "refs"]
COPY = ["copy"]

wikipatterns = {}
svnpatterns = {}

def getpattern(table, template, projectname):
  if table.has_key(projectname):
    return table[projectname]
  else:
    pattern = (template % projectname)
    compiled = re.compile(pattern)
    table[projectname] = compiled
    return compiled

def wikipattern(projectname):
  return getpattern(wikipatterns,
                    "http://code.google.com/p/(%s)/wiki/([^\s\]]+)",
                    projectname)

def svnpattern(projectname):
  return getpattern(svnpatterns,
                    "http://(%s).googlecode.com([^\s\]]+)",
                    projectname)

def change_ref(link, oldproject, newproject):
  return link.replace(oldproject, newproject)

def change_references(bytes, oldprojectname, newprojectname):
  """Returns the string that is the new text for the wiki page, with all
  of the references changed to point to the new project."""

  svn = svnpattern(oldprojectname)
  wiki = wikipattern(oldprojectname)

  postsvn = ""
  postwiki = ""
  ## step one, do all the svn references.
  svnreferences = re.finditer(svn, bytes)
  append_start = 0

  for match in svnreferences:
    matchstart, matchend = match.span()

    ## take all of the stuff from the end of the last match to the beginning
    ## of this one and append it to the output.
    postsvn += bytes[append_start:matchstart]

    ## append the version of this match with the project names replaced
    postsvn += change_ref(match.group(0), oldprojectname, newprojectname)
    append_start = matchend
  postsvn += bytes[append_start:]

  ## step two, do all the wiki references (should be rarer?)
  wikireferences = re.finditer(wiki, postsvn)
  append_start = 0

  for match in wikireferences:
    matchstart, matchend = match.span()

    ## take all of the stuff from the end of the last match to the beginning
    ## of this one and append it to the output.
    postwiki += postsvn[append_start:matchstart]

    ## append the version of this match with the project names replaced
    postwiki += change_ref(match.group(0),
                           oldprojectname,
                           newprojectname)
    append_start = matchend
  postwiki += postsvn[append_start:]

  return postwiki

def find_references(bytes, projectname, fn):
  """Return a list of all the references to a given project in this string"""
  def find_refs(pattern):
    compiled = re.compile(pattern)
    refs  = re.findall(compiled, bytes)
    return refs

  svn = svnpattern(projectname)
  wiki = wikipattern(projectname)

  return find_refs(svn) + find_refs(wiki)

def print_references(bytes, projectname, fn):
  refs = find_references(bytes, projectname, fn)
  if refs:
    print ("** %s" % fn)
    for ref in refs:
      print ":::", ref

def usage():
  commands = (", ").join(NOTHAVE + REFERENCES + COPY)
  print ("usage: %s <cmd> prj1 [prj2]" % sys.argv[0])
  print "\tcmd is one of: ", commands 
  print "\tprj2 is required for 'copy' command"

def copy_into_target(source_project, target_project, source_path,
                     relative_filename, target_dir):
  """Copy one file into the target directory."""
  target_path = os.path.join(target_dir, relative_filename)

  if relative_filename.endswith(".wiki"):
    infile = open(source_path)
    bytes = infile.read()
    changed = change_references(bytes, source_project, target_project)

    head,tail = os.path.split(target_path)
    if not os.path.isdir(head):
      os.makedirs(head)

    outfile = open(target_path, "w")
    outfile.write(changed)
    outfile.close()

    # print ("%s: changed references, wrote into %s." %
    #        (relative_filename, target_project) )
  else:
    shutil.copyfile(source_path, target_path)
    # print ("%s: not a wiki file, copied into the new %s" %
    #        (relative_filename, target_project))

  pass

def find_wikipath(target):
  normalized = os.path.normpath(target)

  if not normalized.endswith("/wiki"):
    normalized += "/wiki"
  return normalized

def all_wiki_docs(wikipath):
  """All of the .wiki files in our wiki directory, with paths, recursively."""
  out = []
  for root, dirs, fileshere in os.walk(wikipath):
    path_elements = root.split(os.sep)
    if ".svn" in path_elements:
      continue

    for fn in fileshere:
      if fn.endswith(".wiki"):
        whole_pathname = os.path.join(root, fn)
        out.append(whole_pathname)
  return out

def nothave(wikipath, projectname):
  toc_path = wikipath + os.path.sep + "TableOfContents.wiki"
  if not os.path.exists(toc_path):
    print "Could not find TableOfContents.wiki for that project."
    return

  listed = BuildTOC(toc_path, projectname)
  all_wiki_pages = all_wiki_docs(wikipath)

  ## run through every .wiki file (recursively under the wiki directory, and if
  ## it's not mentioned in the TOC, print it out.

  for wikipage in all_wiki_pages:
    basename = os.path.basename(wikipage)
    wikiword = basename[:-5]
    node = TOCNode.lookup(wikiword)

    if not node:
      print " ", wikipage

def file_references(fn, projectname):
  infile = open(fn)
  bytes = infile.read()
  print_references(bytes, projectname, fn)

def references(wikipath, projectname):
  wikipaths = all_wiki_docs(wikipath)

  for wikipath  in wikipaths:
    file_references(wikipath, projectname)

def wikicopy(source_dir, source_project, target_dir, target_project):
  print "Copying from", source_project,
  print "to", target_project, "and changing references...",

  ## Walk every file in source_dir. Skip .svn directories.
  for root, dirs, fileshere in os.walk(source_dir):
    path_elements = root.split(os.sep)
    if ".svn" in path_elements:
      continue

    for filename in fileshere:
      ## Find the part of the path after source_dir
      whole_pathname = os.path.join(root, filename)
      after_source_dir = whole_pathname[len(source_dir):]
      if after_source_dir.startswith("/"):
        after_source_dir = after_source_dir[1:]

      copy_into_target(source_project, target_project, whole_pathname,
                       after_source_dir, target_dir)

  print "OK!"
  ## Warn about wiki pages that are not referenced by TOC.
  print "Careful: these wiki pages aren't referenced in your TableOfContents:"
  nothave(source_dir, source_project)
  print "Done."

def main(argv):
  if len(argv) < 3:
    usage()
    return

  source_dir = find_wikipath(argv[2])
  source_projectname = source_dir.split(os.path.sep)[-2]

  if len(argv) > 3:
    target_dir = find_wikipath(argv[3])
    target_projectname = target_dir.split(os.path.sep)[-2]

  if argv[1] in NOTHAVE:
    nothave(source_dir, source_projectname)
  elif argv[1] in REFERENCES:
    references(source_dir, source_projectname)
  elif argv[1] in COPY:
    if not target_dir:
      usage()
      return
    wikicopy(source_dir, source_projectname, target_dir, target_projectname)

if __name__ == '__main__':
  main(sys.argv)
