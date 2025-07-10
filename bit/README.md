

# 🧠 **Bit – A Git-Like Version Control Tool in Java**

Bit is a minimal, Git-like version control system built from scratch in Java, with GitHub integration for remote operations. It mimics basic Git functionality with simplified commands and a custom internal structure.

---

## ✅ **1. Getting Started**

### 🛠️ Requirements

* Java 17+
* Maven
* GitHub Personal Access Token (PAT)

---

### 🔁 Clone & Build

```bash
git clone https://github.com/your-username/bit.git
cd bit
mvn clean package
```

The following file will be created:

```
target/bit-1.0-SNAPSHOT-shaded.jar
```

Use the **shaded JAR** — it contains all required libraries (like `org.json`).

---

## ⚙️ **2. Setup CLI Access on Windows**

### 📝 Create `bit.bat`

Make a batch file named `bit.bat`:

```bat
@echo off
java -jar "C:\bit\bit-1.0-SNAPSHOT-shaded.jar" %*
```

> Replace the path with your actual `.jar` file location.

### ➕ Add to System PATH

1. Go to **Environment Variables**
2. Under **System Variables → Path → Edit → New**
3. Add the folder path where `bit.bat` is saved

Now you can use `bit` like any global CLI command.

---

## 💻 **3. Bit Commands vs Git Commands**

| Git Command               | Bit Equivalent                         |
| ------------------------- | -------------------------------------- |
| `git init`                | `bit start`                            |
| `git add .`               | `bit stage .`                          |
| `git commit -m "msg"`     | `bit save -m "msg"`                    |
| `git remote add origin`   | `bit remote add origin <url>`          |
| `git push`                | `bit upload --token=YOUR_GITHUB_TOKEN` |
| `git pull`                | `bit pull --token=YOUR_GITHUB_TOKEN`   |
| `git status`              | `bit check`                            |
| `git reset --soft HEAD~1` | `bit undo`                             |
| `.gitignore`              | `.bitignore`                           |

---

## 📁 **4. `.bit/` Directory Structure Explained**

The `.bit` directory acts as your project's internal database, much like `.git/`.

```
.bit/
├── config              # Stores remote GitHub repo URL
├── index               # Tracks staged files: <hash> <file path>
├── HEAD                # Points to the latest commit hash
├── refs/
│   └── heads/
│       └── main        # Stores the latest commit hash of the 'main' branch
├── objects/
│   └── <hash>          # Stores raw file data or commit/tree objects
└── ignore              # Loaded from .bitignore for ignored files
```

### 📄 `.bit/index`

* Stores **staged files** as:

  ```
  <hash> <relative/path/to/file>
  ```
* When you run `bit stage .`, this is what gets populated.

---

### 📄 `.bit/HEAD`

* Contains the **commit hash of the current HEAD** (most recent commit).
* Used by `bit check`, `bit save`, and `bit undo`.

---

### 📁 `.bit/objects/`

* Stores the **actual file content** of tracked objects (blobs, commits, trees).
* Named by their SHA-1 hash (like Git).

---

### 📁 `.bit/refs/heads/main`

* A named reference that stores the latest commit hash for a branch.
* Used by `bit check`, `bit merge`, etc.

---

### 📄 `.bit/config`

* Stores the GitHub remote repo URL, e.g.:

  ```
  https://github.com/username/repo.git
  ```

---

## 🧪 **5. Example Workflow**

```bash
bit start
bit stage .
bit save -m "initial commit"
bit remote add origin https://github.com/your-name/repo.git
bit upload --token=ghp_yourtoken
```

To pull:

```bash
bit pull --token=ghp_yourtoken
```

---

## 🆕 **6. New Features Added**

### ✅ 1. Human-Readable Command Aliases

Simplified CLI: `bit start`, `bit stage`, `bit save` instead of verbose Git commands.

---

### ✅ 2. Smart Merge Conflict Resolver (Planned)

Upcoming feature:

* Choose **branch preference** (main or feature)
* Resolve **conflict line-by-line**

---

### ✅ 3. Safe `push --force` (Planned)

* Takes a backup before overwriting remote.
* Prevents accidental loss.

---

## ⚠️ **7. Troubleshooting**

### ❌ `index` is empty after staging?

* Check your `.bitignore`
* Run: `bit stage .`
* Then: `bit check` to verify what's tracked

---

### ❌ Push fails with malformed path?

* Use a `.bitignore` to skip `.venv/`, `.idea/`, etc.
* Avoid using absolute paths or special chars

---

## ❓ **8. How to Make Bit Publicly Usable**

You can share this tool by:

1. Publishing the JAR in a GitHub repo (e.g. Releases tab)
2. Share `bit.bat` and setup steps
3. Optional: Create an installer script (`setup.ps1` for PowerShell)
4. Add documentation (`README.md`) for users to set it up

---


