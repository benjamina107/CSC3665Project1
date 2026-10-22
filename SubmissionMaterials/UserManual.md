# ACME Vet Clinic User Manual

This manual covers startup, account management, schedules, and time-off and shift requests for clinic administrators, doctors, and technicians.



## 1. Accounts and signing in

### Initial credentials

| Account | Username | Initial password | Notes |
| --- | --- | --- | --- |
| Administrator | `admin` | `admin` | Exists by default. Password change required on first login. |
| Doctor | Assigned by the administrator | Assigned by the administrator | Created through **Staff Management**. |
| Technician | Assigned by the administrator | Assigned by the administrator | Created through **Staff Management**. |



### Sign in

1. Open the website.
2. Enter your **Username** and **Password**. Passwords are case-sensitive.
3. Select **Sign In**.

Administrators are taken to **Staff Management** (or to **Change Your Password** if this is their first login). Doctors and technicians are taken to **My Schedule**.



### Change the initial password (administrators)

When an administrator signs in for the first time, the application opens **Change Your Password** before granting dashboard access.

1. Enter a **New Password** of at least eight characters. The strength bar rates the password as Weak, Fair, Good, or Strong; adding uppercase letters, numbers, and symbols improves the rating.
2. Enter the same value in **Confirm Password**. The form shows **Passwords match** when the two fields are identical.
3. Select **Set New Password & Continue**.

The new password is saved. Use the new password on every future login.



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

Doctors see their accompanying technicians. Technicians see the doctors they work alongside. The schedule reflects whatever is currently stored in the database; it updates automatically when an administrator regenerates it or when a request you or a coworker made is approved.

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



## 4. Doctor or technician: requests

Select **Time Off Requests** in the sidebar. From this page you can ask for vacation, ask for a sick day, or ask to work a particular shift. Every request is decided immediately, and the page tells you whether it was approved or denied and why.

### Check your balances

The **Vacation Days** and **Sick Days** cards at the top of the page show how many days you have used and how many remain for the calendar year. Each employee receives eight vacation days and four sick days per year. Only approved requests count toward your balance.

### Request vacation or a sick day

1. Open the **New Request** tab (selected by default).
2. Under **Request Type**, select **Vacation** or **Sick Day**.
3. Choose a **Start Date** and **End Date**. For a single day off, use the same date for both. Every calendar day in the range is counted; the total is shown in the preview row below the date fields.
4. Select **Submit Request**.
5. Read the **Request Approved** or **Request Denied** banner and its explanation.

If a request is approved, your balance updates right away. If your time off falls within the next two weeks, the schedule is rebuilt so that you no longer have shifts on those days and your shifts are covered. Time off further ahead is saved and will be respected when the schedule for those dates is created.

### Request to work a shift

Use this if you would like to work a shift you are not currently assigned.

1. Open the **New Request** tab and select **Work Shift** under **Request Type**.
2. Choose a **Date**. Only dates from tomorrow through the next 13 days can be selected, because those are the days that have a schedule.
3. Choose a **Shift**. The list shows the shifts that run on that day with their hours: Monday to Saturday offers Early Morning, 8:30 Shift, Late Doctor, and Overnight; Sunday offers Sunday Shift and Overnight.
4. Select **Submit Request**.
5. Read the **Request Approved** or **Request Denied** banner and its explanation.

If a shift request is approved, you are added to that shift and the schedule is rebuilt around it. Because the whole two-week schedule is rebuilt, other shifts, including some of your own, may change. Check **My Schedule** after an approval.

A shift you request does not have to keep you to four shifts that week, but the usual rest and weekly rules still apply, as described below.

### Request rules

- **Vacation:** must be requested at least three weeks before the start date.
- **Sick day:** cannot be requested for today; any other date is permitted.
- **Calendar year:** a time-off request cannot run across two calendar years. Submit the days in each year as separate requests.
- **Balance:** a request is denied if it would use more days than you have left this year.
- **Overlap:** a request is denied if it overlaps time off that was already approved for you.
- **Shift requests:** you can only request a shift you are not already working, and not on a day you have approved time off.
- **First come, first served:** requests are decided in the order they are submitted. A request that is possible is approved; a request that conflicts with one that was already approved is denied.

### Review request history

Select **History** to see the requests you have made. Each row shows the request type (and the shift, for shift requests), the date or dates, number of days, status (Approved / Denied), and the reason when a request was denied.

The History tab only lists requests made since you opened the page. It starts empty when you reload the page or sign in again, although your approved requests and balances are kept. A form that is incomplete (for example, no date chosen) shows a message but is not added to history.
