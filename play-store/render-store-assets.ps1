param(
  [string] $SpecPath = "store-assets.spec.json"
)

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$specFile = if ([System.IO.Path]::IsPathRooted($SpecPath)) { $SpecPath } else { Join-Path $root $SpecPath }
$spec = Get-Content -LiteralPath $specFile -Raw -Encoding UTF8 | ConvertFrom-Json

function AssetPath($path) {
  if ([System.IO.Path]::IsPathRooted($path)) { return $path }
  return Join-Path $root $path
}

function Color($hex, [int] $alpha = 255) {
  $hex = $hex.TrimStart("#")
  [System.Drawing.Color]::FromArgb(
    $alpha,
    [Convert]::ToInt32($hex.Substring(0, 2), 16),
    [Convert]::ToInt32($hex.Substring(2, 2), 16),
    [Convert]::ToInt32($hex.Substring(4, 2), 16)
  )
}

function Rect($r) {
  New-Object System.Drawing.RectangleF `
    ([float]$r.x), ([float]$r.y), ([float]$r.width), ([float]$r.height)
}

function RoundedPath([System.Drawing.RectangleF] $rect, [float] $radius) {
  $path = New-Object System.Drawing.Drawing2D.GraphicsPath
  $d = $radius * 2
  $path.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
  $path.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
  $path.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
  $path.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
  $path.CloseFigure()
  return $path
}

function NewCanvas($canvas) {
  $bmp = New-Object System.Drawing.Bitmap ([int]$canvas.width), ([int]$canvas.height), ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
  return @{ Bitmap = $bmp; Graphics = $g }
}

function DrawBackground($g, $canvas, $style) {
  $rect = New-Object System.Drawing.Rectangle 0, 0, ([int]$canvas.width), ([int]$canvas.height)
  $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush `
    $rect, (Color $style.background[0]), (Color $style.background[1]), 120
  $g.FillRectangle($brush, $rect)
  $brush.Dispose()

  $glow = New-Object System.Drawing.SolidBrush (Color $style.green 42)
  $g.FillEllipse($glow, -150, [int]($canvas.height * 0.72), 520, 520)
  $g.FillEllipse($glow, [int]($canvas.width * 0.72), -170, 560, 560)
  $glow.Dispose()
}

function DrawText($g, $text, $fontName, $size, $style, $color, $x, $y, $width, $height) {
  $text = [string]$text -replace "\\n", "`n"
  $font = New-Object System.Drawing.Font $fontName, $size, $style, ([System.Drawing.GraphicsUnit]::Pixel)
  $brush = New-Object System.Drawing.SolidBrush $color
  $format = New-Object System.Drawing.StringFormat
  $format.Trimming = [System.Drawing.StringTrimming]::Word
  $g.DrawString($text, $font, $brush, (New-Object System.Drawing.RectangleF $x, $y, $width, $height), $format)
  $format.Dispose()
  $brush.Dispose()
  $font.Dispose()
}

function DrawBrandPill($g, $style, $x, $y) {
  $pillRect = New-Object System.Drawing.RectangleF $x, $y, 178, 60
  $pillPath = RoundedPath $pillRect 30
  $pillBrush = New-Object System.Drawing.SolidBrush (Color "#ffffff" 20)
  $pillPen = New-Object System.Drawing.Pen (Color "#ffffff" 42), 1
  $g.FillPath($pillBrush, $pillPath)
  $g.DrawPath($pillPen, $pillPath)
  $pillPen.Dispose()
  $pillBrush.Dispose()
  $pillPath.Dispose()
  DrawText $g $style.brand $style.latinFont 26 ([System.Drawing.FontStyle]::Bold) (Color $style.accent) ($x + 18) ($y + 10) 160 48
}

function SaveCanvas($canvas, $g, $outputPath) {
  $g.Dispose()
  $tmpPath = "$outputPath.tmp"
  $canvas.Save($tmpPath, [System.Drawing.Imaging.ImageFormat]::Png)
  $canvas.Dispose()
  Move-Item -LiteralPath $tmpPath -Destination $outputPath -Force
}

