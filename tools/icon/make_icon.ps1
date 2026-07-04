# App icon mockup generator: "speech bubble + han -> A"
# Uses System.Drawing (GDI+) so the Korean glyph renders crisply (vectors can't),
# then downscales to real launcher sizes (512/192/96/48) to check small-size legibility.
# NOTE: Korean glyphs are built from Unicode code points to avoid source-encoding issues.

Add-Type -AssemblyName System.Drawing

$han   = [string][char]0xD55C   # Korean syllable "han"
$arrow = [string][char]0x2192   # rightwards arrow
$hanArrow = "$han$arrow"

$outDir = Join-Path $PSScriptRoot 'out'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# colors
$tile   = [System.Drawing.Color]::FromArgb(255, 26, 95, 180)    # #1A5FB4 blue tile
$bubble = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)  # white bubble
$blue   = [System.Drawing.Color]::FromArgb(255, 14, 76, 146)    # #0E4C92 (han + arrow)
$orange = [System.Drawing.Color]::FromArgb(255, 232, 98, 44)    # #E8622C (A)

function Add-RoundedRect($path, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $d = 2 * $r
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
}

function New-MasterIcon([int]$S) {
    $bmp = New-Object System.Drawing.Bitmap($S, $S)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $f = $S / 512.0   # scale from 512-based coords

    # 1) blue rounded tile
    $tilePath = New-Object System.Drawing.Drawing2D.GraphicsPath
    Add-RoundedRect $tilePath 0 0 $S $S (112 * $f)
    $g.FillPath((New-Object System.Drawing.SolidBrush($tile)), $tilePath)

    # 2) white speech bubble (body + tail)
    $bubBrush = New-Object System.Drawing.SolidBrush($bubble)
    $bodyPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    Add-RoundedRect $bodyPath (32 * $f) (92 * $f) (448 * $f) (262 * $f) (56 * $f)
    $g.FillPath($bubBrush, $bodyPath)
    $tail = @(
        (New-Object System.Drawing.PointF((146 * $f), (332 * $f))),
        (New-Object System.Drawing.PointF((146 * $f), (444 * $f))),
        (New-Object System.Drawing.PointF((236 * $f), (346 * $f)))
    )
    $g.FillPolygon($bubBrush, $tail)

    # 3) text "han -> A" (han+arrow blue, A orange), auto-fit to width
    $fmt = [System.Drawing.StringFormat]::GenericTypographic
    $probe = New-Object System.Drawing.Font("Malgun Gothic", (100 * $f), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $w1p = $g.MeasureString($hanArrow, $probe, [int]::MaxValue, $fmt).Width
    $w2p = $g.MeasureString("A", $probe, [int]::MaxValue, $fmt).Width
    $target = 356 * $f
    $size = (100 * $f) * ($target / ($w1p + $w2p))

    $font = New-Object System.Drawing.Font("Malgun Gothic", $size, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $w1 = $g.MeasureString($hanArrow, $font, [int]::MaxValue, $fmt).Width
    $w2 = $g.MeasureString("A", $font, [int]::MaxValue, $fmt).Width
    $th = $g.MeasureString($han, $font, [int]::MaxValue, $fmt).Height
    $startX = ($S - ($w1 + $w2)) / 2.0
    $ty = (216 * $f) - $th / 2.0

    $g.DrawString($hanArrow, $font, (New-Object System.Drawing.SolidBrush($blue)), $startX, $ty, $fmt)
    $g.DrawString("A", $font, (New-Object System.Drawing.SolidBrush($orange)), ($startX + $w1), $ty, $fmt)

    $g.Dispose()
    return $bmp
}

$master = New-MasterIcon 512
foreach ($sz in 512, 192, 96, 48) {
    $out = New-Object System.Drawing.Bitmap($sz, $sz)
    $gg = [System.Drawing.Graphics]::FromImage($out)
    $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $gg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $gg.DrawImage($master, 0, 0, $sz, $sz)
    $path = Join-Path $outDir "icon_$sz.png"
    $out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $gg.Dispose(); $out.Dispose()
    Write-Output "saved $path"
}
$master.Dispose()
