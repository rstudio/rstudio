from element_parsers import Command, Menu, Shortcut, Text
from i18n_gwt_interface import I18NGwtConstantsInterfaceGenerator, I18NGwtPropertiesGenerator
from lxml.etree import ElementTree


DEFAULT_PARSERS = {
    'cmd': Command,
    'menu': Menu,
    'shortcut': Shortcut,
}


def xml_to_interface(interface_name, root_element: ElementTree, element_type, filename, element_parser=None, prefix: str = ""):
    if element_parser is None:
        element_parser = DEFAULT_PARSERS[element_type]
    interface = I18NGwtConstantsInterfaceGenerator(interface_name)
    interface.add_header_auto_generated(filename)

    # Get cmd's that are direct children of root element
    for element_xml in element_parser.grep_from_element_tree(root_element):
        parsed = element_parser(element_xml)
        for constant in parsed.to_constants(prefix=prefix):
            interface.add_constant(constant)
        interface.add_constant(Text(""))  # Newline to separate groups
    return interface


def xml_to_properties(root_element, element_type, element_parser=None, prefix: str = ""):
    # TODO: Add descriptive header
    if element_parser is None:
        element_parser = DEFAULT_PARSERS[element_type]
    properties = I18NGwtPropertiesGenerator()
    properties.add_header_auto_generated()

    # Get cmd's that are direct children of root element
    for element_xml in element_parser.grep_from_element_tree(root_element):
        parsed = element_parser(element_xml)
        for prop in parsed.to_properties(prefix=prefix):
            properties.add_property(prop)
        properties.add_property(Text(""))  # Newline to separate groups

    return properties
