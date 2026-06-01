package com.hotelos.maintenance.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.*;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration @EnableWebSocketMessageBroker
public class MaintConfig implements WebSocketMessageBrokerConfigurer {
    @Bean public TopicExchange hotelExchange() { return new TopicExchange(HotelOSEvents.EXCHANGE,true,false); }
    @Bean public Queue qNew()  { return QueueBuilder.durable(HotelOSEvents.Q_MAINT_NEW).build(); }
    @Bean public Queue qDone() { return QueueBuilder.durable(HotelOSEvents.Q_MAINT_DONE).build(); }
    @Bean public Binding bNew(TopicExchange e)  { return BindingBuilder.bind(qNew()).to(e).with(HotelOSEvents.RK_MAINT_NEW); }
    @Bean public Binding bDone(TopicExchange e) { return BindingBuilder.bind(qDone()).to(e).with(HotelOSEvents.RK_MAINT_DONE); }
    @Bean public MessageConverter json() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbit(ConnectionFactory cf) { RabbitTemplate t=new RabbitTemplate(cf); t.setMessageConverter(json()); return t; }
    @Override public void configureMessageBroker(MessageBrokerRegistry r) { r.enableSimpleBroker("/topic"); r.setApplicationDestinationPrefixes("/app"); }
    @Override public void registerStompEndpoints(StompEndpointRegistry r) { r.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS(); }
}
