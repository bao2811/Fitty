$file = 'g:\abc\Fitty\app\src\main\java\com\example\fitty\feature_plan\PlanScreen.kt'
$c = Get-Content $file -Raw

# Add import for FittyPink
$c = $c -replace 'import com\.example\.fitty\.core\.ui\.FittyLazyScreen', "import com.example.fitty.core.ui.FittyLazyScreen`nimport com.example.fitty.ui.theme.FittyPink"

# Replace secondary tint with FittyPink
$c = $c -replace 'tint = MaterialTheme\.colorScheme\.secondary', 'tint = FittyPink'

# Replace primary color text with FittyPink
$c = $c -replace 'color = MaterialTheme\.colorScheme\.primary\b', 'color = FittyPink'

# Add elevation to cards that don't have it
$c = $c -replace 'colors = CardDefaults\.cardColors\(containerColor = MaterialTheme\.colorScheme\.surface\)\s*\)', 'colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp))'

# Fix gradient colors
$c = $c -replace 'MaterialTheme\.colorScheme\.primary,\s*MaterialTheme\.colorScheme\.secondary', 'FittyPink, com.example.fitty.ui.theme.FittyGradientEnd'

Set-Content $file $c -NoNewline
Write-Host "Done"
