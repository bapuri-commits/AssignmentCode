function XE($s) { [System.Security.SecurityElement]::Escape($s) }

function Cell {
    param($id, $nm, $st, [string[]]$f, [string[]]$m, $x, $y, $w, $h, $fc, $sc)
    $hdr = if ($st) { "<i>$st</i><br><b>$nm</b>" } else { "<b>$nm</b>" }
    $fh = if ($f.Count -gt 0) { $f -join "<br>" } else { " " }
    $mh = if ($m.Count -gt 0) { $m -join "<br>" } else { " " }
    $html = "<div style=`"text-align:center;border-bottom:1px solid #999;padding:5px 4px;`">$hdr</div>" +
            "<div style=`"text-align:left;border-bottom:1px solid #999;padding:3px 6px;font-size:12px;`">$fh</div>" +
            "<div style=`"text-align:left;padding:3px 6px;font-size:12px;`">$mh</div>"
    $v = XE $html
    $sty = "rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacing=0;overflow=fill;fillColor=$fc;strokeColor=$sc;"
    "    <mxCell id=`"$id`" value=`"$v`" style=`"$sty`" vertex=`"1`" parent=`"1`"><mxGeometry x=`"$x`" y=`"$y`" width=`"$w`" height=`"$h`" as=`"geometry`"/></mxCell>"
}

function Edge {
    param($id, $src, $tgt, $type)
    $sty = switch ($type) {
        "inherit" { "endArrow=block;endSize=16;endFill=0;html=1;" }
        "aggr"    { "startArrow=diamondThin;startFill=0;startSize=14;endArrow=open;endSize=10;html=1;" }
        "comp"    { "startArrow=diamondThin;startFill=1;startSize=14;endArrow=open;endSize=10;html=1;" }
    }
    "    <mxCell id=`"$id`" style=`"$sty`" edge=`"1`" source=`"$src`" target=`"$tgt`" parent=`"1`"><mxGeometry relative=`"1`" as=`"geometry`"/></mxCell>"
}

$BF="#dae8fc"; $BS="#6c8ebf"
$WF="#ffffff"; $WS="#000000"
$YF="#fff2cc"; $YS="#d6b656"
$GF="#d5e8d4"; $GS="#82b366"

$c = @()

$c += Cell -id "c2" -nm "Text" -st "<<abstract>>" `
    -f @() `
    -m @("+ ~Text() : virtual","+ getContent() : string = 0") `
    -x 30 -y 30 -w 280 -h 110 -fc $BF -sc $BS

$c += Cell -id "c3" -nm "NameText" -st "(revised)" `
    -f @("- formattedName: string","- initials: string","- rawLength: size_t","- valid: bool") `
    -m @("+ NameText(rawInput: string)","+ parse(input: string): void","+ checkValid(): bool","+ getContent(): string","+ getInitials(): string","+ getRawInputLength(): size_t","+ getDisplayNameLength(): int") `
    -x 30 -y 210 -w 300 -h 290 -fc $YF -sc $YS

$c += Cell -id "c4" -nm "CardStencil" -st "<<abstract>>" `
    -f @("# borderChar: char") `
    -m @("+ ~CardStencil() : virtual","+ getPattern(): string = 0","+ render(name, initials): string = 0","+ setBorderChar(c: char): void") `
    -x 390 -y 30 -w 340 -h 175 -fc $BF -sc $BS

$c += Cell -id "c5" -nm "CardStencil1" -st "" `
    -f @() `
    -m @("+ getPattern(): string","+ render(name, initials): string") `
    -x 390 -y 275 -w 340 -h 90 -fc $WF -sc $WS

$c += Cell -id "c6" -nm "Product" -st "<<abstract>> (revised)" `
    -f @() `
    -m @("+ ~Product() : virtual","+ toString() : string = 0") `
    -x 790 -y 30 -w 270 -h 120 -fc $YF -sc $YS

$c += Cell -id "c7" -nm "Card" -st "(revised)" `
    -f @("- stencil: shared_ptr&lt;CardStencil&gt;","- cardText: shared_ptr&lt;NameText&gt;") `
    -m @("+ Card(s, t)","+ static create(name): shared_ptr&lt;Card&gt;","+ toString(): string","+ setBorder(c: char): void","+ getDisplayNameLength(): int") `
    -x 790 -y 220 -w 330 -h 210 -fc $YF -sc $YS

$c += Cell -id "c8" -nm "Order" -st "<<interface>>" `
    -f @() `
    -m @("+ ~Order() : virtual","+ getProduct(): shared_ptr&lt;Product&gt;","+ getQuantity(): int","+ getTotalPrice(): int") `
    -x 1180 -y 30 -w 330 -h 145 -fc $BF -sc $BS

$c += Cell -id "c9" -nm "CardOrder" -st "(revised)" `
    -f @("- card: shared_ptr&lt;Card&gt;","- calculator: Calculator","- quantity: int") `
    -m @("+ CardOrder(card: shared_ptr&lt;Card&gt;)","+ setQuantity(q: int): void","+ getProduct(): shared_ptr&lt;Product&gt;","+ getQuantity(): int","+ getTotalPrice(): int","+ getDiscountMsg(): string","+ getCard(): shared_ptr&lt;Card&gt;") `
    -x 1180 -y 245 -w 340 -h 280 -fc $YF -sc $YS

