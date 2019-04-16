package com.nibado.example.springtestcontainers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.nibado.example.springtestcontainers.dto.Counter;
import com.nibado.example.springtestcontainers.dto.Counters;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import java.util.Map;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class CounterIntegrationTest extends AbstractIntegrationTest {
    private static final String TABLE = "counters";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    AmazonDynamoDB dynamoDB;

    @Autowired
    CounterRepository repository;

    @Before
    public void clear() {
        //Clear all items from the table before every test
        repository.findAll().forEach(c -> repository.delete(c.getCounter()));
    }

    @Test
    public void countersTableExists() {
        assertThat(dynamoDB.listTables().getTableNames()).contains("counters");
    }

    @Test
    public void findAll() {
        putCounter("findAll", 42);
        final Counters response = restTemplate.getForObject("/counter", Counters.class);

        assertThat(response.getCounters()).containsExactly(new Counter("findAll", 42));
    }

    @Test
    public void get() {
        putCounter("get", 42);

        final Counter response = restTemplate.getForObject("/counter/get", Counter.class);

        assertThat(response).isEqualTo(new Counter("get", 42));
    }

    @Test
    public void get_404() {
        final ResponseEntity<Counter> response = restTemplate.getForEntity("/counter/get_404", Counter.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void delete() {
        putCounter("delete", 42);
        restTemplate.delete("/counter/delete", Counter.class);

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void increment() {
        Counter response = restTemplate.postForObject("/counter/increment", null, Counter.class);

        assertThat(response).isEqualTo(new Counter("increment", 1));

        response = restTemplate.postForObject("/counter/increment", null, Counter.class);

        assertThat(response).isEqualTo(new Counter("increment", 2));
    }

    private void putCounter(String key, int value) {
        final HashMap<String, AttributeValue> map = new HashMap<>();
        map.put("counter", new AttributeValue().withS(key));
        map.put("value", new AttributeValue().withN(Integer.toString(value)));
        dynamoDB.putItem(TABLE, map);
    }
}
