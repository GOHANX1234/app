# Sarrows — Native App API Reference

**Base URL:** `https://sarrows.vercel.app`

All requests and responses use JSON (`Content-Type: application/json`) unless noted otherwise. All IDs are MongoDB ObjectId strings (24-character hex).

---

## Table of Contents

1. [Authentication Overview](#1-authentication-overview)
2. [Sign Up](#2-sign-up)
3. [Login](#3-login)
4. [Session / Current User](#4-session--current-user)
5. [Logout](#5-logout)
6. [Movies](#6-movies)
   - [List Movies](#61-list-movies)
   - [Get Movie by ID](#62-get-movie-by-id)
7. [Anime & Series](#7-anime--series)
   - [List Anime / Series](#71-list-anime--series)
   - [Get Series by ID *(use Movie endpoint)*](#72-note-on-getting-a-single-series)
8. [Episodes](#8-episodes)
   - [Get Episode by ID](#81-get-episode-by-id)
9. [Streaming & Playback](#9-streaming--playback)
   - [Stream a Movie (HLS / Direct)](#91-stream-a-movie-hls--direct)
   - [Get Movie Embed URL (iframe)](#92-get-movie-embed-url-iframe)
   - [Stream an Episode (HLS / Direct)](#93-stream-an-episode-hls--direct)
   - [Get Episode Embed URL (iframe)](#94-get-episode-embed-url-iframe)
   - [HLS Relay](#95-hls-relay-internal)
   - [Deciding Which Endpoint to Use](#96-deciding-which-playback-endpoint-to-use)
10. [Search](#10-search)
11. [Reviews & Ratings](#11-reviews--ratings)
    - [List Reviews](#111-list-reviews)
    - [Submit / Update a Review](#112-submit--update-a-review)
    - [Delete a Review](#113-delete-a-review)
12. [Watchlist](#12-watchlist)
    - [Check Watchlist Status](#121-check-watchlist-status)
    - [Toggle Watchlist](#122-toggle-watchlist)
13. [Watch History](#13-watch-history)
    - [Save Playback Progress](#131-save-playback-progress)
14. [View Counts](#14-view-counts)
15. [Content Requests](#15-content-requests)
    - [Submit a Request](#151-submit-a-request)
    - [List My Requests](#152-list-my-requests)
    - [Cancel a Request](#153-cancel-a-request)
16. [Data Models Reference](#16-data-models-reference)
17. [Error Reference](#17-error-reference)

---

## 1. Authentication Overview

Sarrows uses **NextAuth v5** with a **JWT session strategy**. Sessions are stored entirely in a signed, encrypted cookie (`authjs.session-token`). There is no separate bearer token — **every authenticated request must include this cookie**.

### How it works for a native app

1. Fetch a CSRF token → `GET /api/auth/csrf`
2. Post credentials → `POST /api/auth/callback/credentials` (form-encoded, not JSON)
3. On success the server sets the session cookie — persist this cookie and send it on every subsequent request.
4. To check if you're still logged in → `GET /api/auth/session`

> **Cookie handling:** Use a cookie jar / `CookieManager` in your HTTP client. All authenticated endpoints check the cookie automatically on the server side. No `Authorization` header is used.

---

## 2. Sign Up

Create a new user account.

```
POST /api/auth/signup
```

### Request body (JSON)

| Field      | Type   | Required | Constraints                                           |
|------------|--------|----------|-------------------------------------------------------|
| `nickname` | string | ✅       | 3–20 chars, letters / numbers / underscores only      |
| `email`    | string | ✅       | Valid email address                                   |
| `password` | string | ✅       | Minimum 8 characters                                  |

```json
{
  "nickname": "CoolUser42",
  "email": "user@example.com",
  "password": "MyPassword123"
}
```

### Responses

**201 Created — success**
```json
{ "success": true }
```

**400 Bad Request — validation failed**
```json
{
  "error": "Validation failed",
  "fieldErrors": {
    "nickname": "Nickname must be at least 3 characters"
  }
}
```

**409 Conflict — email or nickname already taken**
```json
{
  "error": "Email taken",
  "fieldErrors": { "email": "This email is already registered" }
}
```
```json
{
  "error": "Nickname taken",
  "fieldErrors": { "nickname": "This nickname is already taken" }
}
```

> After a successful sign-up the user is **not** automatically logged in. You must call the Login flow separately.

---

## 3. Login

NextAuth credential login is a two-step process.

### Step 1 — Fetch CSRF token

```
GET /api/auth/csrf
```

**Response**
```json
{
  "csrfToken": "abc123..."
}
```

### Step 2 — Submit credentials

```
POST /api/auth/callback/credentials
Content-Type: application/x-www-form-urlencoded
```

**Form fields** (URL-encoded, NOT JSON)

| Field        | Value                         |
|--------------|-------------------------------|
| `email`      | user's email                  |
| `password`   | user's password               |
| `csrfToken`  | token from Step 1             |
| `redirect`   | `false`                       |
| `callbackUrl`| `https://sarrows.vercel.app/` |

```
email=user%40example.com&password=MyPassword123&csrfToken=abc123...&redirect=false&callbackUrl=https%3A%2F%2Fsarrows.vercel.app%2F
```

**On success:**
- HTTP `200` (or `302` if `redirect` wasn't set to `false`)
- The response sets the `authjs.session-token` cookie — persist this in your cookie store
- Body: `{ "url": "https://sarrows.vercel.app/" }`

**On failure (wrong password / account locked):**
- HTTP `200` with a URL containing an `error` query param, e.g.:
  `{ "url": "https://sarrows.vercel.app/login?error=CredentialsSignin" }`

> **Account lock:** After 10 consecutive failed login attempts, the account is locked for 15 minutes. During this period all login attempts silently fail.

---

## 4. Session / Current User

Check if the user is authenticated and get their profile data.

```
GET /api/auth/session
```

**Authenticated response (200)**
```json
{
  "user": {
    "id": "64f1a2b3c4d5e6f7a8b9c0d1",
    "name": "CoolUser42",
    "email": "user@example.com",
    "image": null,
    "role": "user"
  },
  "expires": "2026-08-13T10:32:00.000Z"
}
```

**Unauthenticated response (200)**
```json
{}
```

> `role` is either `"user"` or `"admin"`. The role is re-synced from the database every 60 seconds, so role changes propagate automatically without requiring a re-login.

---

## 5. Logout

```
POST /api/auth/signout
Content-Type: application/x-www-form-urlencoded
```

**Form fields**

| Field       | Value                          |
|-------------|-------------------------------|
| `csrfToken` | token from `GET /api/auth/csrf` |

Clears the session cookie. On success redirects to `/` (or returns `200` if using `redirect=false`).

---

## 6. Movies

### 6.1 List Movies

```
GET /api/movies
```

Returns a paginated list of published movies.

**Query parameters**

| Param   | Type    | Default  | Description                                                |
|---------|---------|----------|------------------------------------------------------------|
| `page`  | integer | `1`      | Page number (1-based)                                      |
| `limit` | integer | `24`     | Items per page (max 50)                                    |
| `sort`  | string  | `latest` | Sort order: `latest`, `views`, `rating`, `year`            |
| `genre` | string  | —        | Filter by genre name (case-insensitive, exact match)       |
| `year`  | integer | —        | Filter by release year, e.g. `2024`                        |

**Example**
```
GET /api/movies?sort=rating&genre=Action&page=1&limit=24
```

**Response (200)**
```json
{
  "movies": [ /* array of Movie objects */ ],
  "total": 142,
  "page": 1,
  "totalPages": 6
}
```

Each item in `movies` is a [Movie object](#movie-object) (without `videoUrl` / `videoType`).

---

### 6.2 Get Movie by ID

```
GET /api/movies/:id
```

**Path params**

| Param | Description          |
|-------|----------------------|
| `id`  | MongoDB ObjectId of the movie |

**Response (200)**
```json
{
  "movie": { /* Movie object */ }
}
```

`videoUrl` and `videoType` are **never** included in this response for regular users — video playback goes through `/api/stream/movie/:id` (see [Section 9](#9-streaming--playback)).

**404** — movie not found or not published.

---

## 7. Anime & Series

### 7.1 List Anime / Series

```
GET /api/anime
```

Returns a paginated list of published anime or series.

**Query parameters**

| Param    | Type    | Default  | Description                                                    |
|----------|---------|----------|----------------------------------------------------------------|
| `type`   | string  | `anime`  | Content type: `anime` or `series`                              |
| `page`   | integer | `1`      | Page number (1-based)                                          |
| `limit`  | integer | `24`     | Items per page (max 50)                                        |
| `sort`   | string  | `latest` | Sort order: `latest`, `views`, `rating`                        |
| `genre`  | string  | —        | Filter by genre name (case-insensitive, exact match)           |
| `status` | string  | —        | Airing status: `ongoing` or `completed`                        |

**Example**
```
GET /api/anime?type=anime&sort=views&status=ongoing&page=1
```

**Response (200)**
```json
{
  "series": [ /* array of Series objects */ ],
  "total": 88,
  "page": 1,
  "totalPages": 4
}
```

Each item is a [Series object](#series-object).

---

### 7.2 Note on Getting a Single Series

There is no dedicated `GET /api/anime/:id` endpoint. To fetch a single series by its MongoDB ID, use the search endpoint or store the full series object when it comes back from the list. The detail pages on the web app are slug-routed (`/anime/[slug]`) and load the series data from the list endpoint filtered server-side.

> **Tip for the native app:** When navigating to a detail screen, pass the full series object you already have from the list rather than making an extra round-trip.

---

## 8. Episodes

### 8.1 Get Episode by ID

```
GET /api/episodes/:id
```

Returns metadata for a single episode. Only works if the parent series is published.

**Path params**

| Param | Description             |
|-------|-------------------------|
| `id`  | MongoDB ObjectId of the episode |

**Response (200)**
```json
{
  "episode": { /* Episode object */ }
}
```

`videoUrl` and `videoType` are **never** included — use `/api/stream/episode/:id` for playback.

**404** — episode not found or parent series is not published.

> To list all episodes for a series, use the search or store episode lists from series detail data. Episodes are indexed by `{ series, season, episodeNumber }`.

---

## 9. Streaming & Playback

> **All stream endpoints require authentication.** An unauthenticated request returns `401`.
>
> **Rate limit:** 30 stream-init or embed requests per user per 60 seconds. HLS relay requests allow 600 per user per 60 seconds.

The server **never** sends the real CDN video URL to the client in a JSON response or page payload. Playback is always proxied or disclosed only at playback time via a separate authenticated endpoint.

---

### 9.1 Stream a Movie (HLS / Direct)

```
GET /api/stream/movie/:id
```

Proxies HLS or direct MP4/WebM video bytes from the CDN to the client. Use this as the video `src` for a native player.

- For **HLS** (`.m3u8`): The server rewrites the manifest so all segment/key URLs point back to `/api/stream/relay?s=<token>` — never the real CDN. Pass this URL as the HLS source in your player (e.g. ExoPlayer, AVPlayer).
- For **direct** (`.mp4`, `.webm`): The server proxies the raw bytes with `Range` header support for seeking.

**Auth:** Cookie session required.

**Path params**

| Param | Description |
|-------|-------------|
| `id`  | Movie ObjectId |

**Success:** Streams the media bytes (HTTP 200 or 206 for range requests).

**Errors:**

| Status | Meaning |
|--------|---------|
| `400`  | If the video is an embed type — use `/embed` endpoint instead |
| `401`  | Not authenticated |
| `404`  | Movie not found / not published / no video configured |
| `429`  | Rate limit exceeded |

---

### 9.2 Get Movie Embed URL (iframe)

```
GET /api/stream/movie/:id/embed
```

For movies whose video is a third-party iframe link (e.g. a streaming embed from another site), this endpoint returns the raw URL to load in a WebView.

**Auth:** Cookie session required.

**Response (200)**
```json
{
  "url": "https://third-party-player.example.com/embed/abc123"
}
```

**Errors:**

| Status | Meaning |
|--------|---------|
| `400`  | Video is not an embed type — use the stream endpoint instead |
| `401`  | Not authenticated |
| `404`  | Movie not found / not published |
| `429`  | Rate limit exceeded |

> Load `url` inside a `WebView` / `WKWebView`. Do not expose it to the user directly.

---

### 9.3 Stream an Episode (HLS / Direct)

```
GET /api/stream/episode/:id
```

Same behaviour as [9.1](#91-stream-a-movie-hls--direct) but for a series episode.

**Auth:** Cookie session required.

**Path params**

| Param | Description |
|-------|-------------|
| `id`  | Episode ObjectId |

**Success:** Streams the media bytes.

**Errors:** Same as [9.1](#91-stream-a-movie-hls--direct).

---

### 9.4 Get Episode Embed URL (iframe)

```
GET /api/stream/episode/:id/embed
```

Same behaviour as [9.2](#92-get-movie-embed-url-iframe) but for a series episode.

**Auth:** Cookie session required.

**Response (200)**
```json
{
  "url": "https://third-party-player.example.com/embed/xyz789"
}
```

---

### 9.5 HLS Relay (internal)

```
GET /api/stream/relay?s=<token>
```

Internal endpoint used automatically by rewritten HLS playlists. **You never need to call this directly** — your HLS player will hit it automatically when fetching segments after you point it at `/api/stream/movie/:id` or `/api/stream/episode/:id`.

**Auth:** Cookie session required (the token is also bound to the viewer's user ID).

---

### 9.6 Deciding Which Playback Endpoint to Use

The episode/movie detail endpoint does **not** tell you whether the content is HLS, direct, or an embed — `videoType` is server-only. Use the following strategy:

```
1. Try   GET /api/stream/movie/:id   (or /episode/:id)
   → If 200, feed the URL to your native player (HLS or direct)
   → If 400 with message "Use /embed for this content":
2. Try   GET /api/stream/movie/:id/embed   (or /episode/:id/embed)
   → If 200, load { url } in a WebView
```

A clean two-step probe is all you need. Cache the result per content ID per session so you don't probe twice.

---

## 10. Search

```
GET /api/search
```

Searches titles and descriptions across published movies and series.

**Query parameters**

| Param | Type   | Required | Description                              |
|-------|--------|----------|------------------------------------------|
| `q`   | string | ✅       | Search query, minimum 2 characters, max 100 |

**Example**
```
GET /api/search?q=attack+on+titan
```

**Response (200)**
```json
{
  "movies": [ /* up to 20 Movie objects */ ],
  "series": [ /* up to 20 Series objects */ ]
}
```

If `q` is shorter than 2 characters:
```json
{ "movies": [], "series": [] }
```

Search is case-insensitive regex on `title` and `description`. Results are not ranked by relevance beyond the MongoDB default.

---

## 11. Reviews & Ratings

### 11.1 List Reviews

```
GET /api/reviews?targetType=<type>&targetId=<id>
```

Returns the latest 50 reviews for a movie or series.

**Query parameters**

| Param        | Type   | Required | Description                          |
|--------------|--------|----------|--------------------------------------|
| `targetType` | string | ✅       | `Movie` or `Series`                  |
| `targetId`   | string | ✅       | ObjectId of the movie or series      |

**Example**
```
GET /api/reviews?targetType=Movie&targetId=64f1a2b3c4d5e6f7a8b9c0d1
```

**Response (200)**
```json
{
  "reviews": [
    {
      "_id": "64f...",
      "user": {
        "_id": "64f...",
        "nickname": "CoolUser42",
        "image": null
      },
      "targetType": "Movie",
      "targetId": "64f...",
      "rating": 8,
      "comment": "Great movie!",
      "createdAt": "2026-07-01T12:00:00.000Z",
      "updatedAt": "2026-07-01T12:00:00.000Z"
    }
  ]
}
```

**400** — missing `targetType` or `targetId`.

---

### 11.2 Submit / Update a Review

```
POST /api/reviews
```

Creates or updates the authenticated user's review for a movie or series. One review per user per content item (upsert). Also recomputes and updates the denormalized `rating` and `ratingCount` on the target.

**Auth:** Required.

**Request body (JSON)**

| Field        | Type    | Required | Constraints               |
|--------------|---------|----------|---------------------------|
| `targetType` | string  | ✅       | `"Movie"` or `"Series"`   |
| `targetId`   | string  | ✅       | ObjectId of the target    |
| `rating`     | integer | ✅       | 1–10 inclusive            |
| `comment`    | string  | —        | Max 1000 characters       |

```json
{
  "targetType": "Movie",
  "targetId": "64f1a2b3c4d5e6f7a8b9c0d1",
  "rating": 9,
  "comment": "Absolutely loved it!"
}
```

**Response (200)**
```json
{
  "review": {
    "_id": "64f...",
    "user": { "_id": "64f...", "nickname": "CoolUser42", "image": null },
    "targetType": "Movie",
    "targetId": "64f...",
    "rating": 9,
    "comment": "Absolutely loved it!",
    "createdAt": "2026-07-01T12:00:00.000Z",
    "updatedAt": "2026-07-01T12:00:00.000Z"
  }
}
```

**400** — validation failed.  
**401** — not authenticated.

> `comment` is sanitised (all HTML stripped) before saving.

---

### 11.3 Delete a Review

```
DELETE /api/reviews/:id
```

Deletes a review. Only the review's author or an admin can delete it.

**Auth:** Required.

**Path params**

| Param | Description        |
|-------|--------------------|
| `id`  | Review ObjectId    |

**Response (200)**
```json
{ "success": true }
```

**401** — not authenticated.  
**403** — not the review owner or admin.  
**404** — review not found.

---

## 12. Watchlist

### 12.1 Check Watchlist Status

```
GET /api/watchlist?targetType=<type>&targetId=<id>
```

Checks whether the authenticated user has the given item in their watchlist.

**Auth:** Not strictly required — returns `{ inWatchlist: false }` for unauthenticated users.

**Query parameters**

| Param        | Type   | Required | Description                     |
|--------------|--------|----------|---------------------------------|
| `targetType` | string | ✅       | `Movie` or `Series`             |
| `targetId`   | string | ✅       | ObjectId of the movie or series |

**Response (200)**
```json
{ "inWatchlist": true }
```
or
```json
{ "inWatchlist": false }
```

---

### 12.2 Toggle Watchlist

```
POST /api/watchlist/toggle
```

Adds the item to the watchlist if it isn't there; removes it if it is. A single endpoint handles both add and remove.

**Auth:** Required.

**Request body (JSON)**

| Field        | Type   | Required | Description                     |
|--------------|--------|----------|---------------------------------|
| `targetType` | string | ✅       | `"Movie"` or `"Series"`         |
| `targetId`   | string | ✅       | ObjectId of the movie or series |

```json
{
  "targetType": "Series",
  "targetId": "64f1a2b3c4d5e6f7a8b9c0d2"
}
```

**Response (200) — added**
```json
{ "inWatchlist": true }
```

**Response (200) — removed**
```json
{ "inWatchlist": false }
```

**400** — invalid `targetType` or non-ObjectId `targetId`.  
**401** — not authenticated.

---

## 13. Watch History

### 13.1 Save Playback Progress

```
POST /api/watch-history
```

Upserts the viewer's playback progress for a movie or episode. Call this periodically during playback (e.g. every 30 seconds) and on pause/stop. Silently no-ops for unauthenticated users (returns `{ ok: true }`).

**Auth:** Recommended (silently skipped if not authenticated).

**Request body (JSON)**

| Field             | Type    | Required | Constraints                         |
|-------------------|---------|----------|-------------------------------------|
| `targetType`      | string  | ✅       | `"Movie"` or `"Episode"`            |
| `targetId`        | string  | ✅       | ObjectId of the movie or episode    |
| `progressSeconds` | integer | ✅       | Playback position in seconds (0–86400) |

```json
{
  "targetType": "Episode",
  "targetId": "64f1a2b3c4d5e6f7a8b9c0d3",
  "progressSeconds": 743
}
```

**Response (200)**
```json
{ "ok": true }
```

**500** — server error.

> The `completed` flag is always set to `false` by this endpoint. There is currently no API endpoint for marking content as fully watched.

---

## 14. View Counts

```
POST /api/views
```

Increments the view counter for a movie, series, or episode. Each user can only count once per content item (subsequent calls for the same user+item are silently ignored via a unique index). For episodes, both the episode view count and the parent series view count are incremented.

**Auth:** Required.

**Request body (JSON)**

| Field        | Type   | Required | Description                              |
|--------------|--------|----------|------------------------------------------|
| `targetType` | string | ✅       | `"Movie"`, `"Series"`, or `"Episode"`    |
| `targetId`   | string | ✅       | ObjectId of the target                   |

```json
{
  "targetType": "Movie",
  "targetId": "64f1a2b3c4d5e6f7a8b9c0d1"
}
```

**Response (200) — first view, count incremented**
```json
{ "ok": true, "counted": true }
```

**Response (200) — already counted for this user**
```json
{ "ok": true, "counted": false }
```

**400** — invalid `targetType` or non-ObjectId `targetId`.  
**401** — not authenticated.  
**404** — content not found.

> Call this once per session when the user starts watching (not repeatedly during playback).

---

## 15. Content Requests

Users can request movies, anime, or series they want added to the platform.

### 15.1 Submit a Request

```
POST /api/requests
```

**Auth:** Required.

**Request body (JSON)**

| Field   | Type   | Required | Constraints                              |
|---------|--------|----------|------------------------------------------|
| `title` | string | ✅       | 1–200 characters                         |
| `type`  | string | ✅       | `"movie"`, `"series"`, or `"anime"`      |
| `note`  | string | —        | Optional extra info, max 500 characters  |

```json
{
  "title": "Demon Slayer: Infinity Castle Arc",
  "type": "anime",
  "note": "The new movie just released!"
}
```

**Response (201)**
```json
{
  "request": {
    "_id": "64f...",
    "user": "64f...",
    "title": "Demon Slayer: Infinity Castle Arc",
    "type": "anime",
    "note": "The new movie just released!",
    "status": "pending",
    "adminNote": null,
    "createdAt": "2026-07-13T10:00:00.000Z",
    "updatedAt": "2026-07-13T10:00:00.000Z"
  }
}
```

**400** — validation failed.  
**401** — not authenticated.

---

### 15.2 List My Requests

```
GET /api/requests
```

Returns all content requests submitted by the authenticated user, newest first.

**Auth:** Required.

**Response (200)**
```json
{
  "requests": [
    {
      "_id": "64f...",
      "user": "64f...",
      "title": "Demon Slayer: Infinity Castle Arc",
      "type": "anime",
      "note": "The new movie just released!",
      "status": "pending",
      "adminNote": null,
      "createdAt": "2026-07-13T10:00:00.000Z",
      "updatedAt": "2026-07-13T10:00:00.000Z"
    }
  ]
}
```

**Request `status` values:**

| Value        | Meaning                              |
|--------------|--------------------------------------|
| `pending`    | Awaiting admin review                |
| `in_progress`| Admin is working on it               |
| `fulfilled`  | Content has been added               |
| `rejected`   | Request won't be fulfilled           |

---

### 15.3 Cancel a Request

```
DELETE /api/requests/:id
```

Cancel (delete) one of your own pending requests. Only works while status is `pending`.

**Auth:** Required.

**Path params**

| Param | Description         |
|-------|---------------------|
| `id`  | Request ObjectId    |

**Response (200)**
```json
{ "success": true }
```

**401** — not authenticated.  
**403** — request belongs to another user.  
**404** — request not found.  
**409** — request is not in `pending` status (already in progress / fulfilled / rejected).

---

## 16. Data Models Reference

### Movie Object

```jsonc
{
  "_id": "64f1a2b3c4d5e6f7a8b9c0d1",
  "title": "Inception",
  "slug": "inception",
  "description": "A thief who enters dreams...",
  "posterUrl": "https://cdn.example.com/posters/inception.jpg",
  "bannerUrl": "https://cdn.example.com/banners/inception.jpg",
  "trailerUrl": "https://youtube.com/watch?v=...",
  "externalId": "27205",              // e.g. TMDB ID, if set
  "duration": 148,                    // minutes
  "releaseYear": 2010,
  "genres": [
    { "_id": "64f...", "name": "Action" },
    { "_id": "64f...", "name": "Sci-Fi" }
  ],
  "cast": [
    {
      "_id": "64f...",
      "name": "Leonardo DiCaprio",
      "character": "Dom Cobb",
      "image": "https://cdn.example.com/cast/leo.jpg",
      "order": 1
    }
  ],
  "rating": 8.7,                      // computed average of all reviews (0–10)
  "ratingCount": 1204,                // total number of reviews
  "views": 45200,
  "status": "published",              // "published" | "draft"
  "createdAt": "2026-01-01T00:00:00.000Z",
  "updatedAt": "2026-07-01T00:00:00.000Z"
  // videoUrl and videoType are NEVER returned to non-admin users
}
```

---

### Series Object

```jsonc
{
  "_id": "64f1a2b3c4d5e6f7a8b9c0d2",
  "title": "Attack on Titan",
  "slug": "attack-on-titan",
  "description": "Humanity lives inside cities...",
  "posterUrl": "https://cdn.example.com/posters/aot.jpg",
  "bannerUrl": "https://cdn.example.com/banners/aot.jpg",
  "externalId": "16498",
  "totalSeasons": 4,
  "releaseYear": 2013,
  "genres": [
    { "_id": "64f...", "name": "Action" },
    { "_id": "64f...", "name": "Drama" }
  ],
  "cast": [
    {
      "_id": "64f...",
      "name": "Yuki Kaji",
      "character": "Eren Yeager",
      "image": "https://cdn.example.com/cast/yuki.jpg",
      "order": 1
    }
  ],
  "status": "completed",              // "ongoing" | "completed"
  "type": "anime",                    // "anime" | "series"
  "rating": 9.1,
  "ratingCount": 8820,
  "views": 210400,
  "publishStatus": "published",       // "published" | "draft"
  "createdAt": "2026-01-10T00:00:00.000Z",
  "updatedAt": "2026-07-01T00:00:00.000Z"
}
```

---

### Episode Object

```jsonc
{
  "_id": "64f1a2b3c4d5e6f7a8b9c0d3",
  "series": "64f1a2b3c4d5e6f7a8b9c0d2",   // parent Series ObjectId
  "season": 1,
  "episodeNumber": 1,
  "title": "To You, in 2000 Years",
  "createdAt": "2026-01-10T00:00:00.000Z",
  "updatedAt": "2026-07-01T00:00:00.000Z"
  // videoUrl and videoType are NEVER returned to non-admin users
}
```

---

### User Object (session)

```jsonc
{
  "id": "64f1a2b3c4d5e6f7a8b9c0d4",
  "name": "CoolUser42",               // this is the nickname
  "email": "user@example.com",
  "image": null,                      // URL string or null
  "role": "user"                      // "user" | "admin"
}
```

---

### Review Object

```jsonc
{
  "_id": "64f...",
  "user": {
    "_id": "64f...",
    "nickname": "CoolUser42",
    "image": null
  },
  "targetType": "Movie",              // "Movie" | "Series"
  "targetId": "64f...",
  "rating": 8,                        // 1–10
  "comment": "Amazing film.",
  "createdAt": "2026-07-01T12:00:00.000Z",
  "updatedAt": "2026-07-01T12:00:00.000Z"
}
```

---

### Request Object

```jsonc
{
  "_id": "64f...",
  "user": "64f...",
  "title": "Demon Slayer: Infinity Castle Arc",
  "type": "anime",                    // "movie" | "series" | "anime"
  "note": "Optional extra info",
  "status": "pending",               // "pending" | "in_progress" | "fulfilled" | "rejected"
  "adminNote": null,                  // string or null
  "createdAt": "2026-07-13T10:00:00.000Z",
  "updatedAt": "2026-07-13T10:00:00.000Z"
}
```

---

## 17. Error Reference

All error responses have at minimum:
```json
{ "error": "<message>" }
```

Some also include `fieldErrors` for validation failures:
```json
{
  "error": "Validation failed",
  "fieldErrors": {
    "email": "Invalid email address",
    "nickname": "Nickname must be at least 3 characters"
  }
}
```

### Standard HTTP Status Codes Used

| Status | Meaning                                                                 |
|--------|-------------------------------------------------------------------------|
| `200`  | Success                                                                 |
| `201`  | Created successfully                                                    |
| `400`  | Bad request — invalid input, missing required field, or wrong endpoint  |
| `401`  | Unauthenticated — session cookie missing or expired                     |
| `403`  | Forbidden — authenticated but not allowed (wrong user or wrong role)    |
| `404`  | Not found — content doesn't exist or isn't published yet                |
| `409`  | Conflict — duplicate (email/nickname taken) or invalid state transition |
| `429`  | Too many requests — rate limit exceeded, retry after a moment           |
| `500`  | Internal server error                                                   |

---

## Quick Reference — Authenticated Endpoints

| Endpoint                              | Auth Required |
|---------------------------------------|:-------------:|
| `POST /api/auth/signup`               | ❌            |
| `GET /api/auth/csrf`                  | ❌            |
| `POST /api/auth/callback/credentials` | ❌            |
| `GET /api/auth/session`               | ❌            |
| `POST /api/auth/signout`              | ✅ (cookie)   |
| `GET /api/movies`                     | ❌            |
| `GET /api/movies/:id`                 | ❌            |
| `GET /api/anime`                      | ❌            |
| `GET /api/episodes/:id`               | ❌            |
| `GET /api/search`                     | ❌            |
| `GET /api/reviews`                    | ❌            |
| `POST /api/reviews`                   | ✅            |
| `DELETE /api/reviews/:id`             | ✅            |
| `GET /api/watchlist`                  | ⚠️ optional  |
| `POST /api/watchlist/toggle`          | ✅            |
| `POST /api/watch-history`             | ⚠️ optional  |
| `POST /api/views`                     | ✅            |
| `GET /api/stream/movie/:id`           | ✅            |
| `GET /api/stream/movie/:id/embed`     | ✅            |
| `GET /api/stream/episode/:id`         | ✅            |
| `GET /api/stream/episode/:id/embed`   | ✅            |
| `POST /api/requests`                  | ✅            |
| `GET /api/requests`                   | ✅            |
| `DELETE /api/requests/:id`            | ✅            |
