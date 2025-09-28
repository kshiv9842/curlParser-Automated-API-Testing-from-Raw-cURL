# 🚀 curlParser - Setup Instructions for Teammates

## 📦 What You're Getting
- **JAR File**: `rest-assured-framework-1.0-SNAPSHOT.jar` (42.7 MB)
- **Self-contained**: All dependencies included
- **Ready to run**: No additional setup required

## 🔧 Prerequisites
- **Java 17** or higher installed on your machine
- **VPN access** to your company's internal APIs (if required)

## 📋 Quick Setup (3 Steps)

### Step 1: Download the JAR File
```bash
# Download the JAR file to your local machine
# File: rest-assured-framework-1.0-SNAPSHOT.jar (42.7 MB)
```

### Step 2: Connect to VPN (if required)
```bash
# Connect to your company VPN before running the application
# This ensures access to internal APIs
```

### Step 3: Run the Application
```bash
# Navigate to the directory containing the JAR file
cd /path/to/your/jar/directory

# Run the application
java -jar rest-assured-framework-1.0-SNAPSHOT.jar
```

## 🌐 Access the Application
Once running, open your browser and go to:
```
http://localhost:8080
```

## 🧪 How to Use

### 1. **Paste Your cURL Command**
- Copy any cURL command from your API documentation
- Paste it into the text area on the web interface

### 2. **Click Execute**
- The system will automatically run 15+ test scenarios
- Results will appear in collapsible sections

### 3. **Review Results**
- ✅ **Green**: Test passed
- ❌ **Red**: Test failed  
- ⚠️ **Orange**: Test skipped

## 🔍 Test Scenarios Included

The system automatically runs these tests:

1. **Basic Smoke Test** - Validates API with original request
2. **Invalid Payload Body** - Tests with corrupted JSON
3. **Missing Headers** - Removes all headers
4. **Missing Authentication** - Removes auth headers
5. **Unsupported HTTP Method** - Uses wrong HTTP method
6. **Missing Request Body** - Removes body for POST/PUT
7. **Invalid Path Parameters** - Corrupts path parameters
8. **Invalid Query Parameters** - Adds malformed query params
9. **Missing Query Parameters** - Removes all query params
10. **Wrong Content-Type** - Uses incorrect media type
11. **Malformed JSON** - Sends invalid JSON syntax
12. **Oversized Payload** - Sends extremely large payloads
13. **SQL Injection** - Tests SQL injection patterns
14. **Special Characters** - Tests Unicode and special chars
15. **Empty Values** - Tests with null/empty field values

## 🛠️ Troubleshooting

### Port Already in Use
```bash
# If port 8080 is busy, use a different port
java -jar rest-assured-framework-1.0-SNAPSHOT.jar --server.port=8081
```

### Java Version Issues
```bash
# Check your Java version
java -version

# Should show Java 17 or higher
# If not, install Java 17 from: https://adoptium.net/
```

### VPN Connection Issues
```bash
# Make sure you're connected to VPN before starting the application
# The application will use your VPN connection to access internal APIs
```

## 📱 Example Usage

### Sample cURL Command:
```bash
curl -X POST https://api.example.com/users \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com"}'
```

### What Happens:
1. System parses the cURL command
2. Runs 15 automated test scenarios
3. Shows detailed results for each test
4. Identifies potential API vulnerabilities

## 🎯 Benefits

- **Zero Manual Test Creation** - Just paste cURL, get comprehensive tests
- **Automated Negative Testing** - Generates edge cases automatically  
- **Security Testing** - Built-in SQL injection and security tests
- **Developer Friendly** - Simple web interface, no complex setup
- **Comprehensive Coverage** - 15 different test scenarios per API

## 📞 Support

If you encounter any issues:
1. Check Java version (must be 17+)
2. Ensure VPN connection is active
3. Verify port 8080 is available
4. Contact the development team for assistance

## 🚀 Ready to Test!

You're all set! Just run the JAR file and start testing your APIs with comprehensive automated test suites.


