package com.hotelos.reception.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topic Exchange architecture:
 *   Publisher → exchange (with routing key) → queue → subscriber
 *   Services NEVER call each other directly.
 */
@Configuration
public class RabbitMQConfig {
    @Bean public TopicExchange hotelExchange() { return new TopicExchange(HotelOSEvents.EXCHANGE, true, false); }
    @Bean public Queue qVacated()     { return QueueBuilder.durable(HotelOSEvents.Q_VACATED).build(); }
    @Bean public Queue qRoomStatus()  { return QueueBuilder.durable(HotelOSEvents.Q_ROOM_STATUS).build(); }
    @Bean public Queue qOrder()       { return QueueBuilder.durable(HotelOSEvents.Q_ORDER).build(); }
    @Bean public Queue qMaintNew()    { return QueueBuilder.durable(HotelOSEvents.Q_MAINT_NEW).build(); }
    @Bean public Queue qMaintDone()   { return QueueBuilder.durable(HotelOSEvents.Q_MAINT_DONE).build(); }
    @Bean public Queue qCharge()      { return QueueBuilder.durable("queue.room.service.charge").build(); }

    @Bean public Binding bVacated(TopicExchange e)    { return BindingBuilder.bind(qVacated()).to(e).with(HotelOSEvents.RK_VACATED); }
    @Bean public Binding bRoomStatus(TopicExchange e) { return BindingBuilder.bind(qRoomStatus()).to(e).with(HotelOSEvents.RK_ROOM_STATUS); }
    @Bean public Binding bOrder(TopicExchange e)      { return BindingBuilder.bind(qOrder()).to(e).with(HotelOSEvents.RK_ORDER); }
    @Bean public Binding bMaintNew(TopicExchange e)   { return BindingBuilder.bind(qMaintNew()).to(e).with(HotelOSEvents.RK_MAINT_NEW); }
    @Bean public Binding bMaintDone(TopicExchange e)  { return BindingBuilder.bind(qMaintDone()).to(e).with(HotelOSEvents.RK_MAINT_DONE); }
    @Bean public Binding bCharge(TopicExchange e)     { return BindingBuilder.bind(qCharge()).to(e).with(HotelOSEvents.RK_CHARGE); }

    @Bean public MessageConverter jsonConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf); t.setMessageConverter(jsonConverter()); return t;
    }
}
