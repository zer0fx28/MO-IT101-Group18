// File: motorph/output/PayrollOutputManager.java
package motorph.output;

import motorph.employee.Employee;
import motorph.hours.AttendanceReader;
import motorph.process.PayrollDateManager;
import motorph.process.PayrollProcessor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Handles display of payroll information
 */
public class PayrollOutputManager {
    private final Scanner scanner;
    private final AttendanceReader attendanceReader;
    private final PayrollProcessor payrollProcessor;
    private final DateTimeFormatter dateFormatter;

    /**
     * Create a new output manager
     */
    public PayrollOutputManager(Scanner scanner, AttendanceReader attendanceReader, PayrollProcessor payrollProcessor) {
        this.scanner = scanner;
        this.attendanceReader = attendanceReader;
        this.payrollProcessor = payrollProcessor;
        this.dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    }

    /**
     * Display main menu
     */
    public void displayMainMenu() {
        System.out.println("\nMAIN MENU:");
        System.out.println("1. Process Payroll");
        System.out.println("2. Find Employee");
        System.out.println("3. View Payroll Calendar");
        System.out.println("4. Exit");
        System.out.print("Enter choice (1-4): ");
    }

    /**
     * Display employee details
     */
    public void displayEmployeeDetails(Employee employee) {
        if (employee == null) {
            return;
        }

        System.out.println("\n===== EMPLOYEE DETAILS =====");
        System.out.println("ID: " + employee.getEmployeeId());
        System.out.println("Name: " + employee.getFullName());
        System.out.println("Position: " + employee.getPosition());
        System.out.println("Basic Salary: ₱" + String.format("%,.2f", employee.getBasicSalary()));
        System.out.println("Rice Subsidy: ₱" + String.format("%,.2f", employee.getRiceSubsidy()));
        System.out.println("Phone Allowance: ₱" + String.format("%,.2f", employee.getPhoneAllowance()));
        System.out.println("Clothing Allowance: ₱" + String.format("%,.2f", employee.getClothingAllowance()));
        System.out.println("Hourly Rate: ₱" + String.format("%.2f", employee.getHourlyRate()));

        System.out.println("\nOptions:");
        System.out.println("1. View Attendance");
        System.out.println("2. Process Payroll");
        System.out.println("3. Return to Main Menu");
        System.out.print("Enter choice: ");
    }

