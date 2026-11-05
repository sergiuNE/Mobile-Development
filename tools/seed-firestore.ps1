<#
.SYNOPSIS
    Fills Firestore with test merchants, stores and bags, so the customer screens have data.

.DESCRIPTION
    Creates (or reuses) one merchant account per test store in Firebase Authentication, then writes
    users/{uid}, stores/{uid} and a few bags/{bagId} through the Firestore REST API. Every write is done
    as that merchant, so the security rules apply exactly as in the app.

    Pickup windows are relative to the moment you run the script: run it again on another day to get
    fresh bags. Running it twice is safe, the same documents are overwritten.

    Besides the available bags it adds three that must NOT show up for customers (sold out, pickup
    window over, inactive), to test the filters of K3.

.PARAMETER Password
    Password for all test merchant accounts (at least 6 characters). Not stored in the repository.

.PARAMETER Latitude
    Centre of the test stores. Default: AP Hogeschool, Antwerpen.

.PARAMETER Longitude
    Centre of the test stores.

.EXAMPLE
    .\tools\seed-firestore.ps1 -Password "kies-een-wachtwoord"

.EXAMPLE
    .\tools\seed-firestore.ps1 -Password "kies-een-wachtwoord" -Latitude 51.0543 -Longitude 3.7174
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidateLength(6, 128)]
    [string] $Password,
    [double] $Latitude = 51.2172,
    [double] $Longitude = 4.4127
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$config = Get-Content (Join-Path $PSScriptRoot '..\app\google-services.json') -Raw | ConvertFrom-Json
$apiKey = $config.client[0].api_key[0].current_key
$projectId = $config.project_info.project_id
$documentsUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"
$authUrl = 'https://identitytoolkit.googleapis.com/v1/accounts'

# --- Firebase REST helpers ---------------------------------------------------------------------

function Invoke-Json($Method, $Uri, $Body, $Token) {
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    # Send UTF-8 bytes: Windows PowerShell would otherwise mangle characters like the é in Café
    $bytes = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 10))
    Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers -Body $bytes -ContentType 'application/json; charset=utf-8'
}

function Get-ErrorCode($ErrorRecord) {
    try { ($ErrorRecord.ErrorDetails.Message | ConvertFrom-Json).error.message } catch { '' }
}

# Sign up the merchant, or sign in when the account already exists from an earlier run
function Get-Merchant($Email) {
    $body = @{ email = $Email; password = $Password; returnSecureToken = $true }
    try {
        return Invoke-Json Post "$authUrl`:signUp?key=$apiKey" $body
    } catch {
        if ((Get-ErrorCode $_) -ne 'EMAIL_EXISTS') { throw }
        try {
            return Invoke-Json Post "$authUrl`:signInWithPassword?key=$apiKey" $body
        } catch {
            throw "Could not sign in as $Email. Was it created earlier with another password? ($(Get-ErrorCode $_))"
        }
    }
}

function Set-Document($Path, $Fields, $Token) {
    Invoke-Json Patch "$documentsUrl/$Path" @{ fields = $Fields } $Token | Out-Null
}