$c += Cell -id "c10" -nm "Calculator" -st "(new)" `
    -f @() `
    -m @("+ calcPricePerCard(len: int): int","+ calcTotal(len: int, qty: int): int","+ hasDiscount(qty: int): bool","+ getDiscountMsg(qty: int): string") `
    -x 1180 -y 595 -w 340 -h 155 -fc $GF -sc $GS

$c += Cell -id "c11" -nm "POS" -st "<<abstract>>" `
    -f @("# input: shared_ptr&lt;Input&gt;","# output: shared_ptr&lt;Output&gt;") `
    -m @("+ POS(i, o)","+ ~POS(): virtual","+ addOrder(order): void = 0","+ calculateTotal(): int = 0","+ getOrders(): vector = 0","+ runScenario(): void = 0") `
    -x 1580 -y 30 -w 360 -h 215 -fc $BF -sc $BS

$c += Cell -id "c12" -nm "VendingMachinePOS" -st "" `
    -f @("- orders: vector&lt;shared_ptr&lt;Order&gt;&gt;") `
    -m @("+ VendingMachinePOS(i, o)","+ addOrder(order): void","+ calculateTotal(): int","+ getOrders(): vector","+ runScenario(): void") `
    -x 1580 -y 315 -w 360 -h 180 -fc $WF -sc $WS

$c += Cell -id "c13" -nm "Input" -st "<<abstract>>" `
    -f @() `
    -m @("+ readLine(): string = 0") `
    -x 2000 -y 30 -w 240 -h 90 -fc $BF -sc $BS

$c += Cell -id "c14" -nm "Output" -st "<<abstract>>" `
    -f @() `
    -m @("+ writeLine(msg: string): void = 0") `
    -x 2000 -y 150 -w 260 -h 90 -fc $BF -sc $BS

$c += Cell -id "c15" -nm "UserInput" -st "" `
    -f @() -m @("+ readLine(): string") `
    -x 2000 -y 340 -w 210 -h 65 -fc $WF -sc $WS

$c += Cell -id "c16" -nm "UserOutput" -st "" `
    -f @() -m @("+ writeLine(msg): void") `
    -x 2250 -y 340 -w 220 -h 65 -fc $WF -sc $WS

$c += Cell -id "c17" -nm "TestInput" -st "" `
    -f @("- inputs: queue&lt;string&gt;") `
    -m @("+ pushInput(input: string): void","+ readLine(): string") `
    -x 2000 -y 440 -w 260 -h 100 -fc $WF -sc $WS

$c += Cell -id "c18" -nm "TestOutput" -st "" `
    -f @("- outputs: vector&lt;string&gt;") `
    -m @("+ writeLine(msg): void","+ getOutputs(): vector&lt;string&gt;") `
    -x 2250 -y 440 -w 270 -h 100 -fc $WF -sc $WS

# Legend
$c += Cell -id "c90" -nm "Legend" -st "" `
    -f @() `
    -m @("Blue = abstract/interface","Yellow = revised","Green = new class") `
    -x 30 -y 570 -w 200 -h 100 -fc "#f5f5f5" -sc "#666666"

$e = @()
$e += Edge -id "e1"  -src "c3"  -tgt "c2"  -type "inherit"
$e += Edge -id "e2"  -src "c5"  -tgt "c4"  -type "inherit"
$e += Edge -id "e3"  -src "c7"  -tgt "c6"  -type "inherit"
$e += Edge -id "e4"  -src "c9"  -tgt "c8"  -type "inherit"
$e += Edge -id "e5"  -src "c12" -tgt "c11" -type "inherit"
$e += Edge -id "e6"  -src "c15" -tgt "c13" -type "inherit"
$e += Edge -id "e7"  -src "c16" -tgt "c14" -type "inherit"
$e += Edge -id "e8"  -src "c17" -tgt "c13" -type "inherit"
$e += Edge -id "e9"  -src "c18" -tgt "c14" -type "inherit"

$e += Edge -id "e10" -src "c7"  -tgt "c4"  -type "aggr"
$e += Edge -id "e11" -src "c7"  -tgt "c2"  -type "aggr"
$e += Edge -id "e12" -src "c9"  -tgt "c7"  -type "aggr"
$e += Edge -id "e13" -src "c9"  -tgt "c10" -type "comp"
$e += Edge -id "e14" -src "c11" -tgt "c13" -type "aggr"
$e += Edge -id "e15" -src "c11" -tgt "c14" -type "aggr"

$all = ($c + $e) -join "`n"

$xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<mxfile>
  <diagram name="LAB1-Revised" id="rev1">
    <mxGraphModel dx="1422" dy="762" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="2800" pageHeight="1000">
      <root>
    <mxCell id="0"/>
    <mxCell id="1" parent="0"/>
$all
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>
"@

[System.IO.File]::WriteAllText(
    "c:\Users\chois\source\repos\OOPstructure\lab1\docs\teammade-revised.drawio",
    $xml,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Done: teammade-revised.drawio"
