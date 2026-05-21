package com.example.floginsignup.api;

/**
 * Single point of access for the ParkingApi.
 * To switch to a real backend, call ApiClient.setApi(new YourRealApi()) once at app start.
 */
public final class ApiClient {
    private static volatile ParkingApi api = new MockParkingApi();

    private ApiClient() {}

    public static ParkingApi get() {
        return api;
    }

    public static void setApi(ParkingApi newApi) {
        api = newApi;
    }
}
