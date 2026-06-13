# OpsMap: Real-Time Collaborative Operational Mapping System

OpsMap is a synchronized, real-time collaborative tactical mapping platform built in Java. [cite_start]Designed for multi-user mission coordination, it enables concurrent operators and commanders to interact simultaneously on a shared geographical canvas, visualize tactical navigation paths, plot structural regions, and communicate via synchronous networks[cite: 11].

---

## Technical Architecture

[cite_start]The platform utilizes a robust Client-Server topology managed via a unified Apache Maven project structure[cite: 29, 30].

### Key Subsystems (Model-View-Controller Layering)

- [cite_start]**`com.opsmap.network` (Distributed Pipeline):** Implements multi-threaded Java Socket Programming[cite: 15]. [cite_start]Features a centralized server engine (`MapServer`) running worker threads (`ClientHandler`) to stream shape models, mouse movements, and action queues across connected clients with minimal overhead[cite: 29, 41].
- [cite_start]**`com.opsmap.view` (User Experience Layer):** Contains interactive vector-based display modules built with JavaFX (`MapPage`, `LoginPage`) for fluid user authentication and real-time mapping actions[cite: 14, 21].
- [cite_start]**`com.opsmap.model` (Core Domain Specifications):** Manages localized shape data entities (Lines, Rectangles, Circles), dynamic map metadata records, and client sessions.

---

## Core Features Implemented

- [cite_start]**Route & Tactical Tools:** Precision vector drawing modules for real-time tracking.
- [cite_start]**Live Sync Engine:** Low-latency packet distribution via TCP/IP object socket channels.
- [cite_start]**Input Validation & Security:** Strict server-side data sanitization and exception propagation pipelines to prevent desynchronization[cite: 44, 46].

---

## Building and Execution

### Build Artifacts
Compile the source codebase and download all module packages using the Maven Wrapper:

```bash
# Compile and clean source targets
./mvnw clean compile

# Build the final executable package
./mvnw package
