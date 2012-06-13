#!/usr/bin/python
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

"""This module providesfunctionality for systems to generate
Elemental interfaces from the IDL database."""

import pdb
import os
import json
import systembaseelemental
from generator_java import *


class ElementalInterfacesSystem(systembaseelemental.SystemElemental):

  def __init__(self, templates, database, emitters, output_dir):
    super(ElementalInterfacesSystem, self).__init__(
        templates, database, emitters, output_dir)
    self._dart_interface_file_paths = []

  def InterfaceGenerator(self,
                         interface,
                         common_prefix,
                         super_interface_name,
                         source_filter):
    """."""

    module = getModule(interface.annotations)
    if super_interface_name is not None:
      interface_name = super_interface_name
    else:
      interface_name = interface.id

    dart_interface_file_path = self._FilePathForElementalInterface(module, interface_name)

    self._dart_interface_file_paths.append(dart_interface_file_path)

    dart_interface_code = self._emitters.FileEmitter(dart_interface_file_path)

    template_file = 'java_interface_%s.darttemplate' % interface_name
    template = self._templates.TryLoad(template_file)
    if not template:
      template = self._templates.Load('java_interface.darttemplate')

    return ElementalInterfaceGenerator(module, self._database,
        interface, dart_interface_code,
        template,
        common_prefix, super_interface_name,
        source_filter)


  def ProcessCallback(self, interface, info):
    """Generates a typedef for the callback interface."""
    interface_name = interface.id
    module = getModule(interface.annotations)
    file_path = self._FilePathForElementalInterface(module, interface_name)
    self._dart_callback_file_paths.append(file_path)
    code = self._emitters.FileEmitter(file_path)

    code.Emit(self._templates.Load('javacallback.darttemplate'),
              ID=interface.id,
              NAME=interface.id,
              TYPE=info.type_name,
              PARAMS=info.ParametersImplementationDeclaration(),
              PACKAGE='elemental.%s' % module,
              IMPORTS='',
              CLASSJAVADOC='')

  def GenerateLibraries(self, lib_dir):
    pass


  def _FilePathForElementalInterface(self, module, interface_name):
    """Returns the file path of the Dart interface definition."""
    return os.path.join(self._output_dir, 'src', 'elemental', module,
                        '%s.java' % interface_name)

def _escapeComments(text):
  return text.replace('*/', '*&#47;').encode('utf-8')

# ------------------------------------------------------------------------------

class ElementalInterfaceGenerator(systembaseelemental.ElementalBase):
  """Generates Elemental Interface definition for one DOM IDL interface."""


  def _GetJavaDoc(self, interface):
    docid = interface.id
    if docid not in self._docdatabase:
        docid = 'HTML%s' % interface.id

    if docid in self._docdatabase:
        if 'summary' in self._docdatabase[docid]:
          return _escapeComments(self._docdatabase[docid]['summary'])
    return ""


  def __init__(self, module, database, interface, emitter, template,
               common_prefix, super_interface, source_filter):
    """Generates Dart code for the given interface.

    Args:
      interface -- an IDLInterface instance. It is assumed that all types have
        been converted to Dart types (e.g. int, String), unless they are in the
        same package as the interface.
      common_prefix -- the prefix for the common library, if any.
      super_interface -- the name of the common interface that this interface
        implements, if any.
      source_filter -- if specified, rewrites the names of any superinterfaces
        that are not from these sources to use the common prefix.
    """
    self._module = module
    self._database = database
    self._interface = interface
    self._emitter = emitter
    self._template = template
    self._common_prefix = common_prefix
    self._super_interface = super_interface
    self._source_filter = source_filter

    current_dir = os.path.dirname(__file__)
    self._docdatabase = json.load(open(os.path.join(current_dir, '..', 'docs/database.json')))


  def addImport(self, imports, typeid):
      # skip primitive types that are all lowercase first letter
      etype = DartType(typeid)
      if '<' not in typeid and etype[0].isupper():
        rawtype = etype.split('<')[0]

        if rawtype in ['Indexable', 'Settable', 'Mappable']:
          pmodule = 'util'
          imports['import elemental.%s.%s;\n' % (pmodule, rawtype)]=1
        elif etype not in java_lang and self._database.HasInterface(typeid):
          pinterface = self._database.GetInterface(typeid)
          pmodule = getModule(pinterface.annotations)
          if pmodule != self._module:
            imports['import elemental.%s.%s;\n' % (pmodule, rawtype)]=1

  def StartInterface(self):
    if self._super_interface:
      typename = self._super_interface
    else:
      typename = self._interface.id


    extends = []
    suppressed_extends = []
    imports = {}
    implements_raw = {}

    for attr in self._interface.attributes:
      self.addImport(imports, attr.type.id)

    for oper in self._interface.operations:
      self.addImport(imports, oper.type.id)
      for arg in oper.arguments:
        self.addImport(imports, arg.type.id)


    alreadyImplemented = {}        
    if len(self._interface.parents) > 0:
      self.getImplements(alreadyImplemented, self._interface.parents[0])

    for parent in self._interface.parents:
      if not parent.type.id in alreadyImplemented or parent.type.id == 'ElementalMixinBase':
        self.addImport(imports, parent.type.id)
      # TODO(vsm): Remove source_filter.
        rawtype = DartType(parent.type.id).split('<')[0]
        if MatchSourceFilter(self._source_filter, parent) and DartType(parent.type.id) != 'Object' and rawtype not in implements_raw:
          # Parent is a DOM type.
          extends.append(DartType(parent.type.id))
          implements_raw[rawtype]=1

