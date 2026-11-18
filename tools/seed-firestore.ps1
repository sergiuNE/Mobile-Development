<#
.SYNOPSIS
    Fills Firestore with test merchants, stores and bags, so the customer screens have data.

.DESCRIPTION
    Creates (or reuses) one merchant account per test store (demo-<key>@nowaste.test) and a customer account (demo-customer@nowaste.test) in Firebase Authentication, then writes
    users/{uid}, stores/{uid} and a few bags/{bagId} through the Firestore REST API. Every write is done
    as that merchant, so the security rules apply exactly as in the app.

    Pickup windows are relative to the moment you run the script: run it again on another day to get
    fresh bags. Running it twice is safe, the same documents are overwritten.

    Besides the available bags it adds three that must NOT show up for customers (sold out, pickup
    window over, inactive), to test the filters of K3.

.PARAMETER Password
    Password for all demo accounts (at least 6 characters). Optional: by default seed.password from local.properties, which is not committed.

.PARAMETER Latitude
    Centre of the test stores. Default: AP Hogeschool, Antwerpen.

.PARAMETER Longitude
    Centre of the test stores.

.EXAMPLE
    .\tools\seed-firestore.ps1 -Password "kies-een-wachtwoord"

.EXAMPLE
    .\tools\seed-firestore.ps1 -Latitude 51.0543 -Longitude 3.7174
#>
param(
    [string] $Password,
    [double] $Latitude = 51.2172,
    [double] $Longitude = 4.4127
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# Without -Password: use seed.password from local.properties (not committed), or make one and save it there
$localProperties = Join-Path $PSScriptRoot '..\local.properties'
if (-not $Password) {
    $line = Get-Content $localProperties -ErrorAction SilentlyContinue | Where-Object { $_ -like 'seed.password=*' }
    if ($line) {
        $Password = $line.Substring('seed.password='.Length)
    } else {
        $Password = -join ((48..57) + (97..122) | Get-Random -Count 16 | ForEach-Object { [char] $_ })
        Add-Content $localProperties "seed.password=$Password"
        Write-Host "New password for the demo accounts saved as seed.password in local.properties"
    }
}
if ($Password.Length -lt 6) { throw 'The password needs at least 6 characters.' }

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
    # ErrorDetails is empty in some PowerShell hosts: then read the body from the response itself
    $message = $ErrorRecord.ErrorDetails.Message
    if (-not $message) {
        try { $message = (New-Object IO.StreamReader $ErrorRecord.Exception.Response.GetResponseStream()).ReadToEnd() } catch {}
    }
    try { ($message | ConvertFrom-Json).error.message } catch { '' }
}

