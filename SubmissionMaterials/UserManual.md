# ACME Vet Clinic User Manual

This manual covers startup, account management, schedules, and time-off requests for clinic administrators, doctors, and technicians.



## 1. Accounts and signing in

### Initial credentials

| Account | Username | Initial password | Notes |
| --- | --- | --- | --- |
| Administrator | `admin` | `admin` | Exists by default. Password change required on first login. |
| Doctor | Assigned by the administrator | Assigned by the administrator | Created through **Staff Management**. |
| Technician | Assigned by the administrator | Assigned by the administrator | Created through **Staff Management**. |



### Sign in

1. Open the website. The login page appears with a teal panel on the left and a sign-in form on the right.
2. Enter your **Username** and **Password**. Passwords are case-sensitive.
3. Use the eye icon to show or hide the password.
4. Select **Sign In**.

Administrators are taken to **Staff Management** (or to **Change Your Password** if this is their first login). Doctors and technicians are taken to **My Schedule**.



### Change the initial password (administrators)

When an administrator signs in for the first time, the application opens **Change Your Password** before granting dashboard access.

1. Enter a **New Password** of at least eight characters. The strength bar rates the password as Weak, Fair, Good, or Strong; adding uppercase letters, numbers, and symbols improves the rating.
2. Enter the same value in **Confirm Password**. The form shows **Passwords match** when the two fields are identical.
3. Select **Set New Password & Continue**.

The new password is saved and the first-login flag is cleared. Use the new password on every future login.



### Sign out

Select **Sign Out** at the bottom of the sidebar. The application returns to the login screen.



## 2. Administrator: staff management

Select **Staff Management** in the sidebar. Summary cards show the current number of doctors and technicians. Use the **Doctors** and **Technicians** tabs to switch between the two lists. The **Generate next 14 days** button is in the upper right.

There is only one administrator account; the admin cannot be added or removed through this screen.

### Add a doctor or technician

1. Select the appropriate tab (**Doctors** or **Technicians**).
2. Select **Add Doctor** or **Add Technician**.
3. In the slide-over panel, enter **Full Name**, **Username**, and **Temporary Password**. All fields are required; the temporary password must be at least six characters.
4. Select **Add Staff Member**.
5. Wait for **Staff member added successfully!** and confirm that the person appears in the list.
6. Close the panel using **Cancel**, the **×** button, or by clicking outside the panel. Closing after a successful addition does not undo it.
7. Give the employee their username and temporary password.

### Reset a staff password

1. Hover over the employee's row to reveal the action buttons.
2. Select **Reset Password**.
3. Enter a new temporary password of at least six characters.
4. Select **Reset**, or select **Cancel** to leave the password unchanged.

The replacement password is saved immediately.

### Remove a staff member

1. Hover over the employee's row.
2. Select **Remove**.
3. Read the confirmation dialog, which shows the employee's name.
4. Select **Remove** to permanently delete the account, or **Cancel** to keep it.

Removal cannot be undone.

### Generate the schedule

1. Select **Generate next 14 days** in the upper right of the Staff Management page.
2. Confirm the dialog that warns existing assignments in the date range will be replaced.
3. Wait for the confirmation message **The schedule for the next 14 days was generated.**

If the current staffing cannot cover all shifts while respecting rest and weekly limits, an error message is shown and the existing schedule is left unchanged. At least one doctor and at least two technicians must exist before generation can succeed.



## 3. Doctor or technician: view a schedule

Select **My Schedule** in the sidebar. The page loads your shifts for today through the next 13 days from the server.

1. Select **Week 1** or **Week 2** to switch the displayed week. The date range and shift count for the selected week appear above the table.
2. Find a row by day and date, then read its shift type, hours, and coworkers.
3. Use the color legend in the top-right of the summary bar to identify shift types. Surgery shifts are highlighted in amber. A **Day Off** row means no shift is assigned for that day.

Doctors see their accompanying technicians. Technicians see the doctors they work alongside. The schedule reflects whatever is currently stored in the database; it updates automatically when an administrator regenerates it.

### Shift reference

| Shift | Hours |
| --- | --- |
| Early Morning | 7:30 a.m. – 6:30 p.m. |
| 8:30 Shift | 8:30 a.m. – 7:30 p.m. |
| Late Doctor | 9:30 a.m. – 8:30 p.m. |
| Surgery | 7:30 a.m. – 6:30 p.m. (Early Morning flagged for surgery) |
| Sunday Shift | 8:00 a.m. – 8:00 p.m. |
| Overnight | 8:00 p.m. – 8:00 a.m. the following day |

### Scheduling rules

Monday through Saturday require Early Morning, 8:30 Shift, Late Doctor, and Overnight coverage. Sunday requires Sunday Shift and Overnight coverage. Early Morning shifts may be designated as surgery shifts; all doctors assigned to that shift are shown as working surgery. Each required shift has exactly one doctor; extra doctor coverage prioritizes Saturday Early Morning and Friday Late Doctor, then other Early Morning shifts. The generator targets approximately four shifts per employee per week.

At least one technician accompanies each doctor on a shift. Overnight has one technician; Sunday Shift has two. At least ten hours of rest are enforced between any two shifts for the same employee. Doctors may not work more than one surgery shift or more than one overnight per week.



## 4. Doctor or technician: time-off requests

Select **Time Off Requests** in the sidebar.

### Check your balances

The **Vacation Days** and **Sick Days** cards at the top of the page show how many days you have used and how many remain for the calendar year. Each employee receives eight vacation days and four sick days per year.

### Submit a request

1. Open the **New Request** tab (selected by default).
2. Select **Vacation** or **Sick Day**.
3. Choose a **Start Date** and **End Date**. For a single day off, use the same date for both. Every calendar day in the range is counted; the total is shown in the preview row below the date fields.
4. Optionally enter **Notes**.
5. Select **Submit Request**.
6. Read the **Request Approved** or **Request Denied** banner and its explanation.

On approval, your balance is updated immediately and the request appears at the top of your history.

### Request policies

- **Vacation:** must be submitted at least three weeks before the start date.
- **Sick Day:** cannot be submitted for today; any other date is permitted.
- A request is denied if it would exceed your remaining balance.
- Requests are processed immediately on a first-come, first-served basis.

### Review request history

Select **History** to view all past requests. Each row shows the request type, start date, end date, number of days, status (Approved / Denied / Pending), and the denial reason where applicable. Requests denied by the form before submission are not added to history.