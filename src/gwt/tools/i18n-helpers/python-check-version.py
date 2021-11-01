#!/usr/bin/env python

import sys

if __name__ == "__main__":
    if sys.version_info[0] < 3:
        raise Exception("Must use Python 3 for i18n generation")
