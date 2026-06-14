package org.trishinfotech.responsibility;

public class TenThousandDispenser extends PaperCurrencyDispenser {

    public TenThousandDispenser() {
        super();
    }

    @Override
    public void dispense(PaperCurrency currency) {
        if (currency != null) {
            int amount = currency.getAmount();
            int remainder = amount;
            if (amount >= 10000) {
                int count = amount / 10000;
                remainder = amount % 10000;
                System.out.printf("Dispensing '%d' 10000원 currency note.\n", count);
            }
            if (remainder > 0 && this.nextDispenser != null) {
                this.nextDispenser.dispense(new PaperCurrency(remainder));
            }
        }
    }
}
