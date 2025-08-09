#!/usr/bin/env python3

import os
import re
import csv
import glob
from pathlib import Path

# Generated files to exclude (category 1)
GENERATED_FILES = {
    'src/gwt/src/org/rstudio/studio/client/workbench/prefs/model/UserPrefsAccessorConstants.java',
    'src/gwt/src/org/rstudio/studio/client/workbench/prefs/model/UserStateAccessorConstants.java',
    'src/gwt/src/org/rstudio/studio/client/workbench/commands/MenuConstants.java',
    'src/gwt/src/org/rstudio/studio/client/workbench/commands/CmdConstants.java'
}

def is_gwt_localization_interface(java_path):
    """Check if the Java file contains an interface extending Messages or Constants."""
    try:
        with open(java_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
        # Check if it's an interface that extends Messages or Constants
        # Pattern matches: interface SomeName extends [...]Messages or [...]Constants
        pattern = r'interface\s+\w+\s+extends\s+(?:[\w.]*(?:Messages|Constants))'
        return bool(re.search(pattern, content))
        
    except Exception as e:
        print(f"Error checking {java_path}: {e}")
        return False

def parse_java_file(java_path):
    """Parse a Java Constants file to extract method keys."""
    keys = []
    
    try:
        with open(java_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
        # Find all method declarations that look like localization strings
        # Pattern matches methods that return String and have @DefaultMessage
        pattern = r'@DefaultMessage\([^)]+\)\s+(?:@AlternateMessage\([^)]+\)\s+)?String\s+(\w+)\('
        matches = re.findall(pattern, content, re.MULTILINE | re.DOTALL)
        keys.extend(matches)
        
        # Also find simpler patterns without annotations
        pattern2 = r'^\s*String\s+(\w+)\(\);'
        matches2 = re.findall(pattern2, content, re.MULTILINE)
        for match in matches2:
            if match not in keys:
                keys.append(match)
                
    except Exception as e:
        print(f"Error parsing {java_path}: {e}")
        
    return keys

def parse_properties_file(prop_path):
    """Parse a properties file and return a dictionary of key-value pairs."""
    properties = {}
    
    if not os.path.exists(prop_path):
        return properties
        
    try:
        with open(prop_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                # Skip comments and empty lines
                if line.startswith('#') or line.startswith('!') or not line:
                    continue
                    
                # Handle multi-line values (lines ending with \)
                while line.endswith('\\'):
                    next_line = next(f, '').strip()
                    line = line[:-1] + next_line
                    
                # Split on first = or :
                match = re.match(r'^([^=:]+)[=:](.*)$', line)
                if match:
                    key = match.group(1).strip()
                    value = match.group(2).strip()
                    
                    # Handle plural forms like key[one]=value
                    base_key = re.match(r'^([^\[]+)', key).group(1)
                    if base_key not in properties:
                        properties[base_key] = value
                        
    except Exception as e:
        print(f"Error parsing {prop_path}: {e}")
        
    return properties

def main():
    # Find all Constants Java files (not Messages since we're looking for *Constants.java)
    java_files = glob.glob('src/gwt/src/org/**/*Constants.java', recursive=True)
    
    # Filter out test files and generated files
    java_files = [f for f in java_files if '/test/' not in f and f not in GENERATED_FILES]
    
    # Also filter out UI binder generated messages
    java_files = [f for f in java_files if 'BinderImplGenMessages' not in f]
    
    # Filter to only include files that are GWT localization interfaces
    java_files = [f for f in java_files if is_gwt_localization_interface(f)]
    
    # Sort for consistent output
    java_files.sort()
    
    # Process each Java file
    rows = []
    
    for java_path in java_files:
        print(f"Processing {java_path}")
        
        # Extract keys from Java file
        keys = parse_java_file(java_path)
        
        if not keys:
            continue
            
        # Determine properties file paths
        base_path = java_path[:-5]  # Remove .java
        en_props = base_path + '_en.properties'
        fr_props = base_path + '_fr.properties'
        
        # Parse properties files
        en_properties = parse_properties_file(en_props)
        fr_properties = parse_properties_file(fr_props)
        
        # Create rows for CSV
        for key in keys:
            row = {
                'path': java_path,
                'key': key,
                'en': en_properties.get(key, ''),
                'fr': fr_properties.get(key, '')
            }
            rows.append(row)
    
    # Write CSV file
    output_path = 'strings.csv'
    with open(output_path, 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = ['path', 'key', 'en', 'fr']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
    
    print(f"\nCreated {output_path} with {len(rows)} entries from {len(java_files)} Java files")

if __name__ == '__main__':
    main()