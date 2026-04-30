# Multi-Threaded File Downloader

This project is a robust, multi-threaded downloader implementation. The application supports segmented (chunked) parallel downloading with automatic file merging upon completion.

##  Features
*   *Parallel Downloading:* Files are split into segments and downloaded simultaneously using concurrency (Coroutines).
*   *Progress Tracking:* Real-time display of download progress in bytes.
*   *Error Handling:* Automatic cleanup of corrupted or partial files if an error occurs during the process.
*   *JUnit 5 Tests:* Comprehensive test coverage for successful downloads, non-existent URLs, and disk write errors.

## How to Run

The application accepts two command-line arguments: the **File URL** and the **Local Destination Path**.

    gradlew run --args="<URL> <DESTINATION_PATH>"

Example
```bash
gradlew run --args="http://localhost:8080/sample.zip C:/Users/YourUser/Desktop/downloaded.zip"
```
Note: If the destination path contains spaces, ensure it is enclosed in quotes within the arguments string.


 Running Tests
 

```bash
    gradlew clean test
```
The test report can be found at: build/reports/tests/test/index.html

Code was tested by running local apache server with docker
```bash
  docker run --rm -p 8080:80 -v "${PWD}/docker-shared:/usr/local/apache2/htdocs/" httpd:latest
```

**Commands are formatted for Windows/PowerShell; Unix-based OS users should adapt the syntax (e.g., shell script execution and path slashes).** 
