# Match-it Android

Mobilní aplikace pro **[Match-it](https://github.com/Cross-bit/match-it-demo-backend/tree/thesis)** — výzkumný prototyp skupinového doporučovacího systému. Aplikace pomáhá skupinám přátel najít společnou shodu na **filmu** nebo **restauraci v okolí** pomocí společného hlasování.

Organizátor vytvoří relaci a pozve ostatní účastníky. Ti se připojí do čekací místnosti a následně hlasují o doporučených položkách pomocí jednoduchých gest: líbí se, nelíbí se nebo neutrální volba. Server průběžně vyhodnocuje hlasy přes WebSockety a posílá další doporučení, dokud skupina nenajde shodu nebo relace neskončí.

Aplikace pokrývá celý uživatelský průběh relace: pozvánky, čekací místnost, hlasovací kola s kartami filmů a restaurací, mapy podniků, chat během relace, FCM upozornění a historii proběhlých relací. Komunikuje s demonstračním backendem přes REST API a WebSockety. Výpočet doporučení a trvalé ukládání dat probíhají plně na straně serveru.

### Hlavní funkcionality

- **Uživatelský účet** — registrace, přihlášení, správa profilu a preferencí uživatele.
- **Přátelé** — vyhledávání uživatelů, posílání a správa žádostí o přátelství a seznam přátel pro pozvání kontaktů do hlasovacích relací.
- **Skupinové hledání shody** — vytváření a připojování se k filmovým nebo restauračním relacím, paralelní distribuované swipe hlasování a synchronizace v reálném čase přes WebSockety.
- **Historie relací** — přehled všech minulých relací, kterých se uživatel účastnil s výsledky hlasování, s možností dalšího pokračovat v chatu.

---

## Požadavky

- Android **9.0 (API 28)** nebo novější
- Google účet přihlášený na zařízení kvůli FCM push notifikacím  
  (pozvánky do relací a upozornění na nalezenou shodu využívají Firebase Cloud Messaging)

---

## Sestavení a spuštění

Doporučený způsob sestavení a spuštění projektu je přes **Android Studio** s použitím dodaných 
Gradle souborů. Alternativně lze použít Gradle wrapper z příkazové řádky:

```sh
./gradlew assembleDebug
```

### Build varianty

Projekt definuje dvě vývojové build varianty. Před sestavením vyberte odpovídající variantu:

| Varianta | Popis |
|---|---|
| `devEmu` | Vývojový build pro Android emulátor (`10.0.2.2`) |
| `devDevice` | Vývojový build pro fyzické zařízení; používá `DEV_SERVER_IP` z `local.properties` |

V Android Studiu lze variantu vybrat v panelu **Build Variants**.

### Lokální konfigurace

Před spuštěním aplikace vytvořte v kořeni projektu soubor `local.properties`:

```properties
# Vyžadováno pro variantu devDevice — nastavte lokální IP adresu svého počítače
DEV_SERVER_IP=192.168.x.x

# Vyžadováno pro Google Maps (zobrazení polohy restaurací)
GOOGLE_MAPS_API_KEY=your-maps-api-key
```

> Pro variantu `devEmu` není `DEV_SERVER_IP` potřeba — emulátor automaticky používá adresu `10.0.2.2`.

---

## Testování

**Unit testy** (`app/src/test`) běží na JVM a nevyžadují fyzické zařízení ani emulátor:

```sh
./gradlew testDebugUnitTest
```

## Poznámky

- Pro fungování push notifikací musí být na zařízení dostupné Firebase Cloud Messaging. Pozvánky do relací a upozornění na nalezenou shodu jsou posílány přes FCM.
- Mapy restaurací používají Google Maps API klíč ze souboru `local.properties` popsaného výše.

---

## Licence

Copyright (c) 2026 Ondřej Kříž

Tento software je výzkumný prototyp licencovaný pouze pro **nekomerční výzkumné a vzdělávací účely**. Komerční použití je zakázáno bez výslovného písemného souhlasu.

Úplné licenční podmínky jsou uvedeny v souboru [LICENSE](./LICENSE). Pro dotazy ohledně komerční licence kontaktujte: ondra.kryz@seznam.cz
