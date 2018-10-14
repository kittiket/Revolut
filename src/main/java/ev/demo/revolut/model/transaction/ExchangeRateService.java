package ev.demo.revolut.model.transaction;

import java.math.BigDecimal;

public class ExchangeRateService {

    public BigDecimal convert(BigDecimal amount, String currencyFrom, String currencyTo) {
        return amount.multiply(getExchangeRate(currencyFrom, currencyTo));
    }

    BigDecimal getExchangeRate(String currencyFrom, String currencyTo) {
        if (currencyFrom.equals(currencyTo)) {
            return new BigDecimal(1.0);
        } else {
            return new BigDecimal(currencyFrom.compareTo(currencyTo) > 0 ? 2 : 0.5); //call exchange rate service here
        }
    }
}
