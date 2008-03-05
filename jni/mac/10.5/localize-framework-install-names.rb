#!/usr/bin/env ruby
require 'ostruct';
require 'pathname';

$CONFIG = OpenStruct.new
$CONFIG.root = File.dirname(__FILE__)
$CONFIG.frameworks = "#{$CONFIG.root}/Frameworks"

class Framework
  attr_reader :libs,:id
  def initialize(framework)
    @path = "#{$CONFIG.frameworks}/#{framework}.framework/Versions/Current/#{framework}"
    reload
    puts @libs.inspect
  end
  
  def id=(name)
    system(
      "install_name_tool",
      "-id",
      name,
      @path)
    reload
  end

  def get_install_name(lib)
    return @libs[lib]
  end

  def set_install_name(lib,val)
    puts "lib=#{lib}/#{@libs[lib]}, val=#{val}"
    system(
      "install_name_tool",
      "-change",
      @libs[lib],
      val,
      @path)
    reload
  end

  private
  def reload
    @libs = IO.popen("otool -L '#{@path}'") { |fh|
      fh.readline
      if fh.readline =~ /([^(]*)/
        @id = $1.strip
      else
        raise "Unable to read otool of framework (#{@path})"
      end
      fh.inject({}) { |coll,line|
        if line =~ /(\w+)\.framework/
          name = $1
          if line =~ /([^(]*)/
            coll.update(name => $1.strip)
          else
            raise "Unable to read otool of framework (#{@path})"
          end
        end
        coll
      }
    }.freeze
  end
end

def create_frameworks
  ["WebKit","WebCore","JavaScriptCore"].map { |name|
    Framework.new(name)
  }
end

def create_framework_ids
  path = Pathname.new(File.dirname(__FILE__)).realpath
  ["WebKit", "WebCore", "JavaScriptCore"].map { |name|
    "#{path}/Frameworks/#{name}.framework/Versions/A/#{name}"
  }
end

def localize_install_names
  wk_fr, wc_fr, js_fr = create_frameworks
  #wk_id, wc_id, js_id = "gwt/WebKit.framework", "gwt/WebCore.framework", "gwt/JavaScriptCore.framework"
  wk_id, wc_id, js_id = create_framework_ids

  js_fr.id = js_id

  wc_fr.id = wc_id
  wc_fr.set_install_name "JavaScriptCore", js_id

  wk_fr.id = wk_id
  wk_fr.set_install_name "JavaScriptCore", js_id
  wk_fr.set_install_name "WebCore", wc_id
end

localize_install_names
