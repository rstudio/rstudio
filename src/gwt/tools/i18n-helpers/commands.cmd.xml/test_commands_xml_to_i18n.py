import pytest
from lxml import etree
from element_parsers import Command

@pytest.fixture()
def sample_command():
    cmd_xml = """
    <cmd id="cmdId"
     label="cmdLabel"
     buttonLabel="cmdButtonlabel"
     menuLabel="cmd_MenuLabel"
     desc="cmdDesc"
     checkable="false"
     radio="false"
     visible="false"
     windowMode="main"
     rebindable="false"/>
    """
    return etree.fromstring(cmd_xml)


def test_Command_parse(sample_command):
    c = Command(sample_command)

    fields_to_compare = ['id'] + c.translatable_fields
    for fieldname in fields_to_compare:
        assert getattr(c, fieldname) == sample_command.get(fieldname)
