
# iTantra Wi-Fi — Kotlin Starter

A clean Kotlin + Jetpack Compose starter for SIH26173.

## Current starter features
- Modern Compose UI
- 10-language selector
- Local Android TTS
- Push-to-talk style interaction
- Connection state UI
- Conversation list
- Performance metric placeholders
- Required Android microphone/network permissions

## Important
This is a **starter project**, not a claim that the SIH requirements are fully implemented.

For the complete SIH prototype, replace the demo connection state with:
1. Android local-network service discovery / pairing.
2. TCP or UDP text-packet transport over the same Wi-Fi.
3. Open-source offline STT (for example an ONNX/TFLite-compatible Indian-language model).
4. Pause/VAD sentence segmentation.
5. Open-source offline TTS models for the required languages.
6. Real timestamps for STT, transmission, TTS and end-to-end latency.
7. RAM/CPU/model-size/data-size measurements.

## Build
Open the folder in Android Studio with a recent Android Gradle Plugin/Kotlin setup.
Run on Android 8.0+.

For two-phone testing, connect both phones to the same local Wi-Fi network.
