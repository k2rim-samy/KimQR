# QR Master

QR Master is a modern, premium QR Code Generator & Scanner application designed for Android and iOS with Flutter. The repository also keeps the existing native Android implementation while adding a clean Flutter application layer for a cross-platform future.

## Flutter App Highlights

- Clean, feature-first architecture under `lib/core` and `lib/features`.
- Material 3 design with responsive layouts, elegant light/dark palettes, and `ThemeMode.system` automatic theme switching.
- Device-language aware localization infrastructure with English, Arabic, Spanish, and French placeholders.
- Modern glassmorphism cards, smooth transitions, and animated scanner framing.
- QR generation for text, URLs, WiFi credentials, phone, SMS, email, vCard, location, and social links.
- Real-time QR preview with configurable colors, gradient backgrounds, and visual styles.
- Scanner screen powered by camera scanning, gallery image analysis, flashlight controls, and URL confirmation before opening.
- Export/share hooks for high-quality PNG and PDF workflows.

## Getting Started

```bash
flutter pub get
flutter run
```

## Existing Android Build

The native Android project can still be built with Gradle:

```bash
./gradlew test
./gradlew assembleDebug
```
