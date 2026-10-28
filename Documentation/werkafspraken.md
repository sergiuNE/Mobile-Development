# Werkafspraken

Hoe we als team van 2 personen (Sergiu en Laurens) aan het Mobile Development-project werken. Lees deze afspraken voordat je aan een taak begint.

Het project is een eigen versie van een app voor verrassingspakketten met voedseloverschotten (zie de opgave). Gebruik **niet** de naam, het logo of de huisstijl van Too Good To Go.

## Technologie

| Onderdeel         | Keuze                                                                                               |
| ----------------- | --------------------------------------------------------------------------------------------------- |
| Platform          | Android, Kotlin                                                                                     |
| UI                | Jetpack Compose, Navigation Compose                                                                 |
| Architectuur      | MVVM: elk scherm heeft een ViewModel met een `StateFlow<…UiState>`                                  |
| Asynchroon werk   | Coroutines in `viewModelScope`, `await()` uit `kotlinx-coroutines-play-services` (geen callbacks)   |
| Backend           | Firebase Authentication (e-mail/wachtwoord) en Cloud Firestore (Spark-plan). Geen Firebase Storage. |
| Lokale opslag     | Room                                                                                                |
| Kaart             | OSMDroid, vertrekkend van `OSM_starter`                                                             |
| Reverse geocoding | Nominatim `/reverse` via Retrofit                                                                   |

## Commando's

Eenmalig: download `google-services.json` uit de Firebase-console (Projectinstellingen › Jouw apps › NoWaste) en zet het in `app/`. Het bestand staat in `.gitignore` en wordt niet gecommit. Zonder dit bestand bouwt de app niet.

Voer deze commando's uit in de projectmap (op Windows `gradlew.bat` in plaats van `./gradlew`):

| Doel                                | Commando                         |
| ----------------------------------- | -------------------------------- |
| App bouwen (debug)                  | `./gradlew assembleDebug`        |
| App installeren op toestel/emulator | `./gradlew installDebug`         |
| Unit tests                          | `./gradlew test`                 |
| Instrumented tests                  | `./gradlew connectedAndroidTest` |
| Lint                                | `./gradlew lint`                 |

## Taal

| Wat                                   | Taal                                                                        |
| ------------------------------------- | --------------------------------------------------------------------------- |
| Code, variabelen, klassen en methoden | Engels                                                                      |
| Commit messages                       | Engels                                                                      |
| Teksten op het scherm                 | Engels, altijd via `strings.xml` (geen hardgecodeerde teksten in de UI)     |
| Documentatie                          | Nederlands                                                                  |

## Naamgeving

- Gebruik Engels voor alle namen in de code.
- Gebruik duidelijke en beschrijvende namen voor variabelen, functies, klassen en bestanden.
- Volg de bestaande naamgevingsconventies van het project.
- Gebruik dezelfde benaming voor hetzelfde begrip in de volledige applicatie.
- Introduceer geen alternatieve namen voor bestaande concepten zonder overleg.
- Nieuwe afspraken over naamgeving worden in deze documentatie opgenomen.

### Woordenlijst

| Nederlands (documentatie)                     | Engels (code en UI)                    |
| --------------------------------------------- | -------------------------------------- |
| Klant                                         | `customer`                             |
| Handelaar                                     | `merchant`                             |
| Winkel                                        | `store`                                |
| Verrassingspakket, pakket                     | `bag`                                  |
| Reservatie                                    | `reservation`                          |
| Ophaalcode                                    | `pickupCode`                           |
| Ophaalvenster                                 | `pickupStart`, `pickupEnd`             |
| Favoriet                                      | `favorite`                             |
| Status gereserveerd / opgehaald / geannuleerd | `reserved` / `collected` / `cancelled` |

### Datamodel

De namen uit de opgave liggen vast. Je mag velden toevoegen, maar niet hernoemen.

- **Firestore:** `users/{uid}`, `stores/{storeId}`, `bags/{bagId}`, `reservations/{id}`. De rol in `users` is `"customer"` of `"merchant"`.
- **Room:** `FavoriteStore`, `CachedReservation`, `CachedAddress` in één `@Database`-singleton.

