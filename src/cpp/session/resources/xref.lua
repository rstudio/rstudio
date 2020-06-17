

function Doc(body, metadata, variables)
  return body
end

function Str(s)
  return s
end

function Header(lev, s, attr)
  return attr.id .. '\n'
end

function CodeBlock(s, attr)
  return '' 
end

function CaptionedImage(src, tit, caption, attr)
  return ''   
end

function Table(caption, aligns, widths, headers, rows)
  return ''
end

function DisplayMath(s)
  return ''  
end

function Blocksep()
  return ''
end


function Space()
  return ''
end

function SoftBreak()
  return ''
end

function LineBreak()
  return ''
end

function Emph(s)
  return ''
end

function Strong(s)
  return ''
end

function Subscript(s)
  return ''
end

function Superscript(s)
  return ''
end

function SmallCaps(s)
  return ''
end

function Strikeout(s)
  return ''
end

function Link(s, src, tit, attr)
  return ''
end

function Image(s, src, tit, attr)
  return ''
end

function Code(s, attr)
  return ''
end

function InlineMath(s)
  return ''  
end

function SingleQuoted(s)
  return ''  
end

function DoubleQuoted(s)
  return '' 
end

function Note(s)
  return ''  
end

function Span(s, attr)
  return '' 
end

function RawInline(format, str)
  return '' 
end

function Cite(s, cs)
  return ''
end

function Plain(s)
  return '' 
end

function Para(s)
  return '' 
end

function BlockQuote(s)
  return ''
end

function HorizontalRule()
  return '' 
end

function LineBlock(ls)
  return ''  
end

function BulletList(items)
  return ''  
end

function OrderedList(items)
  return '' 
end

function DefinitionList(items)
  return '' 
end

function RawBlock(format, str)
  return ''
end

function Div(s, attr)
  return ''
end

-- The following code will produce runtime warnings when you haven't defined
-- all of the functions you need for the custom writer, so it's useful
-- to include when you're working on a writer.
local meta = {}
meta.__index =
  function(_, key)
    io.stderr:write(string.format("WARNING: Undefined function '%s'\n",key))
    return function() return "" end
  end
setmetatable(_G, meta)

