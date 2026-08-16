param(
    [string]$OutputRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\hooked\textures\hooks')
)

Add-Type -AssemblyName System.Drawing

# These atlases are used only by the fired hook-head model. Item icons live in
# textures/items and are intentionally outside this script's output tree.
$palettes = [ordered]@{
    wood = @('#21160E', '#4D3016', '#855226', '#B97B38', '#E1AA58')
    iron = @('#1D2229', '#4A525D', '#858E99', '#C9CED3', '#F8FCFF')
    diamond = @('#063744', '#08758A', '#10BFD0', '#55FFFF', '#D9FFFF')
    red = @('#3A0808', '#760D0D', '#C81717', '#FF3030', '#FFB4A8')
    ender = @('#140927', '#2C1450', '#5B208D', '#9D4EDD', '#E2B6FF')
}

$columnShade = @(1, 4, 3, 2, 1, 3, 4, 2, 1, 4, 3, 2, 1, 3, 4, 2)

foreach ($entry in $palettes.GetEnumerator()) {
    $colors = @($entry.Value | ForEach-Object { [System.Drawing.ColorTranslator]::FromHtml($_) })
    $bitmap = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt 16; $y++) {
            for ($x = 0; $x -lt 16; $x++) {
                $shade = $columnShade[$x]
                if (($y % 8) -eq 0) {
                    $shade = [Math]::Min(4, $shade + 1)
                } elseif (($y % 8) -eq 7) {
                    $shade = [Math]::Max(0, $shade - 1)
                }
                if ((($x * 3 + $y * 5) % 17) -eq 0) {
                    $shade = [Math]::Min(4, $shade + 1)
                }
                $bitmap.SetPixel($x, $y, $colors[$shade])
            }
        }

        $output = Join-Path (Join-Path $OutputRoot $entry.Key) 'hook.png'
        $bitmap.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}
