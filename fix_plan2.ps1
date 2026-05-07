$file = 'g:\abc\Fitty\app\src\main\java\com\example\fitty\feature_plan\PlanScreen.kt'
$c = Get-Content $file -Raw

# Fix cards with trailing comma + empty line + closing paren
$c = $c -replace "colors = CardDefaults\.cardColors\(containerColor = MaterialTheme\.colorScheme\.surface\),\s*\r?\n\s*\)", "colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp))"

# Also fix the gradient import
$c = $c -replace 'FittyPink, com\.example\.fitty\.ui\.theme\.FittyGradientEnd', 'FittyPink, FittyGradientEnd'

# Add missing import for FittyGradientEnd if not present
if ($c -notmatch 'import com\.example\.fitty\.ui\.theme\.FittyGradientEnd') {
    $c = $c -replace 'import com\.example\.fitty\.ui\.theme\.FittyPink', "import com.example.fitty.ui.theme.FittyPink`nimport com.example.fitty.ui.theme.FittyGradientEnd"
}

Set-Content $file $c -NoNewline
Write-Host "Done"
