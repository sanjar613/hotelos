package com.hotelos.housekeeping.config;

/** Central registry of all RabbitMQ exchange/queue/routing-key names. */
public final class HotelOSEvents {
    private HotelOSEvents() {}
    public static final String EXCHANGE      = "hotelos.exchange";
    public static final String Q_VACATED     = "queue.room.vacated";
    public static final String Q_ROOM_STATUS = "queue.room.status.changed";
    public static final String Q_ORDER       = "queue.order.status.changed";
    public static final String Q_MAINT_NEW   = "queue.maintenance.created";
    public static final String Q_MAINT_DONE  = "queue.maintenance.resolved";
    public static final String RK_VACATED     = "room.vacated";
    public static final String RK_ROOM_STATUS = "room.status.changed";
    public static final String RK_ORDER       = "order.status.changed";
    public static final String RK_MAINT_NEW   = "maintenance.created";
    public static final String RK_MAINT_DONE  = "maintenance.resolved";
    public static final String RK_CHARGE      = "room.service.charge";
}
