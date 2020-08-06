
-- custom format that extracts all explicit heading ids from a document
-- and writes them to stdout

-- capture heading id
function Header(lev, s, attr)
  if string.len(attr.id) > 0 then
    io.write('#' .. attr.id .. '\n')
  end
  return ''
end

-- capture links
function Link(s, src, tit, attr)
  if string.len(src) > 1 and string.sub(src, 1, 1) == '#' then
    io.write(src .. '\n')     
  end
  return ''
end

-- no-op everything else
function Doc(body, metadata, variables)
  return ''
end

function Str(s)
  return ''
end

function Space()
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

function Span(s, attr)
  return '' 
end

function RawInline(format, str)
  return ''
end

function Cite(s, cs)
  return ''
end

function Para(s)
  return ''
end

function CodeBlock(s, attr)
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

function SoftBreak()
  return ''
end

function LineBreak()
  return ''
end

function Image(s, src, tit, attr)
  return ''
end

function CaptionedImage(src, tit, caption, attr)
  return ''   
end

function Note(s)
  return ''  
end

function Plain(s)
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

