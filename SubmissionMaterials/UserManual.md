# ACME Vet Clinic User Manual

This manual covers startup, account management, schedules, and requests for the clinic's administrators, doctors, and technicians. It includes the complete intended workflow. Sections marked **Preview** describe screens that currently use sample data; sections marked **Planned** describe functionality that still needs to be implemented.



## 1. Accounts and signing in

### Initial credentials

| Account | Username | Initial password | Setup |
| --- | --- | --- | --- |
| Administrator | `admin` | `admin` | The admin account exists to begin with. |
| Doctor | Assigned by the administrator | Assigned  password | Created through **Staff Management** or inserted by the database maintainer with type `Doctor`. |
| Technician | Assigned by the administrator | Assigned  password | Created through **Staff Management** or inserted by the database maintainer with type `Technician`. |



### Sign in

1. Open the website.
2. Enter your **Username** and **Password**. Passwords must match the stored value exactly, including capitalization.
3. Use the eye icon if you need to show or hide the password.
4. Select **Sign In**.

Administrators go to **Staff Management**, unless a first-login password change is required. Doctors and technicians go to **My Schedule**.



### Change the initial password

When an administrator signs in with `first_login = true`, the application opens **Change Your Password** before allowing access to the dashboard.

1. Enter a **New Password** with at least eight characters.
2. Enter the same value in **Confirm Password**.
3. Check that the page reports **Passwords match**. The strength indicator provides feedback; uppercase letters, numbers, and symbols improve the rating.
4. Select **Set New Password & Continue**.

The new password is saved to the database and the first-login flag is cleared. Use the new password for future logins.



### Sign out

Select **Sign Out** at the bottom of the sidebar. The application returns to the login screen.

## 2. Administrator: staff management

Select **Staff Management** in the sidebar. The summary cards show the number of doctors and technicians loaded from the database. Use the **Doctors** and **Technicians** tabs to switch lists.

There is only one administrator account, so admins cannot be added or removed by this form.
### Add a doctor or technician

1. Select the appropriate staff tab.
2. Select **Add Doctor** or **Add Technician**.
3. Enter **Full Name**, **Username**, and **Temporary Password**. All fields are required, and the temporary password must contain at least six characters.
4. Select **Add Staff Member**.
5. Wait for **Staff member added successfully!** and confirm that the person appears in the list.
6. Close the panel using **Cancel**, the X, or the area outside the panel. Closing it after a successful addition does not undo the addition.
7. Give the employee their username and  password.



### Reset a staff password

1. Select the employee's tab and hover over their row to reveal the action buttons.
2. Select **Reset Password**.
3. Enter a new temporary password of at least six characters.
4. Select **Reset**, or select **Cancel** to leave the password unchanged.
5. Give the employee the replacement password.

The replacement password is saved.
### Remove a staff member

1. Find the employee and hover over their row.
2. Select **Remove**.
3. Check the name in the confirmation dialog.
4. Select **Remove** to delete the account, or **Cancel** to keep it.



## 3. Doctor or technician: view a schedule

**Preview:** the schedule page currently displays fixed sample schedules by employee role. It does not yet retrieve individual assignments or the actual next two weeks from the database.

1. Sign in as a doctor or technician and select **My Schedule**.
2. Select **Week 1** or **Week 2** to switch the displayed week.
3. Read the date range and shift count above the table.
4. Find a row by day and date, then read its shift, hours, and coworkers.
5. Use the color legend to identify shift types. Surgery shifts are highlighted in amber; **Day Off** means no assigned shift.

Doctors see their accompanying technicians. Technicians see the doctor they work with. The intended completed version will show the signed-in employee's real assignments for the next two weeks and update them after approved requests.

### Shift reference

| Shift | Hours |
| --- | --- |
| Early Morning | 7:30 a.m. to 6:30 p.m. |
| 8:30 Shift | 8:30 a.m. to 7:30 p.m. |
| Late Doctor | 9:30 a.m. to 8:30 p.m. |
| Surgery | 7:30 a.m. to 6:30 p.m. |
| Sunday Shift | 8:00 a.m. to 8:00 p.m. |
| Overnight | 8:00 p.m. to 8:00 a.m. the following day |

### Scheduling rules (Planned)

The scheduler is intended to assign approximately four shifts per employee each week, with at least ten hours between shifts. A doctor may not have two surgery days or two overnight shifts in the same week.

Monday through Saturday require Early Morning, 8:30 Shift, Late Doctor, and surgery coverage, together with overnight coverage. Sunday requires Sunday Shift and overnight coverage. Each shift must have at least one doctor; Sunday and overnight shifts must not have two doctors. Extra doctor coverage prioritizes Saturday Early Morning and Friday Late Doctor, then other Early Morning shifts.

At least one technician must accompany each doctor. Overnight requires one technician, and Sundays require two technicians. Remaining technician shifts should be distributed as evenly as possible. Administrators are separate from the doctor and technician workforce.

Automatic schedule generation and enforcement of these rules are not yet available. There is currently no schedule-generation or schedule-editing control in the administrator dashboard.

## 4. Doctor or technician: time off requests

**Preview:** the form, balances, and history currently run with sample data in the browser. An approval on this screen does not save a request to the database or change a real schedule. Leaving the page or refreshing it resets these values.

### Check your balances

Select **Time Off Requests** in the sidebar. The **Vacation Days** and **Sick Days** cards show used and remaining days. The intended annual allowance is eight vacation days and four sick days per employee per calendar year.

The preview starts with two vacation days and one sick day used. These values are examples, not the signed-in employee's actual usage. Database-backed annual usage tracking is planned.

### Submit vacation or sick leave

1. Open the **New Request** tab.
2. Select **Vacation** or **Sick Day**.
3. Choose a **Start Date** and **End Date**. For a single day, use the same date for both.
4. Check the selected day count and estimated remaining balance. Both endpoints are included, and the current form counts every calendar day in the range.
5. Optionally enter **Notes**.
6. Select **Submit Request**.
7. Read the **Request Approved** or **Request Denied** banner and its explanation.

The form rejects an end date before the start date and requests exceeding the displayed allowance. The intended policies require vacation requests at least three weeks in advance and prohibit a sick request for today while allowing other days.

**Current validation limitation:** the preview uses September 15, 2026 as its reference date instead of the current date. Its sick-day check only tests the start date. Requests passing the local checks are automatically approved without checking staffing or conflicts. Notes are not saved into request history.

### Review request history

Select **History** to view request type, start and end dates, number of days, status, and a denial explanation where supplied. Newly approved preview requests appear first. Requests rejected by local validation show a banner but are not added to the history table.

### Completed request processing (Planned)

In the completed system, the same submission workflow will save the request, check actual annual usage and staffing, and return an immediate decision. Requests will be processed first-come, first-served. Feasible requests will be granted and the schedule rearranged accordingly. A request conflicting with a previously granted request will be denied with an explanation. Approved leave will update the employee's usage and schedule; it will not disappear on refresh.

## 5. Request specific work dates and times (Planned)

Doctors and technicians are intended to be able to request the dates and times they work. This control is not yet present; **Time Off Requests** currently offers only Vacation and Sick Day.

Once implemented, the workflow will be to choose a work date and shift or time period, submit the request, and read the immediate approval or denial with its explanation. The scheduler will apply the same first-come, first-served conflict rules, staffing requirements, rest periods, and weekly restrictions. After approval, check **My Schedule** for the resulting assignment and coworkers. Exact button names and placement are still to be determined.


