# Planning

Planning op basis van de opgave. IDs verwijzen naar de vereisten in de opgave (A = account, K = klant, H = handelaar, G = algemeen). Werk dit document bij na elk wekelijks overleg.

## Taakverdeling

Voorstel, samen te bevestigen. Beide teamleden moeten alle code kunnen uitleggen, dus review elkaars werk grondig.

**Sergiu:** account en handelaar
- Firebase-project, Authentication en security rules
- Registreren, inloggen, uitloggen, rol en startscherm per rol (A1, A2, A3, A4)
- Mijn winkel: pin op de kaart, reverse geocoding met adres (H1, H2)
- Pakket aanbieden met foto als Base64 (H3, H4)
- Mijn pakketten met reservaties, ophaalcode controleren, pakket aanpassen (H5, H6, H7)

**Laurens:** klant
- Locatiepermissie en gekozen locatie, "Bags near {city}" (Engelse versie van "Pakketten in de buurt van {gemeente}") (K1, K2)
- Ontdekken-lijst, kaart met markers, realtime updates, filters (K3, K4, K5, K6)
- Pakketdetail, reserveren, ophaalcode (K7, K8, K9)
- Bestellingen offline via Room, annuleren (K10, K11)
- Favorieten in Room (K12)

**Samen:**
- Projectopzet: Compose, Navigation, MVVM-structuur, Room-database, Retrofit voor Nominatim
- Robuustheid: foutmeldingen, laadindicatoren, rotatie (G1, G2, G3)
- Eigen naam, huisstijl, app-icoon en dark mode (G4), zonder paars
- Demovideo en voorbereiding presentatie

## Fasen

| Week | Doel | Vereisten | Wie |
|---|---|---|---|
| 1 | Naam en huisstijl kiezen, Android-project opzetten, Firebase koppelen, navigatiestructuur met bottom navigation | Basis | Samen |
| 2 | Auth met rollen, Firestore-datamodel, security rules | A1, A2, A3 | Sergiu |
| 2 | Room-database met de drie entities, Nominatim via Retrofit met `CachedAddress` | Basis | Laurens |
| 3 | Mijn winkel met kaart en automatisch adres | H1, H2 | Sergiu |
| 3 | Locatiepermissie, gemeente bovenaan, Ontdekken-lijst | K1, K2, K3 | Laurens |
| 4 | Pakket aanbieden met foto, Mijn pakketten realtime | H3, H4, H5 | Sergiu |
| 4 | Kaartweergave met markers, realtime updates | K4, K5 | Laurens |
| 5 | Ophaalcode controleren, pakket aanpassen of offline halen, wachtwoord vergeten | H6, H7, A4 | Sergiu |
| 5 | Pakketdetail, reserveren, ophaalcode, Bestellingen via Room, Favorieten | K7, K8, K9, K10, K12 | Laurens |
| 6 | Annuleren, filters | K11, K6 | Laurens |
| 6 | Robuustheid: geen internet, geen locatie, lege lijsten, ongeldige invoer, rotatie, laadindicatoren | G1, G2, G3 | Samen |
| 7 | Huisstijl en dark mode afwerken, alle Must-vereisten testen, demovideo opnemen | G4 | Samen |

Must-vereisten gaan altijd voor Should-vereisten (A4, K6, K11, H6, H7, G4).

## Wekelijks overleg

Noteer na elk overleg met de docent wat af is en wat gepland is.

| Datum | Wie aanwezig | Af | Gepland volgende week |
|---|---|---|---|
| 28/10 | | Sergiu: structuur en documentatie. Laurens: UI en naam. Samen: planning. | |