function RenderPhone($slide, $style) {
  $ctx = NewCanvas $style.phoneCanvas
  $bmp = $ctx.Bitmap
  $g = $ctx.Graphics
  DrawBackground $g $style.phoneCanvas $style

  $copy = $style.phoneCopy
  DrawText $g $slide.title $style.font $copy.titleSize ([System.Drawing.FontStyle]::Bold) (Color $style.text) $copy.x $copy.y 900 88
  DrawText $g $slide.subtitle $style.font $copy.subtitleSize ([System.Drawing.FontStyle]::Regular) (Color $style.mutedText 220) $copy.x ($copy.y + 84) 900 54

  $deviceRect = Rect $style.phoneDevice
  $shadowRect = New-Object System.Drawing.RectangleF `
    ([float]($deviceRect.X + 10)), ([float]($deviceRect.Y + 16)), $deviceRect.Width, $deviceRect.Height
  $shadowPath = RoundedPath $shadowRect $style.phoneDevice.radius
  $shadowBrush = New-Object System.Drawing.SolidBrush (Color "#000000" 60)
  $g.FillPath($shadowBrush, $shadowPath)
  $shadowBrush.Dispose()
  $shadowPath.Dispose()

  $devicePath = RoundedPath $deviceRect $style.phoneDevice.radius
  $deviceBrush = New-Object System.Drawing.SolidBrush (Color "#07121e")
  $g.FillPath($deviceBrush, $devicePath)
  $deviceBrush.Dispose()

  $screen = [System.Drawing.Bitmap]::FromFile((AssetPath $slide.phoneSource))
  $oldClip = $g.Clip
  $g.SetClip($devicePath)
  $g.DrawImage($screen, (Rect $style.phoneScreen))
  $g.Clip = $oldClip
  $oldClip.Dispose()
  $screen.Dispose()

  $pen = New-Object System.Drawing.Pen (Color "#89949c"), 3
  $g.DrawPath($pen, $devicePath)
  $pen.Dispose()
  $devicePath.Dispose()

  SaveCanvas $bmp $g (AssetPath $slide.phoneOutput)
  Write-Output "Rendered $($slide.phoneOutput)"
}

function RenderTablet($slide, $style) {
  $ctx = NewCanvas $style.tabletCanvas
  $bmp = $ctx.Bitmap
  $g = $ctx.Graphics
  DrawBackground $g $style.tabletCanvas $style

  $copy = $style.tabletCopy
  DrawBrandPill $g $style $copy.x $copy.y
  $title = if ($slide.tabletTitle) { $slide.tabletTitle } else { $slide.title }
  DrawText $g $title $style.font $copy.titleSize ([System.Drawing.FontStyle]::Bold) (Color $style.text) $copy.x ($copy.y + 92) 545 190
  DrawText $g $slide.subtitle $style.font $copy.subtitleSize ([System.Drawing.FontStyle]::Regular) (Color $style.mutedText 220) $copy.x ($copy.y + 300) 545 115

  $device = [System.Drawing.Bitmap]::FromFile((AssetPath $slide.tabletSource))
  $destRect = Rect $style.tabletDevice
  $sourceRect = New-Object System.Drawing.Rectangle 0, 0, $device.Width, $device.Height

  $shadowRect = New-Object System.Drawing.RectangleF `
    ([float]($destRect.X - 12)), ([float]($destRect.Y + 14)), `
    ([float]($destRect.Width + 22)), ([float]($destRect.Height + 10))
  $shadowPath = RoundedPath $shadowRect $style.tabletDevice.radius
  $shadowBrush = New-Object System.Drawing.SolidBrush (Color "#000000" 70)
  $g.FillPath($shadowBrush, $shadowPath)
  $shadowBrush.Dispose()
  $shadowPath.Dispose()

  $clipPath = RoundedPath $destRect $style.tabletDevice.radius
  $oldClip = $g.Clip
  $g.SetClip($clipPath)
  $g.DrawImage($device, $destRect, $sourceRect, [System.Drawing.GraphicsUnit]::Pixel)
  $g.Clip = $oldClip
  $oldClip.Dispose()
  $clipPath.Dispose()
  $device.Dispose()

  SaveCanvas $bmp $g (AssetPath $slide.tabletOutput)
  Write-Output "Rendered $($slide.tabletOutput)"
}

foreach ($slide in $spec.slides.PSObject.Properties.Value) {
  RenderPhone $slide $spec.style
  RenderTablet $slide $spec.style
}
