#!/usr/bin/python
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

"""This module generates Elemental APIs from the IDL database."""

import emitter
import idlnode
import logging
import multiemitter
import os
import re
import shutil
from generator_java import *
from systembaseelemental import *
from systemgwt import *
from systemgwtjso import *
from templateloader import TemplateLoader

_logger = logging.getLogger('elementalgenerator')

def MergeNodes(node, other):
  node.operations.extend(other.operations)
  for attribute in other.attributes:
    if not node.has_attribute(attribute):
      node.attributes.append(attribute)

  node.constants.extend(other.constants)

class ElementalGenerator(object):
  """Utilities to generate Elemental APIs and corresponding JavaScript."""

  def __init__(self, auxiliary_dir, template_dir, base_package):
    """Constructor for the DartGenerator.

    Args:
      auxiliary_dir -- location of auxiliary handwritten classes
      template_dir -- location of template files
      base_package -- the base package name for the generated code.
    """
    self._auxiliary_dir = auxiliary_dir
    self._template_dir = template_dir
    self._base_package = base_package
    self._auxiliary_files = {}
    self._dart_templates_re = re.compile(r'[\w.:]+<([\w\.<>:]+)>')

    self._emitters = None  # set later


  def _StripModules(self, type_name):
    return type_name.split('::')[-1]

  def _IsCompoundType(self, database, type_name):
    if IsPrimitiveType(type_name):
      return True

    striped_type_name = self._StripModules(type_name)
    if database.HasInterface(striped_type_name):
      return True

    dart_template_match = self._dart_templates_re.match(type_name)
    if dart_template_match:
      # Dart templates
      parent_type_name = type_name[0 : dart_template_match.start(1) - 1]
      sub_type_name = dart_template_match.group(1)
      return (self._IsCompoundType(database, parent_type_name) and
              self._IsCompoundType(database, sub_type_name))
    return False

  def _IsDartType(self, type_name):
    return '.' in type_name

  def LoadAuxiliary(self):
    def Visitor(_, dirname, names):
      for name in names:
        if name.endswith('.dart'):
          name = name[0:-5]  # strip off ".dart"
        self._auxiliary_files[name] = os.path.join(dirname, name)
    os.path.walk(self._auxiliary_dir, Visitor, None)

  def RenameTypes(self, database, conversion_table, rename_javascript_binding_names):
    """Renames interfaces using the given conversion table.

    References through all interfaces will be renamed as well.

    Args:
      database: the database to apply the renames to.
      conversion_table: maps old names to new names.
    """

    if conversion_table is None:
      conversion_table = {}
     
    # Rename interfaces:
    for old_name, new_name in conversion_table.items():
      if database.HasInterface(old_name):
        _logger.info('renaming interface %s to %s' % (old_name, new_name))
        interface = database.GetInterface(old_name)
        database.DeleteInterface(old_name)
        if not database.HasInterface(new_name):
          interface.id = new_name
          database.AddInterface(interface)
        else:
          new_interface = database.GetInterface(new_name)
          MergeNodes(new_interface, interface)
        
        if rename_javascript_binding_names:
          interface.javascript_binding_name = new_name
          interface.doc_js_name = new_name
          for member in (interface.operations + interface.constants
              + interface.attributes):
            member.doc_js_interface_name = new_name

 
    # Fix references:
    for interface in database.GetInterfaces():
      for idl_type in interface.all(idlnode.IDLType):
        type_name = self._StripModules(idl_type.id)
        if type_name in conversion_table:
          idl_type.id = conversion_table[type_name]

  def FilterMembersWithUnidentifiedTypes(self, database):
    """Removes unidentified types.

    Removes constants, attributes, operations and parents with unidentified
    types.
    """

    for interface in database.GetInterfaces():
      def IsIdentified(idl_node):
        node_name = idl_node.id if idl_node.id else 'parent'
        for idl_type in idl_node.all(idlnode.IDLType):
          type_name = idl_type.id
          if (type_name is not None and
              self._IsCompoundType(database, type_name)):
            continue
          _logger.warn('removing %s in %s which has unidentified type %s' %
                       (node_name, interface.id, type_name))
          return False
        return True

      interface.constants = filter(IsIdentified, interface.constants)
      interface.attributes = filter(IsIdentified, interface.attributes)
      interface.operations = filter(IsIdentified, interface.operations)
      interface.parents = filter(IsIdentified, interface.parents)

  def FilterInterfaces(self, database,
                       and_annotations=[],
                       or_annotations=[],
                       exclude_displaced=[],
                       exclude_suppressed=[]):
    """Filters a database to remove interfaces and members that are missing
    annotations.

    The FremontCut IDLs use annotations to specify implementation
    status in various platforms. For example, if a member is annotated
    with @WebKit, this means that the member is supported by WebKit.

    Args:
      database -- the database to filter
      all_annotations -- a list of annotation names a member has to
        have or it will be filtered.
      or_annotations -- if a member has one of these annotations, it
        won't be filtered even if it is missing some of the
        all_annotations.
      exclude_displaced -- if a member has this annotation and it
        is marked as displaced it will always be filtered.
      exclude_suppressed -- if a member has this annotation and it
        is marked as suppressed it will always be filtered.
    """

    # Filter interfaces and members whose annotations don't match.
    for interface in database.GetInterfaces():
      def HasAnnotations(idl_node):
        """Utility for determining if an IDLNode has all
        the required annotations"""
        for a in exclude_displaced:
          if (a in idl_node.annotations
              and 'via' in idl_node.annotations[a]):
            return False
        for a in exclude_suppressed:
          if (a in idl_node.annotations
              and 'suppressed' in idl_node.annotations[a]):
            return False
        for a in or_annotations:
          if a in idl_node.annotations:
            return True
        if and_annotations == []:
          return False
        for a in and_annotations:
          if a not in idl_node.annotations:
            return False
        return True

      if HasAnnotations(interface):
        interface.constants = filter(HasAnnotations, interface.constants)
        interface.attributes = filter(HasAnnotations, interface.attributes)
        interface.operations = filter(HasAnnotations, interface.operations)
        interface.parents = filter(HasAnnotations, interface.parents)
      else:
        database.DeleteInterface(interface.id)

    self.FilterMembersWithUnidentifiedTypes(database)


  def Generate(self, database, output_dir,
               module_source_preference=[], source_filter=None,
               super_database=None, common_prefix=None, super_map={},
               html_map={}, lib_dir=None, systems=[]):
    """Generates Dart and JS files for the loaded interfaces.

    Args:
      database -- database containing interfaces to generate code for.
      output_dir -- directory to write generated files to.
      module_source_preference -- priority order list of source annotations to
        use when choosing a module name, if none specified uses the module name
        from the database.
      source_filter -- if specified, only outputs interfaces that have one of
        these source annotation and rewrites the names of superclasses not
        marked with this source to use the common prefix.
      super_database -- database containing super interfaces that the generated
        interfaces should extend.
      common_prefix -- prefix for the common library, if any.
      lib_file_path -- filename for generated .lib file, None if not required.
      lib_template -- template file in this directory for generated lib file.
    """
  
    self._emitters = multiemitter.MultiEmitter()
    self._database = database
    self._output_dir = output_dir

    self._FixEventTargets()
    self._ComputeInheritanceClosure()

    self._systems = []

    # TODO(jmesserly): only create these if needed
    if ('gwtjso' in systems):
      jso_system = ElementalJsoSystem(
          TemplateLoader(self._template_dir, ['dom/jso', 'dom', '']),
          self._database, self._emitters, self._output_dir)
      self._systems.append(jso_system)
    if ('gwt' in systems):
      interface_system = ElementalInterfacesSystem(
          TemplateLoader(self._template_dir, ['dom/interface', 'dom', '']),
          self._database, self._emitters, self._output_dir)
      self._systems.append(interface_system)

