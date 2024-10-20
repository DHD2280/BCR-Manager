# Changelog

## 1.2.2 - 2024-03-15
- Added locales config
- Longpress on bottom player to highlight recording

## 1.2.1 - 2024-03-15
- Fixed bug #4: [Issue #4](https://github.com/DHD2280/BCR-Manager/issues/4)
- Added Latvian Language

## 1.2.0 - 2024-01-21
- Improved Code Quality
  - Using View Model for getting recordings and share data between fragments
  - MediaPlayerService use ExoPlayer from media3
  - Using RxJava for async loading and deleting recordings
  - Breakpoint dialog and Delete dialog moved to new classes instead of inner classes
  - Moved CallLogAdapter to new class
  - All fragments now use CallLogAdapter
- Improved UI
  - Bottom Player has now color of Secondary Container `colorSecondaryContainer`

## 1.1.1 - 2024-01-17
- Fixed bug in Sim Number [Issue #2](https://github.com/DHD2280/BCR-Manager/issues/2)
- Added Sim Filter in Home

## 1.1.0 - 2024-01-16
- Improved code quality
- Improved ContactObserver when clicking edit/add contact
- Improved UI
- Added Themes
- Added Night Mode Override (System, Light, Dark)

## 1.0.4 - 2024-01-14
- Added item customization in Settings

## 1.0.3 - 2024-01-14
- Improved parsing logic: we need only date, then will search in call log for the call.
- Using first contact found in Contacts.
- Improved UI.
- Improved Contact Observer when clicking edit/add contact.

## 1.0.2 - 2024-01-10
- Improved async batch delete
- Added Error Reporting Activity, you can save log directly from app then open a new Issue on GitHub.

## 1.0.1 - 2024-01-09
- Some fix in parsing file name for registrations without JSON file
- Batch delete run on background thread
- Use `timestamp_unix_ms` instead of `timestamp`

## 1.0.0 - 2024-01-08
- First Main Release