#!/usr/bin/python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

"""This is the entry point to create Elemental APIs from the IDL database."""

import elementalgenerator
import database
import logging.config
import optparse
import os
import shutil
import subprocess
import sys

_logger = logging.getLogger('elementaldomgenerator')

_webkit_renames = {
    # W3C -> WebKit name conversion
    # TODO(vsm): Maybe Store these renames in the IDLs.
    'ApplicationCache': 'DOMApplicationCache',
    'BarProp': 'BarInfo',
    'DedicatedWorkerGlobalScope': 'DedicatedWorkerContext',
    'FormData': 'DOMFormData',
    'Selection': 'DOMSelection',
    'SharedWorkerGlobalScope': 'SharedWorkerContext',
    'Window': 'DOMWindow',
    'WorkerGlobalScope': 'WorkerContext'}

_html_strip_webkit_prefix_classes = [
    'Animation',
    'AnimationEvent',
    'AnimationList',
    'BlobBuilder',
    'CSSKeyframeRule',
    'CSSKeyframesRule',
    'CSSMatrix',
    'CSSTransformValue',
    'Flags',
    'LoseContext',
    'Point',
    'TransitionEvent']

def HasAncestor(interface, names_to_match, database):
  for parent in interface.parents:
    if (parent.type.id in names_to_match or
        (database.HasInterface(parent.type.id) and
        HasAncestor(database.GetInterface(parent.type.id), names_to_match,
            database))):
      return True
  return False

def _MakeHtmlRenames(common_database):
  html_renames = {}

  for interface in common_database.GetInterfaces():
    if (interface.id.startswith("HTML") and
        HasAncestor(interface, ['Element', 'Document'], common_database)):
      html_renames[interface.id] = interface.id[4:]

  for subclass in _html_strip_webkit_prefix_classes:
    html_renames['WebKit' + subclass] = subclass

  # TODO(jacobr): we almost want to add this commented out line back.
  #    html_renames['HTMLCollection'] = 'ElementList'
  #    html_renames['NodeList'] = 'ElementList'
  #    html_renames['HTMLOptionsCollection'] = 'ElementList'
  html_renames['DOMWindow'] = 'Window'
  html_renames['DOMSelection'] = 'Selection'
  html_renames['DOMFormData'] = 'FormData'
  html_renames['DOMApplicationCache'] = 'ApplicationCache'
  html_renames['BarInfo'] = 'BarProp'
  html_renames['DedicatedWorkerContext']='DedicatedWorkerGlobalScope'
  html_renames['SharedWorkerContext']='SharedWorkerGlobalScope'
  html_renames['WorkerContext']='WorkerGlobalScope'

  return html_renames

def GenerateDOM(systems, generate_html_systems, output_dir,
                database_dir, use_database_cache):
  current_dir = os.path.dirname(__file__)

  generator = elementalgenerator.ElementalGenerator(
      auxiliary_dir=os.path.join(current_dir, '..', 'src'),
      template_dir=os.path.join(current_dir, '..', 'templates'),
      base_package='')
  generator.LoadAuxiliary()

  common_database = database.Database(database_dir)
  if use_database_cache:
    common_database.LoadFromCache()
  else:
    common_database.Load()

  generator.FilterMembersWithUnidentifiedTypes(common_database)
  webkit_database = common_database.Clone()

  # Generate Dart interfaces for the WebKit DOM.
  generator.FilterInterfaces(database = webkit_database,
                             or_annotations = ['WebKit', 'Dart'],
                             exclude_displaced = ['WebKit'],
                             exclude_suppressed = ['WebKit', 'Dart'])
  generator.RenameTypes(webkit_database, _webkit_renames, True)
  html_renames = _MakeHtmlRenames(common_database)
  generator.RenameTypes(webkit_database, html_renames, True)

  html_renames_inverse = dict((v,k) for k, v in html_renames.iteritems())
  webkit_renames_inverse = dict((v,k) for k, v in _webkit_renames.iteritems())

  generator.Generate(database = webkit_database,
                     output_dir = output_dir,
                     lib_dir = output_dir,
                     module_source_preference = ['WebKit', 'Dart'],
                     source_filter = ['WebKit', 'Dart'],
                     super_database = common_database,
                     common_prefix = 'common',
                     super_map = webkit_renames_inverse,
                     html_map = html_renames_inverse,
                     systems = systems)

  generator.Flush()

def main():
  parser = optparse.OptionParser()
  parser.add_option('--systems', dest='systems',
                    action='store', type='string',
                    default='gwt,gwtjso',
                    help='Systems to generate (gwt)')
  parser.add_option('--output-dir', dest='output_dir',
                    action='store', type='string',
                    default=None,
                    help='Directory to put the generated files')
  parser.add_option('--use-database-cache', dest='use_database_cache',
                    action='store_true',
                    default=False,
                    help='''Use the cached database from the previous run to
                    improve startup performance''')
  (options, args) = parser.parse_args()

  current_dir = os.path.dirname(__file__)
  systems = options.systems.split(',')
  html_system_names = ['htmlgwt']
  html_systems = [s for s in systems if s in html_system_names]
  dom_systems = [s for s in systems if s not in html_system_names]

  database_dir = os.path.join(current_dir, '..', 'database')
  use_database_cache = options.use_database_cache
  logging.config.fileConfig(os.path.join(current_dir, 'logging.conf'))

  if dom_systems:
    output_dir = options.output_dir or os.path.join(current_dir,
        '../generated')
    GenerateDOM(dom_systems, False, output_dir,
                database_dir, use_database_cache)

  if html_systems:
    output_dir = options.output_dir or os.path.join(current_dir,
        '../generated')
    GenerateDOM(html_systems, True, output_dir,
                database_dir, use_database_cache or dom_systems)

if __name__ == '__main__':
  sys.exit(main())