#    if 'gwt' in systems:
#      elemental_system = ElementalSystem(
#          TemplateLoader(self._template_dir, ['dom/elemental', 'dom', '']),
#          self._database, self._emitters, self._output_dir)

#      elemental_system._interface_system = interface_system
#      self._systems.append(elemental_system)



    # Collect interfaces
    interfaces = []
    for interface in database.GetInterfaces():
      if not MatchSourceFilter(source_filter, interface):
        # Skip this interface since it's not present in the required source
        _logger.info('Omitting interface - %s' % interface.id)
        continue
      interfaces.append(interface)

    # TODO(sra): Use this list of exception names to generate information to
    # tell Frog which exceptions can be passed from JS to Dart code.
    exceptions = self._CollectExceptions(interfaces)

    mixins = self._ComputeMixins(self._PreOrderInterfaces(interfaces))
    for system in self._systems:
      # give outputters a chance to see the mixin list before starting
      system.ProcessMixins(mixins)

    # copy all mixin methods from every interface to this base interface
    self.PopulateMixinBase(self._database.GetInterface('ElementalMixinBase'), mixins)

    # Render all interfaces into Dart and save them in files.
    for interface in self._PreOrderInterfaces(interfaces):

      super_interface = None
      super_name = interface.id

      if super_name in super_map:
        super_name = super_map[super_name]

      if (super_database is not None and
          super_database.HasInterface(super_name)):
        super_interface = super_name

      interface_name = interface.id
      auxiliary_file = self._auxiliary_files.get(interface_name)
      if auxiliary_file is not None:
        _logger.info('Skipping %s because %s exists' % (
            interface_name, auxiliary_file))
        continue
      
      info = RecognizeCallback(interface)
      if info:
        for system in self._systems:
          system.ProcessCallback(interface, info)
      else:
        if 'Callback' in interface.ext_attrs:
          _logger.info('Malformed callback: %s' % interface.id)
        self._ProcessInterface(interface, super_interface,
                               source_filter, common_prefix)

    for system in self._systems:
      system.Finish()

  def PopulateMixinBase(self, mixinbase, mixins):
    """Copy all mixin attributes and operations to mixin base class"""
    for mixin_name in mixins:
      if self._database.HasInterface(mixin_name):
        mixin = self._database.GetInterface(mixin_name)
        mixinbase.attributes.extend(mixin.attributes)
        mixinbase.operations.extend(mixin.operations)
        for extattr in mixin.ext_attrs.keys():
          mixinbase.ext_attrs[extattr] = mixin.ext_attrs[extattr]

  # compute all interfaces which are in disjoint type hierarchies
  # that is, cannot be SingleImplJSO without hoisting      
  def _ComputeMixins(self, interfaces):
    implementors = {}
    mixins = {}
    parents = {}
    # first compute the set of all inherited super-interfaces of every interface
    for interface in interfaces:
      if interface.parents:
        # the first parent interface is the superclass
        parent = interface.parents[0]
        # if we haven't processed this one before
        if not interface.id in parents:
          # compute a list of all of the direct superclass this interface
          parents[interface.id] = []
          parents[interface.id].append(parent)
          if parent.type.id in parents:
            # inherit all the super-interfaces
            parents[interface.id].extend(parents[parent.type.id])

    implemented_by = None
    for interface in interfaces:
      # now, examining secondary interfaces 
      for secondary in interface.parents[1:]:
        if secondary.type.id in implementors:
          implemented_by = implementors[secondary.type.id]
          # if the interface is implemented by someone else who is not one of my parents, it is not SingleJsoImpl
          if not implemented_by in parents[interface.id]:
            mixins[secondary.type.id]=implemented_by
            print "Mixin detected %s, previously implemented by %s, but also implemented by %s" % (secondary.type.id, implemented_by, interface.id)
            # add all parents of the mixin as well
            superparents = []
            superiface = secondary.type.id
            if self._database.HasInterface(superiface):
              self.getParents(self._database.GetInterface(superiface), superparents)
            for parent in superparents:
              mixins[parent.id]=implemented_by
              print "Super Mixin detected %s, previously implemented by %s, but also implemented by %s" % (parent.id, implemented_by, interface.id)

        else:
          implementors[secondary.type.id] = interface.id
    # manual patch for outliers not picked up by this logic
    mixins['ElementTimeControl']=1         
    mixins['ElementTraversal']=1         
    return mixins.keys()

  def _PreOrderInterfaces(self, interfaces):
    """Returns the interfaces in pre-order, i.e. parents first."""
    seen = set()
    ordered = []
    def visit(interface):
      if interface.id in seen:
        return
      seen.add(interface.id)
      for parent in interface.parents:
        if IsDartCollectionType(parent.type.id):
          continue
        if self._database.HasInterface(parent.type.id):
          parent_interface = self._database.GetInterface(parent.type.id)
          visit(parent_interface)
      ordered.append(interface)

    for interface in interfaces:
      visit(interface)
    return ordered


  def _ProcessInterface(self, interface, super_interface_name,
                        source_filter,
                        common_prefix):
    """."""
    
    _logger.info('Generating %s' % interface.id)

    generators = [system.InterfaceGenerator(interface,
                                            common_prefix,
                                            super_interface_name,
                                            source_filter)
                  for system in self._systems]
    generators = filter(None, generators)


    mixinbase = self._database.GetInterface("ElementalMixinBase")
    parentops = []
    parentattrs = []


    directParents = []
    mixinOps = []
    # compute the immediate parents of each interface (not including secondary interfaces)
    self.getParents(interface, directParents)

    # if not the mixin base, add its parents
    if interface.id != 'ElementalMixinBase':
      self.getParents(mixinbase, directParents)
      # add the mixin base class itself as the parent of everything                                                                                                           
      directParents.insert(0, mixinbase)

    # for each parent interface
    for pint in directParents:
      for op in pint.operations:
        # compute unique method signatures for each operation                                                                                                           :
        op_name = op.ext_attrs.get('DartName', op.id)
        sig = "%s %s(" % (op.type.id, op_name)
        for arg in op.arguments:
          sig += arg.type.id
          parentops.append(sig)
      for attr in pint.attributes:
        # compute attributes                                                                                                             
        if attr.is_fc_getter:
          parentattrs.append("getter_" + DartDomNameOfAttribute(attr))
        if attr.is_fc_setter:
          parentattrs.append("setter_" + DartDomNameOfAttribute(attr))

    for generator in generators:
      generator.StartInterface()

    for const in sorted(interface.constants, ConstantOutputOrder):
      for generator in generators:
        generator.AddConstant(const)

    attributes = [attr for attr in interface.attributes]

    for (getter, setter) in  _PairUpAttributes(attributes):
      for generator in generators:
        # detect if attribute is inherited (as opposed to just redeclared)                                                                                                             
        inheritedGetter = ("getter_" + DartDomNameOfAttribute(getter)) in parentattrs
        inheritedSetter = setter and ("setter_" + DartDomNameOfAttribute(setter)) in parentattrs
        generator.AddAttribute(getter, setter, inheritedGetter, inheritedSetter)

    # The implementation should define an indexer if the interface directly
    # extends List.
    element_type = MaybeListElementType(interface)
    if element_type:
      for generator in generators:
        generator.AddIndexer(element_type)

    # Generate operations
    alreadyGenerated = []
    for operation in interface.operations:
      op_name = operation.ext_attrs.get('DartName', operation.id)
      sig = "%s %s(" % (operation.type.id, op_name)
      for arg in operation.arguments:
        sig += arg.type.id
      if sig in alreadyGenerated:
        continue
      alreadyGenerated.append(sig)

      # hacks, should be able to compute this from IDL database
      if operation.id == 'toString':
        # implemented on JSO.toString()
        continue
      operations = []
      operations.append(operation)
      info = AnalyzeOperation(interface, operations)
      for generator in generators:
        # don't override stuff hoisted to mixin base in implementors
        inherited = sig in parentops
        if info.IsStatic():
          generator.AddStaticOperation(info, inherited)
        else:
          generator.AddOperation(info, inherited)

    # With multiple inheritance, attributes and operations of non-first
    # interfaces need to be added.  Sometimes the attribute or operation is
    # defined in the current interface as well as a parent.  In that case we
    # avoid making a duplicate definition and pray that the signatures match.

    for parent_interface in self._TransitiveSecondaryParents(interface):
      if isinstance(parent_interface, str):  # IsDartCollectionType(parent_interface)
        continue
      attributes = [attr for attr in parent_interface.attributes
                    if not FindMatchingAttribute(interface, attr)]
      for (getter, setter) in _PairUpAttributes(attributes):
        for generator in generators:
          generator.AddSecondaryAttribute(parent_interface, getter, setter)

      # Group overloaded operations by id
      operationsById = {}
      for operation in parent_interface.operations:
        if operation.id not in operationsById:
          operationsById[operation.id] = []
        operationsById[operation.id].append(operation)

      # Generate operations
      for id in sorted(operationsById.keys()):
        if not any(op.id == id for op in interface.operations):
          operations = operationsById[id]
          info = AnalyzeOperation(interface, operations)
          for generator in generators:
            generator.AddSecondaryOperation(parent_interface, info)

    for generator in generators:
      generator.FinishInterface()
    return

  def getParents(self, interface, results):
    if interface.parents:
      pid = interface.parents[0].type.id
      if self._database.HasInterface(pid):
        pint = self._database.GetInterface(interface.parents[0].type.id)
        results.append(pint)
        self.getParents(pint, results)
    
  def _TransitiveSecondaryParents(self, interface):
    """Returns a list of all non-primary parents.

    The list contains the interface objects for interfaces defined in the
    database, and the name for undefined interfaces.
    """
    def walk(parents):
      for parent in parents:
        if IsDartCollectionType(parent.type.id):
          result.append(parent.type.id)
          continue
        if self._database.HasInterface(parent.type.id):
          parent_interface = self._database.GetInterface(parent.type.id)
          result.append(parent_interface)
          walk(parent_interface.parents)

    result = []
    walk(interface.parents[1:])
    return result;


  def _CollectExceptions(self, interfaces):
    """Returns the names of all exception classes raised."""
    exceptions = set()
    for interface in interfaces:
      for attribute in interface.attributes:
        if attribute.get_raises:
          exceptions.add(attribute.get_raises.id)
        if attribute.set_raises:
          exceptions.add(attribute.set_raises.id)
      for operation in interface.operations:
        if operation.raises:
          exceptions.add(operation.raises.id)
    return exceptions


  def Flush(self):
    """Write out all pending files."""
    _logger.info('Flush...')
    self._emitters.Flush()

  def _FixEventTargets(self):
    for interface in self._database.GetInterfaces():
      # Create fake EventTarget parent interface for interfaces that have
      # 'EventTarget' extended attribute.
      if 'EventTarget' in interface.ext_attrs:
        ast = [('Annotation', [('Id', 'WebKit')]),
               ('InterfaceType', ('ScopedName', 'EventTarget'))]
        interface.parents.append(idlnode.IDLParentInterface(ast))

  def _ComputeInheritanceClosure(self):
    def Collect(interface, seen, collected):
      name = interface.id
      if '<' in name:
        # TODO(sra): Handle parameterized types.
        return
      if not name in seen:
        seen.add(name)
        collected.append(name)
        for parent in interface.parents:
          # TODO(sra): Handle parameterized types.
          if not '<' in parent.type.id:
            if self._database.HasInterface(parent.type.id):
              Collect(self._database.GetInterface(parent.type.id),
                      seen, collected)

    self._inheritance_closure = {}
    for interface in self._database.GetInterfaces():
      seen = set()
      collected = []
      Collect(interface, seen, collected)
      self._inheritance_closure[interface.id] = collected

  def _AllImplementedInterfaces(self, interface):
    """Returns a list of the names of all interfaces implemented by 'interface'.
    List includes the name of 'interface'.
    """
    return self._inheritance_closure[interface.id]

