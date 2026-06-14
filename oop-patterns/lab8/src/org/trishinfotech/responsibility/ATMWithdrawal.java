package org.trishinfotech.responsibility;

import java.util.Scanner;

public class ATMWithdrawal {

    private static final int DAILY_LIMIT = 1_000_000;
    private static final int UNIT = 1000;

    public static void main(String[] args) {
        PaperCurrencyDispenser fiftyThousand = new FiftyThousandDispenser();
        PaperCurrencyDispenser tenThousand = new TenThousandDispenser();
        PaperCurrencyDispenser fiveThousand = new FiveThousandDispenser();
        PaperCurrencyDispenser oneThousand = new OneThousandDispenser();

        fiftyThousand.setNextDispenser(tenThousand);
        tenThousand.setNextDispenser(fiveThousand);
        fiveThousand.setNextDispenser(oneThousand);

        Scanner scanner = new Scanner(System.in);
        int totalWithdrawn = 0;

        System.out.println("===== ATM Withdrawal =====");
        System.out.println("(Enter 0 or a negative number to quit.)");

        while (true) {
            System.out.printf("%nEnter amount to withdraw (KRW): ");

            if (!scanner.hasNextInt()) {
                System.out.println("Invalid amount. Try again!");
                scanner.next();
                continue;
            }

            int amount = scanner.nextInt();

            if (amount <= 0) {
                System.out.println("Goodbye!");
                break;
            }

            if (amount % UNIT != 0) {
                System.out.println("Invalid amount. Try again!");
                continue;
            }

            if (totalWithdrawn + amount > DAILY_LIMIT) {
                System.out.println("Daily withdrawal limit exceeded!");
                continue;
            }

            fiftyThousand.dispense(new PaperCurrency(amount));
            totalWithdrawn += amount;
            System.out.printf("Total withdrawn today: %d원 (remaining limit: %d원)%n",
                    totalWithdrawn, DAILY_LIMIT - totalWithdrawn);
        }

        scanner.close();
    }
}
