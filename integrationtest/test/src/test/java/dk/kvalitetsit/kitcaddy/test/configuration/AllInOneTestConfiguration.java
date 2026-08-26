package dk.kvalitetsit.kitcaddy.test.configuration;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dk.kvalitetsit.kitcaddy.AbstractIntegrationTest;
import dk.kvalitetsit.kitcaddy.TestConstants;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
public class AllInOneTestConfiguration extends AbstractMongoClientConfiguration {

	@Bean
	public MongoClient mongoClient() {
		return MongoClients.create("mongodb://" + AbstractIntegrationTest.mongoHost + ":" + AbstractIntegrationTest.mongoPort);
	}

	@Override
	protected String getDatabaseName() {
		return "default";
	}
	@Bean
	public MongoTemplate spMongoTemplate(MappingMongoConverter converter) {
		return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient(), TestConstants.SP_MONGO_DATABASE), converter);
	}

	@Bean
	@Primary
	public MongoTemplate wscMongoTemplate(MappingMongoConverter converter) {

		return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient(), TestConstants.WSC_MONGO_DATABASE), converter);
	}

	@Bean
	public MongoTemplate wspMongoTemplate(MappingMongoConverter converter) {
		return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient(), TestConstants.WSP_MONGO_DATABASE), converter);
	}
}