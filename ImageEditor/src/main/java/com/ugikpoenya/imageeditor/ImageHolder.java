package com.ugikpoenya.imageeditor;

public enum ImageHolder {
    INSTANCE;
    byte[] bmpObj;

    public static boolean hasData() {
        return INSTANCE.bmpObj != null;
    }

    public static void setData(byte[] bArr) {
        ImageHolder dataHolder = INSTANCE;
        dataHolder.bmpObj = bArr;
    }

    public static byte[] getData() {
        return INSTANCE.bmpObj;
    }
}