def _PairUpAttributes(attributes):
  """Returns a list of (getter, setter) pairs sorted by name.

  One element of the pair may be None.
  """
  names = sorted(set(attr.id for attr in attributes))
  getters = {}
  setters = {}
  for attr in attributes:
    if attr.is_fc_getter:
      getters[attr.id] = attr
    elif attr.is_fc_setter and 'Replaceable' not in attr.ext_attrs:
      setters[attr.id] = attr
  return [(getters.get(id), setters.get(id)) for id in names]

# ------------------------------------------------------------------------------

class DummyImplementationSystem(SystemElemental):
  """Generates a dummy implementation for use by the editor analysis.

  All the code comes from hand-written library files.
  """

  def __init__(self, templates, database, emitters, output_dir):
    super(DummyImplementationSystem, self).__init__(
        templates, database, emitters, output_dir)
    factory_providers_file = os.path.join(self._output_dir, 'src', 'dummy',
                                          'RegularFactoryProviders.dart')
    self._factory_providers_emitter = self._emitters.FileEmitter(
        factory_providers_file)
    self._impl_file_paths = [factory_providers_file]

  def InterfaceGenerator(self,
                         interface,
                         common_prefix,
                         super_interface_name,
                         source_filter):
    return DummyInterfaceGenerator(self, interface)

  def ProcessCallback(self, interface, info):
    pass

  def GenerateLibraries(self, lib_dir):
    # Library generated for implementation.
    self._GenerateLibFile(
        'dom_dummy.darttemplate',
        os.path.join(lib_dir, 'dom_dummy.dart'),
        (self._interface_system._dart_interface_file_paths +
         self._interface_system._dart_callback_file_paths +
         self._impl_file_paths))


