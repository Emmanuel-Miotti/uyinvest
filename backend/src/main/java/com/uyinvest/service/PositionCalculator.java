package com.uyinvest.service;

import com.uyinvest.entity.Transaction;
import com.uyinvest.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PositionCalculator {

    private static final int QUANTITY_SCALE = 8;
    private static final int PRICE_SCALE = 8;
    private static final int MONEY_SCALE = 4;
    private static final int PERCENTAGE_SCALE = 2;

    public Position calculate(List<Transaction> transactions, BigDecimal currentPrice) {
        Holding holding = accumulate(transactions);
        BigDecimal quantity = holding.quantity();
        BigDecimal costBasis = holding.costBasis();

        BigDecimal averagePrice = quantity.signum() > 0
                ? costBasis.divide(quantity, PRICE_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal currentValue = quantity.multiply(currentPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal profitLoss = currentValue.subtract(costBasis);
        BigDecimal profitLossPercentage = costBasis.signum() > 0
                ? profitLoss.divide(costBasis, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new Position(quantity, costBasis, averagePrice, currentPrice, currentValue, profitLoss, profitLossPercentage);
    }

    public BigDecimal calculateCostBasis(List<Transaction> transactions) {
        return accumulate(transactions).costBasis();
    }

    private Holding accumulate(List<Transaction> transactions) {
        List<Transaction> sorted = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate).thenComparing(Transaction::getCreatedAt))
                .toList();

        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal costBasis = BigDecimal.ZERO;

        for (Transaction tx : sorted) {
            if (tx.getType() == TransactionType.BUY) {
                BigDecimal buyCost = tx.getQuantity().multiply(tx.getPrice()).add(tx.getCommission());
                quantity = quantity.add(tx.getQuantity());
                costBasis = costBasis.add(buyCost);
            } else {
                if (quantity.signum() > 0) {
                    BigDecimal averagePriceBeforeSale = costBasis.divide(quantity, PRICE_SCALE, RoundingMode.HALF_UP);
                    costBasis = costBasis.subtract(averagePriceBeforeSale.multiply(tx.getQuantity()));
                }
                quantity = quantity.subtract(tx.getQuantity());
                if (quantity.signum() <= 0) {
                    quantity = BigDecimal.ZERO;
                    costBasis = BigDecimal.ZERO;
                }
            }
        }

        return new Holding(quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP), costBasis.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private record Holding(BigDecimal quantity, BigDecimal costBasis) {
    }
}
