# Type Tutor - Advanced Typing Speed Application

A professional-grade typing speed test application inspired by MonkeyType, designed to help users improve their typing speed and accuracy through practice and detailed performance analytics.

## 🎯 Overview

Type Tutor recreates the beloved MonkeyType experience as a standalone JavaFX desktop application. With its minimalist design, real-time feedback, and comprehensive statistics, it provides an engaging environment for typists of all skill levels to practice and improve.

## ✨ Features

### 🎮 Multiple Difficulty Levels
- **Beginner**: Lowercase letters only, perfect for building foundational typing skills
- **Intermediate**: Includes punctuation marks (commas, periods, quotes)
- **Advanced**: Full complexity with numbers, special characters, and all punctuation

### ⏱️ Flexible Test Durations
- Quick 15-second sprints for rapid practice
- Standard 30 and 60-second tests
- Extended 120-second marathons for endurance training

### 📊 Comprehensive Performance Metrics
- **WPM (Words Per Minute)**: Measures your typing speed based on correct characters
- **Raw WPM**: Total typing speed including errors
- **Accuracy**: Percentage of correctly typed characters
- **Consistency**: Tracks speed stability throughout the test
- **Character Breakdown**: Detailed count of correct/missed/extra/skipped characters

### 📈 Real-Time Analytics
- Live WPM tracking with visual chart
- Error tracking throughout the test
- Moment-by-moment performance visualization
- Historical data points every second

### 🎨 MonkeyType-Inspired Design
- Dark theme optimized for extended typing sessions (Serika Dark color scheme)
- Clean, distraction-free interface
- Color-coded feedback (correct in cream, errors in red, current position in yellow)
- Smooth animations and transitions

### 📚 Dynamic Text Sources
- Loads from local text files if available
- Falls back to Project Gutenberg classics (Pride and Prejudice, Frankenstein, Alice in Wonderland)
- Endless text streaming - never run out of content
- Smart chunk management with word boundary detection

## 🛠️ Technical Highlights

- Built with **JavaFX** for modern, responsive UI
- Asynchronous text loading for smooth performance
- Real-time character-by-character validation
- Statistical analysis with consistency calculations
- Interactive line charts with JavaFX Charts API
- Follows MonkeyType's UX principles

## 🚀 Getting Started

### Prerequisites
- Java 11 or higher
- JavaFX SDK 11+
- Maven or Gradle (optional, for dependency management)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/shashank-kodali/Type-Tutor.git
cd Type-Tutor
