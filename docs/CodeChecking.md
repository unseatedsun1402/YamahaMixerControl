# Java Code Formatting Guide

This project uses **google‑java‑format** to ensure that all Java code follows a consistent, automatic, and deterministic style.  
The formatter is cross‑platform and works on Windows, macOS, and Linux.

Formatting is **mandatory** for all contributions.

---

## Why we use google‑java‑format

- Eliminates style debates  
- Ensures consistent formatting across all contributors  
- Makes diffs cleaner and easier to review  
- Works identically on every platform  
- Safe: it only reformats valid Java code, never changes logic  

---

# 1. Installing the Formatter

Download the **Java‑8‑compatible** version of google‑java‑format:

**google-java-format‑1.7‑all‑deps.jar**  
https://github.com/google/google-java-format/releases/tag/v1.7

Place it in:

```
tools/google-java-format.jar
```

This repository expects the formatter to live in that location.

---

# 2. Checking Your Java Version

This version of the formatter requires **Java 8** or newer.

Check your version:

```
java -version
```

If you see:

```
1.8.x
```

or anything newer (11, 17, 21), you’re good.

---

# 3. Formatting a Single File

```
java -jar tools/google-java-format.jar -i path/to/File.java
```

The `-i` flag means “edit the file in place”.

---

# 4. Formatting the Entire Project (Windows PowerShell)

PowerShell does not support `**/*.java` globbing, so use this:

```powershell
Get-ChildItem -Recurse -Filter *.java | ForEach-Object {
    java -jar tools/google-java-format.jar -i $_.FullName
}
```

This will format every `.java` file under `src/`.

---

# 5. Formatting the Entire Project (Linux/macOS)

```bash
java -jar tools/google-java-format.jar -i $(find src -name "*.java")
```

---

# 6. Dry‑Run Mode (see what would change)

```
java -jar tools/google-java-format.jar --dry-run --set-exit-if-changed src/**/*.java
```

Useful for CI or pre‑commit hooks.

---

# 7. Editor Integration (Optional)

Most editors support google‑java‑format:

### VS Code
Install: *Google Java Format* extension  
Configure it to use the JAR in `/tools`.

### IntelliJ IDEA
Install plugin: *google-java-format*  
Set the path to the JAR.

### Vim / Neovim
Use:

```
:%!java -jar tools/google-java-format.jar -
```

---

# 8. Common Issues

### “UnsupportedClassVersionError”
You downloaded a formatter built for a newer Java version.  
Use version **1.7‑all‑deps**.

### “No files were provided”
You ran the formatter without specifying files.  
Use the recursive commands above.

### Formatter reports syntax errors
The formatter only works on valid Java.  
Fix the syntax error first, then re‑run.

---

# 9. Recommended Workflow

1. Write code  
2. Run the formatter  
3. Run tests  
4. Commit  

This keeps the codebase clean and consistent.

# Python Code Formatting Guide

This project uses **Black**, **isort**, and **Flake8** to ensure that all Python code is consistently formatted, import‑clean, and lint‑checked across all platforms (Windows, macOS, Linux).

Formatting is **mandatory** for all contributions.

---

## Why we use these tools

### **Black**
- Deterministic, opinionated formatter  
- Removes all style debates  
- Makes diffs clean and predictable  

### **isort**
- Automatically sorts imports  
- Groups standard library, third‑party, and local imports  
- Prevents messy or inconsistent import blocks  

### **Flake8**
- Linting for unused variables, undefined names, spacing issues  
- Helps catch real bugs early  
- Enforces basic style consistency  

Together, these tools give us a clean, readable, and maintainable Python codebase.

---

# 1. Installing the Tools

Install all three tools with pip:

```
pip install black isort flake8
```

Or, if using a virtual environment:

```
python -m pip install black isort flake8
```

---

# 2. Formatting a Single File

### Black:
```
black path/to/file.py
```

### isort:
```
isort path/to/file.py
```

---

# 3. Formatting the Entire Project

### Black:
```
black .
```

### isort:
```
isort .
```

These commands recursively format all Python files in the project.

---

# 4. Linting the Project

Run Flake8:

```
flake8 .
```

This will report:

- unused imports  
- undefined variables  
- spacing issues  
- overly long lines  
- syntax errors  

---

# 5. Recommended Workflow

1. Write code  
2. Run `isort .`  
3. Run `black .`  
4. Run `flake8 .`  
5. Commit  

This ensures your code is clean, consistent, and passes linting before it enters the repository.

---

# 6. Editor Integration (Optional)

### VS Code
Install extensions:
- **Black Formatter**
- **isort**
- **Flake8**

Enable format‑on‑save.

### PyCharm
Black and isort can be configured as external tools or via plugins.

### Vim / Neovim
Use:

```
:%!black -
```

or integrate via ALE or null‑ls.

---

# 7. Pre‑Commit Hook (Optional)

To enforce formatting automatically before every commit:

Create `.git/hooks/pre-commit`:

```bash
#!/bin/sh
isort .
black .
flake8 .
```

Make it executable:

```
chmod +x .git/hooks/pre-commit
```

---

# 8. Common Issues

### “Black reformatted X files”
This is normal — Black rewrites files to match its style.

### “Flake8: E501 line too long”
Black does not enforce line length on comments or strings.  
You can adjust Flake8’s max line length in `setup.cfg` if needed.

### “Import order errors”
Run `isort .` again.

---

# 9. Summary

- **Black** → formats code  
- **isort** → sorts imports  
- **Flake8** → linting and error detection  

Run all three before committing to keep the codebase clean and consistent.