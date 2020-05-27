#
# test-code-submit.py
#
# Copyright (C) 2020 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# This script is used to test interactive submission of code to the Python
# REPL. In particular, RStudio should transparently fix up the indentation of
# blank lines within indented blocks when the whole block of code is submitted
# as a single expression.
def test():

   # This is the first block.
   first = "first"

   # This is the second block.
   second = "second"

   # Let's try to print it.
   print(first + " " + second)
   
test()

# Test submission of a class definition.
class HelloWorld(object):
   
   def __init__(self):
      pass
      
   def world(self):
      print("Hello, world!")

hello = HelloWorld()
hello.world()


# Test nested functions.
def outer():
   
   # Define an inner function.
   def inner():

      print("inner")

   # Now back at 'outer' block scope.
   inner()

   print("outer")
   
# Invoke outer.
outer()
