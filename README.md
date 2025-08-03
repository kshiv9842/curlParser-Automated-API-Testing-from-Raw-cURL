# 🧪 curlParser — Automated API Testing from Raw cURL

**curlParser** is a developer-friendly tool that takes raw `curl` commands as input and automatically:
- Parses the command into method, URL, headers, and body
- Sends the request using an internal client
- Generates **negative test cases** automatically
- Validates **response structures against JSON Schemas**
- (Optionally) Validates **request payloads** before sending

> No more repetitive Postman clicks or fragile test scripts. Just paste your `curl`, and you're ready to test.

---

## 📦 Features

✅ **Parse raw `curl` into structured API request**  
✅ **Support for headers, payloads, methods (GET, POST, PUT, DELETE)**  
✅ **Auto-generate negative test cases:**  
   - Missing headers
   - Unauthorized access
   - Invalid or missing payload fields  
✅ **Validate response using JSON Schema**  
✅ (Optional) Validate request body before sending  
✅ Plug-and-play: easily integrate with test runners like JUnit, TestNG, etc.

---

## 🚀 Getting Started

### 1. Paste a `curl` command

```bash
curl -X POST https://api.example.com/user \
  -H "Authorization: Bearer xyz" \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "alice@example.com"}'
