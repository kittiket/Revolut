package ev.demo.revolut.model.transaction;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;

@RunWith(MockitoJUnitRunner.class)
public class ExchangeRateServiceTest {

    @InjectMocks
    ExchangeRateService exchangeRateService;

    @Test
    public void convert_NoConversionForTheSameCurrency() {
        BigDecimal amount = new BigDecimal(100);
        BigDecimal convertedAmount = exchangeRateService.convert(amount, "USD", "USD");
        assertEquals(amount, convertedAmount);
    }

    @Test
    public void convert_amountChanged() {
        BigDecimal amount = new BigDecimal(100);
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate("USD", "EUR");
        BigDecimal convertedAmount = exchangeRateService.convert(amount, "USD", "EUR");
        assertEquals(amount.multiply(exchangeRate), convertedAmount);
    }

    @Test
    public void convert_amountConvertedTwoTimesNotBiggerThanInitial() {
        BigDecimal amount = new BigDecimal(100);
        BigDecimal convertedAmount = exchangeRateService.convert(amount, "USD", "EUR");
        convertedAmount = exchangeRateService.convert(convertedAmount, "EUR", "USD");
        assertTrue(amount.compareTo(convertedAmount) >=0);
    }

    @Test
    public void getExchangeRate_1ForTheSameCurrency() {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate("USD", "USD");
        assertEquals(new BigDecimal(1), exchangeRate);
    }

    @Test
    public void getExchangeRate_Not1ForDifferentCurrencies() {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate("USD", "EUR");
        assertNotEquals(new BigDecimal(1), exchangeRate);
    }
}
