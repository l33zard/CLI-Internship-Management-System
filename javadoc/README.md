# 🚀 Quick Start - View API Documentation

## Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/internship-management-system.git
cd internship-management-system
```

## Step 2: Open the Documentation

The HTML documentation is already generated and ready to view!

### Windows
```bash
start javadoc\index.html
```

### Mac
```bash
open javadoc/index.html
```

### Linux
```bash
xdg-open javadoc/index.html
```

### Or Just Double-Click
Navigate to the `javadoc` folder and double-click `index.html`

---

## 📖 What You'll See

- **Entity Package**: Student, Internship, Application classes
- **Controller Package**: Business logic and workflows  
- **Database Package**: Data repositories and CSV handling

---

## 🎮 Running the Application

### Requirements
- Java JDK 11 or higher

### Steps

1. **Compile**
   ```bash
   javac -d bin -sourcepath src src/app/Main.java
   ```

2. **Run**
   ```bash
   java -cp bin app.Main
   ```

3. **Login** with default credentials:
   - Student: `U1234567A` / `password`
   - Staff: `STAFF001` / `password`

---

## 📁 Important Files

- `javadoc/index.html` - **API Documentation** ⭐ START HERE
- `src/app/Main.java` - Application entry point
- `data/*.csv` - System data files
- `README.md` - Full documentation

---

## 💡 That's It!

The documentation is pre-generated, so you can view it immediately without any setup.

**Just open `javadoc/index.html` and you're good to go!** 🎉
