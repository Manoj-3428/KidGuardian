KidGuardian - Parental Control App

---

Overview

KidGuardian is an Android parental control application that monitors a child's device for inappropriate content and blocks the screen when detected. Parents receive unlock codes and can view detailed analytics about detected content.

---

Screenshots

![kidguradian1](https://github.com/user-attachments/assets/933efcbc-bb53-4bae-ac21-c1590d9bf4ef)
![kg2](https://github.com/user-attachments/assets/4a34e88f-3ad8-4937-bcfb-524bd2e5a458)
![kg3](https://github.com/user-attachments/assets/52bb866e-6679-4520-9d3b-3aa88a0d40b4)
![kg4](https://github.com/user-attachments/assets/54bd4ba5-d9e7-4b1e-a305-e4c727ce2ed6)
![kg5](https://github.com/user-attachments/assets/3c7d54b9-98e4-4a95-b915-868ee7e25d7c)
![kg6](https://github.com/user-attachments/assets/7965fbf4-005c-4f62-a174-76e145b6feaf)
![kg7](https://github.com/user-attachments/assets/dd55a990-0334-41d9-a1e3-a3884f5a6a47)
![kg8](https://github.com/user-attachments/assets/f2b322e6-7daa-4fb9-bcea-e57cb023555c)
![kg9](https://github.com/user-attachments/assets/0f7a014c-bc52-4cb5-9eb9-d1ab208d35c8)
![kg10](https://github.com/user-attachments/assets/527bda9c-98c1-4ed1-a9f2-010f8aa2ef46)
![kg11](https://github.com/user-attachments/assets/bbd3863d-03c4-4b3c-9963-af9cf54851e7)
![kg12](https://github.com/user-attachments/assets/87c3833a-c44b-40ee-8f3c-6fad76de0697)
![kg13](https://github.com/user-attachments/assets/2408f174-476d-4649-9218-775c5be7df2b)
![kg14](https://github.com/user-attachments/assets/181c5566-4368-45c2-ad5b-de7cd549186d)



User Flow

Child Setup
1. Child creates account with name, age, gender, and email
2. Child grants required permissions (Device Admin, Overlay, Accessibility)
3. Child generates a 6-digit linking passcode
4. Child shares passcode with parent

Parent Setup
1. Parent enters child's linking passcode
2. Parent creates profile (name, relationship)
3. Parent account is linked to child

Detection Flow
1. Child uses device normally
2. Accessibility service monitors screen content in real-time
3. When inappropriate word is detected, screen locks immediately
4. Lock screen blocks all access - child cannot enter any other screen or app
5. Parent receives notification with unlock code
6. Child must enter correct code to unlock device

Unlock Flow
1. Child sees lock screen with detected word
2. Child asks parent for unlock code
3. Parent provides 6-digit code from notification or dashboard
4. Child enters code and device unlocks

---

Technical Architecture

MVVM Structure
- Models: Child, Parent, Complaint, UserRole
- ViewModels: ParentDashboardViewModel, ChildDashboardViewModel, LockScreenViewModel
- Repository: UserRepository handles all Firebase operations
- UI: Jetpack Compose screens and components

Core Services

AbuseDetectionService (AccessibilityService)
- Monitors all screen text content
- Matches against abusive words database
- Tracks foreground app for accurate reporting
- Creates complaint and triggers lock screen

LockMonitorService (Foreground Service)
- Keeps lock screen active
- Prevents dismissal attempts
- Runs in kiosk mode when device owner

ScreenshotCaptureService
- Captures screenshot when abuse detected
- Uploads to Firebase Storage
- Attaches URL to complaint

Database Structure (Firebase Firestore)

children collection:
- childId, userId, name, age, gender, email
- parentLinked, linkedParents, linkPasscode
- logoutPasscode, passcodeGeneratedAt

parents collection:
- parentId, name, relationship, linkedChildId
- monitoringEnabled

complaints collection:
- complaintId, childId, detectedWord, appName
- screenshotUrl, category, secretCode
- timestamp, accessed, unlockTimestamp

Key Components

Permissions Required:
- Device Admin (for kiosk mode)
- System Alert Window (overlay)
- Accessibility Service (content monitoring)
- Media Projection (screenshots)

Detection Categories:
- Offensive Language
- Violence and Threats
- Sexual Content
- Substance Abuse
- Bullying and Harassment

---

File Structure

app/src/main/java/com/example/phonelock/
- Activities: Main, Auth, LockScreen, ChildDashboard, ParentDashboard, PermissionSetup
- Services: AbuseDetectionService, LockMonitorService, ScreenshotCaptureService
- Repository: UserRepository
- Models: UserModels, AbusiveWordsDatabase
- ViewModels: ParentDashboardViewModel, ChildDashboardViewModel
- UI Components: ui/parent/, ui/child/

---

Build Requirements

- Android Studio
- Min SDK: 24
- Target SDK: 34
- Kotlin
- Jetpack Compose
- Firebase (Auth, Firestore, Storage)

---

Setup

1. Clone repository
2. Add google-services.json to app folder
3. Enable Device Owner for kiosk mode:
   adb shell dpm set-device-owner com.example.phonelock/.MyDeviceAdminReceiver
4. Build and run

---

Notes

- Kiosk mode requires device owner permission
- Accessibility service must be manually enabled in settings
- Screenshot permission is requested on first detection
- Parent link status is checked every 5 seconds for responsiveness
- When lock screen is active, child cannot access any other screen or application until correct unlock code is entered





