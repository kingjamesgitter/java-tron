package org.tron.core.capsule;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.DeferredStage;
import org.tron.protos.Protocol.DeferredTransaction;
import org.tron.protos.Protocol.Transaction;

@Slf4j(topic = "capsule")
public class DeferredTransactionCapsule implements ProtoCapsule<DeferredTransaction> {
    @Getter
    private DeferredTransaction deferredTransaction;

    @Override
    public byte[] getData() {
        return this.deferredTransaction.toByteArray();
    }

    public byte[] getKey() {
        int size = Long.SIZE/Byte.SIZE;
        long delayUntil = deferredTransaction.getDelayUntil();
        byte[] delayTime =  Longs.toByteArray(delayUntil);
        byte[] trxId = deferredTransaction.getTransactionId().toByteArray();
        byte[] key = new byte[size + trxId.length];
        System.arraycopy(delayTime, 0, key, 0, size);
        System.arraycopy(trxId, 0, key, size, trxId.length);
        return key;
    }

    @Override
    public DeferredTransaction getInstance() {
        return deferredTransaction;
    }

    public DeferredTransactionCapsule(DeferredTransaction deferredTransaction){
        this.deferredTransaction = deferredTransaction;
    }

    public DeferredTransactionCapsule(byte[] data){
        try {
            this.deferredTransaction = DeferredTransaction.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public ByteString getTransactionId(){
        return deferredTransaction.getTransactionId();
    }

    public long getPublishTime(){
        return deferredTransaction.getPublishTime();
    }

    public long getDelayUntil(){
        return deferredTransaction.getDelayUntil();
    }

    public long getExpiration(){
        return deferredTransaction.getExpiration();
    }


    public ByteString getSenderAddress(){
        return deferredTransaction.getSenderAddress();
    }

    public ByteString getReceiverAddress(){
        return deferredTransaction.getReceiverAddress();
    }

    public void setDelaySecond(long delaySecond) {
        if (Objects.isNull(deferredTransaction) || Objects.isNull(deferredTransaction.getTransaction()) ) {
            logger.info("updateDeferredTransaction failed, transaction is null");
            return;
        }

        long delayUntil = deferredTransaction.getPublishTime() + delaySecond * 1000;
        long expiration = delayUntil + Args.getInstance().getTrxExpirationTimeInMilliseconds();
        Transaction transaction = deferredTransaction.getTransaction();
        DeferredStage deferredStage = transaction.getRawData().toBuilder().
            getDeferredStage().toBuilder().setDelaySeconds(delaySecond).build();
        Transaction.raw rawData = transaction.toBuilder().getRawData().toBuilder().setDeferredStage(deferredStage).build();
        transaction = transaction.toBuilder().setRawData(rawData).build();
        deferredTransaction = deferredTransaction.toBuilder().setDelayUntil(delayUntil).
            setDelaySeconds(delaySecond).setExpiration(expiration).setTransaction(transaction).build();
    }

}
