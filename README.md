# MindFocus

A personal diary web app with a vintage paper-notebook look: black binder spine, beige ruled pages, pastel index tabs and typewriter fonts. Each user logs into a private account and keeps notes, plans, goals, habits and moods in one place. The dashboard updates after every action.

**Live demo:** `http://35.159.64.135` (hosted on the AWS Free plan; replace with your server address, see [Deployment on AWS](#deployment-on-aws-free-plan)).

## Features

| Area | What it does |
|---|---|
| Account | Register and log in with email and password. Passwords are stored as BCrypt hashes, sessions use JWT. |
| Notes | Create, edit, delete and search diary notes by date. |
| Planner | Day plans and month plans. A plan can have a deadline and a "remind me" offset (at the deadline, 15 minutes, 1 hour, 3 hours, 1 day or 3 days before). |
| Reminders | A scheduler checks every minute. Due reminders go to the in-app bell, to email and to browser push. Each plan is reminded once. |
| To-do list | Tasks with an optional due date, filters for open and done. |
| Goals | Custom steps with a weight per step. Progress is the done weight divided by the total weight, shown as a percentage bar. |
| Habits | "Build" habits and "Quit a bad habit" habits with a 7-day tick grid, current and best streak, and 30-day success rate. |
| Mood | One mood score (1 to 5) and a short note per day. |
| Calendar | Month grid that merges plans and deadlines, due to-dos, goal target dates and notes. |
| Today (dashboard) | Totals, a 14-day chart and short insights. It reloads after every change anywhere in the app. |

## Tech stack

- **Backend:** Java 21, Spring Boot 3.3, Gradle, Spring Security with JWT, Spring Data JPA (Hibernate), Bean Validation, Spring Mail, Web Push (VAPID)
- **Database:** PostgreSQL 16 (Docker in development, installed on the server in production)
- **Frontend:** Angular 18 (standalone components, lazy-loaded routes, plain SCSS, no UI library)
- **Hosting:** AWS EC2 (Ubuntu 24.04, t3.micro), Nginx as web server and reverse proxy, systemd to keep the backend running
- **Dev tools:** Docker Compose (PostgreSQL and MailHog, a fake inbox for reminder emails)
- **Tests:** JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers (inside the backend); REST Assured, Selenium and Allure (separate `qa` project)

## Project structure

```
MindFocusWeb/
├── docker-compose.yml          PostgreSQL + MailHog
├── README.md
├── backend/                    Spring Boot application (run Gradle from here)
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/mindfocus/
│       │   ├── config/         typed application properties
│       │   ├── security/       JWT service, request filter, security config, CurrentUser
│       │   ├── domain/         JPA entities (per-user rows extend OwnedEntity)
│       │   ├── repo/           Spring Data repositories (OwnedRepository adds user scoping)
│       │   ├── web/            REST controllers (OwnedCrudController is the shared CRUD base)
│       │   └── service/        AnalyticsService, ReminderScheduler, PushSender
│       └── test/java/          unit tests (*Test) and integration tests (*IT)
├── frontend/                   Angular application (run npm from here)
│   ├── public/sw.js            service worker that shows push notifications
│   └── src/app/
│       ├── core/               API service, auth, interceptor, guard, push, refresh signal
│       ├── layout/             notebook shell: sidebar tabs and reminder bell
│       └── pages/              login, dashboard, notes, planner, todos, goals, habits, mood, calendar
└── qa/                         separate Gradle project: black-box API tests and browser tests
    └── src/test/java/com/mindfocus/qa/
        ├── support/            settings, request builders, user registration helpers
        ├── api/                REST Assured tests
        └── e2e/                Selenium tests with page objects
```

## Getting started (local development)

### Prerequisites

JDK 21, Gradle, Node.js (an LTS version such as 20 or 22), Docker, Git. Chrome is needed only for the browser tests.

### Run the app

Use one terminal per part, and leave them running.

**1. Database and mail inbox** (repository root):

```bash
docker compose up -d
```

If PostgreSQL is also installed directly on your computer (for example with Homebrew), stop it first or change the port. Two servers on port 5432 make the app and your database tool look at different databases.

**2. Backend** (`backend` folder):

```bash
cd backend
gradle bootRun
```

Wait for `Started MindFocusApplication`. It runs on http://localhost:8080 and has no pages, only the API. Tables are created automatically on the first start.

**3. Frontend** (`frontend` folder):

```bash
cd frontend
npm install
npm start
```

Wait for `Application bundle generation complete`, then open **http://localhost:4200**. The dev server forwards every `/api` call to the backend, so no CORS setup is needed in development.

Run each tool in its own folder: Gradle for the backend in `backend`, npm in `frontend`, the black-box tests in `qa`. Only `docker compose` runs from the repository root.

### Useful addresses

| Address | What |
|---|---|
| http://localhost:4200 | the app |
| http://localhost:8080/api/... | the REST API |
| http://localhost:8025 | MailHog: reminder emails arrive here, not in a real inbox |

## Configuration

Everything has a default for local development. Override with environment variables.

| Variable | Default | Meaning |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/mindfocus` | database connection |
| `DB_USER`, `DB_PASSWORD` | `mindfocus`, `mindfocus` | database login (must match `docker-compose.yml` locally) |
| `JWT_SECRET` | a development value | **change it in production**; use a random string of 32 or more characters |
| `APP_ZONE` | `Europe/Warsaw` | time zone for deadlines and reminders (independent of the server's own time zone) |
| `CORS_ORIGIN` | `http://localhost:4200` | allowed browser origin; in production set it to the public address of the site |
| `MAIL_HOST`, `MAIL_PORT` | `localhost`, `1025` | SMTP server (MailHog in development) |
| `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM` | empty, empty, `no-reply@mindfocus.local` | mail login and sender address |
| `VAPID_PUBLIC`, `VAPID_PRIVATE`, `VAPID_SUBJECT` | empty | browser push keys (see below) |
| `app.reminder-interval-ms` | `60000` | how often the reminder job runs (the integration tests set it to one hour and call the job themselves) |

If you renamed the database or user in `docker-compose.yml`, change `DB_URL`, `DB_USER` and `DB_PASSWORD` to the same values.

### Browser push notifications (optional)

Without keys, in-app and email reminders still work. To enable push, generate a key pair once:

```bash
npx web-push generate-vapid-keys
```

Start the backend with the keys:

```bash
VAPID_PUBLIC=<public key> VAPID_PRIVATE=<private key> VAPID_SUBJECT=mailto:you@example.com gradle bootRun
```

Then click "Turn on browser alerts" in the app and allow notifications. Push works on `localhost` and on HTTPS sites only, so it does not work on a plain `http://` server address.

## Deployment on AWS (Free plan)

The whole app runs on **one small EC2 server**:

```
Browser --HTTP (port 80)--> Nginx on EC2
                              |-- /       Angular files from /var/www/mindfocus
                              '-- /api/   Spring Boot on localhost:8080 --> PostgreSQL on localhost:5432
```

Only port 80 (web) and port 22 (SSH, from your own IP) are open. The backend and the database are not reachable from the internet.

**Cost:** a new AWS account on the Free plan is protected from charges for up to 6 months or until the free credits are used, whichever comes first. After that AWS closes the account unless you upgrade to a Paid plan. Keep the code in Git and make a database backup before then (see "Backups").

### 1. Build on your computer

```bash
cd backend
gradle clean bootJar -x test
```

The jar is in `backend/build/libs/mindfocus-backend-0.1.0.jar`. Use the file **without** `-plain` in its name.

```bash
cd frontend
npm install
npm run build
```

The website files are in `frontend/dist/mindfocus/browser` (the folder with `index.html`). The frontend calls relative `/api` URLs, so no address needs to be changed.

### 2. Create the server

1. Create an AWS account and choose the **Free** plan. Turn on MFA and create a zero-spend budget alert (AWS Budgets).
2. Choose your region (top right), for example Frankfurt `eu-central-1`.
3. EC2, then **Launch instance**: Ubuntu Server 24.04 LTS, type **t3.micro** (free tier eligible), a new RSA `.pem` key pair, 20 GiB storage.
4. Security group: **SSH** from *My IP* only, **HTTP** from anywhere. Never open ports 5432 or 8080.
5. Copy the **Public IPv4 address** of the instance. It is written as `<EC2_PUBLIC_IP>` below.

### 3. Connect and install software

```bash
chmod 400 ~/Downloads/habit-key.pem
ssh -i ~/Downloads/habit-key.pem ubuntu@<EC2_PUBLIC_IP>
```

On the server:

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless nginx postgresql
```

### 4. Add swap memory

The server has only 1 GB RAM, so Java and PostgreSQL need swap to stay stable:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 5. Create the database

```bash
sudo -u postgres psql
```

```sql
CREATE DATABASE mindfocus;
CREATE USER mindfocus WITH PASSWORD 'choose-a-strong-password';
ALTER DATABASE mindfocus OWNER TO mindfocus;
\q
```

Tables are created automatically when the backend starts for the first time.

### 6. Upload the files

On the server, create the folders (one command per line):

```bash
mkdir -p ~/app ~/frontend
sudo mkdir -p /var/www/mindfocus
```

On **your computer** (not on the server):

```bash
scp -i ~/Downloads/habit-key.pem backend/build/libs/mindfocus-backend-0.1.0.jar ubuntu@<EC2_PUBLIC_IP>:~/app/app.jar
scp -i ~/Downloads/habit-key.pem -r frontend/dist/mindfocus/browser ubuntu@<EC2_PUBLIC_IP>:~/frontend
```

On the server, copy the website into the folder Nginx serves:

```bash
sudo rm -rf /var/www/mindfocus/*
sudo cp -r ~/frontend/browser/* /var/www/mindfocus/
ls /var/www/mindfocus
```

The last command must show `index.html` directly, not inside another folder.

### 7. Configure the backend

```bash
sudo nano /etc/mindfocus.env
```

```
DB_URL=jdbc:postgresql://localhost:5432/mindfocus
DB_USER=mindfocus
DB_PASSWORD=choose-a-strong-password
JWT_SECRET=a-long-random-string-of-at-least-40-characters
CORS_ORIGIN=http://<EC2_PUBLIC_IP>
APP_ZONE=Europe/Warsaw
```

No quotes and no spaces around `=`. Then protect the file:

```bash
sudo chmod 600 /etc/mindfocus.env
```

Email reminders need a real SMTP server (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM` in the same file). Without it, in-app reminders still work and only the email fails.

### 8. Run the backend as a service

```bash
sudo nano /etc/systemd/system/mindfocus.service
```

```ini
[Unit]
Description=MindFocus backend
After=network.target postgresql.service

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/app
EnvironmentFile=/etc/mindfocus.env
ExecStart=/usr/bin/java -Xmx400m -jar /home/ubuntu/app/app.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now mindfocus
sudo systemctl status mindfocus --no-pager
```

Startup takes 20 to 60 seconds. Logs: `sudo journalctl -u mindfocus -f`.

### 9. Configure Nginx

```bash
sudo nano /etc/nginx/sites-available/default
```

Replace the whole file with:

```nginx
server {
    listen 80 default_server;
    server_name _;

    root /var/www/mindfocus;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

```bash
sudo nginx -t
sudo systemctl restart nginx
```

### 10. Check that it works

Open `http://<EC2_PUBLIC_IP>` (with `http://`, there is no HTTPS yet). Register a user, log in and create a note. From the server, `curl -i localhost:8080/api/notes` should answer `401`, which means the backend is up.

### Updating the app

1. Rebuild on your computer (step 1) and upload the new files (step 6).
2. New backend jar: `sudo systemctl restart mindfocus`.
3. New frontend: copy the files into `/var/www/mindfocus` again. No restart is needed.

### Backups

```bash
sudo -u postgres pg_dump mindfocus > ~/mindfocus-backup.sql
```

Download the file to your computer with `scp`.

### Optional: HTTPS and browser push

Browser push needs HTTPS, which needs a domain name. A free subdomain (for example from DuckDNS) works. Open port 443 in the security group, point the name at the server's IP, then:

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d yourname.duckdns.org
```

Afterwards set `CORS_ORIGIN=https://yourname.duckdns.org`, add the VAPID variables to `/etc/mindfocus.env`, and restart the backend.

### Server IP and shutting down

- Stopping and starting the instance gives it a new public IP. A plain reboot keeps it. Update `CORS_ORIGIN` after a change, or attach an Elastic IP (it must stay attached to a running instance).
- When you are done, **terminate** the instance in EC2 and release any Elastic IP.

## How it works

- **Privacy:** every table that belongs to a user has a `user_id`. The user id always comes from the JWT (`CurrentUser`), never from the request, and every query is filtered by it. Another user's row answers 404, so its existence is not revealed.
- **Shared CRUD:** `OwnedCrudController` provides list, create, update and delete. A feature controller only says which fields can be edited. Clients cannot choose the `id`, the owner, or server-controlled fields such as `reminderSent`.
- **Error responses:** Spring forwards errors internally to `/error`. That path must be open in `SecurityConfig` (`.requestMatchers("/error").permitAll()`). If it is protected, every 400, 404 or 409 turns into an empty 401, and the frontend interceptor then logs the user out.
- **Reminders:** `ReminderScheduler` runs every minute, finds plans whose `deadline - reminderMinutesBefore` has passed, creates an in-app notification, sends an email and push messages, then marks the plan as reminded. Changing a deadline re-arms the reminder. A mail failure never loses the in-app reminder.
- **Live dashboard:** every successful change in the frontend triggers a refresh signal. The dashboard and the reminder bell listen to it and reload, so the numbers and insights always match the data.
- **Goals:** progress is calculated, not stored, so it can never be out of date.
- **Habit streaks:** the current streak counts successful days up to today. Yesterday only counts if today has no log yet, and a slip logged today resets the streak to 0.

## API overview

All endpoints except `/api/auth/register`, `/api/auth/login` and `/api/push/public-key` need the header `Authorization: Bearer <token>`.

```
POST /api/auth/register | /api/auth/login        GET /api/auth/me
GET POST /api/notes   PUT DELETE /api/notes/{id}
GET POST /api/plans   PUT DELETE /api/plans/{id}
GET POST /api/todos   PUT DELETE /api/todos/{id}
GET POST /api/goals   PUT DELETE /api/goals/{id}     (steps are sent inside the goal)
GET POST /api/habits  PUT DELETE /api/habits/{id}
GET /api/habits/logs?from&to
PUT DELETE /api/habits/{id}/log
GET /api/habits/stats
GET /api/moods        PUT DELETE /api/moods/{yyyy-mm-dd}
GET /api/calendar?from&to
GET /api/analytics/summary?days=14                (days is limited to 1..90)
GET /api/notifications   POST /api/notifications/{id}/seen   POST /api/notifications/seen-all
GET /api/push/public-key   POST /api/push/subscribe
```

Status codes: `400` invalid input, `401` missing or invalid token or wrong login, `404` row does not exist or belongs to another user, `409` email already registered.

## Tests

The tests follow the test pyramid: many fast tests at the bottom, few slow ones at the top.

| Level | Where | Needs | Command |
|---|---|---|---|
| Unit tests | `backend/src/test` (`*Test`) | nothing | `cd backend && gradle test` |
| Integration tests | `backend/src/test` (`*IT`) | Docker (Testcontainers starts PostgreSQL) | `cd backend && gradle integrationTest` |
| API tests (black-box) | `qa` | backend running | `cd qa && gradle apiTest` |
| Browser tests (black-box) | `qa` | backend and frontend running, Chrome | `cd qa && gradle e2eTest` |
| Smoke tests | `qa` | backend and frontend running | `cd qa && gradle smokeTest` |

Start the system before the `qa` tests. If it is not running, every test fails with `ConnectException`, which is not a test problem.

Report with Allure (install the command line tool once, for example `brew install allure`):

```bash
cd qa
allure serve build/allure-results
```

Environment variables for the `qa` project: `API_URL` (default http://localhost:8080), `BASE_URL` (default http://localhost:4200), `HEADLESS=0` to watch the browser. The first browser run needs internet access, because Selenium downloads the Chrome driver.

### Running the black-box tests against the AWS server

The `qa` tests run on your computer and test the deployed site over the network. Do not upload them to the server:

```bash
cd qa
API_URL=http://<EC2_PUBLIC_IP> BASE_URL=http://<EC2_PUBLIC_IP> gradle apiTest
API_URL=http://<EC2_PUBLIC_IP> BASE_URL=http://<EC2_PUBLIC_IP> gradle e2eTest
```

Nginx forwards `/api` to the backend, so both variables point to the same address. Note that these tests register new users in the live database.

One browser test logs in by writing the token into the browser storage. If you rename the `localStorage` keys in the frontend `AuthService`, change them in `BrowserTest.openLoggedIn` in `qa`, or that test times out.

### What the tests found

| ID | Severity | Summary |
|---|---|---|
| DEF-1 | Major | A habit slip logged today did not reset the current streak. |
| DEF-2 | Major | A habit log without a date reached the database and caused a 500 error. It must be a 400. |
| DEF-3 | Minor | Goal steps were not validated: any weight and empty titles were accepted. Weight must be 1 to 100. |
| DEF-4 | Major | The `/error` path was protected, so every 400, 404 and 409 became a 401 and logged the user out. Spring's MockMvc tests could not see it, because they skip the error forward. Only tests against the running server found it. |

More about the strategy, the risk table and known gaps is in `qa/README.md`.

## Troubleshooting

| Problem | Likely cause and fix |
|---|---|
| `Directory ... does not contain a Gradle build` | Run Gradle in `backend` (or `qa`), not in the repository root. |
| `shell-init: error retrieving current directory` or Java says it cannot determine the working directory | The terminal is inside a folder that was deleted or replaced. Run `cd ~/Documents/MindFocusWeb` and try again. |
| `Port 8080 was already in use` | An old backend is still running. Stop it with Ctrl+C, or find it with `lsof -i :8080`. |
| Table not found in the Docker database | Another PostgreSQL uses port 5432 and the app connected to that one. Check with `lsof -i :5432`, stop it, and restart `docker compose` and the backend. |
| `Project "..." does not exist` when running `npm start` | The project name in `frontend/angular.json` and the `buildTarget` names in its `serve` section must be identical. |
| `http proxy error: /api/... ECONNREFUSED` in the frontend terminal, or "Cannot reach the server" in the app | The backend is not running. Start it in `backend` and wait for `Started MindFocusApplication`. If it stopped with a compile error, fix that first. |
| Every test in `qa` fails with `ConnectException` | The backend is not running. Check with `curl -i localhost:8080/api/notes`: `401` means it is up. |
| Browser tests fail with `net::ERR_CONNECTION_REFUSED` | The frontend is not running on http://localhost:4200 (or `BASE_URL` points to the wrong address). |
| Validation errors show as 401, or the user is logged out after a bad input | `/error` is not open in `SecurityConfig` (see "How it works"). Restart the backend after fixing it. |
| One browser test times out right after login | The `localStorage` key names in `BrowserTest.openLoggedIn` do not match the frontend `AuthService`. |
| 401 on every request | The token is missing or expired. Log in again. |
| No emails | Locally open http://localhost:8025 and check `MAIL_HOST` and `MAIL_PORT`. On the server, a real SMTP server must be configured. |
| Reminder never arrives | Deadlines use `APP_ZONE`. Check the time zone, the "remind me" offset, and that the plan is not marked done. |
| Push says it is not configured | The VAPID environment variables are missing. On a server it also needs HTTPS. |
| Strange build errors with a very new Node version | Switch to an LTS version of Node (20 or 22), delete `node_modules` and run `npm install` again. |
| **AWS:** `ls` or `cp` says a path "cannot access" or "cannot stat" | The command ran on the wrong machine. The server prompt starts with `ubuntu@`, your computer's does not. Run build and `scp` commands on your computer, file commands on the server. |
| **AWS:** the browser shows the Nginx welcome page or a blank page | `/var/www/mindfocus` must contain `index.html` directly, and `root` in the Nginx config must point to it. Run `sudo nginx -t` and restart Nginx. |
| **AWS:** 502 Bad Gateway | The backend is not running. Check `sudo systemctl status mindfocus` and `sudo journalctl -u mindfocus -n 80`. |
| **AWS:** the page times out | The security group must allow HTTP (port 80) from anywhere. |
| **AWS:** SSH stopped working | Your home IP changed. Edit the SSH rule in the security group and choose *My IP* again. |
| **AWS:** the backend keeps crashing | Check that swap is active (`free -h`) and look for `OutOfMemory` in the log. |
| **AWS:** login works locally but not on the server | `JWT_SECRET`, `DB_*` or `CORS_ORIGIN` in `/etc/mindfocus.env` is missing or wrong. Fix it, then `sudo systemctl restart mindfocus`. |

## Before going to production

Done on the AWS server:

- [x] Frontend built with `npm run build` and served by Nginx, which forwards `/api` to the backend
- [x] Backend and database not exposed to the internet (only ports 80 and 22 are open)
- [x] Secrets kept in an environment file on the server, not in the repository

Still recommended:

- Replace `ddl-auto: update` with Flyway or Liquibase migrations.
- Set a strong, unique `JWT_SECRET` and serve everything over HTTPS (needs a domain name, see "Optional: HTTPS").
- Configure a real SMTP server for email reminders.
- Schedule regular database backups.
- Do not commit `.env` files, keys or `.pem` files. They are listed in `.gitignore`, together with `qa/.gradle/` and `qa/build/`.

## Ideas for later

- A regression test for the `/error` behaviour inside the backend (it needs a real web server, for example `@SpringBootTest(webEnvironment = RANDOM_PORT)`)
- Angular component tests and a cross-browser run of the UI tests
- Dark "night diary" theme using the colour variables at the top of `frontend/src/styles.scss`
- Export a diary as PDF, weekly summary email, recurring plans
- Automatic deployment with GitHub Actions