## Architectuurregels

- Composables bevatten geen Firebase-, Room- of Retrofit-code. Die zit in repositories en wordt via de ViewModel aangesproken.
- Lees lijsten realtime met `snapshots()` als `Flow`.
- Het scherm Bestellingen leest enkel uit Room. Elke wijziging van een reservatie in Firestore wordt ook naar Room geschreven.
- Foto's: verkleinen (max. 800 px langste zijde), JPEG-kwaliteit ± 70, `Base64.encodeToString(…, NO_WRAP)` in `photoBase64`. Verkleinen, comprimeren en decoderen gebeurt op `Dispatchers.Default`, nooit bij elke recompositie.
- Nominatim: eigen User-Agent die de app identificeert, maximaal 1 aanvraag per seconde, pas opvragen wanneer de pin wordt losgelaten. Resultaten cachen in `CachedAddress` met afgeronde coördinaten (4 decimalen) als sleutel.
- Locatiepermissie: `ACCESS_FINE_LOCATION` samen met `ACCESS_COARSE_LOCATION`, pas na uitleg en een actie van de gebruiker. Alle gevallen afhandelen, ook definitief geweigerd (knop naar de app-instellingen).
- De app overleeft een rotatie zonder verlies van ingevulde gegevens of toestand.

## Werkwijze per taak

1. **Branch:** Maak voor elke taak een aparte branch vanaf een recente `main`.
2. **Commits:** Maak kleine, duidelijke commits volgens Conventional Commits.
3. **Samenwerken:** Houd je branch regelmatig up-to-date met `main`.

