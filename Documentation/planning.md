# Planning

Planning op basis van de opgave. IDs verwijzen naar de vereisten in de opgave (A = account, K = klant, H = handelaar, G = algemeen). Werk dit document bij na elk wekelijks overleg en vink af wat klaar is.

## Taakverdeling

Voorstel, samen te bevestigen. Beide teamleden moeten alle code kunnen uitleggen, dus review elkaars werk grondig.

**Sergiu:** account en handelaar
- [x] Firebase-project, Authentication en security rules
- [x] Registreren, inloggen, uitloggen, rol en startscherm per rol (A1, A2, A3, A4)
- [x] Mijn winkel: pin op de kaart, reverse geocoding met adres (H1, H2)
- [ ] Pakket aanbieden met foto als Base64 (H3, H4)
- [ ] Mijn pakketten met reservaties, ophaalcode controleren, pakket aanpassen (H5, H6, H7)

**Laurens:** klant
- [x] Locatiepermissie en gekozen locatie, "Bags near {city}" (Engelse versie van "Pakketten in de buurt van {gemeente}") (K1, K2)
- [x] Ontdekken-lijst, kaart met markers, realtime updates, filters (K3, K4, K5, K6)
- [ ] Pakketdetail, reserveren, ophaalcode (K7, K8, K9)
- [ ] Bestellingen offline via Room, annuleren (K10, K11)
- [ ] Favorieten in Room (K12)

**Samen:**
- [x] Projectopzet: Compose, Navigation, MVVM-structuur
- [x] Room-database: `AppDatabase` (singleton) met `CachedAddress`
- [ ] Room-entities `FavoriteStore` (K12) en `CachedReservation` (K10) toevoegen aan `AppDatabase`
- [x] Retrofit voor Nominatim, met User-Agent, max. 1 aanvraag per seconde en cache in Room
- [ ] Robuustheid: foutmeldingen, laadindicatoren, rotatie (G1, G2, G3)
- [x] Eigen naam, huisstijl, app-icoon en dark mode (G4), zonder paars
- [ ] Demovideo en voorbereiding presentatie

## Fasen

| Week | Doel | Vereisten | Wie | Status |
|---|---|---|---|---|
| 1 | Naam en huisstijl kiezen, Android-project opzetten, Firebase koppelen, navigatiestructuur met bottom navigation | Basis | Samen | Klaar (28/10), tab Kaart (5/11) |
| 2 | Auth met rollen, Firestore-datamodel, security rules | A1, A2, A3 | Sergiu | Klaar (28/10), ook A4 |
| 2 | Room-database met de drie entities, Nominatim via Retrofit met `CachedAddress` | Basis | Laurens | Deels (4/11, Sergiu): `AppDatabase` met `CachedAddress` en Nominatim via Retrofit. Nog `FavoriteStore` en `CachedReservation`. |
| 3 | Mijn winkel met kaart en automatisch adres | H1, H2 | Sergiu | Klaar (4/11) |
| 3 | Locatiepermissie, gemeente bovenaan, Ontdekken-lijst | K1, K2, K3 | Laurens | Klaar (5/11) |
| 4 | Pakket aanbieden met foto, Mijn pakketten realtime | H3, H4, H5 | Sergiu | |
| 4 | Kaartweergave met markers, realtime updates | K4, K5 | Laurens | Klaar (5/11, vervroegd) |
| 5 | Ophaalcode controleren, pakket aanpassen of offline halen, wachtwoord vergeten | H6, H7, A4 | Sergiu | A4 klaar (28/10) |
| 5 | Pakketdetail, reserveren, ophaalcode, Bestellingen via Room, Favorieten | K7, K8, K9, K10, K12 | Laurens | |
| 6 | Annuleren, filters | K11, K6 | Laurens | K6 klaar (5/11). Nog K11. |
| 6 | Robuustheid: geen internet, geen locatie, lege lijsten, ongeldige invoer, rotatie, laadindicatoren | G1, G2, G3 | Samen | |
| 7 | Huisstijl en dark mode afwerken, alle Must-vereisten testen, demovideo opnemen | G4 | Samen | |

Must-vereisten gaan altijd voor Should-vereisten (A4, K6, K11, H6, H7, G4).

## Wekelijks overleg

Noteer na elk overleg met de docent wat af is en wat gepland is.

| Datum | Wie aanwezig | Af | Gepland volgende week |
|---|---|---|---|
| 28/10 | | Sergiu: structuur en documentatie, Firebase-project, Authentication, security rules, login en registratie (A1–A4). Laurens: UI en naam, Android-project, bottom navigation, thema. Samen: planning. | |
| 4/11 | | Sergiu: Mijn winkel met pin op de OSMDroid-kaart (tikken of verslepen) en automatisch adres via Nominatim (H1, H2), Room-database met adrescache, strengere security rules voor `stores`. | Sergiu: pakket aanbieden met foto (H3, H4). |

