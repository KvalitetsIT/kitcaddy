package dk.kvalitetsit.kitcaddy.test.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dk.kvalitetsit.kitcaddy.AbstractIntegrationTest;
import dk.kvalitetsit.kitcaddy.TestConstants;

@Configuration
public class AllInOneTestConfiguration extends AbstractMongoClientConfiguration {

	@Override
	public MongoClient mongoClient() {
		return MongoClients.create("mongodb://"+AbstractIntegrationTest.mongoHost+":"+AbstractIntegrationTest.mongoPort);
	}

	@Override
	protected String getDatabaseName() {
		return "default";
	}

	@Bean
	protected MongoTemplate spMongoTemplate() throws Exception {
		return new MongoTemplate(new SimpleMongoClientDbFactory(mongoClient(), TestConstants.SP_MONGO_DATABASE), mappingMongoConverter());
	}

	@Bean
	protected MongoTemplate wscMongoTemplate() throws Exception {
		return new MongoTemplate(new SimpleMongoClientDbFactory(mongoClient(), TestConstants.WSC_MONGO_DATABASE), mappingMongoConverter());
	}

	@Bean
	protected MongoTemplate wspMongoTemplate() throws Exception {
		return new MongoTemplate(new SimpleMongoClientDbFactory(mongoClient(), TestConstants.WSP_MONGO_DATABASE), mappingMongoConverter());
	}
}
