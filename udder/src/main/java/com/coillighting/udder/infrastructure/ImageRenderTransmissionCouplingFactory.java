package com.coillighting.udder.infrastructure;

public abstract class ImageRenderTransmissionCouplingFactory {

    public static Transmitter create(int[] deviceAddressMap) {
        return new ImageRenderTransmitter(null, deviceAddressMap.clone());
    }

}
