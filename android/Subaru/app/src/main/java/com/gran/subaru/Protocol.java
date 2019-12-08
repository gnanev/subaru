package com.gran.subaru;

import android.util.Log;

import com.google.common.collect.EvictingQueue;

public class Protocol implements ICommReceiver{

    public static final int DATA_MAX_LEN = 64;
    public static final int START_BYTE = 0xAA;

    public static final int MAX_FRAMES = 2000;

    public static final int ADDR_HEAD_UNIT = 1;
    public static final int ADDR_CTRL = 2;

    private IComm mComm;
    private int mReceiveStep = 0;
    private int mDataReceived = 0;

    private DataFrame mCurrentFrame;

    private EvictingQueue<DataFrame> mQueue;

    private IDataReceiver mDataReceiver;

    public class DataFrame {
        public int    addressFrom;
        public int    addressTo;
        public int    cmd;
        public int    checkSum;
        public int[]  data;
    }

    public Protocol(IComm comm, IDataReceiver dataReceiver) {
        mComm = comm;
        comm.setReceiver(this);
        mDataReceiver = dataReceiver;
        mQueue = EvictingQueue.create(MAX_FRAMES);
    }

    public DataFrame nextFrame() {
        if (mQueue.isEmpty()) {
            return null;
        }

        return mQueue.poll();
    }

    private void startFrame() {
        mCurrentFrame = new DataFrame();
    }

    private void finishFrame() {
        int checkSum = calcChecksum(mCurrentFrame);

        if (checkSum == mCurrentFrame.checkSum) {
            mQueue.add(mCurrentFrame);
            dropFrame();
            mDataReceiver.onNewData();
        }
        else {
            dropFrame();
        }
    }

    private void dropFrame() {
        mCurrentFrame = null;
        mReceiveStep = 0;
        mDataReceived = 0;
    }

    private int calcChecksum(DataFrame df) {
        return calcChecksum(df.addressFrom, df.addressTo, df.cmd, df.data);
    }

    private int calcChecksum(int addressFrom, int addressTo, int cmd, int[] data) {
        int sum = addressFrom + addressTo + cmd + data.length;
        for (int i=0; i<data.length; i++) {
            sum += data[i];
        }

        return sum % 256;
    }

    private void receiveByte(byte bb) {

        int b = bb & 0xFF; // fucking java

        //Log.d("DATA", ""+b);

        switch (mReceiveStep) {
            case 0: // data frame start
                if (b != START_BYTE)
                    return;

                startFrame();
                mReceiveStep++;
                break;

            case 1: // data source address
                mCurrentFrame.addressFrom = b;
                mReceiveStep++;
                break;

            case 2: // data destination address
                if (b != ADDR_HEAD_UNIT) {
                    // it's not for me, drop
                    dropFrame();
                    return;
                }
                mCurrentFrame.addressTo = b;
                mReceiveStep++;
                break;

            case 3: // command
                mCurrentFrame.cmd = b;
                mReceiveStep++;
                break;

            case 4: // data length
                if (b > DATA_MAX_LEN) {
                    // too large, drop
                    dropFrame();
                    return;
                }

                mCurrentFrame.data = new int[b];
                mReceiveStep++;
                break;

            case 5: // checksum
                mCurrentFrame.checkSum = b;
                if (mCurrentFrame.data == null) {
                    // if no data, this is last byte
                    finishFrame();
                    return;
                }

                mReceiveStep++;
                break;

            case 6:
                mCurrentFrame.data[mDataReceived++] = b;
                if (mDataReceived == mCurrentFrame.data.length) {
                    finishFrame();
                }
                break;
        }
    }

    public void sendData(int addressTo, int cmd, int[] data) {
        sendData(ADDR_HEAD_UNIT, addressTo, cmd, data);
    }

    public void sendData(int addressFrom, int addressTo, int cmd, int[] data) {
        if (!canSendData())
            return;

        int checkSum = calcChecksum(addressFrom, addressTo, cmd, data);
        
        byte[] buff = new byte[6+data.length];
        buff[0] = (byte)START_BYTE;
        buff[1] = (byte)addressFrom;
        buff[2] = (byte)addressTo;
        buff[3] = (byte)cmd;
        buff[4] = (byte)data.length;
        buff[5] = (byte)checkSum;
        for (int i=0; i<data.length; i++) {
            buff[6+i] = (byte)data[i];
        }

        mComm.sendData(buff);
    }

    public boolean canSendData() {
        return mComm.canSendData();
    }

    @Override
    public void onReceive(byte[] buff) {
        for(int i=0; i<buff.length; i++) {
            receiveByte(buff[i]);
        }
    }

    @Override
    public void onDeviceReady() {
        mDataReceiver.onDeviceReady();
    }

    @Override
    public void onDeviceNotFound() {

    }
}
