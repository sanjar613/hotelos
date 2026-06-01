package com.hotelos.maintenance.config;
public final class HotelOSEvents {
    private HotelOSEvents() {}
    public static final String EXCHANGE     = "hotelos.exchange";
    public static final String Q_MAINT_NEW  = "queue.maintenance.created";
    public static final String Q_MAINT_DONE = "queue.maintenance.resolved";
    public static final String RK_MAINT_NEW  = "maintenance.created";
    public static final String RK_MAINT_DONE = "maintenance.resolved";
}
