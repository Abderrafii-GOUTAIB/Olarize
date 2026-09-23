# Payroll preparation instructions

V1 includes preparation and reconciliation, not a proprietary gross-to-net payroll engine. Own payroll periods, authorized roster snapshots, compensation inputs, attendance/leave variables, exceptional adjustments, validation issues, frozen preparation versions, exports and reconciliation. Integrate with an external payroll process using reviewed CSV/file formats before assuming an API.

Use deterministic Java services and BigDecimal with reviewed currency/rounding rules. Maintain source versions and as-of timestamps. Classify missing/invalid inputs explicitly. Separate preview from persisted preparation and freeze; stale previews require recalculation and renewed confirmation.

Call socialprotection's public calculators with immutable base inputs. Do not reach into another module's repositories. Do not build a circular dependency: socialprotection accepts inputs and returns a traceable result; payroll owns its batch integration.

Expose previewPayrollPreparation and estimateEmployerCost only to authorized HR/payroll roles. Disambiguate names and periods. Employer cost must disclose included gross components, employer contributions, benefits and exclusions; employee deductions are not additional employer costs. Incomplete or forecast values are estimates, not authoritative payable totals. Reconciled provider outcomes must retain their origin.

Batch confirmation binds company, period, employees, input/rule versions and exact operation. Use explicit states for draft, reviewed, frozen, exported and reconciled. A prepared/exported batch is not a salary payment or official declaration. Restrict downloads, audit access and guard spreadsheet exports against formula injection.

Test independent expected totals, rounding, period boundaries, duplicate exports, late attendance corrections, reopened periods, missing scheme config, scoped authorization and concurrent freeze/correction. Never silently substitute zero for unknown monetary input.