# Sign up the account, or sign in when it already exists from an earlier run
function Get-Account($Email) {
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
# Offsets in degrees from the centre: 0.01 latitude is about 1.1 km, 0.01 longitude about 0.7 km here.
# Start and End are hours from now: most bags stay available for about 8 hours, enough for a demo later that day.
$stores = @(
    @{
        Key = 'bakery'; Name = 'Bakkerij De Korenaar'; Category = 'bakery'; DLat = 0.003; DLon = 0.004
        Street = 'Lange Nieuwstraat 12'; Postcode = '2000'; City = 'Antwerpen'; Color = '#B7791F'
        Bags = @(
            @{ Title = 'Bread and pastries'; Description = 'A mix of bread, croissants and a sweet surprise from today.'; Price = 3.99; Original = 12.00; Total = 5; Left = 3; Start = 0; End = 8 },
            @{ Title = 'Sweet treats'; Description = 'Cakes and pastries that did not sell today.'; Price = 4.50; Original = 14.00; Total = 3; Left = 1; Start = 1; End = 9 }
        )
    },
    @{
        Key = 'supermarket'; Name = 'Buurtsuper Zuid'; Category = 'supermarket'; DLat = -0.012; DLon = -0.008
        Street = 'Brederodestraat 80'; Postcode = '2018'; City = 'Antwerpen'; Color = '#2F855A'
        Bags = @(
            @{ Title = 'Groceries bag'; Description = 'Fruit, vegetables and dairy close to their best-before date.'; Price = 5.99; Original = 18.00; Total = 8; Left = 6; Start = 0; End = 8 },
            @{ Title = 'Sold out bag (hidden)'; Description = 'Test: quantityLeft is 0, must not be shown.'; Price = 4.99; Original = 15.00; Total = 4; Left = 0; Start = 0; End = 8 }
        )
    },
    @{
        Key = 'restaurant'; Name = 'Trattoria Sole'; Category = 'restaurant'; DLat = 0.006; DLon = -0.010
        Street = 'Hoogstraat 33'; Postcode = '2000'; City = 'Antwerpen'; Color = '#C53030'
        Bags = @(
            @{ Title = 'Pasta dinner'; Description = 'Two portions of fresh pasta with sauce of the day.'; Price = 6.50; Original = 22.00; Total = 4; Left = 2; Start = 2; End = 10 },
            @{ Title = 'Lunch leftovers (hidden)'; Description = 'Test: pickup window is over, must not be shown.'; Price = 4.00; Original = 12.00; Total = 3; Left = 3; Start = -3; End = -1 }
        )
    },
    @{
        Key = 'cafe'; Name = 'Koffiebar Bonen'; Category = 'cafe'; DLat = -0.004; DLon = 0.012
        Street = 'Kammenstraat 5'; Postcode = '2000'; City = 'Antwerpen'; Color = '#6B4423'
        Bags = @(
            @{ Title = 'Sandwiches and cake'; Description = 'Sandwiches from lunch and a slice of cake.'; Price = 3.50; Original = 10.50; Total = 6; Left = 4; Start = 0; End = 8 }
        )
    },
    @{
        Key = 'other'; Name = 'Groentewinkel Vers'; Category = 'other'; DLat = 0.045; DLon = 0.030
        Street = 'Bredabaan 300'; Postcode = '2170'; City = 'Merksem'; Color = '#38A169'
        Bags = @(
            @{ Title = 'Vegetable box'; Description = 'Seasonal vegetables, a bit too ugly for the shelf.'; Price = 4.00; Original = 13.00; Total = 5; Left = 5; Start = 1; End = 9 },
            @{ Title = 'Inactive bag (hidden)'; Description = 'Test: active is false, must not be shown.'; Price = 3.00; Original = 9.00; Total = 2; Left = 2; Start = 1; End = 9; Inactive = $true }
        )
    },
    @{
        Key = 'bakery2'; Name = 'Broodhuis Zurenborg'; Category = 'bakery'; DLat = -0.008; DLon = 0.020
        Street = 'Dageraadplaats 7'; Postcode = '2018'; City = 'Antwerpen'; Color = '#D69E2E'
        Bags = @(
            @{ Title = 'Sourdough bag'; Description = 'Two sourdough loaves and some rolls.'; Price = 3.00; Original = 9.50; Total = 4; Left = 4; Start = 0; End = 7 }
        )
    },
    @{
        Key = 'supermarket2'; Name = 'Versmarkt Noord'; Category = 'supermarket'; DLat = 0.020; DLon = 0.006
        Street = 'Italielei 120'; Postcode = '2000'; City = 'Antwerpen'; Color = '#276749'
        Bags = @(
            @{ Title = 'Dairy and cheese'; Description = 'Yoghurt, milk and cheese with a short best-before date.'; Price = 4.99; Original = 16.00; Total = 5; Left = 2; Start = 1; End = 8 },
            @{ Title = 'Fruit basket'; Description = 'Apples, bananas and whatever else is ripe today.'; Price = 3.49; Original = 11.00; Total = 6; Left = 6; Start = 0; End = 8 }
        )
    },
    @{
        Key = 'restaurant2'; Name = 'Sushi Kawa'; Category = 'restaurant'; DLat = 0.010; DLon = -0.002
        Street = 'Vlasmarkt 18'; Postcode = '2000'; City = 'Antwerpen'; Color = '#2B6CB0'
        Bags = @(
            @{ Title = 'Sushi box'; Description = 'Assorted sushi made today, about 16 pieces.'; Price = 7.00; Original = 24.00; Total = 3; Left = 3; Start = 3; End = 10 }
        )
    },
    @{
        Key = 'cafe2'; Name = 'Tearoom Het Park'; Category = 'cafe'; DLat = -0.020; DLon = 0.025
        Street = 'Lange Lozanastraat 40'; Postcode = '2018'; City = 'Antwerpen'; Color = '#975A16'
        Bags = @(
            @{ Title = 'Afternoon tea bag'; Description = 'Scones, cookies and a piece of pie.'; Price = 4.00; Original = 12.50; Total = 4; Left = 1; Start = 1; End = 8 }
        )
    },
    @{
        Key = 'other2'; Name = 'Bloemen en Planten Linde'; Category = 'other'; DLat = -0.070; DLon = -0.060
        Street = 'Boomsesteenweg 500'; Postcode = '2610'; City = 'Wilrijk'; Color = '#B83280'
        Bags = @(
            @{ Title = 'Flower surprise'; Description = 'A bunch of flowers that are a day past their best look.'; Price = 5.00; Original = 15.00; Total = 3; Left = 3; Start = 0; End = 9 }
        )
    }
)

foreach ($store in $stores) {
    $email = "demo-$($store.Key)@nowaste.test"
    $merchant = Get-Account $email
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

# A customer account to log in with during the demo
$customerEmail = 'demo-customer@nowaste.test'
$customer = Get-Account $customerEmail
Set-Document "users/$($customer.localId)" @{ name = Str 'Demo Customer'; email = Str $customerEmail; role = Str 'customer' } $customer.idToken
Write-Host "Customer: $customerEmail"

Write-Host "Done. Seeded $($stores.Count) stores around $Latitude, $Longitude. Password: seed.password in local.properties (or the -Password you gave)."
