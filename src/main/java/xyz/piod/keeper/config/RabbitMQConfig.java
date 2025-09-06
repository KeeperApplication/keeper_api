package xyz.piod.keeper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String KEEPER_EXCHANGE = "keeper.exchange";
    public static final String MESSAGE_COMMANDS_QUEUE = "keeper.message.commands";
    public static final String MESSAGE_COMMAND_ROUTING_KEY = "cmd.v2.message.#";

    @Bean
    public TopicExchange keeperExchange() {
        return new TopicExchange(KEEPER_EXCHANGE);
    }

    @Bean
    public Queue messageCommandsQueue() {
        return new Queue(MESSAGE_COMMANDS_QUEUE, true);
    }

    @Bean
    public Binding messageCommandsBinding(Queue messageCommandsQueue, TopicExchange keeperExchange) {
        return BindingBuilder.bind(messageCommandsQueue).to(keeperExchange).with(MESSAGE_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}