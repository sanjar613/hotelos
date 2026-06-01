package com.hotelos.housekeeping.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.*;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration @EnableWebSocketMessageBroker
public class HousekeepingConfig implements WebSocketMessageBrokerConfigurer {
    @Bean public TopicExchange hotelExchange() { return new TopicExchange(HotelOSEvents.EXCHANGE,true,false); }
    @Bean public Queue qVacated()    { return QueueBuilder.durable(HotelOSEvents.Q_VACATED).build(); }
    @Bean public Queue qRoomStatus() { return QueueBuilder.durable(HotelOSEvents.Q_ROOM_STATUS).build(); }
    @Bean public Binding bVacated(TopicExchange e)    { return BindingBuilder.bind(qVacated()).to(e).with(HotelOSEvents.RK_VACATED); }
    @Bean public Binding bRoomStatus(TopicExchange e) { return BindingBuilder.bind(qRoomStatus()).to(e).with(HotelOSEvents.RK_ROOM_STATUS); }
    @Bean public MessageConverter json() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbit(ConnectionFactory cf) { RabbitTemplate t=new RabbitTemplate(cf); t.setMessageConverter(json()); return t; }
    @Override public void configureMessageBroker(MessageBrokerRegistry r) { r.enableSimpleBroker("/topic"); r.setApplicationDestinationPrefixes("/app"); }
    @Override public void registerStompEndpoints(StompEndpointRegistry r) { r.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS(); }
}
