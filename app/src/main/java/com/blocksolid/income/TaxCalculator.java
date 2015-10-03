package com.blocksolid.income;

public class TaxCalculator {

    // 2015-16 Tax constants
    private static final int MAX_PERSONAL_ALLOWANCE = 1060000;           // £10,600
    private static final int PERSONAL_ALLOWANCE_THRESHOLD = 10000000;    // £100,000
    private static final int TAX_BASIC_RATE_THRESHOLD = 3178500;             // £31,785
    private static final int TAX_HIGHER_RATE_THRESHOLD = 15000000;           // £150,000
    private static final double TAX_BASIC_RATE = 0.2;                        // 20%
    private static final double TAX_HIGHER_RATE = 0.4;                       // 40%
    private static final double TAX_ADDITIONAL_RATE = 0.45;                  // 45%

    // 2015-16 National Insurance constants
    // All values and calculations assume National Insurance Category A
    private static final int LEL_NI = 582400;            // £5,824       Lower Earnings Limit
    private static final int PT_NI = 806000;             // £8,060       Primary Threshold
    private static final int ST_NI = 811200;             // £8,112       Secondary Threshold
    private static final int UAP_NI = 4004000;           // £40,040      Upper Accrual Point
    private static final int UST_NI = 4238500;           // £42,385      Upper Secondary Threshold (under 21)
    private static final int UEL_NI = 4238500;           // £42,385      Upper Earnings Limit
    private static final double LOWER_RATE_NI = 0.02;    //  2.00%
    private static final double MIDDLE_RATE_NI = 0.0585; //  5.85%
    private static final double UPPER_RATE_NI = 0.106;   // 10.60%
    private static final double TOP_RATE_NI = 0.12;      // 12.00%

    int grossAnnualIncome = 0;
    int personalAllowance = MAX_PERSONAL_ALLOWANCE;
    int basicRateDeduction = 0;
    int higherRateDeduction = 0;
    int additionalRateDeduction = 0;
    int totalTaxDeductions = 0;
    int nationalInsuranceContributions = 0;
    int totalDeductions = 0;
    int netAnnualIncome = 0;

    public TaxCalculator() {

    }

    public void setGrossAnnualIncome(int newGrossAnnualIncome) {
        grossAnnualIncome = newGrossAnnualIncome;
        // All calculations should be done each time the salary is set
        calculateNetAnnualIncome();
    }

    public int getGrossAnnualIncome() {
        return grossAnnualIncome;
    }

    public int calculatePersonalAllowance() {

        /**
         * The standard Personal Allowance is £10,600, which is the amount of income that tax will
         * not be paid on.
         *
         * A persons Personal Allowance may be bigger if they were born before 6 April 1938 or if
         * they get Blind Person’s Allowance.
         *
         * The Personal Allowance goes down by £1 for every £2 that the adjusted net income is
         * above £100,000. This means the allowance is zero if a persons income is £121,200 or over.
         */

        if (grossAnnualIncome > PERSONAL_ALLOWANCE_THRESHOLD) {
            int difference = grossAnnualIncome - PERSONAL_ALLOWANCE_THRESHOLD;
            // £1 is removed for each £2 earned above the personal allowance
            // Remove the odd pound so that the difference can be divided by 2
            if (difference % 200 > 0) {
                difference = difference - 100;
            }
            personalAllowance = MAX_PERSONAL_ALLOWANCE - (difference / 2);
            // If the calculation results in a negative number, set personal allowance to 0
            if (personalAllowance < 0) {
                personalAllowance = 0;
            }
        } else {
            // Personal allowance set to the maximum if salary is below the threshold
            personalAllowance = MAX_PERSONAL_ALLOWANCE;
        }
        return personalAllowance;
    }

    public int calculateTotalDeductions() {
        totalDeductions = calculateTotalTaxDeductions() + calculateNationalInsuranceContributions();
        return totalDeductions;
    }

