# Issue Tracker (Android)

A React Native (Expo) version of the CIS 355 PHP/MySQL issue tracker.
The database is SQLite and lives on the phone, so the app works with no
server and no internet connection.

## Run it on your Android phone

You need Node.js (LTS) on your computer and the **Expo Go** app from the
Google Play Store on your phone.

```bash
cd issue-tracker
npm install
npx expo start
```

Open Expo Go and scan the QR code shown in the terminal. Your phone and
computer must be on the same Wi-Fi. If the QR code won't connect (common on
campus networks), run `npx expo start --tunnel` instead.

The first account you create on the phone becomes an admin. Everyone after
that joins as a regular member, and admins can promote them from the People tab.

## Where the PHP pages went

| Website page                    | App                                  |
| ------------------------------- | ------------------------------------ |
| login.php, login_error.html     | Login screen (errors show inline)    |
| iss_per_create.php              | Create account                       |
| iss_issues.php                  | Issues tab (Open/Closed/All, filter, sort) |
| iss_my_issues.php               | Mine tab (update, resolve/reopen, delete) |
| iss_create_issues.php           | Create tab                           |
| iss_update_issue.php            | Update issue                         |
| iss_issue_description.php       | Issue screen with comments           |
| iss_issue_new_comment.php, iss_update_comment.php | Comment screen     |
| iss_per_list.php                | People tab (admin controls)          |
| iss_per.php, iss_update_per.php | Me tab / Person screen, Update profile |
| logout.php                      | Log out button on the Me tab         |
| database.php, $_SESSION         | src/db/ (SQLite) and src/auth/       |

## Project layout

```
App.js                  opens the database, then shows the app
src/db/schema.js        table definitions (same tables as MySQL)
src/db/queries.js       every SQL query the app runs
src/auth/AuthContext.js login, sign-up, logout, password hashing
src/Navigation.js       tabs and screens
src/screens/            one file per screen
src/components/ui.js    shared buttons, fields, cards
```

## Differences from the website

- Open issues have `close_date = NULL` instead of `'0000-00-00'`.
- Passwords use salted SHA-256 instead of MD5.
- Deleting a person also deletes their issues and comments (the website
  left them orphaned).
- Only admins can delete people, and the last admin can't be demoted, so the
  app can't get locked out of admin controls.
- Issue creators can delete their own issues from the Issue screen as well as
  from Mine.

## Data

The database file (`cis355.db`) is stored in the app's private storage.
Clearing Expo Go's storage, or uninstalling it, deletes the data.
