# Match-it Android

Android client for the **Match-it** application — a group 
activity consensus app that helps friends agree on movies 
or nearby restaurants using a swipe-based voting session.

> The backend is available at [match-it-backend](https://github.com/Cross-bit/match-it-demo-backend).

---

## Overview

The app allows users to create or join a 
shared voting session, swipe through activity 
recommendations, and reach a group consensus. 
Currently **movie matching** is supported.

A short showcase of the application is available [here](https://youtu.be/MYrbK1zM0lk?si=Bxf3wih-Kt6TvfYU).

---

## Requirements

- Android **9.0 (API 28)** or higher
- Google account signed in on the device — required 
for FCM push notifications (session invites and match 
updates rely on Firebase Cloud Messaging)

---

## Build and Run

The recommended way to build and run the project 
is via **Android Studio** using the provided Gradle 
files. Alternatively, use the Gradle wrapper from 
the command line:

```sh
./gradlew assembleDebug
```

### Build Flavors

The project defines four build flavors — select the appropriate one before building:

| Flavor | Description |
|---|---|
| `devEmu` | Development build targeting Android emulator (`10.0.2.2`) |
| `devDevice` | Development build targeting a physical device (uses `DEV_SERVER_IP` from `local.properties`) |

In Android Studio, select the flavor via **Build Variants** panel.

### Local Configuration

Before running the app, create a `local.properties` file in the project root:

```properties
# Required for devDevice flavor — set to your machine's local IP
DEV_SERVER_IP=192.168.x.x

# Required for Google Maps (restaurant location display)
GOOGLE_MAPS_API_KEY=your-maps-api-key
```

> For `devEmu` flavor, `DEV_SERVER_IP` is not needed — the emulator uses `10.0.2.2` automatically.

---

## Notes

- Users must be signed into a Google account on the device for push notifications (FCM) to work. This is required for session invitations and real-time match updates.
- Only **movie matching** is currently supported. Restaurant matching is planned.

---

> This repository contains a research prototype developed as part of a bachelor's thesis.

## License

Copyright (c) 2026 Ondřej Kříž

This software is a research prototype licensed for **non-commercial research and educational use only**. Commercial use is prohibited without explicit written permission.

See [LICENSE](./LICENSE) for full terms. For commercial licensing inquiries contact: ondra.kryz@seznam.cz
