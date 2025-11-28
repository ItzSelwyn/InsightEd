# Firebase (Realtime Database)

This folder contains all Firebase-related configuration files for the BLE Attendance System.  
The project uses **Firebase Realtime Database** as a backend to store:

- Attendance logs
- Student details
- Period/subject information
- user accounts for dashboards

The ESP32 writes attendance using **anonymous authentication**, while all data is publicly readable for dashboards and apps.

---

## 📂 Files in This Folder

### **rules.json**
Defines the security rules used for this Firebase Realtime Database.

These rules enforce:

- **Public READ access** – Anyone can read database data  
- **Restricted WRITE access** – Only authenticated devices (like the ESP32) can write to:
