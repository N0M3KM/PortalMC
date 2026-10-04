# Original 16x16 diagnostic token. No source game assets are read.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$texturePath = Join-Path $PSScriptRoot '../src/client/resources/assets/portalmod/textures/item/hello_world.png'
New-Item -ItemType Directory -Force -Path (Split-Path $texturePath) | Out-Null
$bitmap = [System.Drawing.Bitmap]::new(16, 16)
try {
    for ($y = 1; $y -lt 15; $y++) {
        for ($x = 1; $x -lt 15; $x++) {
            $color = [System.Drawing.Color]::FromArgb(255, 35, 42, 52)
            if ($x -eq 1 -or $x -eq 14 -or $y -eq 1 -or $y -eq 14) {
                $color = [System.Drawing.Color]::FromArgb(255, 210, 219, 225)
            }
            if (($x -eq 4 -or $x -eq 6) -and $y -ge 4 -and $y -le 11 -or
                $x -eq 5 -and ($y -eq 3 -or $y -eq 12)) {
                $color = [System.Drawing.Color]::FromArgb(255, 48, 159, 255)
            }
            if (($x -eq 9 -or $x -eq 11) -and $y -ge 4 -and $y -le 11 -or
                $x -eq 10 -and ($y -eq 3 -or $y -eq 12)) {
                $color = [System.Drawing.Color]::FromArgb(255, 255, 145, 45)
            }
            $bitmap.SetPixel($x, $y, $color)
        }
    }
    $bitmap.Save([System.IO.Path]::GetFullPath($texturePath), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $bitmap.Dispose()
}

# Original, deliberately simple silhouettes for Phase 1 equipment.
$palette = @{
    'W' = [System.Drawing.Color]::FromArgb(255, 222, 232, 238)
    'D' = [System.Drawing.Color]::FromArgb(255, 38, 48, 60)
    'B' = [System.Drawing.Color]::FromArgb(255, 48, 159, 255)
    'O' = [System.Drawing.Color]::FromArgb(255, 255, 145, 45)
}
$sprites = @{
    'portal_gun' = @(
        '................', '................', '................', '..........DDD...',
        '...DDWWWWWDBD...', '..DWWWWWWWWBD...', '..DWWWWWWWWDD...', '...DDWWWWWDOD...',
        '.....DDDDDODD...', '.....DDD........', '....DDD.........', '....DD..........',
        '................', '................', '................', '................')
    'long_fall_boots' = @(
        '................', '................', '...WW....WW.....', '...WW....WW.....',
        '...DW....DW.....', '...DW....DW.....', '...DW....DW.....', '...DW....DW.....',
        '...DW....DW.....', '...DW....DW.....', '..DDWW..DDWW....', '..DWWW..DWWW....',
        '..DDDD..DDDD....', '................', '................', '................')
}
foreach ($spriteName in $sprites.Keys) {
    $sprite = [System.Drawing.Bitmap]::new(16, 16)
    try {
        for ($y = 0; $y -lt 16; $y++) {
            for ($x = 0; $x -lt 16; $x++) {
                $symbol = [string]$sprites[$spriteName][$y][$x]
                if ($palette.ContainsKey($symbol)) { $sprite.SetPixel($x, $y, $palette[$symbol]) }
            }
        }
        $spritePath = Join-Path (Split-Path $texturePath) "$spriteName.png"
        $sprite.Save([System.IO.Path]::GetFullPath($spritePath), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $sprite.Dispose() }
}