#TODO(cromwellian) add in Indexable/IndexableInt/IndexableNumber
#      elif '<' in parent.type.id:
        # Parent is a Dart collection type.
        # TODO(vsm): Make this check more robust.
#        extends.append(parent.type.id)
#      else:
#        suppressed_extends.append('%s.%s' %
#                                  (self._common_prefix, parent.type.id))

    if self._interface.parents:
      rawtype = DartType(self._interface.parents[0].type.id).split('<')[0]
      if rawtype not in implements_raw:
        extends.insert(0, DartType(self._interface.parents[0].type.id))
        self.addImport(imports, self._interface.parents[0].type.id)          


    comment = ' extends'
    extends_str = ''
    if extends:
      extends_str += ' extends ' + ', '.join(extends)
      comment = ','
    if suppressed_extends:
      extends_str += ' /*%s %s */' % (comment, ', '.join(suppressed_extends))

    factory_provider = None
    constructor_info = AnalyzeConstructor(self._interface)

    # TODO(vsm): Add appropriate package / namespace syntax.
    (self._members_emitter,
     self._top_level_emitter) = self._emitter.Emit(
         self._template + '$!TOP_LEVEL',
         ID=typename,
         EXTENDS=extends_str, PACKAGE='elemental.' + self._module,
         CLASSJAVADOC = self._GetJavaDoc(self._interface),
         IMPORTS = ''.join(imports.keys()))

# TODO(cromwellian) auto-generate factory classes?

#    if constructor_info:
#      self._members_emitter.Emit(
#          '\n'
#          '  $CTOR($PARAMS);\n',
#          CTOR=typename,
#          PARAMS=constructor_info.ParametersInterfaceDeclaration());

