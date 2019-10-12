package com.github.omerefrat.kafka.tutorial3;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ElasicSeachConsumer {

    static Logger logger = LoggerFactory.getLogger(ElasicSeachConsumer.class.getName());


    public static RestHighLevelClient createClient() {

        String hostname = "omer-test-cluster-8511842564.ap-southeast-2.bonsaisearch.net";
        String username = "5hv9s67u3u";
        String password = "m5gksqcfuy";

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname, 443, "https"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                        return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });
        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

    private static KafkaConsumer<String,String>  getKafkaConsumer(){
        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "my-fifth-application";
        String topic = "twitter-tweets";

        // create Consumer configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

        // create the Consumer
        KafkaConsumer<String,String> consumer = new KafkaConsumer<String, String>(properties);

        // subscribe the consumer to our topic(s)
        consumer.subscribe(Collections.singleton(topic));

        return consumer;
    }

    public static void main(String[] args) throws IOException {

        RestHighLevelClient client = createClient();

        // setup the kafka consumer
        KafkaConsumer<String,String> kafkaConsumer = getKafkaConsumer();

        // pulling the data from kafka
        int numberOfMessagesToRead = 500;
        boolean keepOnReading = true;
        int numberOfMessagesReadSoFar = 0;

        // poll for new data
        while (keepOnReading){

            logger.info("reading tweets from kafka...");
            ConsumerRecords<String,String> records =  kafkaConsumer.poll(Duration.ofMillis(100));

            logger.info(records.count() + " new tweets received");
            for (ConsumerRecord<String,String> record :records){
                numberOfMessagesReadSoFar++;

                if (!record.value().isEmpty()){
                    String jsonString = record.value();

                    logger.info("start saving tweet text: " + jsonString);
                    IndexRequest indexRequest = new IndexRequest(
                            "twitter",
                            "tweets")
                            .source(jsonString, XContentType.JSON);

                    IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
                    String id = indexResponse.getId();
                    logger.info(id);
                }

                if (numberOfMessagesReadSoFar == numberOfMessagesToRead){
                    keepOnReading = false;
                    break;
                }
            }
        }
        logger.info("Existing the application");
    }
}