package dev.kehlchen.lib.api.economy;

import java.math.BigDecimal;
import java.util.Optional;

public record EconomyTransaction(boolean successful, BigDecimal amount,
                                 BigDecimal balanceAfter, String error) {
    public Optional<BigDecimal> resultingBalance() { return Optional.ofNullable(balanceAfter); }
    public Optional<String> failureReason() { return Optional.ofNullable(error); }
}
