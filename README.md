# Mark Remote

Android remote-support prototype. One phone shares its screen only after explicit Android consent; a second phone joins using a six-digit session code.

## Prototype scope
- WebSocket relay for pairing and remote messages.
- Android MediaProjection screen-capture permission flow.
- Accessibility service for explicitly approved taps/swipes.
- GitHub Actions debug APK build.

## Important limitations
This is a visible support tool, not hidden monitoring. The sharing phone has a persistent foreground notification and can stop sharing. The owner must grant screen capture and manually enable Accessibility. Secure/DRM content, lock screens, and some system screens may be unavailable. Device/OEM support varies.

The first milestone uses compressed screen frames over WebSocket. It is a prototype and not a production WebRTC video pipeline; latency and frame rate depend on the network and relay host.

## Relay server
Run `npm install && npm start` inside `server/`. Deploy `render.yaml` to Render, then replace `RELAY_URL` in `app/build.gradle.kts` with your secure `wss://` endpoint. The default `ws://10.0.2.2:8080` is for Android Emulator testing only.

## Build
Pushes to `main` trigger GitHub Actions. Download the `Mark-Remote-debug-apk` artifact from the workflow run.

## License
MIT
