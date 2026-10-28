# NoWaste

<img src="UI/logo.png" alt="NoWaste-logo" width="160">

Mobile Development Project.

NoWaste is een Android-app tegen voedselverspilling. Handelaars bieden hun overschotten van de dag aan als verrassingspakket aan een lage prijs. Klanten zoeken pakketten in de buurt via een lijst of kaart, reserveren er een en halen het op met een ophaalcode binnen een vast tijdsvenster.

Gebouwd met Kotlin, Jetpack Compose, Firebase, Room en OSMDroid.

**[Planning](Documentation/planning.md)** · [UI](UI/UI.md)

## Installatie

1. Installeer Android Studio (met Android SDK 37).
2. Open de projectmap in Android Studio. `local.properties` met het SDK-pad wordt automatisch aangemaakt.
3. Bouw en start de app met de knop Run, of in een terminal:

   ```bash
   ./gradlew installDebug   # Windows: gradlew.bat installDebug
   ```

Alle commando's staan in [werkafspraken.md](Documentation/werkafspraken.md#commandos).

## Projectstructuur

| Map | Inhoud |
|---|---|
| `app/src/main/java/be/ap/nowaste/ui/theme` | Kleuren en Material-thema (light en dark) |
| `app/src/main/java/be/ap/nowaste/ui/navigation` | Routes en bottom navigation |
| `app/src/main/java/be/ap/nowaste/ui/<scherm>` | Per scherm de composable en de ViewModel |
| `gradle/libs.versions.toml` | Versies van alle dependencies |

Created by Neagu Sergiu!\
CO-Owned by Laurens Rombout!\
Year: 2026-2027\
School: AP Hogeschool
