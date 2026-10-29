# NSync Backend: Lightweight OCR & Seamless IMS Authentication

A lightweight Node.js/Express service that connects the NSync Android/WebView student companion app with the NSUT IMS portal, featuring secure session persistence and an isolated local OCR engine.

---

## Key Features

1. **Seamless IMS Authentication & Accessible OCR Assistance**:
   - Supports official IMS portal challenges with accessible inline rendering.
   - **OCR Form Assistance**: Preprocesses portal verification images using OpenCV and Tesseract to streamline student authentication.
   - Supports inline image refresh, timeout handling, and descriptive error messages.
2. **Secure Session Reuse**:
   - Stores session cookies (`PHPSESSID`, etc.) securely in backend memory with a configurable TTL (default 30 mins).
   - Issues opaque UUID bearer tokens to the mobile app; **never exposes IMS cookies or raw credentials** to the frontend.
   - Reuses active authorized sessions when navigating between timetable, attendance, and dashboard screens.
3. **Lightweight, Local OCR Pipeline**:
   - Built with **Python OpenCV** and **Tesseract OCR**.
   - **No cloud vision APIs** or heavyweight real-time AI models.
   - Memory-efficient streaming architecture: stdin/stdout pipes with **zero temporary files**.
   - Automatic image preprocessing: grayscale conversion, adaptive thresholding, aspect-ratio-preserving resizing, and median noise reduction.
   - Non-blocking execution with watchdog timeouts and graceful fallback if Tesseract is not installed.

---

## Architecture & Workflows

### 1. Authentication Lifecycle
```
[Android App]                      [NSync Backend]                       [NSUT IMS Portal]
      |                                   |                                      |
      |--- POST /v1/auth/challenges ----->|                                      |
      |                                   |--- GET student.htm & captchaimg ---->|
      |                                   |<-- Session Cookie & Captcha Image ---|
      |<-- challengeId + Base64 Captcha --|                                      |
      |                                   |                                      |
[User solves CAPTCHA]                     |                                      |
      |                                   |                                      |
      |--- POST /v1/auth/login ---------->|                                      |
      |    (roll, pass, captchaAnswer)    |--- POST studentlogin.php ----------->|
      |                                   |<-- Authenticated IMS Cookies --------|
      |                                   | [Store cookies in memory keyed by UUID]
      |<-- Opaque Token (No Cookies!) ----|                                      |
      |                                   |                                      |
      |--- GET /v1/academic/attendance -->|                                      |
      |    (Authorization: Bearer token)  |--- GET student_attendance.php ------>|
      |                                   |    (Reusing cached IMS cookies)      |
      |<-- Attendance Data ---------------|<-- Raw Attendance Data -------------|
```

### 2. Document/Screenshot OCR Workflow
```
[Mobile App / Upload]              [NSync Backend]                       [Local Engines]
      |                                   |                                      |
      |--- POST /v1/ocr/extract --------->|                                      |
      |    (Base64 Image <= 5MB)          |-- Magic Number & Size Validation ----|
      |                                   |-- Stream buffer to OpenCV preproc -->|
      |                                   |<-- Preprocessed PNG bytes -----------|
      |                                   |-- Stream buffer to Tesseract CLI --->|
      |                                   |<-- Extracted UTF-8 Text -------------|
      |<-- Extracted Text & Metadata -----|                                      |
```

---

## Installation & Deployment Prerequisites

### 1. Windows Environment (Current Host)

#### Prerequisites
- **Node.js**: v18.0.0 or later (v24+ recommended)
- **Python**: 3.9+ with OpenCV and Pillow:
  ```powershell
  pip install opencv-python numpy pillow
  ```
- **Tesseract OCR**:
  - Install via Winget:
    ```powershell
    winget install --id UB-Mannheim.TesseractOCR -e
    ```
  - Or download installer from: [UB-Mannheim Tesseract Releases](https://github.com/UB-Mannheim/tesseract/releases)
  - Default installation directory: `C:\Program Files\Tesseract-OCR\tesseract.exe`
  - If installed in a custom directory, set `TESSERACT_PATH` in your `.env` file:
    ```env
    TESSERACT_PATH=C:\Program Files\Tesseract-OCR\tesseract.exe
    ```

### 2. Linux / Production Container (Ubuntu/Debian)

```bash
# 1. System packages
sudo apt-get update
sudo apt-get install -y tesseract-ocr tesseract-ocr-eng libgl1-mesa-glx

# 2. Python environment
pip install opencv-python-headless numpy pillow

# 3. Node.js backend
cd server
npm install
```

### 3. macOS Environment

```bash
brew install tesseract
pip install opencv-python-headless numpy pillow
cd server && npm install
```

---

## Configuration (`.env`)

Copy `.env.example` to `.env`:

```env
# Server
PORT=3000
NODE_ENV=development

# NSUT IMS Portal
IMS_BASE_URL=https://imsnsit.org/imsnsit
IMS_MOCK=false

# OCR Configuration
PYTHON_PATH=python
TESSERACT_PATH=tesseract
TESSERACT_LANG=eng
TESSERACT_PSM=3
OCR_TIMEOUT_MS=8000
```

*Note: Set `IMS_MOCK=true` for isolated offline development and testing.*

---

## API Reference

### Authentication
- `POST /v1/auth/challenges`: Generates a fresh official CAPTCHA challenge.
  - **Response**: `{ success: true, challengeId: "...", captchaImage: "data:image/jpeg;base64,...", expiresAt: 171... }`
- `POST /v1/auth/challenges/refresh`: Refreshes an existing challenge.
  - **Body**: `{ "challengeId": "..." }`
- `POST /v1/auth/login`: Authenticates user credentials and CAPTCHA answer.
  - **Body**: `{ "challengeId": "...", "username": "2021UCO...", "password": "...", "captchaAnswer": "..." }`
  - **Response**: `{ "success": true, "token": "<opaque_uuid>", "user": { "rollNumber": "..." }, "expiresAt": ... }`
- `POST /v1/auth/renew`: Renews session expiration time.
  - **Headers**: `Authorization: Bearer <token>`
- `POST /v1/auth/logout`: Revokes the session.
  - **Headers**: `Authorization: Bearer <token>`

### Academic (Protected)
- `GET /v1/academic/attendance`: Retrieves attendance records using the cached IMS session without re-authenticating.
  - **Headers**: `Authorization: Bearer <token>`
- `GET /v1/academic/timetable`: Retrieves student timetable reusing the cached session.
  - **Headers**: `Authorization: Bearer <token>`

### OCR Extraction
- `POST /v1/ocr/extract`: Extracts text from an image/screenshot.
  - **Body**:
    ```json
    {
      "imageBase64": "data:image/png;base64,...",
      "lang": "eng",
      "psm": 3,
      "preprocess": true,
      "preprocessOptions": {
        "adaptiveThreshold": false,
        "denoise": false,
        "resizeMaxDim": 2000
      }
    }
    ```
  - **Response**:
    ```json
    {
      "success": true,
      "data": {
        "text": "Extracted text here",
        "durationMs": 142,
        "preprocessed": true,
        "lang": "eng",
        "psm": "3"
      }
    }
    ```
- `GET /v1/ocr/status`: Health check confirming Tesseract binary presence and settings.

---

## Running Tests

Run the complete Node test suite:

```bash
npm test
```

The test suite covers:
- Session creation, isolation, cookie safety, and TTL expiration.
- Image buffer magic byte validation, size limit enforcement (5MB), and OpenCV preprocessing.
- Tesseract missing binary handling, timeout watchdog, and text recognition.
- End-to-end authentication challenges, challenge validation, credential verification, and session reuse.