    /**
     * Display payroll summary
     */
    public Map<String, Object> displayPayrollSummary(Employee employee, LocalDate startDate, LocalDate endDate, int payPeriodType) {
        if (employee == null) {
            return null;
        }

        System.out.println("\n===== PAYROLL SUMMARY =====");
        System.out.println("Employee: " + employee.getFullName() + " (ID: " + employee.getEmployeeId() + ")");
        System.out.println("Period: " + startDate.format(dateFormatter) + " to " + endDate.format(dateFormatter));
        System.out.println("Payroll Type: " + (payPeriodType == PayrollDateManager.MID_MONTH ? "Mid-month" : "End-month"));

        // Get daily attendance records
        Map<LocalDate, Map<String, Object>> dailyAttendance =
                attendanceReader.getDailyAttendanceForEmployee(employee.getEmployeeId(), startDate, endDate);

        if (dailyAttendance.isEmpty()) {
            System.out.println("\nNo attendance records found for this period.");
            return null;
        }

        // Calculate totals
        double totalHours = 0;
        double totalOvertimeHours = 0;
        double totalLateMinutes = 0;
        double totalUndertimeMinutes = 0;
        boolean isLateAnyDay = false;
        boolean hasUnpaidAbsences = false;
        int unpaidAbsenceCount = 0;

        // Display daily breakdown
        System.out.println("\n--- ATTENDANCE DETAILS ---");
        System.out.printf("%-12s %-10s %-10s %-10s %-10s %-10s %-15s\n",
                "Date", "Time In", "Time Out", "Hours", "OT Hours", "Late", "Absence Type");
        System.out.println("--------------------------------------------------------------------------------");

        for (Map.Entry<LocalDate, Map<String, Object>> entry : dailyAttendance.entrySet()) {
            LocalDate date = entry.getKey();
            Map<String, Object> dayData = entry.getValue();

            String timeIn = (String) dayData.get("timeIn");
            String timeOut = (String) dayData.get("timeOut");
            double hours = (double) dayData.get("hours");
            double overtimeHours = (double) dayData.get("overtimeHours");
            double lateMinutes = (double) dayData.get("lateMinutes");
            double undertimeMinutes = (double) dayData.getOrDefault("undertimeMinutes", 0.0);
            boolean isLate = (boolean) dayData.get("isLate");
            String absenceType = (String) dayData.getOrDefault("absenceType", "");
            boolean isUnpaidAbsence = false;

            if (dayData.containsKey("isUnpaidAbsence")) {
                isUnpaidAbsence = (boolean) dayData.get("isUnpaidAbsence");
            } else if (absenceType != null && !absenceType.isEmpty()) {
                // For backward compatibility when there's no isUnpaidAbsence field
                String type = absenceType.toLowerCase();
                isUnpaidAbsence = type.contains("unpaid") ||
                        type.contains("unauthoriz") ||
                        type.contains("unapproved");
            }

            // Update totals
            totalHours += hours;
            totalOvertimeHours += overtimeHours;
            totalLateMinutes += lateMinutes;
            totalUndertimeMinutes += undertimeMinutes;

            if (isLate) {
                isLateAnyDay = true;
            }

            if (isUnpaidAbsence) {
                hasUnpaidAbsences = true;
                unpaidAbsenceCount++;
            }

            // Format the line
            System.out.printf("%-12s %-10s %-10s %-10.2f %-10.2f %-10s %-15s\n",
                    date.format(dateFormatter),
                    timeIn, timeOut, hours, overtimeHours,
                    (lateMinutes > 0 ? lateMinutes + " min" : "-"),
                    absenceType != null ? absenceType : "-");
        }

        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-34s %-10.2f %-10.2f %-10.2f\n",
                "TOTALS:", totalHours, totalOvertimeHours, totalLateMinutes);

        // Create summary for return
        Map<String, Object> summary = new HashMap<>();
        summary.put("hours", totalHours);
        summary.put("overtimeHours", totalOvertimeHours);
        summary.put("lateMinutes", totalLateMinutes);
        summary.put("undertimeMinutes", totalUndertimeMinutes);
        summary.put("isLateAnyDay", isLateAnyDay);
        summary.put("hasUnpaidAbsences", hasUnpaidAbsences);
        summary.put("unpaidAbsenceCount", unpaidAbsenceCount);

