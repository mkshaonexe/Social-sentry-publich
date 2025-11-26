# Privacy Policy for Social Sentry

**Last Updated:** November 26, 2025  
**Effective Date:** November 26, 2025

## Introduction

Social Sentry ("we", "our", "us") is committed to protecting your privacy. This Privacy Policy explains how our Android application handles your information.

## Data Collection

**Social Sentry collects ZERO data.**

We do not:
- ❌ Collect any personal information
- ❌ Collect any usage data
- ❌ Collect any analytics
- ❌ Store any user content
- ❌ Track your behavior
- ❌ Access your contacts, messages, or files
- ❌ Record your screen or audio

## What We Access

Social Sentry uses Android Accessibility Service API **exclusively** for digital wellbeing features:

### Monitored Apps
The app only monitors these 8 social media applications:
- Instagram (`com.instagram.android`)
- YouTube (`com.google.android.youtube`)
- TikTok (`com.zhiliaoapp.musically`)
- Facebook (`com.facebook.katana`)
- Facebook Lite (`com.facebook.lite`)
- Threads (`com.instagram.barcelona`, `com.instagram.threadsapp`)
- Pinterest (`com.pinterest`)

### What We Read
The Accessibility Service reads:
- UI element labels and descriptions (e.g., "Reels", "Shorts")
- Screen titles to detect specific sections
- Button states for auto-mute feature

**All processing happens locally on your device in real-time. Nothing is stored, logged, or transmitted.**

## Features & Permissions

### 1. Block Reels & Shorts 🚫
- **What it does:** Automatically navigates away from short-form video sections
- **Why we need it:** Must read UI to detect Reels/Shorts screens and perform back navigation
- **Data collected:** NONE - only reads UI labels in real-time

### 2. Safety Countdown ⏸️
- **What it does:** Shows 5-second motivational overlay before opening social media apps
- **Why we need it:** Must detect app launches and display intervention overlay
- **Data collected:** NONE - timestamp-based blocking only

### 3. Time-Based Scroll Limiter ⏱️
- **What it does:** Enforces customizable time limits with mandatory breaks
- **Why we need it:** Must track scroll events and usage time
- **Data collected:** Usage time stored locally only (never transmitted)

### 4. Auto-Mute Facebook Videos 🔇
- **What it does:** Automatically clicks mute buttons on Facebook videos
- **Why we need it:** Must locate and click UI elements
- **Data collected:** NONE - only performs actions in real-time

## Permissions Required

### Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`)
**Why needed:** Core functionality for all features listed above  
**Scope:** Limited to 8 monitored apps only  
**Data access:** UI element labels only (read-only)

### Usage Stats (`PACKAGE_USAGE_STATS`)
**Why needed:** Track time spent in apps for scroll limiter  
**Scope:** Only for monitored apps  
**Data stored:** Locally only, never transmitted

### Display Over Apps (`SYSTEM_ALERT_WINDOW`)
**Why needed:** Show countdown and break overlays  
**Data access:** None - only displays UI

### Boot Completed (`RECEIVE_BOOT_COMPLETED`)
**Why needed:** Maintain service after device restart  
**Data access:** None

### Activity Recognition (`ACTIVITY_RECOGNITION`)
**Why needed:** Future feature for walking counter  
**Data access:** None currently used

## How We Use Information

**We don't.** 

Social Sentry operates 100% offline:
- ✅ No internet connection required
- ✅ No data transmission to servers
- ✅ No cloud storage
- ✅ No third-party services
- ✅ No advertising networks
- ✅ No analytics platforms

## Data Storage

Only these settings are stored **locally on your device**:
- Your preference toggles (which features are enabled/disabled)
- Time limits you set for scroll limiter
- Your app theme preference

**Storage method:** Android DataStore (encrypted local storage)  
**Location:** Your device only  
**Backup:** Optional via Android Auto Backup (user preferences only)

## Third-Party Services

**Social Sentry uses ZERO third-party services.**

No dependencies on:
- Analytics (Google Analytics, Firebase, etc.)
- Crash reporting (Crashlytics, Sentry, etc.)
- Advertising networks
- Cloud services
- Social media SDKs

## Your Rights

You have complete control:
- ✅ **Disable anytime** - Turn off accessibility service in Android settings
- ✅ **Uninstall anytime** - No restrictions or prevention
- ✅ **Delete data** - Uninstalling removes all local data
- ✅ **View permissions** - Check in Android app settings

## Children's Privacy

Social Sentry does not collect any data from anyone, including children under 13. The app is safe for all ages.

## Changes to Privacy Policy

We may update this policy occasionally. Changes will be posted:
- In this document (with updated "Last Updated" date)
- In the app's GitHub repository
- In Play Store listing

Continued use after changes constitutes acceptance of the updated policy.

## Contact Information

**Developer:** MK Shaon  
**Email:** mkshaon2024@gmail.com  
**Website:** https://mkshaon.com/social_sentry  
**GitHub:** https://github.com/mkshaonexe/Social-sentry-publich  

For privacy concerns, questions, or requests, contact us at: **mkshaon2024@gmail.com**

## Compliance

Social Sentry complies with:
- ✅ Google Play Developer Policies
- ✅ Android Accessibility Service API Policy
- ✅ General Data Protection Regulation (GDPR) - no data collected
- ✅ California Consumer Privacy Act (CCPA) - no data collected
- ✅ Children's Online Privacy Protection Act (COPPA) - no data collected

## Legal Basis (GDPR)

Since we collect **zero data**, no legal basis for data processing is required. The app processes UI information **locally and temporarily** only to provide its core functionality.

## Data Retention

**Retention period:** NONE

We do not retain any user data. All processing is real-time and local. When you uninstall the app, all local settings are deleted from your device.

## Security

While we don't collect data, we implement security best practices:
- ✅ No network access (offline-only)
- ✅ Local data encryption (Android DataStore)
- ✅ Minimal permission scope
- ✅ Code obfuscation (ProGuard)
- ✅ Regular security audits

## Transparency

This app is designed with **privacy-first** principles:
- Open about what it does
- Clear about what it accesses
- Transparent about its limitations
- Honest about its capabilities

---

**Summary:** Social Sentry is a 100% offline, privacy-respecting digital wellbeing app that collects absolutely no data. Everything happens on your device, and nothing leaves your device.

If you have any questions about this Privacy Policy, please contact us at mkshaon2024@gmail.com.

---

**Acknowledgment:** By using Social Sentry, you acknowledge that you have read and understood this Privacy Policy.