## Voortgang

**Sergiu**
- [x] Documentatie: werkafspraken, planning, voorwaarden om te slagen, README
- [x] Branding: naam NoWaste, logo en app-icoon (`UI/`)
- [x] Firebase-project `nowaste-d37be` met Authentication (e-mail/wachtwoord) en Cloud Firestore
- [x] Firestore security rules (`firestore.rules`): enkel ingelogde gebruikers, een gebruiker wijzigt enkel zijn eigen profiel
- [x] Registreren met naam, e-mail, wachtwoord en rol klant of handelaar (A1)
- [x] Inloggen, uitloggen en startscherm per rol (A2)
- [x] Foutmeldingen: verkeerd wachtwoord, e-mail al in gebruik, geen internet (A3)
- [x] Wachtwoord vergeten met reset-mail (A4)
- [x] Formulieren overleven rotatie en tonen een laadindicator (deel van G2, G3)

*4/11*
- [x] Mijn winkel: naam, categorie en locatie, opgeslagen in `stores/{uid}` (één winkel per handelaar) (H1)
- [x] Pin plaatsen met een tik op de OSMDroid-kaart of door hem te verslepen, met de levenscyclus van de `MapView` (H1)
- [x] Adres (straat + huisnummer, postcode, gemeente) automatisch ingevuld via Nominatim, nadien aanpasbaar (H2)
- [x] Nominatim: eigen User-Agent, max. 1 aanvraag per seconde, enkel na loslaten van de pin, cache in Room (`CachedAddress`)
- [x] Room: `AppDatabase` als singleton met `CachedAddress` en DAO
- [x] Security rules: enkel een handelaar kan zijn eigen winkel aanmaken of wijzigen
- [x] Geen internet, geen adres gevonden en ongeldige invoer geven een duidelijke melding; formulier overleeft rotatie (deel van G1, G2, G3)
- [x] Unit tests voor adres, cache en winkel (14 tests) en getest op de emulator

**Laurens**
- [x] Android-project opgezet: Kotlin, Jetpack Compose, type-safe Navigation Compose, package `be.ap.nowaste`
- [x] Bottom navigation voor de klant met de tabs Discover, Orders en Favorites (placeholderschermen)
- [x] Thema met het NoWaste-palet, light en dark mode, app-icoon

*5/11*
- [x] Register en login stoppen na 15 seconden met "geen internet" in plaats van eindeloos te laden (deel van A3, G1)
- [x] Locatiepermissie FINE + COARSE pas na uitleg en een tik op de knop; alle gevallen afgehandeld: precies, enkel benadering, geweigerd, definitief geweigerd (knop naar de app-instellingen) (K1)
- [x] Geweigerd, locatie uit of geen fix: de klant kiest een locatie met een pin op de kaart (`PinPickerMap`) (K1)
- [x] "Bags near {city}" bovenaan Ontdekken, via Nominatim met zoom 10 en cache in Room; locatie later wijzigen met "Change" (K2)
- [x] Huidige locatie via de ingebouwde `LocationManager` (`LocationRepository`), zonder extra dependency
- [x] Toestand overleeft rotatie, laadindicator tijdens zoeken van locatie en gemeente, melding en "Try again" zonder internet (deel van G1, G2, G3)
- [x] Getest op een toestel (OnePlus, Android 16)
- [x] Ontdekken-lijst: alle beschikbare pakketten (actief, minstens 1 over, ophaalvenster niet voorbij), gesorteerd op afstand, met foto, winkel, titel, prijs met doorstreepte originele waarde, ophaalvenster, aantal over en afstand in km (K3)
- [x] Tab Kaart: eigen locatie en een marker per winkel met beschikbare pakketten; tik op een marker voor winkelnaam en knop "View" met de pakketten van die winkel (K4)
- [x] Lijst en kaart realtime via `snapshots()`; verlopen pakketten verdwijnen ook zonder wijziging in Firestore (elke minuut opnieuw gefilterd) (K5)
- [x] Filters op maximale afstand (1, 5, 10 km) en categorie, voor lijst en kaart samen (K6)
- [x] `Bag`-model en `BagRepository` volgens het datamodel van de opgave; foto's gedecodeerd op `Dispatchers.Default` en onthouden per pakket
- [x] Testdata: `tools/seed-firestore.ps1` met 5 testwinkels en pakketten (zie README)
- [x] Unit tests voor beschikbaarheid, sortering, afstand en afstandsfilter (7 tests)