# ------------------------------------------------------------------------------

class DummyInterfaceGenerator(object):
  """Generates dummy implementation."""

  def __init__(self, system, interface):
    self._system = system
    self._interface = interface

  def StartInterface(self):
    # There is no implementation to match the interface, but there might be a
    # factory constructor for the Dart interface.
    constructor_info = AnalyzeConstructor(self._interface)
    if constructor_info:
      dart_interface_name = self._interface.id
      self._EmitFactoryProvider(dart_interface_name, constructor_info)

  def _EmitFactoryProvider(self, interface_name, constructor_info):
    factory_provider = '_' + interface_name + 'FactoryProvider'
    self._system._factory_providers_emitter.Emit(
        self._system._templates.Load('factoryprovider.darttemplate'),
        FACTORYPROVIDER=factory_provider,
        CONSTRUCTOR=interface_name,
        PARAMETERS=constructor_info.ParametersImplementationDeclaration())

  def FinishInterface(self):
    pass

  def AddConstant(self, constant):
    pass

  def AddAttribute(self, getter, setter, inheritedGetter, inheritedSetter):
    pass

  def AddSecondaryAttribute(self, interface, getter, setter):
    pass

  def AddSecondaryOperation(self, interface, info):
    pass

  def AddIndexer(self, element_type):
    pass

  def AddTypedArrayConstructors(self, element_type):
    pass

  def AddOperation(self, info, inherited):
    pass

  def AddStaticOperation(self, info, inherited):
    pass

  def AddEventAttributes(self, event_attrs):
    pass