**Werk nooit rechtstreeks op `main`.** Merge je branch pas in `main` wanneer de taak af is en de controles uit [Voor je merget naar `main`](#voor-je-merget-naar-main) slagen. We werken niet met merge requests.

**Push zonder Claude als helper.** Elk teamlid commit en pusht zelf. Claude maakt geen commits of pushes, en commits bevatten geen `Co-Authored-By: Claude`-regel of andere AI-vermelding.

## Branches

Gebruik de volgende naamgeving:

`<type>-<issue-nr>-<korte-beschrijving>`

Voorbeelden:

- `feature-12-add-navigation`
- `bug-17-fix-login`
- `docs-21-update-readme`

Gebruik korte, beschrijvende namen en houd de naamgeving consistent.

## Commits

Gebruik het formaat:

`<type>(<scope>): <description>`

Schrijf commit messages in het Engels, in de gebiedende wijs en zonder punt aan het einde. Git telt mee voor de beoordeling: beide teamleden committen regelmatig hun eigen werk.

Voorbeelden:

```text
feat(auth): add role selection on register
feat(discover): show bags sorted by distance
fix(reservation): prevent quantity below zero
refactor(store): move geocoding to repository
test(bag): add photo compression tests
docs(readme): update installation instructions
```

| Type       | Wanneer                                                    |
| ---------- | ---------------------------------------------------------- |
| `feat`     | Nieuwe functionaliteit                                     |
| `fix`      | Een bug oplossen                                           |
| `refactor` | Code verbeteren zonder gedragswijziging                    |
| `test`     | Tests toevoegen of aanpassen                               |
| `docs`     | Documentatie aanpassen                                     |
| `style`    | UI-styling of codeopmaak aanpassen zonder gedragswijziging |
| `chore`    | Onderhoud, configuratie of opruimwerk                      |
| `build`    | Buildconfiguratie of buildafhankelijkheden aanpassen       |

De scope beschrijft het onderdeel waarop de wijziging betrekking heeft, bijvoorbeeld `auth`, `discover`, `map`, `bag`, `reservation`, `favorites`, `store` of `theme`. Laat de scope weg als die niet zinvol is.

## Samenwerken als team van 2 personen

- Beide teamleden zijn gelijkwaardig en dragen verantwoordelijkheid voor de kwaliteit van het project.
- Verdeel het werk in overleg en maak duidelijk wie aan welke taak werkt (zie [planning.md](planning.md)).
- Communiceer wijzigingen die invloed hebben op het werk van de andere teamgenoot.
- Bespreek belangrijke technische beslissingen samen voordat je ze doorvoert.
- Controleer elkaars code.
- Elk teamlid moet zijn code kunnen uitleggen.
- Wekelijks overloopt minstens één teamlid met de docent wat af is en wat gepland is. Bereid dit samen voor.
- Vermijd onnodige conflicten door regelmatig wijzigingen op te halen en overlappende aanpassingen af te stemmen.
- Documenteer belangrijke beslissingen, zodat beide teamleden dezelfde informatie hebben.

## Codekwaliteit

- Volg de bestaande structuur en conventies van het project.
- Houd code eenvoudig, leesbaar en onderhoudbaar.
- Vermijd onnodige duplicatie.
- Gebruik bestaande componenten en hulpfuncties wanneer dat passend is.
- Verwijder ongebruikte code en tijdelijke debugging voordat je een wijziging indient.
- Voeg geen nieuwe dependencies toe zonder dat daar een duidelijke reden voor is.
- Ga zorgvuldig om met foutmeldingen, gebruikersinvoer en gevoelige gegevens.
- Zet geen wachtwoorden, API-sleutels of andere geheimen in de repository.
- Laat Firestore nooit in "test mode" staan. Security rules: enkel ingelogde gebruikers lezen en schrijven, een gebruiker wijzigt enkel zijn eigen profiel.
- Pas bestaande functionaliteit niet onnodig aan wanneer je een nieuwe feature toevoegt.

## Definition of Done

Een taak is klaar wanneer:

- [ ] De afgesproken functionaliteit werkt.
- [ ] Aan de vereiste uit de opgave (ID) is voldaan.
- [ ] De code is nagekeken en volgt de projectconventies en architectuurregels.
- [ ] Relevante tests zijn toegevoegd of aangepast en slagen (`./gradlew test`).
- [ ] De applicatie bouwt succesvol (`./gradlew assembleDebug`).
- [ ] Er zijn geen nieuwe onverklaarde fouten of lint-warnings.
- [ ] De app crasht niet zonder internet, zonder locatie, bij lege lijsten of ongeldige invoer.
- [ ] Netwerkoperaties tonen een laadindicator.
- [ ] De toestand blijft behouden na een rotatie.
- [ ] De interface is bruikbaar op de relevante schermformaten, in light en dark mode.
- [ ] De zichtbare teksten staan in `strings.xml` en zijn correct en consistent.
- [ ] Relevante documentatie is bijgewerkt.
- [ ] Beide teamleden kunnen de code uitleggen.
- [ ] De wijziging is gemerged in `main` en gepusht.

## Documentatie

- Houd de README up-to-date met installatie-, configuratie- en uitvoeringsinstructies.
- Documenteer belangrijke technische keuzes en afwijkingen van bestaande afspraken.
- Werk documentatie bij wanneer functionaliteit of de projectstructuur verandert.
- Houd [planning.md](planning.md) bij na elk wekelijks overleg.
- Voeg nieuwe algemene afspraken toe aan dit document.
- Verwijder of corrigeer verouderde instructies wanneer de werkwijze verandert.

## Voor je merget naar `main`

Controleer minimaal:

1. De applicatie bouwt succesvol (`./gradlew assembleDebug`).
2. Relevante tests slagen (`./gradlew test`).
3. De wijziging is zelf nagekeken.
4. De documentatie is bijgewerkt indien nodig.
5. Er zijn geen onbedoelde bestanden, geheimen of tijdelijke wijzigingen opgenomen.

## Wijzigingen aan deze afspraken

Deze afspraken gelden voor beide teamleden.

Wijzigingen aan de werkwijze worden samen besproken. Werk dit document bij wanneer jullie nieuwe afspraken maken, zodat het steeds overeenkomt met de manier waarop jullie het project daadwerkelijk ontwikkelen.
