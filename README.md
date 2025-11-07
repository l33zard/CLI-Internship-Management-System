<h1 align="center">Internship Management System (IMS)</h1>

<div align="center">
<p>SC2002 Object-Oriented Design & Programming | AY2024/25 Semester 2</p>

[![Javadoc Badge](https://img.shields.io/badge/Javadoc-F8981D?style=for-the-badge&logo=readthedocs&logoColor=FFFFFF&labelColor=222222)](#)
&nbsp;
[![Class Diagrams Badge](https://img.shields.io/badge/Class%20Diagrams-C2F0C0?style=for-the-badge&logo=diagramsdotnet&logoColor=FFFFFF&labelColor=222222)](#)
&nbsp;
[![Sequence Diagrams Badge](https://img.shields.io/badge/Sequence%20Diagrams-FFF6B6?style=for-the-badge&logo=miro&logoColor=222222&labelColor=222222)](#)

👆 Click the buttons above to view documentation and diagrams 👆

<p align="center">
<a href="#introduction">Introduction</a> &nbsp;&bull;&nbsp;
<a href="#project-structure">Project Structure</a> &nbsp;&bull;&nbsp;
<a href="#team-members">Team Members</a> &nbsp;&bull;&nbsp;
<a href="#features">Features</a> &nbsp;&bull;&nbsp;
<a href="#getting-started">Getting Started</a>
</p>
</div>

---

## Introduction
The **Internship Management System (IMS)** is a Java-based Command Line Interface (CLI) application designed to manage internship placements between students, company representatives, and the career center.  
It was developed as part of the **SC2002 Object-Oriented Design & Programming** module at **Nanyang Technological University, Singapore**, and demonstrates key OOP principles such as encapsulation, abstraction, inheritance, and polymorphism.

---

## Project Structure

📦 Internship-Management-System
├── src/
│ ├── app/ # Main entry point (Main.java)
│ ├── boundary/ # User interface logic (CLI)
│ ├── controller/ # Application logic and flow control
│ ├── database/ # Repositories for persistent data handling
│ ├── entity/ # Core classes and enumerations
│ ├── util/ # Utility classes and helpers
│ ├── data/ # CSV files or seed data
│
├── .gitignore
├── README.md


---

## Team Members

| **Name**        | **GitHub Profile** | **Email Address** |
|-----------------|--------------------|-------------------|
| Lee Xun         | [![GitHub Badge](https://img.shields.io/badge/l33zard-%23181717?logo=github)](https://github.com/l33zard) | xlee065@e.ntu.edu.sg |
| Matthias Kwan   | [![GitHub Badge](https://img.shields.io/badge/holuuu17-%23181717?logo=github)](https://github.com/holuuu17) | matt0076@e.ntu.edu.sg |
| Fong Yee Xin    | [![GitHub Badge](https://img.shields.io/badge/nixE911-%23181717?logo=github)](https://github.com/nixE911) | yfong009@e.ntu.edu.sg |
| Daniel Law      | [GitHub Link] | [Email] |

---

## Features

### **System**
- Login authentication for multiple user roles.
- View and update personal profile.
- Change password securely.
- Persistent data through CSV-based repositories.

### **Student**
- View available internships.
- Apply for internship postings.
- Track application status.
- Request withdrawal of applications.

### **Company Representative**
- Register new internship postings.
- Review student applications.
- Approve or reject applicants.
- Manage internship records.

### **Career Center Staff**
- Manage company registrations and student applications.
- Approve or reject withdrawal requests.
- Oversee system data and generate reports.

---

## Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/l33zard/SC2002-internship-management-system.git
cd SC2002-internship-management-system
```

### 2. Compile the Java Source Files

Compile all Java files in src and store the compiled classes in bin:

```bash
javac -d ./bin ./src/**/*.java
```

If you are on PowerShell (Windows), use:

```powershell
Get-ChildItem -Recurse -Filter *.java -Path .\src | ForEach-Object { javac -d .\bin $_.FullName }
```

### 3. Run the Application

Execute the main class located in src/app/Main.java:

```bash
java -cp ./bin app.Main
```

### 4. Alternatively, Compile & Run in One Step

For Mac/Linux:

```bash
javac -d ./bin ./src/**/*.java && java -cp ./bin app.Main
```

For PowerShell:

```powershell
Get-ChildItem -Recurse -Filter *.java -Path .\src | ForEach-Object { javac -d .\bin $_.FullName }; ja
```


