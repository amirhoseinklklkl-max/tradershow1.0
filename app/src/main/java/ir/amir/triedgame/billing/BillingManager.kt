package ir.amir.triedgame.billing

import android.app.Activity
import android.content.Intent
import android.util.Log
import ir.amir.triedgame.BuildConfig
import ir.myket.billingclient.IabHelper
import ir.myket.billingclient.util.IabResult
import ir.myket.billingclient.util.Inventory
import ir.myket.billingclient.util.Purchase

/**
 * Wraps Myket's IabHelper for the three consumable wallet top-up packages.
 * All three are consumable: the user can buy any of them repeatedly, each
 * purchase is consumed immediately after being verified/credited so it
 * becomes available for purchase again right away.
 */
class BillingManager(
    private val activity: Activity,
    private val onWalletTopUp: (usdAmount: Double) -> Unit,
    private val onSetupError: (String) -> Unit = {}
) {
    private var helper: IabHelper? = null

    companion object {
        private const val TAG = "BillingManager"

        // Product IDs as configured in the Myket developer panel.
        val SKUS = listOf("25dolar", "65dolar", "200dolar", "1000dolar")

        /** USD credited to the wallet for each SKU, per the Myket product setup. */
        val USD_REWARD: Map<String, Double> = mapOf(
            "25dolar" to 25.0,
            "65dolar" to 65.0,
            "200dolar" to 200.0,
            "1000dolar" to 1000.0
        )

        /** Toman price shown in the shop UI for each SKU (informational; the real
         * charge is handled entirely by Myket's own payment UI). */
        val TOMAN_PRICE: Map<String, Long> = mapOf(
            "25dolar" to 5_000L,
            "65dolar" to 10_000L,
            "200dolar" to 20_000L,
            "1000dolar" to 80_000L
        )

        /** SKUs to badge as a special/featured deal in the shop UI. */
        val SPECIAL_OFFER_SKUS = setOf("1000dolar")
    }

    fun start() {
        helper = IabHelper(activity, BuildConfig.MYKET_PUBLIC_KEY)
        helper?.enableDebugLogging(BuildConfig.DEBUG)
        helper?.startSetup { result ->
            if (!result.isSuccess) {
                onSetupError(result.message ?: "billing setup failed")
                return@startSetup
            }
            queryInventoryAndConsumePending()
        }
    }

    /**
     * On every app start we must query inventory: this both refreshes SKU
     * details and, importantly, catches any purchase that completed but
     * wasn't consumed/credited last time (e.g. app was killed mid-flow) so
     * the user is never charged without receiving their wallet top-up.
     */
    private fun queryInventoryAndConsumePending() {
        val h = helper ?: return
        try {
            h.queryInventoryAsync(false, SKUS) { result: IabResult, inventory: Inventory? ->
                if (result.isFailure || inventory == null) {
                    Log.e(TAG, "queryInventory failed: ${result.message}")
                    return@queryInventoryAsync
                }
                SKUS.forEach { sku ->
                    val purchase = inventory.getPurchase(sku)
                    if (purchase != null) {
                        consumeAndCredit(purchase)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "queryInventory error", e)
        }
    }

    fun purchase(sku: String) {
        val h = helper ?: return
        try {
            h.launchPurchaseFlow(activity, sku, { result: IabResult, purchase: Purchase? ->
                if (result.isFailure || purchase == null) {
                    Log.e(TAG, "purchase failed: ${result.message}")
                    return@launchPurchaseFlow
                }
                consumeAndCredit(purchase)
            }, sku /* developerPayload: a per-purchase token would be stronger, sku is a simple baseline */)
        } catch (e: Exception) {
            Log.e(TAG, "launchPurchaseFlow error", e)
        }
    }

    private fun consumeAndCredit(purchase: Purchase) {
        val h = helper ?: return
        try {
            h.consumeAsync(purchase) { consumedPurchase: Purchase, result: IabResult ->
                if (result.isSuccess) {
                    val usd = USD_REWARD[consumedPurchase.sku] ?: 0.0
                    if (usd > 0.0) onWalletTopUp(usd)
                } else {
                    Log.e(TAG, "consume failed: ${result.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "consumeAsync error", e)
        }
    }

    fun dispose() {
        helper?.dispose()
        helper = null
    }
}
