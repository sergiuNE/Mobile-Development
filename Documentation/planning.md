# Planning

Planning op basis van de opgave. IDs verwijzen naar de vereisten in de opgave (A = account, K = klant, H = handelaar, G = algemeen). Werk dit document bij na elk wekelijks overleg en vink af wat klaar is.

## Taakverdeling

Voorstel, samen te bevestigen. Beide teamleden moeten alle code kunnen uitleggen, dus review elkaars werk grondig.

**Sergiu:** account en handelaar
- [x] Firebase-project, Authentication en security rules
- [x] Registreren, inloggen, uitloggen, rol en startscherm per rol (A1, A2, A3, A4)
- [x] Mijn winkel: pin op de kaart, reverse geocoding met adres (H1, H2)
- [x] Pakket aanbieden met foto als Base64 (H3, H4)
- [x] Mijn pakketten met reservaties, ophaalcode controleren, pakket aanpassen (H5, H6, H7)
- [x] Feedback lector: eigen markers op de kaart, slider voor de afstand in km

**Laurens:** klant
- [x] Locatiepermissie en gekozen locatie, "Bags near {city}" (Engelse versie van "Pakketten in de buurt van {gemeente}") (K1, K2)
- [x] Ontdekken-lijst, kaart met markers, realtime updates, filters (K3, K4, K5, K6)
- [x] Pakketdetail, reserveren, ophaalcode (K7, K8, K9)
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
| 4 | Pakket aanbieden met foto, Mijn pakketten realtime | H3, H4, H5 | Sergiu | Klaar (18/11) |
| 4 | Kaartweergave met markers, realtime updates | K4, K5 | Laurens | Klaar (5/11, vervroegd) |
| 5 | Ophaalcode controleren, pakket aanpassen of offline halen, wachtwoord vergeten | H6, H7, A4 | Sergiu | Klaar: A4 (28/10), H6 en H7 (18/11, vervroegd) |
| 5 | Pakketdetail, reserveren, ophaalcode, Bestellingen via Room, Favorieten | K7, K8, K9, K10, K12 | Laurens | K7, K8, K9 klaar (23/11). Nog K10, K12. |
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
| 18/11 | Sergiu en Laurens (samen besproken wie wat doet) | Feedback lector: team 3; standaardmarkers op de kaart vervangen; slider voor de afstand in km. Sergiu: pakket aanbieden met foto als Base64 (H3, H4), Mijn pakketten met reservaties realtime (H5), ophaalcode controleren en als opgehaald markeren (H6), pakket aanpassen en offline halen (H7), feedback lector verwerkt, security rules voor `bags` en `reservations`. Laurens: testdata uitgebreid naar 10 winkels en een demo-klant. | Sergiu: nieuwe security rules publiceren en testen met een klant- en handelaarsaccount; Laurens helpen met reserveren (K8: `quantityLeft` verlagen en reservatie aanmaken in één transactie, sluit aan op H5 en H6); robuustheid nalopen op alle handelaarsschermen: geen internet, lege lijsten, ongeldige invoer, rotatie (G1, G2, G3); dark mode nakijken (G4). |

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

*18/11*
- [x] Handelaar heeft twee tabs: "My bags" en "My store"
- [x] Pakket aanbieden: titel, beschrijving, prijs, originele waarde, aantal en ophaalvenster van vandaag, met validatie per veld (H3)
- [x] Foto met de camera (`TakePicturePreview`) of uit de galerij (`PickVisualMedia`), verkleind tot max. 800 px, JPEG ± 70, als Base64-tekst in `photoBase64`; alles op `Dispatchers.Default` (H4)
- [x] Mijn pakketten: eigen pakketten met aantal over en aantal gereserveerd, realtime via `snapshots()` (H5)
- [x] Ophaalcode controleren (hoofdletters en spaties maken niet uit) en reservatie als opgehaald markeren; een gebruikte code werkt niet meer (H6)
- [x] Pakket aanpassen in een transactie: gereserveerde pakketten blijven gereserveerd, lager dan gereserveerd kan niet (H7)
- [x] Pakket offline halen en terug online zetten (H7)
- [x] `Reservation`-model en `ReservationRepository` (ook bruikbaar voor K8–K11)
- [x] Security rules: een handelaar beheert enkel de pakketten van zijn eigen winkel, een klant mag enkel `quantityLeft` wijzigen; een reservatie is enkel zichtbaar voor de klant en de winkel
- [x] Feedback lector: eigen pin in de huisstijl in plaats van de standaardmarker van OSMDroid (kaart van de klant en "My store")
- [x] Feedback lector: afstandsfilter is een slider van 1 tot 20 km, met "Any distance" helemaal rechts; de slider vangt het terug-gebaar van Android niet meer op
- [x] Geen internet, ongeldige invoer en een onleesbare foto geven een duidelijke melding; formulier en foto overleven rotatie (deel van G1, G2, G3)
- [x] Unit tests voor prijs, ophaalvenster, foto verkleinen, aantal na aanpassen en ophaalcodes (12 nieuwe tests, 33 in totaal) en getest op de emulator

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

*18/11*
- [x] Testdata uitgebreid: 10 testwinkels rond AP Hogeschool, een demo-klantaccount en langere ophaalvensters (`tools/seed-firestore.ps1`)

*23/11*
- [x] Pakketdetail: foto, winkel, adres (uit `stores`), beschrijving, prijs met doorstreepte originele waarde, ophaalvenster, aantal over en een kleine kaart met de winkel; te openen vanuit de lijst en vanuit de kaart. Aantal over wordt realtime bijgewerkt (K7)
- [x] Reserveren: aantal kiezen van 1 tot wat er over is; `quantityLeft` verlagen en de reservatie aanmaken in één transactie, zodat twee klanten nooit hetzelfde laatste pakket krijgen (K8)
- [x] Ophaalcode van 6 tekens zonder 0/O/1/I, getoond na het reserveren; werkt met de controle van de handelaar (H6) (K9)
- [x] Een reservatie bewaart ook `bagTitle`, `storeName`, `pickupStart` en `pickupEnd`, zodat Bestellingen (K10) ze in Room kan zetten zonder het pakket op te vragen
- [x] Geen internet, trage verbinding en "iemand anders was sneller" geven een duidelijke melding; laadindicator tijdens reserveren (deel van G1, G3)
- [x] Unit tests voor reserveren en ophaalcodes (4 nieuwe tests, 37 in totaal)
