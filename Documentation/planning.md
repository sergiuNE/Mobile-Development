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
- [ ] Locatiepermissie en gekozen locatie, "Bags near {city}" (Engelse versie van "Pakketten in de buurt van {gemeente}") (K1, K2)
- [ ] Ontdekken-lijst, kaart met markers, realtime updates, filters (K3, K4, K5, K6)
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
| 1 | Naam en huisstijl kiezen, Android-project opzetten, Firebase koppelen, navigatiestructuur met bottom navigation | Basis | Samen | Klaar (28/10), behalve tab Kaart |
| 2 | Auth met rollen, Firestore-datamodel, security rules | A1, A2, A3 | Sergiu | Klaar (28/10), ook A4 |
| 2 | Room-database met de drie entities, Nominatim via Retrofit met `CachedAddress` | Basis | Laurens | Deels (4/11, Sergiu): `AppDatabase` met `CachedAddress` en Nominatim via Retrofit. Nog `FavoriteStore` en `CachedReservation`. |
| 3 | Mijn winkel met kaart en automatisch adres | H1, H2 | Sergiu | Klaar (4/11) |
| 3 | Locatiepermissie, gemeente bovenaan, Ontdekken-lijst | K1, K2, K3 | Laurens | |
| 4 | Pakket aanbieden met foto, Mijn pakketten realtime | H3, H4, H5 | Sergiu | |
| 4 | Kaartweergave met markers, realtime updates | K4, K5 | Laurens | |
| 5 | Ophaalcode controleren, pakket aanpassen of offline halen, wachtwoord vergeten | H6, H7, A4 | Sergiu | A4 klaar (28/10) |
| 5 | Pakketdetail, reserveren, ophaalcode, Bestellingen via Room, Favorieten | K7, K8, K9, K10, K12 | Laurens | |
| 6 | Annuleren, filters | K11, K6 | Laurens | |
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

**Nog open uit week 1**
- [ ] Tab Kaart in de bottom navigation (K4)
