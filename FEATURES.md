# Maya AI Clone - Advanced Prototype

This repository contains an advanced prototype of a Maya-like Android voice assistant.

Features implemented in this update:
- Voice input (mic) using Android Speech Recognizer
- OpenAI Chat integration (configurable via Settings -> paste your OpenAI API key)
- Asynchronous AI responses and Text-to-Speech (TTS)
- Intent-based device actions: call (dialer), SMS (opens messaging app), open apps (YouTube/WhatsApp/Chrome), open Settings
- Conversation history stored locally (SharedPreferences)
- Image generation via OpenAI Images endpoint (if supported by your API key)
- Settings screen to add API key and clear history
- APK build configuration (debug)

How to test locally:
1. Clone the repo
   git clone https://github.com/shivayadavkumsr9693879133-art/maya-ai-clone
2. Open in Android Studio
3. Build & run on a device (recommended) or emulator with mic
4. Open Settings in the app and paste your OpenAI API key
5. Use the mic or type queries. Examples:
   - "Call 9876543210"
   - "Send SMS to 9876543210 saying hello"
   - "Open YouTube"
   - "Write a short summary about Android development"
   - "Generate image of a mountain at sunset"

Security & Notes:
- Do NOT commit your API key to this repo. The app stores the key in SharedPreferences on your device.
- This is a prototype for demo/testing. Production hardening, privacy policy, and Play Store compliance are needed before publishing.

