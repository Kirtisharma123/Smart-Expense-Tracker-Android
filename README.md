# Smart Expense Tracker – Android App

A feature-rich Android application designed to help users manage, track, and analyze their daily expenses.

## Features

- **Expense Management:** Add and save expenses with title, amount, category, date, payment method, and notes.
- **Room Database:** Store and retrieve expense records locally.
- **Expense Dashboard:** View total expenses and recent transactions.
- **Expense Analytics:** Visualize spending patterns using Pie Charts and Bar Charts.
- **Receipt Scanning (OCR):** Extract receipt text and detected amounts using Google ML Kit.
- **PDF Export:** Generate and save expense reports in PDF format.
- **Excel Export:** Export expense records into `.xlsx` spreadsheets.

## Technologies Used

- Kotlin
- Android Studio
- XML Layouts
- Room Database
- Kotlin Coroutines and Flow
- MPAndroidChart
- Google ML Kit Text Recognition
- Android Storage Access Framework
- PDF Generation
- Excel XLSX Export

## Project Structure

- `data/` – Room database, entities, DAO, and export utilities.
- `MainActivity.kt` – Dashboard, charts, OCR scanning, and export actions.
- `AddExpenseActivity.kt` – Add and save expense records.
- `res/layout/` – Application UI layouts.


## Screenshots

### 1. Dashboard
![Dashboard](https://github.com/Kirtisharma123/Smart-Expense-Tracker-Android/blob/main/Dashboard.jpeg)

### 2. Add Expense
![Add Expense](https://github.com/Kirtisharma123/Smart-Expense-Tracker-Android/blob/main/Add%20Expense.jpeg)

### 3. Analytics
![Analytics](https://github.com/Kirtisharma123/Smart-Expense-Tracker-Android/blob/main/Analytics.jpeg)

### 4. Receipt Scanner
![Receipt Scanner](https://github.com/Kirtisharma123/Smart-Expense-Tracker-Android/blob/main/Receipt%20Scanner.jpeg)

### 5. Transactions
![Transactions](https://github.com/Kirtisharma123/Smart-Expense-Tracker-Android/blob/main/Transactions.jpeg)

### 6. Export as Excel
![Export as Excel Sample](https://github.com/Kirtisharma123/Smart-Expense-Tracker-Android/blob/main/Export%20as%20excel%20sample.jpeg)

### 7. Export as PDF
![Export as PDF Sample](https://github.com/Kirtisharma123/Smart-Expense-Tracker-Android/blob/main/Export%20pdf%20sample.jpeg)


## Installation

1. Clone this repository.
2. Open the project in Android Studio.
3. Allow Gradle sync to complete.
4. Connect an Android device or start an emulator.
5. Run the application.

## Project Purpose

This project was developed as part of an Android Development Internship to demonstrate practical skills in Kotlin, database management, OCR integration, data visualization, and report generation.

## Developer

Android Development Project – Smart Expense Tracker
