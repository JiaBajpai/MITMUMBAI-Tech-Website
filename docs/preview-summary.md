# MIT TECH KERNEL
## Technical Club Website & Members Corner — Project Summary

## 1. Project Overview

**MIT TECH KERNEL** is a technical club website designed to serve two purposes:

1. Provide a professional public-facing website for the technical club.
2. Provide a secure, authenticated **Members Corner** for managing technical sessions, attendance, resources, users, and documentation.

---

# 2. Website Structure

The overall website is divided into two major sections:

```text
MIT TECH KERNEL
│
├── Public Website
│   ├── Home
│   ├── About
│   ├── Technical Domains
│   ├── Team
│   ├── Events
│   ├── Activities
│   ├── Contact
│   └── Members Corner / Login
│
└── Members Corner
    ├── Authentication
    ├── Domains
    ├── Sessions
    ├── Attendance
    ├── Resources
    ├── Summary Generation
    └── User Management
```

---

# 3. Technical Domains

The system contains five fixed technical domains:

1. **General**
2. **Frontend**
3. **Backend**
4. **AI/ML/IoT**
5. **Competitive Programming**

### General

The common club-wide domain for orientations, introductory sessions, interdisciplinary topics, and activities relevant to the entire club.

### Frontend

Covers user-facing technologies such as web development, interfaces, UI implementation, and related frontend technologies.

### Backend

Covers server-side development, APIs, databases, authentication, application architecture, and related technologies.

### AI/ML/IoT

Covers artificial intelligence, machine learning, Internet of Things, experimentation, and related technologies.

### Competitive Programming

Covers algorithms, data structures, problem solving, programming contests, and computational thinking.

---

# 4. Members Corner

The Members Corner is the authenticated internal platform.

Users log in using:

- **Unique User ID**
- **Password**

Each user receives a unique identifier such as:

```text
KRN0001
KRN0002
KRN0003
```

The User ID is used for authentication and for associating users with attendance records.


There are four roles:

- **STUDENT**
- **FACULTY**
- **LEAD**
- **SUPER ADMIN**

---

# 5. Student Role

Students are normal club members and have primarily read-only access.

A Student can:

- View all five domains.
- Open any domain.
- View all sessions.
- Open complete session details.
- View their own attendance.

Session details available to a student include:

- Session title
- Domain
- Date
- Day
- Start time
- End time
- Duration
- Leads
- Lead designations
- Concise description
- Resources
- Their attendance status

Students cannot:

- Add sessions.
- Edit sessions.
- Remove sessions.
- Mark or edit attendance.
- View other students' attendance.
- Generate summary reports.
- Manage users.

The intended student experience is therefore simple: **view the club's technical activities and track personal attendance.**

---

# 6. Faculty Role

Faculty members have global read access to the Members Corner.

Faculty can:

- View all five domains.
- View all sessions.
- View complete session details.
- View attendance for all sessions and members.
- Generate summary reports.

Faculty cannot:

- Add, edit, or remove sessions.
- Mark or modify attendance.
- Manage users.
- Assign Lead domains.

---

# 7. Lead Role

Leads are responsible for managing technical sessions.

A Lead can be assigned one or more technical domains.

Lead accounts can be assigned one or more technical domains, such as Frontend, Backend, or AI/ML/IoT.

Every Lead automatically receives management access to **General**, in addition to their assigned domain or domains.

A Frontend Lead can manage General and Frontend, but cannot modify Backend unless Backend is also assigned.

### Lead viewing permissions

Leads can view:

- All five domains.
- All sessions.
- Complete session details.
- All attendance.
- Resources.
- Session leads and descriptions.

### Lead management permissions

Leads can:

- Add sessions.
- Edit sessions.
- Remove sessions.
- Mark attendance.
- Edit attendance.
- Correct/remove attendance records.

These management actions are restricted to:

```text
General + assigned domain(s)
```

### Lead reporting permissions

Leads can generate summary reports for **any combination of all five domains**, even when those domains are outside their editing permissions.

Thus, Leads have global viewing/reporting access but domain-limited editing access.

---

# 8. Super Admin Role

The Super Admin is specifically responsible for **user/account management**, not general administration.

The Super Admin can perform CRUD operations on user details:

- **Create**
- **Read**
- **Update**
- **Delete/Remove**

The Super Admin can manage:

- User ID
- Name
- Role
- Password
- Lead domain assignments

The roles that can be assigned are:

- STUDENT
- FACULTY
- LEAD

If a user is a Lead, the Super Admin can assign one or more technical domains.

General does not need to be assigned because every Lead automatically receives General management access.

The Super Admin cannot:

- Manage sessions.
- Manage attendance.
- Generate reports.
- Manage technical domains.

User management should happen through the application's controlled interface and backend rather than exposing direct database access.

---

# 9. Session Management

A session is the primary record of a technical activity conducted by the club.

Each session belongs to one domain and contains:

```text
Session
- Session ID
- Domain
- Title
- Date
- Start Time
- End Time
- Leads
- Designations
- Summary
- Resources
- Attendance
```

The **day of the week** is derived from the date rather than manually stored.

The **duration** is calculated from the start and end times.

This prevents inconsistencies caused by manually entering derived information.

The session summary is intentionally concise and describes what was covered. It is not intended to replace complete lecture notes.

---

# 10. Session Leads and Resources

A session may have multiple leads.

Each lead entry can contain a flexible designation, such as:

- Frontend Lead
- Backend Lead
- Instructor
- Co-Instructor
- Workshop Lead
- Session Lead

This allows the system to represent different responsibilities without forcing every session contributor into a single fixed designation.

Sessions can also contain multiple resources.

Resources can initially be stored as text or URLs, such as:

