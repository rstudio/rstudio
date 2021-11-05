from i18n_gwt_interface import I18NGwtConstantsInterfaceGenerator, \
    I18NGwtPropertiesGenerator, Constant, Property, Text
from lxml.etree import ElementTree
import re


class ElementParser:
    tag = None

    def __init__(self, xml_element: ElementTree):
        self.non_translatable_fields = []
        self.translatable_fields = []
        self._xml_element = xml_element
        self._lineage = None
        self.tag = None

    def parse(self):
        self.tag = self._xml_element.get("tag", None)
        for field_name in self.non_translatable_fields + self.translatable_fields:
            setattr(self, field_name, self._xml_element.get(field_name, None))

    @property
    def lineage(self):
        if self._lineage is None:
            self._lineage = get_lineage(self._xml_element)
        return self._lineage

    def _set_lineage_from_xml(self):
        get_lineage(self._xml_element)

    @property
    def populated_translatable_fields(self):
        for field_name in self.translatable_fields:
            if getattr(self, field_name) is not None:
                yield field_name

    def _ensure_translatable_fields_exist(self):
        for field_name in self.translatable_fields:
            if not hasattr(self, field_name):
                raise ValueError(f"Object missing attribute '{field_name}' that is called as required translatable "
                                 f"field.  Did you add a field to self.translatable_fields without adding the "
                                 f"attribute?")

    def to_constants(self, prefix: str = "", include_header=True):
        """
        Returns this Element defined by Java constant statements

        For example:
            [
                "@DefaultStringValue("Some default label")",
                "String someCommandLabel();",
            ]

        :param prefix: (optional) Text prefix to add to the default value on all translatable fields.  Useful for
                       highlighting default values against their true English text (to see if English text is picked up)
        :param include_header: If true, output lines will include a header line stating this element's name
        :return: List of string Java statements for the translatable constants defining this element
        """
        constants = []
        if include_header:
            constants.append(Text(f"// {self.name}"))
        for field_name in self.populated_translatable_fields:
            constant_name = self._field_name_parser(field_name)
            default_value = self._default_value_parser(getattr(self, field_name))
            constants.append(Constant(constant_name, prefix + default_value))
        return constants

    def to_properties(self, prefix: str = "", include_header: bool = True):
        properties = []
        if include_header:
            properties.append(Text(f"# {self.name}"))
        for field_name in self.populated_translatable_fields:
            full_field_name = self._field_name_parser( field_name)
            properties.append(Property(name=full_field_name, value=prefix + getattr(self, field_name)))
        return properties

    @classmethod
    def grep_from_element_tree(cls, element: ElementTree):
        """
        Returns a collection of elements of this type from an ElementTree

        Default grepper behaviour is to return all elements of tag that are
        direct children of the parsed element (does not return nested elements.
        Nested elements need to be found using element.iter())
        """
        return element.findall(cls.tag)

    @property
    def name(self):
        raise NotImplementedError()

    def _default_value_parser(self, default_value):
        return default_value

    def _field_name_parser(self, field_name):
        return field_name


COMMAND_TRANSLATABLE_FIELDS = ["label", "buttonLabel", "menuLabel", "desc"]
COMMAND_NON_TRANSLATABLE_FIELDS = ["id"]


class Command(ElementParser):
    tag = "cmd"

    def __init__(self, xml_element: ElementTree):
        """
        Parsing structure to convert a Command in Commands.cmd.xml format to translatable Java constant statements

        :param xml_element: XML ElementTree describing a Command
        """
        super().__init__(xml_element)
        self.non_translatable_fields = COMMAND_NON_TRANSLATABLE_FIELDS
        self.id = None

        # Fields that could be translated
        self.translatable_fields = COMMAND_TRANSLATABLE_FIELDS
        self.label = None
        self.buttonLabel = None
        self.menuLabel = None
        self.desc = None

        # Ensure these match the fields in TRANSLATABLE_FIELDS
        # (these could instead be defined dynamically from TRANSLATABLE_FIELDS,
        # but that feels more opaque...)
        self._ensure_translatable_fields_exist()

        self.parse()

    @property
    def name(self):
        return self.id

    def _field_name_parser(self, field_name):
        return generate_name(self.name, field_name)


MENU_TRANSLATABLE_FIELDS = ["label"]


class Menu(ElementParser):
    tag = "menu"

    def __init__(self, xml_element):
        super().__init__(xml_element)

        # Fields that could be translated
        self.translatable_fields = MENU_TRANSLATABLE_FIELDS
        self.label = None
 
        self._ensure_translatable_fields_exist()

        self.parse()

    @property
    def lineage(self, path_delimiter: str = '/'):
        """Custom lineage for menu elements which roots at 'main'"""
        if self._lineage is None:
            self._lineage = f"main{path_delimiter}" + get_lineage(self._xml_element)

            # Remove trailing delimiters (can happen if this is straight off 'main'
            self._lineage = self._lineage.rstrip(path_delimiter)
        return self._lineage

    @classmethod
    def grep_from_element_tree(cls, element: ElementTree):
        """
        Returns a collection of elements that apply to this type from an ElementTree

        Returned are all "menu" elements in the top-level "mainMenu" element, including nested menus
        """
        # Get all "menu" elements that are the direct children of element
        menus = element.findall(cls.tag)

        # Assert that there's only one (mainMenu)
        if len(menus) != 1:
            raise ValueError("Structure of ElementTree not as expected.  Has Commands.cmd.xml changed?")
        mainmenu = menus[0]

        # Return all descendents (not just direct children) of mainMenu that are a menu element.
        # iter returns an iterable with this element first followed by all descendents.  Remove this element by
        # consuming the first before returning
        menu_iter = mainmenu.iter(cls.tag)
        next(menu_iter)
        return menu_iter

    @property
    def name(self, lineage_delimiter: str = '$'):
        lineage = re.sub(r'/', lineage_delimiter, self.lineage)
        if lineage:
            lineage += lineage_delimiter

        # Replace any characters that cannot be in a java attribute name with underscores
        label = re.sub(r"[^0-9a-zA-Z_$]", "_", self.label)
        # Collapse runs of underscores into a single underscore
        label = re.sub(r"_+", "_", label)

        return lineage + label

    def _field_name_parser(self, field_name):
        return generate_name(self.name, field_name)


SHORTCUT_TRANSLATABLE_FIELDS = ["value", "title"]
SHORTCUT_NON_TRANSLATABLE_FIELDS = ["refid"]


class Shortcut(ElementParser):
    tag = "shortcut"

    def __init__(self, xml_element: ElementTree):
        """
        Parsing structure to convert a Command in Commands.cmd.xml format to translatable Java constant statements

        :param xml_element: XML ElementTree describing a Command
        """
        super().__init__(xml_element)
        self.non_translatable_fields = SHORTCUT_NON_TRANSLATABLE_FIELDS
        self.refid = None

        # Fields that could be translated
        self.translatable_fields = SHORTCUT_TRANSLATABLE_FIELDS
        self.value = None
        self.title = None

        # Ensure these match the fields in TRANSLATABLE_FIELDS
        # (these could instead be defined dynamically from TRANSLATABLE_FIELDS,
        # but that feels more opaque...)
        self._ensure_translatable_fields_exist()

        self.parse()

    @classmethod
    def grep_from_element_tree(cls, element: ElementTree):
        return element.iter(cls.tag)

    @property
    def name(self):
        return self.refid

    def _field_name_parser(self, field_name):
        return generate_name(self.name, field_name)


# Helpers

def generate_name(this_id, field_name):
    """Helper to generate a name from id and field_name"""
    return f"{this_id}{capitalize_first(field_name)}"


def capitalize_first(s: str):
    """Returns the string s with the first character as uppercase"""
    return s[0].upper() + s[1:]


def get_lineage(e: ElementTree):
    parent = e.getparent()
    if parent is None:
        return ""
    else:
        grandparents = get_lineage(parent)
        if grandparents:
            grandparents += "/"
        return grandparents + parent.attrib.get('label', "")

