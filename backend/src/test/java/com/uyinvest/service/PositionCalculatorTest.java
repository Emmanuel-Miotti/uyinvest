package com.uyinvest.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.uyinvest.entity.Transaction;
import com.uyinvest.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class PositionCalculatorTest {

    private final PositionCalculator calculator = new PositionCalculator();

    private Transaction tx(TransactionType type, String quantity, String price, String commission, Instant date) {
        return Transaction.builder()
                .type(type)
                .quantity(new BigDecimal(quantity))
                .price(new BigDecimal(price))
                .commission(new BigDecimal(commission))
                .currency("USD")
                .transactionDate(date)
                .createdAt(date)
                .build();
    }

    @Test
    void matchesSpecExampleAcrossTwoPurchases() {
        Instant day1 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(1, ChronoUnit.DAYS);

        List<Transaction> transactions = List.of(
                tx(TransactionType.BUY, "10", "100", "0", day1),
                tx(TransactionType.BUY, "10", "120", "0", day2));

        Position position = calculator.calculate(transactions, new BigDecimal("130"));

        assertThat(position.quantity()).isEqualByComparingTo("20");
        assertThat(position.costBasis()).isEqualByComparingTo("2200");
        assertThat(position.averagePrice()).isEqualByComparingTo("110");
        assertThat(position.currentValue()).isEqualByComparingTo("2600");
        assertThat(position.profitLoss()).isEqualByComparingTo("400");
        assertThat(position.profitLossPercentage()).isEqualByComparingTo("18.18");
    }

    @Test
    void partialSellKeepsAveragePriceConstant() {
        Instant day1 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(1, ChronoUnit.DAYS);

        List<Transaction> transactions = List.of(
                tx(TransactionType.BUY, "10", "100", "0", day1),
                tx(TransactionType.SELL, "4", "150", "0", day2));

        Position position = calculator.calculate(transactions, new BigDecimal("100"));

        assertThat(position.quantity()).isEqualByComparingTo("6");
        assertThat(position.averagePrice()).isEqualByComparingTo("100");
        assertThat(position.costBasis()).isEqualByComparingTo("600");
    }

    @Test
    void fullySoldPositionZeroesOutCostBasisAndAveragePrice() {
        Instant day1 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(1, ChronoUnit.DAYS);

        List<Transaction> transactions = List.of(
                tx(TransactionType.BUY, "10", "100", "0", day1),
                tx(TransactionType.SELL, "10", "150", "0", day2));

        Position position = calculator.calculate(transactions, new BigDecimal("100"));

        assertThat(position.quantity()).isEqualByComparingTo("0");
        assertThat(position.costBasis()).isEqualByComparingTo("0");
        assertThat(position.averagePrice()).isEqualByComparingTo("0");
        assertThat(position.currentValue()).isEqualByComparingTo("0");
        assertThat(position.profitLossPercentage()).isEqualByComparingTo("0");
    }

    @Test
    void commissionIsIncludedInCostBasis() {
        Instant day1 = Instant.now();
        List<Transaction> transactions = List.of(tx(TransactionType.BUY, "10", "100", "5", day1));

        Position position = calculator.calculate(transactions, new BigDecimal("100"));

        assertThat(position.costBasis()).isEqualByComparingTo("1005");
        assertThat(position.averagePrice()).isEqualByComparingTo("100.50000000");
    }

    @Test
    void emptyTransactionListProducesEmptyPositionWithoutError() {
        Position position = calculator.calculate(List.of(), new BigDecimal("100"));

        assertThat(position.quantity()).isEqualByComparingTo("0");
        assertThat(position.costBasis()).isEqualByComparingTo("0");
        assertThat(position.currentValue()).isEqualByComparingTo("0");
        assertThat(position.profitLoss()).isEqualByComparingTo("0");
        assertThat(position.profitLossPercentage()).isEqualByComparingTo("0");
    }

    @Test
    void detectsLossWhenCurrentPriceIsBelowAveragePrice() {
        Instant day1 = Instant.now();
        List<Transaction> transactions = List.of(tx(TransactionType.BUY, "10", "100", "0", day1));

        Position position = calculator.calculate(transactions, new BigDecimal("80"));

        assertThat(position.profitLoss()).isEqualByComparingTo("-200");
        assertThat(position.profitLossPercentage()).isEqualByComparingTo("-20.00");
    }

    @Test
    void processesTransactionsInChronologicalOrderRegardlessOfInputOrder() {
        Instant day1 = Instant.now().minus(3, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant day3 = Instant.now().minus(1, ChronoUnit.DAYS);

        List<Transaction> inOrder = List.of(
                tx(TransactionType.BUY, "10", "100", "0", day1),
                tx(TransactionType.SELL, "4", "150", "0", day2),
                tx(TransactionType.BUY, "5", "120", "0", day3));

        List<Transaction> shuffled = List.of(inOrder.get(2), inOrder.get(0), inOrder.get(1));

        Position expected = calculator.calculate(inOrder, new BigDecimal("100"));
        Position actual = calculator.calculate(shuffled, new BigDecimal("100"));

        assertThat(actual.quantity()).isEqualByComparingTo(expected.quantity());
        assertThat(actual.costBasis()).isEqualByComparingTo(expected.costBasis());
        assertThat(actual.averagePrice()).isEqualByComparingTo(expected.averagePrice());
    }
}