- Documentation
- GitHub repositories
- Presentation links
- Tutorials
- Practice material
- Reference articles

A complex file-upload system is not required for the initial implementation.

---

# 11. Attendance System

Attendance connects users and sessions.

Conceptually:

```text
User ←→ Attendance ←→ Session
```

This allows one user to attend multiple sessions and one session to contain many users.

### Students

Students can see only their own attendance.

They cannot view another member's attendance.

### Faculty

Faculty can view attendance for all sessions and all members but cannot modify it.

### Leads

Leads can view attendance across the entire club.

They can modify attendance only for sessions they are authorized to manage:

```text
General + assigned domain(s)
```

Therefore, a Frontend Lead can modify attendance for Frontend and General sessions but cannot modify attendance for Backend sessions.

### Super Admin

Super Admin has no attendance permissions.

This keeps user administration separate from session and attendance operations.

---

# 12. Generate Summary

The **Generate Summary** feature is a documentation and reporting feature available to:

- Faculty
- Leads

Students and Super Admin do not have access to it.

The user selects a date range and one or more domains.

The backend retrieves sessions matching the selected date range and domains.

The output contains date, day, domain, title, time, duration, attendance, summary, leads, and resources.

---

# 13. Database Structure

A possible relational database structure consists of the following entities.

### User

```text
user_id
name
password_hash
role
```

User IDs must be unique and passwords must be stored securely as hashes.

### Domain

```text
domain_id
domain_name
```

### Lead Domain Assignment

```text
user_id
domain_id
```

This allows Leads to have multiple assigned domains.

### Session

```text
session_id
domain_id
title
date
start_time
end_time
summary
```

### Session Lead

```text
session_id
person
designation
```

### Resource

```text
resource_id
session_id
resource_url
```

### Attendance

```text
session_id
user_id
status
```

---

# 14. Backend Responsibilities

The Java backend is responsible for the application's actual business logic.

Its responsibilities include:

### Authentication

- Verify User ID and password.
- Authenticate users.
- Identify their roles.

### Authorization

The backend determines whether the authenticated user can perform a requested operation.

For example:

```text
Student → cannot create a session
Faculty → cannot edit attendance
Frontend Lead → can edit Frontend
Frontend Lead → cannot edit Backend
Super Admin → can update user details
Super Admin → cannot edit sessions
```

### Session Management

The backend handles session creation, retrieval, modification, and deletion while applying domain permissions.

### Attendance

The backend handles attendance creation, viewing, modification, and removal according to role and session permissions.

### User Management

The backend provides controlled CRUD operations for the Super Admin.

### Report Generation

The backend processes the selected dates and domains, retrieves matching sessions, and formats the resulting information as Markdown.

---

# 15. Security and Data Integrity

The system contains account and attendance information, so security is an important part of the design.

### Password Security

Passwords should never be stored as plaintext. Only secure password hashes should be stored.

### Backend Authorization

Permission checks must happen on the backend. Hiding a button in the frontend is not sufficient because users could otherwise attempt unauthorized requests directly.

### Unique IDs

The database should enforce uniqueness for User IDs.

### Input Validation

The backend should validate:

- Required fields
- Dates
- Times
- Domains
- Roles
- User IDs
- Lead assignments
- Session permissions

### Historical Records

User removal should be handled carefully because attendance records may need to remain available for historical documentation. Account deactivation or soft deletion may therefore be preferable to physically deleting records that are still referenced by attendance history.

---

# 16. Core Business Rules

The system follows these major rules:

1. The application contains exactly five technical domains.
2. Every Lead automatically has General management access.
3. Lead editing permissions are limited to General and assigned domains.
4. Leads can view all domains and sessions.
5. Leads and Faculty can generate reports covering all domains.
6. Students can view only their own attendance.
7. Faculty can view all attendance but cannot modify it.
8. Leads can modify attendance only for sessions they can manage.
9. Super Admin is restricted to user/account CRUD.
10. Day of week is derived from the session date.
11. Duration is calculated from start and end times.
12. Session summaries remain concise rather than acting as full lecture notes.
13. Backend authorization must independently enforce all permissions.

---

# 17. Overall User Flows

### Student

```text
Login
  ↓
Members Corner
  ↓
Select Domain
  ↓
View Sessions
  ↓
Open Session
  ↓
View Details + Own Attendance
```

### Faculty

```text
Login
  ↓
Members Corner
  ↓
View Domains
  ↓
Inspect Sessions
  ↓
Review Attendance
  ↓
Generate Summary
```

### Lead

```text
Login
  ↓
Members Corner
  ↓
View All Domains
  ↓
Manage General / Assigned Domain
  ↓
Create / Edit / Remove Session
  ↓
Manage Attendance
  ↓
Generate Global Summary
```

### Super Admin

```text
Login
  ↓
User Management
  ↓
Create / View / Update / Remove Users
  ↓
Set Role
  ↓
Assign Domains to Leads
```

---

# 18. Final Project Summary

MIT TECH KERNEL is designed as a complete digital platform for a college technical club rather than simply a static informational website.

The public website communicates the club's identity, purpose, domains, team, activities, and events.

The Members Corner provides the operational layer of the system. Students can explore sessions and monitor their own attendance. Faculty can review all sessions and attendance and generate documentation. Leads can manage sessions and attendance within General and their assigned domains while retaining global viewing and reporting access. The Super Admin maintains user accounts and Lead assignments without receiving session or attendance permissions.

The Java backend forms the central application layer. It manages authentication, role-based authorization, session operations, attendance, user CRUD, database interaction, and Markdown report generation.

The overall design is based on a key principle:

> **Visibility, management, reporting, and administration are separate permissions.**
