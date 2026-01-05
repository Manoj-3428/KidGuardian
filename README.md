# PhoneLock App

A comprehensive Android application with login/signup functionality and phone locking capabilities.

## Features

### 🔐 Login/Signup Screens
- **Beautiful UI**: Modern design inspired by Allegiant Simple UI template
- **Smooth Animations**: Multiple animations including slide, fade, scale, and content transitions
- **Dual Mode Support**: Toggle between User Mode and Parent Mode
- **Form Validation**: Real-time validation for all input fields
- **Background Images**: Downloaded from Figma design for authentic look

### 📱 Lock Screen
- **Secure Lock**: PIN-based lock screen with device admin integration
- **Kiosk Mode**: Full-screen immersive experience
- **Screenshot Prevention**: Built-in security features

### 🎨 Design Features
- **Gradient Backgrounds**: Pink to purple gradient theme
- **Animated Elements**: 
  - Logo slide-in animation
  - Form field staggered animations
  - Background image scaling
  - Smooth toggle transitions
- **Responsive Layout**: Adapts to different screen sizes
- **Material Design 3**: Modern UI components

## Screenshots

### Login Screen
- Email and password fields with validation
- Parent/User mode toggle
- Smooth animations and transitions

### Signup Screen
- Full name, email, password, and confirm password fields
- Password matching validation
- Create account functionality

## Technical Details

### Animations Used
1. **Slide Animations**: Elements slide in from different directions
2. **Fade Animations**: Smooth opacity transitions
3. **Scale Animations**: Background images scale based on active screen
4. **Content Transitions**: Smooth switching between login and signup forms
5. **Staggered Animations**: Form fields animate in sequence

### Navigation Flow
1. MainActivity → LoginSignupActivity
2. LoginSignupActivity → LockScreenActivity (after successful login/signup)

### Dependencies
- Jetpack Compose for UI
- Material Design 3 components
- Animation libraries
- Device Admin API for lock screen functionality

## Usage

1. **Launch the app** - Starts with the login/signup screen
2. **Choose mode** - Toggle between User Mode and Parent Mode
3. **Switch forms** - Use the toggle buttons to switch between Login and Signup
4. **Fill forms** - Enter required information with real-time validation
5. **Submit** - After successful authentication, proceed to lock screen

## Security Features

- Password field masking
- Form validation
- Secure lock screen implementation
- Device admin integration
- Screenshot prevention

## Development

The app is built using:
- Kotlin
- Jetpack Compose
- Material Design 3
- Android Device Admin API

All animations are smooth and optimized for performance, providing an excellent user experience.
