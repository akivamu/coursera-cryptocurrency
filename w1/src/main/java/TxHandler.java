import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TxHandler {
    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // (1) all outputs claimed by {@code tx} are in the current UTXO pool
        // The desc should be rewritten as: All TX's inputs should only consume the UTXO currently in pool
        for (Transaction.Input input : tx.getInputs()) {
            if (!utxoPool.contains(new UTXO(input.prevTxHash, input.outputIndex))) {
                return false;
            }
        }

        // (2) the signatures on each input of {@code tx} are valid
        // Here we validate all Inputs, by verify the signature of that Input in prev transaction
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);

            // For each Input, find the corresponding Output in prev transaction
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = utxoPool.getTxOutput(utxo);

            // Check valid
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }
        }

        // (3) no UTXO is claimed multiple times by {@code tx}
        // Iterate through all Inputs, claim UTXO and remember. Return false if same UTXO is claimed again.
        List<UTXO> claimedUTXOs = new ArrayList<>();
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (claimedUTXOs.contains(utxo)) {
                return false;
            } else {
                claimedUTXOs.add(utxo);
            }
        }

        // (4) all of {@code tx}s output values are non-negative
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) return false;
        }

        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
        double sumInputs = 0;
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);

            // For each Input, find the corresponding Output in prev transaction
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = utxoPool.getTxOutput(utxo);

            sumInputs += output.value;
        }
        double sumOutputs = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            sumOutputs += output.value;
        }

        return (sumInputs >= sumOutputs);
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Transaction[] result = new Transaction[possibleTxs.length];

        int count = 0;
        for (Transaction transaction : possibleTxs) {
            if (isValidTx(transaction)) {
                result[count] = transaction;
                count++;
            }
        }

        return Arrays.copyOf(result, count);
    }

}
