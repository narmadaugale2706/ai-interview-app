# 🚀 AI Mock Interview Simulator

An AI-powered full-stack application that simulates real interview experiences by generating role-based technical questions and evaluating answers using advanced AI (NVIDIA Nemotron).

This project is designed to help developers prepare for technical interviews with **real-time feedback, scoring, and improvement suggestions**.

---

## 🔥 Features

* 🎯 Generate **role-based technical interview questions**
* 🤖 AI-powered **answer evaluation**
* 📊 Intelligent scoring system (0–10)
* ✅ Highlights **strengths**
* ❌ Identifies **missing concepts**
* ✍️ Provides **improved answers**
* ⚡ Fast API calls using **Spring WebClient (Reactive)**
* 🎨 Clean and responsive **Angular UI**
* 🔁 Single-question flow for better performance

---

## 🛠 Tech Stack

### 👨‍💻 Frontend

* Angular
* TypeScript
* HTML, CSS
* Bootstrap

### ⚙️ Backend

* Spring Boot
* Java
* WebClient (Reactive HTTP calls)
* REST APIs

### 🤖 AI Integration

* NVIDIA Nemotron API (LLM)

---

## 📂 Project Structure

```
ai-interview-app/
│
├── frontend/                 # Angular Application
│   ├── src/app/
│   └── ...
│
├── backend/                 # Spring Boot Application
│   ├── src/main/java/
│   └── ...
│
└── README.md
```

---

## ⚙️ Setup Instructions

### 🔹 1. Clone Repository

```
git clone https://github.com/narmada2706/ai-interview-app.git
cd ai-interview-app
```

---

## 🔹 2. Backend Setup (Spring Boot)

### 👉 Navigate to backend:

```
cd backend
```

### 👉 Run application:

```
./gradlew bootRun
```

### ✅ Backend runs on:

```
http://localhost:8080
```

---

## 🔹 3. Frontend Setup (Angular)

### 👉 Navigate to frontend:

```
cd frontend
```

### 👉 Install dependencies:

```
npm install
```

### 👉 Run application:

```
ng serve
```

### ✅ Frontend runs on:

```
http://localhost:4200
```

---

## 🔐 Environment Variables

Create environment variable for NVIDIA API key:

```
API_KEY=your_nvidia_api_key
```

### 🔹 Usage in code:

```java
String apiKey = System.getenv("API_KEY");
```

---

## 🌐 API Endpoints

| Endpoint             | Method | Description                          |
| -------------------- | ------ | ------------------------------------ |
| `/generate-question` | POST   | Generate a single interview question |
| `/evaluate-answers`  | POST   | Evaluate user answer with AI         |

---

## 📸 Screenshots

> (Add screenshots here for better impact)

Example:

* Home Screen
* Question Screen
* Feedback Screen

---

## ⚡ Application Flow

1. User enters job role
2. AI generates interview question
3. User submits answer
4. AI evaluates answer
5. Feedback displayed with score and improvements

---

## 🚀 Future Enhancements

* 💬 ChatGPT-style conversational UI
* 🎤 Voice-based interview system
* 📈 Performance analytics dashboard
* 🔁 Multi-question adaptive interview
* 🌍 Cloud deployment (Render / Railway / AWS)
* 🔐 Authentication & user tracking

---

## 💡 Key Highlights

* Real-world AI integration (NVIDIA Nemotron)
* Full-stack architecture (Angular + Spring Boot)
* Clean separation of frontend and backend
* Production-ready API design
* Interview-focused practical use case

---

## ⚠️ Known Issues

* AI response latency may vary depending on model
* Free-tier API usage may have limits

---

## 🧪 Testing

You can test APIs using:

* Postman
* Curl
* Frontend UI

---

## 👩‍💻 Author

**Narmada Ugale**

* Java Backend Developer
* Angular Developer
* Passionate about AI & scalable systems

---

## 🌟 Show Your Support

If you found this project helpful:

* ⭐ Star this repository
* 🍴 Fork it
* 📢 Share with others

---

## 📌 License

This project is for educational and demonstration purposes.

---

## 💬 Feedback

Feel free to raise issues or suggestions to improve the project.
