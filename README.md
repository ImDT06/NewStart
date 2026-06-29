# NewStart - Habit & Task Management App

## 1. Introduction
This repository contains the source code for **NewStart**, a comprehensive personal assistant and habit-tracking application developed as a project for the **SE114** course at the University of Information Technology - VNUHCM (UIT).

Going beyond a traditional to-do list, NewStart is designed to help users organize various life contexts: building daily habits, tracking personal goals, writing journals with AI support, and managing tasks efficiently with an offline-first approach.

## 2. Tech Stack
The application is built with a focus on performance, security, and clean code principles, strictly following the **MVVM + Clean Architecture**:

* **Frontend:** 
    * **Jetpack Compose:** Modern toolkit for building native UI.
    * **Material 3:** Latest design system for Android.
    * **Coil:** Image loading library.
    * **CameraX:** Camera integration.
    * **Glance:** Home screen widgets.
* **Backend & Database:** 
    * **Firebase:** Auth (Google Sign-In), Firestore (Remote sync), Storage, and Functions.
    * **Room Database:** Local persistence for offline-first capability.
    * **DataStore:** Preference storage.
* **AI & Integration:**
    * **Google Gemini AI:** Generative AI for habit suggestions and insights.
* **Code Quality & CI/CD:** 
    * **Hilt (Dagger):** Dependency Injection.
    * **WorkManager:** Background task synchronization.
    * **Kotlin Serialization:** Modern JSON handling.

## 3. Authors
This project is collaboratively built and maintained by:

| Name | GitHub Profile |
| :--- | :--- |
| **Nguyễn Minh Trọng** | [@nmt2921](https://github.com/nmt2921) |
| **Trần Đức Trọng** | [@minhtrong2921](https://github.com/minhtrong2921) |
| **Nguyễn Anh Kiệt** | [@anhkietbienhoa-crypto](https://github.com/anhkietbienhoa-crypto) |
