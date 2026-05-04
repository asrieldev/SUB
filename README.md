# StudentUtilityHub

StudentUtilityHub is a Kotlin Android prototype for students who need one place to manage classes, tasks, expenses, reminders, and nearby services.

## Open in Android Studio

1. Open Android Studio.
2. Choose **Open**.
3. Select `C:\Users\MrRobot\AndroidStudioProjects\StudentUtilityHub`.
4. Let Gradle sync finish.
5. Run the `app` configuration on an emulator or Android phone.

## Prototype Features

- Visual dashboard with greeting, weather summary, date chips, and selected-day plans.
- Schedule planner with date/time class dialogs.
- Money screen with editable wallet balance, student-card summary, and transactions.
- Expense categories, income updates, and a simple weekly spending chart.
- Reminder list with date/time reminder dialogs.
- Task list with add, edit, delete, and completion controls.
- Local notifications for upcoming classes and reminders.
- Notification enable/disable and lead-time controls.
- Boot-time notification rescheduling after phone restart or app update.
- Calendar export for classes and reminders through the installed calendar app.
- Map screen with a real OpenStreetMap/osmdroid map, campus markers, service cards, and fallback Google Maps searches.
- Settings screen with dark mode, eco mode, and language switch controls.
- Full local profile editing, privacy policy, and JSON backup/export.
- Room database persistence for classes, expenses, and reminders.
- Local settings persistence for tasks, dark mode, eco mode, and language.

## Next Technical Steps

- Configure external API keys for live weather, Google Sign-In, Google Calendar API, and Google Maps SDK.
- Add Firebase Authentication and Firestore sync.
- Add Google Sign-In and direct Google Calendar API sync with a Google Cloud OAuth client.
- Optionally replace osmdroid with Google Maps SDK after a Google Maps API key is available.