    public int calculateTotalTaxDeductions() {

        /**
         * If I have a salary of 180,000
         * I need to calculate the 45% tax I pay on the amount over £150,000 -> (30000 * 0.45 = 13500)
         * I need to calculate the 40% tax I pay on the amount between £31,785 and £150,000 -> (118215 * 0.40 = 47286)
         * I need to calculate the 20% tax I pay on the amount between £0 and £31,785 -> (31785 * 0.20 = 6357)
         * I need to add these together to get the total amount deducted (13500 + 47286 + 6357 = 67143)
         * I need to subtract this from my starting salary to get my net income (180000 - 67143 = 112857)
         */
        /**
         * If I have a salary of 100,000
         * I need to calculate the 45% tax I pay on the amount over £150,000 -> (0 * 0.45 = 0)
         * I need to calculate the 40% tax I pay on the amount between £42,385 and £150,000 -> (57615 * 0.40 = 23046)
         * I need to calculate the 20% tax I pay on the amount between £10,600 and £42,385 -> (31785 * 0.20 = 6357)
         * I need to add these together to get the total amount deducted (0 + 23046 + 6357 = 29403)
         * I need to subtract this from my starting salary to get my net income (100000 - 29403 = 70597)
         */

        // Calculate additional rate tax deduction
        if (grossAnnualIncome > TAX_HIGHER_RATE_THRESHOLD) {
            additionalRateDeduction = (int)(TAX_ADDITIONAL_RATE * (grossAnnualIncome - TAX_HIGHER_RATE_THRESHOLD));
        } else {
            additionalRateDeduction = 0;
        }

        // Calculate higher rate tax deduction
        personalAllowance = calculatePersonalAllowance();
        int assessedBasicRateThreshold = TAX_BASIC_RATE_THRESHOLD + personalAllowance;
        int newHigherRateThreshold;
        if (grossAnnualIncome > assessedBasicRateThreshold) {
            if (grossAnnualIncome > TAX_HIGHER_RATE_THRESHOLD) {
                // Possibly set a boolean here to avoid doing this same calculation twice
                newHigherRateThreshold = TAX_HIGHER_RATE_THRESHOLD;
            } else {
                newHigherRateThreshold = grossAnnualIncome;
            }
            higherRateDeduction = (int)(TAX_HIGHER_RATE * (newHigherRateThreshold - assessedBasicRateThreshold));
        } else {
            higherRateDeduction = 0;
        }

        // Calculation basic rate tax deduction
        if (grossAnnualIncome > personalAllowance) {
            // If the amount of income breaches the basic rate threshold, then the amount over the
            // threshold needs to be excluded from the basic rate calculation
            if (grossAnnualIncome > assessedBasicRateThreshold) {
                int amountAboveBasicRate = grossAnnualIncome - assessedBasicRateThreshold;
                int amountEligibleForBasicRateTax = grossAnnualIncome - amountAboveBasicRate - personalAllowance;
                basicRateDeduction = (int)(TAX_BASIC_RATE * amountEligibleForBasicRateTax);
            } else {
                int amountEligibleForBasicRateTax = (grossAnnualIncome - personalAllowance);
                basicRateDeduction = (int)(TAX_BASIC_RATE * amountEligibleForBasicRateTax);
            }
        } else {
            basicRateDeduction = 0;
        }

        totalTaxDeductions = basicRateDeduction + higherRateDeduction + additionalRateDeduction;
        return totalTaxDeductions;
    }

    public int calculateNationalInsuranceContributions() {
        int contributionBetweenLelAndPt;
        int contributionBetweenPtAndUap;
        int contributionBetweenUapAndUel;
        int contributionAboveUel;
        nationalInsuranceContributions = 0;

        if (grossAnnualIncome < LEL_NI) {
            return nationalInsuranceContributions;
        }

        // Calculate earnings between LEL_NI and PT_NI and multiply by 0%
        if (grossAnnualIncome < PT_NI) {
            contributionBetweenLelAndPt = (grossAnnualIncome - LEL_NI) * 0;
            return nationalInsuranceContributions = contributionBetweenLelAndPt;
        } else {
            contributionBetweenLelAndPt = (PT_NI - LEL_NI)*0;
        }

        // Calculate earnings between PT_NI and UAP_NI and multiply by TOP_RATE_NI %
        if (grossAnnualIncome < UAP_NI) {
            contributionBetweenPtAndUap = (int) ((grossAnnualIncome - PT_NI) * TOP_RATE_NI);
            return nationalInsuranceContributions = contributionBetweenLelAndPt +
                                                    contributionBetweenPtAndUap;
        } else {
            contributionBetweenPtAndUap = (int) ((UAP_NI - PT_NI) * TOP_RATE_NI);
        }

        // Calculate earnings between UAP and UEL and multiply by TOP_RATE_NI %
        if (grossAnnualIncome <= UEL_NI) {
            contributionBetweenUapAndUel = (int) ((grossAnnualIncome - UAP_NI) * TOP_RATE_NI);
            return nationalInsuranceContributions = contributionBetweenLelAndPt +
                                                    contributionBetweenPtAndUap +
                                                    contributionBetweenUapAndUel;
        } else {
            contributionBetweenUapAndUel = (int) ((UEL_NI - UAP_NI) * TOP_RATE_NI);
        }

        // Calculate earning above UEL and multiply by LOWER_RATE_NI %
        contributionAboveUel = (int)((grossAnnualIncome - UEL_NI) * LOWER_RATE_NI);

        // Add all the values together and return
        return nationalInsuranceContributions = contributionBetweenLelAndPt +
                                                contributionBetweenPtAndUap +
                                                contributionBetweenUapAndUel +
                                                contributionAboveUel;
    }

    public int getTotalTaxDeductions() {
        return totalTaxDeductions;
    }

    public int getPersonalAllowance() {
        return personalAllowance;
    }

    public int getTotalDeductions() {
        return totalDeductions;
    }

    public int getNationalInsuranceContributions() {
        return nationalInsuranceContributions;
    }

    public void calculateNetAnnualIncome() {
        totalDeductions = calculateTotalDeductions();
        netAnnualIncome = grossAnnualIncome - totalDeductions;
    }

    public int getNetAnnualIncome() {
        return netAnnualIncome;
    }
}