#    element_type = MaybeTypedArrayElementType(self._interface)
#    if element_type:
#      self._members_emitter.Emit(
#          '\n'
#          '  $CTOR(int length);\n'
#          '\n'
#          '  $CTOR.fromList(List<$TYPE> list);\n'
#          '\n'
#          '  $CTOR.fromBuffer(ArrayBuffer buffer,'
#                            ' [int byteOffset, int length]);\n',
#          CTOR=self._interface.id,
#          TYPE=DartType(element_type))


  def FinishInterface(self):
    # TODO(vsm): Use typedef if / when that is supported in Dart.
    # Define variant as subtype.
    if (self._super_interface and
        self._interface.id is not self._super_interface):
      consts_emitter = self._top_level_emitter.Emit(
          '\n'
          'interface $NAME extends $BASE {\n'
          '$!CONSTS'
          '}\n',
          NAME=self._interface.id,
          BASE=self._super_interface)
      for const in sorted(self._interface.constants, ConstantOutputOrder):
        self._EmitConstant(consts_emitter, const)
    if self._interface.id == 'Document':
     for iface in self._database.GetInterfaces():
       if iface.id.endswith("Element"):
         if iface.id == 'Element' or iface.id == 'SVGElement':
           continue
         callName = DartType(iface.id)
         if iface.id == 'SVGSVGElement':
           callName = 'SVGElement'
         self._members_emitter.Emit('\n  $TYPE create$CALL();\n',
                                    CALL = callName,
                                    TYPE=DartType(iface.id))

    if self._interface.id == 'Window':
     for iface in self._database.GetInterfaces():
       element_type = MaybeTypedArrayElementType(iface)
       if element_type:
         self._members_emitter.Emit(
           '\n'
           '  $TYPE new$CTOR(int length);\n'
           '\n'
           '  $TYPE new$CTOR(IndexableNumber list);\n'
           '\n'
           '  $TYPE new$CTOR(ArrayBuffer buffer,'
           ' int byteOffset, int length);\n',
           CTOR=iface.id,
           TYPE=iface.id)
       constructor_info = AnalyzeConstructor(iface)
       if constructor_info:
         self._members_emitter.Emit(
           '\n'
           '  $TYPE new$CTOR($PARAMS);\n',
           TYPE=iface.id,
           CTOR=iface.id,
           PARAMS=constructor_info.ParametersInterfaceDeclaration());
           
  def AddConstant(self, constant):
    if (not self._super_interface or
        self._interface.id is self._super_interface):
      self._EmitConstant(self._members_emitter, constant)

  def _EmitConstant(self, emitter, constant):
    javadoc = self.find_doc(constant.id)
    javadoctemplate = '\n  /**\n    * $JAVADOC\n    */\n';
    if javadoc == '':
      javadoctemplate = ''

    emitter.Emit('%s\n    static final $TYPE$NAME = $VALUE;\n' % javadoctemplate,
                 NAME=constant.id,
                 TYPE=TypeOrNothing(DartType(constant.type.id),
                                    None),
                 VALUE=constant.value,
                 JAVADOC = javadoc)


  def AddAttribute(self, getter, setter, inheritedGetter, inheritedSetter):
    javadoc = self.find_doc(getter.id)
    javadoctemplate = '\n  /**\n    * $JAVADOC\n    */\n';
    if javadoc == '':
      javadoctemplate = ''

    if getter:
      self._members_emitter.Emit('\n%s  $TYPE $NAME();\n' % javadoctemplate,
                                 NAME=getterName(getter),
                                 TYPE=TypeOrVar(DartType(getter.type.id),
                                                getter.type.id),
                                 JAVADOC=javadoc)

    if setter:
      self._members_emitter.Emit('\n  void $NAME($TYPE arg);\n',
                                 NAME=setterName(setter),
                                 TYPE=TypeOrVar(DartType(setter.type.id),
                                                setter.type.id),
                                 ARG=setter.type.id)
      return

  def AddIndexer(self, element_type):
    # Interface inherits all operations from List<element_type>.
    pass

  def AddOperation(self, info, inherited):
    """
    Arguments:
      operations - contains the overloads, one or more operations with the same
        name.
    """
    javadoc = self.find_doc(info.name)
    javadoctemplate = '\n  /**\n    * $JAVADOC\n    */\n';
    
    if javadoc == '':
      javadoctemplate = ''
    get_attrs = []
    set_attrs = []
    for attr in self._interface.attributes:
      get_attrs.append(getterName(attr))
      set_attrs.append(setterName(attr))

    if info.name in get_attrs or info.name in set_attrs:
      return

    return_type = info.type_name
    if info.name == 'addEventListener':
      return_type = 'EventRemover'

    self._members_emitter.Emit('\n%s  $TYPE $NAME($PARAMS);\n' % javadoctemplate,
                               TYPE=return_type,
                               NAME=self.fixReservedKeyWords(info.name),
                               PARAMS=info.ParametersInterfaceDeclaration(),
                               JAVADOC=javadoc)

  def AddStaticOperation(self, info, inherited):
    pass

  # Interfaces get secondary members directly via the superinterfaces.
  def AddSecondaryAttribute(self, interface, getter, setter):
    pass

  def AddSecondaryOperation(self, interface, attr):
    pass

  def find_doc(self, name):
    docid = self._interface.id
    if docid not in self._docdatabase:
        docid = 'HTML%s' % self._interface.id

    docmembers = []
    docmember = {}
    if docid in self._docdatabase and 'members' in self._docdatabase[docid]:
        docmembers  = self._docdatabase[docid]['members']
    for member in docmembers:
      if member['name'] == name:
        docmember = member
    if 'help' in docmember:
      return _escapeComments(docmember['help'])
    return ""

def ucfirst(string):
  """Upper cases the 1st character in a string"""
  if len(string):
    return '%s%s' % (string[0].upper(), string[1:])
  return string

def getterName(attr):
  name = DartDomNameOfAttribute(attr)
  if attr.type.id == 'boolean':
    if name.startswith('is'):
      name = name[2:]
    return 'is%s' % ucfirst(name)
  return 'get%s' % ucfirst(name)

def setterName(attr):
  name = DartDomNameOfAttribute(attr)
  return 'set%s' % ucfirst(name)

def getModule(annotations):
  htmlaliases = ['audio', 'webaudio', 'inspector', 'offline', 'p2p', 'window', 'websockets', 'threads', 'view', 'storage', 'fileapi']

  module = "dom"
  if 'WebKit' in annotations and 'module' in annotations['WebKit']:
    module = annotations['WebKit']['module']
  if module in  htmlaliases:
    return "html"
  if module == 'core':
    return 'dom'
  return module

java_lang = ['Object', 'String', 'Exception', 'DOMTimeStamp', 'DOMString']

