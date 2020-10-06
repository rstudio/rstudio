#!/usr/bin/env ruby

#
# unicode-chars-util.rb
#
# Copyright (C) 2020 by RStudio, PBC
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

# Takes utf_info.cxx from Hunspell, which is a list of the Unicode code points
# that the spelling tokenizer should treat as letters, and turns them into a
# compact string representation to be used in UnicodeLetters.java.

if ARGV.length != 1
	$stderr.puts "Usage: #{$0} <path/to/utf_info.cxx>"
	$stderr.puts "utf_info.cxx is available from the Hunspell source distribution"
	exit 1
end

def emit(from, to)
	printf "\\u%04X\\u%04X", from, to
end

open = nil
prev = -1

print '"'
File.open(ARGV[0], "r") do |file|  
	file.each do |line|
		next unless line =~ /\{ 0x([0-9A-F]{4}), 0x([0-9A-F]{4}), 0x([0-9A-F]{4}) }/
		value = $1.to_i(16)

		raise "Lines are not sorted, script needs to be modified to sort lines!" unless value > prev

		if value != prev + 1
			emit(open, prev) if open
			open = value
		end
		prev = value
	end
end

emit(open, prev) if open
puts '"'