# Firestore REST values
function Str($Value) { @{ stringValue = $Value } }
function Int($Value) { @{ integerValue = "$Value" } }
function Dbl($Value) { @{ doubleValue = [double] $Value } }
function Bool($Value) { @{ booleanValue = $Value } }
function Time([datetime] $Value) { @{ timestampValue = $Value.ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ') } }
function Geo($Lat, $Lon) { @{ geoPointValue = @{ latitude = $Lat; longitude = $Lon } } }

# A simple coloured JPEG with the title, scaled and compressed like the app does (max. 800 px, quality 70)
function New-PhotoBase64($Title, $Color) {
    $bitmap = New-Object Drawing.Bitmap 800, 600
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([Drawing.ColorTranslator]::FromHtml($Color))
    $graphics.TextRenderingHint = 'AntiAlias'
    $font = New-Object Drawing.Font 'Segoe UI', 56, ([Drawing.FontStyle]::Bold)
    $format = New-Object Drawing.StringFormat
    $format.Alignment = 'Center'
    $format.LineAlignment = 'Center'
    $graphics.DrawString($Title, $font, [Drawing.Brushes]::White, (New-Object Drawing.RectangleF 40, 40, 720, 520), $format)

    $encoder = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -eq 'image/jpeg'
    $parameters = New-Object Drawing.Imaging.EncoderParameters 1
    $parameters.Param[0] = New-Object Drawing.Imaging.EncoderParameter ([Drawing.Imaging.Encoder]::Quality), 70L
    $stream = New-Object IO.MemoryStream
    $bitmap.Save($stream, $encoder, $parameters)
    $graphics.Dispose(); $bitmap.Dispose()
    [Convert]::ToBase64String($stream.ToArray())
}

# --- Test data ---------------------------------------------------------------------------------

$now = Get-Date
# Offsets in degrees from the centre: 0.01 latitude is about 1.1 km, 0.01 longitude about 0.7 km here
$stores = @(
    @{
        Key = 'bakery'; Name = 'Bakkerij De Korenaar'; Category = 'bakery'; DLat = 0.003; DLon = 0.004
        Street = 'Lange Nieuwstraat 12'; Postcode = '2000'; City = 'Antwerpen'; Color = '#B7791F'
        Bags = @(
            @{ Title = 'Bread and pastries'; Description = 'A mix of bread, croissants and a sweet surprise from today.'; Price = 3.99; Original = 12.00; Total = 5; Left = 3; Start = 1; End = 2 },
            @{ Title = 'Sweet treats'; Description = 'Cakes and pastries that did not sell today.'; Price = 4.50; Original = 14.00; Total = 3; Left = 1; Start = 2; End = 3 }
        )
    },
    @{
        Key = 'supermarket'; Name = 'Buurtsuper Zuid'; Category = 'supermarket'; DLat = -0.012; DLon = -0.008
        Street = 'Brederodestraat 80'; Postcode = '2018'; City = 'Antwerpen'; Color = '#2F855A'
        Bags = @(
            @{ Title = 'Groceries bag'; Description = 'Fruit, vegetables and dairy close to their best-before date.'; Price = 5.99; Original = 18.00; Total = 8; Left = 6; Start = 0; End = 3 },
            @{ Title = 'Sold out bag (hidden)'; Description = 'Test: quantityLeft is 0, must not be shown.'; Price = 4.99; Original = 15.00; Total = 4; Left = 0; Start = 0; End = 3 }
        )
    },
    @{
        Key = 'restaurant'; Name = 'Trattoria Sole'; Category = 'restaurant'; DLat = 0.006; DLon = -0.010
        Street = 'Hoogstraat 33'; Postcode = '2000'; City = 'Antwerpen'; Color = '#C53030'
        Bags = @(
            @{ Title = 'Pasta dinner'; Description = 'Two portions of fresh pasta with sauce of the day.'; Price = 6.50; Original = 22.00; Total = 4; Left = 2; Start = 3; End = 4 },
            @{ Title = 'Lunch leftovers (hidden)'; Description = 'Test: pickup window is over, must not be shown.'; Price = 4.00; Original = 12.00; Total = 3; Left = 3; Start = -3; End = -1 }
        )
    },
    @{
        Key = 'cafe'; Name = 'Koffiebar Bonen'; Category = 'cafe'; DLat = -0.004; DLon = 0.012
        Street = 'Kammenstraat 5'; Postcode = '2000'; City = 'Antwerpen'; Color = '#6B4423'
        Bags = @(
            @{ Title = 'Sandwiches and cake'; Description = 'Sandwiches from lunch and a slice of cake.'; Price = 3.50; Original = 10.50; Total = 6; Left = 4; Start = 1; End = 2 }
        )
    },
    @{
        Key = 'other'; Name = 'Groentewinkel Vers'; Category = 'other'; DLat = 0.045; DLon = 0.030
        Street = 'Bredabaan 300'; Postcode = '2170'; City = 'Merksem'; Color = '#38A169'
        Bags = @(
            @{ Title = 'Vegetable box'; Description = 'Seasonal vegetables, a bit too ugly for the shelf.'; Price = 4.00; Original = 13.00; Total = 5; Left = 5; Start = 1; End = 5 },
            @{ Title = 'Inactive bag (hidden)'; Description = 'Test: active is false, must not be shown.'; Price = 3.00; Original = 9.00; Total = 2; Left = 2; Start = 1; End = 5; Inactive = $true }
        )
    }
)

foreach ($store in $stores) {
    $email = "seed-$($store.Key)@nowaste.test"
    $merchant = Get-Merchant $email
    $uid = $merchant.localId
    $token = $merchant.idToken
    $lat = $Latitude + $store.DLat
    $lon = $Longitude + $store.DLon

    # The role must exist before the store: the rules check it
    Set-Document "users/$uid" @{ name = Str $store.Name; email = Str $email; role = Str 'merchant' } $token
    Set-Document "stores/$uid" @{
        ownerId = Str $uid; name = Str $store.Name; category = Str $store.Category; location = Geo $lat $lon
        address = Str "$($store.Street), $($store.Postcode) $($store.City)"; city = Str $store.City
        street = Str $store.Street; postcode = Str $store.Postcode
    } $token

    $index = 1
    foreach ($bag in $store.Bags) {
        $bagId = "seed-$($store.Key)-$index"
        Set-Document "bags/$bagId" @{
            storeId = Str $uid; storeName = Str $store.Name; title = Str $bag.Title; description = Str $bag.Description
            price = Dbl $bag.Price; originalValue = Dbl $bag.Original
            quantityTotal = Int $bag.Total; quantityLeft = Int $bag.Left
            pickupStart = Time $now.AddHours($bag.Start); pickupEnd = Time $now.AddHours($bag.End)
            location = Geo $lat $lon; active = Bool (-not $bag.Inactive)
            photoBase64 = Str (New-PhotoBase64 $bag.Title $store.Color)
        } $token
        $index++
    }
    Write-Host "$($store.Name): $($store.Bags.Count) bags ($email)"
}

Write-Host "Done. Seeded $($stores.Count) stores around $Latitude, $Longitude."
