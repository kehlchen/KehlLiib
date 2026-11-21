package dev.kehlchen.lib.integration.vault;

import dev.kehlchen.lib.api.Result;
import dev.kehlchen.lib.api.ServiceRegistry;
import dev.kehlchen.lib.api.economy.EconomyProvider;
import dev.kehlchen.lib.api.economy.EconomyTransaction;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.UUID;

/** Bridges Vault's optional Economy service to KehlLib's Vault-independent API. */
public final class VaultEconomyIntegration implements EconomyProvider {
    private final JavaPlugin plugin;
    private final ServiceRegistry services;
    private volatile Economy economy;

    public VaultEconomyIntegration(JavaPlugin plugin, ServiceRegistry services) {
        this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
        this.services = java.util.Objects.requireNonNull(services, "services");
    }

    @Override public String name() { return "vault-economy"; }
    @Override public String version() { return "1.0"; }

    @Override public void onEnable() {
        requireServerThread();
        RegisteredServiceProvider<Economy> registration =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) throw new IllegalStateException("Vault has no registered economy provider");
        economy = registration.getProvider();
        if (services.get(EconomyProvider.class).isPresent()) {
            throw new IllegalStateException("An economy provider is already registered in KehlLib");
        }
        services.register(EconomyProvider.class, this);
    }

    @Override public void onDisable() {
        requireServerThread();
        if (services.get(EconomyProvider.class).orElse(null) == this) {
            services.unregister(EconomyProvider.class);
        }
        economy = null;
    }

    @Override public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Vault")
                && Bukkit.getServicesManager().getRegistration(Economy.class) != null;
    }

    @Override public String currencyNameSingular() {
        requireServerThread();
        return requireEconomy().currencyNameSingular();
    }
    @Override public String currencyNamePlural() {
        requireServerThread();
        return requireEconomy().currencyNamePlural();
    }
    @Override public String format(BigDecimal amount) {
        java.util.Objects.requireNonNull(amount, "amount");
        requireServerThread();
        if (!validAmount(amount)) throw new IllegalArgumentException("Amount must be finite and non-negative");
        return requireEconomy().format(amount.doubleValue());
    }

    @Override public Result<BigDecimal> balance(UUID account) {
        if (!Bukkit.isPrimaryThread()) return Result.failure(new IllegalStateException("Vault economy calls must run on the server thread"));
        try {
            return Result.success(BigDecimal.valueOf(requireEconomy().getBalance(player(account))));
        } catch (RuntimeException failure) {
            return Result.failure(failure);
        }
    }

    @Override public EconomyTransaction deposit(UUID account, BigDecimal amount) {
        return transact(account, amount, Transaction.DEPOSIT);
    }

    @Override public EconomyTransaction withdraw(UUID account, BigDecimal amount) {
        return transact(account, amount, Transaction.WITHDRAW);
    }

    @Override public EconomyTransaction setBalance(UUID account, BigDecimal amount) {
        if (!validAmount(amount) || !Bukkit.isPrimaryThread()) {
            return failed(amount, "Amount must be finite and non-negative and the operation must run on the server thread");
        }
        try {
            Economy provider = requireEconomy();
            OfflinePlayer player = player(account);
            BigDecimal difference = amount.subtract(BigDecimal.valueOf(provider.getBalance(player)));
            if (difference.signum() == 0) return success(amount, amount);
            return apply(provider, player, difference.abs(), difference.signum() > 0);
        } catch (RuntimeException failure) {
            return failed(amount, "Economy provider operation failed (" + failure.getClass().getSimpleName() + ")");
        }
    }

    private EconomyTransaction transact(UUID account, BigDecimal amount, Transaction transaction) {
        if (!validAmount(amount) || amount.signum() == 0 || !Bukkit.isPrimaryThread()) {
            return failed(amount, "Amount must be positive and the operation must run on the server thread");
        }
        try {
            return apply(requireEconomy(), player(account), amount, transaction == Transaction.DEPOSIT);
        } catch (RuntimeException failure) {
            return failed(amount, "Economy provider operation failed (" + failure.getClass().getSimpleName() + ")");
        }
    }

    private EconomyTransaction apply(Economy provider, OfflinePlayer player, BigDecimal amount, boolean deposit) {
        double value = amount.doubleValue();
        if (!Double.isFinite(value)) return failed(amount, "Amount exceeds Vault's supported range");
        EconomyResponse response = deposit ? provider.depositPlayer(player, value) : provider.withdrawPlayer(player, value);
        if (!response.transactionSuccess()) return failed(amount, response.errorMessage);
        return success(amount, BigDecimal.valueOf(response.balance));
    }

    private OfflinePlayer player(UUID account) {
        return Bukkit.getOfflinePlayer(java.util.Objects.requireNonNull(account, "account"));
    }

    private Economy requireEconomy() {
        Economy current = economy;
        if (current == null) throw new IllegalStateException("Vault economy provider is unavailable");
        return current;
    }

    private static boolean validAmount(BigDecimal amount) {
        return amount != null && amount.signum() >= 0 && Double.isFinite(amount.doubleValue());
    }

    private void requireServerThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Vault integration lifecycle requires the server thread");
    }

    private static EconomyTransaction success(BigDecimal amount, BigDecimal balance) {
        return new EconomyTransaction(true, amount, balance, null);
    }

    private static EconomyTransaction failed(BigDecimal amount, String reason) {
        return new EconomyTransaction(false, amount, null, reason);
    }

    private enum Transaction { DEPOSIT, WITHDRAW }
}
