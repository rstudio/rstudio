#!/usr/bin/env python

import os
import argparse
from lxml import etree
from xml_to_i18n import xml_to_interface, xml_to_properties
from xml_to_i18n import DEFAULT_PARSERS as SUPPORTED_TYPES_TO_PARSE

OUTPUT_TYPES = ["constant", "properties"]


def validate_type_to_parse(value):
    if value not in SUPPORTED_TYPES_TO_PARSE:
        raise argparse.ArgumentTypeError(f"{value} is not a valid type_to_parse.  Must be one of {SUPPORTED_TYPES_TO_PARSE}")
    return str(value)


def validate_output_type(file_type):
    if file_type not in OUTPUT_TYPES:
        raise argparse.ArgumentTypeError(f"{file_type} is not a valid output type.  Must be one of {OUTPUT_TYPES}")
    return str(file_type)


def parser_name_to_interface_name(parser_name):
    parser_name = str(parser_name)
    return parser_name[0].upper() + parser_name[1:] + "Constants"


def parse_args():
    parser = argparse.ArgumentParser(
        description="Helper to translate commands.cmd.xml to Java/GWT interface and properties file"
    )

    parser.add_argument("xmlfile", type=str, help='Path to commands.cmd.xml to parse')
    parser.add_argument("type_to_parse",
                        type=validate_type_to_parse,
                        help=f'Type of Commands.cmd.xml entry to parse.  Must be one of {SUPPORTED_TYPES_TO_PARSE}')
    parser.add_argument("output_type",
                        type=validate_output_type,
                        help=f"Type of file to output.  Must be one of {OUTPUT_TYPES}"
                        )
    parser.add_argument("output_path", type=str, help="Filename to output")
    parser.add_argument("--prefix",
                        type=str,
                        default="",
                        help=f"Text prefix used to prepend all output text values.  Useful to visually "
                             f"highlight when a default text statement is used vs a translated one during debugging")
    parser.add_argument("--packages", "--package",
                        default=[],
                        action='append',
                        help="Add one or more package calls to the top of a constants output file.  For multiple "
                             "packages, invoke this multiple times.  Only applies if ")

    return parser.parse_args()


def generate_constants(root, element_type, output_path, prefix=""):
    print(f"Creating constant interface for {element_type} with prefix '{prefix}'\nOutputting to {output_path}")
    interface_name = parser_name_to_interface_name(element_type)
    basename = os.path.basename(output_path)
    constants = xml_to_interface(interface_name, root, element_type=element_type, filename=basename, prefix=prefix)
    for package in args.packages:
        constants.add_package(package)
    constants.write(output_path)


def generate_properties(root, element_type, output_path, prefix=""):
    print(f"Creating properties file for {element_type} with prefix '{prefix}'\nOutputting to {output_path}")
    properties = xml_to_properties(root, element_type=element_type, prefix=prefix)
    properties.write(output_path)


if __name__ == "__main__":
    args = parse_args()

    tree = etree.parse(args.xmlfile)
    root = tree.getroot()

    if args.output_type == 'constant':
        generate_constants(root, args.type_to_parse, args.output_path, prefix=args.prefix)
    elif args.output_type == 'properties':
        generate_properties(root, args.type_to_parse, args.output_path, prefix=args.prefix)
    else:
        raise ValueError("Action unspecified for output_type '{args.output_type}'")
