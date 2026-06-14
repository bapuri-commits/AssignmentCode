package org.trishinfotech.responsibility;

public class FiftyThousandDispenser extends PaperCurrencyDispenser {

    public FiftyThousandDispenser() {
        super();
    }

    @Override
    public void dispense(PaperCurrency currency) {
        if (currency != null) {
            int amount = currency.getAmount();
            int remainder = amount;
            if (amount >= 50000) {
                int count = amount / 50000;
                remainder = amount % 50000;
                System.out.printf("Dispensing '%d' 50000원 currency note.\n", count);
            }
            if (remainder > 0 && this.nextDispenser != null) {
                this.nextDispenser.dispense(new PaperCurrency(remainder));
            }
        }
    }
}
