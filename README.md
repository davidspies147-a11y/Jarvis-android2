# JARVIS Android starter

A native Android (Kotlin + Jetpack Compose) starter with:
- Dark, futuristic chat interface
- Text chat
- Android speech-to-text through the device speech service
- Android text-to-speech replies
- Optional AI connection through a small Node.js backend

## Open the Android app
1. Install Android Studio.
2. Open this folder in Android Studio.
3. Let Gradle sync.
4. Run on an Android phone or emulator (Android 8.0+).
5. Allow microphone permission when prompted.

The chat screen works immediately in demo mode. Until the backend is configured, JARVIS explains that its AI connection is not set up.

## Activate AI replies
1. Install Node.js 18+ on a computer/server.
2. In `backend/`, run `npm install`.
3. Copy `.env.example` to `.env`, add your OpenAI API key, and set the model you have access to.
4. Run `npm start`.
5. Deploy the backend with HTTPS. Do not expose the API key in the Android app.
6. In `MainActivity.kt`, set `BASE_URL` to your deployed HTTPS backend URL.
7. Rebuild and run the Android app.

## Important
- The included backend is a starter, not a production deployment. Add authentication, rate limits, request-size limits, logging controls, and cost limits before public release.
- Use HTTPS for the deployed backend.
- Voice recognition availability and language support depend on the Android device's installed speech service.
- This starter does not yet have long-term memory, reminders, calendar access, wake-word detection, or background listening.
