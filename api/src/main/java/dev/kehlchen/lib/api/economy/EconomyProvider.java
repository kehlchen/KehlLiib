package dev.kehlchen.lib.api.economy;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.integration.Integration;

import java.math.BigDecimal;
import java.util.UUID;

public interface EconomyProvider extends Integration {
    String currencyNameSingular();
    String currencyNamePlural();
    String format(BigDecimal amount);
    Result<BigDecimal> balance(UUID account);
    EconomyTransaction deposit(UUID account, BigDecimal amount);
    EconomyTransaction withdraw(UUID account, BigDecimal amount);
    EconomyTransaction setBalance(UUID account, BigDecimal amount);
}
