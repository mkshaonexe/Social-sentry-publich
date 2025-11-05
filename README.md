# Social Sentry

Social Sentry is a free app that helps you block addictive short videos and reels from social media apps, take control of your scrolling habits, and boost productivity.

## 🚀 Features

🚫 **Block Addictive Content** - Automatically blocks reels, shorts, and videos from social media apps

⚙️ **Customize Blocking Apps** - Select which apps to block individually

📱 **Supported Apps** - Instagram, YouTube, TikTok, Facebook, Facebook Lite

💡 **User-Friendly Interface** - Simple and intuitive design

## Getting Started

### Prerequisites

- Android Studio
- Git

### Installing

1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/SocialSentryPrivate.git
   ```

2. Open the project in Android Studio

3. **⚠️ IMPORTANT: Add Resource IDs**

   The resource IDs for detecting reels/shorts are not included in this source code. You need to add them manually in `SocialSentryAccessibilityService.kt` as these IDs change every time social media apps update:
   
   - Navigate to `app/src/main/java/com/socialsentry/ai/accessibility/SocialSentryAccessibilityService.kt`
   - Add the resource IDs in the `handleInstagram()`, `handleYouTube()`, `handleTikTok()`, `handleFacebook()`, and `handleFacebookLite()` methods
   - You can find resource IDs using Developer Assistant app or similar tools

4. Build and run the app

## ⚠️ Note

**Manual Resource IDs Required**: The resource IDs for detecting reels/shorts are not included in this source code. You must add your own resource IDs as social media apps frequently update their UI structure. This requires manual effort and cannot be automated.

## License

This project is licensed under the GPL v3.0 License - see the LICENSE file for details.

