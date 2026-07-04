# Generates the Android launcher resource PNGs from the approved "bubble + han->A" design.
# Outputs into out/res/mipmap-<density>/:
#   ic_launcher.png / ic_launcher_round.png  (full square tile, legacy fallback)
#   ic_launcher_foreground.png               (bubble+text on transparent, for adaptive icon)
# Korean glyphs are built from Unicode code points to avoid source-encoding issues.

Add-Type -AssemblyName System.Drawing

$han   = [string][char]0xD55C
$arrow = [string][char]0x2192
$hanArrow = "$han$arrow"

$resDir = Join-Path $PSScriptRoot 'out\res'

$tile   = [System.Drawing.Color]::FromArgb(255, 26, 95, 180)
$bubble = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)
$blue   = [System.Drawing.Color]::FromArgb(255, 14, 76, 146)
$orange = [System.Drawing.Color]::FromArgb(255, 232, 98, 44)

function Add-RoundedRect($path, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $d = 2 * $r
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
}

# Draws the icon into a bitmap of side $S.
#   $drawTile   : fill the blue rounded tile (legacy) vs transparent (adaptive foreground)
#   $contentScale: scale the bubble+text within the canvas (adaptive fg keeps it in the safe zone)
function New-Icon([int]$S, [bool]$drawTile, [double]$contentScale) {
    $bmp = New-Object System.Drawing.Bitmap($S, $S)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $base = $S / 512.0
    if ($drawTile) {
        $tilePath = New-Object System.Drawing.Drawing2D.GraphicsPath
        Add-RoundedRect $tilePath 0 0 $S $S (112 * $base)
        $g.FillPath((New-Object System.Drawing.SolidBrush($tile)), $tilePath)
    }

    # content mapping (512 logical coords -> pixels), centered when scaled down
    $cf = $base * $contentScale
    $ox = ($S - 512 * $cf) / 2.0
    $oy = ($S - 512 * $cf) / 2.0

    $bubBrush = New-Object System.Drawing.SolidBrush($bubble)
    $bodyPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    Add-RoundedRect $bodyPath ($ox + 32 * $cf) ($oy + 92 * $cf) (448 * $cf) (262 * $cf) (56 * $cf)
    $g.FillPath($bubBrush, $bodyPath)
    $tail = @(
        (New-Object System.Drawing.PointF(($ox + 146 * $cf), ($oy + 332 * $cf))),
        (New-Object System.Drawing.PointF(($ox + 146 * $cf), ($oy + 444 * $cf))),
        (New-Object System.Drawing.PointF(($ox + 236 * $cf), ($oy + 346 * $cf)))
    )
    $g.FillPolygon($bubBrush, $tail)

    $fmt = [System.Drawing.StringFormat]::GenericTypographic
    $probe = New-Object System.Drawing.Font("Malgun Gothic", (100 * $cf), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $w1p = $g.MeasureString($hanArrow, $probe, [int]::MaxValue, $fmt).Width
    $w2p = $g.MeasureString("A", $probe, [int]::MaxValue, $fmt).Width
    $size = (100 * $cf) * ((356 * $cf) / ($w1p + $w2p))

    $font = New-Object System.Drawing.Font("Malgun Gothic", $size, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $w1 = $g.MeasureString($hanArrow, $font, [int]::MaxValue, $fmt).Width
    $w2 = $g.MeasureString("A", $font, [int]::MaxValue, $fmt).Width
    $th = $g.MeasureString($han, $font, [int]::MaxValue, $fmt).Height
    $startX = ($S - ($w1 + $w2)) / 2.0
    $ty = ($oy + 216 * $cf) - $th / 2.0

    $g.DrawString($hanArrow, $font, (New-Object System.Drawing.SolidBrush($blue)), $startX, $ty, $fmt)
    $g.DrawString("A", $font, (New-Object System.Drawing.SolidBrush($orange)), ($startX + $w1), $ty, $fmt)

    $g.Dispose()
    return $bmp
}

function Save-Png($bmp, $path) {
    $dir = Split-Path $path
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "saved $path"
}

# density -> (legacy tile px, adaptive foreground px[108dp])
$densities = @{
    'mdpi'    = @(48, 108)
    'hdpi'    = @(72, 162)
    'xhdpi'   = @(96, 216)
    'xxhdpi'  = @(144, 324)
    'xxxhdpi' = @(192, 432)
}

foreach ($d in $densities.Keys) {
    $legacyPx = $densities[$d][0]
    $fgPx = $densities[$d][1]
    $dir = Join-Path $resDir "mipmap-$d"

    Save-Png (New-Icon $legacyPx $true 1.0)  (Join-Path $dir 'ic_launcher.png')
    Save-Png (New-Icon $legacyPx $true 1.0)  (Join-Path $dir 'ic_launcher_round.png')
    Save-Png (New-Icon $fgPx    $false 0.80) (Join-Path $dir 'ic_launcher_foreground.png')
}
