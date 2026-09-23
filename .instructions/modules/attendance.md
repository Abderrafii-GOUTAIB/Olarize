# Attendance and absence instructions

Attendance is included in V1. Start with manual and reviewed CSV entry, not biometric integrations. Own schedules, attendance intervals, source provenance, corrections and validated period summaries. Read approved leave through its public interface; never mutate the leave ledger from attendance code.

Interpret the requested date in Africa/Casablanca. Handle overnight intervals and explicit timezone transitions. Distinguish approved absence, recorded attendance, incomplete record and unknown status. Missing clock-in is not proof of misconduct or unpaid absence. Apply reviewed classification and payroll-impact rules only.

Import with a dry run, row-level errors, employee mapping and duplicate detection, followed by authorized confirmation. Audit corrections and invalidate affected draft payroll snapshots; do not silently rewrite frozen/exported batches.

Expose authorized listAbsentEmployees and period-summary APIs. Managers see permitted employees and operational availability, not medical reasons. Test overnight shifts, missing data, leave overlap, corrected records, duplicates and cross-company imports/queries.
