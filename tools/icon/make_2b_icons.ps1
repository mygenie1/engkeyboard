# Generates the "2b 틈새 블록" launcher icon resources.
# Two rounded key blocks placed askew with a small diagonal gap:
#   block 1 "한" (top-left, #46698C), block 2 "A" (bottom-right, #202B36), white bold glyphs.
# Outputs into out2b/res/mipmap-<density>/:
#   ic_launcher.png / ic_launcher_round.png  (white tile + blocks, legacy fallback)
#   ic_launcher_foreground.png               (blocks+glyphs on transparent, adaptive fg)
#   ic_launcher_monochrome.png               (two block silhouettes, single color, adaptive mono)
# Korean glyph built from a Unicode code point to avoid source-encoding issues.

Add-Type -AssemblyName System.Drawing

$han = [string][char]0xD55C   # "한"

$resDir = Join-Path $PSScriptRoot 'out2b\res'

$white  = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)
$block1 = [System.Drawing.Color]::FromArgb(255, 70, 105, 140)   # #46698C
$block2 = [System.Drawing.Color]::FromArgb(255, 32, 43, 54)     # #202B36
$mono   = [System.Drawing.Color]::FromArgb(255, 0, 0, 0)        # silhouette (system tints)

# normalized layout (fraction of canvas)
$blockFrac = 0.29
$gapFrac   = 0.05
$span      = 2 * $blockFrac + $gapFrac
$start     = (1.0 - $span) / 2.0
$o1        = $start
$o2        = $start + $blockFrac + $gapFrac
$radFrac   = 0.263 * $blockFrac   # 10/38 of the block

function Add-RoundedRect($path, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $d = 2 * $r
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
}

function Fill-Block($g, [int]$S, [double]$ox, [double]$oy, $color) {
    $x = $ox * $S; $y = $oy * $S; $wh = $blockFrac * $S; $r = $radFrac * $S
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    Add-RoundedRect $p $x $y $wh $wh $r
    $g.FillPath((New-Object System.Drawing.SolidBrush($color)), $p)
}

function Draw-Glyph($g, [int]$S, [double]$ox, [double]$oy, [string]$text) {
    $wh = $blockFrac * $S
    $cx = ($ox + $blockFrac / 2.0) * $S
    $cy = ($oy + $blockFrac / 2.0) * $S
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::Center
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Center
    $size = 0.47 * $wh   # glyph ~17 on a 38 block
    $font = New-Object System.Drawing.Font("Malgun Gothic", $size, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $rect = New-Object System.Drawing.RectangleF(($cx - $wh), ($cy - $wh), (2 * $wh), (2 * $wh))
    $g.DrawString($text, $font, (New-Object System.Drawing.SolidBrush($white)), $rect, $fmt)
}

# mode: 'legacy' | 'fg' | 'mono'
function New-Icon([int]$S, [string]$mode) {
    $bmp = New-Object System.Drawing.Bitmap($S, $S)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    if ($mode -eq 'legacy') {
        $tp = New-Object System.Drawing.Drawing2D.GraphicsPath
        Add-RoundedRect $tp 0 0 $S $S (0.18 * $S)
        $g.FillPath((New-Object System.Drawing.SolidBrush($white)), $tp)
    }

    if ($mode -eq 'mono') {
        Fill-Block $g $S $o1 $o1 $mono
        Fill-Block $g $S $o2 $o2 $mono
    } else {
        Fill-Block $g $S $o1 $o1 $block1
        Fill-Block $g $S $o2 $o2 $block2
        Draw-Glyph $g $S $o1 $o1 $han
        Draw-Glyph $g $S $o2 $o2 "A"
    }

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

    Save-Png (New-Icon $legacyPx 'legacy') (Join-Path $dir 'ic_launcher.png')
    Save-Png (New-Icon $legacyPx 'legacy') (Join-Path $dir 'ic_launcher_round.png')
    Save-Png (New-Icon $fgPx    'fg')     (Join-Path $dir 'ic_launcher_foreground.png')
    Save-Png (New-Icon $fgPx    'mono')   (Join-Path $dir 'ic_launcher_monochrome.png')
}
