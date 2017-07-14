package com.example.reactivesse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.file.dsl.Files;
import org.springframework.messaging.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.File;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "http://www.martinetherton.com", maxAge = 3600)
public class ReactiveSseApplication {

	@Bean
	SubscribableChannel filesChannel() {
		return MessageChannels.publishSubscribe().get();
	}

	@Bean
	IntegrationFlow integrationFlow(@Value("${input-dir:file://${HOME}/Desktop/in}") File in) {
		return IntegrationFlows.from(Files.inboundAdapter(in).autoCreateDirectory(true),
				poller->poller.poller(spec->spec.fixedRate(1000L)))
				.transform(File.class, File::getAbsolutePath)
				.channel(filesChannel())
				.get();
	}

	@GetMapping (value = "/files/{name}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	Flux<String> files(@PathVariable String name) {
		return Flux.create(sink -> {
			MessageHandler handler = msg -> sink.next((String) msg.getPayload());
            sink.onDispose(()->filesChannel().unsubscribe(handler));
			filesChannel().subscribe(handler);
		});
	}

	public static void main(String[] args) {
		SpringApplication.run(ReactiveSseApplication.class, args);
	}
}
