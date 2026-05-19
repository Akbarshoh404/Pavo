# Pavo

Pavo is an Android marketplace app for animal listings in Uzbekistan. It is built with Kotlin, Jetpack Compose, Firebase, and Supabase, and supports browsing, search, favorites, posting listings, seller profiles, authentication, and in-app chat.

## Features

- Browse active animal listings from the home feed
- Search and filter by listing type, animal type, city, price, and text query
- View detailed listing pages with seller information
- Save favorite listings locally
- Post new listings with image upload to Supabase Storage
- Open seller profiles and start direct chats
- Sign in with email/password, Google, or phone OTP
- Manage profile basics, app theme, and language
- Use the app in Uzbek, English, or Russian

## Tech stack

- Kotlin 2.0
- Jetpack Compose + Material 3
- Android Navigation Compose
- MVVM with a shared `AppViewModel`
- Firebase Authentication
- Firebase Realtime Database for chat
- Supabase PostgREST for listing and user data
- Supabase Storage for uploaded images
- Android DataStore for local user session and saved items
- Coil for image loading

## Project structure

```text
app/
  src/main/
    java/uz/angrykitten/pavo/
      MainActivity.kt
      data/
        model/
        repository/
        SupabaseClient.kt
      ui/
        components/
        localization/
        navigation/
        screens/
        theme/
        viewmodel/
    assets/
    res/
supabase/
  seed_sample_data.sql
  cities_rows.csv
  districts_rows.csv
  properties_rows.csv
play-store-assets/
tools/
```

## Main screens

- Splash
- Home
- Search
- Animal detail
- Seller profile
- Post listing
- Saved listings
- My listings
- Chat list and chat detail
- Login and register
- Settings
- FAQ and privacy policy

## Requirements

- Android Studio
- JDK 11
- Android SDK 36
- A Firebase project
- A Supabase project

## Setup

### 1. Firebase

Place your Firebase config file here:

```text
app/google-services.json
```

Enable these Firebase products:

- Authentication
- Realtime Database

If you plan to use Google sign-in, also add your web client id to `local.properties`:

```properties
GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

If you plan to use phone OTP in Uzbekistan, make sure the SMS region policy in Firebase Auth allows `+998`.

### 2. Supabase

Update [SupabaseClient.kt](D:/Projects/Mobile%20Development/Pavo/Uybek-Mobile/app/src/main/java/uz/angrykitten/pavo/data/SupabaseClient.kt) with your project values:

```kotlin
supabaseUrl = "https://your-project.supabase.co"
supabaseKey = "your-anon-key"
```

The app expects these Supabase resources:

- `cities` table
- `districts` table
- `animals` table
- `users` table
- `Animals` storage bucket

The repository already includes helper data under [supabase](D:/Projects/Mobile%20Development/Pavo/Uybek-Mobile/supabase) that you can use while setting up seed data.

### 3. Local properties

Your `local.properties` should at least point to your Android SDK. If using Google sign-in, also add:

```properties
GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

## Build and run

Debug build:

```powershell
.\gradlew.bat assembleDebug
```

Install on a connected device or emulator:

```powershell
.\gradlew.bat installDebug
```

Run unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

## Data model overview

Listings are represented by the `Animal` model and include:

- listing type such as sale, adoption, or stud
- animal type such as dog, cat, sheep, cow, horse, or other
- breed, age, weight, and vaccination metadata
- city, district, and address fields
- price and currency
- seller profile information
- a list of image URLs

## Notes about the current implementation

- User session state and saved listing ids are persisted with DataStore
- Chats use Firebase Realtime Database directly from the UI layer
- Listings and user profiles are loaded from Supabase
- Listing image upload goes to the Supabase Storage bucket named `Animals`
- App language and theme selection are currently handled in-app; the codebase supports Uzbek, English, and Russian

## Useful paths

- App entry point: [MainActivity.kt](D:/Projects/Mobile%20Development/Pavo/Uybek-Mobile/app/src/main/java/uz/angrykitten/pavo/MainActivity.kt)
- Navigation graph: [AppNavGraph.kt](D:/Projects/Mobile%20Development/Pavo/Uybek-Mobile/app/src/main/java/uz/angrykitten/pavo/ui/navigation/AppNavGraph.kt)
- Shared view model: [AppViewModel.kt](D:/Projects/Mobile%20Development/Pavo/Uybek-Mobile/app/src/main/java/uz/angrykitten/pavo/ui/viewmodel/AppViewModel.kt)
- Listing repository: [AnimalRepository.kt](D:/Projects/Mobile%20Development/Pavo/Uybek-Mobile/app/src/main/java/uz/angrykitten/pavo/data/repository/AnimalRepository.kt)
- Auth repository: [AuthRepository.kt](D:/Projects/Mobile%20Development/Pavo/Uybek-Mobile/app/src/main/java/uz/angrykitten/pavo/data/repository/AuthRepository.kt)

## Verification

Latest local verification completed with:

```powershell
.\gradlew.bat assembleDebug
```

The debug build succeeded after fixing the launcher resource XML files.