        return summary;
    }

    /**
     * Display salary details
     */
    public void displaySalaryDetails(Employee employee) {
        payrollProcessor.displaySalaryDetails(employee);
    }

    /**
     * Display attendance options
     */
    public void displayAttendanceOptions(Employee employee) {
        System.out.println("\n===== ATTENDANCE OPTIONS =====");
        System.out.println("Employee: " + employee.getFullName());
        System.out.println("\nSelect view type:");
        System.out.println("1. Daily Attendance");
        System.out.println("2. Weekly Summary");
        System.out.print("Enter choice (1-2): ");
    }

    /**
     * Display daily attendance
     */
    public void displayDailyAttendance(Employee employee, LocalDate startDate, LocalDate endDate) {
        System.out.println("\n===== DAILY ATTENDANCE =====");
        System.out.println("Employee: " + employee.getFullName());
        System.out.println("Period: " + startDate.format(dateFormatter) + " to " + endDate.format(dateFormatter));

        // Get daily attendance records
        Map<LocalDate, Map<String, Object>> dailyAttendance =
                attendanceReader.getDailyAttendanceForEmployee(employee.getEmployeeId(), startDate, endDate);

        if (dailyAttendance.isEmpty()) {
            System.out.println("\nNo attendance records found for this period.");
            return;
        }

        // Display daily breakdown
        System.out.println("\n--- ATTENDANCE DETAILS ---");
        System.out.printf("%-12s %-10s %-10s %-10s %-10s %-10s %-10s %-15s\n",
                "Date", "Time In", "Time Out", "Hours", "OT Hours", "Late", "Undertime", "Absence Type");
        System.out.println("----------------------------------------------------------------------------------------");

        double totalHours = 0;
        double totalOvertimeHours = 0;
        double totalLateMinutes = 0;
        double totalUndertimeMinutes = 0;
        int unpaidAbsenceCount = 0;

        for (Map.Entry<LocalDate, Map<String, Object>> entry : dailyAttendance.entrySet()) {
            LocalDate date = entry.getKey();
            Map<String, Object> dayData = entry.getValue();

            String timeIn = (String) dayData.get("timeIn");
            String timeOut = (String) dayData.get("timeOut");
            double hours = (double) dayData.get("hours");
            double overtimeHours = (double) dayData.get("overtimeHours");
            double lateMinutes = (double) dayData.get("lateMinutes");
            double undertimeMinutes = (double) dayData.getOrDefault("undertimeMinutes", 0.0);
            String absenceType = (String) dayData.getOrDefault("absenceType", "");
            boolean isUnpaidAbsence = false;

            if (dayData.containsKey("isUnpaidAbsence")) {
                isUnpaidAbsence = (boolean) dayData.get("isUnpaidAbsence");
            } else if (absenceType != null && !absenceType.isEmpty()) {
                // For backward compatibility when there's no isUnpaidAbsence field
                String type = absenceType.toLowerCase();
                isUnpaidAbsence = type.contains("unpaid") ||
                        type.contains("unauthoriz") ||
                        type.contains("unapproved");
            }

            // Update totals
            totalHours += hours;
            totalOvertimeHours += overtimeHours;
            totalLateMinutes += lateMinutes;
            totalUndertimeMinutes += undertimeMinutes;

            if (isUnpaidAbsence) {
                unpaidAbsenceCount++;
            }

            // Format the line
            System.out.printf("%-12s %-10s %-10s %-10.2f %-10.2f %-10s %-10s %-15s\n",
                    date.format(dateFormatter),
                    timeIn, timeOut, hours, overtimeHours,
                    (lateMinutes > 0 ? lateMinutes + " min" : "-"),
                    (undertimeMinutes > 0 ? undertimeMinutes + " min" : "-"),
                    (absenceType != null && !absenceType.isEmpty() ? absenceType : "-"));
        }

        System.out.println("----------------------------------------------------------------------------------------");
        System.out.printf("%-34s %-10.2f %-10.2f %-10.2f %-10.2f\n",
                "TOTALS:", totalHours, totalOvertimeHours, totalLateMinutes, totalUndertimeMinutes);

        if (unpaidAbsenceCount > 0) {
            System.out.println("Unpaid Absences: " + unpaidAbsenceCount + " day(s)");
        }

        // Wait for user
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Display weekly attendance
     */
    public void displayWeeklyAttendance(Employee employee, LocalDate startDate, LocalDate endDate) {
        System.out.println("\n===== WEEKLY ATTENDANCE =====");
        System.out.println("Employee: " + employee.getFullName());
        System.out.println("Period: " + startDate.format(dateFormatter) + " to " + endDate.format(dateFormatter));

        // Get weekly attendance records
        Map<String, Object> weeklyAttendanceData =
                attendanceReader.getWeeklyAttendanceWithDailyLogs(employee.getEmployeeId(), startDate, endDate);

        if (weeklyAttendanceData.isEmpty()) {
            System.out.println("\nNo attendance records found for this period.");
            return;
        }

        // Get the weekly records and daily logs by week
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> weeklyRecords =
                (Map<String, Map<String, Object>>) weeklyAttendanceData.get("weeklyRecords");

        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> dailyLogsByWeek =
                (Map<String, List<Map<String, Object>>>) weeklyAttendanceData.get("dailyLogsByWeek");

        if (weeklyRecords.isEmpty()) {
            System.out.println("\nNo weekly records found.");
            return;
        }

        // Display weekly summary
        System.out.println("\n--- WEEKLY SUMMARY ---");
        System.out.printf("%-30s %-10s %-12s %-12s %-10s\n",
                "Week", "Hours", "Overtime", "Late (min)", "Unpaid Abs");
        System.out.println("--------------------------------------------------------------------------------");

        double totalHours = 0;
        double totalOvertimeHours = 0;
        double totalLateMinutes = 0;
        int totalUnpaidAbsences = 0;

        for (String weekLabel : weeklyRecords.keySet()) {
            Map<String, Object> weekData = weeklyRecords.get(weekLabel);

            double hours = 0;
            double overtimeHours = 0;
            double lateMinutes = 0;
            int unpaidAbsences = 0;

            // Extract values with type safety
            if (weekData.containsKey("hours")) {
                if (weekData.get("hours") instanceof Double) {
                    hours = (Double) weekData.get("hours");
                } else {
                    hours = ((Number) weekData.get("hours")).doubleValue();
                }
            }

            if (weekData.containsKey("overtimeHours")) {
                if (weekData.get("overtimeHours") instanceof Double) {
                    overtimeHours = (Double) weekData.get("overtimeHours");
                } else {
                    overtimeHours = ((Number) weekData.get("overtimeHours")).doubleValue();
                }
            }

            if (weekData.containsKey("lateMinutes")) {
                if (weekData.get("lateMinutes") instanceof Double) {
                    lateMinutes = (Double) weekData.get("lateMinutes");
                } else {
                    lateMinutes = ((Number) weekData.get("lateMinutes")).doubleValue();
                }
            }

            if (weekData.containsKey("unpaidAbsenceCount")) {
                if (weekData.get("unpaidAbsenceCount") instanceof Integer) {
                    unpaidAbsences = (Integer) weekData.get("unpaidAbsenceCount");
                } else {
                    unpaidAbsences = ((Number) weekData.get("unpaidAbsenceCount")).intValue();
                }
            }

            // Add to totals
            totalHours += hours;
            totalOvertimeHours += overtimeHours;
            totalLateMinutes += lateMinutes;
            totalUnpaidAbsences += unpaidAbsences;

            // Format the line
            System.out.printf("%-30s %-10.2f %-12.2f %-12.2f %-10d\n",
                    weekLabel, hours, overtimeHours, lateMinutes, unpaidAbsences);
        }

        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-30s %-10.2f %-12.2f %-12.2f %-10d\n",
                "TOTALS:", totalHours, totalOvertimeHours, totalLateMinutes, totalUnpaidAbsences);

        // Wait for user
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Display payroll calendar
     */
    public void displayPayrollCalendar(int year, int month) {
        System.out.println("\n===== PAYROLL CALENDAR =====");
        System.out.println("Year: " + year);
        System.out.println("Month: " + PayrollDateManager.getMonthName(month));

        // Calculate payroll dates
        LocalDate midMonth = PayrollDateManager.getPayrollDate(year, month, PayrollDateManager.MID_MONTH);
        LocalDate endMonth = PayrollDateManager.getPayrollDate(year, month, PayrollDateManager.END_MONTH);

        // Get cutoff periods
        LocalDate[] midCutoff = PayrollDateManager.getCutoffDateRange(midMonth, PayrollDateManager.MID_MONTH);
        LocalDate[] endCutoff = PayrollDateManager.getCutoffDateRange(endMonth, PayrollDateManager.END_MONTH);

        // Display payroll information
        System.out.println("\n--- MID-MONTH PAYROLL ---");
        System.out.println("Payroll Date: " + midMonth.format(dateFormatter));
        System.out.println("Cutoff Period: " + midCutoff[0].format(dateFormatter) +
                " to " + midCutoff[1].format(dateFormatter));
        System.out.println("Deductions: SSS, PhilHealth, Pag-IBIG");

        System.out.println("\n--- END-MONTH PAYROLL ---");
        System.out.println("Payroll Date: " + endMonth.format(dateFormatter));
        System.out.println("Cutoff Period: " + endCutoff[0].format(dateFormatter) +
                " to " + endCutoff[1].format(dateFormatter));
        System.out.println("Deductions: Withholding Tax");
    }
}