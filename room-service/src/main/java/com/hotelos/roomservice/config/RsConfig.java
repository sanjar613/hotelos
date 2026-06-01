package com.hotelos.roomservice.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.*;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration @EnableWebSocketMessageBroker
public class RsConfig implements WebSocketMessageBrokerConfigurer {
    @Bean public TopicExchange hotelExchange() { return new TopicExchange(HotelOSEvents.EXCHANGE,true,false); }
    @Bean public Queue qOrder()  { return QueueBuilder.durable(HotelOSEvents.Q_ORDER).build(); }
    @Bean public Queue qCharge() { return QueueBuilder.durable("queue.room.service.charge").build(); }
    @Bean public Binding bOrder(TopicExchange e)  { return BindingBuilder.bind(qOrder()).to(e).with(HotelOSEvents.RK_ORDER); }
    @Bean public Binding bCharge(TopicExchange e) { return BindingBuilder.bind(qCharge()).to(e).with(HotelOSEvents.RK_CHARGE); }
    @Bean public MessageConverter json() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbit(ConnectionFactory cf) { RabbitTemplate t=new RabbitTemplate(cf); t.setMessageConverter(json()); return t; }
    @Override public void configureMessageBroker(MessageBrokerRegistry r) { r.enableSimpleBroker("/topic"); r.setApplicationDestinationPrefixes("/app"); }
    @Override public void registerStompEndpoints(StompEndpointRegistry r) { r.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS(); }
}
